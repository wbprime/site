+++
title = "Spring Cloud Gateway Load Testing using Wrk"
description = "Spring Cloud Gateway 作为微服务网关被大家广泛接受，但是其性能的测试数据并没有很完备。本文详述了一轮多组的基于 wrk & wrk2 的性能测试，同时分别测试了匹配链中第一个和第八百个匹配的性能差异。发现 Spring Cloud Gateway 网关在匹配到前几位的路由时性能还可以接受，当需要经过较多次的路由匹配时，性能下降明显。"
date = 2019-09-17T16:20:48+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Spring"]
tags = ["spring-cloud-gateway", "gateway", "testing", "load-test", "wrk", "wrk2"]
+++

[Spring Cloud Gateway][gateway] 是 [Spring][spring] 官方为了完善 [Spring Cloud][springcloud] 的版图
而推出的网关服务组件，使用了非阻塞式网络模式和目前流行的响应式编程模型，吸引了很多公司和开发者的注意
力。

网络上有一些现成的对 [Spring Cloud Gateway][gateway] 的性能测试的案例，比如 [纠错帖：Zuul & Spring Cloud Gateway & Linkerd性能对比](https://www.jianshu.com/p/a19e24a6a747)。根据 [Simple benchmark comparing zuul and spring cloud gateway ](https://github.com/spencergibb/spring-cloud-gateway-bench) 的数据，[Spring Cloud Gateway][gateway] 的性能测试结果参考如下：

| Proxy    | Avg Latency | Avg Req/Sec/Thread |
|----------|-------------|--------------------|
| gateway  | 6.61ms      | 3.24k              |
| linkered | 7.62ms      | 2.82k              |
| zuul     | 12.56ms     | 2.09k              |
| none     | 2.09ms      | 11.77k             |

但是，由于：

1. 上述测试只是简单的对后端反向代理测试，而我们知道 [Spring Cloud Gateway][gateway] 对于路由的匹配是顺序的，匹配链后面的路由的性能并没有被关注到。
2. 测试数据被进行了简化，测试的细节被（有意地）忽略了。

所以，在根据 [Spring Cloud Gateway][gateway] 改造出了初版的公司网关系统之后，我使用 [wrk][wrk] 和 [wrk2][wrk2] 对网关进行了多轮的压测，测试结果汇总为本文。

<!-- more -->

# Overview

为测试选择 3 个对照组：

- `direct` 组，直接访问后端服务。
- `api0` 组，通过网关访问后端服务，路由在匹配链的第 1 个被匹配到。
- `api800` 组，直接访问后端服务，路由在匹配链的第 800 个被匹配到。

1. Round0: 使用 [wrk][wrk] 针对多个不同的并发度（`-c` 选项）进行压测，找到能正确返回的最大并发度和其
   时的 QPS。
2. Round1: 使用 [wrk2][wrk2] 针对上一步得到的并发度，按一定步长使用多个负载（`-R` 选项）测试得到请求
   延时，为了快速定位到合适的负载，设置步长为 5000。
3. Round2: 同 Round1，但此时只对 `api800` 组做测试且可以根据 Round1 的结果选择较小的步长 200。
4. Round3: 同 Round1，但此时只对 `api0` 组做测试且可以根据 Round1 的结果选择较小的步长 200。
5. Round4: 根据 Round1 得到的并发度和 Round3 得到的 QPS，对 `api0` 组重复做 3 组测试。
6. Round5: 根据 Round1 得到的并发度和 Round2 得到的 QPS，对 `api800` 组重复做 3 组测试。

测试使用的后端服务是一个基于 [Vert.x Web](https://vertx.io/docs/vertx-web/java/) 的非常简单的 HTTP 服务，部署于公司的私有云平台上，配置为 4核 4G 内存，访问端口为 10.144.69.53:8080；网关服务器基于 [Spring Cloud Gateway][gateway] Greenwich.SR2 版本，[Spring Boot][springboot] 的版本为 2.1.6.RELEASE，同样部署于公司的私有云平台，配置为 8核 5G 内存，访问端口为 10.145.67.20:8080；压测的客户端程序运行于一台物理机服务器上，配置为 32核 62G 内存。

# Round0

使用 [wrk][wrk] 设置选项 `-c` 值分别为：

- 100
- 300
- 500
- 700
- 900
- 1100
- 1300
- 1500
- 1700
- 1900
- 2100
- 2300
- 2500
- 2700
- 2900

对所有组进行压测。

```sh
wrk -t 16 -d 60s --latency -c ${each_c} ${endpoint}
```

得到结论：

- `direct` 组最多能到 500 并发连接
- `api0` 组最多能到 300 并发连接
- `api800` 组最多能到 100 并发连接

测试脚本及数据[可供下载](#download)。

# Round1

使用 [wrk2][wrk2] 设置选项 `-c` 值分别为：

- 100
- 200
- 300

设置选项 `-R` 值分别为：

- 100
- 1000
- 5000
- 10000
- 15000
- 20000
- 25000
- 30000

对所有组进行压测。

```sh
wrk -t 16 -d 60s --latency -c ${each_c} -R ${each_r} ${endpoint}
```

得到结论：

- `direct` 组最多能到 30000 以上的 QPS
- `api0` 组最多能到 10000 以上 15000 以下的 QPS
- `api800` 组最多能到 1000 以上 5000 以下的 QPS

# Round2

使用 [wrk2][wrk2] 设置选项 `-c` 值分别为：

- 100
- 200
- 300

设置选项 `-R` 值分别为：

- 1000
- 1200
- 1400
- 1600
- 1800
- 2000
- 2200
- 2400
- 2600

对 `api800` 组进行压测。

```sh
wrk -t 16 -d 60s --latency -c ${each_c} -R ${each_r} ${endpoint}
```

得到结论：

- `api800` 组最多能到 2000 以上 2200 以下的 QPS

# Round3

使用 [wrk2][wrk2] 设置选项 `-c` 值分别为：

- 100
- 200
- 300

设置选项 `-R` 值分别为：

- 10000
- 10200
- 10400
- 10600
- 10800
- 11000
- 11200
- 11400
- 11600
- 11800
- 12000
- 12200
- 12400
- 12600
- 12800
- 13000

对 `api0` 组进行压测。

```sh
wrk -t 16 -d 60s --latency -c ${each_c} -R ${each_r} ${endpoint}
```

得到结论：

- `api0` 组最多能到 11000 以上 11200 以下的 QPS

# Round4

使用 [wrk2][wrk2] 设置选项 `-R` 值分别为：

- 10900
- 11000
- 11100

对 `api0` 组进行重复的 3 次压测。

```sh
wrk -t 16 -d 60s --latency -c 300 -R ${each_r} ${endpoint}
```

得到请求延时结果如下：

| KEY       | QPS      | TP90    | TP99     | TP999    | MEAN    | STDEV   |
|-----------|----------|---------|----------|----------|---------|---------|
| 10900.r_0 | 10878.76 | 35.29ms | 83.65ms  | 232.57ms | 14.79ms | 20.57ms |
| 10900.r_1 | 10894.40 | 37.98ms | 95.04ms  | 300.80ms | 16.62ms | 23.66ms |
| 10900.r_2 | 10893.97 | 37.92ms | 86.27ms  | 253.44ms | 16.47ms | 21.83ms |
| 11000.r_0 | 10992.26 | 38.46ms | 68.22ms  | 108.86ms | 16.37ms | 16.47ms |
| 11000.r_1 | 10988.97 | 39.39ms | 66.37ms  | 94.53ms  | 16.95ms | 16.33ms |
| 11000.r_2 | 10988.14 | 40.00ms | 73.54ms  | 140.93ms | 17.48ms | 18.04ms |
| 11100.r_0 | 11081.16 | 46.46ms | 121.47ms | 395.52ms | 22.75ms | 30.49ms |
| 11100.r_1 | 11089.59 | 48.74ms | 115.14ms | 349.70ms | 23.86ms | 28.25ms |
| 11100.r_2 | 11090.92 | 52.26ms | 245.50ms | 811.52ms | 28.82ms | 55.56ms |

# Round5

使用 [wrk2][wrk2] 设置选项 `-R` 值分别为：

- 1900
- 2000
- 2100

对 `api800` 组进行重复的 3 次压测。

```sh
wrk -t 16 -d 60s --latency -c 300 -R ${each_r} ${endpoint}
```

得到请求延时结果如下：

| KEY      | QPS     | TP90    | TP99    | TP999    | MEAN     | STDEV    |
|----------|---------|---------|---------|----------|----------|----------|
| 1900.r_0 | 1898.16 | 40.00ms | 68.61ms | 90.05ms  | 14.36ms  | 16.23ms  |
| 1900.r_1 | 1899.93 | 39.55ms | 69.50ms | 98.50ms  | 14.60ms  | 15.97ms  |
| 1900.r_2 | 1899.61 | 42.33ms | 77.69ms | 106.37ms | 15.94ms  | 17.42ms  |
| 2000.r_0 | 1999.98 | 43.46ms | 70.08ms | 91.97ms  | 15.92ms  | 17.25ms  |
| 2000.r_1 | 2000.46 | 52.58ms | 93.69ms | 174.21ms | 20.32ms  | 22.93ms  |
| 2000.r_2 | 1999.99 | 50.08ms | 83.20ms | 113.92ms | 18.90ms  | 20.27ms  |
| 2100.r_0 | 2059.21 | 1.79s   | 5.75s   | 7.88s    | 716.84ms | 1.09s    |
| 2100.r_1 | 2068.65 | 1.34s   | 4.48s   | 6.99s    | 535.81ms | 829.71ms |
| 2100.r_2 | 2060.51 | 1.49s   | 4.81s   | 7.57s    | 602.15ms | 925.10ms |

# Round X

作为对照，使用 [wrk2][wrk2] 设置选项 `-R` 值分别为：

- 100
- 1000
- 10000

对所有组进行压测。

```sh
wrk -t 16 -d 60s --latency -c 100 -R ${each_r} ${endpoint}
```

| KEY          | QPS     | TP90    | TP99    | TP999   | MEAN   | STDEV    |
|--------------|---------|---------|---------|---------|--------|----------|
| 100.direct   | 100.64  | 1.54ms  | 1.91ms  | 2.17ms  | 0.97ms | 410.39us |
| 100.api0     | 100.74  | 2.32ms  | 31.85ms | 51.97ms | 2.20ms | 4.35ms   |
| 100.api800   | 100.74  | 18.48ms | 34.88ms | 42.24ms | 7.77ms | 6.99ms   |
| 1000.direct  | 1000.23 | 1.49ms  | 1.95ms  | 2.20ms  | 0.86ms | 458.60us |
| 1000.api0    | 1000.23 | 2.33ms  | 3.02ms  | 21.26ms | 1.81ms | 1.19ms   |
| 1000.api800  | 998.72  | 13.99ms | 34.24ms | 43.33ms | 7.13ms | 6.39ms   |
| 10000.direct | 9998.38 | 1.50ms  | 2.06ms  | 6.43ms  | 0.96ms | 682.08us |
| 10000.api0   | 9998.22 | 2.76ms  | 22.40ms | 45.50ms | 2.57ms | 3.94ms   |
| 10000.api800 | 2040.38 | 43.78s  | 47.38s  | 48.23s  | 27.86s | 11.48s   |

# Conclusion

[Spring Cloud Gateway][gateway] 在匹配到前几位的路由时性能还可以接受，当需要经过较多次的路由匹配时，性能下降明显。

可能的优化点应该是针对不同的匹配类型使用前缀树等来提高匹配速度，而不能使用链式的匹配模型。

# Download

所有的测试相关的脚本和数据可供下载。

- [Round 0](round0.tar.xz)
- [Round 1](round1.tar.xz)
- [Round 2](round2.tar.xz)
- [Round 3](round3.tar.xz)
- [Round 4](round4.tar.xz)
- [Round 5](round5.tar.xz)
- [Round X](round6.tar.xz)

---

以上。

[spring]: https://spring.io/ "Spring"
[springboot]: https://spring.io/projects/spring-boot "Spring Boot"
[springcloud]: https://spring.io/projects/spring-cloud "Spring Cloud"
[gateway]: https://spring.io/projects/spring-cloud-gateway "Spring Cloud Gateway"
[wrk]: https://github.com/wg/wrk "Modern HTTP benchmarking tool"
[wrk2]: https://github.com/giltene/wrk2 "A constant throughput, correct latency recording variant of wrk"
