+++
title = "一次由于滥用 OptionalLong.orElse 引发的 BUG"
description = "JDK 8 引入了 Optional 的概念，以解决 `null` 可能引发的可能的 BUG。新的类型 `java.util.Optional<T>`, `java.util.OptionalDouble`, `java.util.OptionalDouble` 和 `java.util.OptionalDouble` 能够有效避免 `null` 值的使用，强制使用者去处理 `absent & present` 的不同情况。"
date = 2019-03-31T15:31:07+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Practise"]
tags = ["bug", "OptionalLong", "Optional"]
+++

JDK 8 引入了 Optional 的概念，以解决 `null` 可能引发的可能的 BUG。新的类型 `java.util.Optional<T>`,
`java.util.OptionalDouble`, `java.util.OptionalDouble` 和 `java.util.OptionalDouble` 能够有效避免
`null` 值的使用，强制使用者去处理 `absent & present` 的不同情况。

<!-- more -->

比如，去数据库中查询一条指定名称的记录之 ID 的方法可能设计为：

```java
@Nullable
Long findByName(final String name);

// or

OptionalLong findByName(final String name);
```

前者设计为：如果能找到对应的 ID，则返回之；否则返回 `null`。后者设计为：如果能找到对应的记录，则返
回 `Optional.of(id)`；否则返回 `Optional.empty()`。

则被调用时，前者会倾向于以下代码：

```java
final Long id = service.findByName("name");
if (null != id) { // 1
    System.out.println("" + (id + 1L));
} else {
    System.out.println("-1");
}
```

后者会倾向于以下代码：

```java
final OptionalLong opt = service.findByName("name");
if (opt.isPresent()) { // 2
    System.out.println("" + (opt.getAsLong() + 1L));
} else {
    System.out.println("-1");
}
```

在实际项目中，`// 1` 处的代码很容易被忽略，从而引发 NPE 导致潜在的 BUG；而 `// 2` 处的代码基本上不会
被无意的忽略掉。

由于以上的优点，以及 Optional 系类型对于函数型用法的支持，我在绝大多数的代码中都会使用 Optional 系的
类型代替可能的 `null` 型值，上面的代码可以简化为：

```java
final OptionalLong opt = service.findByName("name");
System.out.println(opt.map(n -> n + 1L).map(String::valueOf).orElse("-1"));
```

可以看出，`OptionalLong#map` 与 `OptionalLong#orElse` 组合使用能够在编写代码的时候带来巨大的快感。

然而，滥用 `OptionalLong#orElse` 也可能会导致 BUG。

# 背景

在项目中有段查询的逻辑：查找多个队列中最大的一个任务 ID （select max(id) from the_table where
queue in (1, 2, 3);），数据库中存储的是 `int` 类型的队列编号，而查询的条件里面给出的是队列名称。队列
名称到队列编号的映射关系在数据库外部维护。

```java
interface TaskService {
    OptionalLong findByQueues(final List<String> queues);
}

interface QueueService {
    OptionalLong indexFor(final String name);

    long fallbackIndex();
}
```

服务的逻辑里面，一个任务必定属于一个队列；一个给定的队列名称可能没有对应的编号（该队列名可能不存在），所以通过名称查询编号的方法返回的是 `OptionalLong`；存在一个默认队列（fallback 队列）。

在 `findByQueues` 的实现中，会首先根据队列的名称和编号的映射关系确定需要查询的队列编号集合，然后生成对应的查询条件查找结果并返回。

```java
final Set<Long> queueIds = queues.stream()
    .map(name -> queueService.indexFor(name).orElse(queueService.fallbackIndex()))
    .collect(Collectors.toSet());
```

# BUG 分析

线上服务在运行中，有一个增加队列的需求。当增加了队列的映射之后，发现线上服务会不按照限定的队列集合获取任务，查询到的任务有时候会来自于默认队列。因为线上服务对于不同队列的任务可能会有不同的逻辑，获取到的默认队列的任务的处理就出现问题，服务开始报警。

分析代码发现，问题出在上述的查询时队列名称向编号转换的代码里。

正常情况下所有的队列名称都能找到一个对应的编号，`indexFor` 方法始终不会返回 `OptionalLong.empty()` 值。但是增加了一个队列名称与编号的映射关系配置之后，服务并不会立即知道该队列，而是会使用缓存的映射表，这使得查询新增加的队列时 `indexFor` 方法会返回 `OptionalLong.empty()`，进入了上述的 `orElse` 路径。在进行队列转换时使用 `orElse` 是错误的，是不符合业务逻辑的；但由于手滑，编码时不自觉地使用了 `orElse` 用法，使得对于不存在的队列名会返回默认的队列编号，导致了 BUG。

# Fix

问题代码修改为：

```java
final Set<Long> queueIds = queues.stream()
    .map(name -> queueService.indexFor(name))
    .filter(OptionalLong::isPresent)
    .map(OptionalLong::getAsLong)
    .collect(Collectors.toSet());
```

# 总结

根据分析，其实这一次的 BUG 并不是 `OptionalLong` 的锅，而是由与无脑使用 `OptionalLong` 导致的，是滥
用 `OptionalLong.orElse` 的后果。

可知，Optional 系的类型能帮助减少 `null` 的 BUG，但并不能帮助减少逻辑 BUG，因为这是业务逻辑的问题，
需要具体分析具体解决。

以上
