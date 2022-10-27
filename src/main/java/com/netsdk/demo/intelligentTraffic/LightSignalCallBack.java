package com.netsdk.demo.intelligentTraffic;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_TRAFFICLIGHT_INFO;
import com.sun.jna.Pointer;

/**
 * className：LightSignalCallBack
 * description：
 * author：251589
 * createTime：2021/1/19 14:59
 *
 * @version v1.0
 */
public class LightSignalCallBack implements NetSDKLib.fTrafficLightState {

    private static LightSignalCallBack singleInstance = new LightSignalCallBack();

    public static LightSignalCallBack getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new LightSignalCallBack();
        }
        return singleInstance;
    }

    @Override
    public void invoke(NetSDKLib.LLong lLoginID, NetSDKLib.LLong lAttachHandle, Pointer pBuf, long dwUser) {
        NET_TRAFFICLIGHT_INFO info = new NET_TRAFFICLIGHT_INFO();
        ToolKits.GetPointerDataToStruct(pBuf, 0, info);
        System.out.println("设备登录句柄: " + lLoginID.longValue() + "\n" +
                "设备订阅句柄: " + lAttachHandle.longValue() + "\n" +
                "nLightChangedChannels 有效个数: " + info.nRetLightChangedNum + "\n" +
                "stuChannels 有效个数: " + info.nRetChannelNum + "\n" +
                "UTC时间, 发生红绿灯切换时的时间, 本地时区: " + info.stuUTC
        );
    }
}
