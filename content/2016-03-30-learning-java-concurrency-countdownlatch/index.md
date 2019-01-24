+++
title = "Learning Java Concurrency - CountDownLatch"
description = "Learning Java Concurrency - CountDownLatch"
date = 2016-03-30T21:06:08+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Learning Java Concurrency"]
tags = ["java", "concurrency"]
+++

CountDownLatch 是一种比较有意思的线程同步方法，主要用于需要同步启动的环境中。

举个栗子，部门进行聚餐要等到大家都到齐了才能开动。这个时候CountDownLatch可以理解为“还有多少人没有到”这个东西，来了一个，这个东西的值就会减1。一直到人都到齐了，这个东西的值变为了0,也就是可以开吃了。

举个栗子，通用的make进行多工程代码编译，必须所有工程编译完了才能结束。

举个栗子，项目上线，各个模块都上线完了，leader说一句OK，大家才能走。

要注意以上几个栗子都是每个线程减1，但是实际中具体减多少不做限制。

比如，猫有9条命，两个人你一下我一下一刀一刀砍上去，然后它就死了。这个也可用CountDownLatch来描述。

<!-- more -->

# CountDownLatch 的简单使用

首先，估计要参与工作的子工作数，创建一个CountDownLatch。

然后，创建干活的线程，持有该CountDownLatch实例，调用await()等待事件。比如具体到聚餐就是“开吃”，具体到9命猫就是“命没了要死了”。

然后，创建多个准备的线程，每个线程持有相同的CountDownLatch实例。这些线程用来做准备工作，争取早日达到能干活的状态。具体到聚餐上就是“人一个一个来”，具体到猫上就是“一次一次被砍死”。

然后，就结束了。

# CountDownLatch 的 API

1. `CountDownLatch(int)`
    构造一个可以由n个线程共享的闭锁。
2. `void await() throws InterruptedException`
    等待原始的n变成0。调用的线程会被阻塞，直到条件达到。
2. `boolean await(long timeout, TimeUnit unit) throws InterruptedException`
    等待原始的n变成0。调用的线程会被阻塞，直到条件达到(return true)或者超时(return false)。
4. `void countDown()`
    n - 1。
5. `long getCount()`
    查询n的值。

CountDownLatch内部有一个静态类Sync。CountDownLatch的所有方法都委托到内部一个Sync实例。

```java
private static final class Sync extends AbstractQueuedSynchronizer
```

Sync可以理解为一个共享锁，主要使用AbstractQueuedSynchronizer的共享锁方面的功能。

# 示例代码

```java
package me.wbprime.showcase.concurrent;


import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class: CountDownLatchCase
 * Date: 2016/03/30 13:05
 */
public final class CountDownLatchCase {

    public static class Module implements Runnable {
        private CountDownLatch latch;
        private String name;

        public Module(final String name, final CountDownLatch val) {
            this.name = (null != name) ? name : "Anonymous";
            this.latch = val;
        }

        public void run() {
            System.out.println("Begin to deploy module: " + name);

            final Random rnd = new Random(System.currentTimeMillis());
            final int sleepTime = rnd.nextInt(1000) + 1;

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // do nothing
            }

            System.out.println("Finish deploying module: " + name);

            latch.countDown();
        }
    }

    public static class Controller implements Runnable {
        private CountDownLatch latch;

        public Controller(final CountDownLatch val) {
            this.latch = val;
        }

        public void run() {
            try {
                latch.await();
                System.out.println("Finish deploying all modules");
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    public static void main(String[] args) {

        final int moduleCount = 20;

        final CountDownLatch syncLatch = new CountDownLatch(moduleCount);

        final ExecutorService executorService = Executors.newFixedThreadPool(8);

        executorService.execute(new Controller(syncLatch));
        for (int i = 0; i < moduleCount; i++) {
            executorService.execute(new Module("Module " + i, syncLatch));
        }

        executorService.shutdown();
    }
}
```

# 示例代码说明

1. 有一个项目需要上线。项目的各个子模块解耦合做的非常好，彼此可以独立上线。但是由于上线有BOSS看着，所以所有模块的团队不管上没上线完，都得在公司里呆着，防止出意外。

2. Controller 类表征一个上线通知。所有模块上线完了，整个项目才算上线完了。BOSS才发话，大家才可以回家。

3. Module 类表征一个一个的模块。各个模块自己独立上线，上线完了通知项目组一声，然后等别的模块上线。

4. main函数里面，创建了CountDownLatch实例，创建了项目组，创建了各个模块，然后大家一起等上线。

# 代码下载

1. [CountDownLatchCase.java](CountDownLatchCase.java)
