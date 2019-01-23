---
title: 'Learning Java Concurrency - FutureTask & Callable'
date: 2016-04-13 10:17:57
updated: 2016-04-13 10:17:57
categories: ["Learning Java Concurrency"]
tags: ["java", "concurrency"]

---

Java的`java.util.concurrent`包里面提供了多线程并发和同步的支持。

最开始的时候，多线程被认为是执行任务的手段，也就是说，我启动一个新线程来执行代码，至于资源共享、线程同步等可以用锁、同步器等解决。所以`Thread`类和`Rannable`接口暴露了一个`void run()`方法来提供自定义行为。

但慢慢地，人们开始发现如果自定义的线程是计算结果的，那我怎么来拿到计算之后的结果呢？另外，我得知道什么时候计算结束了，如果计算的时间太长了，我也想能够终止计算线程的执行。这些需求其实用基本的多线程工具也可以实现，但是过程比较繁琐；而且这些需求有一段时间又特别普遍。终于，jdk 1.5开始引入了`Callable`接口和`Future`接口，用于支援有返回值的计算任务。

<!-- More -->

# Callable

`Callable`接口和`Runnable`接口很接近，有多接近呢？对比一下代码就知道了。

这是`Runnable`的代码：

```java
public interface Runnable {
    public abstract void run();
}
```

这是`Callable`的接口：

```java
public interface Callable<V> {
    V call() throws Exception;
}
```

可以看到，`Callable`接口比`Runnable`接口多了两个功能：

- 方法可以有返回值
- 方法可以抛出Checked异常

添加返回值就是为了支援计算类的任务；可以抛出Checked异常则是为了完善错误处理机制。

Thread/Runnable 机制是不允许抛出Checked异常的；如果抛出了Unchecked异常，会自动去寻找线程的异常处理器进行处理，参见 [Learning Java Concurrency - Thread & Runnable](/2016/04/11/learning-java-concurrency-thread-runnable/) 里面关于线程异常处理的部分。

# Future

`Future`接口封装了异步计算的结果，用于在异步任务完成之后获取结果。

接口的方法如下：

```java
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();

    boolean isDone();

    V get() throws InterruptedException, ExecutionException;

    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```

`get()`方法用于获取异步任务的结果。如果异步任务还没有结束，该方法的调用会被阻塞，一直到异步任务运行结束（正常结束或者抛出异常），或者异步任务线程被中断。如果异步任务线程被中断，该方法抛出`InterruptedException`异常；如果异步任务执行抛出异常，该方法抛出`ExecutionException`异常。

`get(long, TimeUnit)`方法同上，添加了过期时间的设置：当在指定时间内异步任务没有执行完毕，该方法抛出`TimeoutException`异常。

除了获取异步任务计算结果的方法之外，`Future`接口还提供了判断异步任务状态的方法和取消任务的方法。

`isDone()`方法返回异步任务是否结束。异步任务正常、异常结束，异步任务被手动取消，都被认为是结束（Done）。

`isCancelled()`方法返回异步任务是否被取消。

`cancle(boolean mayInterruptIfRunning)`方法用于取消异步任务。顾名思义，如果异步任务已经运行完毕了，当然没法再取消了，该方法会返回失败；同理，一个已经被取消了的异步任务，该方法也会返回失败。对于正在执行的任务，该方法会根据`mayInterruptIfRunning`参数的值去判断是否需要中断异步线程。对于未开始执行的任务，调用该方法之后，任务永远不会再运行。

# Callable & Future 的使用

因为`Thread`类并不支持直接使用`Callable`接口，所以JUC在`ExecutorService`框架中提供了使用`Callable`和`Future`的入口。

```java
<T> Future<T> submit(Callable<T> task);
```

使用方法很简单：自定义一个`Callable`接口的实现，然后提交给`ExecutorService`实例，然后调用`Future.get()`等待异步任务运行结束并获取结果就好了。当然，如果任务运行期间等不及的话，也可以取消任务。

```java
public class CallableFutureCase {
    private static class SumupTask implements Callable<Long> {
        private final int val_;

        public SumupTask(final int val) {
            this.val_ = val;
        }

        public Long call() throws Exception {
            long result = 0;
            for (int i = 1; i <= val_; i++) {
                result = result + i;
            }

            return Long.valueOf(result);
        }
    }

    public static void main(String [] _args) {
        final ExecutorService executor = Executors.newCachedThreadPool();

        final Future<Long> result = executor.submit(new SumupTask(10000));

        System.out.println("Main thread started!");

        try {
            final Long longResult = result.get();
            System.out.println("Result is " + longResult);
        } catch (InterruptedException e) {
            // do nothing
        } catch (ExecutionException e) {
            // do nothing
        }

        System.out.println("Main thread finished!");

        executor.shutdown();
    }
}
```

# FutureTask

然而，既然已经设计了`Callable`接口和`Future`接口，却不能直接与`Thread`类一起使用，感觉有点心里不是滋味。所以有人设计了一个`FutureTask`类。

```java
public class FutureTask<V> implements RunnableFuture<V> {}

public interface RunnableFuture<V> extends Runnable, Future<V> {}
```

