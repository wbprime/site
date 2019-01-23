---
title: 'Learning Java Concurrency - Semaphore'
date: 2016-03-30 20:33:17
updated: 2016-04-01 11:03:17
categories: ["Learning Java Concurrency"]
tags: [java, concurrency]

---

Semaphore，信号量。信号量可以理解为令牌掌牌使，负责令牌的发放；当线程需要执行任务时，先到信号量处领取令牌，领取到了令牌再去执行任务；如果令牌被领光了，就需要一直等待；如果任务执行完了，需要到信号量处交还令牌。很简单的逻辑！

还是吃个栗子，地铁里面公厕，一般也就3、4个坑位。人有三急，当你有需要的时候，还必须得靠这个解决。这个时候，如果公厕里没有人，或者还剩最后一个坑位，那就啥也别说了，进去吧。但是，如果，满了。就，只有，等，了。不着急可以等一等；实在憋不住的，可以催一催。但是不管急不急，都要等里面随便一个里面出来人了才能进去。这个就是典型的信号量。

还有就是非常典型的生产者/消费者问题了。有一个仓库，里面的仓位是有限的。生产者只有当仓库里面有空仓位时才能进行生产；如果没有空仓位，则需要等待；如果生产了一次，则仓库少了一个空仓位。消费者只有当仓库里有非空仓位时才能消费；如果没有非空仓位，就需要等待；如果消费了一次，仓库里多了一个空仓位。

<!-- More -->

# Semaphore的简单实用

首先，初始化信号量控制的令牌的个数。

然后，消费者去申请令牌，可能申请到，也可能被阻塞。

然后，生产者去释放令牌。

然后，交互就可以开始了。

注意，信号量只是保证资源的可用性，当资源不可用时，阻塞线程；然而线程使用资源的过程不保证是原子的，需要另外加锁控制。

举个例子，你成功申请到了令牌开始执行任务，但是这个任务可能失败，可能成功，还有可能部分成功部分失败。

# Semaphore的API

1. Semaphore(int permits) & Semaphore(int permits, boolean fair)
    构造一个信号量实例（可以是公平的或者非公平的），默认是非公平的。
2. void acquire() throws InterruptedException
    申请一枚令牌；如果没有可用令牌，则阻塞。
3. void acquireUninterruptibly() 
    同上；当调用线程被中断时，不抛出异常。
2. void acquire(int permits) throws InterruptedException
    申请多枚令牌；如果没有可用令牌，则阻塞。
2. void acquireUninterruptibly(int permits) 
    同上；当调用线程被中断时，不抛出异常。
4. boolean tryAcquire() 
    申请一枚令牌；立即返回，申请成功返回true，反之false。
4. boolean tryAcquire(int permits) 
    申请多枚令牌；立即返回，申请成功返回true，反之false。
5. boolean tryAcquire(long timeout, TimeUnit unit) throws TimeoutException
    申请一枚令牌，不允许超时；立即返回，申请成功返回true，反之false。
5. boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws TimeoutException
    申请多枚令牌，不允许超时；立即返回，申请成功返回true，反之false。
10. void release()
    归还一枚令牌。
11. void release(int permits)
    归还多枚令牌。
12. int availablePermits()
    当前可用的令牌数。
13. int drainPermits()
    申请获取所有可用令牌，返回申请到的令牌数。
14. boolean isFair()
    是否公平。

Semaphore内部有一个静态类Sync来实现公平策略，NonFairSync来实现非公平策略。

```java
static class Sync extends AbstractQueuedSynchronizer

...

static final class NonfairSync extends Sync
```

公平与非公平的区别在于申请令牌的调用中是否可以插队。公平的策略是将所有线程放入一个FIFO队列，按照出队顺序分配令牌；非公平策略是如果申请的时候有新的释放出来的令牌，直接获取，不需要排队。由于线程阻塞然后被唤醒的开销可能会比较大，所以非公平可能会比公平策略有潜在的更高的性能。

