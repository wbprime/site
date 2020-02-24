+++
title = "一次由于网络套接字文件描述符泄露导致线上服务事故原因的排查经历"
description = "最近，线上服务遭遇了一次事故。一个 Java 的网络服务，间接使用了 Ignite 作为内存数据库和 RPC 基础件；服务对 Ignite 的访问操作通过公司另外部门维护的一个二次封装接口（公共 JAR 包形式）进行。在生产环境中，运维按计划下线 Ignite 服务之后，服务的正常业务流程未受到影响；按计划继续下线 Ignite 服务所在的服务器之后，服务很快开始报 `java.net.SocketException: Too many open files` 错误，导致服务很快不可用。在测试环境部署模拟复现该现象，并排查到此次事故是由 Ignite 的一个 BUG 导致的。事实上，该 BUG 已经在 Ignite 的主线版本中被修复，但由于隔壁部门自己 FORK 了 Ignite 的较低版本并且没有及时地同步主线更新，使得服务在生产环境出现了该事故。现记录下事故原因排查的经历，以鉴后事。"
date = 2020-02-23T22:51:55+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Practise"]
tags = ["ignite", "lsof", "strace", "jstack", "TCPv6", "fd leaking"]
+++

最近，线上服务遭遇了一次事故。该服务（记为 A 服务）是一个 Java 的网络服务，间接使用了 [Ignite][ignite] 作为内存数
据库和 RPC 基础件；服务对 [Ignite][ignite] 的访问操作通过公司另外部门维护的一个二次封装接口（公共 JAR 包形式）进行。

在生产环境中，运维按计划下线 [Ignite][ignite] 服务之后，A 服务的正常业务流程未受到影响；按计划继续下线
[Ignite][ignite] 服务所在的服务器之后，A 服务很快开始报 `java.net.SocketException: Too many open files` 错误，导致 A 服务很快不可用。

我在测试环境部署模拟复现了该现象，并排查到此次事故是由 [Ignite][ignite] 的一个 [BUG][ignite-bug] 导
致的。事实上，该 [BUG][ignite-bug] 已经在 [Ignite][ignite] 的主线版本中被修复，但由于隔壁部门自己 FORK 了 [Ignite][ignite] 的较低版本并且没有及时地同步主线更新，使得 A 服务在生产环境出现了该事故。

现记录下事故原因排查的经历，以鉴后事。

<!-- more -->

# Background

[Ignite][ignite] 是一个多功能的网络中间件，可以提供分布式的内存缓存、内存数据网格（K-V）、内存数据库（SQL）、网格计算、RPC 基础支持和消息中间件等功能。

[Ignite][ignite] 提供嵌入式的开发包，将每一个包含了开发包的服务进程扩展为一个 [Ignite][ignite] 节点；同一个服务的不同实
例或不同服务的不同实例，只要包含相同的 [Ignite][ignite] 集群配置，就会相互发现成为同一个集群的子节点；这样就不需要额外部署 [Ignite][ignite] 服务实例，只需要部署服务实例。

A 服务通过包含隔壁部门二次封装的 [Ignite][ignite] 开发包访问 [Ignite][ignite] 集群，主要是服务 B。

```
-------------     -------------
| Service A | ==> | Service B |
-------------     -------------
```

值得一提的是，服务 A 和服务 B 不是部署在同一台服务器上。

# Crash Report

线上运维一共进行了两次操作：

1. 将服务 B 实例关停。
2. 将服务 B 实例所在的服务器下线。

[Ignite][ignite] 集群的各节点之间会保持心跳连接以实现集群拓扑的动态变化。

当服务 B 实例被关停之后，服务 A 中 [Ignite][ignite] 相关的线程会不停地重试连接服务 B 的监听端口，但不出意外地会失败；服务 A 中的其他业务逻辑理论上不会受到影响。事实也是如此，此时服务 A 的运行正常。

当服务 B 实例的服务器被下线之后，理论上对服务 A 的影响同服务 B 被关停。但是，在等待一段时间后（5分
钟级别），服务 A 开始出现 `java.net.SocketException: Too many open files` 异常。
从异常说明可以猜测：此时服务 A 中存在一个或多个不停地创建 Socket 但不释放的线程，导致网络套接字文件
描述符被耗尽。一个合理的猜测是本次的网络套接字泄露是由于 [Ignite][ignite] 的心跳线程导致的：在不查看具体代码的逻辑下，可以猜测出该线程的主体逻辑是一个 `while` 无限循环，循环体中尝试连接目标节点并发送心跳消息，如果检测心跳成功，则休眠给定时间间隔以等待下一次心跳检测；如果失败，则立即重试给定次数以确定目标节点是否已下线。

