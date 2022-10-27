package com.netsdk.module.entity;

import java.io.Serializable;

/**
 * @author 47081
 * @version 1.0
 * @description 添加智能分析任务的结果
 * @date 2020/10/19
 */
public class AddAnalyseTaskResult implements Serializable {
    /**
     * 添加任务是否成功
     */
    private boolean result;
    /**
     * 任务id
     */
    private int taskId;
    /**
     * 任务对应的虚拟通道号
     */
    private int virtualChannel;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getVirtualChannel() {
        return virtualChannel;
    }

    public void setVirtualChannel(int virtualChannel) {
        this.virtualChannel = virtualChannel;
    }
}
