package com.netsdk.demo.customize.healthCodeEx.entity;

/**
 * 监听回调数据
 *
 * @author 47040
 * @since Created at 2021/5/28 20:57
 */
public class ListenInfo {
    /**
     * 设备序列号
     */
    public String devSerial;
    /**
     * 设备 IP
     */
    public String devIpAddress;
    /**
     * 设备 Port
     */
    public Integer devPort;

    public ListenInfo(String devSerial, String devIpAddress, Integer devPort) {
        this.devSerial = devSerial;
        this.devIpAddress = devIpAddress;
        this.devPort = devPort;
    }
}
