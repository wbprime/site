+++
title = "Learning Java Concurrency - CyclicBarrier"
description = "Learning Java Concurrency - CyclicBarrier"
date = 2016-03-30T21:06:26+08:00
draft = false
[taxonomies]
categories =  ["Learning Java Concurrency"]
tags = ["java", "concurrency"]
+++

CyclicBarrier，正如同名字里面表达出来的，是一个可以循环使用的屏障。与CountDownLatch类似，它可以用来表达等待某个状态，比如大家都到齐了，那么开始开会吧。与CountDownLatch不同的是，它可以多次等待，也就是可以等待多个状态。

好吃不过栗子。比如哈利波特的三强争霸赛，要一项一项任务地完成，大家都结束了（不管成功还是失败），才开始计划下一个任务。第一个是去挑战龙；然后是到海里面挑战人鱼群；最后是挑战伏地魔。每一个任务总是要等大家都完成了才开始。

还有一个栗子。大家一起去面试，有的公司为了省事，等凑齐了一波人才开始走流程。HR领着大家一起先笔试，然后安排初面官，然后安排复试官。每一关刷掉一波人，但是只有大家都结束了才进行下一轮。别问我哪里有这样的招聘部门，人家开心就好。

<!-- More -->

# CyclicBarrier 的简单使用

首先，需要判断有哪些关卡，设计好通关条件。

然后，创建闯关的线程，各自吃好喝好，准备闯关。

然后，每过一关，可以有看守的线程出来引导，这也是一个任务。

之后，就可以开始了。

# CyclicBarrier 的API

CyclicBarrier是来做多线程同步的，首先需要确定有多少个线程参与同步。每一个线程都需要调用await()表示自己已经就绪；当所有线程都调用了await()之后，CyclicBarrier达到了第一个屏障。此时可以简单地放行，也可以设置一个任务，由最后一个就绪的线程执行，执行完才放行。

1. CyclicBarrier(int n)
    构造一个有n个线程参与同步的同步器，阻塞所有线程直到阻塞的线程个数大于等于n。
2. CyclicBarrier(int n, Runnable action)
    构造一个有n个线程参与同步的同步器，阻塞所有线程直到阻塞的线程个数大于等于n。接触阻塞之前，由最后一个达到的新城执行action。
3. int await() throws InterruptedException, BrokenBarrierException
    阻塞调用线程，直到所有n个线程都调用了本方法。最后一个调用本方法的线程，需要去执行设置的阻塞任务，如果设置了的话。
4. int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException
    同上，加上了超时限制。返回值表示还有多少个线程未就绪，0表示调用线程是最后一个线程。
5. int getParties()
    返回构造器传入的n值。
6. boolean isBroken()
    是否被损坏，损坏原因可能是线程被中断或者超时，或者阻塞action发生异常。
7. void reset()
    重置到初始化状态。
8. int getNumberWaiting()
    返回当前已经等待的线程数。

CyclicBarrier内部是通过一个ReentrantLock实例来进行同步的，用该实例的一个Condition实例来控制是否达到放行状态。

```java
/** The lock for guarding barrier entry */
private final ReentrantLock lock = new ReentrantLock();
/** Condition to wait on until tripped */
private final Condition trip = lock.newCondition();
```

# 示例代码

```java
package me.wbprime.showcase.concurrent;


import com.google.common.collect.ImmutableList;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class: CyclicBarrierCase
 * Date: 2016/03/30 13:35
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public final class CyclicBarrierCase {
    private static class Interviewee implements Runnable {

        private CyclicBarrier barrier;
        private List<String>  jobs;

        public Interviewee(final List<String> dt, final CyclicBarrier b) {
            barrier = b;
            jobs = dt;
        }

        public void run() {
            try {
                final String myName = Thread.currentThread().getName();

                final Random rnd = new Random(System.currentTimeMillis());

                for (final String eachJob: jobs) {
                    barrier.await();
                    System.out.println(myName + ": Start processing job: " + eachJob);

                    final int sleepTime = rnd.nextInt(1000) + 1;
                    Thread.sleep(sleepTime);

                    System.out.println(myName + ": Finish processing job: " + eachJob);
                }
            } catch (InterruptedException e) {
                // do nothing
            } catch (BrokenBarrierException e) {
                // do nothing
            }
        }
    }

    private static class HR implements Runnable {

        public HR() {
        }

        public void run() {
            System.out.println("Hello everyone, go on to next challenge");
        }
    }

    public static void main(final String[] args) {
        final int memberCount = 3;

        final List<String> workflow = ImmutableList.of(
            "Self introduction",
            "Coding exam",
            "Tech interview",
            "Leader interview",
            "HR interview",
            "Offer"
        );

        final CyclicBarrier barrier = new CyclicBarrier(memberCount, new HR());

        final ExecutorService executor = Executors.newCachedThreadPool();

        for (int i = 0; i < memberCount; i ++) {
            executor.execute(new Interviewee(workflow, barrier));
        }

        executor.shutdown();
    }
}
```

# 示例代码说明

1. 某公司公开招聘，一共有3个人过来面试，HR进行安排。

2. Interviewee类表征前来面试的人，手上有一份日程表（jobs），每一个任务都需要HR领着所有人一起开始。

3. HR类表征协调的人力资源，每次准备下一关时，可以给大家解释疑问，加油打气。

4. main函数里面，创建日程表，创建CyclicBarrier实例，创建HR和应试者，然后大家开始面试，从自我介绍开始吧。

# 代码下载

1. [CyclicBarrierCase.java](CyclicBarrierCase.java)
