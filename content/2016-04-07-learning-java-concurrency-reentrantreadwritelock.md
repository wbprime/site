+++
title = "Learning Java Concurrency - ReentrantReadWriteLock"
description = "Learning Java Concurrency - ReentrantReadWriteLock"
date = 2016-04-07T21:54:38+08:00
draft = false
[taxonomies]
categories =  ["Learning Java Concurrency"]
tags = ["java", "concurrency"]
+++

`ReentrantLock`是互斥锁，对于要保护的资源，同一时间只能有一个线程进行访问。所谓的访问，就是读和写。但是在实际中，往往是读操作对互斥性的要求远远低于写操作。

考虑一个共享资源，比如一个List对象，可能会有多个线程对其进行读写。

下面是使用`ReentrantLock`实现的一个版本。

```java
private static class ExclusiveLockStack {
    private final List<String> list = new ArrayList<String>();
    private final ReentrantLock lock = new ReentrantLock();

    public void push(final String val) {
        if (null == val)    return;

        lock.lock();
        try {
            list.add(val);
        } finally {
            lock.unlock();
        }
    }

    public String last() {
        lock.lock();
        String str = null;
        try {
            final int lastIdx = list.size() - 1;
            if (lastIdx >= 0) {
                str = list.get(lastIdx);
            }
        } finally {
            lock.unlock();
        }
        return str;
    }
}
```

下面是使用`ReentrantReadWriteLock`实现的一个版本。

```java
ReadWrittatic class ReadWriteLockStack {
    private final List<String>           list  = new ArrayList<String>();
    private final ReentrantReadWriteLock lock  = new ReentrantReadWriteLock();
    private final Lock                   rLock = lock.readLock();
    private final Lock                   wLock = lock.writeLock();

    public void push(final String val) {
        if (null == val)    return;

        wLock.lock();
        try {
            list.add(val);
        } finally {
            wLock.unlock();
        }
    }

    public String last() {
        rLock.lock();
        String str = null;
        try {
            final int lastIdx = list.size() - 1;
            if (lastIdx >= 0) {
                str = list.get(lastIdx);
            }
        } finally {
            rLock.unlock();
        }
        return str;
    }
}
```

<!-- more -->

`ReentrantReadWriteLock`虽然看起来像一个Lock，但却并不是真正的Lock，没有去实现`java.util.concurrent.locks.Lock`接口，而是去实现了一个`ReadWriteLock`的接口。

# 接口概览

对比一下`ReentrantLock`和`ReentrantReadWriteLock`的实现的接口就能知道二者有什么区别了。

`ReentrantLock`的根底如下：

```java
public class ReentrantLock implements Lock {}

public interface Lock {

    boolean tryLock();

    void lockInterruptibly() throws InterruptedException;

    boolean tryLock();

    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    void unlock();

    Condition newCondition();
}
```

`ReentrantReadWriteLock`的跟脚如下：

```java
public class ReentrantReadWriteLock implements ReadWriteLock {}

public interface ReadWriteLock {
    Lock readLock();

    Lock writeLock();
}
```

可见，一个真正的锁需要提供`lock`和`unlock`等方法。`ReentrantLock`直接实现了`Lock`接口，而`ReentrantReadWriteLock`通过`readLock()`和`writeLock()`方法来返回`Lock`供使用。

`ReentrantReadWriteLock`类提供的方法如下：

1. ReentrantReadWriteLock() & ReentrantReadWriteLock(boolean fair)
2. Lock readLock()
3. Lock writeLock()
4. boolean isFair()
5. int getReadLockCount()
6. boolean isWriteLocker()

# 共享锁与互斥锁

`ReentrantReadWriteLock`向外界提供了一个读锁和写锁。

读锁是一种共享锁，同一时间多个线程都可以持有该锁。

写锁是一种互斥锁，同一时间只能有一个线程持有该锁。写锁上有一个计数器，只有当计数器为0时，新的线程才能去持有该锁；如果计数器不为0,但是持有锁的线程正是当前线程，则计数器加1,表示锁需要被unlock两次才能真正释放。

读锁和写锁在一起工作时，相互之间也会有影响。如果读锁已经被持有，则所有请求写锁的线程会被阻塞。如果写锁已经被持有，则任何新的读锁和写锁的请求线程都会被阻塞。

```java
public static class ReadLock implements Lock {
    private final Sync sync;

    public void lock() {
        sync.acquireShared(1);
    }

    public void lockInterruptibly() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public  void unlock() {
        sync.releaseShared(1);
    }
}

public static class WriteLock implements Lock {
    private final Sync sync;

    public void lock() {
        sync.acquire(1);
    }

    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    public void unlock() {
        sync.release(1);
    }
}
```

