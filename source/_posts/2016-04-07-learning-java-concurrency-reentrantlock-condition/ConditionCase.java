package me.wbprime.showcase.concurrent;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class: ConditionCase
 * Date: 2016/04/07 10:09
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public final class ConditionCase {
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
