---
title: "Thrift Note"
date: 2018-02-06T13:22:50+08:00
categories: ["Notes"]
tags: ["thrift"]
description: "Note of thrift first guide"
draft: false
---

笔记，来自网络，来源不可考。

# 一个简单的 Thrift 实例

## thrift 源文件

清单 1. Hello.thrift

```
 namespace java service.demo 
 service Hello{ 
  string helloString(1:string para) 
  i32 helloInt(1:i32 para) 
  bool helloBoolean(1:bool para) 
  void helloVoid() 
  string helloNull() 
 }
```

## HelloSerivce 实现代码

清单 2. HelloServiceImpl.java

```
 package service.demo; 
 import org.apache.thrift.TException; 
 public class HelloServiceImpl implements Hello.Iface { 
    @Override 
    public boolean helloBoolean(boolean para) throws TException { 
        return para; 
    } 
    @Override 
    public int helloInt(int para) throws TException { 
        try { 
            Thread.sleep(20000); 
        } catch (InterruptedException e) { 
            e.printStackTrace(); 
        } 
        return para; 
    } 
    @Override 
    public String helloNull() throws TException { 
        return null; 
    } 
    @Override 
    public String helloString(String para) throws TException { 
        return para; 
    } 
    @Override 
    public void helloVoid() throws TException { 
        System.out.println("Hello World"); 
    } 
 }
```

## 服务器端代码

清单 3. HelloServiceServer.java

```
 package service.server; 
 import org.apache.thrift.TProcessor; 
 import org.apache.thrift.protocol.TBinaryProtocol; 
 import org.apache.thrift.protocol.TBinaryProtocol.Factory; 
 import org.apache.thrift.server.TServer; 
 import org.apache.thrift.server.TThreadPoolServer; 
 import org.apache.thrift.transport.TServerSocket; 
 import org.apache.thrift.transport.TTransportException; 
 import service.demo.Hello; 
 import service.demo.HelloServiceImpl; 

 public class HelloServiceServer { 
    /** 
     * 启动 Thrift 服务器
     * @param args 
     */ 
    public static void main(String[] args) { 
        try { 
            // 设置服务端口为 7911 
            TServerSocket serverTransport = new TServerSocket(7911); 
            // 设置协议工厂为 TBinaryProtocol.Factory 
            Factory proFactory = new TBinaryProtocol.Factory(); 
            // 关联处理器与 Hello 服务的实现
            TProcessor processor = new Hello.Processor(new HelloServiceImpl()); 
            TServer server = new TThreadPoolServer(processor, serverTransport, 
                    proFactory); 
            System.out.println("Start server on port 7911..."); 
            server.serve(); 
        } catch (TTransportException e) { 
            e.printStackTrace(); 
        } 
    } 
 }
```

参见:

![thrift
client](/posts/2018-02-06-thrift-note.dir/uml_thrift_server_side_sequence.png) 。

## 客户端代码

清单 4. HelloServiceClient.java

```
package service.client; 
 import org.apache.thrift.TException; 
 import org.apache.thrift.protocol.TBinaryProtocol; 
 import org.apache.thrift.protocol.TProtocol; 
 import org.apache.thrift.transport.TSocket; 
 import org.apache.thrift.transport.TTransport; 
 import org.apache.thrift.transport.TTransportException; 
 import service.demo.Hello; 

 public class HelloServiceClient { 
 /** 
     * 调用 Hello 服务
     * @param args 
     */ 
    public static void main(String[] args) { 
        try { 
            // 设置调用的服务地址为本地，端口为 7911 
            TTransport transport = new TSocket("localhost", 7911); 
            transport.open(); 
            // 设置传输协议为 TBinaryProtocol 
            TProtocol protocol = new TBinaryProtocol(transport); 
            Hello.Client client = new Hello.Client(protocol); 
            // 调用服务的 helloVoid 方法
            client.helloVoid(); 
            transport.close(); 
        } catch (TTransportException e) { 
            e.printStackTrace(); 
        } catch (TException e) { 
            e.printStackTrace(); 
        } 
    } 
 }
```

参见：

![thrift
server](/posts/2018-02-06-thrift-note.dir/uml_thrift_client_side_sequence.png) 。

# 数据类型

- Thrift 脚本可定义的数据类型包括以下几种类型：

## 基本类型

