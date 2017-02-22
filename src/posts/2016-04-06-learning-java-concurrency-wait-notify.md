---
title: 'Learning Java Concurrency - wait & notify'
date: 2016-04-06 10:00:56
updated: 2016-04-06 10:00:56
categories: "Learning Java Concurrency"
tags: [Java, Concurrency]

---

在synchronized关键字之外，Java提供了另外的`wait`和`notify`函数族用于支援多线程通信，使用上类似于JUC的Condition类。

`wait()`、`notify()`和`notifyAll()`是Object类的方法，与synchronized配套使用。

```java
public class Object {
    ...

    public final native void wait(long timeout) throws InterruptedException;
    public final void wait(long timeout, int nanos) throws InterruptedException;
    public final void wait() throws InterruptedException;

    public final native void notify();
    public final native void notifyAll();

    ...
}
```

`wait`一共有三个函数。调用`wait`的线程必须已经持有了同一个对象的同步器（使用synchronized）。调用`wait`的线程会进入等待状态，直到另外的线程调用了同一个对象的`notify`函数，或者指定的等待时间过期，或者被中断（引发InterruptedException）。调用`wait`函数之后，当前线程会放弃已经持有的同步器。

`notify`一共是有两个函数。`notify()`函数会唤醒当前的由于执行`wait`而进入等待的某个线程；注意，被唤醒的线程是不可预料的，也就是说不同的JVM实现可以用不同的规则算法来决定被唤醒的是哪一个线程。`notifyAll()`函数会唤醒所有的等待线程，但是只会有一个线程最终进入执行。

<!-- More -->

下面用两个例子来说明`wait & notify`的使用场景和使用方式。

# 银行取钱

银行取钱是传统的生产者和消费者模型的一个简化版本。

假设有一个银行账户，两个用户分别要往里面存钱和取钱（可以想象为一个通知汇款，儿子在上大学要花钱，打电话让父亲给打钱；两个ATM机，父亲手哆嗦地五百五百地存，儿子不耐烦地刷，有钱就取出来）。

## 代码

```java
package me.wbprime.showcase.concurrent;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class: DepositCase
 * Date: 2016/04/05 14:42
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public class DepositCase {
    private static class DepositAccount {
        private int money;

        public DepositAccount() {
            this.money = 0;
        }

        public synchronized void withdraw(final int val) {
            while (money < val) {
                try {
                    this.wait(); // 钱不够，等一会儿
                } catch (InterruptedException e) {
                    // do nothing here
                }
            }

            money -= val;
        }

        public synchronized void deposite(final int val) {
            money += val;
            this.notifyAll(); // 存完钱周知一下
        }
    }

    private static class OldFather implements Runnable {
        private DepositAccount account;
        private final int totalMoney;
        private int depositedMoney;

        public OldFather(final DepositAccount account, final int val) {
            this.account = account;

            this.totalMoney = val;
            this.depositedMoney = 0;
        }

        public void run() {
            final int MAX_MONEY_EACH_TIME = 5000; // 每次最多存这么多钱
            while (depositedMoney < totalMoney) {
                final int moneyEachTime =
                    (totalMoney - depositedMoney) < MAX_MONEY_EACH_TIME ? (totalMoney - depositedMoney) : MAX_MONEY_EACH_TIME;

                account.deposite(moneyEachTime);
                depositedMoney += moneyEachTime;

                System.out.println("父亲 sent " + moneyEachTime + " RMB to his son");

                try {
                    Thread.sleep(5000); // 缓口气
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }

    private static class Son implements Runnable {
        private final String name;
        private DepositAccount account;
        private final int neededMoney;
        private int availMoney;

        public Son(final String name, final DepositAccount account, final int money) {
            this.name = name;

            this.account = account;

            this.neededMoney = money;
            this.availMoney = 0;
        }

        public void run() {
            final int MAX_MONEY_EACH_TIME = 1000; // 每次最多取这么多钱
            while (availMoney < neededMoney) {
                final int moneyEachTime =
                    (neededMoney - availMoney) < MAX_MONEY_EACH_TIME ? (neededMoney - availMoney) : MAX_MONEY_EACH_TIME;

                account.withdraw(moneyEachTime);
                availMoney += moneyEachTime;

                System.out.println(name + " get " + moneyEachTime + " RMB from his father");

                try {
                    Thread.sleep(1000); // 抽根烟
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }

    public static void main(final String[] args) {
        final DepositAccount account = new DepositAccount();

        final ExecutorService executorService = Executors.newCachedThreadPool();

        final int moneyToSon1 = 10000;
        final int moneyToSon2 = 18600;
        final int moneyToSon3 = 10240;

        executorService.execute(
            new OldFather(account, moneyToSon1 + moneyToSon2 + moneyToSon3)
        );
        executorService.execute(
            new Son("胡大", account, moneyToSon1)
        );
        executorService.execute(
            new Son("胡二", account, moneyToSon2)
        );
        executorService.execute(
            new Son("胡三", account, moneyToSon3)
        );

        executorService.shutdown();
    }
}
```

