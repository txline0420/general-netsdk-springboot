package com.netsdk.demo.customize.surfaceEventDemo.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.ptr.IntByReference;

/**
 * 通用登录接口
 *
 * @author 47040
 * @since Created in 2021/3/25 18:56
 */
public class LogonModule {

    private static final NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;

    // 登录按钮事件
    public NetSDKLib.LLong login(String ipAddress, Integer port, String userName, String password, NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo) {
        System.out.println("设备地址：" + ipAddress + "\n端口号：" + port + "\n用户名：" + userName + "\n密码：" + password);

        NetSDKLib.LLong m_hLoginHandle = NetSdk.CLIENT_LoginEx2(
                ipAddress, port, userName, password,
                0, null, m_stDeviceInfo, new IntByReference(0));
        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[%s]\n", ipAddress, port, ToolKits.getErrorCode());
        } else {
            System.out.println("Login Success [ " + ipAddress + " ]");
        }
        return m_hLoginHandle;
    }

    // 登出按钮事件
    public void logout(NetSDKLib.LLong m_hLoginHandle) {
        if (m_hLoginHandle.longValue() != 0) {
            System.out.println("Logout Button Action");

            if (NetSdk.CLIENT_Logout(m_hLoginHandle)) {
                System.out.println("Logout Success.");
                m_hLoginHandle.setValue(0);
            }
        }
    }

}
