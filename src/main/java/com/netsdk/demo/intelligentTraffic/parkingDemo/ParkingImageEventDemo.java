package com.netsdk.demo.intelligentTraffic.parkingDemo;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_PARKINGSPACE_STATE;
import com.netsdk.lib.structure.*;
import com.sun.jna.ptr.IntByReference;

import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.UUID;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 47040
 * @update 2020/11/19 舟曲项目
 * 1）事件抓图(SnapByEvent)
 * 2）设置停车位状态(setParkStatus)
 * 3）修改停车记录(modifyParkingInfo)
 * @since Created in 2020/7/20 16:28
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

    private NetSDKLib.fAnalyzerDataCallBack analyzerDataCB = ParkingAnalyzerDataCallBack.getSingleInstance();

    private int channel = 0;

    public void setChannelID() {
        System.out.println("请输入订阅通道,-1 表示全部");
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
     * 手动抓图 (ParkingSnapshot)
     */
    public void TestParkingSnapshot() {

        NetSDKLib.NET_IN_SNAP_MNG_SHOT stuInBuf = new NetSDKLib.NET_IN_SNAP_MNG_SHOT();
        stuInBuf.nChannel = channel;
        stuInBuf.nTime = 1; // 连拍次数, 0表示停止抓拍,正数表示连续抓拍的张数

        ParkingUtils.ParkingSnapshot(m_hLoginHandle, stuInBuf);
    }

    /**
     * 手动抓图 通用二代协议
     */
    public void TestParkingSnapshotCommon() {

        NetSDKLib.MANUAL_SNAP_PARAMETER snapParameter = new NetSDKLib.MANUAL_SNAP_PARAMETER();
        snapParameter.nChannel = channel;
        String sUuid = UUID.randomUUID().toString();
        byte[] bUuid = sUuid.getBytes();
        System.arraycopy(bUuid, 0, snapParameter.bySequence, 0, bUuid.length);
        int emType = NetSDKLib.CtrlType.CTRLTYPE_MANUAL_SNAP;
        snapParameter.write();
        boolean ret = netsdk.CLIENT_ControlDevice(m_hLoginHandle, emType, snapParameter.getPointer(), 3000);
        if (!ret) {
            System.err.println("手动抓图失败! " + ToolKits.getErrorCode());
            return;
        }
        System.out.println("手动抓图命令下发成功!");
    }


    /**
     * 下发车辆清理接口, 清除的车辆为 P1 的 “一位多车信息”中车辆（车位号，车牌号）
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

    /**
     * 指定事件抓图
     */
    public void TestSnapPictureByEvent() {

        NET_IN_SNAP_BY_EVENT stuParamIn = new NET_IN_SNAP_BY_EVENT();
        stuParamIn.nChannel = channel;    // Todo 通道号
        stuParamIn.dwEventID = NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACE_MANUALSNAP;  // 指定"路侧停车位手动抓图"事件
        byte[] serialNo = UUID.randomUUID().toString().getBytes();   // Todo 自定义序列号，用于匹配事件
        System.arraycopy(serialNo, 0, stuParamIn.szSerialNo, 0, serialNo.length);

        NET_OUT_SNAP_BY_EVENT stuParamOut = new NET_OUT_SNAP_BY_EVENT();

        stuParamIn.write();
        stuParamOut.write();
        boolean ret = netsdk.CLIENT_SnapPictureByEvent(m_hLoginHandle, stuParamIn.getPointer(), stuParamOut.getPointer(), 3000);
        if (!ret) {
            System.err.println("指定事件抓图指令下发失败: " + ToolKits.getErrorCode());
            return;
        }
        System.out.println("指定事件抓图指令下发成功。");
    }

    /**
     * 设置停车位状态
     */
    public void TestSetParkingStatus() throws UnsupportedEncodingException {

        // 入参
        NET_IN_SET_PARKINGSPACE_STATE_INFO stuParamIn = new NET_IN_SET_PARKINGSPACE_STATE_INFO();
        stuParamIn.nChannel = channel; // Todo 通道号
        byte[] plateNumber = "浙A826RW".getBytes(encode);  // Todo 车牌号
        System.arraycopy(plateNumber, 0, stuParamIn.szPlateNumber, 0, plateNumber.length);
        byte[] parkingNo = "A0001".getBytes(); // Todo 车位号
        System.arraycopy(parkingNo, 0, stuParamIn.szParkingNo, 0, parkingNo.length);
        stuParamIn.emState = EM_PARKINGSPACE_STATE.EM_PARKINGSPACE_STATE_PARKING.getValue();  // Todo 车位状态(是否有车)
        stuParamIn.bUnNeedPic = 1; // Todo 是否需要图片 0 不需要 1 需要
        // 出参
        NET_OUT_SET_PARKINGSPACE_STATE_INFO stuParamOut = new NET_OUT_SET_PARKINGSPACE_STATE_INFO();

        stuParamIn.write();
        stuParamOut.write();
        boolean ret = netsdk.CLIENT_SetParkingSpaceState(m_hLoginHandle, stuParamIn.getPointer(), stuParamOut.getPointer(), 3000);
        if (!ret) {
            System.err.println("设置停车位失败: " + ToolKits.getErrorCode());
            return;
        }
        System.out.println("设置停车位成功。");
    }

    /**
     * 修改停车记录
     */
    public void TestModifyParkingRecord() throws UnsupportedEncodingException {

        // 入参
        NET_IN_MODIFY_PARKINGRECORD_INFO stuParamIn = new NET_IN_MODIFY_PARKINGRECORD_INFO();
        //->旧的记录
        stuParamIn.stuOld.nChannel = channel;      // Todo 旧通道号
        byte[] oldPlateNumber = "浙A826RW".getBytes(encode);  // Todo 旧车牌号
        System.arraycopy(oldPlateNumber, 0, stuParamIn.stuOld.szPlateNumber, 0, oldPlateNumber.length);
        byte[] oldParkingNo = "A0001".getBytes(); // Todo 旧车位号
        System.arraycopy(oldParkingNo, 0, stuParamIn.stuOld.szParkingNo, 0, oldParkingNo.length);
        //->新的记录
        stuParamIn.stuNew.nChannel = channel;      // Todo 新通道号
        byte[] newPlateNumber = "沪C0M959".getBytes(encode);  // Todo 新车牌号
        System.arraycopy(newPlateNumber, 0, stuParamIn.stuNew.szPlateNumber, 0, newPlateNumber.length);
        byte[] newParkingNo = "A0002".getBytes();  // Todo 新车位号
        System.arraycopy(newParkingNo, 0, stuParamIn.stuNew.szParkingNo, 0, newParkingNo.length);

        // 出参
        NET_OUT_MODIFY_PARKINGRECORD_INFO stuParamOut = new NET_OUT_MODIFY_PARKINGRECORD_INFO();

        stuParamIn.write();
        stuParamOut.write();
        boolean ret = netsdk.CLIENT_ModifyParkingRecord(m_hLoginHandle, stuParamIn.getPointer(), stuParamOut.getPointer(), 3000);
        if (!ret) {
            System.err.println("修改停车记录失败: " + ToolKits.getErrorCode());
            return;
        }
        System.out.println("修改停车记录成功。");
    }

    // 相机状态侦听句柄
    private NetSDKLib.LLong m_hCameraStateHandle = new NetSDKLib.LLong(0); // 订阅相关

    // 订阅侦听相机状态
    public void AttachCameraState() {

        this.DetachCameraState();   // 先退订

        NET_IN_CAMERASTATE stuIn = new NET_IN_CAMERASTATE();
        stuIn.nChannels = 1;
        stuIn.pChannels = new IntByReference(-1).getPointer();  // 查全部
        stuIn.cbCamera = ParkingCameraStateCallBack.getSingleInstance();  // 注册回调

        NET_OUT_CAMERASTATE stuOut = new NET_OUT_CAMERASTATE();

        stuIn.write();
        stuOut.write();
        m_hCameraStateHandle =  netsdk.CLIENT_AttachCameraState(m_hLoginHandle, stuIn.getPointer(), stuOut.getPointer(), 3000);
        if (m_hCameraStateHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_AttachCameraState Success\n", -1);
        } else {
            System.out.printf("Ch[%d] CLIENT_AttachCameraState Failed!LastError = %s\n", -1, ToolKits.getErrorCode());
        }
    }

    // 停止订阅侦听相机状态
    public void DetachCameraState() {
        if (m_hCameraStateHandle.longValue() != 0) {
            netsdk.CLIENT_DetachCameraState(m_hCameraStateHandle);
        }
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
        menu.addItem(new CaseMenu.Item(this, "手动抓图(TrafficManualSnap)", "TestParkingSnapshot"));
        menu.addItem(new CaseMenu.Item(this, "手动抓图(通用 Snapshot 二代协议)", "TestParkingSnapshotCommon"));
        menu.addItem(new CaseMenu.Item(this, "获取停车超时配置", "TestParkingTimeoutGetConfig"));
        menu.addItem(new CaseMenu.Item(this, "设置停车超时配置", "TestParkingTimeoutSetConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发车辆清理接口", "TestRemoveParkingCarInfo"));

        //////////////////////////////////////////// 舟曲新增 ///////////////////////////////////////////////

        menu.addItem(new CaseMenu.Item(this, "事件抓图(SnapByEvent)", "TestSnapPictureByEvent"));
        menu.addItem(new CaseMenu.Item(this, "设置停车位状态(setParkStatus)", "TestSetParkingStatus"));
        menu.addItem(new CaseMenu.Item(this, "修改停车记录(modifyParkingInfo)", "TestModifyParkingRecord"));

        //////////////////////////////////////////// 摄像头状态主动回调 //////////////////////////////////////

        menu.addItem(new CaseMenu.Item(this, "摄像头状态订阅(AttachCameraState)", "AttachCameraState"));
        menu.addItem(new CaseMenu.Item(this, "摄像头状态退订(DetachCameraState)", "DetachCameraState"));


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
//    private String m_strIpAddr = "172.24.9.9";   // 盒子
//    private String m_strIpAddr = "172.24.3.56";   // 盒子
    private String m_strIpAddr = "10.80.9.45"; // 盒子
//    private String m_strIpAddr = "10.80.9.162"; // 相机
//    private String m_strIpAddr = "172.27.1.102"; // 相机
//    private String m_strIpAddr = "10.34.3.63";    // 模拟器

    private int m_nPort = 37777;
    private String m_strUser = "admin";
//    private String m_strPassword = "admin123";
//    private String m_strPassword = "admin123B";
    private String m_strPassword = "dahua2020";
//    private String m_strPassword = "admin";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        ParkingImageEventDemo demo = new ParkingImageEventDemo();

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
