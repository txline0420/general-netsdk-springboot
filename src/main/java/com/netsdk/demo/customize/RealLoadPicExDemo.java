package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.*;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;

import static com.netsdk.lib.NetSDKLib.NET_MAX_DETECT_REGION_NUM;

public class RealLoadPicExDemo extends Initialization {


    int channel= -1;

    NetSDKLib.LLong    attachHandle=new NetSDKLib.LLong(0);
    /**
     * 订阅智能任务
     */

    public NetSDKLib.LLong AttachEventRealLoadPic() {
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        if(attachHandle.longValue()!=0){
            this.DetachEventRealLoadPic();
        }

        // 需要图片
        int bNeedPicture = 1;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, EM_EVENT_TYPE.EVENT_IVS_ALL.getType(), bNeedPicture,
                AnalyzerDataCB.getInstance(), null, null);
        if (attachHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
                    ToolKits.getErrorCode());
        }

        return attachHandle;
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

        @Override
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) throws UnsupportedEncodingException {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }

            System.out.println("===================================EVENT RECEIVED=======================================");
//            System.out.println(Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType)).getDescription());
            switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
                case EVENT_IVS_TRAFFIC_THROW : {
                    System.out.println("交通抛洒物品事件, code = " + dwAlarmType);

                    DEV_EVENT_TRAFFIC_THROW_INFO msg = new DEV_EVENT_TRAFFIC_THROW_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName:" + new String(szName));
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //UTC时间
                    NET_TIME_EX UTC = msg.UTC;
                    System.out.println("UTC:" + UTC.toStringTime());
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //事件对应文件信息
                    NetSDKLib.NET_EVENT_FILE_INFO stuFileInfo = msg.stuFileInfo;
                    System.out.println("stuFileInfo:" + stuFileInfo.toString());
                    //图片分辨率
                    NetSDKLib.NET_RESOLUTION_INFO stuResolution = msg.stuResolution;
                    System.out.println("stuResolution:" + stuResolution.toString());
                    //图片
                    int picSizes = 0;
                    NetSDKLib.NET_PIC_INFO stPicInfo = msg.stuObject.stPicInfo;
                    System.out.println("stPicInfo:" + stPicInfo.toString());
                    //图片保存
                    if (stPicInfo != null && stPicInfo.dwFileLenth > 0) {
                        String picture = picturePath + "/" + System.currentTimeMillis() + "related_EVENT_IVS_TRAFFIC_THROW.jpg";
                        ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, picture);
                    }
                    //抓图标志(按位),0位:"*",1位:"Timing",2位:"Manual",3位:"Marked",4位:"Event",5位:"Mosaic",6位:"Cutout"
                    int dwSnapFlagMask = msg.dwSnapFlagMask;
                    System.out.println("dwSnapFlagMask:" + dwSnapFlagMask);
                    //事件动作,0表示脉冲事件,1表示持续性事件开始,2表示持续性事件结束
                    byte bEventAction = msg.bEventAction;
                    System.out.println("bEventAction:" + bEventAction);
                    //智能事件所属大类
                    int ClassType = msg.stuIntelliCommInfo.emClassType;
                    System.out.println("emClassType:" + EM_CLASS_TYPE.getNoteByValue(ClassType));
                    //对应车道号
                    int nLane = msg.nLane;
                    System.out.println("nLane:" + nLane);
                    break;
                }
                case EVENT_IVS_DREGS_UNCOVERED : {
                    System.out.println("渣土车未遮盖载货检测事件, code = " + dwAlarmType);

                    DEV_EVENT_DREGS_UNCOVERED_INFO msg = new DEV_EVENT_DREGS_UNCOVERED_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName:" + new String(szName));
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //UTC时间
                    NET_TIME_EX UTC = msg.UTC;
                    System.out.println("UTC:" + UTC.toStringTime());
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //智能事件所属大类
                    int ClassType = msg.emClassType;
                    System.out.println("emClassType:" + EM_CLASS_TYPE.getNoteByValue(ClassType));
                    //检测区域,[0,8191]
                    int nDetectRegionNum = msg.nDetectRegionNum;
                    System.out.println("nDetectRegionNum:" + nDetectRegionNum);
                    //事件对应文件信息
                    NetSDKLib.NET_POINT[] stuDetectRegion = msg.stuDetectRegion;
                    for(int i = 0; i < NET_MAX_DETECT_REGION_NUM; i ++){
                        System.out.println("stuDetectRegion[" + i + "]:" + stuDetectRegion[i].toString());
                    }
                    //渣土车车辆信息
                    DREGS_UNCOVERED_VEHICLE_INFO stuVehicleInfo = msg.stuVehicleInfo;
                    System.out.println("szPlateNumber:" + new String(stuVehicleInfo.szPlateNumber).trim());
                    System.out.println("stuBoundingBox:" + stuVehicleInfo.stuBoundingBox.toString());
                    //图片保存
                    if (pBuffer != null) {
                        String picture = picturePath + "/" + System.currentTimeMillis() + "related_EVENT_IVS_DREGS_UNCOVERED.jpg";
                        ToolKits.savePicture(pBuffer, 0, dwBufSize, picture);
                    }
                    break;
                }
                case EVENT_IVS_TRAFFIC_VEHICLE_CLEANLINESS : {
                    System.out.println("交通车辆清洁度检测事件检测, code = " + dwAlarmType);

                    DEV_EVENT_TRAFFIC_VEHICLE_CLEANLINESS_INFO msg = new DEV_EVENT_TRAFFIC_VEHICLE_CLEANLINESS_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName:" + new String(szName).trim());
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //UTC时间
                    NET_TIME_EX UTC = msg.UTC;
                    System.out.println("UTC:" + UTC.toStringTime());
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //事件id
                    int nRuleId = msg.nRuleId;
                    System.out.println("nRuleId:" + nRuleId);

                    //事件id
                    int objObjectID = msg.stuObject.nObjectID;
                    System.out.println("ObjObjectID:" + objObjectID);

                    //事件id
                    String objName = new String(msg.stuObject.szObjectType).trim();
                    System.out.println("objName:" + objName);
                    //图片
                    NetSDKLib.NET_PIC_INFO stPicInfo = msg.stuObject.stPicInfo;
//                    System.out.println("stPicInfo:" + stPicInfo.toString());
                    //图片保存
                    if (stPicInfo != null && stPicInfo.dwFileLenth > 0) {
                        String picture = picturePath + "/" + System.currentTimeMillis() + "related_EVENT_IVS_TRAFFIC_VEHICLE_CLEANLINESS.jpg";
                        System.out.println("Picture saved, the path is  " + picture);
                        ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, picture);
                    }
                    //图片
                    NetSDKLib.NET_PIC_INFO stPicInfo1 = msg.stuVehicle.stPicInfo;