公平与非公平策略只影响申请令牌时的操作；如果已经被放入了等待队列，则公平与非公平没有区别。

# 示例代码

```java
package me.wbprime.showcase.concurrent;


import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Class: SemaphoreCase
 * Date: 2016/03/30 18:15
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public final class SemaphoreCase {
    private static class Item {
        private String name;

        public Item(int idx) {
            name = String.format("%s %d", Thread.currentThread().getName(), idx);
        }

        public final String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    private static class WareHouse {
        private final List<Item> items;
        private final Semaphore notFull;
        private final Semaphore notEmpty;
        private final Semaphore mutex;

        public WareHouse(final int cnt) {
            items = Lists.newArrayListWithExpectedSize(cnt);
            this.notFull = new Semaphore(cnt);
            this.notEmpty = new Semaphore(0);
            this.mutex = new Semaphore(1);
        }

        public String itemsString() {
            return items.toString();
        }

        public void put(final Item obj) throws InterruptedException {
            if (null != obj) {

                /*
                 * 获取非满的保证
                 *
                 * 如果是满的，则挂起
                 */
                notFull.acquire();

                /*
                 * 获取容器操作的独占保证
                 */
                mutex.acquire();

                items.add(obj);

                System.out.println("Put " + obj.getName());
                System.out.println(items.toString());

                /*
                 * 结束容器操作
                 */
                mutex.release();

                /*
                 * 保证非空，允许take操作（唤醒挂起线程）
                 */
                notEmpty.release();
            }
        }

        public Item take() throws InterruptedException {

            /*
             * 获取非空的保证
             *
             * 如果是空的，则挂起
             */
            notEmpty.acquire();

            /*
             * 获取容器操作的独占保证
             */
            mutex.acquire();

            final int lastIdx = items.size() - 1;
            final Item item = items.get(lastIdx);
            items.remove(lastIdx);

            System.out.println("Take " + item.getName());
            System.out.println(items.toString());

            /*
             * 结束容器操作
             */
            mutex.release();

            /*
             * 保证非满，允许put操作（唤醒挂起进程）
             */
            notFull.release();

            return item;
        }
    }

    private static class Producer implements Runnable {
        private WareHouse wareHouse;

        private int i = 0;

        public Producer(final WareHouse s) {
            wareHouse = s;
        }

        public void run() {
            try {
                while (true) {
                    final Item itm = new Item(i++);

                    wareHouse.put(itm);

                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {

            }
        }
    }

    private static class Consumer implements Runnable {
        private WareHouse wareHouse;

        public Consumer(final WareHouse s) {
            wareHouse = s;
        }

        public void run() {
            try {
                while (true) {
                    wareHouse.take();

                    Thread.sleep(1500);
                }
            } catch (InterruptedException e) {

            }
        }
    }

    public static void main(final String[] args) {
        final WareHouse wareHouse = new WareHouse(5);

        final ExecutorService executor = Executors.newCachedThreadPool();

        final int countOfConsumers = 3;
        final int countOfProducers = 5;

        for (int i = 0; i < countOfProducers; i++) {
            executor.execute(new Producer(wareHouse));
        }

        for (int i = 0; i < countOfConsumers; i++) {
            executor.execute(new Consumer(wareHouse));
        }

//        try {
//            executor.awaitTermination(1, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            executor.shutdown();
//        }
    }
}
```

# 示例代码说明

1. 典型的生产者/消费者模型，WareHouse类表征仓库，Consumer类表征消费者，Producer类表征生产者。

2. notFull信号量负责发放生产令牌，由生产者acquire，消费者release。

3. notEmpty信号量负责发放消费令牌，由消费者acquire，生产者release。

4. mutex信号量表示生产和消费的互斥，用来保证列表元素读取的线程安全性，可以用ReentrantLock代替。

# 代码下载

1. [SemaphoreCase.java](SemaphoreCase.java)
