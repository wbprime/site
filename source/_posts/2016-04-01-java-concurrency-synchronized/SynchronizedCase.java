package me.wbprime.showcase.concurrent;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Singleton1 {
    private static Singleton1 instance_;

    private Singleton1() {}

    public Singleton1 instance() {
        if (null == instance_) {
            synchronized (Singleton1.class) {
                if (null == instance_) {
                    instance_ = new Singleton1();
                    return instance_;
                }
            }
        }

        return instance_;
    }
}

class Singleton2 {
    private static volatile Singleton2 instance_;

    private Singleton2() {}

    public Singleton2 instance() {
        if (null == instance_) {
            synchronized (Singleton2.class) {
                if (null == instance_) {
                    instance_ = new Singleton2();
                    return instance_;
                }
            }
        }

        return instance_;
    }
}

class Singleton3 {
    private static volatile Singleton3 instance_;

    private Singleton3() {}

    public Singleton3 instance() {
        Singleton3 var = instance_;
        if (null == var) {
            synchronized (Singleton3.class) {
                var = instance_;
                if (null == var) {
                    instance_ = var = new Singleton3();
                }
            }
        }

        return var;
    }
}

class Synchronized1 {
    private final List<String> list_ = new ArrayList<String>();

    public synchronized void append_1(final String val) {
        System.out.println("Begin add " + val);
        list_.add(val);
        System.out.println("Finish add " + val);
    }

    public void append_2(final String val) {
        System.out.println("Begin add " + val);
        list_.add(val);
        System.out.println("Finish add " + val);
    }
}

/**
 * Class: SynchronizedCase
 * Date: 2016/04/01 16:52
 *
 * @author Elvis Wang [bo.wang35@renren-inc.com]
 */
public final class SynchronizedCase {

    public static void main(final String[] args) {
        testSynchronized();

        testUnsynchronized();
    }

    private static void testUnsynchronized() {
        final ExecutorService executorService = Executors.newCachedThreadPool();

        final Synchronized1 synchronized1 = new Synchronized1();
        executorService.execute(new Runnable() {
            public void run() {
                for (int i = 0; i < 5; i++) {
                    final String str = String.format("str 1: %d", i);
                    synchronized1.append_2(str);
                }
            }
        });

        executorService.execute(new Runnable() {
            public void run() {
                for (int i = 0; i < 5; i++) {
                    final String str = String.format("str 2: %d", i);
                    synchronized1.append_2(str);
                }
            }
        });

        executorService.shutdown();
    }

    private static void testSynchronized() {
        final ExecutorService executorService = Executors.newCachedThreadPool();

        final Synchronized1 synchronized1 = new Synchronized1();
        executorService.execute(new Runnable() {
            public void run() {
                for (int i = 0; i < 5; i++) {
                    final String str = String.format("str 1: %d", i);
                    synchronized1.append_1(str);
                }
            }
        });

        executorService.execute(new Runnable() {
            public void run() {
                for (int i = 0; i < 5; i++) {
                    final String str = String.format("str 2: %d", i);
                    synchronized1.append_1(str);
                }
            }
        });

        executorService.shutdown();
    }
}
