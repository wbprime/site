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
