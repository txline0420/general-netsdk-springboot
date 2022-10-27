package com.netsdk.demo.customize.surfaceEventDemo;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author 47040
 * @since Created in 2021/5/12 10:43
 */
public class EventResource {

    /**
     * 展示列表的最大长度
     */
    private static final Integer MaxEventListSize = 50;

    /**
     * 用于在列表展示的事件列表，每次都插入到最前端
     * <p>
     * 最大长度随表格长度 超出后舍弃最旧的事件
     */
    private static final List<SurfaceEventInfo> surfaceEventList = Collections.synchronizedList(new LinkedList<>());

    /**
     * 专门给展示列表用的读写锁
     */
    private static final ReentrantReadWriteLock listLock = new ReentrantReadWriteLock();

    /**
     * 缓存的最大长度
     */
    private static final Integer MaxEventCacheSize = 100;

    /**
     * 用于缓存所有事件 判断是否有可以组合成事件组的列表
     * <p>
     * 找到事件组后 取出这些事件并从列表中删除
     */
    private static final List<SurfaceEventInfo> surfaceEventCache = Collections.synchronizedList(new LinkedList<>());
    /**
     * 专门给缓存用的读写锁
     */
    private static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    /**
     * 用于在存入事件列表和缓存后 触发监听器 的阻塞队列
     * <p>
     * 监听器在取到事件数据后 把它存入缓存的同时 判断是否有事件组
     */
    private static final LinkedBlockingQueue<SurfaceEventInfo> surfaceEventQueue = new LinkedBlockingQueue<>(1000);

    /**
     * 用于事件list表界面展示 的阻塞队列
     * <p>
     * 这么做主要是为了当数据量大时，不会卡死 UI
     */
    private static final LinkedBlockingQueue<List<SurfaceEventInfo>> surfaceEventListQueue = new LinkedBlockingQueue<>(1000);

    /**
     * 用于事件组界面展示 的阻塞队列
     * <p>
     * 这么做主要是为了当数据量大时有个缓存，不会卡死 UI
     */
    private static final LinkedBlockingQueue<List<SurfaceEventInfo>> surfaceEventGroupQueue = new LinkedBlockingQueue<>(1000);

    public static List<SurfaceEventInfo> getSurfaceEventList() {
        return surfaceEventList;
    }

    public static List<SurfaceEventInfo> getSurfaceEventCache() {
        return surfaceEventCache;
    }

    public static LinkedBlockingQueue<SurfaceEventInfo> getSurfaceEventQueue() {
        return surfaceEventQueue;
    }

    public static LinkedBlockingQueue<List<SurfaceEventInfo>> getSurfaceEventListQueue() {
        return surfaceEventListQueue;
    }

    public static LinkedBlockingQueue<List<SurfaceEventInfo>> getSurfaceEventGroupQueue() {
        return surfaceEventGroupQueue;
    }

    // 给展示列表更新数据并添加到队列
    public static void AddEventListEle(SurfaceEventInfo surfaceEventInfo) {
        listLock.writeLock().lock();

        getSurfaceEventList().add(0, surfaceEventInfo);
        if (getSurfaceEventList().size() > MaxEventListSize) {
            getSurfaceEventList().remove(MaxEventListSize.intValue());
        }

        // 浅拷贝一份推送到队列
        getSurfaceEventListQueue().offer(new ArrayList<>(getSurfaceEventList()));

        listLock.writeLock().unlock();
    }

    // 给缓存添加数据 并检查本事件组的事件是否都已收到 收到则添加到队列
    public static void AddCacheAndCheckGroupEvents(SurfaceEventInfo surfaceEventInfo) {

        cacheLock.writeLock().lock();

        // 新事件添加进缓存
        getSurfaceEventCache().add(surfaceEventInfo);
        if (getSurfaceEventCache().size() > MaxEventCacheSize)
            getSurfaceEventCache().remove(0);

        Integer eventID = surfaceEventInfo.getEventID();
        Integer count = surfaceEventInfo.getFileCount();

        // 查找本 groupId 下的所有事件
        List<SurfaceEventInfo> groupEvent = getSurfaceEventCache().stream()
                .filter(e -> e.getEventID().equals(eventID)).collect(Collectors.toList());

        // 该事件组内的事件已全部接受到
        if (groupEvent.size() == count) {
            // 排序后把事件组放进队列
            groupEvent.sort(Comparator.comparingInt(o -> (Integer) o.getFileIndex()));
            getSurfaceEventGroupQueue().offer(groupEvent);
            // 从缓存里移除
            getSurfaceEventCache().removeIf(e -> e.getEventID().equals(eventID));
        }

        cacheLock.writeLock().unlock();
    }

    /**
     * 清空数据
     */
    public static void clearAll(){
        surfaceEventList.clear();
        surfaceEventCache.clear();
        surfaceEventQueue.clear();
        surfaceEventListQueue.clear();
        surfaceEventGroupQueue.clear();
    }
}