`ReadLock`和`WriteLock`持有同一个`Sync`的对象实例，而`Sync`继承自`AbstractQueuedSynchronizer`。二者的区别在于各自的`lock & unlock`操作分别调用的是`AbstractQueuedSynchronizer`的共享和独占方法。

所以，读锁和写锁，实际上是同一个锁。

# 真正的锁

`Sync`中主要的方法如下：

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    abstract boolean readerShouldBlock();

    abstract boolean writerShouldBlock();
}
```

`Sync`定义了两个抽象方法，用来实现公平性策略。这两个方法的作用不言自明，具体的实现在其两个子类中。

## 写锁的获取

写锁的`lock`调用了`Sync`的`tryAcquire()`。

当前线程获取写锁的需要先判断逻辑：

1. 没有上锁的读线程；
2. 如果有上锁的写线程，则该线程必须是当前线程；
3. 公平性策略允许。如果通过了逻辑判断，则获取写锁，写计数器加1（这意味着写计数器的值与持有锁的重入次数保持一致）。

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    protected final boolean tryAcquire(int acquires) {
        /*
         * 总结:
         * 1. 如果当前有读线程持有锁，直接返回失败
         × 2. 如果当前有写线程持有锁，并且当前线程并不是该写线程，直接失败
         * 3. 如果当前有写线程持有锁，并且当前线程是该写线程，预判断加锁后的写计
         *		数器的值是否超过了最大值（65535），超过则失败，否则成功
         * 4. 如果没有读线程和写线程持有锁，则进行公平性策略判断
         *		如果读线程可以非公平抢占锁，则去插队；否则，失败（去排队）
         */
        Thread current = Thread.currentThread();
        int c = getState();
        int w = exclusiveCount(c);
        if (c != 0) {
            // (Note: if c != 0 and w == 0 then shared count != 0)
            if (w == 0 || current != getExclusiveOwnerThread())
                return false;
            if (w + exclusiveCount(acquires) > MAX_COUNT)
                throw new Error("Maximum lock count exceeded");
            // Reentrant acquire
            setState(c + acquires);
            return true;
        }
        if (writerShouldBlock() ||
            !compareAndSetState(c, c + acquires))
            return false;
        setExclusiveOwnerThread(current);
        return true;
    }
}
```

## 写锁的释放

写锁的`unlock`调用了`Sync`的`tryRelease()`。

当前线程释放写锁，只需要判断：

1. 当前线程确实持有写锁；
2. 写计数器的值是否会变为0。

如果写计数器的值会变为0，则写锁会被释放；否则说明当前线程多次调用了`lock`而没有执行相应数目的`unlock`操作，需要等更多的`unlock`才能释放写锁（可重入）。

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    protected final boolean tryRelease(int releases) {
        /*
         * 总结：
         * 1. 如果当前线程没有持有锁，则失败，抛出异常
         * 2. 如果当前的写计数器的值减1之后为0,则成功；否则失败
         */
        if (!isHeldExclusively())
            throw new IllegalMonitorStateException();
        int nextc = getState() - releases;
        boolean free = exclusiveCount(nextc) == 0;
        if (free)
            setExclusiveOwnerThread(null);
        setState(nextc);
        return free;
    }
}
```

## 读锁的获取

读锁的`lock`调用了`Sync`的`tryAcquireShared()`。

线程获取读锁的需要先判断逻辑：

1. 写锁没有被其他线程获取，同一个线程可以先获取写锁在获取读锁（降级）；
2. 公平性策略允许。

如果通过了逻辑判断，则获取读锁，读计数器加1（这意味着读计数器的值与持有锁的重入次数保持一致）。读锁的获取是一个不断尝试的自旋过程。

由于读锁也是可以重入的，所以用读计数器表示所有读线程的重入次数外，`Sync`还维护了一套读线程的线程局部计数器（ThreadLocal），用于记录每一个写线程的重入数。

```java

