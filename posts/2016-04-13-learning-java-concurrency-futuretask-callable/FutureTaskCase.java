package me.wbprime.showcase.concurrent;


import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Class: FutureTaskCase
 * Date: 2016/04/13 12:11
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public class FutureTaskCase {
    private static class CallableMax implements Callable<Integer> {
        private final List<Integer> list_;

        public CallableMax(final List<Integer> list) {
            this.list_ = list;
        }

        public Integer call() throws Exception {
            if (null != list_ && !list_.isEmpty()) {
                int result = list_.get(0).intValue();
                for (final Integer val: list_) {
                    if (result < val.intValue()) {
                        result = val.intValue();
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
                return Integer.valueOf(result);
            } else {
                return null;
            }
        }
    }

    private static class ValueHolder {
        Integer value;
    }

    private static class RunnableMax implements Runnable {
        private final List<Integer> list_;
        private final ValueHolder holder;

        public RunnableMax(
            final List<Integer> list, final ValueHolder valueHolder
        ) {
            this.list_ = list;
            this.holder = valueHolder;
        }

        public void run() {
            if (null == holder) return ;

            if (null != list_ && !list_.isEmpty()) {
                int result = list_.get(0).intValue();
                for (final Integer val : list_) {
                    if (result < val.intValue()) {
                        result = val.intValue();
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }

                holder.value = Integer.valueOf(result);
            } else {
                holder.value = null;
            }
        }
    }

    public static void main(String [] _args) {
        final List<Integer> list = ImmutableList.of(
            1, 199, 6, 3, 56, 299, 199, 28, 10, 234
        );
        final FutureTask<Integer> task1 = new FutureTask(new CallableMax(list));

        final Thread task1Thread = new Thread(task1);
        task1Thread.start();

        System.out.println("Main thread started!");

        try {
            final Integer re1 = task1.get();
            System.out.println("Result is " + re1);
        } catch (InterruptedException e) {
            // do nothing
        } catch (ExecutionException e) {
            // do nothing
        }

        System.out.println("Main thread finished!");

        final ValueHolder holder = new ValueHolder();
        final FutureTask<ValueHolder> task2 = new FutureTask(
            new RunnableMax(list, holder),
            holder
        );

        final Thread task2Thread = new Thread(task2);
        task2Thread.start();

        System.out.println("Main thread started!");

        try {
            final ValueHolder re2 = task2.get();
            System.out.println("Result is " + re2.value);
        } catch (InterruptedException e) {
            // do nothing
        } catch (ExecutionException e) {
            // do nothing
        }

        System.out.println("Main thread finished!");
    }
}
