package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NETDEV_VIRTUALCAMERA_STATE_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.Vector;

import static com.netsdk.lib.Utils.getOsPrefix;

public class DemoCommon {

   /* static {
        System.setProperty("java.io.tmpdir", "D:/");
    }*/

    static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

    private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
    private static LLong loginHandle = new LLong(0);   //登陆句柄
    private LLong m_hAttachHandle = new LLong(0);
    private LLong m_hAttachParkHandle = new LLong(0);
    private LLong m_hFindHandle = new LLong(0);
    private LLong m_lAttachHandle = new LLong(0);
    private LLong m_TransComChannel = new LLong(0);
    private LLong m_lSearchHandle = new LLong(0);

    private Vector<LLong> lstAttachHandle = new Vector<LLong>();

    private static LLong nlRealPlay = new LLong(0);

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    public class fDisConnectCB implements fDisConnect {
        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
        }
    }

    // 网络连接恢复，设备重连成功回调
    // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
    public class HaveReConnect implements fHaveReConnect {
        public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
        }
    }

    private fDisConnectCB m_DisConnectCB = new fDisConnectCB();
    private HaveReConnect haveReConnect = new HaveReConnect();

    public void EndTest() {
        System.out.println("End Test");
        if (loginHandle.longValue() != 0) {
            netsdkApi.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netsdkApi.CLIENT_Cleanup();
        System.exit(0);
    }

    public void InitTest() {
        //初始化SDK库
        netsdkApi.CLIENT_Init(m_DisConnectCB, null);

        // 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
        // 此操作为可选操作，但建议用户进行设置
        netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 10000; //登录请求响应超时时间设置为5S
        int tryTimes = 3;    //登录时尝试建立链接3次
        netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);

        // 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NET_PARAM netParam = new NET_PARAM();
        netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
        netParam.nGetConnInfoTime = 5000;
        netsdkApi.CLIENT_SetNetworkParam(netParam);

        // 打开日志，可选
        LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();

        File path = new File(".");
        String logPath = path.getAbsoluteFile().getParent() + File.separator + "sdklog";

        File file = new File(logPath);
        if (!file.exists()) {
            file.mkdir();
        }

        logPath = file + File.separator + "123456789.log";

        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

        System.out.println();
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        boolean bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
        if (!bLogopen) {
            System.err.println("Failed to open NetSDK log !!!");
        }

        // 向设备登入
        int nSpecCap = 0;
        Pointer pCapParam = null;
        IntByReference nError = new IntByReference(0);
        loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
                m_strPassword, nSpecCap, pCapParam, deviceinfo, nError);

        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Port[%d]Success!\n", m_strIp, m_nPort);
        } else {
            System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[0x%x]\n", m_strIp, m_nPort, netsdkApi.CLIENT_GetLastError());
            EndTest();
        }

    }

    /**
     * 获取接口错误码
     *
     * @return
     */
    public static String getErrorCode() {
        return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff) + " ). 参考  NetSDKLib.java }";
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 订阅智能分析数据－图片
     */
    public void flowstateIVSEvent() {
        //  LLong hAttachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, 0,  NetSDKLib.EVENT_IVS_ALL , bNeedPicture , m_AnalyzerDataCB , null , null);


        if (deviceinfo.byDVRType == NET_DEVICE_TYPE.NET_TPC_SERIAL) { // 热成像设备特殊处理
            for (int i = 0; i < deviceinfo.byChanNum; ++i) {
                lstAttachHandle.add(flowstateIVSEvent(i));
            }
        } else {
            int ChannelId = deviceinfo.byChanNum == 1 ? 0 : -1; // 通道
            m_hAttachHandle = flowstateIVSEvent(ChannelId);
        }
    }

    public LLong flowstateIVSEvent(int ChannelId) {
        int bNeedPicture = 1; // 是否需要图片
        LLong hAttachHandle = netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, ChannelId, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, m_AnalyzerDataCB, null, null);
        if (hAttachHandle.longValue() != 0) {
            System.out.println("CLIENT_RealLoadPictureEx Success Channel: " + ChannelId);
        } else {
            System.err.printf("CLIENT_RealLoadPictureEx Failed! Channel:%d LastError = %x\n", ChannelId, netsdkApi.CLIENT_GetLastError());
        }

        return hAttachHandle;
    }

    /**
     * 停止上传智能分析数据－图片
     */
    public void detachIVSEvent() {
        if (0 != m_hAttachHandle.longValue()) {
            netsdkApi.CLIENT_StopLoadPic(m_hAttachHandle);
            System.out.println("Stop detach IVS event");
            m_hAttachHandle.setValue(0);
        }

        for (LLong lAttachHandle : lstAttachHandle) {
            if (0 != lAttachHandle.longValue()) {
                netsdkApi.CLIENT_StopLoadPic(lAttachHandle);
            }
        }
        lstAttachHandle.clear();
    }

    private fAnalyzerDataCB m_AnalyzerDataCB = new fAnalyzerDataCB();

    final DEV_EVENT_FACERECOGNITION_INFO dstInfo = new DEV_EVENT_FACERECOGNITION_INFO();

    /* 智能报警事件回调 */
    public class fAnalyzerDataCB implements fAnalyzerDataCallBack/*, StdCallCallback*/ {
        NET_MSG_OBJECT plateObject;
        NET_MSG_OBJECT vehicleObject;
        NET_MSG_OBJECT_EX objEx;
        boolean msgFlags = false;

        @Override
        public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
                          Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (pAlarmInfo == null) {
                return 0;
            }

            File path = new File("./EventPicture/");
            if (!path.exists()) {
                path.mkdir();
            }

            plateObject = new NET_MSG_OBJECT();  // 车牌信息
            vehicleObject = new NET_MSG_OBJECT();   // 车身信息
            objEx = new NET_MSG_OBJECT_EX();
            System.out.println("dwAlarmType:" + dwAlarmType);
            switch (dwAlarmType) {
                case NetSDKLib.EVENT_IVS_FACERECOGNITION:  ///< 人脸识别事件
                {
                    //两种方式都要将结构体对象作为全局变量放在外面；避免每次进入回调都要进行初始化；
                    //1:选择想要的字段进行复制，不再将结构体中所有的字段全都拷贝下来

                    Pointer pDst = dstInfo.getPointer();
                    /*for (int i = 0; i < 2; i++) {*/
                    long ts = System.currentTimeMillis();

                    int utcOffset = dstInfo.fieldOffset("UTC");
                    dstInfo.UTC.getPointer().write(0, pAlarmInfo.getByteArray(utcOffset, dstInfo.UTC.size()), 0, dstInfo.UTC.size());
                    dstInfo.UTC.read();

                    System.out.println(dstInfo.UTC.dwYear);

                    pDst.write(0, pAlarmInfo.getByteArray(0, dstInfo.size()), 0, dstInfo.size());
                    dstInfo.readField("nRetCandidatesExNum");
                    System.out.println(dstInfo.nRetCandidatesExNum);
                    int CandidatesExOffset = dstInfo.fieldOffset("stuCandidatesEx");
                    for (int j = 0; j < dstInfo.nRetCandidatesExNum; j++) {
                        int size = dstInfo.stuCandidatesEx[j].size();
                        System.out.println(size);
                        dstInfo.stuCandidatesEx[j].getPointer().write(0, pAlarmInfo.getByteArray(CandidatesExOffset + j * size, size), 0, size);
                        dstInfo.stuCandidatesEx[j].read();

                        System.out.println(dstInfo.stuCandidatesEx[j].nChannelID);

                        /*}*/
                    }
                    long ts1 = System.currentTimeMillis();
                    System.out.println(ts1 - ts);

                    //2:当前线程只进行不耗时write()操作，将read()放到另一条线程中进行

					/*long time1=System.currentTimeMillis();
					msg.getPointer().write(0, pAlarmInfo.getByteArray(0, msg.size()), 0, pAlarmInfo.getByteArray(0, msg.size()).length);

					new Thread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							msg.read();
							System.out.println("szName : " + new String(msg.szName).trim() + "\n" );
						}
					}).start();

					long time2=System.currentTimeMillis();
					System.out.println(time1-time2);*/

                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_FLOWSTATE: ///< 交通流量统计事件
                {
                    DEV_EVENT_TRAFFIC_FLOW_STATE msg = new DEV_EVENT_TRAFFIC_FLOW_STATE();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    for (int i = 0; i < msg.nStateNum; i++) {
                        System.out.printf("Lane[%d] \n Flow[%d] \n Period[%d]\n",
                                msg.stuStates[i].nLane,
                                msg.stuStates[i].dwFlow,
                                msg.stuStates[i].dwPeriod);
                    }

                    break;
                }
                case NetSDKLib.EVENT_IVS_FACEDETECT: ///< 人脸检测事件
                {
                    DEV_EVENT_FACEDETECT_INFO msg = new DEV_EVENT_FACEDETECT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    plateObject = msg.stuObject;

                    System.out.println("Time : " + msg.UTC.toString());
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFICJUNCTION: ///< 路口事件
                {
                    DEV_EVENT_TRAFFICJUNCTION_INFO msg = new DEV_EVENT_TRAFFICJUNCTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;

                    try {
                        System.out.println(new String(msg.stuObject.szText, "GBK").trim());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case NetSDKLib.EVENT_IVS_VEHICLE_RECOGNITION: ///< 车牌对比事件
                {
                    DEV_EVENT_VEHICLE_RECOGNITION_INFO msg = new DEV_EVENT_VEHICLE_RECOGNITION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;

                    try {
                        System.out.println(new String(msg.stuObject.szText, "GBK").trim());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING: ///< 车位有车事件
                {
                    DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO msg = new DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    plateObject = msg.stuObject;    // 车牌信息
                    vehicleObject = msg.stuVehicle; // 车身信息

                    System.out.println("车位有车, 车位号：" + msg.nLane);
                    System.out.println("szParkingNum：" + new String(msg.szParkingNum));
                    try {
                        System.out.println("车牌号：" + new String(plateObject.szText, "GBK").trim());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING: ///< 车位无车事件
                {
                    DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO msg = new DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;

                    System.out.println("车位无车, 车位号：" + msg.nLane);
                    System.out.println("szParkingNum：" + new String(msg.szParkingNum));
                    break;
                }
                case NetSDKLib.EVENT_IVS_ACCESS_CTL:   // 门禁事件
                {
                    DEV_EVENT_ACCESS_CTL_INFO msg = new DEV_EVENT_ACCESS_CTL_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    plateObject = msg.stuObject;

                    System.out.println("事件名称 :" + new String(msg.szName).trim());
                    if (msg.emEventType == 1) {
                        System.out.println("门禁事件类型 : 进门！");
                    } else if (msg.emEventType == 2) {
                        System.out.println("门禁事件类型 : 出门！");
                    }

                    if (msg.bStatus == 1) {
                        System.out.println("刷卡结果 : 成功！");
                    } else if (msg.bStatus == 0) {
                        System.out.println("刷卡结果 : 失败！");
                    }

                    System.out.println("卡类型：" + msg.emCardType);
                    System.out.println("开门方式：" + msg.emOpenMethod);
                    System.out.println("卡号 :" + new String(msg.szCardNo).trim());
                    System.out.println("密码 :" + new String(msg.szPwd).trim());
                    System.out.println("开门用户 :" + new String(msg.szUserID).trim());
                    System.out.println("抓拍照片存储地址 :" + new String(msg.szSnapURL).trim());
                    System.out.println("开门失败原因错误码：" + msg.nErrorCode);
                    System.out.println("考勤状态：" + msg.emAttendanceState);

                    break;
                }
                case NetSDKLib.EVENT_IVS_CITIZEN_PICTURE_COMPARE:   //人证比对事件
                {
                    DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO msg = new DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    try {
                        System.out.println("事件名称 :" + new String(msg.szName).trim());
                        System.out.println("比对结果:" + msg.bCompareResult);
                        System.out.println("两张图片的相似度:" + msg.nSimilarity);
                        System.out.println("检测阈值:" + msg.nThreshold);
                        System.out.print("性别:");
                        if (msg.emSex == 1) {
                            System.out.println("男");
                        } else if (msg.emSex == 2) {
                            System.out.println("女");
                        } else {
                            System.out.println("未知");
                        }
                        System.out.println("民族:" + msg.nEthnicity + "(参照 DEV_EVENT_ALARM_CITIZENIDCARD_INFO 的 nEthnicity 定义)");
                        System.out.println("居民姓名:" + new String(msg.szCitizen, "GBK").trim());
                        System.out.println("住址:" + new String(msg.szAddress, "GBK").trim());
                        System.out.println("身份证号:" + new String(msg.szNumber).trim());
                        System.out.println("签发机关:" + new String(msg.szAuthority, "GBK").trim());

                        SimpleDateFormat orignalDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
                        SimpleDateFormat convertDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
                        System.out.println("出生日期:" + convertDateFormat.format(orignalDateFormat.parse(msg.stuBirth.toString())));
                        System.out.println("起始日期:" + convertDateFormat.format(orignalDateFormat.parse(msg.stuValidityStart.toString())));
                        if (msg.bLongTimeValidFlag == 1) {
                            System.out.println("截止日期：永久");
                        } else {
                            System.out.println("截止日期:" + convertDateFormat.format(orignalDateFormat.parse(msg.stuValidityEnd.toString())));
                        }
                    } catch (Exception e) {
                        System.err.println("转GBK编码失败！");
                    }

                    // 拍摄照片
                    String strFileName = path + "\\" + System.currentTimeMillis() + "citizen_shoot.jpg";
                    ToolKits.savePicture(pBuffer, msg.stuImageInfo[0].dwOffSet, msg.stuImageInfo[0].dwFileLenth, strFileName);
                    // 身份证照片
                    strFileName = path + "\\" + System.currentTimeMillis() + "citizen_card.jpg";
                    ToolKits.savePicture(pBuffer, msg.stuImageInfo[1].dwOffSet, msg.stuImageInfo[1].dwFileLenth, strFileName);

                    break;
                }
                case NetSDKLib.EVENT_IVS_HUMANTRAIT:   // 人体特征事件
                {
                    DEV_EVENT_HUMANTRAIT_INFO msg = new DEV_EVENT_HUMANTRAIT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    PrintStruct.print(msg);

                    //保存全景图片
                    if (msg.stuSceneImage.nLength > 0) {
                        String strFileName = path + "\\" + System.currentTimeMillis() + "HumanTrait_全景图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, strFileName);
                    } else {
                        System.out.println("无全景图");
                    }

                    //保存人脸图
                    if (msg.stuFaceImage.nLength > 0) {
                        String strFileName = path + "\\" + System.currentTimeMillis() + "HumanTrait_人脸图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuFaceImage.nOffSet, msg.stuFaceImage.nLength, strFileName);
                    } else {
                        System.out.println("无人脸图");
                    }

                    //保存人脸全景图
                    if (msg.stuFaceSceneImage.nLength > 0) {
                        String strFileName = path + "\\" + System.currentTimeMillis() + "HumanTrait_人脸全景图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuFaceSceneImage.nOffSet, msg.stuFaceSceneImage.nLength, strFileName);
                    } else {
                        System.out.println("无人脸全景图");
                    }

                    //保存人体图
                    if (msg.stuHumanImage.nLength > 0) {
                        String strFileName = path + "\\" + System.currentTimeMillis() + "HumanTrait_人体图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuHumanImage.nOffSet, msg.stuHumanImage.nLength, strFileName);
                    } else {
                        System.out.println("无人体图");
                    }

                    //帽子类型
                    switch (msg.stuHumanAttributes.emCap) {
                        case 0:
                            System.out.println("未知");
                            break;

                        case 1:
                            System.out.println("普通帽子");
                            break;

                        case 2:
                            System.out.println("头盔");
                            break;
                        case 3:
                            System.out.println("安全帽");
                            break;

                        default:
                            System.out.println("未知");
                            break;
                    }


                    break;
                }
                case NetSDKLib.EVENT_IVS_FLOATINGOBJECT_DETECTION: {    // 漂浮物检测事件
                    DEV_EVENT_FLOATINGOBJECT_DETECTION_INFO msg = new DEV_EVENT_FLOATINGOBJECT_DETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    try {
                        System.out.println("漂浮物检测事件: 事件名称[" + new String(msg.szName, "GBK").trim() + "] 预置点名称[" + new String(msg.szPresetName, "GBK").trim() + "]");
                    } catch (Exception e) {
                        // Exception
                    }
                    PrintStruct.FIELD_NOT_PRINT = "szName szPresetName UTC stuFileInfo stuDetectRegion stuObjects stuIntelliCommInfo byReserved";
                    PrintStruct.print(msg);

                    //保存原始图
                    if (msg.stuOriginalImage.nLength > 0) {
                        String strFileName = path + "\\" + System.currentTimeMillis() + "Original_漂浮物检测图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuOriginalImage.nOffSet, msg.stuOriginalImage.nLength, strFileName);
                    } else {
                        System.out.println("漂浮物检测事件无原始图");
                    }

                    //保存球机变到最小倍下的抓图
                    if (msg.stuSceneImage.nLength > 0) {
                        String strFileName = path + "\\" + System.currentTimeMillis() + "Scene_漂浮物检测图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, strFileName);
                    } else {
                        System.out.println("漂浮物检测事件无球机变到最小倍下的抓图");
                    }

                    break;
                }
                case NetSDKLib.EVENT_IVS_WATER_LEVEL_DETECTION: {    // 水位检测事件
                    DEV_EVENT_WATER_LEVEL_DETECTION_INFO msg = new DEV_EVENT_WATER_LEVEL_DETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    try {
                        System.out.println("水位检测事件: 事件名称[" + new String(msg.szName, "GBK").trim() + "] 预置点名称[" + new String(msg.szPresetName, "GBK").trim() + "] 水位尺编号[" + new String(msg.stuWaterRuler.szRulerNum, "GBK").trim() + "]");
                    } catch (Exception e) {
                        // Exception
                    }
                    PrintStruct.FIELD_NOT_PRINT = "szName szPresetName szRulerNum UTC stuFileInfo stuIntelliCommInfo byReserved";
                    PrintStruct.print(msg);

                    //保存原始图
                    if (msg.stuOriginalImage.nLength > 0) {
                        String strFileName = path + "\\" + System.currentTimeMillis() + "Original_水位检测图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuOriginalImage.nOffSet, msg.stuOriginalImage.nLength, strFileName);
                    } else {
                        System.out.println("水位检测事件无原始图");
                    }

                    //保存球机变到最小倍下的抓图
                    if (msg.stuSceneImage.nLength > 0) {
                        String strFileName = path + "\\" + System.currentTimeMillis() + "Scene_水位检测图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, strFileName);
                    } else {
                        System.out.println("水位检测事件无球机变到最小倍下的抓图");
                    }
                    break;
                }
                case NetSDKLib.EVENT_IVS_PHONECALL_DETECT: ///< 打电话检测事件
                {
                    DEV_EVENT_PHONECALL_DETECT_INFO msg = new DEV_EVENT_PHONECALL_DETECT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("打电话检测事件 时间(UTC)：" + msg.UTC + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_SMOKING_DETECT: ///< 吸烟检测事件
                {
                    DEV_EVENT_SMOKING_DETECT_INFO msg = new DEV_EVENT_SMOKING_DETECT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("吸烟检测事件 时间(UTC)：" + msg.UTC + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_FIREWARNING: ///< 火警事件
                {
                    DEV_EVENT_FIREWARNING_INFO msg = new DEV_EVENT_FIREWARNING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("火警事件 火情编号ID：" + msg.nFSID);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_WANDERDETECTION: ///< 徘徊事件
                {
                    DEV_EVENT_WANDER_INFO msg = new DEV_EVENT_WANDER_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("徘徊事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID
                            + " 事件触发的预置点号:" + msg.nPreserID + " 事件触发的预置名称:" + new String(msg.szPresetName));
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_LEFTDETECTION: ///< 物品遗留事件
                {
                    DEV_EVENT_LEFT_INFO msg = new DEV_EVENT_LEFT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("物品遗留事件 时间(UTC)：" + msg.UTC + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime
                            + " 事件触发的预置点号:" + msg.nPreserID + " 事件触发的预置名称:" + new String(msg.szPresetName));
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_RIOTERDETECTION: ///< 聚众事件
                {
                    DEV_EVENT_RIOTERL_INFO msg = new DEV_EVENT_RIOTERL_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("聚众事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 检测到的物体个数:" + msg.nObjectNum
                            + " 事件触发累计次数:" + msg.nOccurrenceCount + " 事件源设备唯一标识:" + new String(msg.szSourceDevice));
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_MOVEDETECTION: ///< 移动事件
                {
                    DEV_EVENT_MOVE_INFO msg = new DEV_EVENT_MOVE_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("移动事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_ABNORMALRUNDETECTION: ///< 异常奔跑事件
                {
                    DEV_EVENT_ABNORMALRUNDETECTION_INFO msg = new DEV_EVENT_ABNORMALRUNDETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("异常奔跑事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime
                            + " 物体运动速度:" + msg.dbSpeed + " km/h " + " 触发速度:" + msg.dbTriggerSpeed + " km/h " + " 异常奔跑类型(0-快速奔跑, 1-突然加速, 2-突然减速):" + msg.bRunType);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TAKENAWAYDETECTION: ///< 物品搬移事件
                {
                    DEV_EVENT_TAKENAWAYDETECTION_INFO msg = new DEV_EVENT_TAKENAWAYDETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("物品搬移事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime
                            + " 事件触发的预置点号:" + msg.nPreserID + " 事件触发的预置名称:" + new String(msg.szPresetName));
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_PARKINGDETECTION: ///< 非法停车事件
                {
                    DEV_EVENT_PARKINGDETECTION_INFO msg = new DEV_EVENT_PARKINGDETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("非法停车事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime
                            + " 事件触发累计次数:" + msg.nOccurrenceCount + " 事件源设备唯一标识:" + new String(msg.szSourceDevice));
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_DRIVER_SMOKING: ///< 驾驶员抽烟事件
                {
                    DEV_EVENT_TRAFFIC_DRIVER_SMOKING msg = new DEV_EVENT_TRAFFIC_DRIVER_SMOKING();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("驾驶员抽烟事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime
                            + " 对应车道号:" + msg.nLane);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_DRIVER_CALLING: ///< 驾驶员打电话事件
                {
                    DEV_EVENT_TRAFFIC_DRIVER_CALLING msg = new DEV_EVENT_TRAFFIC_DRIVER_CALLING();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("驾驶员打电话事件(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime
                            + " 对应车道号:" + msg.nLane);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_TIREDPHYSIOLOGICAL: ///< 生理疲劳驾驶事件
                {
                    DEV_EVENT_TIREDPHYSIOLOGICAL_INFO msg = new DEV_EVENT_TIREDPHYSIOLOGICAL_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("生理疲劳驾驶事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_DRIVERYAWN: ///< 开车打哈欠事件
                {
                    DEV_EVENT_DRIVERYAWN_INFO msg = new DEV_EVENT_DRIVERYAWN_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("开车打哈欠事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                    break;
                }
                case NetSDKLib.EVENT_IVS_HIGHSPEED: ///<  车辆超速报警事件
                {
                    DEV_EVENT_HIGHSPEED_INFO msg = new DEV_EVENT_HIGHSPEED_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 车辆超速报警事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID
                            + " 车连限速值(km/h):" + msg.nSpeedLimit + " 当前车辆速度(km/h):" + msg.nCurSpeed + " 最高速度(km/h):" + msg.nMaxSpeed);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_FORWARDCOLLISION_WARNNING: ///<  前向碰撞预警
                {
                    DEV_EVENT_FORWARDCOLLISION_WARNNING_INFO msg = new DEV_EVENT_FORWARDCOLLISION_WARNNING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 前向碰撞预警 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_LANEDEPARTURE_WARNNING: ///<  车道偏移预警
                {
                    DEV_EVENT_LANEDEPARTURE_WARNNING_INFO msg = new DEV_EVENT_LANEDEPARTURE_WARNNING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 车道偏移预警 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                    PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_ALARM_LOCALALARM: ///<  外部报警事件
                {
                    DEV_EVENT_ALARM_INFO msg = new DEV_EVENT_ALARM_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 外部报警事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                    PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_DRIVERLEAVEPOST: ///<  开车离岗报警事件
                {
                    DEV_EVENT_DRIVERLEAVEPOST_INFO msg = new DEV_EVENT_DRIVERLEAVEPOST_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 开车离岗报警事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_TIREDLOWERHEAD: ///<  开车低头报警事件
                {
                    DEV_EVENT_TIREDLOWERHEAD_INFO msg = new DEV_EVENT_TIREDLOWERHEAD_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 开车低头报警事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_ALARM_VIDEOBLIND: ///<  视频遮挡事件
                {
                    DEV_EVENT_ALARM_VIDEOBLIND msg = new DEV_EVENT_ALARM_VIDEOBLIND();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 视频遮挡事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_DRIVERLOOKAROUND: ///<  开车左顾右盼报警事件
                {
                    DEV_EVENT_DRIVERLOOKAROUND_INFO msg = new DEV_EVENT_DRIVERLOOKAROUND_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 开车左顾右盼报警事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_CROSSREGIONDETECTION: ///<  警戒区事件
                {
                    DEV_EVENT_CROSSREGION_INFO msg = new DEV_EVENT_CROSSREGION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 警戒区事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime);
//	             	PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.EVENT_IVS_GASSTATION_VEHICLE_DETECT: // 加油站车辆检测事件
                {
                    int num = 0;
                    DEV_EVENT_GASSTATION_VEHICLE_DETECT_INFO msg = new DEV_EVENT_GASSTATION_VEHICLE_DETECT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    switch (msg.stuDetectVehicleInfo.emAction) {
                        case 1:
                            num = 1;
                            System.out.println(new String(msg.szName).trim() + "车辆进入检测区域" + " 事件发生的时间:" + msg.UTC + " 通道号:" + msg.nChannelID + "车牌号: " + new String(msg.stuDetectPlateInfo.szPlateNumber).trim());
                            break;
                        case 2:
                            num = 1;
                            System.out.println(new String(msg.szName).trim() + "车辆离开检测区域" + " 事件发生的时间:" + msg.UTC + " 通道号:" + msg.nChannelID + "车牌号: " + new String(msg.stuDetectPlateInfo.szPlateNumber).trim());
                            break;
                        case 3:
                            num = 0;
                            System.out.println(new String(msg.szName).trim() + "车辆正在加油" + " 事件发生的时间:" + msg.UTC + " 通道号:" + msg.nChannelID + "车牌号: " + new String(msg.stuDetectPlateInfo.szPlateNumber).trim());
                            break;
                    }
                    if (num == 1) {
                        System.out.println(new String(msg.szName).trim() + "有空车位");
                    } else {
                        System.out.println(new String(msg.szName).trim() + "目前无车位请等待");
                    }
                    /*//全景图
                    if(msg.bIsGlobalScene && msg.stuSceneImage.nLength>0){
                    	String strFileName = path + "\\" + System.currentTimeMillis() + ".jpg";
        				ToolKits.savePicture(pBuffer , msg.stuSceneImage.nLength ,msg.stuSceneImage.nOffset, strFileName);
        				System.out.println("strFileName : " + strFileName);
                    }
                    //车身图
                    else if(msg.stuDetectVehicleInfo.stuVehicleImage.nLength>0){
                    	String strFileName = path + "\\" + System.currentTimeMillis() + "car.jpg";
            			ToolKits.savePicture(pBuffer, msg.stuDetectVehicleInfo.stuVehicleImage.nLength, msg.stuDetectVehicleInfo.stuVehicleImage.nOffset, strFileName);
                		System.out.println("strFileName : " + strFileName);
                    }
                    //车牌抠图
                    else if(msg.stuDetectPlateInfo.stuPlateImage.nLength>0){
                    	String strFileName = path + "\\" + System.currentTimeMillis() + "plate.jpg";
            			ToolKits.savePicture(pBuffer,msg.stuDetectPlateInfo.stuPlateImage.nLength, msg.stuDetectPlateInfo.stuPlateImage.nOffset, strFileName);
                		System.out.println("strFileName : " + strFileName);
                    }*/
                    break;
                }
                case NetSDKLib.EVENT_IVS_MAN_NUM_DETECTION: ///<  立体视觉区域内人数统计事件
                {
                    DEV_EVENT_MANNUM_DETECTION_INFO msg = new DEV_EVENT_MANNUM_DETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println(" 立体视觉区域内人数统计事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID +
                            " 区域人员列表数量:" + msg.nManListCount + " 人员身高:" + msg.stuManList[0].nStature);
                    break;
                }
                default:
                    break;
            }


            // 保存大图
            if (pBuffer != null && dwBufSize > 0) {
                //pBuffer收到的图片数据
                //保存图片到本地文件
                String strFileName = path + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, dwBufSize, strFileName);
                System.out.println("strFileName : " + strFileName);
            }

            ///////////车牌抠图////////////
            if (plateObject == null) {
                return 0;
            }

            if (plateObject.bPicEnble == 1) {
                //根据pBuffer中数据偏移保存小图图片文件
                int picLength = plateObject.stPicInfo.dwFileLenth;
                if (picLength > 0) {
                    String strFileName = path + "\\" + System.currentTimeMillis() + "plate.jpg";
                    ToolKits.savePicture(pBuffer, plateObject.stPicInfo.dwOffSet, picLength, strFileName);
                    System.out.println("strFileName : " + strFileName);
                }
            } /*else {
				//根据大图中的坐标偏移计算显示车牌小图
				if(plateObject.BoundingBox == null) {
					return 0;
				}
                if (plateObject.BoundingBox.bottom.longValue() == 0
                		&& plateObject.BoundingBox.top.longValue() == 0) {
                    return 0;
                }

                NetSDKLib.DH_RECT dhRect = plateObject.BoundingBox;
        		//1.BoundingBox的值是在8192*8192坐标系下的值，必须转化为图片中的坐标
                //2.OSD在图片中占了64行,如果没有OSD，下面的关于OSD的处理需要去掉(把OSD_HEIGHT置为0)
        		final int OSD_HEIGHT = 0;
        		BufferedImage snapImage = null;
                long nWidth = snapImage.getWidth(null);
                long nHeight = snapImage.getHeight(null);

                nHeight = nHeight - OSD_HEIGHT;
                if ((nWidth <= 0) || (nHeight <= 0)) {
                    return 0;
                }

                NetSDKLib.DH_RECT dstRect = new NetSDKLib.DH_RECT();

                dstRect.left.setValue((long)((nWidth * dhRect.left.longValue()) / 8192.0));
                dstRect.right.setValue((long)((nWidth * dhRect.right.longValue()) / 8192.0));
                dstRect.bottom.setValue((long)((nHeight * dhRect.bottom.longValue()) / 8192.0));
                dstRect.top.setValue((long)((nHeight * dhRect.top.longValue()) / 8192.0));

                int x = dstRect.left.intValue();
                int y = dstRect.top.intValue() + OSD_HEIGHT;
                int w = dstRect.right.intValue() - dstRect.left.intValue();
                int h = dstRect.bottom.intValue() - dstRect.top.intValue();
                //System.out.println(" x =" + x + ", y =" + y + "; w = "+ w +"; h = "+ h);
                try {
                    BufferedImage plateImage = snapImage.getSubimage(x, y, w, h);
            		String strFileName = path + "\\" + System.currentTimeMillis() + "plate.jpg";
                    ImageIO.write(plateImage, "jpg", new File(strFileName));
                    System.out.println("strFileName : " + strFileName);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			/*
			/////////// 驾驶员抠图///////////////
			if(msgFlags == false) {
				return 0;
			}
			if(objEx.bPicEnble == 1) {
        		//根据pBuffer中数据偏移保存小图图片文件
        		int picLength = objEx.stPicInfo.dwFileLenth;
        		if (picLength > 0) {
        			String strFileName = path + "\\" + System.currentTimeMillis() + "device.jpg";
        			ToolKits.savePicture(pBuffer, objEx.stPicInfo.dwOffSet, picLength, strFileName);
            		System.out.println("strFileName : " + strFileName);
        		}
			} else {
				//根据大图中的坐标偏移计算显示车牌小图
                if (objEx.BoundingBox.bottom.longValue() == 0
                		&& objEx.BoundingBox.top.longValue() == 0) {
                    return 0;
                }

                NetSDKLib.DH_RECT dhRect = objEx.BoundingBox;
        		//1.BoundingBox的值是在8192*8192坐标系下的值，必须转化为图片中的坐标
                //2.OSD在图片中占了64行,如果没有OSD，下面的关于OSD的处理需要去掉(把OSD_HEIGHT置为0)
        		final int OSD_HEIGHT = 0;
        		BufferedImage snapImage = null;
                long nWidth = snapImage.getWidth(null);
                long nHeight = snapImage.getHeight(null);

                nHeight = nHeight - OSD_HEIGHT;
                if ((nWidth <= 0) || (nHeight <= 0)) {
                    return 0;
                }

                NetSDKLib.DH_RECT dstRect = new NetSDKLib.DH_RECT();

                dstRect.left.setValue((long)((nWidth * dhRect.left.longValue()) / 8192.0));
                dstRect.right.setValue((long)((nWidth * dhRect.right.longValue()) / 8192.0));
                dstRect.bottom.setValue((long)((nHeight * dhRect.bottom.longValue()) / 8192.0));
                dstRect.top.setValue((long)((nHeight * dhRect.top.longValue()) / 8192.0));

                int x = dstRect.left.intValue();
                int y = dstRect.top.intValue() + OSD_HEIGHT;
                int w = dstRect.right.intValue() - dstRect.left.intValue();
                int h = dstRect.bottom.intValue() - dstRect.top.intValue();
                //System.out.println(" x =" + x + ", y =" + y + "; w = "+ w +"; h = "+ h);
                try {
                    BufferedImage plateImage = snapImage.getSubimage(x, y, w, h);
        			String strFileName = path + "\\" + System.currentTimeMillis() + "device.jpg";
                    ImageIO.write(plateImage, "jpg", new File(strFileName));
                    System.out.println("strFileName : " + strFileName);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}*/
            return 0;
        }
    }

    ////交通流量查询
    public void findTrafficFlowState() {
        // 设置查询条件
        FIND_RECORD_TRAFFICFLOW_CONDITION flowCondition = new FIND_RECORD_TRAFFICFLOW_CONDITION();
        flowCondition.bStatisticsTime = 1;  //查询是否为统计时间
        flowCondition.bStartTime = 1; // 使能
        flowCondition.bEndTime = 1; // 使能
        flowCondition.stStartTime.dwYear = 2018;
        flowCondition.stStartTime.dwMonth = 1;
        flowCondition.stStartTime.dwDay = 4;
        flowCondition.stStartTime.dwHour = 17;
        flowCondition.stStartTime.dwMinute = 24;
        flowCondition.stStartTime.dwSecond = 0;

        flowCondition.stEndTime.dwYear = 2018;
        flowCondition.stEndTime.dwMonth = 1;
        flowCondition.stEndTime.dwDay = 5;
        flowCondition.stEndTime.dwHour = 17;
        flowCondition.stEndTime.dwMinute = 40;
        flowCondition.stEndTime.dwSecond = 11;

        System.out.println(flowCondition.stStartTime.toStringTime() + "\n" + flowCondition.stEndTime.toStringTime());

        // CLIENT_FindRecord 入参
        NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
        stuFindInParam.emType = EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICFLOW_STATE;
        stuFindInParam.pQueryCondition = flowCondition.getPointer();

        // CLIENT_FindRecord 出参
        NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();

        flowCondition.write();
        boolean bRet = netsdkApi.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 3000);
        flowCondition.read();
        if (!bRet) {
            System.err.println("Can Not Find This Record" + Integer.toHexString(netsdkApi.CLIENT_GetLastError()));
            return;
        }

        int count = 0;  //循环的次数
        int nFindCount = 0;    ///查询到的总数初始化
        int nRecordCount = 10;  // 每次查询的个数
        int vehicleCount = 0;
        NET_RECORD_TRAFFIC_FLOW_STATE[] pstRecord = new NET_RECORD_TRAFFIC_FLOW_STATE[nRecordCount];
        for (int i = 0; i < nRecordCount; i++) {
            pstRecord[i] = new NET_RECORD_TRAFFIC_FLOW_STATE();
        }

        ///CLIENT_FindNextRecord入参
        NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
        stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
        stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数

        ///CLIENT_FindNextRecord出参
        NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
        stuFindNextOutParam.nMaxRecordNum = nRecordCount;
        stuFindNextOutParam.pRecordList = new Memory(pstRecord[0].dwSize * nRecordCount);
        stuFindNextOutParam.pRecordList.clear(pstRecord[0].dwSize * nRecordCount);

        ToolKits.SetStructArrToPointerData(pstRecord, stuFindNextOutParam.pRecordList);    //将数组内存拷贝给Pointer指针

        while (true) {  //循环查询
            if (netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000)) {
                ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, pstRecord);

                for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; i++) {
                    nFindCount = i + count * nRecordCount;

//					System.out.println("[" + nFindCount + "]记录编号:" + pstRecord[i].nRecordNum);
//					System.out.println("[" + nFindCount + "]通道号:" + pstRecord[i].nChannel);
//					System.out.println("[" + nFindCount + "]车道号:" + pstRecord[i].nLane);
//					System.out.println("[" + nFindCount + "]通过车辆总数:" + pstRecord[i].nVehicles);
                    vehicleCount += pstRecord[i].nVehicles;
                }

                if (stuFindNextOutParam.nRetRecordNum <= nRecordCount) {
                    break;
                } else {
                    count++;
                }
            } else {
                System.err.println("FindNextRecord Failed" + netsdkApi.CLIENT_GetLastError());
                break;
            }
        }
        System.out.println("vehicleCount : " + vehicleCount);
        netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
    }

    // 开始录像
    public void recordMode() {
        String szCommand = NetSDKLib.CFG_CMD_RECORDMODE;

        AV_CFG_RecordMode[] recordModes = new AV_CFG_RecordMode[deviceinfo.byChanNum];  // m_stDeviceInfo.byChanNum为设备通道数
        for (int i = 0; i < recordModes.length; ++i) {
            recordModes[i] = new AV_CFG_RecordMode();
        }

        int retCount = ToolKits.GetDevConfig(loginHandle, -1, szCommand, recordModes);
        for (int i = 0; i < retCount; i++) {
            System.out.println("i " + i + "录像模式:" + recordModes[i].nMode + "\n" + " 辅码流录像模式:" + recordModes[i].nModeExtra1);
        }

//		for(int i=0; i<recordModes.length; i++) {
//		    recordModes[i].nMode = 1;
//			recordModes[i].nModeExtra1 = 1;
//		}

        recordModes[0].nMode = 1;
        recordModes[0].nModeExtra1 = 2;

        recordModes[1].nMode = 1;
        recordModes[1].nModeExtra1 = 2;

        boolean bRet = ToolKits.SetDevConfig(loginHandle, -1, szCommand, recordModes);
        if (bRet) {
            for (int i = 0; i < retCount; i++) {
                System.out.println("i = " + i + "\n" + "录像模式:" + recordModes[i].nMode + "\n" + " 辅码流录像模式:" + recordModes[i].nModeExtra1);
            }
        }
    }


    // 查找录像文件帧信息
    public void findFrameInfo() {
        // 1、先查询时间段内的文件信息
        NET_TIME stTimeStart = new NET_TIME();
        stTimeStart.dwYear = 2017;
        stTimeStart.dwMonth = 8;
        stTimeStart.dwDay = 11;
        stTimeStart.dwHour = 2;
        stTimeStart.dwMinute = 0;
        stTimeStart.dwSecond = 0;

        NET_TIME stTimeEnd = new NET_TIME();
        stTimeEnd.dwYear = 2017;
        stTimeEnd.dwMonth = 8;
        stTimeEnd.dwDay = 11;
        stTimeEnd.dwHour = 3;
        stTimeEnd.dwMinute = 0;
        stTimeEnd.dwSecond = 0;

        //**************按时间查找视频文件**************
        int nFileCount = 50; //每次查询的最大文件个数
        NET_RECORDFILE_INFO[] numberFile = (NET_RECORDFILE_INFO[]) new NET_RECORDFILE_INFO().toArray(nFileCount);
        int maxlen = nFileCount * numberFile[0].size();

        IntByReference outFileCoutReference = new IntByReference(0);

        boolean cRet = netsdkApi.CLIENT_QueryRecordFile(loginHandle, 0, 0, stTimeStart, stTimeEnd, null, numberFile, maxlen, outFileCoutReference, 5000, false);

        if (cRet) {
            System.out.println("QueryRecordFile  Succeed! \n" + "查询到的视频个数：" + outFileCoutReference.getValue()
                    + "\n" + "码流类型：" + numberFile[0].bRecType);
            for (int i = 0; i < outFileCoutReference.getValue(); i++) {
                System.out.println("【" + i + "】：");
                System.out.println("开始时间：" + numberFile[i].starttime);
                System.out.println("结束时间：" + numberFile[i].endtime);
                System.out.println("通道号：" + numberFile[i].ch);
            }

        } else {
            System.err.println("QueryRecordFile  Failed!" + netsdkApi.CLIENT_GetLastError());
        }


        // 2、利用上面查到的文件信息，获取查询句柄
        NET_IN_FIND_FRAMEINFO_PRAM inFindFrame = new NET_IN_FIND_FRAMEINFO_PRAM();
        inFindFrame.stuRecordInfo.ch = 0;    // 有视频的通道,对应打标签的视频时间
        inFindFrame.stuRecordInfo = numberFile[0];   // 需要打标签的录像文件信息

        NET_OUT_FIND_FRAMEINFO_PRAM outFindFrame = new NET_OUT_FIND_FRAMEINFO_PRAM();

        boolean bRet = netsdkApi.CLIENT_FindFrameInfo(loginHandle, inFindFrame, outFindFrame, 5000);
        if (bRet) {
            m_hFindHandle = outFindFrame.lFindHandle;
            System.out.println("FindFrameInfo Succeed!" + outFindFrame.lFindHandle.longValue());
        } else {
            System.err.println("FindFrameInfo Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 获取标签信息（查找录像文件帧信息获取m_hFindHandle后，再用此功能）即通过标签查询录像
    public void getTags() {
        NET_IN_FILE_STREAM_GET_TAGS_INFO inFileGetTags = new NET_IN_FILE_STREAM_GET_TAGS_INFO();

        NET_OUT_FILE_STREAM_GET_TAGS_INFO outFielGetTags = new NET_OUT_FILE_STREAM_GET_TAGS_INFO();

        int nMaxNumber = 10;
        NET_FILE_STREAM_TAG_INFO_EX[] pstuTagInfoArr = new NET_FILE_STREAM_TAG_INFO_EX[nMaxNumber];
        for (int i = 0; i < pstuTagInfoArr.length; ++i) {
            pstuTagInfoArr[i] = new NET_FILE_STREAM_TAG_INFO_EX();
        }

        outFielGetTags.nMaxNumber = nMaxNumber;
        outFielGetTags.pstuTagInfo = new Memory(nMaxNumber * pstuTagInfoArr[0].size());
        outFielGetTags.pstuTagInfo.clear(nMaxNumber * pstuTagInfoArr[0].size());

        ToolKits.SetStructArrToPointerData(pstuTagInfoArr, outFielGetTags.pstuTagInfo);

        inFileGetTags.write();
        outFielGetTags.write();
        boolean bRet = netsdkApi.CLIENT_FileStreamGetTags(m_hFindHandle, inFileGetTags, outFielGetTags, 5000);

        ToolKits.GetPointerDataToStructArr(outFielGetTags.pstuTagInfo, pstuTagInfoArr);

        inFileGetTags.read();
        outFielGetTags.read();
        if (bRet) {
            System.out.println("实际返回的标签信息个数 = " + outFielGetTags.nRetTagsCount);
            for (int i = 0; i < outFielGetTags.nRetTagsCount; ++i) {
                System.out.println("【" + i + "】:" + "\n" + "stuTime :" + pstuTagInfoArr[i].stuTime + "\n" +
                        "nSequence :" + pstuTagInfoArr[i].nSequence + "\n" +
                        "szContext : " + new String(pstuTagInfoArr[i].szContext).trim() + "\n" +
                        "szUserName : " + new String(pstuTagInfoArr[i].szUserName).trim() + "\n" +
                        "szChannelName : " + new String(pstuTagInfoArr[i].szChannelName).trim());
            }
        } else {
            System.err.println("GetTags Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }


    // 设置标签信息（查找录像文件帧信息获取m_hFindHandle后，再用此功能），即平台调用SDK下发给NVR
    public void setTags() {
        NET_IN_FILE_STREAM_TAGS_INFO inFileSetTags = new NET_IN_FILE_STREAM_TAGS_INFO();

        int nArrayCount = 2;
        NET_FILE_STREAM_TAG_INFO[] fileTagsArr = new NET_FILE_STREAM_TAG_INFO[nArrayCount];
        for (int i = 0; i < fileTagsArr.length; i++) {
            fileTagsArr[i] = new NET_FILE_STREAM_TAG_INFO();
        }

        inFileSetTags.nArrayCount = nArrayCount;
        inFileSetTags.pstuTagInfo = new Memory(nArrayCount * fileTagsArr[0].size());
        inFileSetTags.pstuTagInfo.clear(nArrayCount * fileTagsArr[0].size());

        System.arraycopy("Text1".getBytes(), 0, fileTagsArr[0].szContext, 0, "Text1".getBytes().length);
        System.arraycopy("admin".getBytes(), 0, fileTagsArr[0].szUserName, 0, "admin".getBytes().length);
        System.arraycopy("beep".getBytes(), 0, fileTagsArr[0].szChannelName, 0, "beep".getBytes().length);

        // 有视频的时间段内，时间要在查询句柄的时间内
        fileTagsArr[0].stuTime.dwYear = 2017;
        fileTagsArr[0].stuTime.dwMonth = 8;
        fileTagsArr[0].stuTime.dwDay = 11;
        fileTagsArr[0].stuTime.dwHour = 2;
        fileTagsArr[0].stuTime.dwMinute = 11;
        fileTagsArr[0].stuTime.dwSecond = 30;

//		System.arraycopy("Text2".getBytes(), 0, fileTagsArr[1].szContext, 0, "Text2".getBytes().length);
//		System.arraycopy("admin".getBytes(), 0, fileTagsArr[1].szUserName, 0, "admin".getBytes().length);
//		System.arraycopy("chn".getBytes(), 0, fileTagsArr[1].szChannelName, 0, "chn".getBytes().length);
//		fileTagsArr[1].stuTime.dwYear = 2017;
//		fileTagsArr[1].stuTime.dwMonth = 8;
//		fileTagsArr[1].stuTime.dwDay = 11;
//		fileTagsArr[1].stuTime.dwHour = 2;
//		fileTagsArr[1].stuTime.dwMinute = 30;
//		fileTagsArr[1].stuTime.dwSecond = 0;

        ToolKits.SetStructArrToPointerData(fileTagsArr, inFileSetTags.pstuTagInfo);

        NET_OUT_FILE_STREAM_TAGS_INFO outFileSetTags = new NET_OUT_FILE_STREAM_TAGS_INFO();

        inFileSetTags.write();
        outFileSetTags.write();
        boolean bRet = netsdkApi.CLIENT_FileStreamSetTags(m_hFindHandle, inFileSetTags, outFileSetTags, 8000);
        inFileSetTags.read();
        outFileSetTags.write();
        if (bRet) {
            System.out.println("SetTags Succeed!");
        } else {
            System.err.println("SetTags Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 查询设备序列号
    public void queryDevSerialNo() {
        int nType = NetSDKLib.NET_DEVSTATE_SOFTWARE;
        NETDEV_VERSION_INFO devVersionInfo = new NETDEV_VERSION_INFO();
        IntByReference intRetLen = new IntByReference();

        devVersionInfo.write();
        boolean bRet = netsdkApi.CLIENT_QueryDevState(loginHandle, nType, devVersionInfo.getPointer(), devVersionInfo.size(), intRetLen, 3000);
        devVersionInfo.read();

        if (bRet) {
            System.out.println("QueryDev Succeed!" + new String(devVersionInfo.szDevType).trim());
        } else {
            System.err.println("QueryDev Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 查询录像视频
    public void queryRecordFile() {
        NET_TIME stTimeStart = new NET_TIME();
        stTimeStart.dwYear = 2016;
        stTimeStart.dwMonth = 12;
        stTimeStart.dwDay = 1;
        stTimeStart.dwHour = 11;
        stTimeStart.dwMinute = 30;
        stTimeStart.dwSecond = 0;

        NET_TIME stTimeEnd = new NET_TIME();
        stTimeEnd.dwYear = 2016;
        stTimeEnd.dwMonth = 12;
        stTimeEnd.dwDay = 1;
        stTimeEnd.dwHour = 12;
        stTimeEnd.dwMinute = 0;
        stTimeEnd.dwSecond = 0;

        //**************按时间查找视频文件**************
        int nFileCount = 200; //每次查询的最大文件个数
        NET_RECORDFILE_INFO[] numberFile = (NET_RECORDFILE_INFO[]) new NET_RECORDFILE_INFO().toArray(nFileCount);
        int maxlen = nFileCount * numberFile[0].size();

        IntByReference outFileCoutReference = new IntByReference(0);

        boolean cRet = netsdkApi.CLIENT_QueryRecordFile(loginHandle, 0, 0, stTimeStart, stTimeEnd, null, numberFile, maxlen, outFileCoutReference, 5000, false);

        if (cRet) {
            System.out.println("QueryRecordFile  Succeed! \n" + "查询到的视频个数：" + outFileCoutReference.getValue()
                    + "\n" + "码流类型：" + numberFile[0].bRecType);
            System.out.printf("%s", numberFile[0].filename);

        } else {
            System.err.println("QueryRecordFile  Failed!" + netsdkApi.CLIENT_GetLastError());
        }

        // *********查询时间段内是否有视频*************
		/*IntByReference bResult = new IntByReference(0);
		boolean cRet = NetSdk.CLIENT_QueryRecordTime(loginHandle, 0, 0, stTimeStart, stTimeEnd, null, bResult, 5000);
		if(cRet) {
			System.out.printf("QueryRecordFile  Succeed!\n bResult = %b", bResult.getValue());
		} else {
			System.err.println("QueryRecordFile  Failed!" + NetSdk.CLIENT_GetLastError());
		}*/
    }

    // 主动查询视频丢失
    public void queryDev() {
        int nType = NetSDKLib.NET_DEVSTATE_VIDEOLOST;
        NET_CLIENT_VIDEOLOST_STATE clientState = new NET_CLIENT_VIDEOLOST_STATE();
        int nBufLen = clientState.size();

        IntByReference intRetLen = new IntByReference();
        clientState.write();
        boolean zRet = netsdkApi.CLIENT_QueryDevState(loginHandle, nType, clientState.getPointer(), nBufLen, intRetLen, 5000);
        clientState.read();
        if (zRet) {
            System.out.println("QueryDevState Succeed! \r\n" + "channelcount = " + clientState.channelcount);
            for (int i = 0; i < clientState.channelcount; ++i) {
                System.out.println("dwAlarmState[" + i + "] = " + ((clientState.dwAlarmState[0] >> i) & 0x01)); // 1-视频丢失; 0-无视频丢失
            }

        } else {
            System.err.println("QueryDevState Failed! " + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 开闸事件
    public static void openStrobe(LLong loginHandle) {
        NET_CTRL_OPEN_STROBE openStrobe = new NET_CTRL_OPEN_STROBE();
        openStrobe.nChannelId = 0;
//		String plate = "浙A.8888";       // 可设可不设，没影响
//		System.arraycopy(plate.getBytes(), 0, openStrobe.szPlateNumber, 0, plate.getBytes().length);   // 要用数组拷贝
        openStrobe.write();

        boolean openRet = netsdkApi.CLIENT_ControlDevice(loginHandle, CtrlType.CTRLTYPE_CTRL_OPEN_STROBE, openStrobe.getPointer(), 3000);
        openStrobe.read();
        if (openRet) {
            System.out.println("OpenStore Success!");
        } else {
            System.err.println("OpenStore Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 过车记录订阅，上传卡号协议
    public void attachParkRecord() {
        NET_IN_PARKING_CONTROL_PARAM pInParam = new NET_IN_PARKING_CONTROL_PARAM();
        pInParam.cbCallBack = ParkingControlRecordCB.getInstance();

        NET_OUT_PARKING_CONTROL_PARAM pOutParam = new NET_OUT_PARKING_CONTROL_PARAM();
        m_hAttachHandle = netsdkApi.CLIENT_ParkingControlAttachRecord(loginHandle, pInParam, pOutParam, 3000);

        if (m_hAttachHandle.longValue() == 0) {
            System.err.println("过车记录订阅失败" + netsdkApi.CLIENT_GetLastError());
        } else {
            System.out.println("过车记录订阅成功");
        }
    }

    // 取消过车记录订阅
    public boolean detachParkRecord() {
        return netsdkApi.CLIENT_ParkingControlDetachRecord(m_hAttachHandle);
    }

    // 订阅过车记录数据回调函数原型,建议写成单例模式
    private static class ParkingControlRecordCB implements fParkingControlRecordCallBack {
        private ParkingControlRecordCB() {
        }

        private static class ParkingControlRecordCBHolder {
            private static ParkingControlRecordCB fParkingcb = new ParkingControlRecordCB();
        }

        public static ParkingControlRecordCB getInstance() {
            return ParkingControlRecordCBHolder.fParkingcb;
        }

        public void invoke(final LLong lLoginID, LLong lAttachHandle,
                           NET_CAR_PASS_ITEM pInfo, int nBufLen, Pointer dwUser) {

            System.out.println("卡号:" + pInfo.dwCardNo + "\n IC卡用户类型：" + pInfo.emCardType + "\n 过车记录类型：" + pInfo.emFlag);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    openStrobe(lLoginID);
                }
            });
        }
    }

    // 车位状态订阅
    public void attachPark() {
        NET_IN_PARK_INFO_PARAM parkIn = new NET_IN_PARK_INFO_PARAM();
        parkIn.stuFilter.dwNum = 10;
        parkIn.stuFilter.emType[0] = NET_ECK_PARK_DETECTOR_TYPE.NET_ECK_PARK_DETECTOR_TYPE_ALL;
        parkIn.cbCallBack = parkInfoCB;

        NET_OUT_PARK_INFO_PARAM parkOut = new NET_OUT_PARK_INFO_PARAM();
        parkIn.write();
        parkOut.write();
        m_hAttachParkHandle = netsdkApi.CLIENT_ParkingControlAttachParkInfo(loginHandle, parkIn, parkOut, 3000);
        parkIn.read();
        parkOut.read();

        if (0 != m_hAttachParkHandle.longValue()) {
            System.out.println("AttachPark Succeed! \r\n");
        } else {
            System.err.println("AttachPark Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    private ParkInfoCB parkInfoCB = new ParkInfoCB();

    // 订阅车位信息回调函数原型
    public class ParkInfoCB implements fParkInfoCallBack {
        public void invoke(LLong lLoginID, LLong lAttachHandle,
                           NET_PARK_INFO_ITEM pInfo, int nBufLen, Pointer dwUser) {
            System.out.println("车位号: " + new String(pInfo.szParkNo).trim() + "\n");   // 车位号
            System.out.println("位号显示对应的诱导屏分屏号 : " + pInfo.dwScreenIndex + "\n"); // 位号显示对应的诱导屏分屏号
            System.out.println("屏号显示的当前空余车位数目 : " + pInfo.dwFreeParkNum + "\n");  // 屏号显示的当前空余车位数目

            // 车位状态
            switch (pInfo.emState) {
                case NET_ECK_PARK_STATE.NET_ECK_PARK_STATE_PARK:
                    System.out.println("车位状态：有车位" + "\n");
                    break;
                case NET_ECK_PARK_STATE.NET_ECK_PARK_STATE_NOPARK:
                    System.out.println("车位状态：无车位" + "\n");
                    break;
                default:
                    break;
            }
        }
    }

    // 取消车位状态订阅
    public void detachPark() {
        boolean bRet = netsdkApi.CLIENT_ParkingControlDetachParkInfo(m_hAttachParkHandle);
        if (bRet) {
            System.out.println("DetachPark Succeed! \r\n");
        } else {
            System.err.println("DetachPark Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /*
     * 下发到诱导屏
     */
    public void setParkInfo() {
        int emType = CtrlType.CTRLTYPE_CTRL_ECK_SET_PARK_INFO;   // 智能停车系统出入口机设置车位信息
        NET_CTRL_ECK_SET_PARK_INFO_PARAM ctrlSetParkInfo = new NET_CTRL_ECK_SET_PARK_INFO_PARAM();
        ctrlSetParkInfo.nScreenNum = 3;    // 屏数量，不能大于8

        ctrlSetParkInfo.nScreenIndex[0] = 0;  // 屏号
        ctrlSetParkInfo.nFreeParkNum[0] = 5;  // 对应屏显示的空余车位数

//		ctrlSetParkInfo.nScreenIndex[1] = 1;  // 屏号
//		ctrlSetParkInfo.nFreeParkNum[1] = 11;  // 对应屏显示的空余车位数

//		ctrlSetParkInfo.nScreenIndex[2] = 2;  // 屏号
//		ctrlSetParkInfo.nFreeParkNum[2] = 11;  // 对应屏显示的空余车位数

        ctrlSetParkInfo.write();

        boolean bRet = netsdkApi.CLIENT_ControlDevice(loginHandle, emType, ctrlSetParkInfo.getPointer(), 3000);

        ctrlSetParkInfo.read();

        if (bRet) {
            System.out.println("SetParkInfo Succeed!");
        } else {
            System.err.printf("SetParkInfo Failed! %x\n", netsdkApi.CLIENT_GetLastError());
        }
    }


    /*
     * 大屏开关控制
     */
    public void powerControl() {
        NET_IN_WM_POWER_CTRL inPowerCtrl = new NET_IN_WM_POWER_CTRL();
        inPowerCtrl.nMonitorWallID = 0; // 电视墙序号
        inPowerCtrl.pszBlockID = null;  // null/""-所有区块
        inPowerCtrl.nTVID = -1;   // -1表示区块中所有显示单元
        inPowerCtrl.bPowerOn = 1; // 1是开，0是关

        NET_OUT_WM_POWER_CTRL outPowerCtrl = new NET_OUT_WM_POWER_CTRL();

        inPowerCtrl.write();
        outPowerCtrl.write();
        boolean bPowerCtr = netsdkApi.CLIENT_PowerControl(loginHandle, inPowerCtrl, outPowerCtrl, 3000);
        inPowerCtrl.read();
        outPowerCtrl.read();

        if (bPowerCtr) {
            System.out.println("PowerControl Succeed!");
        } else {
            System.err.println("PowerControl Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /*
     * 切换信号，信号切换是在调用模式后使用    // 查询显示源
     */
    public void getSplitSource() {
        int nChannel = 130; // 输出通道号
        int nWindow = -1;   // 输出通道对应的窗口号， -1表示所有窗口

        int maxCount = 10; // 设置显示源数组的最大个数
        NET_SPLIT_SOURCE[] splitSourceArr = (NET_SPLIT_SOURCE[]) new NET_SPLIT_SOURCE().toArray(maxCount);
        int nMaxCount = maxCount * splitSourceArr[0].size();

        IntByReference pnRetCount = new IntByReference(0);  // 返回的显示源数量

        boolean bGetSplit = netsdkApi.CLIENT_GetSplitSource(loginHandle, nChannel, nWindow, splitSourceArr, nMaxCount, pnRetCount, 5000);


        if (bGetSplit) {
            System.out.println("GetSplitSource Succeed!" + "\n" + "显示源数量:" + pnRetCount.getValue());
            for (int i = 0; i < pnRetCount.getValue(); i++) {
                System.out.println("设备协议类型:" + splitSourceArr[i].emProtocol + "\n"
                        + "码流类型:" + splitSourceArr[i].emPushStream + "\n"
                        + "通道号：" + splitSourceArr[i].nChannelID + "\n");
            }
        } else {
            System.err.println("GetSplitSource Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 远程抓图
    public void snapPicture() {
        //设置抓图回调函数， 图片主要在m_SnapReceiveCB中返回
        netsdkApi.CLIENT_SetSnapRevCallBack(m_SnapReceiveCB, null);

        // 发送抓图命令给前端设备，抓图的信息
        SNAP_PARAMS stuSnapParams = new SNAP_PARAMS();
        stuSnapParams.Channel = 0;  //抓图通道
        stuSnapParams.mode = 0;     //表示请求一帧
        stuSnapParams.Quality = 3;
        stuSnapParams.InterSnap = 5;
        stuSnapParams.CmdSerial = 100; // 请求序列号，有效值范围 0~65535，超过范围会被截断为

        IntByReference reserved = new IntByReference(0);

        if (false == netsdkApi.CLIENT_SnapPictureEx(loginHandle, stuSnapParams, reserved)) {
            System.out.printf("CLIENT_SnapPictureEx Failed!Last Error[%x]\n", netsdkApi.CLIENT_GetLastError());
            return;
        } else {
            System.out.println("CLIENT_SnapPictureEx success");
        }
    }

    private fSnapReceiveCB m_SnapReceiveCB = new fSnapReceiveCB();

    public class fSnapReceiveCB implements fSnapRev {
        public void invoke(LLong lLoginID, Pointer pBuf, int RevLen, int EncodeType, int CmdSerial, Pointer dwUser) {
            //pBuf收到的图片数据
            File currentPath = new File(".");
            String strFileName = currentPath.getAbsoluteFile().getParent() + "\\" + System.currentTimeMillis() + ".jpg";

            byte[] buf = pBuf.getByteArray(0, RevLen);
            ByteArrayInputStream byteArrInput = new ByteArrayInputStream(buf);
            try {
                BufferedImage bufferedImage = ImageIO.read(byteArrInput);
                if (bufferedImage == null) {
                    return;
                }
                ImageIO.write(bufferedImage, "jpg", new File(strFileName));
            } catch (IOException e) {
                e.printStackTrace();
            }

//			//保存图片到本地文件
//			ToolKits.savePicture(pBuf , RevLen , strFileName);
//			System.out.printf("Get Picture EncodeType = %d CmdSerial=%d\n", EncodeType , CmdSerial.intValue());
//			System.out.println("strFileName : " + strFileName);
        }
    }

    /*
     * 报警输出控制   二代协议
     */
    public void queryAlarmOut() {
        boolean bRet = false;
        int emType = NET_IOTYPE.NET_ALARMOUTPUT;

        // 获取报警输出通道个数
        IntByReference nIOCountChannel = new IntByReference(0);
        bRet = netsdkApi.CLIENT_QueryIOControlState(loginHandle, emType, null, 0, nIOCountChannel, 5000);
        if (bRet) {
            System.out.println("报警输出通道个数 : " + nIOCountChannel.getValue());
        }


        /// 获取报警输出通道状态
        int numChannel = nIOCountChannel.getValue();  // 通道数
        /// 构建一个数组，并初始化
        ALARM_CONTROL[] alarmControl = new ALARM_CONTROL[numChannel];
        for (int i = 0; i < alarmControl.length; i++) {
            alarmControl[i] = new ALARM_CONTROL();
        }

        /// 分配内存
        int maxlen = numChannel * alarmControl[0].size();
        Pointer pState = new Memory(maxlen);
        pState.clear(maxlen);

        /// 将结构体数组拷贝到内存
        ToolKits.SetStructArrToPointerData(alarmControl, pState);
        IntByReference nIOCount = new IntByReference(0);
        bRet = netsdkApi.CLIENT_QueryIOControlState(loginHandle, emType, pState, maxlen, nIOCount, 5000);
        ToolKits.GetPointerDataToStructArr(pState, alarmControl);
        if (bRet) {
            System.out.println("nIOCount : " + nIOCount.getValue());
            for (int j = 0; j < nIOCount.getValue(); j++) {
                System.out.println("state : " + alarmControl[j].state);
            }
        } else {
            System.err.println("Get Failed" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /// 设置报警输出
    public void alarmOutControl() {
        boolean bRet = false;
        int emType = NET_IOTYPE.NET_ALARMOUTPUT;

        // 获取报警输出通道个数
        IntByReference nIOCountChannel = new IntByReference(0);
        bRet = netsdkApi.CLIENT_QueryIOControlState(loginHandle, emType, null, 0, nIOCountChannel, 5000);
        if (bRet) {
            System.out.println("报警输出通道个数 : " + nIOCountChannel.getValue());
        }

        int numChannel = nIOCountChannel.getValue();  // 通道数
        /// 构建一个数组，并初始化
        ALARM_CONTROL[] alarmControl = new ALARM_CONTROL[numChannel];
        for (int i = 0; i < alarmControl.length; i++) {
            alarmControl[i] = new ALARM_CONTROL();
        }

        /// 分配内存
        int maxlen = numChannel * alarmControl[0].size();
        Pointer pState = new Memory(maxlen);
        pState.clear(maxlen);

        alarmControl[0].state = 1;
        alarmControl[1].state = 1;

        /// 将结构体数组拷贝到内存
        ToolKits.SetStructArrToPointerData(alarmControl, pState);

        bRet = netsdkApi.CLIENT_IOControl(loginHandle, emType, pState, maxlen);
        if (bRet) {
            System.out.println("Set Succeed!");
        } else {
            System.err.println("Set Failed" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /*
     * 报警输出配置  三代协议
     */
    public void alarmOutInfo() {
        boolean bRet = false;
        int emType = NET_IOTYPE.NET_ALARMOUTPUT;

        // 获取报警输出通道个数
        IntByReference nIOCountChannel = new IntByReference(0);
        bRet = netsdkApi.CLIENT_QueryIOControlState(loginHandle, emType, null, 0, nIOCountChannel, 5000);
        if (bRet) {
            System.out.println("报警输出通道个数 : " + nIOCountChannel.getValue());
        }


        int nChn = nIOCountChannel.getValue();
        String strCmd = NetSDKLib.CFG_CMD_ALARMOUT;
        CFG_ALARMOUT_INFO alarmOutInfo = new CFG_ALARMOUT_INFO();
        for (int i = 0; i < nChn; i++) {
            // 获取状态
            bRet = false;
            bRet = ToolKits.GetDevConfig(loginHandle, i, strCmd, alarmOutInfo);  // 获取单个通道的信息
            if (bRet) {
                System.out.println("nChannelID : " + alarmOutInfo.nChannelID);
                System.out.println("nOutputMode : " + alarmOutInfo.nOutputMode);
            }

            // 设置
            bRet = false;
            alarmOutInfo.nChannelID = i;
            if (alarmOutInfo.nChannelID == 0) {
                alarmOutInfo.nOutputMode = 1;
            } else if (alarmOutInfo.nChannelID == 1) {
                alarmOutInfo.nOutputMode = 0;
            }

            bRet = ToolKits.SetDevConfig(loginHandle, i, strCmd, alarmOutInfo);    // 设置单个通道的信息
            if (bRet) {
                System.out.println("Set Succeed!");
            }
        }
    }

    /*
     * 限速设置
     */
    public void trafficSnapshot() {
        boolean bRet = false;
//		int nLowSpeed = 0;
//		int nHignSpeed = 60;
        String strCmd = NetSDKLib.CFG_CMD_INTELLECTIVETRAFFIC;
        CFG_TRAFFICSNAPSHOT_INFO trafficSnapshot = new CFG_TRAFFICSNAPSHOT_INFO();

        System.out.println("size: " + trafficSnapshot.size());
        bRet = ToolKits.GetDevConfig(loginHandle, -1, strCmd, trafficSnapshot);
        if (bRet) {
//			System.out.print("nLowSpeed : " + trafficSnapshot.arstDetector[0].arnSmallCarSpeedLimit[0]);
//			System.out.print("nHignSpeed :" + trafficSnapshot.arstDetector[0].arnSmallCarSpeedLimit[1]);
            System.out.print("nParkType :" + trafficSnapshot.nParkType + "\n");
        } else {
            System.err.println("Get SpeedLimit Failed!" + netsdkApi.CLIENT_GetLastError());
        }

        System.out.println("nOSDCustomSortNum:" + trafficSnapshot.stOSD.nOSDCustomSortNum);
        System.out.println("0-nElementNum:" + trafficSnapshot.stOSD.stOSDCustomSorts[0].nElementNum);
        System.out.println("0-nElementNum[0].szName:" + new String(trafficSnapshot.stOSD.stOSDCustomSorts[0].stElements[0].szName).trim());

//		for (int i = 0; i < trafficSnapshot.stOSD.stOSDCustomSorts[0].stElements[0].szName.length; i ++ )
//			trafficSnapshot.stOSD.stOSDCustomSorts[0].stElements[0].szName[i] = 0;
//		}

        System.out.println("0-nElementNum[0].szPrefix:" + new String(trafficSnapshot.stOSD.stOSDCustomSorts[0].stElements[0].szPrefix).trim());
        System.out.println("0-nElementNum[1].szName:" + new String(trafficSnapshot.stOSD.stOSDCustomSorts[0].stElements[1].szName).trim());
        System.out.println("0-nElementNum[1].szPrefix:" + new String(trafficSnapshot.stOSD.stOSDCustomSorts[0].stElements[1].szPrefix).trim());

        System.out.println("1-nElementNum:" + trafficSnapshot.stOSD.stOSDCustomSorts[1].nElementNum);
        System.out.println("1-nElementNum[0].szName:" + new String(trafficSnapshot.stOSD.stOSDCustomSorts[1].stElements[0].szName).trim());
        System.out.println("1-nElementNum[0].szPrefix:" + new String(trafficSnapshot.stOSD.stOSDCustomSorts[1].stElements[0].szPrefix).trim());
        System.out.println("1-nElementNum[1].szName:" + new String(trafficSnapshot.stOSD.stOSDCustomSorts[1].stElements[1].szName).trim());
        System.out.println("1-nElementNum[1].szPrefix:" + new String(trafficSnapshot.stOSD.stOSDCustomSorts[1].stElements[1].szPrefix).trim());


        bRet = false;
//		trafficSnapshot.arstDetector[0].arnSmallCarSpeedLimit[0] = nLowSpeed;
//		trafficSnapshot.arstDetector[0].arnSmallCarSpeedLimit[1] = nHignSpeed;
//		trafficSnapshot.nParkType = 2;

        bRet = ToolKits.SetDevConfig(loginHandle, 0, strCmd, trafficSnapshot);
        if (bRet) {
            System.out.print("Set SpeedLimit Succeed! \n");
        } else {
            System.err.println("Set SpeedLimit Failed!" + netsdkApi.CLIENT_GetLastError());
        }

    }

    /*
     * 透明串口
     */
    public void transComChannel() {
        if (m_TransComChannel.longValue() != 0) {
            netsdkApi.CLIENT_DestroyTransComChannel(m_TransComChannel);
        }

        // 创建透明串口通道, 用于接收数据
        int TransComType1 = 1;  // 0:串口(232)， 1:485口
        int baudrate = 4;  // 1~8分别表示 1200 2400  4800 9600 19200 38400 57600 115200
        int databits = 8;  // 4~8表示4位~8位
        int stopbits = 1;  // 232串口 ： 数值0 代表停止位1; 数值1 代表停止位1.5; 数值2 代表停止位2.    485串口 ： 数值1 代表停止位1; 数值2 代表停止位2.
        int parity = 0;    // 0：无校验，1：奇校验；2：偶校验;
        m_TransComChannel = netsdkApi.CLIENT_CreateTransComChannel(loginHandle, TransComType1, baudrate, databits, stopbits, parity, cbTransCom, null);
        if (m_TransComChannel.longValue() != 0) {
            System.out.println("CLIENT_CreateTransComChannel Succeed!" + m_TransComChannel.longValue());
        } else {
            System.err.println("CLIENT_CreateTransComChannel Failed!" + netsdkApi.CLIENT_GetLastError());
        }

        // 透明串口发送数据
        int bufferSize = 10;  // 可变
        byte pBuffer[] = new byte[bufferSize];
        pBuffer[0] = 'a';
        pBuffer[1] = 'b';
        pBuffer[2] = 'c';
        pBuffer[3] = 'd';
        pBuffer[4] = 'e';
        pBuffer[5] = 'f';
        pBuffer[6] = 'g';
        pBuffer[7] = 'h';
        pBuffer[8] = 'i';
        pBuffer[9] = 'j';
        if (netsdkApi.CLIENT_SendTransComData(m_TransComChannel, pBuffer, pBuffer.length)) {
            System.out.println("CLIENT_SendTransComData Succeed!");
        } else {
            System.err.println("CLIENT_SendTransComData Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    TestTransComCallBack cbTransCom = new TestTransComCallBack();

    public class TestTransComCallBack implements fTransComCallBack {
        @Override
        public void invoke(LLong lLoginID, LLong lTransComChannel,
                           Pointer pBuffer, int dwBufSize, Pointer dwUser) {

            byte[] buffer = pBuffer.getByteArray(0, dwBufSize);   // 将Pointer转为byte数组

            for (int i = 0; i < dwBufSize; i++) {
                System.out.printf(Integer.toHexString((buffer[i] & 0x000000ff) | 0xffffff00).substring(6) + " ");  // 十六进制输出
            }
        }
    }

    ///查询全部门禁用户信息
    public void findAllAccess() {
        NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
        stuFindInParam.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;

        NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();

        if (netsdkApi.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) {

            System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);

            int count = 0;  //循环的次数
            int nFindCount = 0;
            int nRecordCount = 10;  // 每次查询的个数
            NET_RECORDSET_ACCESS_CTL_CARD[] pstRecord = new NET_RECORDSET_ACCESS_CTL_CARD[nRecordCount];
            for (int i = 0; i < nRecordCount; i++) {
                pstRecord[i] = new NET_RECORDSET_ACCESS_CTL_CARD();
            }

            NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
            stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
            stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数

            NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
            stuFindNextOutParam.nMaxRecordNum = nRecordCount;
            stuFindNextOutParam.pRecordList = new Memory(pstRecord[0].dwSize * nRecordCount);
            stuFindNextOutParam.pRecordList.clear(pstRecord[0].dwSize * nRecordCount);

            ToolKits.SetStructArrToPointerData(pstRecord, stuFindNextOutParam.pRecordList);    //将数组内存拷贝给Pointer指针

            while (true) {  //循环查询
                if (netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000)) {
                    ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, pstRecord);

                    for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; i++) {
                        nFindCount = i + count * nRecordCount;

                        System.out.println("[" + nFindCount + "]卡号:" + new String(pstRecord[i].szCardNo).trim());
                        System.out.println("[" + nFindCount + "]用户ID:" + new String(pstRecord[i].szUserID).trim());
                        System.out.println("[" + nFindCount + "]开始时间:" + pstRecord[i].stuValidStartTime.toStringTime());
                        System.out.println("[" + nFindCount + "]结束时间:" + pstRecord[i].stuValidEndTime.toStringTime());
                    }

                    if (stuFindNextOutParam.nRetRecordNum < nRecordCount) {
                        break;
                    } else {
                        count++;
                    }
                } else {
                    System.err.println("FindNextRecord Failed" + netsdkApi.CLIENT_GetLastError());
                    break;
                }
            }

            netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
        } else {
            System.err.println("Can Not Find This Record" + String.format("0x%x", netsdkApi.CLIENT_GetLastError()));
        }
    }


    /// 准确查询门禁用户信息
    public void findSingleAccess() {
        FIND_RECORD_ACCESSCTLCARD_CONDITION stuAccessCondition = new FIND_RECORD_ACCESSCTLCARD_CONDITION();
        stuAccessCondition.abCardNo = 1; //卡号查询条件是否有效
        String cardNo = "111";
        System.arraycopy(cardNo.getBytes(), 0, stuAccessCondition.szCardNo, 0, cardNo.getBytes().length);

        NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
        stuFindInParam.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
        stuFindInParam.pQueryCondition = stuAccessCondition.getPointer();

        NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();

        stuAccessCondition.write();
        if (netsdkApi.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) {
            stuAccessCondition.read();
            System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);

            int nRecordCount = 10;
            NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
            stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
            stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数

            NET_RECORDSET_ACCESS_CTL_CARD pstRecord = new NET_RECORDSET_ACCESS_CTL_CARD();

            NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
            stuFindNextOutParam.nMaxRecordNum = nRecordCount;
            stuFindNextOutParam.pRecordList = pstRecord.getPointer();

            pstRecord.write();
            boolean zRet = netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000);
            pstRecord.read();

            if (zRet) {
                System.out.println("用户ID:" + new String(pstRecord.szUserID).trim());
                System.out.println("开始时间:" + pstRecord.stuValidStartTime.toStringTime());
                System.out.println("结束时间:" + pstRecord.stuValidEndTime.toStringTime());
            } else {
                System.err.println("FindNextRecord Failed" + netsdkApi.CLIENT_GetLastError());
            }

            netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
        } else {
            System.err.println("Can Not Find This Record" + String.format("0x%x", netsdkApi.CLIENT_GetLastError()));
        }
    }

    /// 搜索设备
    public void searchDevice() {
        ///停止搜索
        netsdkApi.CLIENT_StopSearchDevices(m_lSearchHandle);

        ///开始搜索
        m_lSearchHandle = netsdkApi.CLIENT_StartSearchDevices(cbSearchDevices, null, null);
    }

    Test_fSearchDevicesCB cbSearchDevices = new Test_fSearchDevicesCB();

    private class Test_fSearchDevicesCB implements fSearchDevicesCB {
        @Override
        public void invoke(Pointer pDevNetInfo, Pointer pUserData) {
            DEVICE_NET_INFO_EX deviceInfo = new DEVICE_NET_INFO_EX();
            ToolKits.GetPointerData(pDevNetInfo, deviceInfo);

            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = simpleDate.format(new Date());
            String dateString = date.replaceAll("-", "").replaceAll(":", "").replaceAll(" ", "");

            if (deviceInfo.iIPVersion == 4) {  ///过滤IPV4;   如果等于6，输出IPV6;   不过滤IPV4/IPV6都输出
                System.out.println("dateTime:" + dateString + "\n" +
                        "deviceId:" + new String(deviceInfo.szSerialNo).trim() + "\n" +
                        "IP:" + new String(deviceInfo.szIP).trim() + "\n" +
                        "mask:" + new String(deviceInfo.szSubmask).trim() + "\n" +
                        "gateWay:" + new String(deviceInfo.szGateway).trim() + "\n" +
                        "deviceType:" + new String(deviceInfo.szDeviceType).trim() + "\n");
            }
        }
    }

    // 获取NVR时间
    public void getSystemTime() {
        NET_TIME curDateTime = new NET_TIME();
        IntByReference lpBytesReturned = new IntByReference(0);

        curDateTime.write();
        boolean bRet = netsdkApi.CLIENT_GetDevConfig(loginHandle, NetSDKLib.NET_DEV_TIMECFG, -1,
                curDateTime.getPointer(), curDateTime.size(), lpBytesReturned, 5000);
        curDateTime.read();
        if (bRet) {
            System.out.println("curDateTime :" + curDateTime);
        }

        NETDEV_NET_CFG_EX device = new NETDEV_NET_CFG_EX();
        IntByReference lpBytesReturned1 = new IntByReference(0);
        device.write();
        boolean bRet1 = netsdkApi.CLIENT_GetDevConfig(loginHandle, NetSDKLib.NET_DEV_NETCFG_EX, 0,
                device.getPointer(), device.size(), lpBytesReturned1, 5000);
        device.read();
        if (bRet1) {
            System.out.println(new String(device.stEtherNet[0].sDevIPAddr).trim());
            System.out.println(new String(device.stEtherNet[0].sDevIPMask).trim());
            System.out.println(new String(device.stEtherNet[0].sGatewayIP).trim());
        }
    }

    // 获取盒子状态
    public void getWorkState() {
        NET_QUERY_WORK_STATE workState = new NET_QUERY_WORK_STATE();
        IntByReference pRetLen = new IntByReference(0);

        workState.write();
        boolean bRet = netsdkApi.CLIENT_QueryDevState(loginHandle, NetSDKLib.NET_DEVSTATE_GET_WORK_STATE,
                workState.getPointer(), workState.size(), pRetLen, 5000);
        workState.read();
        if (bRet) {
            System.out.println("temperature:" + workState.stuWorkState.fTemperature);
            System.out.println("cpuUsage:" + workState.stuWorkState.nUtilizationOfCPU);
            System.out.printf("memUsage: %.2f", workState.stuWorkState.dbMemInfoTotal - workState.stuWorkState.dbMemInfoFree);
            System.out.println("memUsage: " + String.valueOf(workState.stuWorkState.dbMemInfoTotal - workState.stuWorkState.dbMemInfoFree));
            System.out.println("temperature:" + workState.stuWorkState.fTemperature);
        } else {
            System.err.println(netsdkApi.CLIENT_GetLastError());
        }
    }

    // 时区同步
    public void SetNTP() {
        CFG_NTP_INFO ntpInfo = new CFG_NTP_INFO();
        if (ToolKits.GetDevConfig(loginHandle, -1, NetSDKLib.CFG_CMD_NTP, ntpInfo)) {
            System.out.println("bEnable : " + ntpInfo.bEnable);
            System.out.println("emTimeZoneType : " + ntpInfo.emTimeZoneType);
            System.out.println("szTimeZoneDesc : " + new String(ntpInfo.szTimeZoneDesc).trim());
        }

        // 设置
        ntpInfo.bEnable = 1;
        ntpInfo.emTimeZoneType = EM_CFG_TIME_ZONE_TYPE.EM_CFG_TIME_ZONE_0;
        if (ToolKits.SetDevConfig(loginHandle, -1, NetSDKLib.CFG_CMD_NTP, ntpInfo)) {
            System.out.println("SetNTP Succeed!");
        }
    }

    // 获取设备时间
    public void QueryDeviceTime() {
        NET_TIME pDeviceTime = new NET_TIME();
        if (netsdkApi.CLIENT_QueryDeviceTime(loginHandle, pDeviceTime, 5000)) {
            System.out.println("设备时间：" + pDeviceTime.toStringTime());
        }
    }

    // 时间同步
    public void SetupDeviceTime() {
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = simpleDate.format(new Date());

        String[] dateTime = date.split(" ");
        String[] mDate1 = dateTime[0].split("-");
        String[] mDate2 = dateTime[1].split(":");

        NET_TIME pDeviceTime = new NET_TIME();
        pDeviceTime.dwYear = Integer.parseInt(mDate1[0]);
        pDeviceTime.dwMonth = Integer.parseInt(mDate1[1]);
        pDeviceTime.dwDay = Integer.parseInt(mDate1[2]);
        pDeviceTime.dwHour = Integer.parseInt(mDate2[0]);
        pDeviceTime.dwMinute = Integer.parseInt(mDate2[1]);
        pDeviceTime.dwSecond = Integer.parseInt(mDate2[2]);

        if (netsdkApi.CLIENT_SetupDeviceTime(loginHandle, pDeviceTime)) {
            System.out.println("SetupDeviceTime Succeed!");
        }
    }

    /// 根据卡号查询门禁刷卡记录
    public void findAccessRecordByCardNo() {
        ///查询条件
        FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX recordCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
        recordCondition.bCardNoEnable = 1; //启用卡号查询
        String cardNo = "FB0DCF65";
        System.arraycopy(cardNo.getBytes(), 0, recordCondition.szCardNo, 0, cardNo.getBytes().length);

        ///CLIENT_FindRecord入参
        NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
        stuFindInParam.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;
        stuFindInParam.pQueryCondition = recordCondition.getPointer();

        ///CLIENT_FindRecord出参
        NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();

        recordCondition.write();
        if (netsdkApi.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) {
            recordCondition.read();
            System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);

            int count = 0;  //循环的次数
            int nFindCount = 0;
            int nRecordCount = 10;  // 每次查询的个数
            ///门禁刷卡记录记录集信息
            NET_RECORDSET_ACCESS_CTL_CARDREC[] pstRecord = new NET_RECORDSET_ACCESS_CTL_CARDREC[nRecordCount];
            for (int i = 0; i < nRecordCount; i++) {
                pstRecord[i] = new NET_RECORDSET_ACCESS_CTL_CARDREC();
            }

            ///CLIENT_FindNextRecord入参
            NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
            stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
            stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数

            ///CLIENT_FindNextRecord出参
            NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
            stuFindNextOutParam.nMaxRecordNum = nRecordCount;
            stuFindNextOutParam.pRecordList = new Memory(pstRecord[0].dwSize * nRecordCount);
            stuFindNextOutParam.pRecordList.clear(pstRecord[0].dwSize * nRecordCount);

            ToolKits.SetStructArrToPointerData(pstRecord, stuFindNextOutParam.pRecordList);    //将数组内存拷贝给Pointer指针

            while (true) {  //循环查询
                if (netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000)) {
                    ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, pstRecord);

                    for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; i++) {
                        nFindCount = i + count * nRecordCount;

                        System.out.println("[" + nFindCount + "]刷卡时间:" + pstRecord[i].stuTime.toStringTime());
                        System.out.println("[" + nFindCount + "]用户ID:" + new String(pstRecord[i].szUserID).trim());
                        System.out.println("[" + nFindCount + "]卡号:" + new String(pstRecord[i].szCardNo).trim());
                        System.out.println("[" + nFindCount + "]门号:" + pstRecord[i].nDoor);
                        if (pstRecord[i].emDirection == 1) {
                            System.out.println("[" + nFindCount + "]开门方向: 进门");
                        } else if (pstRecord[i].emDirection == 2) {
                            System.out.println("[" + nFindCount + "]开门方向: 出门");
                        }
                    }

                    if (stuFindNextOutParam.nRetRecordNum < nRecordCount) {
                        break;
                    } else {
                        count++;
                    }
                } else {
                    System.err.println("FindNextRecord Failed" + netsdkApi.CLIENT_GetLastError());
                    break;
                }
            }
            netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
        } else {
            System.err.println("Can Not Find This Record" + String.format("0x%x", netsdkApi.CLIENT_GetLastError()));
        }
    }


    ///按时间查找门禁刷卡记录
    public void findAccessRecordByTime() {
        ///查询条件
        FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX recordCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
        recordCondition.bTimeEnable = 1;  // 启用时间段查询
        //开始时间
        recordCondition.stStartTime.dwYear = 2017;
        recordCondition.stStartTime.dwMonth = 9;
        recordCondition.stStartTime.dwDay = 5;
        recordCondition.stStartTime.dwHour = 1;
        recordCondition.stStartTime.dwMinute = 0;
        recordCondition.stStartTime.dwSecond = 0;
        //结束时间
        recordCondition.stEndTime.dwYear = 2017;
        recordCondition.stEndTime.dwMonth = 9;
        recordCondition.stEndTime.dwDay = 5;
        recordCondition.stEndTime.dwHour = 12;
        recordCondition.stEndTime.dwMinute = 0;
        recordCondition.stEndTime.dwSecond = 0;

        ///CLIENT_FindRecord入参
        NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
        stuFindInParam.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;
        stuFindInParam.pQueryCondition = recordCondition.getPointer();

        ///CLIENT_FindRecord出参
        NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();

        recordCondition.write();
        if (netsdkApi.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) {
            recordCondition.read();
            System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);

            int count = 0;  //循环的次数
            int nFindCount = 0;
            int nRecordCount = 10;  // 每次查询的个数
            ///门禁刷卡记录记录集信息
            NET_RECORDSET_ACCESS_CTL_CARDREC[] pstRecord = new NET_RECORDSET_ACCESS_CTL_CARDREC[nRecordCount];
            for (int i = 0; i < nRecordCount; i++) {
                pstRecord[i] = new NET_RECORDSET_ACCESS_CTL_CARDREC();
            }

            ///CLIENT_FindNextRecord入参
            NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
            stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
            stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数

            ///CLIENT_FindNextRecord出参
            NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
            stuFindNextOutParam.nMaxRecordNum = nRecordCount;
            stuFindNextOutParam.pRecordList = new Memory(pstRecord[0].dwSize * nRecordCount);
            stuFindNextOutParam.pRecordList.clear(pstRecord[0].dwSize * nRecordCount);

            ToolKits.SetStructArrToPointerData(pstRecord, stuFindNextOutParam.pRecordList);    //将数组内存拷贝给Pointer指针

            while (true) {  //循环查询
                if (netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000)) {
                    ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, pstRecord);

                    for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; i++) {
                        nFindCount = i + count * nRecordCount;

                        if (new String(pstRecord[i].szCardNo).trim() != null) {
                            System.out.println("[" + nFindCount + "]刷卡时间:" + pstRecord[i].stuTime.toStringTime());
                            System.out.println("[" + nFindCount + "]用户ID:" + new String(pstRecord[i].szUserID).trim());
                            System.out.println("[" + nFindCount + "]卡号:" + new String(pstRecord[i].szCardNo).trim());
                            System.out.println("[" + nFindCount + "]门号:" + pstRecord[i].nDoor);
                            if (pstRecord[i].emDirection == 1) {
                                System.out.println("[" + nFindCount + "]开门方向: 进门");
                            } else if (pstRecord[i].emDirection == 2) {
                                System.out.println("[" + nFindCount + "]开门方向: 出门");
                            }
                        }
                    }

                    if (stuFindNextOutParam.nRetRecordNum < nRecordCount) {
                        break;
                    } else {
                        count++;
                    }
                } else {
                    System.err.println("FindNextRecord Failed" + netsdkApi.CLIENT_GetLastError());
                    break;
                }
            }
            netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
        } else {
            System.err.println("Can Not Find This Record" + String.format("0x%x", netsdkApi.CLIENT_GetLastError()));
        }
    }

    // 查询录像文件
    public void QueryFile() {
        int type = EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FILE;

        // 查询条件
        NET_IN_MEDIA_QUERY_FILE queryCondition = new NET_IN_MEDIA_QUERY_FILE();

        // 工作目录列表,一次可查询多个目录,为空表示查询所有目录。
        // 目录之间以分号分隔,如“/mnt/dvr/sda0;/mnt/dvr/sda1”,szDirs==null 或"" 表示查询所有
        queryCondition.szDirs = "";

        // 文件类型,0:查询任意类型,1:查询jpg图片,2:查询dav
        queryCondition.nMediaType = 2;

        // 通道号从0开始,-1表示查询所有通道
        queryCondition.nChannelID = 1;

//		// 视频码流 0-未知 1-主码流 2-辅码流1 3-辅码流2 4-辅码流3
//		queryCondition.byVideoStream = 1;

        // 开始时间
        queryCondition.stuStartTime.dwYear = 2018;
        queryCondition.stuStartTime.dwMonth = 10;
        queryCondition.stuStartTime.dwDay = 20;
        queryCondition.stuStartTime.dwHour = 0;
        queryCondition.stuStartTime.dwMinute = 0;
        queryCondition.stuStartTime.dwSecond = 0;

        // 结束时间
        queryCondition.stuEndTime.dwYear = 2018;
        queryCondition.stuEndTime.dwMonth = 12;
        queryCondition.stuEndTime.dwDay = 20;
        queryCondition.stuEndTime.dwHour = 10;
        queryCondition.stuEndTime.dwMinute = 0;
        queryCondition.stuEndTime.dwSecond = 0;

        queryCondition.write();
        LLong lFindHandle = netsdkApi.CLIENT_FindFileEx(loginHandle, type, queryCondition.getPointer(), null, 3000);
        if (lFindHandle.longValue() == 0) {
            System.err.println("FindFileEx Failed!" + netsdkApi.CLIENT_GetLastError());
            return;
        }
        queryCondition.read();


        int nMaxConut = 10;
        NET_OUT_MEDIA_QUERY_FILE[] pMediaQueryFile = new NET_OUT_MEDIA_QUERY_FILE[nMaxConut];
        for (int i = 0; i < pMediaQueryFile.length; ++i) {
            pMediaQueryFile[i] = new NET_OUT_MEDIA_QUERY_FILE();
        }

        int MemorySize = pMediaQueryFile[0].size() * nMaxConut;
        Pointer mediaFileInfo = new Memory(MemorySize);
        mediaFileInfo.clear(MemorySize);

        ToolKits.SetStructArrToPointerData(pMediaQueryFile, mediaFileInfo);

        //循环查询
        int nCurCount = 0;
        int nFindCount = 0;
        while (true) {
            int nRet = netsdkApi.CLIENT_FindNextFileEx(lFindHandle, nMaxConut, mediaFileInfo, MemorySize, null, 3000);
            ToolKits.GetPointerDataToStructArr(mediaFileInfo, pMediaQueryFile);
            System.out.println("nRet : " + nRet);

            if (nRet <= 0) {
                System.err.println("FindNextFileEx failed!" + netsdkApi.CLIENT_GetLastError());
                break;
            }

            for (int i = 0; i < nRet; i++) {
                nFindCount = i + nCurCount * nMaxConut;
                System.out.println("[" + nFindCount + "]通道号 :" + pMediaQueryFile[i].nChannelID);
                System.out.println("[" + nFindCount + "]开始时间 :" + pMediaQueryFile[i].stuStartTime.toStringTime());
                System.out.println("[" + nFindCount + "]结束时间 :" + pMediaQueryFile[i].stuEndTime.toStringTime());
                if (pMediaQueryFile[i].byFileType == 1) {
                    System.out.println("[" + nFindCount + "]文件类型 : jpg图片");
                } else if ((pMediaQueryFile[i].byFileType == 2)) {
                    System.out.println("[" + nFindCount + "]文件类型 : dav");
                }
                System.out.println("[" + nFindCount + "]文件路径 :" + new String(pMediaQueryFile[i].szFilePath).trim());
            }

            if (nRet < nMaxConut) {
                break;
            } else {
                nCurCount++;
            }
        }

        netsdkApi.CLIENT_FindCloseEx(lFindHandle);
    }

    // 控制设备开始/停止分析录像
    public void ControlVideoAnalyse() {
        boolean bRet = false;
        // 开始
//		NetSDKLib.NET_CTRL_START_VIDEO_ANALYSE startVideoAnalyse = new NetSDKLib.NET_CTRL_START_VIDEO_ANALYSE();
//		startVideoAnalyse.nChannelId = 0;
//
//		startVideoAnalyse.write();
//		bRet = NetSdk.CLIENT_ControlDevice(loginHandle, NetSDKLib.CtrlType.CTRLTYPE_CTRL_START_VIDEO_ANALYSE,
//											startVideoAnalyse.getPointer(), 3000);
//		startVideoAnalyse.read();
//		if(bRet) {
//			System.out.println("[0] Start Video Analyse Succeed!");
//		} else {
//			System.err.println("[0] Start Video Analyse Failed!" + NetSdk.CLIENT_GetLastError());
//		}


        NET_CTRL_START_VIDEO_ANALYSE startVideoAnalyse111 = new NET_CTRL_START_VIDEO_ANALYSE();
        startVideoAnalyse111.nChannelId = 1;
        startVideoAnalyse111.write();
        bRet = netsdkApi.CLIENT_ControlDevice(loginHandle, CtrlType.CTRLTYPE_CTRL_START_VIDEO_ANALYSE,
                startVideoAnalyse111.getPointer(), 3000);
        startVideoAnalyse111.read();
        if (bRet) {
            System.out.println("[1] Start Video Analyse Succeed!");
        } else {
            System.err.println("[1] Start Video Analyse Failed!" + netsdkApi.CLIENT_GetLastError());
        }

//		// 停止
//		bRet = false;
//		NetSDKLib.NET_CTRL_STOP_VIDEO_ANALYSE stopVideoAnalyse = new NetSDKLib.NET_CTRL_STOP_VIDEO_ANALYSE();
//		stopVideoAnalyse.nChannelId = 1;
//		stopVideoAnalyse.write();
//		bRet = NetSdk.CLIENT_ControlDevice(loginHandle, NetSDKLib.CtrlType.CTRLTYPE_CTRL_STOP_VIDEO_ANALYSE,
//											stopVideoAnalyse.getPointer(), 3000);
//		stopVideoAnalyse.read();
//		if(bRet) {
//			System.out.println("Stop Video Analyse Succeed!");
//		} else {
//			System.err.println("Stop Video Analyse Failed!" + NetSdk.CLIENT_GetLastError());
//		}
    }

    // 获取录像分析进度
    LLong l_hAttachHandle = new LLong(0);

    public void VideoAnalyseState() {
        boolean bRet = false;
        // 订阅智能分析进度
        NET_IN_ATTACH_VIDEOANALYSE_STATE pstInParam = new NET_IN_ATTACH_VIDEOANALYSE_STATE();
        pstInParam.nChannleId = 1;
        pstInParam.cbVideoAnalyseState = fVideoAnalyseStatecb;

        NET_OUT_ATTACH_VIDEOANALYSE_STATE pstOutParam = new NET_OUT_ATTACH_VIDEOANALYSE_STATE();

        bRet = netsdkApi.CLIENT_AttachVideoAnalyseState(loginHandle, pstInParam, pstOutParam, 3000);
        if (bRet) {
            System.out.println("AttachVideoAnalyseState Succeed!");
            l_hAttachHandle = pstOutParam.lAttachHandle;

        }

        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (l_hAttachHandle.longValue() != 0) {
            System.out.println("l_hAttachHandle : " + l_hAttachHandle.longValue());
            netsdkApi.CLIENT_DetachVideoAnalyseState(l_hAttachHandle);
        }
    }

    private fVideoAnalyseStateCB fVideoAnalyseStatecb = new fVideoAnalyseStateCB();

    public class fVideoAnalyseStateCB implements fVideoAnalyseState {
        @Override
        public int invoke(LLong lAttachHandle,
                          NET_VIDEOANALYSE_STATE pAnalyseStateInfos, Pointer dwUser,
                          Pointer pReserved) {

            System.out.printf("Chn State : [%s] , Progress : [%d] \n",
                    new String(pAnalyseStateInfos.szState).trim(),
                    pAnalyseStateInfos.dwProgress);
            if (pAnalyseStateInfos.dwProgress == 100) {
                // 停止订阅
                netsdkApi.CLIENT_DetachVideoAnalyseState(l_hAttachHandle);
            }

            return 0;
        }
    }

    // 配置视频分析资源接口
    public void AnalyseSource() {
        CFG_ANALYSESOURCE_INFO channelInfo = new CFG_ANALYSESOURCE_INFO();
        System.out.println("byChanNum : " + deviceinfo.byChanNum);
        for (int channel = 0; channel < 2; ++channel) {
            // 获取
            if (!ToolKits.GetDevConfig(loginHandle, channel, NetSDKLib.CFG_CMD_ANALYSESOURCE, channelInfo)) {
                return;
            } else {
                System.out.printf("bEnable [%d] \n nChannelID [%d] \n emSourceType [%d] \n",
                        channelInfo.bEnable, channelInfo.nChannelID, channelInfo.emSourceType);
            }

            // 设置
            channelInfo.nChannelID = channel;
            channelInfo.bEnable = 1;
            channelInfo.nStreamType = 1;
            channelInfo.emSourceType = 1;

            String pathFile = null;
            if (channel == 0) {
                pathFile = "C:\\Test\\20170912_142959.dav";
            } else if (channel == 1) {
                pathFile = "C:\\Test\\20170912_155916.dav";
            }
            System.arraycopy(pathFile.getBytes(), 0, channelInfo.stuSourceFile.szFilePath, 0, pathFile.getBytes().length);
            channelInfo.stuSourceFile.emFileType = 1;

//			channelInfo.stuDeviceInfo.bEnable = 1;
//			System.arraycopy("172.23.1.102".getBytes(), 0, channelInfo.stuDeviceInfo.szIP, 0, "172.23.1.102".getBytes().length);
//			System.arraycopy("admin".getBytes(), 0, channelInfo.stuDeviceInfo.szUser, 0, "admin".getBytes().length);
//			System.arraycopy("admin123".getBytes(), 0, channelInfo.stuDeviceInfo.szPassword, 0, "admin123".getBytes().length);

            if (ToolKits.SetDevConfig(loginHandle, channel, NetSDKLib.CFG_CMD_ANALYSESOURCE, channelInfo)) {
                System.out.println("Set AnalyseSource Succeed!");
            }
        }
    }

    public void QueryMatrixCardInfo() {
        NET_MATRIX_CARD_LIST pstuCardList = new NET_MATRIX_CARD_LIST();
        if (netsdkApi.CLIENT_QueryMatrixCardInfo(loginHandle, pstuCardList, 5000)) {
            System.out.println("nCount : " + pstuCardList.nCount);
            for (int i = 0; i < pstuCardList.nCount; i++) {
                System.out.println("bEnable : " + pstuCardList.stuCards[i].bEnable);
            }
        }

        int cameraCount = 60;
        NET_MATRIX_CAMERA_INFO[] cameraInfo = new NET_MATRIX_CAMERA_INFO[cameraCount];
        for (int i = 0; i < cameraCount; i++) {
            cameraInfo[i] = new NET_MATRIX_CAMERA_INFO();
        }

        NET_IN_MATRIX_GET_CAMERAS inMatrix = new NET_IN_MATRIX_GET_CAMERAS();

        NET_OUT_MATRIX_GET_CAMERAS outMatrix = new NET_OUT_MATRIX_GET_CAMERAS();
        outMatrix.nMaxCameraCount = cameraCount;
        outMatrix.pstuCameras = new Memory(cameraInfo[0].size() * cameraCount);
        outMatrix.pstuCameras.clear(cameraInfo[0].size() * cameraCount);

        ToolKits.SetStructArrToPointerData(cameraInfo, outMatrix.pstuCameras);  // 将数组内存拷贝到Pointer

        if (netsdkApi.CLIENT_MatrixGetCameras(loginHandle, inMatrix, outMatrix, 5000)) {
            ToolKits.GetPointerDataToStructArr(outMatrix.pstuCameras, cameraInfo);  // 将 Pointer 的内容 输出到   数组
            System.out.println("nRetCameraCount : " + outMatrix.nRetCameraCount);

            for (int j = 0; j < outMatrix.nRetCameraCount; j++) {
                System.out.println("[" + j + "]nChannelID : " + cameraInfo[j].nChannelID);
            }

        }
    }

    /**
     * 警灯配置
     */
    public void alarmlamp() {
        // 获取
        CFG_ALARMLAMP_INFO alarmlamp = new CFG_ALARMLAMP_INFO();
        String strCmd = NetSDKLib.CFG_CMD_ALARMLAMP;
        boolean bRet = ToolKits.GetDevConfig(loginHandle, -1, strCmd, alarmlamp);
        if (bRet) {
            System.out.println("警灯状态：" + alarmlamp.emAlarmLamp);
        } else {
            System.err.println("获取警灯配置失败" + netsdkApi.CLIENT_GetLastError());
        }

        // 配置
        bRet = false;
        alarmlamp.emAlarmLamp = EM_ALARMLAMP_MODE.EM_ALARMLAMP_MODE_ON;
        alarmlamp.emAlarmLamp = EM_ALARMLAMP_MODE.EM_ALARMLAMP_MODE_OFF;
        bRet = ToolKits.SetDevConfig(loginHandle, -1, strCmd, alarmlamp);
        if (bRet) {
            System.out.println("警灯配置成功！");
        } else {
            System.err.println("警灯配置失败！" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 下发巡更
     */
    public void patrolstatus() {
        int emNotifyType = NET_EM_NOTIFY_TYPE.NET_EM_NOTIFY_PATROL_STATUS;
        NET_IN_PATROL_STATUS_INFO inpatrol = new NET_IN_PATROL_STATUS_INFO();
        inpatrol.emPatrolStatus = NET_EM_PATROL_STATUS.NET_EM_PATROL_STATUS_BEGIN;
        NET_OUT_PATROL_STATUS_INFO outpatrol = new NET_OUT_PATROL_STATUS_INFO();
        inpatrol.write();
        outpatrol.write();
        boolean bRet = netsdkApi.CLIENT_SendNotifyToDev(loginHandle, emNotifyType, inpatrol.getPointer(), outpatrol.getPointer(), 5000);
        inpatrol.read();
        outpatrol.read();
        if (bRet) {
            System.out.println("下发巡更成功！");
        } else {
            System.err.println("下发巡更失败！" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 开启警笛
     */
    public void startalarmbell() {
        int emType = CtrlType.CTRLTYPE_CTRL_START_ALARMBELL;
        NET_CTRL_ALARMBELL alarmbell = new NET_CTRL_ALARMBELL();
//		alarmbell.nChannelID = 1;
        alarmbell.write();
        boolean bRet = netsdkApi.CLIENT_ControlDevice(loginHandle, emType, alarmbell.getPointer(), 3000);
        alarmbell.read();
        if (bRet) {
            System.out.println("开启警笛成功！");
        } else {
            System.err.println("开启警笛失败" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 关闭警笛
     */
    public void stopalarmbell() {
        int emType = CtrlType.CTRLTYPE_CTRL_STOP_ALARMBELL;
        NET_CTRL_ALARMBELL alarmbell = new NET_CTRL_ALARMBELL();
//		alarmbell.nChannelID = 1;
        alarmbell.write();
        boolean bRet = netsdkApi.CLIENT_ControlDevice(loginHandle, emType, alarmbell.getPointer(), 3000);
        alarmbell.read();
        if (bRet) {
            System.out.println("关闭警笛成功！");
        } else {
            System.err.println("关闭警笛失败" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 开门
     */
    public static boolean openDoor() {
        NET_CTRL_ACCESS_OPEN open = new NET_CTRL_ACCESS_OPEN();
        open.nChannelID = 0;   // 通道号

        open.write();
        boolean openSuccess = netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_CTRL_ACCESS_OPEN, open.getPointer(), null, 3000);
        if (!openSuccess) {
            System.err.println("Failed to open door." + getErrorCode());
        }
        open.read();

        return openSuccess;
    }

    /**
     * 订阅报警信息
     *
     * @return
     */
    public void startListen() {
        // 设置报警回调函数
        netsdkApi.CLIENT_SetDVRMessCallBack(fAlarmDataCB.getCallBack(), null);

        // 订阅报警
        boolean bRet = netsdkApi.CLIENT_StartListenEx(loginHandle);
        if (!bRet) {
            System.err.println("订阅报警失败! LastError = 0x%x\n" + netsdkApi.CLIENT_GetLastError());
        } else {
            System.out.println("订阅报警成功.");
        }
    }

    /**
     * 取消订阅报警信息
     *
     * @return
     */
    public void stopListen() {
        // 停止订阅报警
        boolean bRet = netsdkApi.CLIENT_StopListen(loginHandle);
        if (bRet) {
            System.out.println("取消订阅报警信息.");
        }
    }

    /**
     * 报警信息回调函数原形,建议写成单例模式
     */
    private static class fAlarmDataCB implements fMessCallBack {
        private fAlarmDataCB() {
        }

        private static class fAlarmDataCBHolder {
            private static fAlarmDataCB callback = new fAlarmDataCB();
        }

        public static fAlarmDataCB getCallBack() {
            return fAlarmDataCBHolder.callback;
        }

        public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, Pointer dwUser) {
//	  		System.out.printf("command = %x\n", lCommand);
            switch (lCommand) {
                case NetSDKLib.NET_ALARM_ACCESS_CTL_NOT_CLOSE: { // 门禁未关事件
                    ALARM_ACCESS_CTL_NOT_CLOSE_INFO msg = new ALARM_ACCESS_CTL_NOT_CLOSE_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("门禁名称:" + new String(msg.szDoorName).trim());

                    break;
                }
                case NetSDKLib.NET_ALARM_ACCESS_CTL_BREAK_IN: { // 闯入事件
                    ALARM_ACCESS_CTL_BREAK_IN_INFO msg = new ALARM_ACCESS_CTL_BREAK_IN_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("门禁名称:" + new String(msg.szDoorName).trim());

                    break;
                }
                case NetSDKLib.NET_ALARM_TRAFFIC_LINKAGEALARM: { // 各种违章事件联动报警输出事件
                    ALARM_TRAFFIC_LINKAGEALARM_INFO msg = new ALARM_TRAFFIC_LINKAGEALARM_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("事件发生的时间:" + msg.stuTime.toString());
                    System.out.println("违章联动报警事件:" + new String(msg.szCode).trim());

                    break;
                }
                case NetSDKLib.NET_ALARM_WIFI_VIRTUALINFO_SEARCH: {  //  WIFI虚拟身份上报事件
                    ALARM_WIFI_VIRTUALINFO_SEARCH_INFO msg = new ALARM_WIFI_VIRTUALINFO_SEARCH_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("通道号:" + msg.nChannel);

                    for (int i = 0; i < msg.nVirtualInfoNum; i++) {
                        System.out.println("访问时间:" + msg.stuVirtualInfo[i].stuAccessTime.toStringTime());
                        System.out.println("虚拟信息的来源MAC:" + new String(msg.stuVirtualInfo[i].szSrcMac).trim());
                        System.out.println("虚拟信息的目标MAC:" + new String(msg.stuVirtualInfo[i].szDstMac).trim());
                        System.out.println("采集设备编号:" + new String(msg.stuVirtualInfo[i].szDevNum).trim());
                        System.out.println("虚拟用户ID:" + new String(msg.stuVirtualInfo[i].szUserID).trim());
                        System.out.println("协议代号:" + msg.stuVirtualInfo[i].nProtocal);
                    }

                    break;
                }
                case NetSDKLib.NET_ALARM_WIFI_SEARCH: {   // 获取到周围环境中WIFI设备上报事件
	  /*			Date time1=new Date();
	  			System.out.println(time1);
	  			final ALARM_WIFI_SEARCH_INFO msg=new ALARM_WIFI_SEARCH_INFO();
	  			final NET_WIFI_DEV_INFO stu=new NET_WIFI_DEV_INFO();
	  			stu.getPointer().write(0, pStuEvent.getByteArray(0, stu.size()), 0, pStuEvent.getByteArray(0, stu.size()).length);
	  			Date time2=new Date();
	  			System.out.println(msg.size());
	  			System.out.println(stu.size());
	  			System.out.println(time2);*/
                    Date time1 = new Date();
                    System.out.println(time1);
                    final ALARM_WIFI_SEARCH_INFO msg = new ALARM_WIFI_SEARCH_INFO();
                    ToolKits.GetPointerData(pStuEvent,msg);
                    final NET_WIFI_DEV_INFO[] stu = new NET_WIFI_DEV_INFO[msg.nWifiNum];
                    long offset = 4; // modified by 251589
                    for (int i = 0; i < msg.nWifiNum; i++) {
                        stu[i] = new NET_WIFI_DEV_INFO();
                        stu[i].getPointer().write(0, pStuEvent.getByteArray(offset, stu[i].size()), 0, pStuEvent.getByteArray(offset, stu[i].size()).length);
                        offset += stu[i].size();
                    }
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            for (int i = 0; i < msg.nWifiNum; i++) {
                                stu[i].read();
                                System.out.println("Wifi设备的Mac地址:" + new String(stu[i].szMac).trim());
                            }
                        }
                    }).start();
                    Date time2 = new Date();
                    System.out.println(time2);
	  	/*		ALARM_WIFI_SEARCH_INFO_EX msg = new ALARM_WIFI_SEARCH_INFO_EX();
	  			ToolKits.GetPointerData(pStuEvent, msg);
	  			NET_WIFI_DEV_INFO[] nwdi=new NET_WIFI_DEV_INFO[msg.nWifiNum];
	  			System.out.println("通道号:" + msg.nChannel);
	  			long offset=0;
	  			// 周围Wifi设备的信息
	  			for(int i = 0; i < msg.nWifiNum; i++) {
	  				nwdi[i]=new NET_WIFI_DEV_INFO();
	  				ToolKits.GetPointerDataToStruct(msg.pstuWifi, offset,nwdi[i]);
	  				System.out.println("Wifi设备的Mac地址:" + new String(nwdi[i].szMac).trim());
	  				System.out.println("接入热点Mac的Mac地址:" + new String(nwdi[i].szAPMac).trim());
	  				System.out.println("接入热点SSID:" + new String(nwdi[i].szAPSSID).trim());
	  				System.out.println("设备类型:" + nwdi[i].emDevType);
	  				System.out.println("Mac地址所属制造商:" + new String(nwdi[i].szManufacturer).trim());
	  				offset=offset += nwdi[i].size();
	  			}*/

                    // Wifi事件上报基础信息
/*	  			System.out.println("本周期上报的wifi总数:" + msg.stuWifiBasiInfo.nDeviceSum);
	  			System.out.println("本次事件上报的Wifi设备数量:" + msg.stuWifiBasiInfo.nCurDeviceCount);*/
                    break;
                }
                case NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT: {  // 门禁事件
                    ALARM_ACCESS_CTL_EVENT_INFO msg = new ALARM_ACCESS_CTL_EVENT_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println(msg.toString());

                    if (msg.nErrorCode == 0x10) {
                        // 密码开门
                        if (msg.emOpenMethod == NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_PWD_ONLY) {
                            System.out.println("密码开门失败");
                        } else if (msg.emOpenMethod == NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_CARD) {
                            // 刷卡开门  - (1202B-D 的 二维码方式)
                            System.out.println("刷卡方式失败");
                        }
                    }


                    /// 触发开门
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            openDoor();
                        }
                    }).start();

                    break;
                }
                case NetSDKLib.NET_VIDEOLOST_ALARM_EX:  // 视频丢失报警

                    if (dwBufLen > 0) {
                        byte[] channelState = pStuEvent.getByteArray(0, dwBufLen);  // 将缓存pStuEvent的值，按byte[]输出
                        for (int i = 0; i < dwBufLen; i++) {
                            System.out.print(" Channel[" + i + "] State = " + (channelState[i]));  // 1-视频丢失; 0-无视频丢失
                        }

                        System.out.println();

                    }
                    break;
                case NetSDKLib.NET_ALARM_TALKING_INVITE: //  设备请求对方发起对讲事件
                {
                    ALARM_TALKING_INVITE_INFO msg = new ALARM_TALKING_INVITE_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("emCaller :" + msg.emCaller);
                    System.out.println("stuTime :" + msg.stuTime);
                    System.out.println("szCallID :" + new String(msg.szCallID).trim());
                    System.out.println("nLevel :" + msg.nLevel);

                    break;
                }
                case NetSDKLib.NET_ALARM_RADAR_HIGH_SPEED:  // 雷达监测超速报警事件
                {
                    ALARM_RADAR_HIGH_SPEED_INFO msg = new ALARM_RADAR_HIGH_SPEED_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("事件发生时间：" + msg.stuTime);
                    System.out.println("速度：" + msg.fSpeed);
                    System.out.println("车牌：" + new String(msg.szPlateNumber, Charset.forName("GBK")).trim());

                    break;
                }
                case NetSDKLib.NET_ALARM_POLLING_ALARM:     // 设备巡检报警事件
                {
                    ALARM_POLLING_ALARM_INFO msg = new ALARM_POLLING_ALARM_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("事件发生时间：" + msg.stuTime);
                    break;
                }
                case NetSDKLib.NET_ALARM_ARMMODE_CHANGE_EVENT: // 布撤防状态变化事件
                {
                    ALARM_ARMMODE_CHANGE_INFO msg = new ALARM_ARMMODE_CHANGE_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("Happend time is " + msg.stuTime.toStringTime());
                    System.out.println("bArm is " + msg.bArm + "\r\n");
                    System.out.println("emSceneMode is " + msg.emSceneMode);
                    System.out.println("dwID is " + msg.dwID);
                    System.out.println("emTriggerMode is " + msg.emTriggerMode);

                    System.out.println("布撤防状态变化事件, lCommand.intValue() = " + lCommand);
                    break;
                }
                case NetSDKLib.NET_ALARM_RCEMERGENCY_CALL:     // 火警或SOS报警
                {
                    ALARM_RCEMERGENCY_CALL_INFO msg = new ALARM_RCEMERGENCY_CALL_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("nAction is " + msg.nAction + "\r\n");
                    System.out.println("emType is " + msg.emType + "\r\n");
                    System.out.println("Happend time is " + msg.stuTime.toStringTime() + "\r\n");
                    System.out.println("emMode is " + msg.emMode + "\r\n");
                    System.out.println("dwID is " + msg.dwID + "\r\n");

                    if (msg.emType == 1) {
                        System.out.println("火警");
                    } else if (msg.emType == 2) {
                        System.out.println("胁迫");
                    } else if (msg.emType == 3) {
                        System.out.println("匪警");
                    } else if (msg.emType == 4) {
                        System.out.println("医疗");

                    } else if (msg.emType == 5) {
                        System.out.println("紧急");
                    }
                    break;
                }
                case NetSDKLib.NET_ALARM_ALARM_EX2: // 本地报警事件
                {
                    ALARM_ALARM_INFO_EX2 msg = new ALARM_ALARM_INFO_EX2();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("Channel is " + msg.nChannelID);
                    System.out.println("Action is " + msg.nAction);
                    System.out.println("Happend time is " + msg.stuTime.toStringTime());
                    System.out.println("Sense type is " + msg.emSenseType + "\r\n");
                    System.out.println("Defence area type is " + msg.emDefenceAreaType);

                    if (msg.emSenseType == 1) {
                        System.out.println("被动红外对射入侵报警");
                    } else if (msg.emSenseType == 5) {
                        System.out.println("主动红外对射入侵报警");
                    }

                    break;
                }
                case NetSDKLib.NET_ALARM_FIREWARNING_INFO:  // 热成像火情报警信息上报
                {
                    ALARM_FIREWARNING_INFO_DETAIL msg = new ALARM_FIREWARNING_INFO_DETAIL();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.printf("通道 ： %d \n\n ", msg.nChannel);
                    for (int i = 0; i < msg.nWarningInfoCount; i++) {
                        System.out.println("预置点编号 : " + msg.stuFireWarningInfo[i].nPresetId);
                        if (msg.stuFireWarningInfo[i].nTemperatureUnit
                                == NET_TEMPERATURE_UNIT.NET_TEMPERATURE_UNIT_CENTIGRADE) {
                            System.out.println("温度单位 : 摄氏度");
                        } else if (msg.stuFireWarningInfo[i].nTemperatureUnit
                                == NET_TEMPERATURE_UNIT.NET_TEMPERATURE_UNIT_FAHRENHEIT) {
                            System.out.println("温度单位 : 华氏度");
                        }

                        System.out.println("最高点温度值 : " + msg.stuFireWarningInfo[i].fTemperature);
                        System.out.println("着火点距离 : " + msg.stuFireWarningInfo[i].nDistance);
                    }

                    break;
                }
                case NetSDKLib.NET_ALARM_FACEINFO_COLLECT: //->人脸信息录入事件
                {
                    ALARM_FACEINFO_COLLECT_INFO msg = new ALARM_FACEINFO_COLLECT_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("nAction : " + msg.nAction);                     // 事件动作,1表示持续性事件开始,2表示持续性事件结束;
                    System.out.println("stuTime : " + msg.stuTime.toString());             // 事件发生的时间
                    System.out.println("szUserID :" + new String(msg.szUserID).trim());  // 用户ID

                    break;
                }
                case NetSDKLib.NET_URGENCY_ALARM_EX2: // 紧急报警EX2
                {
                    ALARM_URGENCY_ALARM_EX2 msg = new ALARM_URGENCY_ALARM_EX2();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("紧急报警EX2");
                    System.out.println("nID : " + msg.nID);
                    System.out.println("Time : " + msg.stuTime.toStringTime());

                    break;
                }
                case NetSDKLib.NET_TRAF_CONGESTION_ALARM_EX: // 交通阻塞报警(车辆出现异常停止或者排队)
                {
                    ALARM_TRAF_CONGESTION_INFO msg = new ALARM_TRAF_CONGESTION_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("交通阻塞报警");
                    System.out.println("Time : " + msg.stuTime.toStringTime());
                    PrintStruct.FIELD_NOT_PRINT = "stuTime reserve";
                    PrintStruct.print(msg);

                    break;
                }
                case NetSDKLib.NET_ALARM_TRAFFIC_FLUX_STAT: // 交通流量统计报警
                {
                    ALARM_TRAFFIC_FLUX_LANE_INFO msg = new ALARM_TRAFFIC_FLUX_LANE_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("交通流量统计报警");
                    System.out.println("Time : " + msg.stuCurTime.toStringTime());
                    PrintStruct.FIELD_NOT_PRINT = "stuCurTime byReserved";
                    PrintStruct.print(msg);

                    break;
                }

                case NetSDKLib.NET_ALARM_FLOATINGOBJECT_DETECTION: {    // 漂浮物检测事件
                    ALARM_FLOATINGOBJECT_DETECTION_INFO msg = new ALARM_FLOATINGOBJECT_DETECTION_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    try {
                        System.out.println("漂浮物检测事件: 事件名称[" + new String(msg.szName, "GBK").trim() + "] 预置点名称[" + new String(msg.szPresetName, "GBK").trim() + "]");
                    } catch (Exception e) {
                        // Exception
                    }
                    PrintStruct.FIELD_NOT_PRINT = "szName szPresetName UTC reserved1 stuDetectRegion byReserved";
                    PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.NET_ALARM_WATER_LEVEL_DETECTION: {    // 水位检测事件
                    ALARM_WATER_LEVEL_DETECTION_INFO msg = new ALARM_WATER_LEVEL_DETECTION_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    try {
                        System.out.println("水位检测事件: 事件名称[" + new String(msg.szName, "GBK").trim() + "] 预置点名称[" + new String(msg.szPresetName, "GBK").trim() + "] 水位尺编号[" + new String(msg.stuWaterRuler.szRulerNum, "GBK").trim() + "]");
                    } catch (Exception e) {
                        // Exception
                    }

                    PrintStruct.FIELD_NOT_PRINT = "szName szPresetName szRulerNum UTC reserved1 byReserved";
                    PrintStruct.print(msg);
                    break;
                }
                case NetSDKLib.NET_ALARM_FIREWARNING: {    // 热成像着火点事件
                    ALARM_FIREWARNING_INFO msg = new ALARM_FIREWARNING_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println("热成像着火点事件: 状态[" + msg.nState + "] 对应视频通道号[" + msg.nChannel + "]");
                    break;
                }
                case NetSDKLib.NET_ALARM_ENCLOSURE_ALARM: {    // 电子围栏事件
                    ALARM_ENCLOSURE_ALARM_INFO msg = new ALARM_ENCLOSURE_ALARM_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println("电子围栏事件: 当前时间[" + msg.stuTime + "] 报警类型[" + msg.dwAlarmType + "] 司机编号[" + msg.dwDriverNo
                            + "] 围栏ID[" + msg.dwEnclosureID + "]");
                    break;
                }
                default:
                    break;
            }

//			Native.free(Pointer.nativeValue(pStuEvent));
//			Pointer.nativeValue(pStuEvent, 0);
            return true;
        }
    }

    /**
     * GPS信息订阅
     */
    public void GetGPSInfo() {
        // 设置GPS订阅回调函数--扩展,   只需要回调一次
        netsdkApi.CLIENT_SetSubcribeGPSCallBackEX(OnGPSMessage, null);


        // GPS信息订阅
        int bStart = 1;    // 0：取消; 1:订阅
        int KeepTime = -1; // KeepTime:订阅持续时间(单位秒) 值为-1时,订阅时间为极大值,可视为永久订阅
        boolean bRet = netsdkApi.CLIENT_SubcribeGPS(loginHandle, bStart, KeepTime, 3000);
        if (bRet) {
            System.out.println("SubcribeGPS Succeed!");
        } else {
            System.err.println("SubcribeGPS Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    private fGPSRevCb OnGPSMessage = new fGPSRevCb();

    public class fGPSRevCb implements fGPSRevEx {

        @Override
        public void invoke(LLong lLoginID, GPS_Info.ByValue GpsInfo,
                           ALARM_STATE_INFO.ByValue stAlarmInfo,
                           Pointer dwUserData, Pointer reserved) {
            System.out.println();

        }
    }

    private static class GPSMessage implements fGPSRevEx2 {
        private GPSMessage() {
        }

        private static class GPSMessageHolder {
            private static GPSMessage gpsMessage = new GPSMessage();
        }

        public static GPSMessage getInstance() {
            return GPSMessageHolder.gpsMessage;
        }

        @Override
        public void invoke(LLong lLoginID, NET_GPS_LOCATION_INFO lpData,
                           Pointer dwUserData, Pointer reserved) {

            System.out.println("定位时间:" + lpData.stuGpsInfo.revTime.toStringTime());

            System.out.println("序列号:" + new String(lpData.stuGpsInfo.DvrSerial).trim());


            System.out.println("longitude : " + lpData.stuGpsInfo.longitude);
            System.out.println("latidude : " + lpData.stuGpsInfo.latidude);

            double latidude = 0;
            double longitude = 0;
            if (lpData.stuGpsInfo.latidude > 90 * 1000000) {
                latidude = ((lpData.stuGpsInfo.latidude - 90 * 1000000) / 1000000);
            } else {
                latidude = (90 * 1000000 - lpData.stuGpsInfo.latidude) / 1000000;
            }


            if (lpData.stuGpsInfo.longitude > 180 * 1000000) {
                longitude = (lpData.stuGpsInfo.longitude - 180 * 1000000) / 1000000;
            } else {
                longitude = (180 * 1000000 - lpData.stuGpsInfo.longitude) / 1000000;
            }

            System.out.println("------ latidude : " + latidude + "   longitude : " + longitude);
        }
    }

    /**
     * 订阅GPS
     */
    public boolean subcribeGPS() {
        netsdkApi.CLIENT_SetSubcribeGPSCallBackEX2(GPSMessage.getInstance(), null);

        // GPS信息订阅
        int bStart = 1;    // 0：取消; 1:订阅
        int KeepTime = -1; // KeepTime:订阅持续时间(单位秒) 值为-1时,订阅时间为极大值,可视为永久订阅
        int InterTime = 5; // unit: s
        return netsdkApi.CLIENT_SubcribeGPS(loginHandle, bStart, KeepTime, InterTime);
    }

    /*
     * 取消订阅GPS
     */
    public boolean stopSubcribeGPS() {
        int bStart = 0;    // 0：取消; 1:订阅
        int KeepTime = -1; // KeepTime:订阅持续时间(单位秒) 值为-1时,订阅时间为极大值,可视为永久订阅
        return netsdkApi.CLIENT_SubcribeGPS(loginHandle, bStart, KeepTime, 3000);
    }

    public void getFaceInfo() {
        // 获取
        String userId = "123";  // 用户ID
        int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_GET;
        NET_IN_GET_FACE_INFO inGet = new NET_IN_GET_FACE_INFO();
        System.arraycopy(userId.getBytes(), 0, inGet.szUserID, 0, userId.getBytes().length);   // 用户ID

        NET_OUT_GET_FACE_INFO outGet = new NET_OUT_GET_FACE_INFO();
        outGet.nPhotoData = 1;    // 白光人脸照片数据个数 1
        outGet.nInPhotoDataLen[0] = 200 * 1024; // 用户申请的每张白光人脸照片大小 200KB
        int size = outGet.nInPhotoDataLen[0] * outGet.nPhotoData;
        outGet.pPhotoData[0] = new Memory(size);
        outGet.pPhotoData[0].clear(size);
        inGet.write();
        outGet.write();
        boolean bRet = netsdkApi.CLIENT_FaceInfoOpreate(loginHandle, emType, inGet.getPointer(), outGet.getPointer(), 5000);
        inGet.read();
        outGet.read();
        if (bRet) {
            System.out.println("FaceInfoOpreate Get Succeed!");
            System.out.println("图片个数 : " + outGet.nPhotoData);
            for (int i = 0; i < outGet.nPhotoData; i++) {
                ToolKits.savePicture(outGet.pPhotoData[i], outGet.nOutPhotoDataLen[i], "photo_data_" + i + ".jpg");
            }
        } else {
            System.err.println("FaceInfoOpreate Get Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    public void addFaceInfo() {
        // 添加
        int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_ADD;
        NET_IN_ADD_FACE_INFO inAdd = new NET_IN_ADD_FACE_INFO();
        String userId = "123";  // 用户ID
        System.arraycopy(userId.getBytes(), 0, inAdd.szUserID, 0, userId.getBytes().length);

        String username = "admin";   // 用户名
        System.arraycopy(username.getBytes(), 0, inAdd.stuFaceInfo.szUserName, 0, username.getBytes().length);

        inAdd.stuFaceInfo.nFaceData = 1;
        String faceDataAdd = "msmdsddajfdjf";  // 人脸模板数据
        System.arraycopy(faceDataAdd.getBytes(), 0, inAdd.stuFaceInfo.szFaceDataArr[0].szFaceData, 0, faceDataAdd.getBytes().length);

        inAdd.stuFaceInfo.nRoom = 1;
        System.arraycopy("111".getBytes(), 0, inAdd.stuFaceInfo.szRoomNoArr[0].szRoomNo, 0, "111".getBytes().length);

        NET_OUT_ADD_FACE_INFO outAdd = new NET_OUT_ADD_FACE_INFO();
        inAdd.write();
        outAdd.write();
        boolean bRet = netsdkApi.CLIENT_FaceInfoOpreate(loginHandle, emType, inAdd.getPointer(), outAdd.getPointer(), 5000);
        inAdd.read();
        outAdd.read();
        if (bRet) {
            System.out.println("FaceInfoOpreate Add Succeed!");
        } else {
            System.err.println("FaceInfoOpreate Add Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    public void updateFaceeInfo() {
        // 更新
        int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_UPDATE;
        NET_IN_UPDATE_FACE_INFO inUpdate = new NET_IN_UPDATE_FACE_INFO();
        String userId = "123";  // 用户ID
        System.arraycopy(userId.getBytes(), 0, inUpdate.szUserID, 0, userId.getBytes().length);

        String usernameUpdate = "admin";   // 用户名
        System.arraycopy(usernameUpdate.getBytes(), 0, inUpdate.stuFaceInfo.szUserName, 0, usernameUpdate.getBytes().length);

        inUpdate.stuFaceInfo.nFaceData = 2;
        String faceDataUpdate = "456admin";  // 人脸模板数据
        System.arraycopy(faceDataUpdate.getBytes(), 0, inUpdate.stuFaceInfo.szFaceDataArr[0].szFaceData, 0, faceDataUpdate.getBytes().length);
        String faceDataUpdate1 = "654admin";  // 人脸模板数据
        System.arraycopy(faceDataUpdate1.getBytes(), 0, inUpdate.stuFaceInfo.szFaceDataArr[1].szFaceData, 0, faceDataUpdate1.getBytes().length);


        NET_OUT_UPDATE_FACE_INFO outUpdate = new NET_OUT_UPDATE_FACE_INFO();
        inUpdate.write();
        outUpdate.write();
        boolean bRet = netsdkApi.CLIENT_FaceInfoOpreate(loginHandle, emType, inUpdate.getPointer(), outUpdate.getPointer(), 5000);
        inUpdate.read();
        outUpdate.read();
        if (bRet) {
            System.out.println("FaceInfoOpreate Update Succeed!");
        } else {
            System.err.println("FaceInfoOpreate Update Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    public void removeFaceInfo() {
        // 删除
        int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_REMOVE;
        NET_IN_REMOVE_FACE_INFO inRemove = new NET_IN_REMOVE_FACE_INFO();
        String userId = "123";  // 用户ID
        System.arraycopy(userId.getBytes(), 0, inRemove.szUserID, 0, userId.getBytes().length);

        NET_OUT_REMOVE_FACE_INFO outRemove = new NET_OUT_REMOVE_FACE_INFO();
        inRemove.write();
        outRemove.write();
        boolean bRet = netsdkApi.CLIENT_FaceInfoOpreate(loginHandle, emType, inRemove.getPointer(), outRemove.getPointer(), 5000);
        inRemove.read();
        outRemove.read();
        if (bRet) {
            System.out.println("FaceInfoOpreate Remove Succeed!");
        } else {
            System.err.println("FaceInfoOpreate Remove Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    public void clearFaceInfo() {
        // 清除
        int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_CLEAR;
        NET_IN_CLEAR_FACE_INFO inClear = new NET_IN_CLEAR_FACE_INFO();

        NET_OUT_REMOVE_FACE_INFO outClear = new NET_OUT_REMOVE_FACE_INFO();
        inClear.write();
        outClear.write();
        boolean bRet = netsdkApi.CLIENT_FaceInfoOpreate(loginHandle, emType, inClear.getPointer(), outClear.getPointer(), 5000);
        inClear.read();
        outClear.read();
        if (bRet) {
            System.out.println("FaceInfoOpreate Clear Succeed!");
        } else {
            System.err.println("FaceInfoOpreate Clear Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 人脸信息记录操作
     */
    public void faceInfoOpreate() {
        getFaceInfo();  // 获取

        addFaceInfo();  // 添加

        updateFaceeInfo(); // 更新修改

        removeFaceInfo();  // 删除

        clearFaceInfo();   // 清除
    }

    // 拉流
    int playsdkPort = 0;

    public void RealPlay() {
        // 获取 playsdk free port
//    	IntByReference plPort = new IntByReference();
//    	if (playsdkApi.PLAY_GetFreePort(plPort) == 0) {
//    		System.err.printf("GetFreePort. Error = %d", playsdkApi.PLAY_GetLastError(playsdkPort));
//    		return;
//    	}

        final JWindow wnd = new JWindow();
        wnd.setVisible(true);

        int nChannelID = 0; // 通道号
        int rType = NET_RealPlayType.NET_RType_Realplay;  // 预览类型
        nlRealPlay = netsdkApi.CLIENT_RealPlayEx(loginHandle, nChannelID, null, rType);  // Native.getComponentPointer(wnd)
        if (nlRealPlay.longValue() == 0) {
            System.err.println("RealPlay Failed!");
            return;
        } else {
            System.out.println("RealPlay Succeed!");
        }

//    	playsdkPort = plPort.getValue();
//    	System.out.println("playsdkPort " + playsdkPort);
//    	if(playsdkApi.PLAY_OpenStream(playsdkPort, null, 0, 1024*1024) == 0) {
//    		System.err.printf("OpenStream. Error = %d", playsdkApi.PLAY_GetLastError(playsdkPort));
//    		return;
//    	}
//
//    	if(playsdkApi.PLAY_Play(playsdkPort, Native.getComponentPointer(wnd)) == 0) {
//    		System.err.printf("Play. Error = %d", playsdkApi.PLAY_GetLastError(playsdkPort));
//    		playsdkApi.PLAY_CloseStream(playsdkPort);
//    		return;
//    	}

        int dwFlag = 1;  // YUV数据
        if (netsdkApi.CLIENT_SetRealDataCallBackEx(nlRealPlay, cbRealData, null, dwFlag)) {
            System.out.println("SetRealDataCallBack Succeed!");
        } else {
            System.err.println("SetRealDataCallBack Failed!" + netsdkApi.CLIENT_GetLastError());
        }

        // 创建一个保存视频流的文件
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("d://22.yuv")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 停止拉流
    public void stopRealPlay() {
        netsdkApi.CLIENT_StopRealPlayEx(nlRealPlay);
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    DataOutputStream out = null;
    long offset = 0;
    private RealData cbRealData = new RealData();

    private class RealData implements fRealDataCallBackEx {
        @Override
        public void invoke(LLong lRealHandle, int dwDataType,
                           Pointer pBuffer, int dwBufSize, int param, Pointer dwUser) {
            System.out.println("dwDataType:" + dwDataType);
            if (dwDataType == 2) {  // wDataType为2时才为yuv数据
                System.out.println("流的处理在此处");
                try {
                    out.write(pBuffer.getByteArray(0, dwBufSize), 0, dwBufSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 获取当前相机焦距值的接口
    public void QueryPTZInfo() {
        int nType = NetSDKLib.NET_DEVSTATE_PTZ_LOCATION;
        NET_PTZ_LOCATION_INFO ptzLocationInfo = new NET_PTZ_LOCATION_INFO();
        IntByReference intRetLen = new IntByReference();

        ptzLocationInfo.write();
        boolean bRet = netsdkApi.CLIENT_QueryDevState(loginHandle, nType, ptzLocationInfo.getPointer(), ptzLocationInfo.size(), intRetLen, 3000);
        ptzLocationInfo.read();

        if (bRet) {
            System.out.println("云台光圈变动位置:" + ptzLocationInfo.nPTZZoom);
            System.out.println("聚焦位置:" + ptzLocationInfo.fFocusPosition);
            System.out.println("云台水平运动位置:" + ptzLocationInfo.nPTZPan);
            System.out.println("云台垂直运动位置:" + ptzLocationInfo.nPTZTilt);
            System.out.println("当前倍率:" + (ptzLocationInfo.nZoomValue * 100));  // 真实变倍值 当前倍率（扩大100倍表示）
        } else {
            System.err.println("QueryDev Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 获取/设置相机当前曝光补偿
    public void Get_Set_ExposureNormalInfo() {
        NET_VIDEOIN_EXPOSURE_NORMAL_INFO exposureNormalInfo = new NET_VIDEOIN_EXPOSURE_NORMAL_INFO();

        int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VIDEOIN_EXPOSURE_NORMAL;
        int nChannelID = 0;

        // 获取
        exposureNormalInfo.write();
        if (netsdkApi.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID, exposureNormalInfo.getPointer(),
                exposureNormalInfo.size(), 4000, null)) {
            exposureNormalInfo.read();
            System.out.println("配置类型:" + exposureNormalInfo.emCfgType);
            System.out.println("曝光模式:" + exposureNormalInfo.emExposureMode);
            System.out.println("曝光补偿:" + exposureNormalInfo.nCompensation);

            // 设置，在获取的基础上设置
            IntByReference restart = new IntByReference(0);
            exposureNormalInfo.nCompensation = 30;
            exposureNormalInfo.write();
            if (netsdkApi.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID, exposureNormalInfo.getPointer(),
                    exposureNormalInfo.size(), 4000, restart, null)) {
                exposureNormalInfo.read();
                System.out.println("SetConfig Succeed!");
            } else {
                System.err.println("SetConfig Failed!" + netsdkApi.CLIENT_GetLastError());
            }

        } else {
            System.err.println("GetConfig Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 获取/设置相机当前背光补偿
    public void Get_Set_BacklightInfo() {
        NET_VIDEOIN_BACKLIGHT_INFO backlightInfo = new NET_VIDEOIN_BACKLIGHT_INFO();

        int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VIDEOIN_BACKLIGHT;
        int nChannelID = 0;

        // 获取
        backlightInfo.write();
        if (netsdkApi.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID, backlightInfo.getPointer(),
                backlightInfo.size(), 4000, null)) {
            backlightInfo.read();
            System.out.println("配置类型:" + backlightInfo.emCfgType);              // 具体信息，参考库里的枚举
            System.out.println("背光模式:" + backlightInfo.emBlackMode);              // 具体信息，参考库里的枚举
            System.out.println("背光补偿模式:" + backlightInfo.emBlackLightMode);  // 具体信息，参考库里的枚举

            // 设置，在获取的基础上设置
            IntByReference restart = new IntByReference(0);
            backlightInfo.emBlackMode = NET_EM_BACK_MODE.NET_EM_BACKLIGHT_MODE_BACKLIGHT;  //背光补偿模式
            backlightInfo.write();
            if (netsdkApi.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID, backlightInfo.getPointer(),
                    backlightInfo.size(), 4000, restart, null)) {
                backlightInfo.read();
                System.out.println("SetConfig Succeed!");
            } else {
                System.err.println("SetConfig Failed!" + netsdkApi.CLIENT_GetLastError());
            }

        } else {
            System.err.println("GetConfig Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 获取/设置相机当前对焦模式
    public void Get_Set_FocusModeInfo() {
        NET_VIDEOIN_FOCUSMODE_INFO focusModeInfo = new NET_VIDEOIN_FOCUSMODE_INFO();

        int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VIDEOIN_FOCUSMODE;
        int nChannelID = 0;

        // 获取
        focusModeInfo.write();
        if (netsdkApi.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID, focusModeInfo.getPointer(),
                focusModeInfo.size(), 4000, null)) {
            focusModeInfo.read();
            System.out.println("配置类型:" + focusModeInfo.emCfgType);     // 具体信息，参考库里的枚举
            System.out.println("聚焦模式:" + focusModeInfo.emFocusMode);    // 具体信息，参考库里的枚举

            // 设置，在获取的基础上设置
            IntByReference restart = new IntByReference(0);
            focusModeInfo.emFocusMode = NET_EM_FOCUS_MODE.NET_EM_FOCUS_AUTO;
            focusModeInfo.write();
            if (netsdkApi.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID, focusModeInfo.getPointer(),
                    focusModeInfo.size(), 4000, restart, null)) {
                focusModeInfo.read();
                System.out.println("SetConfig Succeed!");
            } else {
                System.err.println("SetConfig Failed!" + netsdkApi.CLIENT_GetLastError());
            }

        } else {
            System.err.println("GetConfig Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 获取/设置相机图像翻转角度
    public void Get_Set_ImageRotateInfo() {
        NET_VIDEOIN_IMAGE_INFO imageInfo = new NET_VIDEOIN_IMAGE_INFO();

        int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VIDEOIN_IMAGE_OPT;
        int nChannelID = 0;

        // 获取
        imageInfo.write();
        if (netsdkApi.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID, imageInfo.getPointer(),
                imageInfo.size(), 4000, null)) {
            imageInfo.read();
            System.out.println("配置类型:" + imageInfo.emCfgType);
            System.out.println("是否开启画面翻转功能:" + imageInfo.bFlip);
            System.out.println("翻转角度:" + imageInfo.nRotate90);  // bFlip = 1 时有效。0-不旋转，1-顺时针90°，2-逆时针90°

            // 设置，在获取的基础上设置
            IntByReference restart = new IntByReference(0);
            imageInfo.bFlip = 1;
            imageInfo.nRotate90 = 2;
            imageInfo.write();
            if (netsdkApi.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID, imageInfo.getPointer(),
                    imageInfo.size(), 4000, restart, null)) {
                imageInfo.read();
                System.out.println("SetConfig Succeed!");
            } else {
                System.err.println("SetConfig Failed!" + netsdkApi.CLIENT_GetLastError());
            }

        } else {
            System.err.println("GetConfig Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 端口配置
    public void Get_Set_DVRIPInfo() {
        String command = NetSDKLib.CFG_CMD_DVRIP;
        int channel = 0;
        CFG_DVRIP_INFO dvrIp = new CFG_DVRIP_INFO();

        // 获取
        if (ToolKits.GetDevConfig(loginHandle, channel, command, dvrIp)) {
            System.out.println("TCP服务端口 : " + dvrIp.nTcpPort);
            System.out.println("SSL服务端口 :" + dvrIp.nSSLPort);
            System.out.println("UDP服务端口 :" + dvrIp.nUDPPort);
            System.out.println("组播端口号 :" + dvrIp.nMCASTPort);

            // 设置，在获取的基础上设置

            if (ToolKits.SetDevConfig(loginHandle, channel, command, dvrIp)) {
                System.out.println("Set DVRIP Succeed!");
            } else {
                System.err.println("Set DVRIP Failed!" + netsdkApi.CLIENT_GetLastError());
            }

        } else {
            System.err.println("Get DVRIP Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // IP配置
    public void Get_Set_NetWorkInfo() {
        String command = NetSDKLib.CFG_CMD_NETWORK;
        int channel = 0;
        CFG_NETWORK_INFO network = new CFG_NETWORK_INFO();

        // 获取
        if (ToolKits.GetDevConfig(loginHandle, channel, command, network)) {
            System.out.println("主机名称 : " + new String(network.szHostName).trim());
            for (int i = 0; i < network.nInterfaceNum; i++) {
                System.out.println("网络接口名称 : " + new String(network.stuInterfaces[i].szName).trim());
                System.out.println("ip地址 : " + new String(network.stuInterfaces[i].szIP).trim());
                System.out.println("子网掩码 : " + new String(network.stuInterfaces[i].szSubnetMask).trim());
                System.out.println("默认网关 : " + new String(network.stuInterfaces[i].szDefGateway).trim());
                System.out.println("DNS服务器地址 : " + new String(network.stuInterfaces[i].szDnsServersArr[0].szDnsServers).trim() + "\n" +
                        new String(network.stuInterfaces[i].szDnsServersArr[1].szDnsServers).trim());
                System.out.println("MAC地址 : " + new String(network.stuInterfaces[i].szMacAddress).trim());
            }

            // 设置，在获取的基础上设置
            String szIp = "172.23.1.101";
            System.arraycopy(szIp.getBytes(), 0, network.stuInterfaces[0].szIP, 0, szIp.getBytes().length);  // 要用数组拷贝

            if (ToolKits.SetDevConfig(loginHandle, channel, command, network)) {
                System.out.println("Set NETWORK Succeed!");
            } else {
                System.err.println("Set NETWORK Failed!" + netsdkApi.CLIENT_GetLastError());
            }

        } else {
            System.err.println("Get NETWORK Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 设置道闸配置
    public void SetTrafficStrobeInfo() {

        NET_CFG_TRAFFICSTROBE_INFO trafficStrobeInfo = new NET_CFG_TRAFFICSTROBE_INFO();

        int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TRAFFICSTROBE;
        int nChannelID = 0;

        // 设置
        trafficStrobeInfo.bEnable = 1;

        trafficStrobeInfo.nCtrlTypeCount = 2;
        trafficStrobeInfo.emCtrlType[0] = NET_EM_CFG_TRAFFICSTROBE_CTRTYPE.NET_EM_CFG_CTRTYPE_TRAFFICTRUSTLIST;
        trafficStrobeInfo.emCtrlType[1] = NET_EM_CFG_TRAFFICSTROBE_CTRTYPE.NET_EM_CFG_CTRTYPE_ALLSNAPCAR;

        trafficStrobeInfo.stuStationaryOpen.bEnable = 1;
        trafficStrobeInfo.stuStationaryOpen.stTimeShecule.bEnableHoliday = 1;

//		TIME_SECTION_WEEK_DAY_6[] stuTimeSectionWeekDay = trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay;
//		for (int i = 0; i < stuTimeSectionWeekDay.length; i++) {
//			trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].stuTimeSection[0].dwRecordMask = 1;
//			trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].stuTimeSection[0].nBeginHour = 6;
//			trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].stuTimeSection[0].nBeginMin = 30;
//			trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].stuTimeSection[0].nBeginSec = 0;
//			trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].stuTimeSection[0].nEndHour = 23;
//			trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].stuTimeSection[0].nEndMin = 0;
//			trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].stuTimeSection[0].nEndSec = 0;
//		}
//
//		trafficStrobeInfo.write();
//
//		if(netsdkApi.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID, trafficStrobeInfo.getPointer(),
//				trafficStrobeInfo.size(), 4000, null, null)) {
//				System.out.println("SetConfig Succeed!");
//		} else {
//				System.err.println("SetConfig Failed!" + getErrorCode());
//		}
    }

    // 获取道闸配置
    public void GetTrafficStrobeInfo() {

        NET_CFG_TRAFFICSTROBE_INFO trafficStrobeInfo = new NET_CFG_TRAFFICSTROBE_INFO();

        int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TRAFFICSTROBE;
        int nChannelID = 0;

        // 获取
        trafficStrobeInfo.write();
        if (netsdkApi.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID, trafficStrobeInfo.getPointer(),
                trafficStrobeInfo.size(), 4000, null)) {
            trafficStrobeInfo.read();
            PrintStruct.print(trafficStrobeInfo);
        } else {
            System.err.println("GetConfig Failed!" + getErrorCode());
        }
    }

    // 设置通道名称(根据通道号，先获取，再配置)
    public void setChannelName() {
        int chn = 0; // 通道号

        NET_ENCODE_CHANNELTITLE_INFO msg = new NET_ENCODE_CHANNELTITLE_INFO();
        int type = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ENCODE_CHANNELTITLE;
        int nChannelID = 0;

        // 获取
        msg.write();
        if (netsdkApi.CLIENT_GetConfig(loginHandle, type, nChannelID, msg.getPointer(),
                msg.size(), 6000, null)) {
            msg.read();

            System.out.println("szChannelName:" + new String(msg.szChannelName).trim());

            // 修改通道名称
            String channelName = "NetSDK";
            ToolKits.StringToByteArray(channelName, msg.szChannelName);

            IntByReference restart = new IntByReference(0);

            msg.write();
            if (netsdkApi.CLIENT_SetConfig(loginHandle, type, chn, msg.getPointer(),
                    msg.size(), 6000, restart, null)) {
                msg.read();
                System.out.println("SetConfig ChannelName Succeed!");
            } else {
                System.err.println("SetConfig ChannelName Failed!" + getErrorCode());
            }
        } else {
            System.err.println("GetConfig ChannelName Failed!" + getErrorCode());
        }
    }

    public void MatrixAddCamerasByDevice() {
        Scanner scanner = new Scanner(System.in);

        String line = "";
        while (true) {
            System.out.println("请输入通道号：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }
        scanner.close();

        int channel = Integer.parseInt(line); // 通道

        // 视频源信息
        int count = 1; // 视频源信息个数
        NET_LOGIC_BYDEVICE_ADD_CAMERA_PARAM[] addCamera = new NET_LOGIC_BYDEVICE_ADD_CAMERA_PARAM[count];
        addCamera[0] = new NET_LOGIC_BYDEVICE_ADD_CAMERA_PARAM();
        addCamera[0].nChannel = channel; // 通道号[IVSS的通道号]
        addCamera[0].nUniqueChannel = channel;

        /**
         * 入参
         */
        NET_IN_ADD_LOGIC_BYDEVICE_CAMERA stIn = new NET_IN_ADD_LOGIC_BYDEVICE_CAMERA();
        ///////////////////////////// 远程设备信息[向IVSS添加的设备信息] ////////////////////////////
        // 使能
        stIn.stuRemoteDevice.bEnable = 1;

        // IP
        String ip = "172.23.2.91";
        System.arraycopy(ip.getBytes(), 0, stIn.stuRemoteDevice.szIp, 0, ip.getBytes().length);

        // 用户名
        String username = "admin";
        System.arraycopy(username.getBytes(), 0, stIn.stuRemoteDevice.szUser, 0, username.getBytes().length);

        // 密码
        String passwd = "admin";
        System.arraycopy(passwd.getBytes(), 0, stIn.stuRemoteDevice.szPwd, 0, passwd.getBytes().length);

        // 端口号
        stIn.stuRemoteDevice.nPort = 37777;

        // 设备名称
        String deviceName = "IPC111";
        System.arraycopy(deviceName.getBytes(), 0, stIn.stuRemoteDevice.szDevName, 0, deviceName.getBytes().length);


        stIn.stuRemoteDevice.nVideoInputChannels = 1;              // 视频输入通道数

        stIn.stuRemoteDevice.nAudioInputChannels = 1;              // 音频输入通道数

        ///////////////////////// 视频信息源信息传给pCameras指针 /////////////////////////
        stIn.nCameraCount = count;
        stIn.pCameras = new Memory(addCamera[0].size() * count);   // 申请内存
        stIn.pCameras.clear(addCamera[0].size() * count);

        ToolKits.SetStructArrToPointerData(addCamera, stIn.pCameras);

        // 添加视频源结果信息
        int countResult = 16;
        NET_LOGIC_BYDEVICE_ADD_CAMERA_RESULT[] addCameraResult = new NET_LOGIC_BYDEVICE_ADD_CAMERA_RESULT[countResult];
        for (int i = 0; i < countResult; i++) {
            addCameraResult[i] = new NET_LOGIC_BYDEVICE_ADD_CAMERA_RESULT();
        }

        /**
         *  出参
         */
        NET_OUT_ADD_LOGIC_BYDEVICE_CAMERA stOut = new NET_OUT_ADD_LOGIC_BYDEVICE_CAMERA();
        stOut.nMaxResultCount = countResult;   // 结果数组大小, 用户填写
        stOut.pResults = new Memory(addCameraResult[0].size() * countResult);   // 申请内存
        stOut.pResults.clear(addCameraResult[0].size() * countResult);

        ToolKits.SetStructArrToPointerData(addCameraResult, stOut.pResults);

        if (netsdkApi.CLIENT_MatrixAddCamerasByDevice(loginHandle, stIn, stOut, 4000)) {
            ToolKits.GetPointerDataToStructArr(stOut.pResults, addCameraResult);

            System.out.println("实际结果数量:" + stOut.nRetResultCount);

            for (int i = 0; i < stOut.nRetResultCount; i++) {
                if (addCameraResult[i].nFailedCode == 0) {
                    System.out.println("成功！");
                } else if (addCameraResult[i].nFailedCode == 1) {
                    System.out.println("通道不支持设置！");
                }
            }
        } else {
            System.err.println("MatrixAddCamerasByDevice Failed!" + ToolKits.getErrorCode());
        }
    }

    // 设置人脸检测 和 人脸识别的使能
    public void SetFaceDetectScene() {
        Scanner scanner = new Scanner(System.in);

        String line = "";
        while (true) {
            System.out.println("请输入通道号：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }


        int channel = Integer.parseInt(line); // 通道
        String command = NetSDKLib.CFG_CMD_ANALYSEGLOBAL;

        CFG_ANALYSEGLOBAL_INFO msg = new CFG_ANALYSEGLOBAL_INFO();

        // 获取
        if (ToolKits.GetDevConfig(loginHandle, channel, command, msg)) {
            System.out.println("应用场景 : " + new String(msg.szSceneType).trim());
			/*System.out.println("实际场景个数 : " + msg.nSceneCount);
			for(int i = 0; i < msg.nSceneCount; i++) {
				System.out.println("应用场景 : " + new String(msg.szSceneTypeListArr[i].szSceneTypeList).trim());
			}*/

            line = "";
            while (true) {
                System.out.println("请输入配置的智能事件编号[0-不设置][1-人脸识别(FaceCompare)]||[2-人脸检测(FaceAttribute)]||[3-识别和检测(FaceAnalysis)]：");
                line = scanner.nextLine();
                if (!line.equals("")) break;
            }

            scanner.close();

            // 设置
            String scene = "";
            if (Integer.parseInt(line) == 0) {

            } else if (Integer.parseInt(line) == 1) {
                scene = "TrafficParkingSpaceNoParking";
                System.arraycopy(scene.getBytes(), 0, msg.szSceneType, 0, scene.getBytes().length);        // 人脸识别场景是否有效(此场景为单使能模式会关闭人脸检测)
            } else if (Integer.parseInt(line) == 2) {
                scene = "TrafficParkingSpaceParking";
                System.arraycopy(scene.getBytes(), 0, msg.szSceneType, 0, scene.getBytes().length);        // 人脸检测场景是(此场景为单使能模式会关闭人脸识别)
            } else if (Integer.parseInt(line) == 3) {
                scene = "FaceAnalysis";
                System.arraycopy(scene.getBytes(), 0, msg.szSceneType, 0, scene.getBytes().length);        // 人脸识别及人脸检测场景同时设置
            }

            if (ToolKits.SetDevConfig(loginHandle, channel, command, msg)) {
                System.out.println("设置场景成功!");
            } else {
                System.err.println("设置场景失败!" + ToolKits.getErrorCode());
            }
        } else {
            System.err.println("获取场景失败!" + ToolKits.getErrorCode());
        }
    }

    public void SetFaceDetectEnable() {
        Scanner scanner = new Scanner(System.in);

        String line = "";
        while (true) {
            System.out.println("请输入通道号：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }
        scanner.close();

        int channel = Integer.parseInt(line); // 通道号
        String command = NetSDKLib.CFG_CMD_ANALYSERULE;

        int ruleCount = 10;  // 事件规则个数
        CFG_RULE_INFO[] ruleInfo = new CFG_RULE_INFO[ruleCount];
        for (int i = 0; i < ruleCount; i++) {
            ruleInfo[i] = new CFG_RULE_INFO();
        }

        CFG_ANALYSERULES_INFO analyse = new CFG_ANALYSERULES_INFO();
        analyse.nRuleLen = 1024 * 1024 * 40;
        analyse.pRuleBuf = new Memory(1024 * 1024 * 40);    // 申请内存
        analyse.pRuleBuf.clear(1024 * 1024 * 40);

        // 获取
        if (ToolKits.GetDevConfig(loginHandle, channel, command, analyse)) {
            int offset = 0;
            System.out.println("设备返回的事件规则个数:" + analyse.nRuleCount);

            int count = analyse.nRuleCount < ruleCount ? analyse.nRuleCount : ruleCount;

            for (int i = 0; i < count; i++) {
                ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);

                offset += ruleInfo[0].size();   // 智能规则偏移量

                switch (ruleInfo[i].dwRuleType) {
                    case NetSDKLib.EVENT_IVS_FACERECOGNITION:   // 人脸识别
                    {
                        CFG_FACERECOGNITION_INFO msg = new CFG_FACERECOGNITION_INFO();

                        ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);

                        System.out.println("规则名称：" + new String(msg.szRuleName).trim());
                        System.out.println("使能：" + msg.bRuleEnable);

                        ruleInfo[i].stuRuleCommInfo.emClassType = NetSDKLib.EVENT_IVS_FACERECOGNITION;
                        ToolKits.SetStructDataToPointer(ruleInfo[i], analyse.pRuleBuf, offset - ruleInfo[0].size());

                        // 设置使能开
                        System.arraycopy("FaceRecognition".getBytes(), 0, msg.szRuleName, 0, "FaceRecognition".getBytes().length);
                        msg.bRuleEnable = 0;
                        ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);

                        break;
                    }
                    case NetSDKLib.EVENT_IVS_FACEDETECT:    // 人脸检测
                    {
                        CFG_FACEDETECT_INFO msg = new CFG_FACEDETECT_INFO();

                        ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);

                        System.out.println("规则名称：" + new String(msg.szRuleName).trim());
                        System.out.println("使能：" + msg.bRuleEnable);


                        // 设置使能开
                        msg.bRuleEnable = 0;
                        ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);

                        break;
                    }
                    case NetSDKLib.EVENT_IVSS_FACEATTRIBUTE:    // IVSS人脸检测
                    {
                        CFG_FACEATTRIBUTE_INFO msg = new CFG_FACEATTRIBUTE_INFO();
                        ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);

                        System.out.println("规则名称：" + new String(msg.szRuleName).trim());
                        System.out.println("使能：" + msg.bRuleEnable);

                        // 设置使能开
                        msg.bRuleEnable = 0;
                        ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);
                        break;
                    }
                    case NetSDKLib.EVENT_IVS_FACEANALYSIS:    // IVSS人脸分析
                    {
                        CFG_FACEANALYSIS_INFO msg = new CFG_FACEANALYSIS_INFO();
                        ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);

                        System.out.println("规则名称：" + new String(msg.szRuleName).trim());
                        System.out.println("使能：" + msg.bRuleEnable);

                        // 设置使能开
                        msg.bRuleEnable = 0;
                        ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);

                        break;
                    }
                    case NetSDKLib.EVENT_IVSS_FACECOMPARE:        // IVSS人脸识别
                    {
                        CFG_FACECOMPARE_INFO msg = new CFG_FACECOMPARE_INFO();
                        ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);

                        System.out.println("规则名称：" + new String(msg.szRuleName).trim());
                        System.out.println("使能：" + msg.bRuleEnable);

                        // 设置使能开
                        msg.bRuleEnable = 0;
                        ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);

                        break;
                    }
                    case NetSDKLib.EVENT_IVS_CROSSLINEDETECTION: {
                        System.out.println("警戒线");
                        break;
                    }
                    case NetSDKLib.EVENT_IVS_CROSSREGIONDETECTION: {
                        System.out.println("警戒区");
                        break;
                    }
                    default:
                        break;
                }

                offset += ruleInfo[i].nRuleSize;   // 智能事件偏移量
            }

            // 设置
            if (ToolKits.SetDevConfig(loginHandle, channel, command, analyse)) {
                System.out.println("设置使能成功!");
            } else {
                System.err.println("设置使能失败!" + ToolKits.getErrorCode());
            }
        } else {
            System.err.println("获取使能失败!" + ToolKits.getErrorCode());
        }
    }

    public void SetVideoAnalyseModule() {
        int channel = 0;
        String command = NetSDKLib.CFG_CMD_ANALYSEMODULE;

        CFG_ANALYSEMODULES_INFO msg = new CFG_ANALYSEMODULES_INFO();

        // 获取
        if (ToolKits.GetDevConfig(loginHandle, channel, command, msg)) {
            msg.nMoudlesNum = 2;
            //CFG_MODULE_INFO [] cmi=new CFG_MODULE_INFO[msg.nMoudlesNum];
            for (int i = 0; i < msg.nMoudlesNum; i++) {
                //cmi[i]=new CFG_MODULE_INFO();
                msg.stuModuleInfo[i].nDetectRegionPoint = 4;
                for (int j = 0; j < 4; j++) {
                    msg.stuModuleInfo[i].stuDetectRegion[j].nX = 2000 + j * 1000;
                    msg.stuModuleInfo[i].stuDetectRegion[j].nY = 4000;
                    msg.stuModuleInfo[i].stuDetectRegion[j].nX = 2000 + j * 1000;
                    msg.stuModuleInfo[i].stuDetectRegion[j].nY = 3000;
                    msg.stuModuleInfo[i].stuDetectRegion[j].nX = 3000 + j * 1000;
                    msg.stuModuleInfo[i].stuDetectRegion[j].nY = 3000;
                    msg.stuModuleInfo[i].stuDetectRegion[j].nX = 3000 + j * 1000;
                    msg.stuModuleInfo[i].stuDetectRegion[j].nY = 4000;
                }
            }
            if (ToolKits.SetDevConfig(loginHandle, channel, command, msg)) {
                System.out.println("设置使能成功!");
            } else {
                System.err.println("设置使能失败!" + ToolKits.getErrorCode());
            }
        }
    }


    // 带回调的拉流、回放、下载等测试
    public void TestFunctionWithCallBack() {

        CommonWithCallBack commonWithCallBack = new CommonWithCallBack(loginHandle);
        commonWithCallBack.RealPlay();
        //commonWithCallBack.RealPlayByDataType();
        //commonWithCallBack.PlayBackByDataType();
        //commonWithCallBack.DownloadByDataType();

    }

    // 单独文件处理的默认注释
    // IVSS设备添加功能
	/*public void IVSSDeviceAddModule() throws InterruptedException {
		String url= "dahua://admin:admin@10.33.11.9:37777";
		AsyncDeviceManager asyncDeviceManager = new AsyncDeviceManager(loginHandle, url);
		asyncDeviceManager.GetDeviceInfo(null);
		asyncDeviceManager.RemoveDevice("sim-3777725311");
		asyncDeviceManager.ExecTask();
		System.out.println(asyncDeviceManager.nLogicChannel);
	}

	// RTMP配置
	/*public void setRTMPInfo() {
		String command = NetSDKLib.CFG_CMD_RTMP;
		int channel = -1;
		CFG_RTMP_INFO cfgRTMP = new CFG_RTMP_INFO();

		// 获取
		if(ToolKits.GetDevConfig(loginHandle, channel, command, cfgRTMP)) {

			System.out.println("RTMP配置是否开启 : " + (cfgRTMP.bEnable == 1?"已开启": "未开启"));
			System.out.println("RTMP服务器地址 : " + new String(cfgRTMP.szAddr).trim());
			System.out.println("RTMP服务器端口 : " + cfgRTMP.nPort);

			System.out.print("启用主码流通道号列表 : ");
			for(int i = 0; i < cfgRTMP.nMainChnNum; i++) {
				System.out.print(cfgRTMP.szMainChannel[i] + " ");
			}
			System.out.println();

			System.out.println("定制路径名 : " + new String(cfgRTMP.szCustomPath).trim());
			System.out.println("码流路径前缀 : " + new String(cfgRTMP.szStreamPath).trim());

			// 设置，在获取的基础上设置
			cfgRTMP.bEnable = 1;
			String szAddr = "172.23.1.101";
			ToolKits.StringToByteArray(szAddr, cfgRTMP.szAddr);
			cfgRTMP.nPort = 37777;

			if(ToolKits.SetDevConfig(loginHandle, channel, command, cfgRTMP)) {
				System.out.println("Set RTMP Succeed!");
			} else {
				System.err.println("Set RTMP Failed!" + netsdkApi.CLIENT_GetLastError());
			}

		} else {
			System.err.println("Get RTMP Failed!" + netsdkApi.CLIENT_GetLastError());
		}
	}*/

    // 地磁车位同步上报车位信息
    public void syncParkingInfo() {
        // 入参
        NET_IN_SYNC_PARKING_INFO stIn = new NET_IN_SYNC_PARKING_INFO();
        // 出参
        NET_OUT_SYNC_PARKING_INFO stOut = new NET_OUT_SYNC_PARKING_INFO();

        stIn.nChannel = 0;            // 通道号
        String strParkingNum = "WUCHE00102";
        System.arraycopy(strParkingNum.getBytes(), 0, stIn.szParkingNum, 0, strParkingNum.getBytes().length);// 车位编号

        for (int i = 1; i <= 3; ++i) {
            stIn.dwPresetNum = i;        // 预置点编号
            stIn.bHaveCar = 0;            // 车位是否有车
            stIn.bParkingFault = 0;    // 车位是否有故障
            stIn.nSnapTimes = 1 + i;        // 补拍次数
            stIn.nSnapIntervel = 4 + i;        // 补拍间隔

            if (netsdkApi.CLIENT_SyncParkingInfo(loginHandle, stIn, stOut, 3000)) {
                System.out.println("同步上报车位信息成功！");
            } else {
                System.err.println("同步上报车位信息失败！" + ToolKits.getErrorCode());
            }
        }


    }

    // 门禁事件配置
    public void AccessConfig() {
        // 获取
        String szCommand = NetSDKLib.CFG_CMD_ACCESS_EVENT;
        int nChn = 0;  // 通道
        CFG_ACCESS_EVENT_INFO access = new CFG_ACCESS_EVENT_INFO();  // m_stDeviceInfo.byChanNum为设备通道数

        if (ToolKits.GetDevConfig(loginHandle, nChn, szCommand, access)) {
            System.out.println("门禁通道名称:" + new String(access.szChannelName).trim());
            System.out.println("首卡使能:" + access.stuFirstEnterInfo.bEnable);   // 0-false; 1-true
            System.out.println("首卡权限验证通过后的门禁状态:" + access.stuFirstEnterInfo.emStatus); // 状态参考枚举  CFG_ACCESS_FIRSTENTER_STATUS
            System.out.println("需要首卡验证的时间段, 值为通道号:" + access.stuFirstEnterInfo.nTimeIndex);
        }

        // 设置
        boolean bRet = ToolKits.SetDevConfig(loginHandle, nChn, szCommand, access);
        if (bRet) {
            System.out.println("Set Succeed!");
        }
    }

    // 普通配置， 先获取，再配置
    public void GeneralConfig() {
        // 获取
        String strCmd = NetSDKLib.CFG_CMD_DEV_GENERRAL;
        int nChn = -1;

        CFG_DEV_DISPOSITION_INFO msg = new CFG_DEV_DISPOSITION_INFO();

        if (ToolKits.GetDevConfig(loginHandle, nChn, strCmd, msg)) {
            System.out.println("序列号:" + new String(msg.szMachineName).trim());

            // 设置
            String name = "4E01D85PAKFC751";

            // 因为是先获取的，设置前需要将字段内容清空
            for (int i = 0; i < msg.szMachineName.length; i++) {
                msg.szMachineName[i] = 0;
            }

            // 数组拷贝赋值
            System.arraycopy(name.getBytes(), 0, msg.szMachineName, 0, name.getBytes().length);

            if (ToolKits.SetDevConfig(loginHandle, nChn, strCmd, msg)) {
                System.out.println("Set General Succeed!");
            } else {
                System.err.println("Set General Failed!" + ToolKits.getErrorCode());
            }
        } else {
            System.err.println("Get General Failed!" + ToolKits.getErrorCode());
        }
    }

    /// 查询通话记录
    public void queryVideoTalkLog() {
        FIND_RECORD_VIDEO_TALK_LOG_CONDITION stuQueryCondition = new FIND_RECORD_VIDEO_TALK_LOG_CONDITION();
        stuQueryCondition.bCallTypeEnable = 1; // 呼叫类型查询条件有效
        stuQueryCondition.nCallTypeListNum = 2; //呼叫类型有效枚举个数
        stuQueryCondition.emCallTypeList[0] = EM_VIDEO_TALK_LOG_CALLTYPE.EM_VIDEO_TALK_LOG_CALLTYPE_INCOMING;
        stuQueryCondition.emCallTypeList[1] = EM_VIDEO_TALK_LOG_CALLTYPE.EM_VIDEO_TALK_LOG_CALLTYPE_OUTGOING;
        stuQueryCondition.bEndStateEnable = 1; // 最终状态查询条件有效
        stuQueryCondition.nEndStateListNum = 2; //最终状态有效枚举个数
        stuQueryCondition.emEndStateList[0] = EM_VIDEO_TALK_LOG_ENDSTATE.EM_VIDEO_TALK_LOG_ENDSTATE_MISSED;
        stuQueryCondition.emEndStateList[1] = EM_VIDEO_TALK_LOG_ENDSTATE.EM_VIDEO_TALK_LOG_ENDSTATE_RECEIVED;

        NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
        stuFindInParam.emType = EM_NET_RECORD_TYPE.NET_RECORD_VIDEOTALKLOG;
        stuFindInParam.pQueryCondition = stuQueryCondition.getPointer();

        NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();

        stuQueryCondition.write();
        if (netsdkApi.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) { // 按查询条件查询记录
            System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);

            int nRecordCount = 10;
            NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
            stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
            stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数

            NET_RECORD_VIDEO_TALK_LOG[] stuRecord = new NET_RECORD_VIDEO_TALK_LOG[nRecordCount];
            for (int i = 0; i < nRecordCount; i++) {
                stuRecord[i] = new NET_RECORD_VIDEO_TALK_LOG();
            }

            NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
            stuFindNextOutParam.nMaxRecordNum = nRecordCount;
            stuFindNextOutParam.pRecordList = new Memory(stuRecord[0].size() * nRecordCount);
            stuFindNextOutParam.pRecordList.clear(stuRecord[0].size() * nRecordCount);
            ToolKits.SetStructArrToPointerData(stuRecord, stuFindNextOutParam.pRecordList);  // 将数组内存拷贝到Pointer
            boolean zRet = netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000);
            if (zRet) {
                ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, stuRecord);
                System.out.println("nRetRecordNum " + stuFindNextOutParam.nRetRecordNum);
                for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; ++i) {
                    System.out.println();
                    PrintStruct.print(stuRecord[i]);
                }
            } else {
                System.err.println("FindNextRecord Failed!" + ToolKits.getErrorCode());
            }

            netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
        } else {
            System.err.println("FindRecord Failed!" + ToolKits.getErrorCode());
        }
    }

    public void GetRecordState() {
        System.out.println("方法1：CLIENT_QueryDevState(NET_DEVSTATE_RECORDING_DETAIL)获取录像状态");

        IntByReference intRetLen = new IntByReference();

        NET_RECORD_STATE_DETAIL[] stuRecordState = new NET_RECORD_STATE_DETAIL[deviceinfo.byChanNum];
        for (int i = 0; i < deviceinfo.byChanNum; i++) {
            stuRecordState[i] = new NET_RECORD_STATE_DETAIL();
        }

        int nSize = stuRecordState[0].size() * deviceinfo.byChanNum;
        Pointer plstRecordState = new Memory(nSize);
        plstRecordState.clear(nSize);
        ToolKits.SetStructArrToPointerData(stuRecordState, plstRecordState);  // 将数组内存拷贝到Pointer

        if (netsdkApi.CLIENT_QueryDevState(loginHandle,
                NetSDKLib.NET_DEVSTATE_RECORDING_DETAIL,
                plstRecordState,
                nSize,
                intRetLen,
                5000)) {

//			stuPresetList.read();
            ToolKits.GetPointerDataToStructArr(plstRecordState, stuRecordState);
            int nRetNum = intRetLen.getValue() / stuRecordState[0].size();
            System.out.println("返回个数: " + nRetNum);
            for (int i = 0; i < nRetNum; ++i) {
                System.out.println("通道: " + i);
                PrintStruct.print(stuRecordState[i]);
            }
        } else {
            System.err.println("QueryDevState Failed!" + ToolKits.getErrorCode());
        }
        Native.free(Pointer.nativeValue(plstRecordState));
        Pointer.nativeValue(plstRecordState, 0);
        System.out.println("方法2：CLIENT_GetNewDevConfig(CFG_CMD_DEVRECORDGROUP)获取录像状态");
        CFG_DEVRECORDGROUP_INFO cmdObject = new CFG_DEVRECORDGROUP_INFO();
        if (ToolKits.GetDevConfig(loginHandle, 0, NetSDKLib.CFG_CMD_DEVRECORDGROUP, cmdObject)) {
            System.out.println("通道个数: " + cmdObject.nChannelNum);
            for (int i = 0; i < cmdObject.nChannelNum; ++i) {
                PrintStruct.print(cmdObject.stuDevRecordInfo[i]);
            }
        } else {
            System.err.println("GetDevConfig Failed!" + ToolKits.getErrorCode());
        }
    }

    public void GetHardDiskState() {
        IntByReference intRetLen = new IntByReference();
        NET_DEV_HARDDISK_STATE diskInfo = new NET_DEV_HARDDISK_STATE();
        if (netsdkApi.CLIENT_QueryDevState(loginHandle,
                NetSDKLib.NET_DEVSTATE_DISK,
                diskInfo.getPointer(),
                diskInfo.size(),
                intRetLen,
                5000)) {
            diskInfo.read();

            String[] diskType = {"读写驱动器", "只读驱动器", "备份驱动器或媒体驱动器", "冗余驱动器", "快照驱动器"};
            String[] diskStatus = {"休眠", "活动", "故障"};
            String[] diskSignal = {"本地", "远程"};
            for (int i = 0; i < diskInfo.dwDiskNum; ++i) {
                System.out.println("-----------------硬盘" + (i + 1) + "-----------------");
                System.out.println("硬盘号:" + diskInfo.stDisks[i].bDiskNum + " 分区号:" + diskInfo.stDisks[i].bSubareaNum);
                System.out.println("容量:" + diskInfo.stDisks[i].dwVolume + "MB" + " 剩余空间:" + diskInfo.stDisks[i].dwFreeSpace + "MB");
                System.out.println("标识:" + diskSignal[diskInfo.stDisks[i].bSignal] +
                        " 类型:" + diskType[(diskInfo.stDisks[i].dwStatus & 0xF0) >> 4] + " 状态:" + diskStatus[diskInfo.stDisks[i].dwStatus & 0x0F]);
            }
        } else {
            System.err.println("QueryDevState NET_DEVSTATE_DISK Failed!" + ToolKits.getErrorCode());
        }
    }

    public void AroudWifiSearch() {
        // 获取配置
        CFG_WIFI_SEARCH_INFO wifiSearchInfo = new CFG_WIFI_SEARCH_INFO();
        String strCmd = NetSDKLib.CFG_CMD_WIFI_SEARCH;
        boolean bRet = ToolKits.GetDevConfig(loginHandle, -1, strCmd, wifiSearchInfo);
        if (bRet) {
            System.out.println("获取周围无线设备配置成功,wifiSearchInfo.bEnable:" + wifiSearchInfo.bEnable + ",wifiSearchInfo.nPeriod:" + wifiSearchInfo.nPeriod + ",wifiSearchInfo.bOptimizNotification:" + wifiSearchInfo.bOptimizNotification);
        } else {
            System.err.println("获取周围无线设备配置失败" + netsdkApi.CLIENT_GetLastError());
        }

        // 设置配置
        bRet = false;
        if (wifiSearchInfo.bEnable == 0) {
            wifiSearchInfo.bEnable = 1;
        } else if (wifiSearchInfo.bEnable == 1) {
            wifiSearchInfo.bEnable = 0;
        }

        wifiSearchInfo.nPeriod++;
        if (wifiSearchInfo.bOptimizNotification == 0) {
            wifiSearchInfo.bOptimizNotification = 1;
        } else if (wifiSearchInfo.bOptimizNotification == 1) {
            wifiSearchInfo.bOptimizNotification = 0;
        }
        bRet = ToolKits.SetDevConfig(loginHandle, -1, strCmd, wifiSearchInfo);
        if (bRet) {
            System.out.println("设置周围无线设备配置成功！");
        } else {
            System.err.println("设置周围无线设备配置配置失败！" + netsdkApi.CLIENT_GetLastError());
        }

        // 获取
        CFG_WIFI_SEARCH_INFO wifiSearchInfo1 = new CFG_WIFI_SEARCH_INFO();
        String strCmd1 = NetSDKLib.CFG_CMD_WIFI_SEARCH;
        boolean bRet1 = ToolKits.GetDevConfig(loginHandle, -1, strCmd1, wifiSearchInfo1);
        if (bRet1) {
            System.out.println("获取周围无线设备配置成功,wifiSearchInfo.bEnable:" + wifiSearchInfo1.bEnable + ",wifiSearchInfo.nPeriod:" + wifiSearchInfo1.nPeriod + ",wifiSearchInfo.bOptimizNotification:" + wifiSearchInfo1.bOptimizNotification);
        } else {
            System.err.println("获取周围无线设备配置失败" + netsdkApi.CLIENT_GetLastError());
        }
    }

    public void FindFaceDetection() {
        int type = EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FACE_DETECTION;

        // 查询条件
        MEDIAFILE_FACE_DETECTION_PARAM queryCondition = new MEDIAFILE_FACE_DETECTION_PARAM();

        // 图片类型,0:未知类型,1:人脸全景大图,2:人脸小图
        queryCondition.emPicType = 1;

        // 通道号从0开始,-1表示查询所有通道
        queryCondition.nChannelID = 0;

        // 开始时间
        queryCondition.stuStartTime.dwYear = 2019;
        queryCondition.stuStartTime.dwMonth = 3;
        queryCondition.stuStartTime.dwDay = 26;
        queryCondition.stuStartTime.dwHour = 0;
        queryCondition.stuStartTime.dwMinute = 0;
        queryCondition.stuStartTime.dwSecond = 0;

        // 结束时间
        queryCondition.stuEndTime.dwYear = 2019;
        queryCondition.stuEndTime.dwMonth = 3;
        queryCondition.stuEndTime.dwDay = 26;
        queryCondition.stuEndTime.dwHour = 14;
        queryCondition.stuEndTime.dwMinute = 0;
        queryCondition.stuEndTime.dwSecond = 0;

        queryCondition.write();
        LLong lFindHandle = netsdkApi.CLIENT_FindFileEx(loginHandle, type, queryCondition.getPointer(), null, 3000);
        if (lFindHandle.longValue() == 0) {
            System.err.println("FindFileEx Failed!" + netsdkApi.CLIENT_GetLastError());
            return;
        } else {
            System.out.println("FindFileEx success.");
        }
        queryCondition.read();


        int nMaxConut = 10;
        MEDIAFILE_FACE_DETECTION_INFO[] pMediaFaceDetection = new MEDIAFILE_FACE_DETECTION_INFO[nMaxConut];
        for (int i = 0; i < pMediaFaceDetection.length; ++i) {
            pMediaFaceDetection[i] = new MEDIAFILE_FACE_DETECTION_INFO();
        }

        int MemorySize = pMediaFaceDetection[0].size() * nMaxConut;
        Pointer mediaFileInfo = new Memory(MemorySize);
        mediaFileInfo.clear(MemorySize);

        ToolKits.SetStructArrToPointerData(pMediaFaceDetection, mediaFileInfo);

        //循环查询
        int nCurCount = 0;
        int nFindCount = 0;
        while (true) {
            int nRet = netsdkApi.CLIENT_FindNextFileEx(lFindHandle, nMaxConut, mediaFileInfo, MemorySize, null, 3000);
            ToolKits.GetPointerDataToStructArr(mediaFileInfo, pMediaFaceDetection);
            System.out.println("nRet : " + nRet);

            if (nRet < 0) {
                System.err.println("FindNextFileEx failed!" + netsdkApi.CLIENT_GetLastError());
                break;
            } else if (nRet == 0) {
                break;
            }

            for (int i = 0; i < nRet; i++) {
                nFindCount = i + nCurCount * nMaxConut;
                System.out.println("[" + nFindCount + "]通道号 :" + pMediaFaceDetection[i].ch);
                System.out.println("[" + nFindCount + "]开始时间 :" + pMediaFaceDetection[i].starttime.toStringTime());
                System.out.println("[" + nFindCount + "]结束时间 :" + pMediaFaceDetection[i].endtime.toStringTime());
                if (pMediaFaceDetection[i].nFileType == 1) {
                    System.out.println("[" + nFindCount + "]文件类型 : jpg图片");
                }
                System.out.println("[" + nFindCount + "]文件路径 :" + new String(pMediaFaceDetection[i].szFilePath).trim());
            }

            if (nRet < nMaxConut) {
                break;
            } else {
                nCurCount++;
            }
        }

        netsdkApi.CLIENT_FindCloseEx(lFindHandle);

    }

    // 刻录机控制 CTRLTYPE_BURNING_START
    public void startburning() {
        BURNNG_PARM openStrobe = new BURNNG_PARM();

        //通道掩码
        openStrobe.bySpicalChannel = (byte) (0x01 | openStrobe.channelMask);    //通道掩码,按位表示要刻录的通道
        openStrobe.devMask = (byte) (0x01 | openStrobe.devMask);                // 刻录机掩码,根据查询到的刻录机列表,按位表示

        openStrobe.write();

        boolean openRet = netsdkApi.CLIENT_ControlDevice(loginHandle, CtrlType.CTRLTYPE_BURNING_START, openStrobe.getPointer(), 3000);
        openStrobe.read();
        if (openRet) {
            System.out.println("start burning Success!");
        } else {
            System.err.println("start burning Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 刻录机控制 CTRLTYPE_BURNING_STOP
    public void stopburning() {
        BURNNG_PARM openStrobe = new BURNNG_PARM();

        //通道掩码
        openStrobe.bySpicalChannel = (byte) (0x01 | openStrobe.channelMask);    //通道掩码,按位表示要刻录的通道
        openStrobe.devMask = (byte) (0x01 | openStrobe.devMask);                // 刻录机掩码,根据查询到的刻录机列表,按位表示

        openStrobe.write();

        boolean openRet = netsdkApi.CLIENT_ControlDevice(loginHandle, CtrlType.CTRLTYPE_BURNING_STOP, openStrobe.getPointer(), 3000);
        openStrobe.read();
        if (openRet) {
            System.out.println("stop burning Success!");
        } else {
            System.err.println("stop burning Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    public void markFile() {
        NET_IN_SET_MARK_FILE_BY_TIME stuIn = new NET_IN_SET_MARK_FILE_BY_TIME();
        NET_OUT_SET_MARK_FILE_BY_TIME stuOut = new NET_OUT_SET_MARK_FILE_BY_TIME();

        stuIn.nChannel = -1;
        stuIn.bFlag = 1;

        stuIn.stuStartTime.dwYear = 2019;
        stuIn.stuStartTime.dwMonth = 5;
        stuIn.stuStartTime.dwDay = 1;
        stuIn.stuStartTime.dwHour = 17;
        stuIn.stuStartTime.dwMinute = 24;
        stuIn.stuStartTime.dwSecond = 0;

        stuIn.stuEndTime.dwYear = 2019;
        stuIn.stuEndTime.dwMonth = 5;
        stuIn.stuEndTime.dwDay = 16;
        stuIn.stuEndTime.dwHour = 17;
        stuIn.stuEndTime.dwMinute = 40;
        stuIn.stuEndTime.dwSecond = 11;

        boolean bret = netsdkApi.CLIENT_SetMarkFileByTime(loginHandle, stuIn, stuOut, 5000);
        if (bret) {
            System.out.println("markFile Success!");
        } else {
            System.out.println("markFile failed!");
            System.err.println("markFile Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    public void GetRecordSaveInfo() {
        NETDEV_AUTOMT_CFG stuCfg = new NETDEV_AUTOMT_CFG();
        IntByReference nReturnLen = new IntByReference(0);
        if (!netsdkApi.CLIENT_GetDevConfig(loginHandle, NetSDKLib.NET_DEV_AUTOMTCFG, -1,
                stuCfg.getPointer(), stuCfg.size(), nReturnLen, 5000)) {
            System.out.println("CLIENT_GetDevConfig NET_DEV_DEVICECFG Config Failed!" + ToolKits.getErrorCode());
        } else {
            stuCfg.read();
            System.out.println("-----------------------Storage Global-----------------------");
            PrintStruct.print(stuCfg);
        }

        System.out.println("-----------------------Storage Groups-----------------------");
        AV_CFG_StorageGroup[] storageGroups = new AV_CFG_StorageGroup[3];
        for (int i = 0; i < storageGroups.length; ++i) {
            storageGroups[i] = new AV_CFG_StorageGroup();
        }

        int retCount = ToolKits.GetDevConfig(loginHandle, -1, NetSDKLib.CFG_CMD_STORAGEGROUP, storageGroups);
        System.out.println("0~31，天为单位，0表示永不过期。和StorageGlobal.FileHoldTime共同作用，以两者较短的保留时间为准");
        System.out.println("返回个数: " + retCount);
        for (int i = 0; i < retCount; i++) {
            PrintStruct.FIELD_NOT_PRINT = "szSubDevices";
            PrintStruct.print(storageGroups[i]);
        }

    }


    // 门禁控制-重置密码
    public void ResetPasswd() {
        NET_CTRL_ACCESS_RESET_PASSWORD msg = new NET_CTRL_ACCESS_RESET_PASSWORD();
        // 门禁序号(从0开始)
        msg.nChannelID = 0;

        // 密码类型
        msg.emType = EM_ACCESS_PASSWORD_TYPE.EM_ACCESS_PASSWORD_OPENDOOR;

        // 用户ID
        String userId = "JJS555";
        System.arraycopy(userId.getBytes(), 0, msg.szUserID, 0, userId.getBytes().length);

        // 新密码
        String passwd = "admin123";
        System.arraycopy(passwd.getBytes(), 0, msg.szNewPassword, 0, passwd.getBytes().length);

        msg.write();
        boolean bRet = netsdkApi.CLIENT_ControlDevice(loginHandle, CtrlType.CTRLTYPE_CTRL_ACCESS_RESET_PASSWORD, msg.getPointer(), 3000);
        msg.read();
        if (bRet) {
            System.out.println("ResetPasswd Success!");
        } else {
            System.err.println("ResetPasswd Failed!" + getErrorCode());
        }
    }

    public static void ptzControlToXYPos() {
        PTZ_CONTROL_ABSOLUTELY absPos = new PTZ_CONTROL_ABSOLUTELY();

        int angleHorizontal = Math.round(1 * 10);
        int angleVertical = Math.round(2 * 20);
        absPos.stuPosition.nPositionX = angleHorizontal;
        absPos.stuPosition.nPositionY = angleVertical;
        absPos.stuPosition.nZoom = 4;

        if (!netsdkApi.CLIENT_DHPTZControlEx2(loginHandle, 0,
                NET_EXTPTZ_ControlType.NET_EXTPTZ_MOVE_ABSOLUTELY,
                0, 4, 0, 0, absPos.getPointer())) {

            System.err.println("MOVE_ABSOLUTELY Failed!" + getErrorCode());

        } else {
            System.out.println("MOVE_ABSOLUTELY Success!");
        }
    }


    //设置焦距
    public static void ptzControlToFocus() {
        PTZ_FOCUS_ABSOLUTELY absFocus = new PTZ_FOCUS_ABSOLUTELY();
        absFocus.dwValue = 5000;
        absFocus.dwSpeed = 5;
        absFocus.write();
        if (!netsdkApi.CLIENT_DHPTZControlEx2(loginHandle, 0,
                NET_EXTPTZ_ControlType.NET_EXTPTZ_FOCUS_ABSOLUTELY,
                0, 4, 0, 0, absFocus.getPointer())) {

            System.err.println("FOCUS_ABSOLUTELY Failed!" + getErrorCode());

        } else {
            System.out.println("FOCUS_ABSOLUTELY Success!");
        }
    }


    /**
     * 设置停车信息
     */
    public static void ctrlDeviceSetParkInfo() {
        NET_CTRL_SET_PARK_INFO stuSetParkInfo = new NET_CTRL_SET_PARK_INFO();
        stuSetParkInfo.nParkTime = 30; // 停车时长,单位:分钟
        stuSetParkInfo.nRemainDay = 3; // 到期天数
        stuSetParkInfo.nRemainSpace = 3; // 停车库余位数

        String plateNumber = "浙A1233454"; // 车牌号码
        try {
            System.arraycopy(plateNumber.getBytes("GBK"), 0, stuSetParkInfo.szPlateNumber, 0, plateNumber.getBytes("GBK").length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String userType = "yearlyCardUser"; // 用户类型
        System.arraycopy(userType.getBytes(), 0, stuSetParkInfo.szUserType, 0, userType.getBytes().length);


        stuSetParkInfo.stuInTime.setTime(2019, 7, 9, 10, 45, 0);    // 车辆入场时间
        stuSetParkInfo.stuOutTime.setTime(2019, 7, 9, 10, 52, 0); // 车辆出场时间

        stuSetParkInfo.emCarStatus = EM_CARPASS_STATUS.EM_CARPASS_STATUS_CARPASS; // 过车状态

        stuSetParkInfo.write();
        if (!netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_CTRL_SET_PARK_INFO,
                stuSetParkInfo.getPointer(), null, 3000)) {
            System.err.println("Set Park Info Failed." + getErrorCode());
        } else {
            System.out.println("Set Park Info Success.");
        }
    }

    /**
     * OSD叠加
     */
    public void osdOverlay() {

        int emOsdBlendType = NET_EM_OSD_BLEND_TYPE.NET_EM_OSD_BLEND_TYPE_MAIN; // 叠加到主码流

        { // 叠加通道标题属性配置
            int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_CHANNELTITLE;
            NET_OSD_CHANNEL_TITLE stuChannelTitle = new NET_OSD_CHANNEL_TITLE();
            stuChannelTitle.emOsdBlendType = emOsdBlendType;    // 叠加类型
            if (getConfig(emCfgOpType, stuChannelTitle)) {
                if (stuChannelTitle.bEncodeBlend == 1) {
                    System.out.println("已叠加通道标题");
                } else {
                    // 未叠加通道标题属性
                    stuChannelTitle.bEncodeBlend = 1; // 叠加
                    if (setConfig(emCfgOpType, stuChannelTitle)) {
                        System.out.println("叠加通道标题成功");
                    }
                }
            }
        }

        { // 叠加时间标题属性配置
            int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TIMETITLE;
            NET_OSD_TIME_TITLE stuTimeTitle = new NET_OSD_TIME_TITLE();
            stuTimeTitle.emOsdBlendType = emOsdBlendType;    // 叠加类型
            if (getConfig(emCfgOpType, stuTimeTitle)) {
                if (stuTimeTitle.bEncodeBlend == 1
                        && stuTimeTitle.bShowWeek == 1) {
                    System.out.println("已叠加时间标题且显示星期");
                } else {
                    // 未叠加通道标题属性
                    stuTimeTitle.bEncodeBlend = 1; // 叠加
                    stuTimeTitle.bShowWeek = 1; // 显示星期
                    if (setConfig(emCfgOpType, stuTimeTitle)) {
                        System.out.println("叠加时间标题成功");
                    }
                }
            }
        }

        { // 叠加自定义标题属性配置
            int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_CUSTOMTITLE;
            NET_OSD_CUSTOM_TITLE stuCustomTitle = new NET_OSD_CUSTOM_TITLE();
            stuCustomTitle.emOsdBlendType = emOsdBlendType;    // 叠加类型
            if (getConfig(emCfgOpType, stuCustomTitle)) {
                for (int i = 0; i < stuCustomTitle.nCustomTitleNum; ++i) {
                    try {
                        System.out.printf("自定义标题%d: %s \n", i, new String(stuCustomTitle.stuCustomTitle[i].szText, "GBK"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                stuCustomTitle.nCustomTitleNum = 2;
                try {
                    stuCustomTitle.stuCustomTitle[0].bEncodeBlend = 1; // 叠加
                    stuCustomTitle.stuCustomTitle[0].stuRect.left = 800;
                    stuCustomTitle.stuCustomTitle[0].stuRect.top = 800;
                    stuCustomTitle.stuCustomTitle[0].stuRect.right = stuCustomTitle.stuCustomTitle[0].stuRect.left;
                    stuCustomTitle.stuCustomTitle[0].stuRect.bottom = stuCustomTitle.stuCustomTitle[0].stuRect.top;
                    copyByteArray("Custom Title 111111111111111111111111".getBytes(), stuCustomTitle.stuCustomTitle[0].szText);

                    stuCustomTitle.stuCustomTitle[1].bEncodeBlend = 1; // 叠加
//					stuCustomTitle.stuCustomTitle[1].stuRect.left = 0;
//					stuCustomTitle.stuCustomTitle[1].stuRect.top = 650;
//					stuCustomTitle.stuCustomTitle[1].stuRect.right = 3000;
//					stuCustomTitle.stuCustomTitle[1].stuRect.bottom = 1150;
                    copyByteArray("自 定 义 标 题 2".getBytes("GBK"), stuCustomTitle.stuCustomTitle[1].szText);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (setConfig(emCfgOpType, stuCustomTitle)) {
                    System.out.println("叠加自定义标题成功");
                }
            }
        }

        { // 叠加自定义标题对齐方式属性配置
            int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_CUSTOMTITLETEXTALIGN;
            NET_OSD_CUSTOM_TITLE_TEXT_ALIGN stuCustomTitleAlign = new NET_OSD_CUSTOM_TITLE_TEXT_ALIGN();
            if (getConfig(emCfgOpType, stuCustomTitleAlign)) {
                for (int i = 0; i < stuCustomTitleAlign.nCustomTitleNum; ++i) {
                    System.out.printf("自定义标题%d对齐方式: %d \n", i, stuCustomTitleAlign.emTextAlign[i]);
                    stuCustomTitleAlign.emTextAlign[i] = EM_TITLE_TEXT_ALIGNTYPE.EM_TEXT_ALIGNTYPE_CENTER; // 居中
                }
                if (stuCustomTitleAlign.nCustomTitleNum > 0) {
                    if (setConfig(emCfgOpType, stuCustomTitleAlign)) {
                        System.out.println("叠加自定义标题对齐方式成功");
                    }
                } else {
                    System.out.println("尚未叠加自定义标题");
                }
            }
        }


        { // 叠加公共属性配置 (实际测试时并不起作用)
            int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_OSDCOMMINFO;
            NET_OSD_COMM_INFO stuCommInfo = new NET_OSD_COMM_INFO();
            if (getConfig(emCfgOpType, stuCommInfo)) {
                System.out.printf("叠加到主码流上的字体大小放大比例:%f 全局字体大小:%dpx \n", stuCommInfo.fFontSizeScale, stuCommInfo.nFontSize);
                stuCommInfo.fFontSizeScale = 0;    // 放大比例
                stuCommInfo.nFontSize = 20;    // 修改为25px
                if (setConfig(emCfgOpType, stuCommInfo)) {
                    System.out.println("叠加公共属性成功");
                }
            }
        }
    }

    public static void copyByteArray(byte[] src, byte[] dst) {
        Arrays.fill(dst, (byte) 0);
        int len = src.length > dst.length - 1 ? dst.length - 1 : src.length;
        System.arraycopy(src, 0, dst, 0, len);
    }


    /**
     * 获取配置信息
     *
     * @param emCfgOpType
     * @param stuCfg
     * @return true--success , false--failed.
     */
    public boolean getConfig(int emCfgOpType, SdkStructure stuCfg) {
        int nChannelID = 0;    // 通道号
        stuCfg.write();
        if (!netsdkApi.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID, stuCfg.getPointer(),
                stuCfg.size(), 4000, null)) {
            System.err.println("GetConfig Failed!" + ToolKits.getErrorCode());
            return false;
        }

        stuCfg.read();
        return true;
    }

    /**
     * 设置配置信息（在获取的基础上设置）
     *
     * @param emCfgOpType
     * @param stuCfg
     * @return true--success , false--failed.
     */
    public boolean setConfig(int emCfgOpType, SdkStructure stuCfg) {
        int nChannelID = 0; // 通道号
        stuCfg.write();
        if (!netsdkApi.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID, stuCfg.getPointer(),
                stuCfg.size(), 4000, null, null)) {
            System.err.println("SetConfig Failed!" + ToolKits.getErrorCode());
            return false;
        }
        return true;
    }

    // 订阅无人机实时消息
    public void attachUAVInfo() {
        NET_IN_ATTACH_UAVINFO stuIn = new NET_IN_ATTACH_UAVINFO();
        stuIn.cbNotify = UAVInfoCallBack.getInstance();

        NET_OUT_ATTACH_UAVINFO stuOut = new NET_OUT_ATTACH_UAVINFO();
        m_hAttachParkHandle = netsdkApi.CLIENT_AttachUAVInfo(loginHandle, stuIn, stuOut, 3000);
        if (0 != m_hAttachParkHandle.longValue()) {
            System.out.println("AttachUAVInfo Succeed! \r\n");
        } else {
            System.err.println("AttachUAVInfo Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    // 订阅车位信息回调函数原型
    public static class UAVInfoCallBack implements fUAVInfoCallBack {

        private UAVInfoCallBack() {
        }

        private static class CallBackHolder {
            private static final UAVInfoCallBack cb = new UAVInfoCallBack();
        }

        public static final UAVInfoCallBack getInstance() {
            return CallBackHolder.cb;
        }

        @Override
        public void invoke(LLong lAttachHandle, NET_UAVINFO pstuUAVInfo,
                           int dwUAVInfoSize, Pointer dwUser) {

            switch (pstuUAVInfo.emType) { // 无人机实时消息类型
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_HEARTBEAT:  // 心跳状态
                {
                    NET_UAV_HEARTBEAT msg = new NET_UAV_HEARTBEAT();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【心跳状态】 飞行模式:" + msg.emUAVMode + " 飞行器形态类型:" + msg.emUAVType
                            + " 系统状态:" + msg.emSystemStatus);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_SYS_STATUS:  // 系统状态
                {
                    NET_UAV_SYS_STATUS msg = new NET_UAV_SYS_STATUS();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【系统状态】 距离Home的距离(m):" + msg.nHomeDistance + " 剩余飞行时间(s):" + msg.nRemainingFlightTime
                            + " 剩余电量百分比:" + msg.nRemainingBattery);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_GPS_STATUS:  // GPS状态
                {
                    NET_UAV_GPS_STATUS msg = new NET_UAV_GPS_STATUS();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【GPS状态】 可见卫星个数:" + msg.nVisibleNum);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_ATTITUDE:  // 姿态信息
                {
                    NET_UAV_ATTITUDE msg = new NET_UAV_ATTITUDE();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【姿态信息】 滚转角:" + msg.fRollAngle + " 俯仰角:" + msg.fPitchAngle
                            + " 偏航角:" + msg.fYawAngle);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_RC_CHANNELS:  // 遥控通道信息
                {
                    NET_UAV_RC_CHANNELS msg = new NET_UAV_RC_CHANNELS();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【遥控通道信息】 遥控器信号百分比:" + msg.nControllerSignal);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_VFR_HUD:  // 平视显示信息
                {
                    NET_UAV_VFR_HUD msg = new NET_UAV_VFR_HUD();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【平视显示信息】 水平速度(m/s):" + msg.fGroundSpeed
                            + " 高度(m):" + msg.fAltitude + " 垂直速度(m/s):" + msg.fClimbSpeed);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_STATUSTEXT:  // 报警文本信息
                {
                    NET_UAV_STATUSTEXT msg = new NET_UAV_STATUSTEXT();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【报警文本信息】 故障等级:" + msg.emSeverity + " 文本信息:" + new String(msg.szText).trim());
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_GLOBAL_POSITION:  // 全球定位数据
                {
                    NET_UAV_GLOBAL_POSITION msg = new NET_UAV_GLOBAL_POSITION();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【全球定位数据】 纬度:" + msg.fLatitude + " 经度:" + msg.fLongitude
                            + " 海拔高度(cm):" + msg.nAltitude);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_GPS_RAW:  // GPS原始数据
                {
                    NET_UAV_GPS_RAW msg = new NET_UAV_GPS_RAW();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【GPS原始数据】 定位类型:" + msg.nFixType + " 整体移动方向(100*度):" + msg.nCourseOverGround);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_SYS_TIME:  // 系统时间
                {
                    NET_UAV_SYS_TIME msg = new NET_UAV_SYS_TIME();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【系统时间】 UTC:" + msg.UTC.toStringTime() + " 启动时间(ms):" + msg.dwBootTime);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_MISSION_CURRENT:  // 当前航点
                {
                    NET_UAV_MISSION_CURRENT msg = new NET_UAV_MISSION_CURRENT();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【当前航点】 序号:" + msg.nSequence);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_MOUNT_STATUS:  // 云台姿态
                {
                    NET_UAV_MOUNT_STATUS msg = new NET_UAV_MOUNT_STATUS();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【云台姿态】 滚转角:" + msg.fRollAngle + " 俯仰角:" + msg.fPitchAngle
                            + " 偏航角:" + msg.fYawAngle + " 目标系统:" + msg.nTargetSystem);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_HOME_POSITION:  // Home点位置信息
                {
                    NET_UAV_HOME_POSITION msg = new NET_UAV_HOME_POSITION();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【Home点位置信息】 纬度:" + msg.fLatitude + " 经度:" + msg.fLongitude
                            + " 海拔高度(cm):" + msg.nAltitude);
                    break;
                }
                case EM_UAVINFO_TYPE.EM_UAVINFO_TYPE_MISSION_REACHED:  // 到达航点
                {
                    NET_UAV_MISSION_REACHED msg = new NET_UAV_MISSION_REACHED();
                    ToolKits.GetPointerData(pstuUAVInfo.pInfo, msg);
                    System.out.println("【到达航点】 序号:" + msg.nSequence);
                    break;
                }
                default:
                    break;
            }
        }
    }

    // 退订无人机实时消息
    public void detachUAVInfo() {
        boolean bRet = netsdkApi.CLIENT_DetachUAVInfo(m_hAttachParkHandle);
        if (bRet) {
            System.out.println("DetachUAVInfo Succeed! \r\n");
        } else {
            System.err.println("DetachUAVInfo Failed!" + ToolKits.getErrorCode());
        }
    }

    // 备用方案
    public void getChannelName() {
        String channelName = "";
        int channel = 0;
        AV_CFG_ChannelName channelTitleName = new AV_CFG_ChannelName();
        if (ToolKits.GetDevConfig(loginHandle, channel, NetSDKLib.CFG_CMD_CHANNELTITLE, channelTitleName)) {
            try {
                channelName = new String(channelTitleName.szName, "GBK");
            } catch (Exception e) {
                System.err.println("getChannelName Failed!");
            }
        } else {
            System.err.println("Get Channel Name Failed." + ToolKits.getErrorCode());
        }
        System.out.println(channelName);
    }

    // 查询通道状态
    public void QueryDeviceState(LLong m_hLoginHandle, int channel) {

        NETDEV_VIRTUALCAMERA_STATE_INFO info = new NETDEV_VIRTUALCAMERA_STATE_INFO();

        info.nChannelID = channel;

        info.write();
        boolean bRet = netsdkApi.CLIENT_QueryDevState(m_hLoginHandle, NetSDKLib.NET_DEVSTATE_VIRTUALCAMERA,
                info.getPointer(), info.size(), new IntByReference(0), 5000);
        info.read();

        if (bRet) {
            try {
                // UNCONNECT = 0;CONNECTING = 1; CONNECTED = 2;
                System.out.println("通道：" + info.nChannelID + "->" + "名称: " + new String(info.szDeviceName, encode).trim() + " -> " + info.emConnectState);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Query Failed!" + ToolKits.getErrorCode());
        }

    }

    public void QueryDeviceStateTest() {

        for (int i = 0; i < deviceinfo.byChanNum; i++) {
            QueryDeviceState(loginHandle, i);
        }

    }


    ////////////////////////////////////////////////////////////////

//    String m_strIp = "172.23.12.14"; // 172.10.199.198"; // 172.24.2.40
    String m_strIp = "10.80.9.44"; // 172.10.199.198"; // 172.24.2.40
    int m_nPort = 37777;
    String m_strUser = "admin";
//    String m_strPassword = "admin123";
    String m_strPassword = "dahua2020";
    ////////////////////////////////////////////////////////////////

    public void RunTest() {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
/*
		menu.addItem(new CaseMenu.Item(this , "ResetPasswd" , "ResetPasswd"));
		menu.addItem(new CaseMenu.Item(this , "startListen" , "startListen"));
		menu.addItem(new CaseMenu.Item(this , "stopListen" , "stopListen"));
		menu.addItem(new CaseMenu.Item(this , "addFaceInfo" , "addFaceInfo"));
		menu.addItem(new CaseMenu.Item(this , "updateFaceeInfo" , "updateFaceeInfo"));
		menu.addItem(new CaseMenu.Item(this , "removeFaceInfo" , "removeFaceInfo"));
		menu.addItem(new CaseMenu.Item(this , "getFaceInfo" , "getFaceInfo"));
		menu.addItem(new CaseMenu.Item(this , "SetFaceDetectScene" , "SetFaceDetectScene"));
		menu.addItem(new CaseMenu.Item(this , "SetFaceDetectEnable" , "SetFaceDetectEnable"));
		menu.addItem(new CaseMenu.Item(this , "SetVideoAnalyseModule" , "SetVideoAnalyseModule"));
		menu.addItem(new CaseMenu.Item(this , "flowstateIVSEvent", "flowstateIVSEvent"));
		menu.addItem(new CaseMenu.Item(this , "detachIVSEvent", "detachIVSEvent"));
		menu.addItem(new CaseMenu.Item(this , "TestFunctionWithCallBack" , "TestFunctionWithCallBack"));
		menu.addItem(new CaseMenu.Item(this , "syncParkingInfo" , "syncParkingInfo"));
		menu.addItem(new CaseMenu.Item(this , "queryVideoTalkLog" , "queryVideoTalkLog"));
*/
        menu.addItem(new CaseMenu.Item(this, "ptzControlToFocus", "ptzControlToFocus"));
        menu.addItem(new CaseMenu.Item(this, "QueryPTZInfo", "QueryPTZInfo"));
        menu.addItem(new CaseMenu.Item(this, "QueryDeviceStateTest", "QueryDeviceStateTest"));
/*		menu.addItem(new CaseMenu.Item(this , "SetFaceDetectScene", "SetFaceDetectScene"));
		menu.addItem(new CaseMenu.Item(this, "startListen", "startListen"));
		menu.addItem(new CaseMenu.Item(this, "startListen", "startListen"));
		menu.addItem(new CaseMenu.Item(this , "stopListen" , "stopListen"));*/
        menu.addItem(new CaseMenu.Item(this, "QueryDeviceTime", "QueryDeviceTime"));
        menu.addItem(new CaseMenu.Item(this, "SetupDeviceTime", "SetupDeviceTime"));


        menu.run();
    }

    public static void main(String[] args) {
        DemoCommon demo = new DemoCommon();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}

