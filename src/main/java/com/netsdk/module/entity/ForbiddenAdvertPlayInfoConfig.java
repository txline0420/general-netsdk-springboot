package com.netsdk.module.entity;

import com.netsdk.lib.structure.NET_TIME_EX1;

import java.io.Serializable;

/**
 * @author 47081
 * @version 1.0
 * @description 广告禁播时间信息
 * @date 2020/9/15
 */
public class ForbiddenAdvertPlayInfoConfig implements Serializable {
    /**
     * 时间段使能
     */
    private boolean bEnable;
    /**
     * 广告禁用开始时间
     */
    public NET_TIME_EX1 stuBeginTime;
    /**
     * 广告结束开始时间
     */
    public NET_TIME_EX1 stuEndTime;

    public boolean isbEnable() {
        return bEnable;
    }

    public void setbEnable(boolean bEnable) {
        this.bEnable = bEnable;
    }

    public NET_TIME_EX1 getStuBeginTime() {
        return stuBeginTime;
    }

    public void setStuBeginTime(NET_TIME_EX1 stuBeginTime) {
        this.stuBeginTime = stuBeginTime;
    }

    public NET_TIME_EX1 getStuEndTime() {
        return stuEndTime;
    }

    public void setStuEndTime(NET_TIME_EX1 stuEndTime) {
        this.stuEndTime = stuEndTime;
    }

    public ForbiddenAdvertPlayInfoConfig() {
    }

    public ForbiddenAdvertPlayInfoConfig(boolean bEnable, int startHour, int startMinute, int startSecond, int endHour, int endMinute, int endSecond) {
        this.bEnable = bEnable;
        this.stuBeginTime = new NET_TIME_EX1();
        this.stuEndTime = new NET_TIME_EX1();
        this.stuBeginTime.dwHour = startHour;
        this.stuBeginTime.dwMinute = startMinute;
        this.stuBeginTime.dwSecond = startSecond;
        this.stuEndTime.dwHour = endHour;
        this.stuEndTime.dwMinute = endMinute;
        this.stuEndTime.dwSecond = endSecond;
    }

    public ForbiddenAdvertPlayInfoConfig(boolean bEnable, NET_TIME_EX1 stuBeginTime, NET_TIME_EX1 stuEndTime) {
        this.bEnable = bEnable;
        this.stuBeginTime = stuBeginTime;
        this.stuEndTime = stuEndTime;
    }

}
