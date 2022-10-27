package com.netsdk.demo.intelligentTraffic.parkingDemo;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;

import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import static com.netsdk.lib.NetSDKLib.CFG_CMD_CHANNELTITLE;
import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/21 15:56
 */
public class ParkingAlarmEventDemoEx1 {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    ////////////////////////////////////// 登录相关 ///////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登录句柄

    /**
     * login with high level 高安全级别登陆
     */
    public void loginWithHighLevel() {

        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {{
                    szIP = m_strIpAddr.getBytes();
                    nPort = m_nPort;
                    szUserName = m_strUser.getBytes();
                    szPassword = m_strPassword.getBytes();
                }};   // 输入结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输结构体参数

        // 写入sdk
        m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
                    netsdk.CLIENT_GetLastError());
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + m_strIpAddr);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }
    }

    /**
     * logout 退出
     */
    public void logOut() {
        if (m_hLoginHandle.longValue() != 0) {
            netsdk.CLIENT_Logout(m_hLoginHandle);
            System.out.println("LogOut Success");
        }
    }

    //////////////////////////////////////// 订阅事件/退订 ///////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    private NetSDKLib.fMessCallBackEx1 messCallBackEx1 = ParkingMessageCallBackEx1.getSingleInstance();

    private int channel = 0;

    public void setChannelID() {
        Scanner sc = new Scanner(System.in);
        this.channel = sc.nextInt();
    }

    /**
     * 订阅任务， start listen
     */
    public void AttachEventStartListen() {
        boolean bRet = netsdk.CLIENT_StartListenEx(m_hLoginHandle);
        if (bRet) {
            System.out.println("CLIENT_StartListenEx success.");
        } else {
            System.out.printf("CLIENT_StartListenEx fail, error:%s\n", ToolKits.getErrorCode());
        }
    }

    /**
     * 退订任务， stop listen
     */
    public void DetachEventStopListen() {
        boolean bRet = netsdk.CLIENT_StopListen(m_hLoginHandle);
        if (bRet) {
            System.out.println("CLIENT_StopListen success");
        } else {
            System.out.printf("CLIENT_StopListen fail, error:%s\n", ToolKits.getErrorCode());
        }
    }

    /////////////////////////////////////// RPC 接口 ////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 获取停车超时配置
     */
    public void TestParkingTimeoutGetConfig() {

        NetSDKLib.NET_CFG_PARKING_TIMEOUT_DETECT timeoutDetectConfig = new NetSDKLib.NET_CFG_PARKING_TIMEOUT_DETECT();
        boolean ret = ParkingUtils.ParkingTimeoutGetConfig(m_hLoginHandle, timeoutDetectConfig);
        if (!ret) return;
        StringBuilder builder = new StringBuilder()
                .append("<<-----Parking Timeout Config----->>").append("\n")
                .append("bEnable: ").append(timeoutDetectConfig.bEnable).append("\n")
                .append("nParkingTime: ").append(timeoutDetectConfig.nParkingTime);
        System.out.println(builder.toString());
    }

    // 获取所有通道名称
    public void getChannelTitleConfig() {

        NetSDKLib.AV_CFG_ChannelName info = new NetSDKLib.AV_CFG_ChannelName();

        for (int i = 0; i < deviceInfo.byChanNum; i++) {

            // m_hLoginHandle 登录句柄, i 通道, CFG_CMD_CHANNELTITLE 配置枚举, info 入/出 参
            boolean ret = ToolKits.GetDevConfig(m_hLoginHandle, i, CFG_CMD_CHANNELTITLE, info);

            if (ret) {
                System.out.println("设备唯一编号：" + info.nSerial);
                try {
                    System.out.println(new String(info.szName, encode));     // 这里获取名称
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 设置停车超时配置
     */
    public void TestParkingTimeoutSetConfig() {

        NetSDKLib.NET_CFG_PARKING_TIMEOUT_DETECT timeoutDetectConfig = new NetSDKLib.NET_CFG_PARKING_TIMEOUT_DETECT();
        timeoutDetectConfig.bEnable = 1;
        timeoutDetectConfig.nParkingTime = 302400;    // 3600-604800 秒
        ParkingUtils.ParkingTimeoutSetConfig(m_hLoginHandle, timeoutDetectConfig);
    }

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {

        ParkingUtils.Init(); // 初始化SDK库
        netsdk.CLIENT_SetDVRMessCallBackEx1(messCallBackEx1, null);   // 注册一般事件回调函数
        this.loginWithHighLevel();   // 高安全登录
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "订阅任务", "AttachEventStartListen"));
        menu.addItem(new CaseMenu.Item(this, "退订任务", "DetachEventStopListen"));
        menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
        menu.addItem(new CaseMenu.Item(this, "获取停车超时配置", "TestParkingTimeoutGetConfig"));
        menu.addItem(new CaseMenu.Item(this, "设置停车超时配置", "TestParkingTimeoutSetConfig"));
        menu.addItem(new CaseMenu.Item(this, "获取通道名称", "getChannelTitleConfig"));
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        this.logOut();  // 退出
        System.out.println("See You...");

        ParkingUtils.cleanAndExit();  // 清理资源并退出
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
//    private String m_strIpAddr = "172.24.3.57";
    private String m_strIpAddr = "172.23.12.29";
    //    private String m_strIpAddr = "10.34.3.63";
    private int m_nPort = 37777;
    //    private int m_nPort = 37778;
    private String m_strUser = "admin";
    private String m_strPassword = "admin11111";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        ParkingAlarmEventDemoEx1 demo = new ParkingAlarmEventDemoEx1();

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
