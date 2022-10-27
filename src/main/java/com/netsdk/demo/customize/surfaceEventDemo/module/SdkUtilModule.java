package com.netsdk.demo.customize.surfaceEventDemo.module;

import com.netsdk.demo.util.TimeUtils;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;

import java.io.File;

/**
 * 通用 SDK 初始化接口
 *
 * @author 47040
 * @since Created in 2021/3/25 18:50
 */
public class SdkUtilModule {

    private static final NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;

    private static boolean bInit = false;
    private static boolean bLogOpen = false;

    private static final DefaultDisconnectCallback disConnect = DefaultDisconnectCallback.getINSTANCE();          // 设备断线通知回调
    private static final DefaultHaveReconnectCallBack haveReConnect = DefaultHaveReconnectCallBack.getINSTANCE(); // 网络连接恢复

    // 初始化 SDK
    public static boolean init() {

        bInit = NetSdk.CLIENT_Init(disConnect, null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }

        //打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        File path = new File(".");
        String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\PtzNewDemo" + TimeUtils.getTimeStr(System.currentTimeMillis()) + ".log";

        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        bLogOpen = NetSdk.CLIENT_LogOpen(setLog);
        if (!bLogOpen) {
            System.err.println("Failed to open NetSDK log");
        }

        // 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
        // 此操作为可选操作，但建议用户进行设置
        NetSdk.CLIENT_SetAutoReconnect(haveReConnect, null);

        // 设置登录超时时间和尝试次数，可选
        int waitTime = 5000; // 登录请求响应超时时间设置为5S
        int tryTimes = 3;    // 登录时尝试建立链接3次
        NetSdk.CLIENT_SetConnectTime(waitTime, tryTimes);

        // 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
        NetSdk.CLIENT_SetNetworkParam(netParam);
        return true;
    }

    // 清理 SDK 资源
    public static void cleanup() {
        if (bLogOpen) {
            NetSdk.CLIENT_LogClose();
        }

        if (bInit) {
            NetSdk.CLIENT_Cleanup();
        }
    }
}
