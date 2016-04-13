title: 'Learning Java Concurrency - Thread & Runnable'
date: 2016-04-11 10:04:31
updated: 2016-04-11 10:04:31
categories: "Learning Java Concurrency"
tags: [Java, Concurrency]

---

Java并发多线程的第一课，应该就是`Thread`了。顾名思义，`Thread`就是一个线程。线程是很底层的一个概念，在不同的操作系统上实现的模型和细节并不相同，甚至于可以说天差地别；但是总体来说，线程是比进程更细粒度的操作系统调度的单位，线程有自己的运行栈，但是同一个进程的线程共享方法区和堆区数据。

对于进程和线程的差别，有一种说法是：线程是操作系统调度的基本单位，而进程是操作系统分配运行资源的基本单位。

Java中对线程作了很好的封装：`Thread`类。`Thread`类的使用非常简单。

```java
public class ThreadCase {
    private static class EchoThread extends Thread {
        private final String word;

        public EchoThread(final String word) {
            this.word = word;
        }

        @Override
        public void run() {
            for (int i = 0; i < 1000; i++) {
                System.out.println(this.getName() + " echos " + word);
            }
        }
    }

    public static void main(String [] _args) {
        final Thread echo1 = new EchoThread("First");
        final Thread echo2 = new EchoThread("Second");

        System.out.println("Main thread started!");

        echo1.start();
        echo2.start();

        joinThread(echo1);
        joinThread(echo2);

        System.out.println("Main thread finished!");
    }

    private static void joinThread(final Thread th) {
        try {
            th.join();
        } catch (InterruptedException e) {
            System.out.println(th.getName() + " interrupted!");
        }
    }
}
```

可以很明显地发现：

- main负责启动其他线程，main本身也是一个线程
- 线程的调度是难以预料的，`echo1`和`echo2`的输出结果相互交错可以看出这一点
- 线程之间可以进行同步控制，使用`Thread.join()`方法可以强制等待另一个线程结束
- 自定义线程行为只需要重新实现`Thread.run()`方法即可
- 线程的启动入口是`Thread.start()`方法，不要直接运行`Thread.run()`方法
- `Thread`类是一个`class`（与`interface`相对应），意味着自定义线程类不能继承别的父类

<!-- More -->

# Thread

## API 列表

1. Thread()
2. Thread(Runnable target)
3. Thread(Runnable target, AccessControlContext acc)
4. Thread(ThreadGroup group, Runnable target) 
5. Thread(String name)
6. Thread(ThreadGroup group, String name)
7. Thread(Runnable target, String name)
8. Thread(ThreadGroup group, Runnable target, String name) 
9. Thread(ThreadGroup group, Runnable target, String name, long stackSize)
10. void start()
11. void interrupt() 
12. boolean isInterrupted() 
13. boolean isAlive()
14. State getState()
15. void run()
16. void join(long millis)
17. void join(long millis, int nanos)
18. void join() throws InterruptedException
19. static native void sleep(long millis) throws InterruptedException
20. static void sleep(long millis, int nanos)
21. void setDaemon(boolean on) 
22. boolean isDaemon()
23. static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler()
24. static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh)
25. UncaughtExceptionHandler getUncaughtExceptionHandler()
26. void setUncaughtExceptionHandler(UncaughtExceptionHandler eh)
27. static native Thread currentThread()

## 创建线程类对象

`Thread`类一共有9个公开的构造函数，咋一看很杂乱无章的，但是其实是有规律的。

考虑以下事实：

- 每个线程应该有一个名字，用来标识自己
- 每个线程可以有自己的行为，应该有一个用于自定义行为的类`Runnable`
- 线程应该可以分组，属于某个特定的`ThreadGroup`实例
- 线程应该有权限控制，用`AccessControlContext`来设置
- 每个线程有自己的栈，应该可以自定义栈的大小

这些线程相关的属性相互组合，并添加一些默认值，能够得到的构造函数绝对不止9个，哈！

实际上，以上所有的构造函数都是调用了内部私有的`init()`方法。

注意：`Thread`类是一个普通的Java类，所以构造器创建的对象引用是分配在当前的线程。该实例对应的线程还没有被创建。

## 启动线程

构造了一个线程对象之后，就可以启动该线程对象代表的线程开始执行任务。

`run()`方法里面是需要执行的任务。想要在该线程对象代表的线程中运行该任务，需要调用`start()`方法。

需要再次澄清一下，`Thread`对象是存在于创建它的线程中，调用`start()`方法会启动一个新的线程来运行`run()`里面的代码。如果直接调用`run()`方法，只是让当前线程去执行该任务，达不到预期的效果。

由于线程的运行需要操作系统进行调度，所以执行`start()`方法之后，什么时候执行线程是不可预期的。如果对线程运行的先后顺序有要求，请主动对线程进行同步控制。

## 线程的生命周期

当前线程创建了`Thread`对象，实际的线程还没有被创建，线程对象处于`NEW`状态。

根据`Thread`的api说明，一个`Thread`对象会有6种状态，同一时间该对象只可能处于一种状态。

