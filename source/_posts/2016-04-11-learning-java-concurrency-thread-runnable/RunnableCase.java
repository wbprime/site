package me.wbprime.showcase.concurrent;


/**
 * Class: RunnableCase
 * Date: 2016/04/11 19:53
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public final class RunnableCase {
    public static void main(String [] _args) {
        final Thread echo1 =  new Thread(
            new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        System.out.println(Thread.currentThread().getName() + " echos first");
                    }
                }
            }
        );

        final Thread echo2 =  new Thread(
            new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        System.out.println(Thread.currentThread().getName() + " echos second");
                    }
                }
            }
        );

        System.out.println("Main thread started!");

        echo1.start();
        echo2.start();

        joinThread(echo1);
        joinThread(echo2);

        System.out.println("Main thread finished!");
    }

    private static void joinThread(final Thread th) {
        try {
            th.join();
        } catch (InterruptedException e) {
            System.out.println(th.getName() + " interrupted!");
        }
    }
}
