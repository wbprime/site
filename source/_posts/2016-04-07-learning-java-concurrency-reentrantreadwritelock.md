title: 'Learning Java Concurrency - ReentrantReadWriteLock'
date: 2016-04-07 21:54:38
updated: 2016-04-08 10:54:38
categories: "Learning Java Concurrency"
tags: [Java, Concurrency]

---

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

<!-- More -->