这个过程基本上是轮询的套路，**我最开始认为** 基本上不会出现网络套接字泄露的问题（[Ignite][ignite] 可是高大上的大厂产品 __^-^__），而且心跳重试过程如果有问题的话，服务被关停和服务器被下线时的表现不会不一样（__^-^__）。

所以，开始尝试从底层定位问题。

# Steps

## Reproduce BUG

首先尝试在测试环境复现该 BUG 。修改配置文件之后再重启即可。

此时可以通过 `jps -lmv` 命令查看到服务运行实例的进程号。

然后简单地查看 CPU/Memory/IO 的状态，基本与正常情况没有差别。

然后查看进程使用文件描述符的情况，运行 `lsof -p $pid` 。[lsof](https://linux.die.net/man/8/lsof) 是
Linux 下查看打开的文件描述符的工具，`-p` 选项指定只列出给定进程号的进程所打开的文件描述符。

在等待大约 5 分钟之后，开始出现下述输出：

```
$ lsof -p 16404
...omit...
java    16404 fo_dev 1885u     sock                0,7       0t0  18695292 protocol: TCPv6
java    16404 fo_dev 1886u     sock                0,7       0t0  18695293 protocol: TCPv6
java    16404 fo_dev 1887u     sock                0,7       0t0  18695294 protocol: TCPv6
java    16404 fo_dev 1888u     sock                0,7       0t0  18695295 protocol: TCPv6
java    16404 fo_dev 1889u     sock                0,7       0t0  18695298 protocol: TCPv6
...omit...
```

可以看到服务 A 的实例在不断地创建 `TCPv6` 类型的套接字。该类型套接字的个数在几分钟之内达到 1000 以上。

查看 `ulimit` 的输出，没有异常。

这个 `TCPv6` 看的我一头雾水，怎么好像跟 IPv6 还有关系？

使用关键字上网搜寻，只找到这一篇 [Tomcat服务故障排查：打开文件过多](https://blog.csdn.net/define_us/article/details/84950934) 有点相关，但是其也没有提到原因和解决办法。

## Disable JDK IPv6 Stack

由于当前的现象是服务不断创建 `TCPv6` 类型的套接字，所以开始怀疑是 IPv6 相关的问题。

参考 [Networking IPv6 User Guide for JDK/JRE](https://docs.oracle.com/javase/7/docs/technotes/guides/net/ipv6_guide/index.html) 的说明，修改服务启动参数，添加 `-Djava.net.preferIPv4Stack=true` 选项禁用 IPv6。

重新运行 `lsof` 得到以下输出（类似）：

```
$ lsof -p 16404
...omit...
java    16404 fo_dev 1885u     sock                0,7       0t0  18695292 protocol: TCP
java    16404 fo_dev 1886u     sock                0,7       0t0  18695293 protocol: TCP
java    16404 fo_dev 1887u     sock                0,7       0t0  18695294 protocol: TCP
java    16404 fo_dev 1888u     sock                0,7       0t0  18695295 protocol: TCP
java    16404 fo_dev 1889u     sock                0,7       0t0  18695298 protocol: TCP
...omit...
```

这说明服务套接字泄露的问题与 IPv6 无关。

## `lsof` Per Thread

回过头去看看 `lsof` 的手册，发现有一个选项 `-K`：

 > **-K**       selects the listing of tasks (threads) of processes, on dialects  where  task  (thread) reporting  is supported.  (If help output - i.e., the output of the -h or -?  options - shows this option, then task (thread) reporting is supported by the dialect.)
 >                When -K and -a are both specified on Linux,  and  the  tasks  of  a  main  process  are selected  by  other  options,  the main process will also be listed as though it were a task, but without a task ID.  (See the description of the TID column in the OUTPUT sec‐
 >                tion.)

 >                Where the FreeBSD version supports threads, all threads will be listed with their IDs.
 >                In  general  threads  and tasks inherit the files of the caller, but may close some and open others, so lsof always reports all the open files of threads and tasks.

这个选项的意思是在输出的结果中显示文件描述符对应的线程号，棒极了。

输出如下：

```
$ lsof -p 16404 -K
...omit...
java      16404 17818    fo_dev 1314u     sock                0,7        0t0  18703132 protocol: TCPv6
java      16404 17818    fo_dev 1315u     sock                0,7        0t0  18703133 protocol: TCPv6
java      16404 17818    fo_dev 1316u     sock                0,7        0t0  18703134 protocol: TCPv6
java      16404 17818    fo_dev 1317u     sock                0,7        0t0  18703135 protocol: TCPv6
java      16404 17818    fo_dev 1318u     sock                0,7        0t0  18703138 protocol: TCPv6
...omit...
```

结果输出很长，主要表现为同一个文件描述符在所有的线程上都有显示。很明显，Linux (Red Hat Enterprise Linux Server x86_64 7.4) 下面不支持这个选项，所以 `all threads will be listed with their IDs`。

## `strace`

套接字泄露确实在发生，但是却没有办法能明确定位到问题在哪里。如果能直接看到进程调用 `socket` 和 `close` 系统调用的记录，就可以知道在哪里发生了套接字泄露。

于是找到了 [strace](https://linux.die.net/man/1/strace) 工具，该工具可以跟踪进程的系统调用记录。

```
$ strace -t -T -f -p 23345 -e trace=network,close -o strace.out
...omit...
904   16:52:35 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2379 <0.000105>
904   16:52:35 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2380 <0.000061>
904   16:52:35 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2381 <0.000070>
904   16:52:35 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2382 <0.000062>
904   16:52:37 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2383 <0.000170>
904   16:52:37 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2384 <0.000188>
904   16:52:37 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2385 <0.000161>
904   16:52:37 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2386 <0.000075>
904   16:52:39 close(2387)              = 0 <0.000190>
904   16:52:39 socket(AF_INET, SOCK_DGRAM|SOCK_NONBLOCK, IPPROTO_IP) = 2387 <0.000106>
904   16:52:39 close(2387)              = 0 <0.000059>
904   16:52:39 socket(AF_INET, SOCK_DGRAM|SOCK_NONBLOCK, IPPROTO_IP) = 2387 <0.000104>
904   16:52:39 close(2387)              = 0 <0.000053>
904   16:52:39 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2387 <0.000111>
...omit...
32663 16:52:40 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2391 <0.000115>
32663 16:52:41 close(2391)              = 0 <0.000045>
32663 16:52:51 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2415 <0.000113>
32663 16:52:52 close(2415)              = 0 <0.000061>
32663 16:53:02 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2435 <0.000124>
32663 16:53:03 close(2435)              = 0 <0.000050>
32663 16:53:13 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2459 <0.000210>
32663 16:53:14 close(2459)              = 0 <0.000061>
32663 16:53:24 socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 2479 <0.000111>
32663 16:53:25 close(2479)              = 0 <0.000053>
...omit...
```

选项 `-t` 表明在输出结果中加上时间戳信息，`-T` 表示在输出结果中加上系统调用的耗时信息，`-f` 表示在输出结果中加上关联的线程号信息 (Trace child processes as they are created by currently traced processes as a result of the `fork(2)`, `vfork(2)` and `clone(2)` system calls.  Note that `-p` PID `-f` will attach all threads of process PID if it is multi-threaded, not only thread with thread_id = PID.)，`-p` 输出指定进程的结果，`-e` 表示只输出网络相关和 `close()` 系统调用的日志，`-o` 表示将输出结果存储到指定文件中。

说明，上述输出经过了整理，过滤了无关的输出并按照线程分组按时间排序。

可以直接从输出结果中发现未成对的 `socket()/close()` 调用，这表明出现了套接字泄露。同时也可以看到发生泄露的线程号 (904)。

## `jstack`

通过 `jstack` 可以得到指定 Jvm 进程的线程堆栈信息（包含线程号）。

但是 `jstack` 输出的线程号是 16 进制的结果，可以通过 `printf` 来进行 10/16 进制的线程号之间的转换。

```
$ printf %x 904
388
$ printf %d 0x388
904
```

现在就定位到了有问题的线程的堆栈了。

```
"tcp-client-disco-msg-worker-#7%XXXXXXX%" #182 prio=5 os_prio=0 tid=0x00007f4e1c42b000 nid=0x388 waiting on condition [0x00007f4e14dcf000]
   java.lang.Thread.State: TIMED_WAITING (sleeping)
        at java.lang.Thread.sleep(Native Method)
        at org.apache.ignite.spi.discovery.tcp.ClientImpl.joinTopology(ClientImpl.java:575)
        at org.apache.ignite.spi.discovery.tcp.ClientImpl.access$900(ClientImpl.java:124)
        at org.apache.ignite.spi.discovery.tcp.ClientImpl$MessageWorker.tryJoin(ClientImpl.java:1825)
        at org.apache.ignite.spi.discovery.tcp.ClientImpl$MessageWorker.body(ClientImpl.java:1541)
        at org.apache.ignite.spi.IgniteSpiThread.run(IgniteSpiThread.java:62)
```

说明，上述输出经过了整理，修改了线程名中不影响结论的部分。

这样就定位到了有问题的线程。

现在必须要去查看 [Ignite][ignite] 的实际代码了。

## Review Ignite Code

按照堆栈提示定位到问题代码，具体过程不赘述了。

代码片段摘抄如下：

```java
// [[[3]]]
protected Socket openSocket(InetSocketAddress sockAddr, IgniteSpiOperationTimeoutHelper timeoutHelper) throws IOException, IgniteSpiOperationTimeoutException {
    return this.openSocket(this.createSocket(), sockAddr, timeoutHelper);
}

// [[[5]]]
protected Socket openSocket(Socket sock, InetSocketAddress remAddr, IgniteSpiOperationTimeoutHelper timeoutHelper) throws IOException, IgniteSpiOperationTimeoutException {
    assert remAddr != null;

    // [[[6]]]
    InetSocketAddress resolved = remAddr.isUnresolved() ? new InetSocketAddress(InetAddress.getByName(remAddr.getHostName()), remAddr.getPort()) : remAddr;
    InetAddress addr = resolved.getAddress();

    assert addr != null;

    sock.connect(resolved, (int)timeoutHelper.nextTimeoutChunk(this.sockTimeout));
    this.writeToSocket(sock, (TcpDiscoveryAbstractMessage)null, (byte[])U.IGNITE_HEADER, timeoutHelper.nextTimeoutChunk(this.sockTimeout));
    return sock;
}

// [[[4]]]
Socket createSocket() throws IOException {
    Socket sock;
    if (this.isSslEnabled()) {
        sock = this.sslSockFactory.createSocket();
    } else {
        sock = new Socket();
    }

    sock.bind(new InetSocketAddress(this.locHost, 0));
    sock.setTcpNoDelay(true);
    return sock;
}

// [[[0]]]
// ...omit...
Socket sock = null;
try {
    // ...omit...
    sock = openSocket(addr, timeoutHelper); // [[[1]]]
    // ...omit...
} catch (IgniteCheckedException | IOException e) {
    U.closeQuiet(sock); // close socket // [[[2]]]
    // ...omit...
}
// ...omit...
```

[Ignite][ignite] 内部会重试连接节点，连接节点的逻辑简略如 `[[[0]]]` 处的代码所示：在 `try` 块中创建连接处理套接字 (`[[[1]]]` 处)，在 `catch` 块中关闭套接字 (`[[[2]]]` 处)。

创建并连接套接字的操作在 `[[[3]]]` 中：首先创建套接字 (`[[[4]]]` 处)，然后连接到目标服务器地址 (`[[[5]]]` 处)，最后返回创建的套接字。

问题在于连接目标服务器地址的过程中有可能会抛出异常 (`[[[6]]]` 处)，导致已创建的套接字并不能被返回到最外层以关闭。连接目标服务器之前需要获取目标服务器的地址 (`[[[6]]]` 处)，此处调用了 `InetAddress.getByName()` 方法试图将一个主机名解析为合法的IP 地址，如果无法解析则会抛出 `java.net.UnknownHostException` 异常，导致 `[[[5]]]` 方法半途退出，从而导致套接字泄露。

## Verify using Plain IP

[Ignite][ignite] 在解析主机名时抛出异常导致套接字泄露，可以通过配置目标地址为显式 IP 地址来验证，而之前配置的服务 B 的地址是主机名（通过内网 DNS 解析定位到服务器地址）。

修改配置之后重启服务并观察 `lsof` 的输出，发现不存在套接字泄露现象。

果然！

## Ignite on Github

还有一个问题是，作为这么牛的开源项目，应该不能存在这么低级的 BUG 吧。

上 [Github](https://github.com/apache/ignite) 看了一下，发现这个 BUG 已经被 [发现并 FIX][ignite-bug] 了，时间是 **2018-09-21**，呃。

看样子是隔壁部门的锅，其维护了一个较低版本的 [Ignite][ignite]，没有及时跟踪官方的主线修复。

# Conclusion

在网络服务出现 `Too many open files` 类似的异常之后，可以考虑排查服务是否存在网络套接字文件描述符泄露
的 BUG。

1. 首先使用 `jps` 工具获取到目标服务的进程号
2. 使用 `lsof` 工具查看目标进程打开的文件描述符的类型和特征
3. 使用 `jstack` 工具查看目标进程的线程堆栈是否有异常
4. 使用 `strace` 工具查看目标进程的系统调用，考虑添加 `-f` 参数显示对应的线程号
5. 使用 `jstack` 工具找到目标进程中对应线程号的堆栈信息
6. 查看对应代码，最终定位问题

---

以上。

[ignite]: https://ignite.apache.org/ "Apache Ignite: In-Memory Computing Platform"
[ignite-bug]: https://github.com/apache/ignite/commit/6c3a486f0d7f0dd55c377af233d7c525d86f600a
"Fixed socket leak in TcpDiscoverySpi"