abstract static class Sync extends AbstractQueuedSynchronizer {
    protected final int tryAcquireShared(int unused) {
        /*
         * 总结：
         * 1. 如果已经有写线程持有锁，判断该线程是不是当前线程，
         *		如果不是，直接失败；否则可以试图去获取锁
         * 2. 不管有没有写线程持有锁，当前线程都可以去试图获取锁
         * 3. 如果读计数器的值会超过最大值（65535），则失败
         */
        Thread current = Thread.currentThread();
        int c = getState();
        if (exclusiveCount(c) != 0 &&
            getExclusiveOwnerThread() != current)
            return -1;
        int r = sharedCount(c);
        if (!readerShouldBlock() &&
            r < MAX_COUNT &&
            compareAndSetState(c, c + SHARED_UNIT)) {
            if (r == 0) {
                firstReader = current;
                firstReaderHoldCount = 1;
            } else if (firstReader == current) {
                firstReaderHoldCount++;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != current.getId())
                    cachedHoldCounter = rh = readHolds.get();
                else if (rh.count == 0)
                    readHolds.set(rh);
                rh.count++;
            }
            return 1;
        }
        return fullTryAcquireShared(current);
    }
}
```

## 读锁的释放

读锁的`unlock`调用了`Sync`的`tryReleaseShared()`。当前线程释放写锁，只需要判断线程局部计数器是否大于1。

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    protected final boolean tryReleaseShared(int unused) {
        /*
         * 总结：
         * 1. 判断当前读线程的局部计数器是否可以往下减（大于1）
         */
        Thread current = Thread.currentThread();
        if (firstReader == current) {
            // assert firstReaderHoldCount > 0;
            if (firstReaderHoldCount == 1)
                firstReader = null;
            else
                firstReaderHoldCount--;
        } else {
            HoldCounter rh = cachedHoldCounter;
            if (rh == null || rh.tid != current.getId())
                rh = readHolds.get();
            int count = rh.count;
            if (count <= 1) {
                readHolds.remove();
                if (count <= 0)
                    throw unmatchedUnlockException();
            }
            --rh.count;
        }
        for (;;) {
            int c = getState();
            int nextc = c - SHARED_UNIT;
            if (compareAndSetState(c, nextc))
                // Releasing the read lock has no effect on readers,
                // but it may allow waiting writers to proceed if
                // both read and write locks are now free.
                return nextc == 0;
        }
    }
}
```

# 公平性

在`Sync`的共享锁和互斥锁的获取过程中都去判断了公平性策略，公平性是同时对写线程和读线程起作用，因为读线程和写线程在同一个等待队列里面进行排队。公平性判断实在获取锁的过程中进行的，此时有空余的锁可供获取（有可能是等待队列是空的；也有可能等待队列不是空的，里面有很多读线程和写线程，但是当前线程申请锁的时候，刚好有一个线程释放了锁）。如果当前线程申请锁的时候没有空余的锁，只能乖乖地进入等待序列排队。

公平与非公平是通过`FairSync`和`NonfairSync`来区分的，分别实现`writerShouldBlock()`和`readerShouldBlock()`方法。

## 非公平的策略

对于写线程而言，非公平就是能插队就插队。不公平是对那些在等待队列中线程而言的，它们有可能一直在等待锁。

对于读线程而言，非公平就是只要等待队列的第一个（等待最久）线程不是写线程就去插队。但其实读锁是一个共享锁，所以插队也没有意义；但是可以保证写线程有机会拿到锁。

```java
static final class NonfairSync extends Sync {
    final boolean writerShouldBlock() {
        return false; // 写线程有机会就抢锁，没办法采取排队等锁
    }
    final boolean readerShouldBlock() {
        /*
         × 请求写锁和读锁的线程在同一个队列里面排队
         * 如果等待队列的第一个线程（等的黄花菜最凉的那一位）请求的是写锁，返回true；否则返回false
         */
        return apparentlyFirstQueuedIsExclusive();
    }
}
```

## 公平的策略

对于写线程而言，公平就是只要等待队列中没有其他就插队；对于读线程也是一样。

好像这个没有什么意义。进入等待队列进行排队的意思是，当前线程要被挂起，排到了的时候要被恢复，这都需要操作系统的调度，存在一定的开销。而插队就是先不去挂起，先尝试获取锁，获取失败再排队。当队列是空的情况下，当然不需要去排队了，这样貌似可以降低一些系统开销。

```java
static final class FairSync extends Sync {
    final boolean writerShouldBlock() {
        /*
         * 等待队列没有前驱节点
         */
        return hasQueuedPredecessors();
    }
    final boolean readerShouldBlock() {
        /*
         * 等待队列没有前驱节点
         */
        return hasQueuedPredecessors();
    }
}
```

# 锁降级

如果当前线程已经持有了读锁，下一次可以再次申请到读锁，这个是读锁的可重入。对于写锁也是存在可重入的。

如果当前线程已经持有了写锁，可以申请到读锁，然后释放写锁，这个称之为锁的降级。

但是，锁不能升级。获取到写锁的条件之一是没有读锁被持有，不管是不是当前线程。

# 条件变量

锁就有条件变量`Condition`。

读锁是共享锁，条件变量没有意义，所以获取条件变量的方法会抛出`UnsupportedOperationException`异常。

写锁的条件变量的用法同[`ReentrantLock`](/2016/04/07/learning-java-concurrency-reentrantlock-condition/)

# 总结

`ReentrantReadWriteLock`本身并不是锁，它通过一个内部锁来实现读锁、写锁以及二者的同步。读锁是一种共享锁，写锁是一种互斥锁。

`ReentrantReadWriteLock`的使用也存在公平不公平的选择。

`ReentrantReadWriteLock`可以保护共享资源的访问，当共享资源是很大的集合并且读线程（远远）多余写线程的时候，对于性能的提升有很明显效果。
