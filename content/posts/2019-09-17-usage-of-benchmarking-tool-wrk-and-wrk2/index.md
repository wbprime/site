+++
title = "Usage of Benchmarking Tool WRK and WRK2"
description = "wrk 是一款短小精悍又备受赞誉的开源性能测试工具，能够用来对 HTTP 服务进行压测；wrk2 是 对 wrk 的改进，增加了压测结果的直方图输出。网路上有不少介绍 wrk 或 wrk2 的文章，但大多泛泛而谈，对于压测的结果输出项的解释也是云里雾里或者简单跳过，殊为遗憾。本文主要介绍 wrk 和 wrk2 的使用，并在阅读源码的基础上对输出结果的项进行解释。"
date = 2019-09-17T09:27:06+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Utils"]
tags = ["benchmark", "load-test", "wrk", "wrk2"]
+++


[wrk][wrk] 是一款短小精悍又备受赞誉的开源性能测试工具，能够用来对 HTTP 服务进行压测；[wrk2][wrk2] 是
对 [wrk][wrk] 的改进，增加了压测结果的直方图输出。

网路上有不少介绍 [wrk][wrk] 或 [wrk2][wrk2] 的文章，但大多泛泛而谈，对于压测的结果输出项的解释也是云
里雾里或者简单跳过，殊为遗憾。

本文主要介绍 [wrk][wrk] 和 [wrk2][wrk2] 的使用，并在阅读源码的基础上对输出结果的项进行解释。

<!-- more -->

# WRK

来自官方 [wrk][wrk] 的介绍：

> wrk is a modern HTTP benchmarking tool capable of generating significant load when run on a single
> multi-core CPU. It combines a multithreaded design with scalable event notification systems such as
> epoll and kqueue.

简单说，[wrk][wrk] 是一个用来对 HTTP 服务进行压测的工具，能够有效利用 epoll/kqueue 技术在普通的 PC
上模拟较大的一个请求负载。

## 基本用法

[wrk][wrk] 的安装非常简单，最直接的方式就是从 [Github][wrk] 上下载源代码然后 `make -j8`。

其单独无选项和参数运行的输出如下：

```
Usage: wrk <options> <url>
  Options:
    -c, --connections <N>  Connections to keep open
    -d, --duration    <T>  Duration of test
    -t, --threads     <N>  Number of threads to use

    -s, --script      <S>  Load Lua script file
    -H, --header      <H>  Add header to request
        --latency          Print latency statistics
        --timeout     <T>  Socket/request timeout
    -v, --version          Print version details

  Numeric arguments may include a SI unit (1k, 1M, 1G)
  Time arguments may include a time unit (2s, 2m, 2h)
```

