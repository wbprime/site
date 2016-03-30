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
 * @author Elvis Wang [bo.wang35@renren-inc.com]
 */
public final class CyclicBarrierCase {
    private static class Employee implements Runnable {

        private CyclicBarrier barrier;
        private List<String>  jobs;

        public Employee(final List<String> dt, final CyclicBarrier b) {
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
            executor.execute(new Employee(workflow, barrier));
        }

        executor.shutdown();
    }
}
