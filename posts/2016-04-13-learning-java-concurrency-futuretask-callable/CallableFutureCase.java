package me.wbprime.showcase.concurrent;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class: CallableFutureCase
 * Date: 2016/04/13 10:34
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public class CallableFutureCase {
    private static class SumupTask implements Callable<Long> {
        private final int val_;

        public SumupTask(final int val) {
            this.val_ = val;
        }

        public Long call() throws Exception {
            long result = 0;
            for (int i = 1; i <= val_; i++) {
                result = result + i;
            }

            return Long.valueOf(result);
        }
    }

    public static void main(String [] _args) {
        final ExecutorService executor = Executors.newCachedThreadPool();

        final Future<Long> result = executor.submit(new SumupTask(10000));

        System.out.println("Main thread started!");

        try {
            final Long longResult = result.get();
            System.out.println("Result is " + longResult);
        } catch (InterruptedException e) {
            // do nothing
        } catch (ExecutionException e) {
            // do nothing
        }

        System.out.println("Main thread finished!");

        executor.shutdown();
    }
}
