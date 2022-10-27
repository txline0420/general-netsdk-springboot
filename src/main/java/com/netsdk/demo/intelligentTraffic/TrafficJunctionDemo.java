package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.demo.util.EventTaskHandler;
import com.netsdk.demo.util.QueueGeneration;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.sun.jna.Pointer;

import java.io.*;
import java.util.Scanner;
import java.util.UUID;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/8/31 10:26
 */
public class TrafficJunctionDemo {

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

        TrafficJunctionDemo.enableLog();  // 配置日志

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


    //////////////////////////////////////// 回调子函数 /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 智能事件回调
     */
    public static class DefaultAnalyzerDataCallBack implements NetSDKLib.fAnalyzerDataCallBack {

        private static DefaultAnalyzerDataCallBack singleInstance;

        public static DefaultAnalyzerDataCallBack getSingleInstance() {
            if (singleInstance == null) {
                singleInstance = new DefaultAnalyzerDataCallBack();
            }
            return singleInstance;
        }

        public static String encode;

        static {
            String osPrefix = getOsPrefix();
            if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
                encode = "GBK";
            } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
                encode = "UTF-8";
            }
        }

        private QueueGeneration eventCBQueueService = new QueueGeneration();    // 保存图片用队列

        public DefaultAnalyzerDataCallBack() {
            eventCBQueueService.init();
        }

        private final String imageSaveFolder = "TrafficPic/";      // 图片保存路径

        @Override
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved) {
            switch (dwAlarmType) {
                case NetSDKLib.EVENT_IVS_TRAFFICJUNCTION: ///< 路口事件
                {
                    System.out.println("\n\n<Event> TRAFFIC [ JUNCTION 路口事件]");

                    NetSDKLib.DEV_EVENT_TRAFFICJUNCTION_INFO stuJunctionInfo = new NetSDKLib.DEV_EVENT_TRAFFICJUNCTION_INFO();
                    ToolKits.GetPointerDataToStruct(pAlarmInfo, 0, stuJunctionInfo);

                    ////////////////////////////////// 卡口事件主要信息 ///////////////////////////
                    ////////////////////////////////////////////////////////////////////////////

                    // 事件发生时间
                    StringBuilder mainInfo = new StringBuilder()
                            .append("<---------------卡口事件主要信息-------------->").append("\n")
                            .append("车道号: ").append(stuJunctionInfo.nLane).append("\n")
                            .append("过车时间(毫秒时间戳): ").append(stuJunctionInfo.PTS).append("\n")
                            .append("交通信号灯状态(0 未知,1 绿灯,2 红灯,3 黄灯): ").append(stuJunctionInfo.byLightState).append("\n")
                            .append("违反规则掩码: ").append(stuJunctionInfo.dwBreakingRule).append("\n");
                    System.out.println(mainInfo.toString());

                    ////////////////////////////////// 卡口车辆信息 //////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////

                    String uuid = UUID.randomUUID().toString();
                    //<-----存在机动车
                    if (stuJunctionInfo.bNonMotorInfoEx == 0) {
                        try {
                            /// 机动车主要信息
                            StringBuilder trafficCarInfo = new StringBuilder()
                                    .append("<<-----车辆主要信息----->>").append("\n")
                                    .append("车道: ").append(stuJunctionInfo.stTrafficCar.nLane).append("\n")
                                    .append("车牌: ").append(new String(stuJunctionInfo.stTrafficCar.szPlateNumber, encode).trim()).append("\n")
                                    .append("车牌颜色: ").append(new String(stuJunctionInfo.stTrafficCar.szPlateColor, encode).trim()).append("\n")
                                    .append("车速: ").append(stuJunctionInfo.stTrafficCar.nSpeed).append("\n")
                                    .append("车标: ").append(new String(stuJunctionInfo.stuVehicle.szText, encode).trim()).append("\n")
                                    .append("车辆类型: ").append(new String(stuJunctionInfo.stuVehicle.szObjectSubType, encode).trim()).append("\n")
                                    .append("违规代码: ").append(new String(stuJunctionInfo.stTrafficCar.szViolationCode, encode).trim()).append("\n")
                                    .append("违规描述: ").append(new String(stuJunctionInfo.stTrafficCar.szViolationDesc, encode).trim()).append("\n");
                            System.out.println(trafficCarInfo.toString());
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        /// 车牌,车辆图相对坐标
                        System.out.printf("车牌图相对坐标：Top=%d,Left=%d;Bottom=%d,Right=%d\n",
                                stuJunctionInfo.stuObject.BoundingBox.top.longValue(),
                                stuJunctionInfo.stuObject.BoundingBox.left.longValue(),
                                stuJunctionInfo.stuObject.BoundingBox.bottom.longValue(),
                                stuJunctionInfo.stuObject.BoundingBox.right.longValue());
                        System.out.printf("车辆图相对坐标：Top=%d,Left=%d;Bottom=%d,Right=%d\n",
                                stuJunctionInfo.stuVehicle.BoundingBox.top.longValue(),
                                stuJunctionInfo.stuVehicle.BoundingBox.left.longValue(),
                                stuJunctionInfo.stuVehicle.BoundingBox.bottom.longValue(),
                                stuJunctionInfo.stuVehicle.BoundingBox.right.longValue());

                        /// 附加物体信息(悬挂物也在这里)
                        StringBuilder attachObjects = new StringBuilder();
                        int nAttachObject = stuJunctionInfo.stCommInfo.nAttachmentNum;

                        if (nAttachObject > 0)
                            attachObjects.append("<<-----车辆附加物体信息----->>");
                        for (int i = 0; i < nAttachObject; i++) {
                            attachObjects.append("\n").append("第 ").append(i + 1).append(" 个附加物：")
                                    .append("\n").append("   类型->").append(parseAttachmentType(stuJunctionInfo.stCommInfo.stuAttachment[i].emAttachmentType))
                                    .append("\n").append("   相对坐标->").append(
                                    String.format("Top=%d,Left=%d;Bottom=%d,Right=%d",
                                            stuJunctionInfo.stCommInfo.stuAttachment[i].stuRect.top,
                                            stuJunctionInfo.stCommInfo.stuAttachment[i].stuRect.left,
                                            stuJunctionInfo.stCommInfo.stuAttachment[i].stuRect.bottom,
                                            stuJunctionInfo.stCommInfo.stuAttachment[i].stuRect.right));
                        }
                        System.out.println(attachObjects.toString());

                        /// 车辆图保存
                        if (stuJunctionInfo.stuVehicle.bPicEnble == 1 && (stuJunctionInfo.stuVehicle.stPicInfo.dwFileLenth > 0)) {
                            String imgPath = null;
                            try {
                                imgPath = String.format("TrafficPic/机动车-车辆图-%s_%d_%d_%d_%s.jpg",
                                        new String(stuJunctionInfo.stuObject.szText, encode).trim(),
                                        stuJunctionInfo.stuFileInfo.nGroupId,
                                        stuJunctionInfo.stuFileInfo.bCount,
                                        stuJunctionInfo.stuFileInfo.bIndex,
                                        uuid);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            int imgOffset = stuJunctionInfo.stuVehicle.stPicInfo.dwOffSet;
                            int imgLength = stuJunctionInfo.stuVehicle.stPicInfo.dwFileLenth;

                            // 保存图片
                            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, imgOffset, imgLength, imgPath));
                        }
                        /// 车牌图保存
                        if (stuJunctionInfo.stuObject.bPicEnble == 1 && (stuJunctionInfo.stuObject.stPicInfo.dwFileLenth > 0)) {
                            String imgPath = null;
                            try {
                                imgPath = String.format("TrafficPic/机动车-车牌图-%s_%d_%d_%d_%s.jpg",
                                        new String(stuJunctionInfo.stuObject.szText, encode).trim(),
                                        stuJunctionInfo.stuFileInfo.nGroupId,
                                        stuJunctionInfo.stuFileInfo.bCount,
                                        stuJunctionInfo.stuFileInfo.bIndex,
                                        uuid);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            int imgOffset = stuJunctionInfo.stuObject.stPicInfo.dwOffSet;
                            int imgLength = stuJunctionInfo.stuObject.stPicInfo.dwFileLenth;

                            // 保存图片
                            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, imgOffset, imgLength, imgPath));
                        }

                        /// 保存驾驶室人脸图，副驾驶室有人的话，有图片。
                        int nDriversNum = stuJunctionInfo.stCommInfo.nDriversNum;
                        NetSDKLib.NET_MSG_OBJECT_EX[] driverInfos = new NetSDKLib.NET_MSG_OBJECT_EX[nDriversNum];
                        for (int i = 0; i < nDriversNum; i++)
                            driverInfos[i] = new NetSDKLib.NET_MSG_OBJECT_EX();
                        ToolKits.GetPointerDataToStructArr(stuJunctionInfo.stCommInfo.pstDriversInfo, driverInfos);

                        for (int i = 0; i < nDriversNum; i++) {
                            String imgPath = null;
                            try {
                                if (i == 0) {  // 第一张是主驾驶人脸图
                                    imgPath = String.format("TrafficPic/机动车-主驾驶室图-%s_%d_%d_%s.jpg",
                                            new String(stuJunctionInfo.stuObject.szText, encode).trim(),
                                            stuJunctionInfo.stuFileInfo.nGroupId, i, uuid);
                                } else {       // 剩下是副驾驶的
                                    imgPath = String.format("TrafficPic/机动车-副驾驶室图-%s_%d_%d_%s.jpg",
                                            new String(stuJunctionInfo.stuObject.szText, encode).trim(),
                                            stuJunctionInfo.stuFileInfo.nGroupId, i, uuid);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (driverInfos[i].bPicEnble == 1) {
                                int imgOffset = driverInfos[i].stPicInfo.dwOffSet;
                                int imgLength = driverInfos[i].stPicInfo.dwFileLenth;

                                // 保存图片
                                eventCBQueueService.addEvent(new SavePicHandler(pBuffer, imgOffset, imgLength, imgPath));
                            }
                        }
                    } else {     //<-----存在非机动车
                        /// 保存车身图
                        if (stuJunctionInfo.stuNonMotor.stuImage.uLength > 0) {

                            String imgPath = String.format("TrafficPic/非机动车-车身图-%d_%s.jpg", stuJunctionInfo.stuNonMotor.nObjectID, uuid);

                            int imgOffset = stuJunctionInfo.stuNonMotor.stuImage.uOffset;
                            int imgLength = stuJunctionInfo.stuNonMotor.stuImage.uLength;

                            // 保存图片
                            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, imgOffset, imgLength, imgPath));
                        }
                        /// 保存全景广角图
                        if (stuJunctionInfo.stuNonMotor.stuSceneImage.nLength > 0) {

                            String imgPath = String.format("TrafficPic/非机动车-场景图-%d_%s.jpg", stuJunctionInfo.stuNonMotor.nObjectID, uuid);

                            int imgOffset = stuJunctionInfo.stuNonMotor.stuSceneImage.nOffSet;
                            int imgLength = stuJunctionInfo.stuNonMotor.stuSceneImage.nLength;

                            // 保存图片
                            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, imgOffset, imgLength, imgPath));
                        }
                        /// 人脸全景广角图
                        if (stuJunctionInfo.stuNonMotor.stuFaceSceneImage.nLength > 0) {

                            String imgPath = String.format("TrafficPic/非机动车-场景图-%d_%s.jpg", stuJunctionInfo.stuNonMotor.nObjectID, uuid);

                            int imgOffset = stuJunctionInfo.stuNonMotor.stuFaceSceneImage.nOffSet;
                            int imgLength = stuJunctionInfo.stuNonMotor.stuFaceSceneImage.nLength;

                            // 保存图片
                            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, imgOffset, imgLength, imgPath));
                        }

                        int nNumOfCycling = stuJunctionInfo.stuNonMotor.nNumOfCycling;
                        System.out.println("非机动车骑行人员数量：" + nNumOfCycling);

                        if (nNumOfCycling > 0) {

                            for (int i = 0; i < nNumOfCycling; i++) {
                                System.out.println("骑行人员序号:" + (i + 1));

                                if (stuJunctionInfo.stuNonMotor.stuRiderList[i].stuFaceImage.uLength > 0) {

                                    String imgPath = String.format("TrafficPic/非机动车-骑行人员%d_%d.jpg", stuJunctionInfo.stuNonMotor.nObjectID, (i + 1));

                                    int imgOffset = stuJunctionInfo.stuNonMotor.stuRiderList[i].stuFaceImage.uOffset;
                                    int imgLength = stuJunctionInfo.stuNonMotor.stuRiderList[i].stuFaceImage.uLength;

                                    // 保存图片
                                    eventCBQueueService.addEvent(new SavePicHandler(pBuffer, imgOffset, imgLength, imgPath));
                                }
                            }
                        }
                    }

                    //<-----原始图片
                    String rawImgPath = String.format("TrafficPic/交通路口事件-原始图-%s_%s.jpg", stuJunctionInfo.UTC.toString().replaceAll("[^0-9]", "-"), uuid);
                    eventCBQueueService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, rawImgPath));

                    break;
                }
                default:
                    System.out.printf("Get Event 0x%x\n", dwAlarmType);
                    break;
            }
            return 0;
        }

        // 获取附加物类型名称
        private String parseAttachmentType(int emType) {

            String emName;
            switch (emType) {
                case NetSDKLib.EM_COMM_ATTACHMENT_TYPE.COMM_ATTACHMENT_TYPE_UNKNOWN:
                    emName = "未知类型";
                    break;
                case NetSDKLib.EM_COMM_ATTACHMENT_TYPE.COMM_ATTACHMENT_TYPE_FURNITURE:
                    emName = "摆件";
                    break;
                case NetSDKLib.EM_COMM_ATTACHMENT_TYPE.COMM_ATTACHMENT_TYPE_PENDANT:
                    emName = "挂件";
                    break;
                case NetSDKLib.EM_COMM_ATTACHMENT_TYPE.COMM_ATTACHMENT_TYPE_TISSUEBOX:
                    emName = "纸巾盒";
                    break;
                case NetSDKLib.EM_COMM_ATTACHMENT_TYPE.COMM_ATTACHMENT_TYPE_DANGER:
                    emName = "危险品";
                    break;
                case NetSDKLib.EM_COMM_ATTACHMENT_TYPE.COMM_ATTACHMENT_TYPE_PERFUMEBOX:
                    emName = "香水";
                    break;
                default:
                    emName = "类型数据有错误";
                    break;
            }
            return emName;
        }

        private class SavePicHandler implements EventTaskHandler {
            private static final long serialVersionUID = 1L;

            private final byte[] imgBuffer;
            private final int length;
            private final String savePath;

            public SavePicHandler(Pointer pBuf, int dwBufOffset, int dwBufSize, String sDstFile) {

                this.imgBuffer = pBuf.getByteArray(dwBufOffset, dwBufSize);
                this.length = dwBufSize;
                this.savePath = sDstFile;
            }

            @Override
            public void eventCallBackProcess() {
                System.out.println("保存图片中...路径：" + savePath);
                File path = new File(imageSaveFolder);
                if (!path.exists()) path.mkdir();
                try {
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(savePath)));
                    out.write(imgBuffer, 0, length);
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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


    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {

        TrafficJunctionDemo.Init();         // 初始化SDK库
        this.loginWithHighLevel();   // 高安全登录
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "订阅任务", "AttachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "退订任务", "DetachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        this.logOut();  // 退出
        System.out.println("See You...");

        TrafficJunctionDemo.cleanAndExit();  // 清理资源并退出
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
//    private String m_strIpAddr = "10.34.3.63";
    private String m_strIpAddr = "192.168.129.115";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    //    private String m_strPassword = "admin";
    private String m_strPassword = "admin123";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        TrafficJunctionDemo demo = new TrafficJunctionDemo();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
