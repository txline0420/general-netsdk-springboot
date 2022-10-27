package com.netsdk.lib;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/7/20 16:01
 */
public class ThreadPoolUtil {

        private static final int CORE_SIZE = 20;

        private static final int MAX_SIZE = 40;

        private static final long KEEP_ALIVE_TIME = 30;

        private static final int QUEUE_SIZE = 5000;

        private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(QUEUE_SIZE), new ThreadPoolExecutor.AbortPolicy());

        public static ThreadPoolExecutor getThreadPool() {
            return threadPool;

    }


}
