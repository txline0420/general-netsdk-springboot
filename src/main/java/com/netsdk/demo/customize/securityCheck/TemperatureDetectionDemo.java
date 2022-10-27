package com.netsdk.demo.customize.securityCheck;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.structure.DEV_EVENT_SECURITYGATE_PERSONALARM_INFO;
import com.netsdk.lib.structure.NET_SECURITYGATE_ALARM_FACE_INFO;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Objects;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 291189
 * @version 1.0
 * @description 安检门温度检测 安检门事件
 * @date 2021/6/30
 */
public class TemperatureDetectionDemo {

    /**
     * 安检门
     */
    private String m_strIpAddr 				    = "10.35.232.160";
    private int    m_nPort 				    = 37777;
    private String m_strUser 			    = "admin";
    private String m_strPassword 		    = "admin123";


    static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;


    // public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    // 登陆句柄
    private static NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 订阅句柄
    private static NetSDKLib.LLong AttachHandle = new NetSDKLib.LLong(0);

    // 设备信息扩展
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    /**
     * 设备断线回调
     */
    private static class DisconnectCallback implements NetSDKLib.fDisConnect {
        private static DisconnectCallback instance = new DisconnectCallback();

        private DisconnectCallback() {
        }

        public static DisconnectCallback getInstance() {
            return instance;
        }

        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s:%d] Disconnect!\n", pchDVRIP, nDVRPort);
        }
    }
    /**
     * 设备重连回调
     */
    private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
        private static HaveReconnectCallback instance = new HaveReconnectCallback();

        private HaveReconnectCallback() {
        }

        public static HaveReconnectCallback getInstance() {
            return instance;
        }

        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s:%d] HaveReconnected!\n", pchDVRIP, nDVRPort);
        }
    }


    public void InitTest(){
        // 初始化SDK库
        netSdk.CLIENT_Init(DisconnectCallback.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);

        //打开日志，可选0
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)){
            System.err.println("Open SDK Log Failed!!!");
        }

        loginWithHighLevel();
    }
    /**
     * 高安全登录
     */
    public void loginWithHighLevel() {
        // 输入结构体参数
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
            {
                szIP = m_strIpAddr.getBytes();
                nPort = m_nPort;
                szUserName = m_strUser.getBytes();
                szPassword = m_strPassword.getBytes();
            }
        };
        // 输出结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        // 写入sdk
        loginHandle = netSdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
        if (loginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
                    netSdk.CLIENT_GetLastError());
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + m_strIpAddr);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }
    }

    /**
     * 退出清理环境
     */
    public void LoginOut(){
        System.out.println("End Test");
        if( loginHandle.longValue() != 0)
        {
            netSdk.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }


    // 编码格式
    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win64-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }


    /**
     * 选择通道
     */
    private int channel = 0;


    /**
     * 订阅智能任务
     */
    public void attachIVSEvent() {

            // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
            this.DetachEventRealLoadPic();
            // 需要图片
            int bNeedPicture = 1;
            //订阅所有事件
            AttachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
                    AnalyzerDataCB.getInstance(), null, null);
            if (AttachHandle.longValue() != 0) {
                System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
            } else {
                System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
                        ToolKits.getErrorCode());
            }

    }


    /**
     * 报警事件（智能）回调
     */
    private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        private final File picturePath;
        private static AnalyzerDataCB instance;

        private AnalyzerDataCB() {
            picturePath = new File("./AnalyzerPicture/");
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }
        }

        public static AnalyzerDataCB getInstance() {
            if (instance == null) {
                synchronized (AnalyzerDataCB.class) {
                    if (instance == null) {
                        instance = new AnalyzerDataCB();
                    }
                }
            }
            return instance;
        }

        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }

            switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
                case EVENT_IVS_ANATOMY_TEMP_DETECT: {// 人体温智能检测事件(对应 DEV_EVENT_ANATOMY_TEMP_DETECT_INFO)

                   NetSDKLib.DEV_EVENT_ANATOMY_TEMP_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_ANATOMY_TEMP_DETECT_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("人体温智能检测事件");

                    // 通道号
                    int nChannelID = msg.nChannelID;

                    System.out.println("nChannelID:"+nChannelID);

                    // 1:开始 2:停止
                    int nAction = msg.nAction;

                    System.out.println("nAction:"+nAction);

                    // 事件名称
                    byte[] szName = msg.szName;
                    try {
                        String name = new String(szName, "gbk");
                        System.out.println("szName:"+name);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }



                    // 时间戳(单位是毫秒)
                    double pts = msg.PTS;

                    System.out.println("pts:"+pts);

                    // 事件发生的时间
                    NetSDKLib.NET_TIME_EX utc = msg.UTC;

                    System.out.println("utc:"+utc);

                    // 事件ID
                    int nEventID = msg.nEventID;

                    System.out.println("nEventID:"+nEventID);


                    //智能事件所属大类(对应枚举类型EM_CLASS_TYPE)
                    int emClassType = msg.emClassType;

                    System.out.println("emClassType:"+emClassType);

                    // 事件触发的预置点号, 从1开始, 没有该字段,表示预置点未知
                    int nPresetID = msg.nPresetID;
                    System.out.println("nPresetID:"+nPresetID);


                    NetSDKLib.NET_MAN_TEMP_INFO stManTempInfo = msg.stManTempInfo;

                    System.out.printf("【区域内人员体温信息】:nObjectID="+stManTempInfo.nObjectID+",dbHighTemp="
                            +stManTempInfo.dbHighTemp+",nTempUnit="+stManTempInfo.nTempUnit
                            +",bIsOverTemp="+stManTempInfo.bIsOverTemp+",bIsUnderTemp="
                            +stManTempInfo.bIsUnderTemp+",emMaskDetectResult="+stManTempInfo.emMaskDetectResult+
                            ",nAge="+stManTempInfo.nAge+",emSex="+stManTempInfo.emSex
                            +",stThermalRect_top="+stManTempInfo.stThermalRect.top
                            +",stThermalRect_left="+stManTempInfo.stThermalRect.left
                            +",stThermalRect_right="+stManTempInfo.stThermalRect.right
                            +",stThermalRect_bottom="+stManTempInfo.stThermalRect.bottom+"\n");

                    //可见光全景图
                    if(msg.stVisSceneImage!=null && msg.stVisSceneImage.nLength> 0){
                        String visibleLightPicture = picturePath + "\\" + System.currentTimeMillis() + "visibleLight.jpg";
                        ToolKits.savePicture(pBuffer, msg.stVisSceneImage.nOffset,  msg.stVisSceneImage.nLength, visibleLightPicture);
                    }
                    //热成像全景图
                    if(msg.stThermalSceneImage!=null && msg.stThermalSceneImage.nLength> 0){
                        String thermographyPicture = picturePath + "\\" + System.currentTimeMillis() + "thermography.jpg";
                        ToolKits.savePicture(pBuffer, msg.stThermalSceneImage.nOffset, msg.stThermalSceneImage.nLength, thermographyPicture);
                    }

                    break;
                }
                case EVENT_IVS_SECURITYGATE_PERSONALARM: { //安检门人员报警事件

                    System.out.println("安检门人员报警事件");

                    DEV_EVENT_SECURITYGATE_PERSONALARM_INFO msg = new DEV_EVENT_SECURITYGATE_PERSONALARM_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    // 通道号
                    int nChannelID = msg.nChannelID;

                    System.out.println("nChannelID:" + nChannelID);

                    // 0:脉冲 1:开始 2:停止
                    int nAction = msg.nAction;

                    System.out.println("nAction:" + nAction);

                    // 事件名称
                    byte[] szName = msg.szName;
                    try {
                        String name = new String(szName, "gbk");
                        System.out.println("szName:" + name);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    // 时间戳(单位是毫秒)
                    double pts = msg.PTS;

                    System.out.println("pts:" + pts);

                    // 事件发生的时间
                    NET_TIME_EX utc = msg.UTC;

                    System.out.println("utc:" + utc);


                    // 事件ID
                    int nEventID = msg.nEventID;

                    System.out.println("nEventID:" + nEventID);

                    /**
                     *  人员通过方向枚举,参考枚举{@link com.netsdk.lib.enumeration.EM_SECURITYGATE_PERSON_PASS_DIRECTION }
                     */
                    int emDirection = msg.emDirection;

                    System.out.println("emDirection:" + emDirection);

                    /**
                     *  报警级别,参考枚举{@link com.netsdk.lib.enumeration.EM_SECURITYGATE_ALARM_LEVEL }
                     */
                    int emAlarmLevel = msg.emAlarmLevel;

                    System.out.println("emAlarmLevel:" + emAlarmLevel);

                    // 关联进入通道
                    int nChannelIn = msg.nChannelIn;
                    System.out.println("nChannelIn:" + nChannelIn);

                    // 关联离开通道
                    int channelOut = msg.ChannelOut;
                    System.out.println("ChannelOut:" + channelOut);

                    // 报警位置个数
                    int nAlarmPositionNum = msg.nAlarmPositionNum;
                    System.out.println("nAlarmPositionNum:" + nAlarmPositionNum);


                    //  人脸信息
                    NET_SECURITYGATE_ALARM_FACE_INFO faceInfo = msg.stuSecurityGateFaceInfo;
                    System.out.println("【人脸信息】:emSex=" + faceInfo.emSex + ",nAge=" + faceInfo.nAge + ",emEmotion="
                            + faceInfo.emEmotion + ",emGlasses=" + faceInfo.emGlasses + ",emMask=" + faceInfo.emMask + ",emBeard=" +
                            faceInfo.emBeard + ",nAttractive=" + faceInfo.nAttractive 
                            + ",emMouth=" + faceInfo.emMouth + ",emEye=" + faceInfo.emEye + ",fTemperature=" + faceInfo.fTemperature + ",emTempUnit="
                            + faceInfo.emTempUnit + ",emTempType=" + faceInfo.emTempType);


                    /**
                     *  报警位置,参考枚举{@link com.netsdk.lib.enumeration.EM_SECURITYGATE_ALARM_POSITION }
                     */
                    // 报警位置
                    int[] emAlarmPosition = msg.emAlarmPosition;

                    for (int i = 0; i < emAlarmPosition.length; i++) {
                        System.out.println("报警位置:" + i + "[" + emAlarmPosition[i] + "]");
                    }

                    //人脸图片信息
                    if (msg.stuImageInfo != null && msg.stuImageInfo.nLength > 0) {
                        String facePicture = picturePath + "\\" + System.currentTimeMillis() + "face.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuImageInfo.nOffSet, msg.stuImageInfo.nLength, facePicture);
                    }

                    //人脸小图
                    if (msg.stuFaceImageInfo != null && msg.stuFaceImageInfo.nLength > 0) {
                        String faceSmallPicture = picturePath + "\\" + System.currentTimeMillis() + "faceSmall.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuFaceImageInfo.nOffSet, msg.stuFaceImageInfo.nLength, faceSmallPicture);
                    }


                    break;
                }
                default:
                    System.out.println("其他事件--------------------" + dwAlarmType);
                    break;
            }
            return 0;
        }

    }


    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (AttachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(AttachHandle);
        }
    }

    public void RunTest(){
        CaseMenu menu=new CaseMenu();
        menu.addItem((new CaseMenu.Item(this , "attachIVSEvent" , "attachIVSEvent")));
        menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));
        menu.run();
    }

    public static void main(String []args){
        TemperatureDetectionDemo TD=new TemperatureDetectionDemo();
        TD.InitTest();
        TD.RunTest();
        TD.LoginOut();
    }
}