```java
public enum State {
    NEW,			// 新建状态
    RUNNABLE, 		// 运行状态
    BLOCKED,		// 阻塞状态
    WAITING,		// 无条件的等待状态
    TIMED_WAITING, 	// 有条件的等待状态
    TERMINATED; 	// 终止状态
}
```

新建状态表明线程对象还未运行；运行状态表明线程对象正在运行一个线程；阻塞状态表明本线程对象在等待一个锁或同步器；无条件等待状态表明本线程对象在无限期地等待一个条件，比如调用了无过期时间的`Object.wait()`、`Thread.join()`等方法；有条件等待状态表明本线程对象在有条件地等待一个条件，是无条件等待状态的过期时间版本（timeout）；终止状态表明本线程对象代表的线程已经结束运行。

## 自定义线程行为

前面已经说到，线程执行的任务在`run()`方法里面。所以，自定义线程就需要自定义该方法。

```java
public void run() {
    if (target != null) {
        target.run();
    }
}
```

`run()`方法的默认实现是去执行构造器里面提供的`Runnable`对象的`run()`方法。

所以，有两种方法可以自定义线程。

- 继承`Thread`类，覆盖`run()`方法
- 创建一个`Runnable`对象并用之构造一个`Thread`对象

这两种方式没有本质上的区别，选择哪一种需要看具体的场合。

值得一提的是，`Thread`类实现了`Runnable`接口。

## 线程等待

线程的操作系统调度是不可预期的，所以在需要显式地控制线程运行的场合，需要使用额外的方法来达到目的。

比较高级的工具有`ReentrantLock`、`CountDownLatch`和`Semaphore`等，最简单的方法是调用`Thread.join()`方法。

假如有一个线程对象A，它创建了一个新的线程对象B，然后调用`B.start()`启动B线程。这时候A线程可以紧接着调用`B.join()`进入等待状态，知道B线程执行完毕才开始执行A线程。

`join()`方法有多个变体，区别在于是否提供超时时间。如果对线程对象的生命周期还有映像的话，提供了超时时间的`join()`方法会导致当前线程进入有条件等待状态，反之进入无条件等待状态。

## 后台线程

正常情况下，JVM会等待所有的线程都运行结束之后才会退出。通过设置线程为后台线程可以使得JVM不用等待。

当所有运行的线程都是后台线程时，JVM会结束运行。

`setDaemon()`方法用来设置线程为后台线程，`isDaemon()`方法可以用来检测是否为后台线程。

需要注意的是，`setDaemon()`方法需要在`start()`方法被调用之前调用才能生效。

## 异常处理

线程执行`run()`方法的过程中，有可能会遇到未捕获的异常。Java规范规定，JVM在执行线程过程中遇到了未捕获的异常，会主动去寻找该线程对象的未捕获异常处理器，如果没找到就去该线程对象的`ThreadGroup`对象里找，如果还是没有找到就去找`Thread`类的静态的未捕获异常处理器。

```java
public interface UncaughtExceptionHandler {
    void uncaughtException(Thread t, Throwable e);
}

private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;

public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) ;

public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler()

public UncaughtExceptionHandler getUncaughtExceptionHandler();
```

可以通过上面的方法自定义未捕获异常的处理行为。

# ThreadGroup

顾名思义，`ThreadGroup`类表征了一个线程组。

可以在创建线程对象的时候指定所属的线程组。可以通过线程组对象控制组内的线程。

`ThreadGroup`类比较有用的方法如下：

- void setDaemon(boolean daemon)
- boolean isDaemon()
- void setMaxPriority(int pri)
- int getMaxPriority() 
- void interrupt()
- void uncaughtException(Thread t, Throwable e) 

各个方法的含义不言自明，比较有意思的是`uncaughtException(Thread, Throwable)`方法。如果还记得线程对象的异常处理流程的话，就能明白为什么线程对象本身没有设置未捕获异常处理器时，会到所属的线程组对象里找。

因为`ThreadGroup`类实现了`Thread.UncaughtExceptionHandler`接口。`ThreadGroup`类的`uncaughtException(Thread, Throwable)`方法实现中，首先委托父线程组对象处理未捕获异常，如果没有父线程组，则跳到`Thread`类的静态的默认未捕获异常处理器进行处理。

# Runnable

`Runnable`接口很简单。

```java
public interface Runnable {
    public abstract void run();
}
```

实际使用中使用`Runnable`的方式经常是使用匿名内部类。

```java
public final class RunnableCase {
    public static void main(String [] _args) {
        final Thread echo1 =  new Thread(
            new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        System.out.println(Thread.currentThread().getName() + " echos first");
                    }
                }
            }
        );

        final Thread echo2 =  new Thread(
            new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        System.out.println(Thread.currentThread().getName() + " echos second");
                    }
                }
            }
        );

        System.out.println("Main thread started!");

        echo1.start();
        echo2.start();

        joinThread(echo1);
        joinThread(echo2);

        System.out.println("Main thread finished!");
    }

    private static void joinThread(final Thread th) {
        try {
            th.join();
        } catch (InterruptedException e) {
            System.out.println(th.getName() + " interrupted!");
        }
    }
}
```

# 代码下载

[RunnableCase.java](RunnableCase.java)
[ThreadCase.java](ThreadCase.java)
