title: 'Learning Java Concurrency - ReentrantLock & Condition'
date: 2016-04-07 09:47:10
updated: 2016-04-07 09:47:10
categories: "Learning Java Concurrency"
tags: [Java, Concurrency]

---

`ReentrantLock`是`synchronized`的高阶版本，用来控制多线程同步。`ReentrantLock`是一种独占锁，同一时间只能有一个线程使用一把锁，其他请求加锁的线程都会被阻塞。除了控制多线程同步之外，`ReentrantLock`还提供了`Condition`用来进行多线程通讯。`Condition`是`Object`类的方法`wait & notify`的替代版本，可以用等待/通知模式来有效控制多线程对共享资源的访问。

仿[`synchronized`](/2016/04/01/java-concurrency-synchronized/)，用`ReentrantLock`实现单例模式的代码如下：

```
private static class Singleton {
    private static volatile Singleton INSTANCE;
    private static ReentrantLock lock = new ReentrantLock();

    private Singleton() {}

    public static Singleton instance() {
        Singleton var = INSTANCE;
        if (null == var) {
            lock.lock();
            try {
                var = INSTANCE;
                if (null == var) {
                    INSTANCE = var = new Singleton();
                }
            } finally {
                lock.unlock();
            }
        }

        return var;
    }
}
```

仿[`wait & notify`](/2016/04/06/learning-java-concurrency-wait-notify/)，用`Condition`来实现父子通知汇款的代码如下：

```
private static class DepositAccount {
    private int money;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition cond = lock.newCondition();

    public DepositAccount() {
        this.money = 0;
    }

    public void withdraw(final int val) {
        lock.lock();
        try {
            while (money < val) {
                try {
                    cond.await(); // 钱不够，等一会儿
                } catch (InterruptedException e) {
                    // do nothing here
                }
            }

            money -= val;
        } finally {
            lock.unlock();
        }
    }

    public void deposite(final int val) {
        lock.lock();
        try {
            money += val;

            cond.signalAll(); // 存完钱周知一下
        } finally {
            lock.unlock();
        }
    }
}
```

<!-- More -->

# ReentrantLock's API

1. ReentrantLock() & ReentrantLock(boolean fair)
2. void lock()
3. void lockInterruptibly() throws InterruptedException
4. boolean tryLock()
5. boolean tryLock(long timeout, TimeUnit unit)
6. void unlock()
7. boolean isHelpByCurrentThread()
8. boolean isLocked()
9. boolean isFair()
10. Condition newCondition()
11. boolean hasWaiters(Condition cond)

`ReentrantLock`的实现是委托给内部静态类`FairSync`和`NonfairSync`，这两个类又继承自`AbstractQueuedSynchronizer`。

```
abstract static class Sync extends AbstractQueuedSynchronizer {
    ...
}

static final class NonfairSync extends Sync {
    ...
}

static final class FairSync extends Sync {
    ...
}
```

## 公平锁与非公平锁

`FairSync`和`NonfairSync`即所谓的公平锁和非公平锁。对比一下两个锁版本对于`lock`方法的实现，就能明白公平和非公平的区别在哪里了。

```
final void lock() { // NonfairSync
    if (compareAndSetState(0, 1))
        setExclusiveOwnerThread(Thread.currentThread());
    else
        acquire(1);
}

final void lock() { // FairSync
    acquire(1);
}
```

可以看到，公平锁直接让当前线程加入等待队列；而非公平锁首先试图去获取锁，如果当前有线程恰好释放了锁，就可以插队获取到锁，如果获取失败，还是会加入等待队列。也就是说，非公平锁会在调用lock的时候去尝试获取锁。如果这时候能够获取锁，就直接获取到锁，这样可以减少线程挂起和恢复的性能开销；缺点就是有可能在等待队列中的线程有可能永远拿不到锁。

`ReentrantLock`在构造器中可以指定使用公平锁还是非公平锁策略，默认是非公平锁。

## `lock`

`lock()`和`lockInterruptibly()`用于获取锁。调用线程试图去获取一个锁，如果锁已经被当前线程占用，则锁的计数器加1；如果锁被其他线程占用，则当前线程被阻塞，进入等待队列等待锁被释放。

两者的区别在于等待线程在等待的过程中是否可以被中断。

## `tryLock`

如果调用线程不想被阻塞，可以使用`lock`的异步版本`tryLock`。`tryLock`会试图去获取锁，如果获取成功了，就返回成功；如果失败了，就会返回失败。获取失败，可以设定一个等待时间，自旋等待。

## `unlock`

如果调用线程持有了目标锁，当前线程调用`unlock`会试图释放锁定。准确的说是让目标锁的计数器减1,如果目标锁的计数器为0,则锁被释放。

如果调用线程没有持有目标锁，会导致`IllegalMonitorStateException`异常。

## `newCondition`

创建一个与目标锁相关联的`Condition`对象。

# Condition's API

1. void await() throws InterruptedException
2. void awaitUninterruptibly()
3. long awaitNanos(long nanos) throws InterruptedException
4. boolean await(long timeout, TimeUnit unit) throws InterruptedException
5. boolean awaitUnit(Date dt) throws InterruptedException
6. void signal()
7. void signalAll()

`Condition`的作用和用法可以参考`Object`类的`wait & notify`族。

`ReentrantLock`返回的`Condition`对象与一个互斥锁相关联。`Condition`对象本身维护一个线程的等待队列，`await`会将调用线程放到等待队列中；`signal`会将等待线程中的所有线程放到关联的`ReentrantLock`对象的等待队列中。这样线程的挂起和唤醒工作就由`ReentrantLock`对象完成。

## `await`

同`Object`的`wait`族，估计就是因为已经有了`wait`，所以新的API才被命名为`await`。

调用线程会被挂起，一直等到条件满足被其他线程用`signal`来唤醒。

等待可以设置允不允许中断，也可以设置等待时间间隔。

调用线程被挂起之后会释放持有的锁，加入到`Condition`对象的等待队列中去。

## `signal`

同`Object`的`notify`族。

`signal`唤醒线程的顺序是未定义的，不同的JVM实现会有不同的策略，可以是等待时间最长的最先被唤醒。

`signal`会将需要唤醒的线程从`Condition`自己的等待队列移动到绑定的`ReentrantLock`对象的等待序列，按照`ReentrantLock`的规则去获取锁。

# 代码下载

[ConditionCase.java](ConditionCase.java)
[ReentrantLockCase.java](ReentrantLockCase.java)
