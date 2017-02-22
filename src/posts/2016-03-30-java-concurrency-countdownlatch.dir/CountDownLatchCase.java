package me.wbprime.showcase.concurrent;


import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class: CountDownLatchCase
 * Date: 2016/03/30 13:05
 *
 * @author Elvis Wang [mail@wbprime.me]
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
