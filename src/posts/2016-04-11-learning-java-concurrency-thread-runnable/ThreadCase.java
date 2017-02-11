package me.wbprime.showcase.concurrent;


/**
 * Class: ThreadCase
 * Date: 2016/04/11 10:07
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public class ThreadCase {
    private static class EchoThread extends Thread {
        private final String word;

        public EchoThread(final String word) {
            this.word = word;
        }

        @Override
        public void run() {
            for (int i = 0; i < 1000; i++) {
                System.out.println(this.getName() + " echos " + word);
            }
        }
    }

    public static void main(String [] _args) {
        final Thread echo1 = new EchoThread("First");
        final Thread echo2 = new EchoThread("Second");

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
