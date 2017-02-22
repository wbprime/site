package me.wbprime.showcase.concurrent;


import java.util.concurrent.locks.ReentrantLock;

/**
 * Class: ReentrantLockCase
 * Date: 2016/04/07 09:51
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public final class ReentrantLockCase {
    private static class Singleton {
        private static volatile Singleton INSTANCE;
        private static ReentrantLock lock = new ReentrantLock();

        private Singleton() {}

        public static Singleton instance() {
            Singleton var = INSTANCE;
            if (null == var) {
                lock.lock();
                try {
                    var = INSTANCE;
                    if (null == var) {
                        INSTANCE = var = new Singleton();
                    }
                } finally {
                    lock.unlock();
                }
            }

            return var;
        }
    }
}
