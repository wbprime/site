---
title : "Learning Java Concurrency - synchronized"
date : 2016-04-01T16:39:24+08:00
updated: 2016-04-01 16:39:24
categories : ["Learning Java Concurrency"]
tags : ["java", "concurrency"]

---

synchronized，同步控制器，是Java原生提供的多线程同步控制的工具，是Java语法的一部分。

synchronized在语义上等同于一个独占锁。synchronized可以用来修饰一个方法，标识该方法是可同步的；也可以用来修饰语句块，标识该语句块是同步的。在代码经过编译之后，JVM会在方法或者语句块的前后插入`monitorenter`和`monitorexit`的虚拟机指令，这两条指令又会隐式地调用`lock`原语。

synchronized可以使用在普通方法里，也可以使用在静态方法里。

```java
class Synchronized {

    public synchronized void method1(final String val) {
        System.out.println("1: Begin add " + val);
        System.out.println("1: Finish add " + val);
    }
    
    public synchronized static void method2(final String val) {
        System.out.println("2: Begin add " + val);
        System.out.println("2: Finish add " + val);
    }

    public void method3(final String val) {
        synchronized(this) {
            System.out.println("3: Begin add " + val);
            System.out.println("3: Finish add " + val);
        }
    }

    public static void method4(final String val) {
        synchronized(Synchronized.class) {
            System.out.println("4: Begin add " + val);
            System.out.println("4: Finish add " + val);
        }
    }
}
```

<!-- More -->

# 单例模式

synchronized的学习最好结合单例模式来进行。

## Version 0

最简单的单例模式，可以表示如下：

```java
class Singleton0 {
     
    private static Singleton0 instance_ = new Singleton0(); // init while class loaded
     
    private Singleton0(){}
     
    public static Singleton0 instance() {
        return instance_;
    }
}
```

本单例实现会在Singleton0类加载的时候实例化。使构造器私有是为了保证实例单一化，不允许外部构造新实例。如果要使用Java的序列化机制，可能需要额外的代码保证实例的唯一性。如果有可能用上反射构造对象，最简单的应对方法是在构造器里面抛出异常。

## Version 1 (wrong)

如果想延迟初始化，可以使用下面的方案。

```java
class Singleton1 {

    private static Singleton1 instance_;

    private Singleton1() {}

    public static Singleton1 instance() { // lazy inited but with multi thread problem
        if (null == instance_) {
            instance_ = new Singleton1();
        }

        return instance_;
    }
}
```

本实现的问题在于多线程同步导致的潜在的多次实例化。可以使用synchronized关键字来解决这个问题，因为synchronized可以保证同一时间只有一个线程进行操作，其他的线程被阻塞。

## Version 2

```java
class Singleton2 {

    private static Singleton2 instance_;

    private Singleton2() {}

    public synchronized static Singleton2 instance() {// lazy inited
        if (null == instance_) {
            instance_ = new Singleton2();
        }

        return instance_;
    }
}
```

## Version 3 (wrong)

然而上面的版本还可以进行改进。可以只在实例化的时候才加以同步控制，如果已经实例化了，就不需要同步控制代码。

```java
class Singleton3 {

    private static Singleton3 instance_;

    private Singleton3() {}

    public static Singleton3 instance() {
        if (null == instance_) {
            synchronized(Singleton3.class) { // multi thread may conflict here
                instance_ = new Singleton3();
            }
        }

        return instance_;
    }
}
```

但是这种方式导致了新的问题：如果在两个线程都通过了为非空的条件判断时，一个线程（线程A）已经获取了同步器并创建了对象实例，另一个线程（线程B）则被阻塞以获取同步器，则线程B获取到同步器之后还是会去创建对象。

## Version 4 (wrong)

这个时候需要使用到双重检锁机制：在获取同步器之前和之后都需要进行条件判断。

```java
class Singleton4 {

    private static Singleton4 instance_; // variable visibility

    private Singleton4() {}

    public static Singleton4 instance() {
        if (null == instance_) {
            synchronized (Singleton4.class) {
                if (null == instance_) {
                    instance_ = new Singleton4();
                }
            }
        }

        return instance_;
    }
}
```

这样看起来就可以了。但是还是会有一个问题：JVM无法保证变量instance_在多个线程间的可见性。具体的来说，就是线程A和线程B同时通过了第一次条件判断，然后线程A获取到了同步器并创建实例然后给instance_变量赋值；现在线程B拿到了同步器，开始做第二次条件测试，测试变量instance_的值是否非空。从时间顺序上说，线程A给变量赋完值之后，然后线程B再去取变量的值做判断，此时线程B拿到的肯定是非空的。然而，JVM并不保证线程B拿到的变量值是非空的。

