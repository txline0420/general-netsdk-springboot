package com.netsdk.demo.thermalImaging;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;

import java.io.File;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/7 9:59
 */
public class ThermalUtils {

    ////////////////// SDK 初始化/清理资源 /////////////////////
    //////////////////////////////////////////////////////////

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    private static boolean bInit = false;    // 判断是否初始化
    private static boolean bLogOpen = false; // 判断log是否打开

    // 回调函数需要是静态的，防止被系统回收
    private static NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();     // 断线回调
    private static NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();  // 重连回调

    /**
     * Init boolean. sdk 初始化
     *
     * @return the boolean
     */
    public static boolean Init() {
        bInit = netsdk.CLIENT_Init(disConnectCB, null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }

        ThermalUtils.enableLog();  // 配置日志

        // 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
        netsdk.CLIENT_SetAutoReconnect(haveReConnectCB, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 3000;                        //登录请求响应超时时间设置为3S
        int tryTimes = 1;                           //登录时尝试建立链接 1 次
        netsdk.CLIENT_SetConnectTime(waitTime, tryTimes);
        // 设置更多网络参数， NET_PARAM 的nWaittime ， nConnectTryNum 成员与 CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        netParam.nConnectTime = 10000;              // 登录时尝试建立链接的超时时间
        netParam.nGetConnInfoTime = 3000;           // 设置子连接的超时时间
        netsdk.CLIENT_SetNetworkParam(netParam);
        return true;
    }

    /**
     * 打开 sdk log
     */
    private static void enableLog() {
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        File path = new File("sdklog/");
        if (!path.exists()) path.mkdir();

        // 这里的log保存地址依据实际情况自己调整
        String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + AnalyseTaskUtils.getDate() + ".log";
        setLog.nPrintStrategy = 0;
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        System.out.println(logPath);
        setLog.bSetPrintStrategy = 1;
        bLogOpen = netsdk.CLIENT_LogOpen(setLog);
        if (!bLogOpen) System.err.println("Failed to open NetSDK log");
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

    /////////////////////// 枚举 ///////////////////////////
    ///////////////////////////////////////////////////////

    // 测温区域类型
    public enum AreaSubtype {

        UNKNOWN(0, "未知"),
        RECT(1, "矩形"),
        ELLIPSE(2, "椭圆"),
        POLYGON(3, "多边形");

        private int value;
        private String note;

        AreaSubtype(int givenValue, String note) {
            this.value = givenValue;
            this.note = note;
        }

        public String getNote() {
            return note;
        }

        public int getValue() {
            return value;
        }

        public static String getNoteByValue(int givenValue) {
            for (AreaSubtype enumType : AreaSubtype.values()) {
                if (givenValue == enumType.getValue()) {
                    return enumType.getNote();
                }
            }
            return null;
        }
    }


    // 测温模式的类型
    public enum ArrMeterType {

        UNKNOWN(0, "未知"),
        SPOT(1, "点"),
        LINE(2, "线"),
        AREA(3, "区域");

        private int value;
        private String note;

        ArrMeterType(int givenValue, String note) {
            this.value = givenValue;
            this.note = note;
        }

        public String getNote() {
            return note;
        }

        public int getValue() {
            return value;
        }

        public static String getNoteByValue(int givenValue) {
            for (ArrMeterType enumType : ArrMeterType.values()) {
                if (givenValue == enumType.getValue()) {
                    return enumType.getNote();
                }
            }
            return null;
        }
    }

    // 测温报警类型
    // 统计量类型
    public enum AlarmType
    {
        /**
         * 测温：具体值，
         * 线测温：最大, 最小, 平均
         * 区域测温：最大, 最小, 平均, 标准, 中间, ISO
         */
        UNKNOWN(0, "未知"),
        VAL(1, "具体值"),
        MAX(2, "最大"),
        MIN(3, "最小"),
        AVR(4, "平均"),
        STD (5, "标准"),
        MID(6, "中间"),
        ISO(7, "ISO");

        private int value;
        private String note;

        AlarmType(int givenValue, String note) {
            this.value = givenValue;
            this.note = note;
        }

        public String getNote() {
            return note;
        }

        public int getValue() {
            return value;
        }

        public static String getNoteByValue(int givenValue) {
            for (AlarmType enumType : AlarmType.values()) {
                if (givenValue == enumType.getValue()) {
                    return enumType.getNote();
                }
            }
            return null;
        }
    }

    // 比较运算结果
    public enum AlarmResultType
    {
        UNKNOWN(0, "未知"),
        BELOW(1, "低于"),
        MATCH(2, "匹配"),
        ABOVE(3, "高于");

        private int value;
        private String note;

        AlarmResultType(int givenValue, String note) {
            this.value = givenValue;
            this.note = note;
        }

        public String getNote() {
            return note;
        }

        public int getValue() {
            return value;
        }

        public static String getNoteByValue(int givenValue) {
            for (AlarmResultType enumType : AlarmResultType.values()) {
                if (givenValue == enumType.getValue()) {
                    return enumType.getNote();
                }
            }
            return null;
        }
    }

}