因为`FutureTask`实现了`Runnable`接口，所以可以与`Thread`类配合使用；又因为`FutureTask`实现了`Future`接口，所以可以控制任务的状态以及获取任务执行结果。

`FutureTask`类有两个构造方法：

- public FutureTask(Callable<V> callable)
- public FutureTask(Runnable runnable, V result)

可以通过实例来看一下`FutureTask`类实际使用。

## FutureTask by Callable

```java
public class FutureTaskCase {
    private static class CallableMax implements Callable<Integer> {
        private final List<Integer> list_;

        public CallableMax(final List<Integer> list) {
            this.list_ = list;
        }

        public Integer call() throws Exception {
            if (null != list_ && !list_.isEmpty()) {
                int result = list_.get(0).intValue();
                for (final Integer val: list_) {
                    if (result < val.intValue()) {
                        result = val.intValue();
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
                return Integer.valueOf(result);
            } else {
                return null;
            }
        }
    }

    public static void main(String [] _args) {
        final List<Integer> list = ImmutableList.of(
            1, 199, 6, 3, 56, 299, 199, 28, 10, 234
        );

        final FutureTask<Integer> task1 = new FutureTask(new CallableMax(list));

        final Thread task1Thread = new Thread(task1);
        task1Thread.start();

        System.out.println("Main thread started!");

        try {
            final Integer re1 = task1.get();
            System.out.println("Result is " + re1);
        } catch (InterruptedException e) {
            // do nothing
        } catch (ExecutionException e) {
            // do nothing
        }

        System.out.println("Main thread finished!");
    }
}
```

使用`Callable`实例去构造`FutureTask`类实例时，因为结果是`call()`方法直接返回的，所以用法比较简单。但是使用`Runnable`实例去构造`FutureTask`实例时，因为`run()`方法不能返回结果，所以要提供一个共享变量用来作为容器接受`run()`方法处理的结果，同时传递结果给调用线程。注意，使用`Runnable`方式时，共享的变量需要是一个可变的对象，不可遍对象类如`String`、`Integer`等需要提供一个包装类。

## FutureTask by Runnable

```java
public class FutureTaskCase {
    private static class ValueHolder {
        Integer value;
    }

    private static class RunnableMax implements Runnable {
        private final List<Integer> list_;
        private final ValueHolder holder;

        public RunnableMax(
            final List<Integer> list, final ValueHolder valueHolder
        ) {
            this.list_ = list;
            this.holder = valueHolder;
        }

        public void run() {
            if (null == holder) return ;

            if (null != list_ && !list_.isEmpty()) {
                int result = list_.get(0).intValue();
                for (final Integer val : list_) {
                    if (result < val.intValue()) {
                        result = val.intValue();
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }

                holder.value = Integer.valueOf(result);
            } else {
                holder.value = null;
            }
        }
    }

    public static void main(String [] _args) {
        final List<Integer> list = ImmutableList.of(
            1, 199, 6, 3, 56, 299, 199, 28, 10, 234
        );

        final ValueHolder holder = new ValueHolder();
        final FutureTask<ValueHolder> task2 = new FutureTask(
            new RunnableMax(list, holder),
            holder
        );

        final Thread task2Thread = new Thread(task2);
        task2Thread.start();

        System.out.println("Main thread started!");

        try {
            final ValueHolder re2 = task2.get();
            System.out.println("Result is " + re2.value);
        } catch (InterruptedException e) {
            // do nothing
        } catch (ExecutionException e) {
            // do nothing
        }

        System.out.println("Main thread finished!");
    }
}
```

代码中使用了`ValueHolder`来保存`run()`计算的结果并传递给`Future.get()`返回调用线程。

为什么要使用这种方式呢？我们来看一下`FutureTask`的对应代码。

```java
public FutureTask(Runnable runnable, V result) {
    this.callable = Executors.callable(runnable, result);
    this.state = NEW;       // ensure visibility of callable
}
```

该构造器调用了`Executors.callable()`方法。

```java
public static <T> Callable<T> callable(Runnable task, T result) {
    if (task == null)
        throw new NullPointerException();
    return new RunnableAdapter<T>(task, result);
}
```

`Executors.callable()`方法用一个`RunnableAdapter`类对传入的参数进行了包装。

```java
static final class RunnableAdapter<T> implements Callable<T> {
    final Runnable task;
    final T result;
    RunnableAdapter(Runnable task, T result) {
        this.task = task;
        this.result = result;
    }
    public T call() {
        task.run();
        return result;
    }
}
```

在包装类`RunnableAdapter`内部简单地保存了`Runnable`实例和目标类型对象。可以理解为，`T`类型的对象负责记录任务结果，而`Runnable`实例的`run()`方法在执行过程中修改该对象的值。如果`T`是一个不可变对象，则`run()`方法中的修改传递不到外部来。

既然这么复杂，那么还是尽量使用带`Callable`构造器的版本吧！

# 代码下载

[CallableFutureCase.java](CallableFutureCase.java)
[FutureTaskCase.java](FutureTaskCase.java)
