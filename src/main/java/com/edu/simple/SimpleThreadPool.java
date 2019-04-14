package com.edu.simple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangzhe
 * @date 2019/4/13 16:02
 */
public class SimpleThreadPool extends Thread{

    private int size;

    private static final int DEFAULT_SIZE = 10;

    private static final int DEFAULT_TASK_QUEUE_SIZE = 2000;

    public static final DiscardPolicy DEFAULT_DISCARD_POLICY = ()->{
      throw new DiscardException("discard the task");
    };

    private DiscardPolicy discardPolicy;

    private int taskQueueSize;

    private static AtomicInteger count = new AtomicInteger(0);

    private static final String PREFIX = "simple_thread_pool-";

    private static final ThreadGroup GROUP = new ThreadGroup("pool_group");

    private static final LinkedList<Runnable> TASK_QUEUE = new LinkedList<Runnable>();

    private static final List<WorkThread> THREAD_QUEUE = new ArrayList<WorkThread>();

    private volatile boolean alive = true;

    private int min;

    private int active;

    private int max;

    public SimpleThreadPool(){
        this(4,6,12, DEFAULT_TASK_QUEUE_SIZE, DEFAULT_DISCARD_POLICY);
    }

    public SimpleThreadPool(int min, int active, int max, int taskQueueSize,DiscardPolicy discardPolicy){
        this.min = min;
        this.active = active;
        this.max = max;
        this.taskQueueSize = taskQueueSize;
        this.discardPolicy = discardPolicy;
        init();
    }

    private void init(){
        for(int i = 0; i < min; i++){
            createThread();
        }
        this.size = min;
        this.start();
    }


    public int size(){
        return size;
    }

    public int queueSize(){
        return taskQueueSize;
    }

    public boolean Alive(){
        return alive;
    }


    @Override
    public void run() {

       while(alive){
           System.out.printf("pool# min : %d, active : %d, max : %d, current : %d, remaining task : %d \n",
                   this.min, this.active, this.max, this.size, TASK_QUEUE.size());

           try {
               Thread.sleep(5_000);
               if(TASK_QUEUE.size() > active && size < active){
                   for(int i = size; i <= active; i++){
                       createThread();
                   }
                   System.out.println("The pool has incremented to active.");
                   size = active;
                   continue ;
               }


               if(TASK_QUEUE.size() > max && size < max){
                   for(int i = size; i <= active; i++){
                       createThread();
                   }
                   System.out.println("The pool has incremented to max.");
                   size = max;
               }


               if(TASK_QUEUE.isEmpty() && size > active){
                   synchronized (THREAD_QUEUE){
                       int releaseSize = size - active;
                       for(Iterator<WorkThread> it = THREAD_QUEUE.iterator(); it.hasNext();){
                           if(releaseSize <= 0)
                               break;

                           WorkThread thread = it.next();
                           thread.close();
                           it.remove();
                           thread.interrupt();
                           releaseSize--;
                       }
                       size = active;
                   }

               }






           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
    }

    public void submit(Runnable runnable){

        if(alive == false)
            throw new IllegalStateException("The thread pool is closed and not allow add the task.");

        synchronized(TASK_QUEUE){
            if(TASK_QUEUE.size() > taskQueueSize)
                discardPolicy.discard();

            TASK_QUEUE.addLast(runnable);
            TASK_QUEUE.notifyAll();
        }
    }


    private void createThread(){
        WorkThread thread = new WorkThread(GROUP,PREFIX + count.getAndIncrement());
        thread.start();
        THREAD_QUEUE.add(thread);
    }


    public void shutdown(){
        while(!TASK_QUEUE.isEmpty()){
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int value = THREAD_QUEUE.size();
        synchronized(THREAD_QUEUE){
            while(value > 0){
                for(WorkThread thread : THREAD_QUEUE){
                    if(thread.getThreadState() == ThreadState.BLOCKED){   //线程执行完任务以后都会处于block状态
                        thread.interrupt();
                    }else{
                        thread.close();
                    }

                    value--;
                }
            }
        }

        alive = false;
        System.out.println("The thread pool is disposed.");

    }



    private enum ThreadState{
        FREE,RUNNING,BLOCKED,DEAD
    }


    private static class WorkThread extends Thread{

        private volatile ThreadState state = ThreadState.FREE;

//        private final ThreadGroup group;
//
//        private final String name;

        public WorkThread(ThreadGroup group, String name){
            super(group,name);
        }


        public ThreadState getThreadState(){
            return state;
        }

        public void close(){
            state = ThreadState.DEAD;
        }

        @Override
        public void run() {

            awake:
            while(state != ThreadState.DEAD){
                Runnable runnable;
                synchronized(TASK_QUEUE){
                    while(TASK_QUEUE.isEmpty()){
                        try {
                            this.state = ThreadState.BLOCKED;
                            TASK_QUEUE.wait();

                        } catch (InterruptedException e) {
                            System.out.println("closed.");
                            break awake;
//                            this.state = ThreadState.FREE
                        }
                    }
                    runnable = TASK_QUEUE.removeFirst();

                }
                if(runnable != null){
                    this.state = ThreadState.RUNNING;
                    runnable.run();
                    this.state = ThreadState.FREE;
                }


            }
        }
    }











}
