package com.netsdk.demo.example.parkingDemo;

import com.netsdk.demo.example.parkingDemo.callback.DefaultAnalyzerDataCallBack;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;

import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/20 16:28
 */
public class ParkingImageEventDemo {

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

    private NetSDKLib.LLong m_hAttachHandle = new NetSDKLib.LLong(0); // 订阅相关

    private NetSDKLib.fAnalyzerDataCallBack analyzerDataCB = DefaultAnalyzerDataCallBack.getSingleInstance();

    private int channel = 0;

    public void setChannelID() {
        Scanner sc = new Scanner(System.in);
        this.channel = sc.nextInt();
    }

    public void AttachEventRealLoadPic() {

        this.DetachEventRealLoadPic();   // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回

        int bNeedPicture = 1;   // 需要图片

        m_hAttachHandle = netsdk.CLIENT_RealLoadPictureEx(m_hLoginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, analyzerDataCB, null, null);
        if (m_hAttachHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel, ToolKits.getErrorCode());
        }
    }

    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (m_hAttachHandle.longValue() != 0) {
            netsdk.CLIENT_StopLoadPic(m_hAttachHandle);
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

    /**
     * 设置停车超时配置
     */
    public void TestParkingTimeoutSetConfig() {

        NetSDKLib.NET_CFG_PARKING_TIMEOUT_DETECT timeoutDetectConfig = new NetSDKLib.NET_CFG_PARKING_TIMEOUT_DETECT();
        timeoutDetectConfig.bEnable = 1;
        timeoutDetectConfig.nParkingTime = 302400;
        ParkingUtils.ParkingTimeoutSetConfig(m_hLoginHandle, timeoutDetectConfig);
    }


    /**
     * 手动抓图
     */
    public void TestParkingSnapshot() {

        NetSDKLib.NET_IN_SNAP_MNG_SHOT stuInBuf = new NetSDKLib.NET_IN_SNAP_MNG_SHOT();
        stuInBuf.nChannel = channel;
        stuInBuf.nTime = 1; // 连拍次数, 0表示停止抓拍,正数表示连续抓拍的张数

        ParkingUtils.ParkingSnapshot(m_hLoginHandle, stuInBuf);
    }


    /**
     * 下发车辆清理接口, 清除的车辆为 1 的“一位多车信息”中车辆（车位号，车牌号）
     */
    public void TestRemoveParkingCarInfo() throws UnsupportedEncodingException {
        NetSDKLib.NET_IN_REMOVE_PARKING_CAR_INFO stuInBuf = new NetSDKLib.NET_IN_REMOVE_PARKING_CAR_INFO();
        NetSDKLib.DEV_OCCUPIED_WARNING_INFO stuParkingCarInfo = stuInBuf.stuParkingCarInfo;

        byte[] szParkingNo = "P1".getBytes();
        System.arraycopy(szParkingNo, 0, stuParkingCarInfo.szParkingNo, 0, szParkingNo.length);

        // 设备会对以下的数据作严格校验，如果不存在会返回校验错误，另外务必注意编码格式 encode

        stuParkingCarInfo.nPlateNumber = 3;

        byte[] iSzPlatNumber1 = "皖SLV662".getBytes(encode);
        System.arraycopy(iSzPlatNumber1, 0, stuParkingCarInfo.szPlateNumber[0].plateNumber, 0, iSzPlatNumber1.length);
        byte[] iSzPlatNumber2 = "浙A826RW".getBytes(encode);
        System.arraycopy(iSzPlatNumber2, 0, stuParkingCarInfo.szPlateNumber[1].plateNumber, 0, iSzPlatNumber2.length);
        byte[] iSzPlatNumber3 = "沪C0M959".getBytes(encode);
        System.arraycopy(iSzPlatNumber3, 0, stuParkingCarInfo.szPlateNumber[2].plateNumber, 0, iSzPlatNumber3.length);

        ParkingUtils.RemoveParkingCarInfo(m_hLoginHandle, stuInBuf);
    }


    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {

        ParkingUtils.Init();         // 初始化SDK库
        this.loginWithHighLevel();   // 高安全登录
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "订阅任务", "AttachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "退订任务", "DetachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
        menu.addItem(new CaseMenu.Item(this, "手动抓图", "TestParkingSnapshot"));
        menu.addItem(new CaseMenu.Item(this, "获取停车超时配置", "TestParkingTimeoutGetConfig"));
        menu.addItem(new CaseMenu.Item(this, "设置停车超时配置", "TestParkingTimeoutSetConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发车辆清理接口", "TestRemoveParkingCarInfo"));
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
//    private String m_strIpAddr = "172.24.0.118";
//    private String m_strIpAddr = "171.2.7.212";
    private String m_strIpAddr = "10.34.3.63";
    //    private String m_strIpAddr = "172.25.239.22";
//    private String m_strIpAddr = "10.172.177.239";
//    private int m_nPort = 37777;
    private int m_nPort = 37778;
    private String m_strUser = "admin";
//    private String m_strPassword = "admin123";
    private String m_strPassword = "admin";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        ParkingImageEventDemo demo = new ParkingImageEventDemo();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }

}