1. `-c` 选项。同时保持打开的连接数，就是字面意思。
2. `-d` 选项。测试的时长；wrk 在开始执行指定的测试计划之后，会启动一个或多个工作线程分别开始在连接上发送请求接收响应，主线程会等待工作线程固定的时间，这个时间就是测试的时长 `-d`。
3. `-t` 选项。测试的并发线程数；wrk 在开始执行指定的测试计划之后，会启动指定数量的工作线程分别开始在连接上发送请求接收响应，每个工作线程会非阻塞地在多个连接上工作固定的时间；每个线程的连接数由 `-c` 指定的值除以并发线程数确定；每个线程的工作时长由 `-d` 指定的值确定。
4. `-s` 选项。可以使用 [Lua](https://www.lua.org/) 来扩展测试计划，这需要了解 wrk 的 Lua 扩展点。
5. `-H` 选项。可以在每一个 HTTP 请求中添加上自定义的请求头，比如 `"Host: a.b.c.org"`。
6. `--latency` 选项。不填加此选项，则压测的结果会比较简单；添加此选项之后，压测的结果中会包含请求延时百分位信息。
7. `--timeout` 选项。每个请求的超时设置。
8. `-v` 选项。输出当前 wrk 的版本信息并退出。

综上，常用的 [wrk][wrk] 命令行如下：

```sh
./wrk -c 100 -d 60s -t 16 --latency http://127.0.0.1:1993/instant
```

## 输出结果

命令 `./wrk -c 1700 -d 60s -t 16 --latency http://127.0.0.1:1993/instant` 的典型输出结果如下：

```
(01) Running 1m test @ http://127.0.0.1:1993/instant
(02)   16 threads and 1700 connections
(03)   Thread Stats   Avg      Stdev     Max   +/- Stdev
(04)     Latency    28.51ms   11.92ms 688.79ms   96.20%
(05)     Req/Sec     3.65k   542.74    14.77k    87.77%
(06)   Latency Distribution
(07)      50%   27.86ms
(08)      75%   29.95ms
(09)      90%   32.71ms
(10)      99%   46.93ms
(11)   3491898 requests in 1.00m, 272.70MB read
(12)   Socket errors: connect 0, read 0, write 0, timeout 435
(13) Requests/sec:  58184.19
(14) Transfer/sec:      4.54MB
```

1. 第 1 行和第 2 行简要说明了测试计划的情况和参数选项信息。
2. 第 3/4/5 行是线程级别的测试统计结果，包括请求延时（从请求发出到收到响应的耗时）的平均值、标准差、最大值、平均值上下一倍标准差范围内的请求占比和每秒请求数（QPS）的对应值。
3. 第 6/7/8/9/10 行是请求延时的百分位统计，分别有 TP50/TP75/TP90/TP99。
4. 第 11/12 行是测试结果的总体信息，如果有的话还会包含 HTTP 的响应不是 `20x` 的返回码的数据。
5. 第 13 行是总的 QPS。
6. 第 14 行是总的传输数据量。

比较重要的部分是 `Thread Stats` & `Latency Distribution` & `Socket errors`。

### Thread Stats

线程级别的统计输出了请求延时和每秒请求数的平均值、最大值和标准差等信息。

```c
typedef struct {
    uint64_t count;
    uint64_t limit;
    uint64_t min;
    uint64_t max;
    uint64_t data[];
} stats;
```

统计采样数据存储在一个巨大的数组 `data` 来存放统计数据。每得到一个采样数据之后，该采样数据被放入数组中，对 `count` 加一，并且同步更新 `min` 和 `max` 的值。由于采样是在多个线程并发进行，所以对上述数据值的更新都是采用了原子操作保护的。

数组的索引是采样值（请求延时即微秒数，请求数即每100毫秒请求数的1000倍），值是相同采样值重复的次数。

```
-------------------------------------------------
| 0 | ... |  101 | ... | 135 | ... |  501 | ... |
-------------------------------------------------
| 0 | ... | 2050 | ... |  71 | ... | 1049 | ... |
-------------------------------------------------
```

上述的数组第一行是索引，第二行是数值。可能表示的是请求延时的采样，延时为 101us 的请求数有 2050 次，135us 的 71 次，501us 的 1049 次。

在程序初始化时，请求延时对应的采样数组被初始化为请求超时时长的毫秒数（`--timeout` 选项指定值，或 2000s 的默认值），请求数对应初始化为 10000000。

所以，`Latency` 行和 `Req/Sec` 行的数据分别通过计算上述数据结构的对应值（均值、标准差等）得到。

### Latency Distribution

延时分布即请求延时的百分位 [Percentile](https://en.wikipedia.org/wiki/Percentile) 信息，包括 TP50/TP75/TP90/TP99。

关于为什么需要在性能测试中关注百分位，可以参考 [性能测试应该怎么做？](https://coolshell.cn/articles/17381.html) 。

### Socket Errors

做性能测试时，需要得到的是在 HTTP 服务返回正确数据的情况下的极限能力；所以，每一次性能测试需要关注请求错误，包括连接失败、读失败、写失败、请求超时、返回码不符合预期等。

### "Req/Sec" VS "Requests/sec"

在 [wrk][wrk] 的输出中，有两个地方显示的是压测的 QPS: 第 5 行 "Req/Sec" 和 第 13 行 "Requests/sec"。

[如上所述](#Thread Stats)，"Req/Sec" 是线程采样数据的统计结果：多个线程分别每 100ms 收集一次累积的请求响应数，该值乘以 1000 放入统计数组中参与最终的统计；假若程序总的 1s 中的请求响应数是 1000，线程数是 4，则统计数组中会出现 `4 x 10 = 40` 个采样，"Req/Sec" 是这 40 个采样的统计结果（如均值）。

而 "Requests/sec" 是简单地将所有的请求响应数除以测试时长，按前一段的描述应该得到 1000。

所以，基本上 "Req/Sec" 的值是 "Requests/sec" 值除以线程数；但由于采样误差和[其他原因](https://github.com/wg/wrk/issues/259)，二者的值会有一个差值存在，不是严格相等。

# WRK2 基本用法

[Gil Tene](https://www.linkedin.com/in/giltene) 发现了 [wrk][wrk] 在进行性能测试采样和衡量时存在一些误解和不足，于 2014 年 fork 了 [wrk][wrk] 并添加了基于 [HDR 直方图](http://hdrhistogram.org/) 的更详细的延时分布和对 Coordinated Omission 进行修正的延时统计，还有固定吞吐量模式支持，这就是 [wrk2][wrk2]。

> wrk2 is wrk modifed to produce a constant throughput load, and accurate latency details to the high 9s (i.e. can produce accurate 99.9999%'ile when run long enough). In addition to wrk's arguments, wrk2 takes a throughput argument (in total requests per second) via either the --rate or -R parameters (default is 1000).

## Coordinated Omission

Coordinated Omission 是性能测试中的一个陷阱，在 [Your Load Generator is Probably Lying to You - Take the Red Pill and Find Out Why](http://highscalability.com/blog/2015/10/5/your-load-generator-is-probably-lying-to-you-take-the-red-pi.html) 和 [七层网络性能基准测试中的协调遗漏问题--Coordinated Omission](https://blog.csdn.net/minxihou/article/details/97318121) 中有详细介绍。

简单地说，在性能测试中会有两种情况造成测试得到的请求延时数据不能反应真实的情况。

1. 当测试计划中请求的间隔小于单次请求耗时时，会造成后续的请求会等待被发送。由于 HTTP 是请求响应模型，同一个连接内只有当前一个请求的响应收到之后才能继续发送新的请求，所以如果计划 1s 之内要发送 10 个请求，也就是请求 1 被发送之后 100ms 就需要发送请求 2；但是请求 1 对应的响应 1 过了 150ms 才收到，所以实际上请求 2 在第 150ms 才被发出去；如果再过 150ms 之后请求 2 的响应才收到，则参与统计的采样是请求 1 对应的延时 150ms 和 请求 2 对应的延时 150ms；这看上去很合理，但是如果考虑到实际上请求 2 等待了 50ms 才发送请求的话，请求 2 的延时应该是 200ms。距离来说的话，就是你去麦当劳排队买东西，你买东西的耗时应该是你排队的时间加上店员处理你的订单的时间；如果排队超过 10 分钟，你可能就会不耐烦，尽管有可能店员给你拿一个汉堡你再付钱总共不超过 30s。
2. 当测试计划被其他因素影响，导致请求的延时中包含了其他部分的耗时，比如进程调度或作业挂起等导致的睡眠时间。

[wrk](https://github.com/wg/wrk/commit/ef6a836b7d41cdbe8dae27d81d43f2d03a1665ef) 已经于 2015 年添加了对 Coordinated Omission 的修正。

## 基本用法

[wrk2][wrk2] 的安装也非常简单，最直接的方式就是从 [Github][wrk2] 上下载源代码然后 `make -j8`，其编译出来的二进制程序名，如果不特别指定的话，也是 `wrk`。

其单独无选项和参数运行的输出如下：

```
Usage: wrk <options> <url>
  Options:
    -c, --connections <N>  Connections to keep open
    -d, --duration    <T>  Duration of test
    -t, --threads     <N>  Number of threads to use

    -s, --script      <S>  Load Lua script file
    -H, --header      <H>  Add header to request
    -L  --latency          Print latency statistics
    -U  --u_latency        Print uncorrected latency statistics
        --timeout     <T>  Socket/request timeout
    -B, --batch_latency    Measure latency of whole
                           batches of pipelined ops
                           (as opposed to each op)
    -v, --version          Print version details
    -R, --rate        <T>  work rate (throughput)
                           in requests/sec (total)
                           [Required Parameter]


  Numeric arguments may include a SI unit (1k, 1M, 1G)
  Time arguments may include a time unit (2s, 2m, 2h)
```

参数选项基本同 [wrk][wrk]。

1. `-L` 选项。输出经 HDR 直方图增强后的延时分布信息。
2. `-U` 选项。输出经 HDR 直方图增强后的延时分布信息，与 `-L` 选项不同的是输出未修正 Coordinated Omission 的延时数据。
3. `-R` 选项。设置测试计划的预期负载。

与 [wrk][wrk] 不同的是，[wrk2][wrk2] 在执行测试计划时需要指定期望的负载，而不是尽全力产生最大的负载。

## 输出结果

命令 `./wrk -c 300 -d 60s -t 16 -R 1200 --latency http://127.0.0.1:1993/instant` 的典型输出结果如下：

```
Running 1m test @ http://10.145.67.20:8080/entry/rest0
  16 threads and 300 connections
  Thread calibration: mean lat.: 1.816ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.759ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.794ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.802ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.804ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.814ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.788ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.775ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.794ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.808ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.804ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.807ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.816ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.796ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.771ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.741ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.84ms    1.24ms  43.68ms   98.54%
    Req/Sec    77.48    100.86   777.00     66.65%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.73ms
 75.000%    2.01ms
 90.000%    2.32ms
 99.000%    3.86ms
 99.900%   20.09ms
 99.990%   29.93ms
 99.999%   40.51ms
100.000%   43.71ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.813     0.000000            1         1.00
       1.192     0.100000         5995         1.11
       1.356     0.200000        11983         1.25
       1.493     0.300000        18014         1.43
       1.616     0.400000        23995         1.67
       1.734     0.500000        29979         2.00
       1.789     0.550000        32932         2.22
       1.847     0.600000        35990         2.50
       1.899     0.650000        38931         2.86
       1.954     0.700000        41939         3.33
       2.013     0.750000        44904         4.00
       2.049     0.775000        46407         4.44
       2.089     0.800000        47925         5.00
       2.135     0.825000        49438         5.71
       2.185     0.850000        50902         6.67
       2.247     0.875000        52424         8.00
       2.279     0.887500        53144         8.89
       2.317     0.900000        53888        10.00
       2.361     0.912500        54648        11.43
       2.407     0.925000        55396        13.33
       2.463     0.937500        56132        16.00
       2.499     0.943750        56512        17.78
       2.535     0.950000        56888        20.00
       2.579     0.956250        57263        22.86
       2.629     0.962500        57636        26.67
       2.691     0.968750        58010        32.00
       2.727     0.971875        58195        35.56
       2.769     0.975000        58377        40.00
       2.821     0.978125        58563        45.71
       2.891     0.981250        58752        53.33
       3.013     0.984375        58940        64.00
       3.119     0.985938        59032        71.11
       3.279     0.987500        59125        80.00
       3.621     0.989062        59219        91.43
       4.123     0.990625        59311       106.67
       5.139     0.992188        59405       128.00
       5.947     0.992969        59453       142.22
       7.071     0.993750        59498       160.00
       8.367     0.994531        59545       182.86
      10.527     0.995313        59592       213.33
      12.343     0.996094        59639       256.00
      12.999     0.996484        59662       284.44
      13.879     0.996875        59685       320.00
      14.839     0.997266        59710       365.71
      15.943     0.997656        59733       426.67
      17.007     0.998047        59757       512.00
      17.519     0.998242        59767       568.89
      17.999     0.998437        59779       640.00
      18.511     0.998633        59792       731.43
      19.279     0.998828        59802       853.33
      20.367     0.999023        59814      1024.00
      20.687     0.999121        59820      1137.78
      21.215     0.999219        59826      1280.00
      22.751     0.999316        59832      1462.86
      23.583     0.999414        59837      1706.67
      24.095     0.999512        59843      2048.00
      25.231     0.999561        59846      2275.56
      26.159     0.999609        59849      2560.00
      26.527     0.999658        59852      2925.71
      26.895     0.999707        59855      3413.33
      27.343     0.999756        59858      4096.00
      27.791     0.999780        59859      4551.11
      28.767     0.999805        59861      5120.00
      28.895     0.999829        59862      5851.43
      29.439     0.999854        59864      6826.67
      29.519     0.999878        59865      8192.00
      29.935     0.999890        59866      9102.22
      33.567     0.999902        59867     10240.00
      33.567     0.999915        59867     11702.86
      36.991     0.999927        59868     13653.33
      38.047     0.999939        59869     16384.00
      38.047     0.999945        59869     18204.44
      39.871     0.999951        59870     20480.00
      39.871     0.999957        59870     23405.71
      39.871     0.999963        59870     27306.67
      40.511     0.999969        59871     32768.00
      40.511     0.999973        59871     36408.89
      40.511     0.999976        59871     40960.00
      40.511     0.999979        59871     46811.43
      40.511     0.999982        59871     54613.33
      43.711     0.999985        59872     65536.00
      43.711     1.000000        59872          inf
#[Mean    =        1.836, StdDeviation   =        1.244]
#[Max     =       43.680, Total count    =        59872]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  72016 requests in 1.00m, 5.62MB read
Requests/sec:   1200.22
Transfer/sec:     95.91KB
```

与 [wrk][wrk] 的输出相比，[wrk2][wrk2] 的输出多了一些内容：

1. "Thread calibration" 显示了 10s 的线程预热/修正的数据。
2. "Latency Distribution" 部分多了 TP999/TP9999/TP99999/TP100 的数据。
3. "Detailed Percentile spectrum" 显示了详细的百分位图谱。

综上，如果需要在相同并发度下使用不同的吞吐量QPS对 HTTP 服务进行压测，又或者需要获取更精确的 TP999/TP9999 百分位数据，可以考虑使用 [wrk2][wrk2] 代替 [wrk][wrk] 进行性能测试。

我的一个性能测试实践是先使用 [wrk][wrk] 找到一个符合基本要求的并发读（`-c` 选项）和对应的QPS，然后以这个QPS为上限使用 [wrk2][wrk2] 在相同并发度的基础上测试不同吞吐量（`-R` 选项）下的请求延时。

以上。

[wrk]: https://github.com/wg/wrk "Modern HTTP benchmarking tool"
[wrk2]: https://github.com/giltene/wrk2 "A constant throughput, correct latency recording variant of wrk"

