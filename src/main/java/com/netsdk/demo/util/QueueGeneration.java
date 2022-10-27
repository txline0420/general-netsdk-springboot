package com.netsdk.demo.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

public class QueueGeneration {

    // 设置一个队列，容量看情况改
    private final int MAX_TASK_COUNT = 10000;   // 队列容量
    private final LinkedBlockingDeque<EventTaskHandler> eventTasks = new LinkedBlockingDeque<EventTaskHandler>(MAX_TASK_COUNT);

    // 起一个线程池
    private final int MAX_THREAD_COUNT = 10;    // 线程池容量
    private ExecutorService eventQueueService = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

    // 用于检验服务运行状态
    private volatile boolean running = true;

    // 用于查看当前线程状态
    private Future<?> eventQueueThreadStatus;

    // 初始化
    public void init() {
        eventQueueThreadStatus = eventQueueService.submit(new Thread(() -> {
            while (running) {
                try {
                    //开始一个任务
                    EventTaskHandler task = eventTasks.take();
                    try {
                        // System.out.println("Call back Task processing...");
                        task.eventCallBackProcess();    // 主要的运行任务
                    } catch (Exception e) {
                        System.err.println("任务处理发生错误");   // error
                    }
                } catch (InterruptedException e) {
                    System.err.println("任务已意外停止");   // error
                    running = false;
                }
            }
        }, "Event call back thread init"));
    }

    // 向队列添加新的任务
    public boolean addEvent(EventTaskHandler eventHandler) {
        if (!running) {
            System.err.println("任务已停止");   // error
            return false;
        }
        boolean success = eventTasks.offer(eventHandler);
        if (!success) {
            // 队列已满，无法再添加
            System.err.println("添加到事件队列失败"); // error
        }
        return success;
    }

    // 判断队列是否还有任务
    public boolean isEmpty() {
        return eventTasks.isEmpty();
    }

    // 检查服务是否还在运行
    public boolean checkIfServiceStillRunning() {
        return running && !eventQueueService.isShutdown() && !eventQueueThreadStatus.isDone();
    }


    // 启动服务
    public void activeService() {
        running = true;
        if (eventQueueService.isShutdown()) {
            eventQueueService = Executors.newFixedThreadPool(MAX_THREAD_COUNT);
            ;
            init();
            System.out.println("线程池已关闭，重新初始化线程池及任务");
        }
        if (eventQueueThreadStatus.isDone()) {
            init();
            System.out.println("线程池任务结束，重新初始化任务");
        }
    }

    // 关闭服务
    public void destroy() {
        running = false;
        eventQueueService.shutdownNow();
    }
}
