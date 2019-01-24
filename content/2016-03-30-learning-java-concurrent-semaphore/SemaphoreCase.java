package me.wbprime.showcase.concurrent;


import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Class: SemaphoreCase
 * Date: 2016/03/30 18:15
 *
 * @author Elvis Wang [mail@wbprime.me]
 */
public final class SemaphoreCase {
    private static class Item {
        private String name;

        public Item(int idx) {
            name = String.format("%s %d", Thread.currentThread().getName(), idx);
        }

        public final String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    private static class WareHouse {
        private final List<Item> items;
        private final Semaphore notFull;
        private final Semaphore notEmpty;
        private final Semaphore mutex;

        public WareHouse(final int cnt) {
            items = Lists.newArrayListWithExpectedSize(cnt);
            this.notFull = new Semaphore(cnt);
            this.notEmpty = new Semaphore(0);
            this.mutex = new Semaphore(1);
        }

        public String itemsString() {
            return items.toString();
        }

        public void put(final Item obj) throws InterruptedException {
            if (null != obj) {

                /*
                 * 获取非满的保证
                 *
                 * 如果是满的，则挂起
                 */
                notFull.acquire();

                /*
                 * 获取容器操作的独占保证
                 */
                mutex.acquire();

                items.add(obj);

                System.out.println("Put " + obj.getName());
                System.out.println(items.toString());

                /*
                 * 结束容器操作
                 */
                mutex.release();

                /*
                 * 保证非空，允许take操作（唤醒挂起线程）
                 */
                notEmpty.release();
            }
        }

        public Item take() throws InterruptedException {

            /*
             * 获取非空的保证
             *
             * 如果是空的，则挂起
             */
            notEmpty.acquire();

            /*
             * 获取容器操作的独占保证
             */
            mutex.acquire();

            final int lastIdx = items.size() - 1;
            final Item item = items.get(lastIdx);
            items.remove(lastIdx);

            System.out.println("Take " + item.getName());
            System.out.println(items.toString());

            /*
             * 结束容器操作
             */
            mutex.release();

            /*
             * 保证非满，允许put操作（唤醒挂起进程）
             */
            notFull.release();

            return item;
        }
    }

    private static class Producer implements Runnable {
        private WareHouse wareHouse;

        private int i = 0;

        public Producer(final WareHouse s) {
            wareHouse = s;
        }

        public void run() {
            try {
                while (true) {
                    final Item itm = new Item(i++);

                    wareHouse.put(itm);

                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {

            }
        }
    }

    private static class Consumer implements Runnable {
        private WareHouse wareHouse;

        public Consumer(final WareHouse s) {
            wareHouse = s;
        }

        public void run() {
            try {
                while (true) {
                    wareHouse.take();

                    Thread.sleep(1500);
                }
            } catch (InterruptedException e) {

            }
        }
    }

    public static void main(final String[] args) {
        final WareHouse wareHouse = new WareHouse(5);

        final ExecutorService executor = Executors.newCachedThreadPool();

        final int countOfConsumers = 3;
        final int countOfProducers = 5;

        for (int i = 0; i < countOfProducers; i++) {
            executor.execute(new Producer(wareHouse));
        }

        for (int i = 0; i < countOfConsumers; i++) {
            executor.execute(new Consumer(wareHouse));
        }

//        try {
//            executor.awaitTermination(1, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            executor.shutdown();
//        }
    }
}
