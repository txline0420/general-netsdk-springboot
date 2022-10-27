package com.netsdk.demo.intelligentTraffic.trafficRadarDemo;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.Utils;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/12/14 10:05
 */
public class TrafficRadarUtils {

    ////////////////// SDK 初始化/清理资源 /////////////////////
    //////////////////////////////////////////////////////////

    // The constant net sdk
    public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;

    private static boolean bInit = false;    // 判断是否初始化
    private static boolean bLogOpen = false; // 判断log是否打开

    // 回调函数需要是静态的，防止被系统回收
    private static final NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();     // 断线回调
    private static final NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();  // 重连回调

    // 多平台的字符串编码选择
    public static String GetSystemEncode() {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            return "GBK";
        } else if(osPrefix.toLowerCase().startsWith("linux-amd64")) {
            return "UTF-8";
        } // 有其他枚举这里继续加
        return "UTF-8";
    }

    /**
     * Init boolean. sdk 初始化
     *
     * @return the boolean
     */
    public static boolean Init() {
        bInit = netSdk.CLIENT_Init(disConnectCB, null);
        if (!bInit) {
            System.err.println("Initialize NetSDK failed");
            return false;
        }

        EnableLog();  // 配置日志

        // 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
        netSdk.CLIENT_SetAutoReconnect(haveReConnectCB, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 3000;                        //登录请求响应超时时间设置为3S
        int tryTimes = 1;                           //登录时尝试建立链接 1 次
        netSdk.CLIENT_SetConnectTime(waitTime, tryTimes);

        // 设置更多网络参数， NET_PARAM 的nWaitTime ， nConnectTryNum 成员与 CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        netParam.nConnectTime = 10000;              // 登录时尝试建立链接的超时时间
        netParam.nGetConnInfoTime = 3000;           // 设置子连接的超时时间
        netSdk.CLIENT_SetNetworkParam(netParam);
        return true;
    }

    /**
     * 打开 sdk log
     */
    private static void EnableLog() {
        String sdkFolder = "sdklog";
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        File path = new File(sdkFolder + "/");
        if (!path.exists()) {
            if (!path.mkdir()) {
                System.err.println("Create NetSDK Log Folder Failed.");
            }
        }
        // 这里的log保存地址依据实际情况自己调整
        String logPath = path.getAbsoluteFile().getParent() + "\\" + sdkFolder + "\\" + "sdklog-" + Utils.getDate() + ".log";
        setLog.bSetFilePath = 1;      // 自定义日志路径
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length); // 写入自定义路径
        System.out.println("Reset NetSDK Log Path to: " + logPath);
        setLog.bSetPrintStrategy = 1; // 自定义日志输出机制
        setLog.nPrintStrategy = 0;    // 输出到文件
        bLogOpen = netSdk.CLIENT_LogOpen(setLog);
        if (!bLogOpen) System.err.println("Open NetSDK Log Failed.");
    }

    /**
     * Cleanup. 清除 sdk 环境
     */
    public static void cleanup() {
        if (bLogOpen) {
            netSdk.CLIENT_LogClose();
        }
        if (bInit) {
            netSdk.CLIENT_Cleanup();
        }
    }

    /**
     * 清理并退出
     */
    public static void cleanAndExit() {
        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }
}