~~众所周知的，处理器的执行指令的速度高出主内存读写速度好几个量级。为了防止处理器的指令执行经常被主内存读写操作所堵塞，JVM会对指令进行优化，不是所有的对变量的赋值操作都会立即写入到主内存；很合理的假设，JVM只要保证改变量在下一个读取之前被改写就可以了，这样既保证了程序的正确，也保证了JVM可以对指令进行重排序以优化执行效率。但问题是，在执行之前，指令的优化已经做完了；而多线程操作是执行期间的事情；万一线程B获取变量值的指令在线程A赋值变量指令之前执行怎么办？~~ 内存的可见性可以由同步器来保证，根据[Double-checked locking](https://en.wikipedia.org/wiki/Double_checked_locking_pattern)的说明，本实现的问题在于：实例化执行构造器的有可能是耗时操作，线程A拿到同步器然后执行构造器代码，JVM有可能已经对变量进行了赋值；线程B在第一次条件判断时可能认为对象已经初始化（实际上还没有初始化完成，或者线程A被挂起），就会直接使用部分初始化的对象。

## Version 5

双重检锁的推荐实现是使用synchronized来保证多线程同步，使用volatile来保证变量的多线程可见性：

```java
class Singleton5 {

    private static volatile Singleton5 instance_; // Add volatile to keep variable memory visibility

    private Singleton5() {}

    public static Singleton5 instance() {
        if (null == instance_) {
            synchronized (Singleton5.class) {
                if (null == instance_) {
                    instance_ = new Singleton5();
                }
            }
        }

        return instance_;
    }
}
```

## Other versions

到这里，基本上可以梳理清楚synchronized关键字的用法。

事实上，随着Java版本的提升，目前公认的比较好的Java单例模式实现是使用enum。

```java
enum Singleton6 {
    INSTANCE;

    Singleton6() {}
}
```

如果不想使用enum关键字，也可以使用[Initialization-on-demand holder idiom](https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom)。

```java
class Singleton7 {

    private static class SingletonHolder {
        private static final Singleton7 INSTANCE = new Singleton7();
    }

    private Singleton7() {}

    public static Singleton7 instance() {
        return SingletonHolder.INSTANCE;
    }
}
```

当然也可以使用双检锁版本，或者其改进版本：

```java
class Singleton8 {
    private static volatile Singleton8 instance_;

    private Singleton8() {}

    public Singleton8 instance() {
        Singleton8 var = instance_;
        if (null == var) {
            synchronized (Singleton8.class) {
                var = instance_;
                if (null == var) {
                    instance_ = var = new Singleton8();
                }
            }
        }

        return var;
    }
}
```

局部变量`var`的引入是为了效率考虑，减少对`volatile`变量的读取次数。按照[Double-checked locking](https://en.wikipedia.org/wiki/Double-checked_locking)的说法，可以提升25%的效率。

# 同步器的释放

同一时间同一个对象只允许一个线程持有同步器，其他请求同步器的线程会被阻塞。当前线程超出了同步器约束的作用域（方法体或者代码块）或者当前线程调用了同一个对象的`wait()`方法。

具体来说，就是：

1. 当前线程正常退出作用域。
2. 当前线程作用域内出现了未处理的Error或者Exception。
3. 当前线程在作用域内执行了同步器锁定对象的`wait()`方法。

特别的，以下两种情况可能会有迷惑性，当前执行线程是不会释放同步器的。

1. 当前线程在作用域内调用`Thread.sleep()`或`Thread.yield()`暂停执行。
2. 当前线程在作用域内时，其他线程调用了当前线程的`suspend()`方法。

# 与 ReentrantLock 的比较

synchronized和java.util.concurrency包中的ReentrantLock在作用和用法上具有很高的相似性。前者是Java语法层面的多线程同步器，后者是API层面的互斥锁。

从功能上讲，ReentrantLock是synchronized的超集，增加了公平性、可中断性和条件控制。

1. 公平性是指当锁/同步器被释放时，等待的线程是否需要按照FIFO的顺序来获得锁/同步器。ReentrantLock在非公平锁之外还提供了非公平锁。

2. 可中断性是指线程在被阻塞的时候，是否可以选择放弃等待，进行别的工作。RenentrantLock提供了设置超时时间获取锁的方法来提供可中断。

3. ReentrantLock提供了获得Condition的方法，可以用来进行多线程通信。

从性能上讲，synchronized是Java锁，ReentrantLock是使用的底层系统实现，所以ReentrantLock会比synchronized具有更高的性能。根据《深入理解Java虚拟机》（周志明 著）的实验，JDK 1.6以后，JVM实现对synchronized的编译进行了大幅度的优化，两者的性能差别不大；在可以遇见到的未来，JVM的实现团队们肯定会逐步地优化作为Java语法的synchronized的编译和执行，而ReentrantLock作为Java附带的API库，优化的空间不是很大，所以二者的性能差异不应该成为进行选择的主要依据。

synchronized具有比ReentrantLock更底层的语法和更简单的使用；ReentrantLock具有比synchronized更优秀的性能和更复杂的功能。在实际的使用中，主要应根据使用场景来选择使用的工具；在synchronized可以解决问题的场合，不必要为了追求所谓的一点点性能就去使用ReentrantLock或者其他JUC的API。
