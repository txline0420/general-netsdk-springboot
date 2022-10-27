package com.netsdk.demo.accessControl.accessFaceQuality;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * @author ： 47040
 * @since ： Created in 2020/8/26 20:27
 */
public class FaceQualityUtils {

    ////////////////// SDK 初始化/清理资源 /////////////////////
    //////////////////////////////////////////////////////////

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    private static boolean bInit = false;    // 判断是否初始化
    private static boolean bLogOpen = false; // 判断log是否打开

    // 回调函数建议写成静态单例的，防止被系统回收
    private static NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();     // 断线回调
    private static NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();  // 重连回调

    /**
     * Init boolean. sdk 初始化
     */
    public static boolean Init() {
        bInit = netsdk.CLIENT_Init(disConnectCB, null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }

        EnableFileLog();  // 配置日志

        // 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
        netsdk.CLIENT_SetAutoReconnect(haveReConnectCB, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 3000;                        // 登录请求响应超时时间设置为3S
        int tryTimes = 3;                           // 登录时尝试建立链接 3 次
        netsdk.CLIENT_SetConnectTime(waitTime, tryTimes);

        // 设置sdk全局网络参数,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        netParam.nConnectTime = 10000;              // 登录时尝试建立链接的超时时间 10s
        netParam.nGetConnInfoTime = 3000;           // 设置子连接的超时时间 3s
        netsdk.CLIENT_SetNetworkParam(netParam);

        return true;
    }

    /**
     * 配置 sdk 日志 -> 文件日志
     */
    private static void EnableFileLog() {

        String logFolder = "sdklog";
        File path = new File(logFolder + "/");
        if (!path.exists()) path.mkdir();

        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        setLog.bSetFilePath = 1;      // 重置日志路径
        // 这里的log保存地址依据实际情况自己调整, 注意C库需要绝对路径
        String logName = "sdklog" + GetDate() + ".log";
        String logPath = path.getAbsoluteFile().getParent() + "\\" + logFolder + "\\" + logName;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1; // 重置输出策略
        setLog.nPrintStrategy = 0;    // 输出到文件
        bLogOpen = netsdk.CLIENT_LogOpen(setLog);
        if (!bLogOpen)
            System.err.println("Failed to reset NetSDK log config.");
        System.out.println("sdk日志路径: " + logPath);
    }

    /**
     * 获取当前时间
     */
    public static String GetDate() {
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDate.format(new java.util.Date()).replaceAll("[^0-9]", "-");
    }

    /**
     * Cleanup. 清除 sdk 环境
     */
    public static void cleanup() {
        if (bLogOpen) {
            netsdk.CLIENT_LogClose();
        }
        if (bInit) {
            netsdk.CLIENT_Cleanup();
        }
    }

    /**
     * 清理并退出
     */
    public static void cleanAndExit() {
        netsdk.CLIENT_Cleanup();
        System.exit(0);
    }

}
