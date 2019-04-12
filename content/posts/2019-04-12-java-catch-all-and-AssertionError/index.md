+++
title = "Java try-catch and AssertionError"
description = "Java try-catch and AssertionError"
date = 2019-04-12T10:10:44+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Practise"]
tags = ["Java", "try-catch", "AssertionError"]
+++

Java 的异常处理机制是 try-catch-finally 。被调用的方法或代码块可以抛出异常（Throwable，含 Exception 和 Error）；被调用的方法或代码块被包含在 try 块中；catch 块中会根据具体的异常类型来实现相关的逻辑；finally 块中实现无论是否发生异常都需要的逻辑。

异常处理机制很简单；但是怎么设计异常处理方式是有一些坑的。

有一个线上服务在更新逻辑上线时，触发了一个错误处理异常的 BUG，现简单记录之。

<!-- more -->

# 背景

服务中有一段逻辑：从 DB 中通过 ID 查询任务的描述；任务描述是一个 JSON 格式的字符串，通过一个 type 字段来决定对应的任务实体类型；任务描述根据任务类型被反序列化为不同的任务实体。

相关的逻辑伪代码如下：

```
final Long taskId = 10000L;
final Integer taskType = findTaskTypeById(taskId);
final String taskStr = findTaskStringById(taskId);
switch (taskType) {
case 0:
    TaskV1 taskV1 = parseTaskAsV1(taskStr);
    // More logic on task v1
default:
    throw new AssertionError("Unexpected task type " + taskType);
}
```

当时考虑到任务的类型是静态的：在运行时，对于某个任务类型，服务或者支持或者不支持；如果添加新的任务类型支持，需要开发对应的处理逻辑并重新发布上线；如果在反序列化时遇到了不支持的任务类型，则说明应该是不可恢复的错误。

新的逻辑如下：

```
final Long taskId = 10000L;
final Integer taskType = findTaskTypeById(taskId);
final String taskStr = findTaskStringById(taskId);
switch (taskType) {
case 0:
    TaskV1 taskV1 = parseTaskAsV1(taskStr);
    // More logic on task v1
case 1:
    TaskV1 taskV2 = parseTaskAsV2(taskStr);
    // More logic on task v2
default:
    throw new AssertionError("Unexpected task type " + taskType);
}
```

对于不支持的任务类型，处理的方式是直接抛出 `java.lang.AssertionError` 。

# BUG

由于 `java.lang.AssertionError` 是 `java.lang.Error` 的子类型，如果没有显式地用 catch 块处理的话，会发生栈回滚导致逻辑中断。

线上服务在一个单独的线程里面执行以上逻辑代码；上述代码抛出 Error 之后外层没有捕获，导致线程退出，使得线程的逻辑中断。

临时通过添加 try-catch Throwable 的代码 fix 了此 BUG。

# 总结

## Error & Exception

Java 的异常机制的最顶层的类是 `java.lang.Throwable`，是 throw 和 catch 能操作的最基本的类型。Throwable 有两个子类，分别代表了不同的错误类型：`java.lang.Exception` 是程序运行期间内的错误，一般是逻辑错误；`java.lang.Error` 是程序运行期间 JVM 的错误，一般是系统异常。

按照 JDK 的文档说明，Exception 及其子类是应该被程序代码处理的错误；Error 及其子类不应该被外部代码处理。换言之，在代码的 catch 块中所应该捕获的最顶层错误类型是 Exception，不应该是 Throwable 或 Error。

如果要保持上述惯例，则需要抛出异常时保持相同的管理：不能在程序中抛出 Error 及其子类（更不用说 Throwable），throw 的对象应该是 Exception 或其子类。

理论上，服务代码中不应该与 Error 打交道，不能抛出 (throw) 也不能捕获 (catch) 。

JDK 中对 Exception 的子类型做了区分：受检异常和非受检异常。非受检异常是 `java.lang.RuntimeException` 及其子类，服务代码中可以抛出也可以捕获；受检异常是不属于非受检异常的 Exception 及其子类。非受检异常和受检异常的区别在于，抛出受检异常的方法或代码块被调用时，必须要捕获（catch 或显式向外传递），非受检异常则不然。

## AssertionError

AssertionError 是 `java.lang.Error` 的子类型，属于不能在代码中主动抛出和捕获的错误类型。

## enum 相关

对于任务类型类似的场景（可简化为一个 switch-case-default 结构）：根据一个枚举类型（或整型）来分别组织不同的代码逻辑，都需要处理不能识别的类型的逻辑。

```
switch (type) {
    case 0: /* do somthing here */ break;
    case 1: /* do somthing here */ break;
    /* More branches here */
    default:
        /* unknown type */ break;
}
```

如何处理不能识别的类型？

- 策略 1 是简单忽略，即在 default 块中使用空代码，不处理或直接退出。
- 策略 2 是抛出异常。

实际的策略需要分析场景具体制定。对于有确定分类的类型，我倾向于抛出异常。对于异常类型，我之前倾向于使用 AssertionError，现在则倾向于使用 RuntimeException。

## UncaughtExceptionHandler

对于没有捕获的异常，JVM 会调用线程的 uncaughtExceptionHandler 进行处理；如果需要对线程的未捕获的异常做一个最终的处理，需要设置线程的 uncaughtExceptionHandler 或静态的 defaultUncaughtExceptionHandler 。

```
public class Thread {
	public interface UncaughtExceptionHandler {
		void uncaughtException(Thread t, Throwable e);
	}

	public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
		defaultUncaughtExceptionHandler = eh;
	}

	public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
		uncaughtExceptionHandler = eh;
	}
}
```

## Result<T, E> in Rust

上述的 Error & Exception & RuntimeException 在使用时的区别大部分是语义上的，IDE 和编译器不能帮助开发者严格保持一定的模式。

作为对比，[Rust](https://www.rust-lang.org) 里的错误处理机制是基于 `Result<T, E>` 的。`Result<T, E>` 是作为方法的返回值存在的，方法被调用时必须要处理错误。Rust 中不存在由于错误没有捕获导致的 BUG （类比 Java 中未捕获 Error 或 RuntimeException）。
