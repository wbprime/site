package me.wbprime.showcase.concurrent;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class: ReentrantReadWriteLockCase
 * Date: 2016/04/07 21:52
 *
 * @author Elvis Wang [bo.wang35@renren-inc.com]
 */
public final class ReentrantReadWriteLockCase {

    private static class ReadWriteLockStack {
        private final List<String>           list  = new ArrayList<String>();
        private final ReentrantReadWriteLock lock  = new ReentrantReadWriteLock();
        private final Lock                   rLock = lock.readLock();
        private final Lock                   wLock = lock.writeLock();

        public void push(final String val) {
            if (null == val)    return;

            wLock.lock();
            try {
                list.add(val);
            } finally {
                wLock.unlock();
            }
        }

        public String last() {
            rLock.lock();
            String str = null;
            try {
                final int lastIdx = list.size() - 1;
                if (lastIdx >= 0) {
                    str = list.get(lastIdx);
                }
            } finally {
                rLock.unlock();
            }
            return str;
        }
    }

    private static class ExclusiveLockStack {
        private final List<String> list = new ArrayList<String>();
        private final ReentrantLock lock = new ReentrantLock();

        public void push(final String val) {
            if (null == val)    return;

            lock.lock();
            try {
                list.add(val);
            } finally {
                lock.unlock();
            }
        }

        public String last() {
            lock.lock();
            String str = null;
            try {
                final int lastIdx = list.size() - 1;
                if (lastIdx >= 0) {
                    str = list.get(lastIdx);
                }
            } finally {
                lock.unlock();
            }
            return str;
        }
    }

    public static void main(final String[] args) {
        final ReadWriteLockStack stack = new ReadWriteLockStack();
//        final ExclusiveLockStack stack = new ExclusiveLockStack();

        final ExecutorService service = Executors.newCachedThreadPool();
        final CountDownLatch fireLatch = new CountDownLatch(1);

        service.execute(new Runnable() {
            public void run() {
                try {
                    fireLatch.await();
                } catch (InterruptedException e) {
                    // do nothing
                }

                int i = 0;
                while (i++ < 100000) {
                    final String str = stack.last();
                    if (null != str) {
                        System.out.println(str);
                    }
                }
            }
        });
        service.execute(new Runnable() {
            public void run() {
                try {
                    fireLatch.await();
                } catch (InterruptedException e) {
                    // do nothing
                }

                int i = 0;
                while (i++ < 100000) {
                    stack.push("Ele" + i);
                }
            }
        });

        fireLatch.countDown();

        service.shutdown();
    }

}
