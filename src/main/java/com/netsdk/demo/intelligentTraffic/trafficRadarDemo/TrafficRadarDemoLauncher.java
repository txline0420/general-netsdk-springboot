package com.netsdk.demo.intelligentTraffic.trafficRadarDemo;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;

import java.util.Scanner;

/**
 * 雷视一体机定制
 *
 * @author 47040
 * @since Created in 2020/12/14 10:28
 */
public class TrafficRadarDemoLauncher {

    // The constant NetSDK
    public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    // The constant ConfigSDK.
    public static NetSDKLib configSdk = NetSDKLib.CONFIG_INSTANCE;
    // The encode of String
    public static final String encode = TrafficRadarUtils.GetSystemEncode();

    ////////////////////////////////////// 登录相关 //////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////

    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0);  // 登录句柄

    /**
     * login with high level 高安全级别登陆
     */
    public void loginWithHighLevel() {
        // 入参
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstInParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();
        System.arraycopy(m_strIpAddr.getBytes(), 0, pstInParam.szIP, 0, m_strIpAddr.length());           // ip
        pstInParam.nPort = m_nPort;                                                                                     // port
        System.arraycopy(m_strUser.getBytes(), 0, pstInParam.szUserName, 0, m_strUser.length());         // username
        System.arraycopy(m_strPassword.getBytes(), 0, pstInParam.szPassword, 0, m_strPassword.length()); // password
        // 出参
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        // 执行登录 获取登录句柄
        m_hLoginHandle = netSdk.CLIENT_LoginWithHighLevelSecurity(pstInParam, pstOutParam);
        if (m_hLoginHandle.longValue() == 0) {      // 如果成功登录 获取到的句柄是非0的
            System.err.printf("Login Device IpAddr[%s] Port[%d] Failed. %s\n", m_strIpAddr, m_nPort, ToolKits.getErrorCode());
        } else {
            System.out.printf("Login Device IpAddr[%s] Port[%d] Succeed.\n", m_strIpAddr, m_nPort);
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("The Count of Device Channel: " + deviceInfo.byChanNum);
        }
    }

    /**
     * logout 退出
     */
    public void logout() {
        if (m_hLoginHandle.longValue() != 0) {
            netSdk.CLIENT_Logout(m_hLoginHandle);
            System.out.println("Logout Succeed.");
        }
    }

    //////////////////////////////////////// 订阅事件/退订 ////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////
    // 智能事件订阅句柄
    private NetSDKLib.LLong m_hAttachHandle = new NetSDKLib.LLong(0);
    // 智能事件订阅函数
    private final NetSDKLib.fAnalyzerDataCallBack analyzerDataCallBack = TrafficRadarAnalyzerDataCB.getSingleInstance();
    // 报警事件订阅函数
    private final NetSDKLib.fMessCallBackEx1 messageCallBack = TrafficRadarMessageCB.getSingleInstance();

    private int channel = 0; // 通道选择

    // 选择通道
    public void setChannelID() {
        System.out.println("请输入订阅通道,-1 表示全部.");
        Scanner sc = new Scanner(System.in);
        this.channel = sc.nextInt();
    }

    /**
     * 开始侦听智能事件
     */
    public void AttachEventRealLoadPic() {

        this.DetachEventRealLoadPic();   // 先退订 部分设备不会对重复订阅作校验 重复订阅后会有重复的事件返回
        int bNeedPicture = 1;            // 是否需要图片 如果不需要图片这里填 0
        // 订阅事件并获取订阅句柄 m_hAttachHandle
        // 同一时间 各个不同设备的不同通道的订阅句柄之间不会重复
        // 这个句柄在回调函数里是会上报的，请依据这个信息建立上报事件与订阅设备、通道间的映射关系
        m_hAttachHandle = netSdk.CLIENT_RealLoadPictureEx(
                m_hLoginHandle,             // 登录句柄
                channel,                    // 订阅通道号，SDK从0开始计数 -1 表示订阅所有通道
                NetSDKLib.EVENT_IVS_ALL,    // 订阅事件类型 直接全定就行 回调函数里通过枚举区分事件
                bNeedPicture,               // 是否需要图片
                analyzerDataCallBack,       // 注册回调函数 必须保证该函数不被JVM回收 推荐写成静态单例
                null, null // 这两个参数可以不填 也不推荐使用
        );
        if (m_hAttachHandle.longValue() != 0) {    // 订阅成功时 句柄非 0
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Succeed\n", channel);
        } else {
            System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel, ToolKits.getErrorCode());
        }
    }

    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (m_hAttachHandle.longValue() != 0) {
            if (netSdk.CLIENT_StopLoadPic(m_hAttachHandle)) {
                System.out.printf("Chn[%d] CLIENT_StopLoadPic Succeed\n", channel);
            } else {
                System.out.printf("Ch[%d] CLIENT_StopLoadPic Failed!LastError = %s\n", channel, ToolKits.getErrorCode());
            }
        }
    }

    /**
     * 订阅一般报警事件
     */
    public void AttachEventStartListen() {

        // 一般报警不需要指定通道，自动全通道订阅
        boolean bRet = netSdk.CLIENT_StartListenEx(m_hLoginHandle);
        if (bRet) {
            System.out.println("CLIENT_StartListenEx Succeed.");
        } else {
            System.err.printf("CLIENT_StartListenEx fail, error:%s\n", ToolKits.getErrorCode());
        }
    }

    /**
     * 退订一般报警事件
     */
    public void DetachEventStopListen() {
        boolean bRet = netSdk.CLIENT_StopListen(m_hLoginHandle);
        if (bRet) {
            System.out.println("CLIENT_StopListen succeed");
        } else {
            System.err.printf("CLIENT_StopListen fail, error:%s\n", ToolKits.getErrorCode());
        }
    }

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {

        TrafficRadarUtils.Init();         // 初始化SDK库

        // 注册报警一般事件回调函数 回调函数必须保证不被JVM回收 推荐写成静态单例 dwUser可以不填 也不推荐使用
        // 事件订阅时需要传登录句柄 回调函数会回传获该句柄 回调的数据还会包含通道 可以依据这两者建立事件和设备、通道的映射关系
        netSdk.CLIENT_SetDVRMessCallBackEx1(messageCallBack, null);

        this.loginWithHighLevel();       // 高安全登录 推荐使用 部分旧设备可能不支持
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
        menu.addItem(new CaseMenu.Item(this, "订阅智能事件任务(RealLoadPic)", "AttachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "退订智能事件任务(StopLoadPic)", "DetachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "订阅报警事件任务(StartListen)", "AttachEventStartListen"));
        menu.addItem(new CaseMenu.Item(this, "退订报警事件任务(StopListen)", "DetachEventStopListen"));

        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        this.logout();  // 退出
        System.out.println("See You...");

        TrafficRadarUtils.cleanAndExit();  // 清理资源并退出
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
//    private String m_strIpAddr = "192.168.3.244";
    private String m_strIpAddr = "10.34.3.63";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        TrafficRadarDemoLauncher demo = new TrafficRadarDemoLauncher();

        if (args.length == 4) {
            demo.m_strIpAddr = args[0];
            demo.m_nPort = Integer.parseInt(args[1]);
            demo.m_strUser = args[2];
            demo.m_strPassword = args[3];
        }

        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
