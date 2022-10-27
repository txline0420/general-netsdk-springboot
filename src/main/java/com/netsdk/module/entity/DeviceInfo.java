package com.netsdk.module.entity;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.EMDeviceType;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * @author 47081
 * @version 1.0
 * @description 设备信息的二次封装类
 * @date 2020/9/12
 */
public class DeviceInfo {
    private long loginHandler;
    /**
     * 序列号
     */
    private String serialNumber;
    /**
     * DVR报警输入个数
     */
    private int byAlarmInPortNum;
    /**
     * DVR报警输出个数
     */
    private int byAlarmOutPortNum;
    /**
     * DVR硬盘个数
     */
    private int byDiskNum;
    /**
     * DVR类型
     */
    private EMDeviceType byDVRType;
    /**
     * DVR通道个数
     */
    private int byChanNum;
    /**
     * 当登陆失败原因为密码错误时,通过此参数通知用户,剩余登陆次数,为0时表示此参数无效
     */
    private int byLeftLogTimes;
    /**
     * 在线超时时间,为0表示不限制登陆,非0表示限制的分钟数
     * 该参数只适用于{@link com.netsdk.module.BaseModule#loginEx2(String, int, String, String, int, Pointer)}和
     * {@link com.netsdk.module.BaseModule#loginWithHighSecurity(String, int, String, String, int, Structure, NetSDKLib.NET_DEVICEINFO_Ex)}
     */
    private int byLimitLoginTime;
    /**
     * 当登陆失败,用户解锁剩余时间（秒数）, -1表示设备未设置该参数
     */
    private int byLockLeftTime;

    public long getLoginHandler() {
        return loginHandler;
    }

    public void setLoginHandler(long loginHandler) {
        this.loginHandler = loginHandler;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public int getByAlarmInPortNum() {
        return byAlarmInPortNum;
    }

    public void setByAlarmInPortNum(int byAlarmInPortNum) {
        this.byAlarmInPortNum = byAlarmInPortNum;
    }

    public int getByAlarmOutPortNum() {
        return byAlarmOutPortNum;
    }

    public void setByAlarmOutPortNum(int byAlarmOutPortNum) {
        this.byAlarmOutPortNum = byAlarmOutPortNum;
    }

    public int getByDiskNum() {
        return byDiskNum;
    }

    public void setByDiskNum(int byDiskNum) {
        this.byDiskNum = byDiskNum;
    }

    public EMDeviceType getByDVRType() {
        return byDVRType;
    }

    public void setByDVRType(EMDeviceType byDVRType) {
        this.byDVRType = byDVRType;
    }

    public int getByChanNum() {
        return byChanNum;
    }

    public void setByChanNum(int byChanNum) {
        this.byChanNum = byChanNum;
    }

    public int getByLeftLogTimes() {
        return byLeftLogTimes;
    }

    public void setByLeftLogTimes(int byLeftLogTimes) {
        this.byLeftLogTimes = byLeftLogTimes;
    }

    public int getByLimitLoginTime() {
        return byLimitLoginTime;
    }

    public void setByLimitLoginTime(int byLimitLoginTime) {
        this.byLimitLoginTime = byLimitLoginTime;
    }

    public int getByLockLeftTime() {
        return byLockLeftTime;
    }

    public void setByLockLeftTime(int byLockLeftTime) {
        this.byLockLeftTime = byLockLeftTime;
    }

    public static DeviceInfo create(long loginHandler, NetSDKLib.NET_DEVICEINFO info) {
        DeviceInfo device = new DeviceInfo();
        device.serialNumber = new String(info.sSerialNumber).trim();
        device.loginHandler = loginHandler;
        device.byAlarmInPortNum = info.byAlarmInPortNum;
        device.byAlarmOutPortNum = info.byAlarmOutPortNum;
        device.byChanNum = info.union.byChanNum;
        device.byLeftLogTimes = info.union.byLeftLogTimes;
        device.byDiskNum = info.byDiskNum;
        device.byDVRType = EMDeviceType.getEMDeviceType(info.byDVRType);
        return device;
    }

    public static DeviceInfo create(long loginHandler, NetSDKLib.NET_DEVICEINFO_Ex info) {
        DeviceInfo device = new DeviceInfo();
        device.serialNumber = new String(info.sSerialNumber).trim();
        device.loginHandler = loginHandler;
        device.byAlarmInPortNum = info.byAlarmInPortNum;
        device.byAlarmOutPortNum = info.byAlarmOutPortNum;
        device.byChanNum = info.byChanNum;
        device.byDiskNum = info.byDiskNum;
        device.byDVRType = EMDeviceType.getEMDeviceType(info.byDVRType);
        device.byLeftLogTimes = info.byLeftLogTimes;
        device.byLimitLoginTime = info.byLimitLoginTime;
        device.byLockLeftTime = info.byLockLeftTime;
        return device;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "loginHandler=" + loginHandler +
                ", serialNumber='" + serialNumber + '\'' +
                ", byAlarmInPortNum=" + byAlarmInPortNum +
                ", byAlarmOutPortNum=" + byAlarmOutPortNum +
                ", byDiskNum=" + byDiskNum +
                ", byDVRType=" + byDVRType +
                ", byChanNum=" + byChanNum +
                ", byLeftLogTimes=" + byLeftLogTimes +
                ", byLimitLoginTime=" + byLimitLoginTime +
                ", byLockLeftTime=" + byLockLeftTime +
                '}';
    }
}
