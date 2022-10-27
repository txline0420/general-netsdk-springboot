package com.netsdk.demo.customize.JordanPSD;

import com.netsdk.lib.NetSDKLib;

/**
 * 主动注册设备信息
 *
 * @author 47040
 * @since Created at 2021/5/28 20:16
 */
public class DeviceInfo {
    /**
     * 登录句柄 在本服务器上同一时刻下必然唯一
     */
    public NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0);
    /**
     * 注册设备的IP
     */
    public String ipAddress;
    /**
     * 注册设备的端口
     */
    public int port;
    /**
     * 注册设备的用户名
     */
    public String username = "admin";
    /**
     * 注册设备的密码
     */
    public String password = "admin123";
    /**
     * 设备信息
     */
    public NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
    /**
     * 设备登录状态
     */
    public boolean isLogin = false;

    public DeviceInfo(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String printRegisterInfo() {
        return String.format("IP: %s Port: %d", ipAddress, port);
    }
}
