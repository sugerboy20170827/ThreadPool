package com.edu.simple;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author zhangzhe
 * @date 2019/4/13 18:17
 */
public class SimpleThreadTest {

    public static void main(String[] args) {

        SimpleThreadPool threadPool = new SimpleThreadPool();

        IntStream.rangeClosed(0,40).forEach(i->{
            threadPool.submit(()->{
                System.out.println("The runnable task " + i + " will be served by " + Thread.currentThread() + "started");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    System.out.println("I'm sorry, i caught the signal.");
                    e.printStackTrace();
                }
                System.out.println("The runnable task " + i + " will be served by " + Thread.currentThread() + "done");
            });
        });

        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        System.out.println("All the task have been done.");
        threadPool.shutdown();
//        threadPool.submit(()->{
//            System.out.println("================");
//        });

    }
}
