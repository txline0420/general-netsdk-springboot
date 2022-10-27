package com.netsdk.demo.customize.healthCodeEx.callback;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

/**
 * @author 47081
 * @version 1.0
 * @description 基础实现的设备断线重连的回调函数, 建议使用单例模式编写回调函数
 * @date 2020/6/5
 */
public class DefaultHaveReconnectCallBack implements NetSDKLib.fHaveReConnect {
    private static DefaultHaveReconnectCallBack INSTANCE;

    private DefaultHaveReconnectCallBack() {

    }

    public static DefaultHaveReconnectCallBack getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new DefaultHaveReconnectCallBack();
        }
        return INSTANCE;
    }

    @Override
    public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        System.out.printf("Device[%s] Port[%d] ReConnected!\n", pchDVRIP, nDVRPort);
    }
}
