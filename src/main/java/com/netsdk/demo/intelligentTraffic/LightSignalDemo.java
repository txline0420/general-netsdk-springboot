package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_IN_ATTACH_TRAFFICLIGHT_INFO;
import com.netsdk.lib.structure.NET_OUT_ATTACH_TRAFFICLIGHT_INFO;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * className：LightSingnalDemo
 * description：
 * author：251589
 * createTime：2021/1/19 14:15
 *
 * @version v1.0
 */
public class LightSignalDemo {
    // SDk对象初始化
    public static final NetSDKLib netSDK = NetSDKLib.NETSDK_INSTANCE;
    public static NetSDKLib configSDK = NetSDKLib.CONFIG_INSTANCE;

    // 判断是否初始化
    private static boolean bInit = false;
    // 判断log是否打开
    private static boolean bLogOpen = false;
    // 设备信息
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
    // 登录句柄
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 订阅句柄
    private NetSDKLib.LLong attachHandle = new NetSDKLib.LLong(0);

    // 订阅回调
    private NetSDKLib.fTrafficLightState callBack = LightSignalCallBack.getSingleInstance();

    // 断线回调
    private static NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();
    // 重连回调
    private static NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();

    // 编码格式
    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    /**
     * 初始化SDK库
     */
    public static boolean init() {
        bInit = netSDK.CLIENT_Init(disConnectCB, null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }
        // 配置日志
        LightSignalDemo.enableLog();

        // 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
        netSDK.CLIENT_SetAutoReconnect(haveReConnectCB, null);

        // 登录请求响应超时时间设置为3S
        int waitTime = 3000;
        // 登录时尝试建立连接次数
        int tryTimes = 1;
        netSDK.CLIENT_SetConnectTime(waitTime, tryTimes);
        // 设置更多网络参数， NET_PARAM 的nWaittime ， nConnectTryNum 成员与 CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        // 登录时尝 试建立链接的超时时间
        netParam.nConnectTime = 10000;
        // 设置子连接的超时时间
        netParam.nGetConnInfoTime = 3000;
        netSDK.CLIENT_SetNetworkParam(netParam);
        return true;
    }

    /**
     * 记录SDK日志到本地文件
     */
    private static void enableLog() {
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        File path = new File("sdklog/");
        if (!path.exists())
            path.mkdir();

        String logPath = path.getAbsoluteFile().getParent() + "\\sdklog" +  AnalyseTaskUtils.getDate() + ".log";
        setLog.bSetFilePath = 1;
        setLog.nPrintStrategy = 0;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        System.out.println(logPath);
        setLog.bSetPrintStrategy = 1;
        bLogOpen = netSDK.CLIENT_LogOpen(setLog);
        if (!bLogOpen)
            System.err.println("Failed to open NetSDK log");
    }

    /**
     * 高安全登录
     */
    public void loginWithHighLevel() {
        // 输入结构体参数
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
            {
                szIP = ip.getBytes();
                nPort = port;
                szUserName = username.getBytes();
                szPassword = password.getBytes();
            }
        };
        // 输出结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        // 写入sdk
        loginHandle = netSDK.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
        if (loginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", ip, port, netSDK.CLIENT_GetLastError());
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + ip);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }
    }

    /**
     * 退出
     */
    public void logOut() {
        if (loginHandle.longValue() != 0) {
            netSDK.CLIENT_Logout(loginHandle);
            System.out.println("LogOut Success");
        }
    }

    /**
     * 清除 sdk环境
     */
    public static void cleanup() {
        if (bLogOpen) {
            netSDK.CLIENT_LogClose();
        }
        if (bInit) {
            netSDK.CLIENT_Cleanup();
        }
    }

    /**
     * 清理并退出
     */
    public static void cleanAndExit() {
        netSDK.CLIENT_Cleanup();
        System.exit(0);
    }


    /**
     * 订阅交通信号灯状态
     */
    public void attachTrafficLightState() {
        this.detachEventRealLoadPic();   // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        NET_IN_ATTACH_TRAFFICLIGHT_INFO inParam = new NET_IN_ATTACH_TRAFFICLIGHT_INFO();
        inParam.cbState = callBack;
        inParam.write();
        NET_OUT_ATTACH_TRAFFICLIGHT_INFO outParam = new NET_OUT_ATTACH_TRAFFICLIGHT_INFO();
        outParam.write();
        attachHandle = netSDK.CLIENT_AttachTrafficLightState(loginHandle, inParam.getPointer(), outParam.getPointer(), 3000);
        if (attachHandle.longValue() != 0) {
            System.out.printf("CLIENT_AttachTrafficLightState Success!");
        } else {
            System.err.printf("CLIENT_AttachTrafficLightState Failed! LastError = %s\n errMsg = %s", ToolKits.getErrorCode(), ENUMERROR.getErrorMessage());
        }
    }

    /**
     * 退订交通信号灯状态
     */
    public void detachEventRealLoadPic() {
        if (attachHandle.longValue() != 0) {
            boolean ret = netSDK.CLIENT_DetachTrafficLightState(attachHandle);
            if (ret)
                System.out.println("detachEventRealLoadPic succeed!");
        }
    }


    /******************************** 测试控制台 ***************************************/
    // 登录参数  172.5.1.76   37777 admin admin456
    private String ip = "172.5.1.76";
    private int port = 37777;
    private String username = "admin";
    private String password = "admin456";

    public static void main(String[] args) {
        LightSignalDemo demo = new LightSignalDemo();
        demo.initTest();
        demo.runTest();
        demo.endTest();

    }

    /**
     * 初始化测试
     */
    public void initTest() {
        LightSignalDemo.init();
        this.loginWithHighLevel();
    }

    /**
     * 测试内容，控制台选择
     */
    public void runTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "订阅", "attachTrafficLightState"));
        menu.addItem(new CaseMenu.Item(this, "退订", "detachEventRealLoadPic"));
        menu.run();
    }

    /**
     * 结束测试
     */
    public void endTest() {
        System.out.println("End Test");
        this.logOut(); // 退出
        this.detachEventRealLoadPic();
        System.out.println("See You...");
        TrafficLightDemo.cleanAndExit(); // 清理资源并退出
    }
    /******************************** 结束 ***************************************/
}
