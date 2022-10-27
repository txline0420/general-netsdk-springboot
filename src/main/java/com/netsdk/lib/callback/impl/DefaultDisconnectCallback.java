package com.netsdk.lib.callback.impl;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

/**
 * @author 47081
 * @version 1.0
 * @description 基础实现的设备断线回调函数, 建议使用单例模式编写
 * @date 2020/6/5
 */
public class DefaultDisconnectCallback implements NetSDKLib.fDisConnect {
    private static volatile DefaultDisconnectCallback INSTANCE;

    private DefaultDisconnectCallback() {}

    public static DefaultDisconnectCallback getINSTANCE() {
        if (INSTANCE == null) {
            synchronized (DefaultDisconnectCallback.class){
                if (INSTANCE == null){
                    INSTANCE = new DefaultDisconnectCallback();
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        System.out.printf("Device[%s] Port[%d] DisConnected!\n", pchDVRIP, nDVRPort);
    }
}