- bool：布尔值，true 或 false，对应 Java 的 boolean
- byte：8 位有符号整数，对应 Java 的 byte
- i16：16 位有符号整数，对应 Java 的 short
- i32：32 位有符号整数，对应 Java 的 int
- i64：64 位有符号整数，对应 Java 的 long
- double：64 位浮点数，对应 Java 的 double
- string：未知编码文本或二进制字符串，对应 Java 的 String

## 结构体类型

- struct：定义公共的对象，类似于 C 语言中的结构体定义，在 Java 中是一个 JavaBean

## 容器类型

- list：对应 Java 的 ArrayList
- set：对应 Java 的 HashSet
- map：对应 Java 的 HashMap

## 异常类型：

- exception：对应 Java 的 Exception

## 服务类型：

- service：对应服务的类

# 协议

Thrift 可以让用户选择客户端与服务端之间传输通信协议的类别，在传输协议上总体划分为文本 (text) 和二进制 (binary) 传输协议，为节约带宽，提高传输效率，一般情况下使用二进制类型的传输协议为多数，有时还会使用基于文本类型的协议，这需要根据项目 / 产品中的实际需求。常用协议有以下几种：

## TBinaryProtocol 二进制编码格式进行数据传输

使用方法如清单 3 和清单 4 所示。

## TCompactProtocol 高效率的、密集的二进制编码格式进行数据传输

构建 TCompactProtocol 协议的服务器和客户端只需替换清单 3 和清单 4 中 TBinaryProtocol 协议部分即可，替换成如下代码：

清单 5. 使用 TCompactProtocol 协议构建的 HelloServiceServer.java

```
 TCompactProtocol.Factory proFactory = new TCompactProtocol.Factory();
```

清单 6. 使用 TCompactProtocol 协议的 HelloServiceClient.java

```
 TCompactProtocol protocol = new TCompactProtocol(transport);
```

## TJSONProtocol 使用 JSON 的数据编码协议进行数据传输

构建 TJSONProtocol 协议的服务器和客户端只需替换清单 3 和清单 4 中 TBinaryProtocol 协议部分即可，替换成如下代码：

清单 7. 使用 TJSONProtocol 协议构建的 HelloServiceServer.java

```
 TJSONProtocol.Factory proFactory = new TJSONProtocol.Factory();
```

清单 8. 使用 TJSONProtocol 协议的 HelloServiceClient.java

```
 TJSONProtocol protocol = new TJSONProtocol(transport);
```

## TSimpleJSONProtocol 只提供 JSON 只写的协议，适用于通过脚本语言解析

# 传输层

## TSocket 使用阻塞式 I/O 进行传输，是最常见的模式

使用方法如清单 4 所示。

## TFramedTransport 使用非阻塞方式，按块的大小进行传输，类似于 Java 中的 NIO

若使用 TFramedTransport 传输层，其服务器必须修改为非阻塞的服务类型，客户端只需替换清单 4 中 TTransport 部分，代码如下，清单 9 中 TNonblockingServerTransport 类是构建非阻塞 socket 的抽象类，TNonblockingServerSocket 类继承 TNonblockingServerTransport

清单 9. 使用 TFramedTransport 传输层构建的 HelloServiceServer.java

```
 TNonblockingServerTransport serverTransport; 
 serverTransport = new TNonblockingServerSocket(10005); 
 Hello.Processor processor = new Hello.Processor(new HelloServiceImpl()); 
 TServer server = new TNonblockingServer(processor, serverTransport); 
 System.out.println("Start server on port 10005 ..."); 
 server.serve();
```

清单 10. 使用 TFramedTransport 传输层的 HelloServiceClient.java

```
 TTransport transport = new TFramedTransport(new TSocket("localhost", 10005));
```

## TNonblockingTransport 使用非阻塞方式，用于构建异步客户端

使用方法请参考 Thrift 异步客户端构建

# 服务端类型

## TSimpleServer 单线程服务器端使用标准的阻塞式 I/O

清单 11. 使用 TSimpleServer 服务端构建的 HelloServiceServer.java

```
 TServerSocket serverTransport = new TServerSocket(7911); 
 TProcessor processor = new Hello.Processor(new HelloServiceImpl()); 
 TServer server = new TSimpleServer(processor, serverTransport); 
 System.out.println("Start server on port 7911..."); 
 server.serve();
```

客户端的构建方式可参考清单 4。

## TThreadPoolServer 多线程服务器端使用标准的阻塞式 I/O

使用方法如清单 3 所示。

## TNonblockingServer 多线程服务器端使用非阻塞式 I/O

使用方法请参考 Thrift 异步客户端构建