//                    System.out.println("stPicInfo1:" + stPicInfo1.toString());
                    //图片保存
                    if (stPicInfo1 != null && stPicInfo1.dwFileLenth > 0) {
                        String picture = picturePath + "/" + System.currentTimeMillis() + "related_VehiclePic.jpg";
                        System.out.println("Picture saved, the path is  " + picture);
                        ToolKits.savePicture(pBuffer, stPicInfo1.dwOffSet, stPicInfo1.dwFileLenth, picture);
                    }
                    //事件id
                    int emTriggerType = msg.emTriggerType;
                    System.out.println("emTriggerType:" + emTriggerType);
                    //事件id
                    int nCleanValue = msg.nCleanValue;
                    System.out.println("nCleanValue:" + nCleanValue);
                    break;
                }
                case  EVENT_IVS_TRAFFIC_FLOWSTATE : {
                    System.out.println("交通流量事件, code = " + dwAlarmType);

                    NetSDKLib.DEV_EVENT_TRAFFIC_FLOW_STATE msg = new NetSDKLib.DEV_EVENT_TRAFFIC_FLOW_STATE();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName:" + new String(szName).trim());
                    // 规则编号
                    int nRuleID = msg.nRuleID;
                    System.out.println("nRuleID:" + nRuleID);
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //UTC时间
                    NetSDKLib.NET_TIME_EX UTC = msg.UTC;
                    System.out.println("UTC:" + UTC.toStringTime());
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //序号
                    int Sequence = msg.nSequence;
                    System.out.println("nSequence:" + Sequence);
                    //流量状态数量
                    int nStateNum = msg.nStateNum;
                    System.out.println("nStateNum:" + nStateNum);
                    //流量状态
                    for(int i = 0; i < nStateNum; i ++){
                        System.out.println("stuStates[" + i + "]:" + msg.stuStates[i].toString());
                    }
                    //该事件触发的预置点
                    int nPresetID = msg.stuIntelliCommInfo.nPresetID;
                    System.out.println("nPresetID:" + nPresetID);
                    //智能事件所属大类
                    String emClassType = EM_CLASS_TYPE.getNoteByValue(msg.stuIntelliCommInfo.emClassType);
                    System.out.println("emClassType:" + emClassType);
                    //流量状态数量
                    int nStopVehiclenum = msg.nStopVehiclenum;
                    System.out.println("nStopVehiclenum:" + nStopVehiclenum);
                    //流量状态数量
                    int nDetectionAreaVehicleNum = msg.nDetectionAreaVehicleNum;
                    System.out.println("nDetectionAreaVehicleNum:" + nDetectionAreaVehicleNum);
                    //溢出状态
                    for(int i = 0; i < msg.nStateNum; i ++){
                        int emOverflowState = msg.stuStates[i].emOverflowState;
                        System.out.println("emOverflowState[" + i + "]:" +  NET_EM_OVER_FLOW_STATE.getNoteByValue(emOverflowState));
                    }
                    //图片保存
                    if (msg != null) {
                        String picture = picturePath + "/" + System.currentTimeMillis() + "related_TrafficFlowState.jpg";
                        System.out.println("Picture saved, the path is  " + picture);
                        ToolKits.savePicture(pBuffer, dwBufSize, picture);
                    }
                    break;
                }
                case  EVENT_IVS_HELMET_DETECTION : {
                    System.out.println("安全帽检测事件, code = " + dwAlarmType);

                    DEV_EVENT_HELMET_DETECTION_INFO msg = new DEV_EVENT_HELMET_DETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    // 动作
                    int nAction = msg.nAction;
                    System.out.println("nAction:" + nAction);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName UTF-8:" + new String(szName,"UTF-8").trim());
                    System.out.println("szName GBK:" + new String(szName,"GBK").trim());
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //UTC时间
                    NET_TIME_EX UTC = msg.UTC;
                    System.out.println("UTC:" + UTC.toStringTime());
                    //事件类型
                    String emClassType = EM_CLASS_TYPE.getNoteByValue(msg.emClassType);
                    System.out.println("emClassType:" + emClassType);
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //规则编号
                    int nRuleID = msg.nRuleID;
                    System.out.println("nRuleID:" + nRuleID);
                    //目标ID
                    int nObjectID = msg.nObjectID;
                    System.out.println("nObjectID:" + nObjectID);
                    //触发事件类型
                    String emHelmetEventType = EM_HELMET_EVENT_TYPE.getNoteByValue(msg.emHelmetEventType);
                    System.out.println("emHelmetEventType:" + emHelmetEventType);
                    //事件对应文件信息
                    NetSDKLib.NET_EVENT_FILE_INFO stuFileInfo = msg.stuFileInfo;
                    System.out.println("stuFileInfo:" + stuFileInfo.toString());
                    //图片保存
                    if (msg != null) {
                        String picture = picturePath + "/" + System.currentTimeMillis() + "related_HelmetHuman" + msg.stuHumanImage.nIndexInData + ".jpg";
                        System.out.println("Picture saved, the path is  " + picture);
                        ToolKits.savePicture(pBuffer, msg.stuHumanImage.nOffSet, msg.stuHumanImage.nLength , picture);
                    }
                    //图片保存
                    if (msg != null) {
                        String picture = picturePath + "/" + System.currentTimeMillis() + "related_HelmetScene" + msg.stuSceneImage.nIndexInData + ".jpg";
                        System.out.println("Picture saved, the path is  " + picture);
                        ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength , picture);
                    }
                    break;
                }
                case  EVENT_IVS_WORKSTATDETECTION : {
                    System.out.println("作业统计事件, code = " + dwAlarmType);

                    DEV_EVENT_WORKSTATDETECTION_INFO msg = new DEV_EVENT_WORKSTATDETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    // 动作
                    int nAction = msg.nAction;
                    System.out.println("nAction:" + nAction);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName UTF-8:" + new String(szName,"UTF-8").trim());
                    System.out.println("szName GBK:" + new String(szName,"GBK").trim());
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //UTC时间
                    NET_TIME_EX UTC = msg.UTC;
                    System.out.println("UTC:" + UTC.toStringTime());
                    //事件类型
                    String emClassType = EM_CLASS_TYPE.getNoteByValue(msg.emClassType);
                    System.out.println("emClassType:" + emClassType);
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //规则编号
                    int nRuleID = msg.nRuleID;
                    System.out.println("nRuleID:" + nRuleID);
                    //目标ID
                    int nObjectID = msg.nObjectID;
                    System.out.println("nObjectID:" + nObjectID);
                    //作业行为状态个数
                    int nWorkActionNum = msg.nWorkActionNum;
                    System.out.println("nWorkActionNum:" + nWorkActionNum);
                    //作业行为状态信息
                    for(int i = 0; i < nWorkActionNum; i ++){
                        System.out.println("emWorkAction[" + i + "]:" + EM_WORKACTION_STATE.getNoteByValue(msg.emWorkAction[i]));
                    }
                    //触发事件类型
                    String emRuleType = EM_WORKSTATDETECTION_TYPE.getNoteByValue(msg.emRuleType);
                    System.out.println("emRuleType:" + emRuleType);
                    //图片保存
                    if (msg != null) {
                        String picture = picturePath + "/" + System.currentTimeMillis() + "related_WorkStateScene" + msg.stuSceneImage.nIndexInData + ".jpg";
                        System.out.println("Picture saved, the path is  " + picture);
                        ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength , picture);
                    }
                    break;
                }
                case EVENT_IVS_FACERECOGNITION:  // 人脸识别事件
                {
                    System.out.println("人脸识别事件, code = " + dwAlarmType);
                    NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO msg = new NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    System.out.println("szVideoPath : " + new String(msg.szVideoPath).trim() + "\n" );
                    break;

                }
                case EVENT_IVS_WORKCLOTHES_DETECT: ///<  安全帽检测事件
                {
                    System.out.println("工装检测事件, code = " + dwAlarmType);
                    NetSDKLib.DEV_EVENT_WORKCLOTHES_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_WORKCLOTHES_DETECT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    // 动作
                    int nAction = msg.nAction;
                    System.out.println("nAction:" + nAction);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName UTF-8:" + new String(szName,"UTF-8").trim());
                    System.out.println("szName GBK:" + new String(szName,"GBK").trim());
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //UTC时间
                    NetSDKLib.NET_TIME_EX UTC = msg.UTC;
                    System.out.println("UTC:" + UTC.toStringTime());
                    //事件类型
                    String emClassType = EM_CLASS_TYPE.getNoteByValue(msg.emClassType);
                    System.out.println("emClassType:" + emClassType);
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
                        String bigPicture = picturePath + "\\" + System.currentTimeMillis() + "related_WorkClothes" + msg.stuSceneImage.nIndexInData + ".jpg";
                        ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
                        if (msg.stuHumanImage != null && msg.stuHumanImage.nLength > 0) {
                            String smallPicture = picturePath + "\\" + System.currentTimeMillis() + "related_WorkClothesSmall" + msg.stuSceneImage.nIndexInData + "small.jpg";
                            ToolKits.savePicture(pBuffer, msg.stuHumanImage.nOffSet, msg.stuHumanImage.nLength, smallPicture);
                        }
                    }
                    System.out.println(" 安全帽检测事件(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                    break;
                }
                case EVENT_IVS_NUMBERSTAT: ///<  数量统计事件
                {
                    System.out.println("数量统计事件, code = " + dwAlarmType);
                    DEV_EVENT_NUMBERSTAT_INFO msg = new DEV_EVENT_NUMBERSTAT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    // 动作
                    int nNumber = msg.nNumber;
                    System.out.println("nNumber:" + nNumber);
                    // 动作
                    int nEnteredNumber = msg.nEnteredNumber;
                    System.out.println("nEnteredNumber:" + nEnteredNumber);
                    // 动作
                    int nExitedNumber = msg.nExitedNumber;
                    System.out.println("nExitedNumber:" + nExitedNumber);
                    // 动作
                    int emType = msg.emType;
                    System.out.println("emType:" + emType + "," + EM_NUMBER_STAT_TYPE.getNoteByValue(emType));
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName UTF-8:" + new String(szName,"UTF-8").trim());
                    System.out.println("szName GBK:" + new String(szName,"GBK").trim());
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
//                    NET_IMAGE_INFO_EX2 pstuImageInfo = new NET_IMAGE_INFO_EX2();
//                    ToolKits.GetPointerDataToStruct(msg.pstuImageInfo, 0, pstuImageInfo);
//                    System.out.println("pstuImageInfo.nOffset:" + pstuImageInfo.nOffset);
//                    System.out.println("pstuImageInfo.nLength:" + pstuImageInfo.nLength);
                    if (pBuffer != null && dwBufSize > 0) {
                        System.out.println("basic picture received!");
                        String bigPicture = picturePath + "\\" + System.currentTimeMillis() + "related_NumberStat_basic" + ".jpg";
                        ToolKits.savePicture(pBuffer, dwBufSize, bigPicture);
                    }
//                    if (pstuImageInfo != null && pstuImageInfo.nLength > 0) {
//                        System.out.println("pstuImageInfo picture received!");
//                        String bigPicture = picturePath + "\\" + System.currentTimeMillis() + "related_NumberStat_pstuImageInfo" + ".jpg";
//                        ToolKits.savePicture(pBuffer, pstuImageInfo.nOffset, pstuImageInfo.nLength, bigPicture);
//                    }
                    break;
                }
                case EVENT_IVS_TRAFFIC_NONMOTOR:  // 交通非机动车事件检测
                {
                    System.out.println("交通非机动车事件检测, code = " + dwAlarmType);
                    DEV_EVENT_TRAFFIC_NONMOTOR_INFO msg = new DEV_EVENT_TRAFFIC_NONMOTOR_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    // 动作
                    int nAction = msg.nAction;
                    System.out.println("nAction:" + nAction);
                    // 规则
                    int nRuleId = msg.nRuleId;
                    System.out.println("nRuleId:" + nRuleId);
                    // 触发方式
                    int emTriggerType = msg.emTriggerType;
                    System.out.println("emTriggerType:" + emTriggerType + "," + EM_TRIGGER_TYPE.getNoteByValue(emTriggerType));
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName UTF-8:" + new String(szName,"UTF-8").trim());
                    System.out.println("szName GBK:" + new String(szName,"GBK").trim());
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //图片保存
                    if (pBuffer != null && dwBufSize > 0) {
                        System.out.println("basic picture received!");
                        String bigPicture = picturePath + "\\" + System.currentTimeMillis() + "related_TrafficNoMotor_basic" + ".jpg";
                        ToolKits.savePicture(pBuffer, dwBufSize, bigPicture);
                    }
                    if (pBuffer != null && dwBufSize > 0 && msg.stuNonMotor.stuSceneImage.nLength > 0) {
                        System.out.println("Scene picture received!");
                        String scenePicture = picturePath + "\\" + System.currentTimeMillis() + "related_TrafficNoMotor_scene" + ".jpg";
                        ToolKits.savePicture(pBuffer, msg.stuNonMotor.stuSceneImage.nOffSet, msg.stuNonMotor.stuSceneImage.nLength , scenePicture);
                    }
                    if (pBuffer != null && dwBufSize > 0 && msg.stuNonMotor.stuFaceSceneImage.nLength > 0) {
                        System.out.println("face picture received!");
                        String facePicture = picturePath + "\\" + System.currentTimeMillis() + "related_TrafficNoMotor_face" + ".jpg";
                        ToolKits.savePicture(pBuffer, msg.stuNonMotor.stuFaceSceneImage.nOffSet, msg.stuNonMotor.stuFaceSceneImage.nLength , facePicture);
                    }
                    //图片保存
                    if (pBuffer != null && dwBufSize > 0 && msg.stuCommInfo.nPictureNum > 0) {
                        System.out.println(msg.stuCommInfo.nPictureNum + " pictures received!");
                        for(int i = 0; i < msg.stuCommInfo.nPictureNum; i ++){
                            String bigPicture = picturePath + "\\" + System.currentTimeMillis() + "related_TrafficJam_common_" + i + ".jpg";
                            ToolKits.savePicture(pBuffer, msg.stuCommInfo.stuPicInfos[i].nOffset, msg.stuCommInfo.stuPicInfos[i].nLength, bigPicture);
                        }
                    }
                    break;
                }
                case EVENT_IVS_TRAFFICJAM: /// 交通拥堵
                {
                    System.out.println("交通拥堵事件, code = " + dwAlarmType);
                    NetSDKLib.DEV_EVENT_TRAFFICJAM_INFO msg = new NetSDKLib.DEV_EVENT_TRAFFICJAM_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    // 车道号
                    int nLane = msg.nLane;
                    System.out.println("nLane:" + nLane);
                    // 报警时间间隔
                    int nAlarmIntervalTime = msg.nAlarmIntervalTime;
                    System.out.println("nAlarmIntervalTime:" + nAlarmIntervalTime);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName UTF-8:" + new String(szName,"UTF-8").trim());
                    System.out.println("szName GBK:" + new String(szName,"GBK").trim());
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //图片保存
                    if (pBuffer != null && dwBufSize > 0 && msg.stCommInfo.nPictureNum > 0) {
                        System.out.println(msg.stCommInfo.nPictureNum + " pictures received!");
                        for(int i = 0; i < msg.stCommInfo.nPictureNum; i ++){
                            String bigPicture = picturePath + "\\" + System.currentTimeMillis() + "related_TrafficJam_common_" + i + ".jpg";
                            ToolKits.savePicture(pBuffer, msg.stCommInfo.stuPicInfos[i].nOffset, msg.stCommInfo.stuPicInfos[i].nLength, bigPicture);
                        }
                    }
                    break;
                }
                case EVENT_IVS_TRAFFIC_REAREND_ACCIDENT:  // 交通事故事件
                {
                    System.out.println("交通事故事件, code = " + dwAlarmType);
                    DEV_EVENT_TRAFFIC_REAREND_ACCIDENT_INFO msg = new DEV_EVENT_TRAFFIC_REAREND_ACCIDENT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    // 动作
                    int nAction = msg.nAction;
                    System.out.println("nAction:" + nAction);
                    // 车道号
                    int nLaneID = msg.nLaneID;
                    System.out.println("nLaneID:" + nLaneID);
                    // 抓拍过程
                    int emCaptureProcess = msg.emCaptureProcess;
                    System.out.println("emCaptureProcess:" + emCaptureProcess + "," + EM_CAPTURE_PROCESS_END_TYPE.getNoteByValue(emCaptureProcess));
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName UTF-8:" + new String(szName,"UTF-8").trim());
                    System.out.println("szName GBK:" + new String(szName,"GBK").trim());
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //图片保存
                    if (pBuffer != null && dwBufSize > 0 && msg.stuCommInfo.nPictureNum > 0) {
                        System.out.println(msg.stuCommInfo.nPictureNum + " pictures received!");
                        for(int i = 0; i < msg.stuCommInfo.nPictureNum; i ++){
                            String bigPicture = picturePath + "\\" + System.currentTimeMillis() + "related_TrafficAccident_common_" + i + ".jpg";
                            ToolKits.savePicture(pBuffer, msg.stuCommInfo.stuPicInfos[i].nOffset, msg.stuCommInfo.stuPicInfos[i].nLength, bigPicture);
                        }
                    }
                    break;

                }
                case EVENT_IVS_TRAFFIC_VISIBILITY:  // 交通能见度事件检测
                {
                    System.out.println("交通能见度事件检测, code = " + dwAlarmType);
                    DEV_EVENT_TRAFFIC_VISIBILITY_INFO msg = new DEV_EVENT_TRAFFIC_VISIBILITY_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    // 动作
                    int nAction = msg.nAction;
                    System.out.println("nAction:" + nAction);
                    // 规则
                    int nRuleId = msg.nRuleId;
                    System.out.println("nRuleId:" + nRuleId);
                    // 能见程度
                    int nVisibility = msg.nVisibility;
                    System.out.println("nVisibility:" + nVisibility);
                    // 触发方式
                    int emTriggerType = msg.emTriggerType;
                    System.out.println("emTriggerType:" + emTriggerType + "," + EM_TRIGGER_TYPE.getNoteByValue(emTriggerType));
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName UTF-8:" + new String(szName,"UTF-8").trim());
                    System.out.println("szName GBK:" + new String(szName,"GBK").trim());
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //图片保存
                    if (pBuffer != null && dwBufSize > 0 && msg.stuCommInfo.nPictureNum > 0) {
                        System.out.println(msg.stuCommInfo.nPictureNum + " pictures received!");
                        for(int i = 0; i < msg.stuCommInfo.nPictureNum; i ++){
                            String bigPicture = picturePath + "\\" + System.currentTimeMillis() + "related_TrafficAccident_common_" + i + ".jpg";
                            ToolKits.savePicture(pBuffer, msg.stuCommInfo.stuPicInfos[i].nOffset, msg.stuCommInfo.stuPicInfos[i].nLength, bigPicture);
                        }
                    }
                    break;

                }
                case EVENT_IVS_TRAFFIC_PARKING:  // 停车检测
                {
                    System.out.println("停车检测, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_TRAFFIC_PEDESTRAIN:  // 行人检测
                {
                    System.out.println("行人检测, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_CROSSREGIONDETECTION:  // 区域入侵
                {
                    System.out.println("区域入侵, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_TRAFFIC_CROSSLANE:  // 违章变道
                {
                    System.out.println("违章变道, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_TRAFFIC_RETROGRADE:  // 逆行检测
                {
                    System.out.println("逆行检测, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_TRAFFIC_BACKING:  // 倒车检测
                {
                    System.out.println("倒车检测, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_TRAFFIC_ROAD_CONSTRUCTION:  // 施工检测
                {
                    System.out.println("施工检测, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_TRAFFIC_ROAD_BLOCK:  // 路障检测
                {
                    System.out.println("路障检测, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_SMOKEDETECTION:  // 烟雾报警
                {
                    System.out.println("烟雾报警, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_FIREDETECTION:  // 火焰检测
                {
                    System.out.println("火焰检测, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_TRAFFIC_OVERLINE:  // 压线检测
                {
                    System.out.println("压线检测, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_TRAFFIC_OVERSPEED:  // 超速检测
                {
                    System.out.println("超速检测, code = " + dwAlarmType);
                    break;
                }
                case EVENT_IVS_TRAFFIC_UNDERSPEED:  // 低速检测
                {
                    System.out.println("低速检测, code = " + dwAlarmType);
                    break;
                }
                default:
                    System.out.println("其他事件--------------------"+ dwAlarmType);
                    break;
            }
            return 0;
        }
    }


    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (this.attachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(this.attachHandle);
        }
    }
    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "AttachEventRealLoadPic" , "AttachEventRealLoadPic")));
        menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));

        menu.run();
    }

    public static void main(String[] args) {
        RealLoadPicExDemo RealLoadPicExDemo=new RealLoadPicExDemo();
        Scanner sc = new Scanner(System.in);
        System.out.print("ip:");
        String ip = sc.nextLine();
        System.out.print("port:");
        String tmp = sc.nextLine();
        int port = Integer.parseInt(tmp);
        System.out.print("username:");
        String username = sc.nextLine();
        System.out.print("password:");
        String pwd = sc.nextLine();
        InitTest(ip,port,username,pwd);
        RealLoadPicExDemo.RunTest();
        LoginOut();

    }
}
