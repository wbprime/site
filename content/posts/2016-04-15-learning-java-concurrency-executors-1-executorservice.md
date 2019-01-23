---
title: 'Learning Java Concurrency - Executors(1) ExecutorService'
date: 2016-04-15 16:34:20
updated: 2016-04-15 16:34:20
categories: ["Learning Java Concurrency"]
tags: [java, concurrency]

---

回过头来看`Thread`类，其实可以发现该类是对一件任务的抽象。通过将要完成的任务抽象出来用`Thread`或者`Runnable`来表示，然后委托给另外的线程来处理。`Thread`类在这里充当的是任务执行者的角色，表示一个执行任务的线程。

有时候，任务是不是由另一个线程执行并不重要，甚至于由几个线程共同完成我们也并不在意，我们只关心任务被完成了。任务这个概念已经被抽象的很好了：`Runnable`；接下来要抽象出任务执行者这个概念了。

Java提供了另一个接口`Executor`来真正地抽象任务执行者这个概念：线程池。怎么理解呢，看一下`Executor`接口的代码就好了。

<!-- More -->

# Executor

```java
public interface Executor {
    void execute(Runnable command);
}
```

是不是很简单？

任务执行者的角色有了，但是还是不够，我们还需要控制任务执行者的行为。

# ExecutorService

```java
public interface ExecutorService extends Executor {
    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    <T> Future<T> submit(Callable<T> task);

    <T> Future<T> submit(Runnable task, T result);

    Future<?> submit(Runnable task);

    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

    <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit
    ) throws InterruptedException;

    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

    <T> T invokeAny(
        Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit
    ) throws InterruptedException, ExecutionException, TimeoutException;
}
```

添加的`ExecutorService`接口扩展了`Executor`接口的功能，提供了对任务执行的更多的控制。`ExecutorService`从功能上才能真正称为线程池。

- 可以停止执行任务
- 可以判断任务的执行情况
- 可以提交执行有返回值的任务
- 增加了对批量任务的支持

## 线程池的关闭

线程池被创建之后，就可以向其提交任务了，直到该线程池被关闭。

`shutdown()`方法和`shutdownNow()`方法被用来关闭线程池。区别在于`shutdown()`方法只是让线程池停止接受新的任务；而`shutdownNow()`方法除了设置停止接受新任务之外，还将线程池的等待队列中的任务也取消掉。

具体地说，就是`shutdown()`方法会设置线程池为关闭状态，不再接受新的任务；原有的已经提交的任务不受影响，会继续执行直到结束；`shutdown()`方法调用后立即返回，并不会阻塞等待已提交任务的执行结束。`shutdownNow()`方法会设置线程池为关闭状态，不再接受新的任务；已经提交的任务但是未开始的任务会被取消；已经执行的任务会尝试去取消，但是不保证能取消成功；`shutdownNow()`方法的调用也是立即返回。

因为`shutdown()`方法并不会阻塞，所以还有另外一个方法来阻塞等待任务结束：`awaitTermination()`。该方法有一个过期时间的参数，如果在给定的时间内线程池的任务结束，返回成功，否则返回失败。注意，`awaitTermination()`方法本身并不试图关闭线程池，往往用来配合`shutdown()`方法使用。

```java
executorService.shutdownNow();
boolean re = executorService.awaitTermination(10, TimeUnit.SECONDS);
if (!re) {
    // handle error
}
```

`isShutdown()`方法和`isTerminated()`方法用来判断线程池是否已关闭和已结束。可以看出，线程池总是先关闭再结束。

## 有返回值的任务 submit()

除了可以向线程池提交`Runnable`类实例外，还可以提交`Callable`类实例。线程池也提供了用`Callable`类实例包装`Runnable`类实例的方法。

`Callable`类和`Future`类的使用参见 [FutureTask & Callable](/2016/04/13/learning-java-concurrency-futuretask-callable/) 。

## 批量任务 invokeAll & invokeAny()

线程池当然页提供了提交批量任务的方法。

`invokeAll()`方法用于向线程池提交多个异步任务，返回任务的对应`Future`类实例。

`invokeAny()`方法用于向线程池提交多个任务。与`invokeAll()`方法不同的是，该方法会阻塞直到批量任务的任一个执行结束；只要有一个任务结束，该方法就会返回该任务的结果，其他任务会被结束。

# ScheduledExecutorService

Java通过`ScheduledExecutorService`接口扩展了`ExecutorService`接口的功能，增加了延迟执行任务的功能。

```java
public interface ScheduledExecutorService extends ExecutorService {
    public ScheduledFuture<?> schedule(
        Runnable command, long delay, TimeUnit unit
    );

    public <V> ScheduledFuture<V> schedule(
        Callable<V> callable, long delay, TimeUnit unit
    );

    public ScheduledFuture<?> scheduleAtFixedRate(
        Runnable command, long initialDelay, long period, TimeUnit unit
    );

    public ScheduledFuture<?> scheduleWithFixedDelay(
        Runnable command, long initialDelay, long delay, TimeUnit unit
    );
}
```

`ScheduledExecutorService`接口内各方法的含义不言自明。需要注意的是`ScheduledFuture`接口。

```java
public interface ScheduledFuture<V> extends Delayed, Future<V> {
}

public interface Delayed extends Comparable<Delayed> {
    long getDelay(TimeUnit unit);
}
```

可以发现`ScheduledFuture`接口是`Future`接口的扩展，增加了一个返回延迟时间的参数。

Java线程池的几个重要接口就介绍到这里，具体的实现下次再说吧！