## 代码说明

1. `DepositAccount`类表征银行存款帐号，主要记录当前有多少钱，并提供同步的存款、取款的方法。取钱的钱不够了就没法取，只能等着；存钱的就没关系，可以一直往里面存，每存一笔钱就通知一遍要取钱的人。
2. `OldFather`类表征存钱的父亲，连续往账户里面存钱，每次存钱都有一个限额。父亲每存一笔钱，要叹一口气。
3. `Son`类表征在外的儿子（们），缺钱了去取钱，每次取钱有限额。如果账户里面没有钱了，只能抽一根烟等着了。
4. `main()`函数，首先构造帐号，然后构造父亲和儿子（胡大、胡二和胡三），然后用一个线程池跑起来。

代码很简单。

# 令狐冲被困西湖底

另外一个例子可以考虑令狐冲被困在西湖底下面的情形。

冲哥被困在了西湖底的地牢里面，不见天日。梅庄四友发善心，每天都让人送饭给他吃。

现在来看，冲哥要吃饭，只能等人送饭过来；送饭的人是个聋哑人，到点了过来看一下，发现饭被吃了，就给一份新的饭。也就是，一个人送一个人吃；吃完了才送，没吃完不送；送来了才有得吃，没送就没得吃。

代码说话。

## 代码

```java
package me.wbprime.showcase.concurrent;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class: LinghuChongCase
 * Date: 2016/04/06 12:38
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public class LinghuChongCase {
    private static class Bowl {
        private boolean isFull;

        public synchronized void eat() {
            while (! isFull) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // do nothing
                }
            }

            isFull = false;
            this.notifyAll();
        }

        public synchronized void provide() {
            while (isFull) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // do nothing
                }
            }

            isFull = true;
            this.notifyAll();
        }
    }

    private static class LinghuChong implements Runnable{
        private final Bowl bowl;
        private final int days;

        public LinghuChong(final Bowl bowl, final int days) {
            this.bowl = bowl;
            this.days = days;
        }

        public void run() {
            int existingDays = 0;
            while (existingDays < days) {
                bowl.eat();
                System.out.println("Linghu Chong enjoys eating，");

                try {
                    Thread.sleep(1000); // 练吸心大法
                } catch (InterruptedException e) {
                    // do nothing
                }

                existingDays ++;
            }
        }
    }

    private static class SomeBody implements Runnable{
        private final Bowl bowl;
        private final int days;

        public SomeBody(final Bowl bowl, final int days) {
            this.bowl = bowl;
            this.days = days;
        }

        public void run() {
            int existingDays = 0;
            while (existingDays < days) {
                bowl.provide();
                System.out.println("Prepared a bowl for a prisoner to eat");

                try {
                    Thread.sleep(2000); // 不知道干嘛
                } catch (InterruptedException e) {
                    // do nothing
                }
                existingDays++;
            }
        }
    }

    public static void main(final String[] args) {
        final Bowl bowl = new Bowl();

        final ExecutorService executorService = Executors.newCachedThreadPool();

        final int daysLostFreedom = 15;

        executorService.execute(
            new LinghuChong(bowl, daysLostFreedom)
        );
        executorService.execute(
            new SomeBody(bowl, daysLostFreedom)
        );

        executorService.shutdown();
    }
}
```

## 代码说明

1. `Bowl`类表征饭碗，里面还有饭令狐冲才可以吃，送饭人就不会送；里面没饭了，令狐冲就没得吃，送饭人才会送。
2. `LinghuChong`类表征令狐冲，吃饭，没饭吃的时候就练吸心大法。
3. `SomeBody`类表征送饭的人，不知道干嘛的。定时送饭。
4. `main()`函数，构造饭碗，构造令狐冲和送饭人，然后设定冲哥被关了半个月。

# 代码下载

[DepositCase.java](2016-04-06-learning-java-concurrency-wait-notify/DepositCase.java)
[LinghuChongCase.java](2016-04-06-learning-java-concurrency-wait-notify/LinghuChongCase.java)
