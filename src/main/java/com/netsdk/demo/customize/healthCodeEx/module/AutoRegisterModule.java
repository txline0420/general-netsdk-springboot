package com.netsdk.demo.customize.healthCodeEx.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.ENUMERROR;

/**
 * 主动注册监听
 *
 * @author 47040
 * @since Created at 2021/5/28 19:52
 */
public class AutoRegisterModule {

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;

    // 开启监听
    public static NetSDKLib.LLong ServerStartListen(String serverIPAddr, int listenPort, NetSDKLib.fServiceCallBack ServerListenCB) {
        // 这里的 nTimeout 其实是无效的
        NetSDKLib.LLong m_hListenHandle = NetSdk.CLIENT_ListenServer(serverIPAddr, listenPort, 1000, ServerListenCB, null);
        if (m_hListenHandle == null || m_hListenHandle.longValue() == 0) {
            System.err.println("开启监听失败: " + ENUMERROR.getErrorCode());
            return m_hListenHandle;
        }
        System.out.println("开启监听成功   CLIENT_ListenServer Sucess");
        return m_hListenHandle;
    }

    // 结束监听
    public static void ServerStopListen(NetSDKLib.LLong m_hListenHandle) {
        if (m_hListenHandle.longValue() == 0) return;
        boolean ret = NetSdk.CLIENT_StopListenServer(m_hListenHandle);
        if (!ret) {
            System.err.println("结束监听失败: " + ENUMERROR.getErrorCode());
            return;
        }
        System.out.println("结束监听成功 CLIENT_StopListenServer Sucess");
    }
}
