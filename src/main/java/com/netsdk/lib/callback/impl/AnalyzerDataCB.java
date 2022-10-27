package com.netsdk.lib.callback.impl;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.structure.*;
import com.sun.jna.Pointer;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Objects;


/**
 * 智能报警事件回调
 */
public class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {

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

    /**
     *
     * @param lAnalyzerHandle 订阅句柄
     * @param dwAlarmType 事件类型枚举
     * @param pAlarmInfo 事件信息,具体结构体视枚举而定,每个事件类型对应不同的事件结构体
     * @param pBuffer 图片信息
     * @param dwBufSize 图片长度
     * @param dwUser 用户自定义数据
     * @param nSequence 表示上传的相同图片情况,为0时表示是第一次出现,为2表示最后一次出现或仅出现一次,为1表示此次之后还有
     * @param reserved 表示当前回调数据的状态, 为0表示当前数据为实时数据,为1表示当前回调数据是离线数据,为2时表示离线数据传送结束
     * @return
     */
    public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo,
                      Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved) {
        if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
            return -1;
        }

        NetSDKLib.NET_EVENT_FILE_INFO stuFileInfo = null;
        NetSDKLib.NET_PIC_INFO stPicInfo = null;

        switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
            case EVENT_IVS_PARKINGDETECTION: // 非法停车事件
            {
                NetSDKLib.DEV_EVENT_PARKINGDETECTION_INFO msg = new NetSDKLib.DEV_EVENT_PARKINGDETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                stPicInfo = msg.stuObject.stPicInfo;
                System.out.printf("【非法停车事件】 时间(UTC):%s 通道号:%d 开始时间:%s 结束时间:%s 事件触发累计次数:%d 事件源设备唯一标识:%s \n",
                        msg.UTC, msg.nChannelID, msg.stuObject.stuStartTime, msg.stuObject.stuEndTime,
                        msg.nOccurrenceCount, new String(msg.szSourceDevice));
                break;
            }
            case EVENT_IVS_FIGHTDETECTION: // 斗殴事件
            {
                NetSDKLib.DEV_EVENT_FIGHT_INFO msg = new NetSDKLib.DEV_EVENT_FIGHT_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 斗殴事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                break;
            }
            case EVENT_IVS_LEAVEDETECTION: // 离岗检测事件
            {
                NetSDKLib.DEV_EVENT_IVS_LEAVE_INFO msg = new NetSDKLib.DEV_EVENT_IVS_LEAVE_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                stPicInfo = msg.stuObject.stPicInfo;
                System.out.printf("【离岗检测事件】 时间(UTC):%s 通道号:%d 开始时间:%s 结束时间:%s \n",
                        msg.UTC, msg.nChannelID, msg.stuObject.stuStartTime, msg.stuObject.stuEndTime);
                break;
            }
            case EVENT_IVS_CROSSLINEDETECTION: // 警戒线事件
            {
                NetSDKLib.DEV_EVENT_CROSSLINE_INFO msg = new NetSDKLib.DEV_EVENT_CROSSLINE_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                stPicInfo = msg.stuObject.stPicInfo;
                System.out.printf("【警戒线事件】 时间(UTC):%s 通道号:%d 开始时间:%s 结束时间:%s 事件触发累计次数:%d 事件源设备唯一标识:%s \n",
                        msg.UTC, msg.nChannelID, msg.stuObject.stuStartTime, msg.stuObject.stuEndTime,
                        msg.nOccurrenceCount, new String(msg.szSourceDevice));
                break;
            }
            case EVENT_IVS_RIOTERDETECTION: // 聚众事件
            {
                NetSDKLib.DEV_EVENT_RIOTERL_INFO msg = new NetSDKLib.DEV_EVENT_RIOTERL_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                stPicInfo = msg.stuObjectIDs[0].stPicInfo; // 取第一个
                System.out.printf("【聚众事件】 时间(UTC):%s 通道号:%d 检测到的物体个数:%d 事件触发累计次数:%d 事件源设备唯一标识:%s \n",
                        msg.UTC, msg.nChannelID, msg.nObjectNum,
                        msg.nOccurrenceCount, new String(msg.szSourceDevice));
                break;
            }
            case EVENT_IVS_LEFTDETECTION: // 物品遗留事件
            {
                NetSDKLib.DEV_EVENT_LEFT_INFO msg = new NetSDKLib.DEV_EVENT_LEFT_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                stPicInfo = msg.stuObject.stPicInfo;
                System.out.printf("【物品遗留事件】 时间(UTC):%s 通道号:%d 开始时间:%s 结束时间:%s 事件触发累计次数:%d 事件源设备唯一标识:%s \n",
                        msg.UTC, msg.nChannelID, msg.stuObject.stuStartTime, msg.stuObject.stuEndTime,
                        msg.nOccurrenceCount, new String(msg.szSourceDevice));
                break;
            }
            case EVENT_IVS_STAYDETECTION: // 停留事件
            {
                //System.out.printf("【停留事件】\n");
                NetSDKLib.DEV_EVENT_STAY_INFO msg = new NetSDKLib.DEV_EVENT_STAY_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                stPicInfo = msg.stuObject.stPicInfo;
                System.out.printf("【停留事件】 时间(UTC):%s 通道号:%d 开始时间:%s 结束时间:%s \n",
                        msg.UTC, msg.nChannelID, msg.stuObject.stuStartTime, msg.stuObject.stuEndTime);
                break;
            }
            case EVENT_IVS_WANDERDETECTION: // 徘徊事件
            {
                NetSDKLib.DEV_EVENT_WANDER_INFO msg = new NetSDKLib.DEV_EVENT_WANDER_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                stPicInfo = msg.stuObjectIDs[0].stPicInfo; // 取第一个
                System.out.printf("【徘徊事件】 时间(UTC):%s 通道号:%d 检测到的物体个数:%d 事件触发累计次数:%d 事件源设备唯一标识:%s \n",
                        msg.UTC, msg.nChannelID, msg.nObjectNum,
                        msg.nOccurrenceCount, new String(msg.szSourceDevice));
                break;
            }
            case EVENT_IVS_MAN_NUM_DETECTION: ///<  立体视觉区域内人数统计事件
            {
                NetSDKLib.DEV_EVENT_MANNUM_DETECTION_INFO msg = new NetSDKLib.DEV_EVENT_MANNUM_DETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                System.out.println(" 立体视觉区域内人数统计事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID +
                        " 区域人员列表数量:" + msg.nManListCount + " 人员身高:" + msg.stuManList[0].nStature);
                break;
            }
            case EVENT_IVS_CROWDDETECTION: ///<  人群密度检测事件
            {
                NetSDKLib.DEV_EVENT_CROWD_DETECTION_INFO msg = new NetSDKLib.DEV_EVENT_CROWD_DETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                System.out.println(" 人群密度检测事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                break;
            }
            case EVENT_IVS_WORKCLOTHES_DETECT: ///<  安全帽检测事件
            {
                NetSDKLib.DEV_EVENT_WORKCLOTHES_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_WORKCLOTHES_DETECT_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
                    String bigPicture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                    ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
                    if (msg.stuHumanImage != null && msg.stuHumanImage.nLength > 0) {
                        String smallPicture = picturePath + "\\" + System.currentTimeMillis() + "small.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuHumanImage.nOffSet, msg.stuHumanImage.nLength, smallPicture);
                    }
                }
                System.out.println(" 安全帽检测事件(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                break;
            }
            case EVENT_IVS_CITY_MOTORPARKING: ///<   城市机动车违停事件
            {
                NetSDKLib.DEV_EVENT_CITY_MOTORPARKING_INFO msg = new NetSDKLib.DEV_EVENT_CITY_MOTORPARKING_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                System.out.println(" 城市机动车违停事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + "违停持续时长(单位秒)：" + msg.nParkingDuration
                        + "检测到的物体个数:" + msg.nObjectNum);
                break;
            }
            case EVENT_IVS_CITY_NONMOTORPARKING: ///<   城市非机动车违停事件
            {
                NetSDKLib.DEV_EVENT_CITY_NONMOTORPARKING_INFO msg = new NetSDKLib.DEV_EVENT_CITY_NONMOTORPARKING_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                System.out.println(" 城市非机动车违停事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + "检测到的物体个数:" + msg.nObjectNum);
                break;
            }
            case EVENT_IVS_HOLD_UMBRELLA: ///<   违规撑伞检测事件
            {
                NetSDKLib.DEV_EVENT_HOLD_UMBRELLA_INFO msg = new NetSDKLib.DEV_EVENT_HOLD_UMBRELLA_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                System.out.println(" 违规撑伞检测事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + "违法持续时长(单位秒)" + msg.nViolationDuration);
                break;
            }
            case EVENT_IVS_PEDESTRIAN_JUNCTION: ///<  行人卡口事件
            {
                NetSDKLib.DEV_EVENT_PEDESTRIAN_JUNCTION_INFO msg = new NetSDKLib.DEV_EVENT_PEDESTRIAN_JUNCTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                System.out.println(" 行人卡口事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + "人行道号" + msg.nLane);
                break;
            }
            case EVENT_IVS_DUSTBIN_OVER_FLOW: ///<  垃圾桶满溢检测事件
            {
                NetSDKLib.DEV_EVENT_DUSTBIN_OVER_FLOW_INFO msg = new NetSDKLib.DEV_EVENT_DUSTBIN_OVER_FLOW_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                System.out.println(" 垃圾桶满溢检测事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID
                        + "违法持续时长(单位秒)" + msg.nViolationDuration + "检测到的物体个数" + msg.nObjectNum);
                break;
            }
            case EVENT_IVS_DOOR_FRONT_DIRTY: ///<   门前脏乱检测事件
            {
                NetSDKLib.DEV_EVENT_DOOR_FRONT_DIRTY_INFO msg = new NetSDKLib.DEV_EVENT_DOOR_FRONT_DIRTY_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                System.out.println(" 门前脏乱检测事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID
                        + "违法持续时长(单位秒)" + msg.nViolationDuration + "检测到的物体个数" + msg.nObjectNum);
                break;
            }
            case EVENT_IVS_GARBAGE_EXPOSURE: ///<   垃圾暴露检测事件
            {
                NetSDKLib.DEV_EVENT_GARBAGE_EXPOSURE_INFO msg = new NetSDKLib.DEV_EVENT_GARBAGE_EXPOSURE_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                System.out.println(" 垃圾暴露检测事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID
                        + "违法持续时长(单位秒)" + msg.nViolationDuration + "检测到的物体个数" + msg.nObjectNum);
                break;
            }
            case EVENT_IVS_VIOLENT_THROW_DETECTION: ///<   暴力抛物事件事件
            {
                NetSDKLib.DEV_EVENT_VIOLENT_THROW_DETECTION_INFO msg = new NetSDKLib.DEV_EVENT_VIOLENT_THROW_DETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
                    String bigPicture = picturePath + "\\" + System.currentTimeMillis() + "big.jpg";
                    ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
                }
                stuFileInfo = msg.stuFileInfo;
                System.out.println(" 暴力抛物事件事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID
                        + "暴力抛物检测区域名称" + new String(msg.szRegionName, Charset.forName(Utils.getPlatformEncode())));
                break;
            }
            case EVENT_IVS_FOG_DETECTION: ///<   起雾检测事件
            {
                NetSDKLib.DEV_EVENT_FOG_DETECTION msg = new NetSDKLib.DEV_EVENT_FOG_DETECTION();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 起雾检测事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID
                        + "事件组ID" + msg.nGroupID + "事件数据类型" + msg.emEventType + "雾等级" + msg.stuFogInfo.emFogLevel);
                break;
            }
            case EVENT_IVS_ACCESS_CTL: ///<   门禁事件
            {
                NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO msg = new NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 门禁事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID
                        + "事件组ID" + msg.nGroupID + "事件数据类型" + msg.emEventType + "人员温度信息是否有效" + msg.bManTemperature
                        + "人员温度" + msg.stuManTemperatureInfo.fCurrentTemperature + "温度单位" + msg.stuManTemperatureInfo.emTemperatureUnit
                        + "是否超温" + msg.stuManTemperatureInfo.bIsOverTemperature);
                break;
            }
            case EVENT_IVS_AUDIO_ABNORMALDETECTION: //  声音异常检测
            {
                NetSDKLib.DEV_EVENT_IVS_AUDIO_ABNORMALDETECTION_INFO msg = new NetSDKLib.DEV_EVENT_IVS_AUDIO_ABNORMALDETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 声音异常检测 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + "声音强度" + msg.nDecibel + "声音频率" + msg.nFrequency);
                break;
            }
            case EVENT_IVS_CLIMBDETECTION: // 攀高检测事件
            {
                NetSDKLib.DEV_EVENT_IVS_CLIMB_INFO msg = new NetSDKLib.DEV_EVENT_IVS_CLIMB_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                stuFileInfo = msg.stuFileInfo;
                stPicInfo = msg.stuObject.stPicInfo;
                System.out.println(" 攀高检测事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime);
                break;
            }
            case EVENT_IVS_CROSSREGIONDETECTION: ///<  警戒区事件
            {
                NetSDKLib.DEV_EVENT_CROSSREGION_INFO msg = new NetSDKLib.DEV_EVENT_CROSSREGION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 警戒区事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime);
//             	PrintStruct.print(msg);
                break;
            }
            case EVENT_IVS_VIDEOABNORMALDETECTION: ///<   视频异常事件
            {
                NetSDKLib.DEV_EVENT_VIDEOABNORMALDETECTION_INFO msg = new NetSDKLib.DEV_EVENT_VIDEOABNORMALDETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 视频异常事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + "异常类型" + msg.bType);
                break;
            }
            case EVENT_IVS_STAY_ALONE_DETECTION: ///<   单人独处事件
            {
                NetSDKLib.DEV_EVENT_STAY_ALONE_DETECTION_INFO msg = new NetSDKLib.DEV_EVENT_STAY_ALONE_DETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 单人独处事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + "物体ID" + msg.stuHuman.nObjectID);
                break;
            }
            case EVENT_IVS_PRISONERRISEDETECTION: ///<   看守所囚犯起身事件
            {
                NetSDKLib.DEV_EVENT_PRISONERRISEDETECTION_INFO msg = new NetSDKLib.DEV_EVENT_PRISONERRISEDETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 看守所囚犯起身事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                break;
            }
            case EVENT_IVS_HIGH_TOSS_DETECT: ///<   高空抛物检测事件
            {
                NetSDKLib.DEV_EVENT_HIGH_TOSS_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_HIGH_TOSS_DETECT_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 高空抛物检测事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                break;
            }
            case EVENT_IVS_TRAFFIC_PEDESTRAINRUNREDLIGHT://行人闯红灯事件
            {
                DEV_EVENT_TRAFFIC_PEDESTRAINRUNREDLIGHT_INFO info = new DEV_EVENT_TRAFFIC_PEDESTRAINRUNREDLIGHT_INFO();
                ToolKits.GetPointerData(pAlarmInfo, info);
                stuFileInfo = info.stuFileInfo;
                stPicInfo = info.stuObject.stPicInfo;
                System.out.println(info.toString());
                break;
            }
            case EVENT_IVS_FACEDETECT://人脸检测事件
            {
                NetSDKLib.DEV_EVENT_FACEDETECT_INFO info = new NetSDKLib.DEV_EVENT_FACEDETECT_INFO();
                ToolKits.GetPointerData(pAlarmInfo, info);
                stuFileInfo = info.stuFileInfo;
                stPicInfo = info.stuObject.stPicInfo;
                System.out.println("人脸检测: dwOffSet:" + stPicInfo.dwOffSet + ",dwFileLength:" + stPicInfo.dwFileLenth);
                break;
            }
            case EVENT_IVS_SHOP_WINDOW_POST://橱窗张贴事件
            {
                DEV_EVENT_SHOP_WINDOW_POST_INFO info = new DEV_EVENT_SHOP_WINDOW_POST_INFO();
                ToolKits.GetPointerData(pAlarmInfo, info);
                stuFileInfo = info.stuFileInfo;
                System.out.println(" 橱窗张贴事件  时间(UTC)：" + info.UTC + " 通道号:" + info.nChannelID + " 商铺地址: " + new String(info.szShopAddress, Charset.forName(Utils.getPlatformEncode())).trim()
                        + " 违法持续时间: " + info.nViolationDuration + " 检测到的物体数量: " + info.nObjectNum);
                for (int i = 0; i < info.nObjectNum; i++) {
                    stPicInfo = info.stuObjects[i].stPicInfo;
                    if (stPicInfo != null && stPicInfo.dwFileLenth > 0) {
                        String smallPicture = picturePath + File.separator + String.format("Small_%d_%s_%d_%d_%d_%d.jpg",
                                dwAlarmType, stuFileInfo.stuFileTime.toStringTitle(), stuFileInfo.bCount,
                                stuFileInfo.bIndex, stuFileInfo.bFileType, stuFileInfo.nGroupId);
                        ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, smallPicture);
                    }
                }

                break;
            }
            case EVENT_IVS_SHOP_SIGN_ABNORMAL: {//店招异常事件
                DEV_EVENT_SHOP_SIGN_ABNORMAL_INFO info = new DEV_EVENT_SHOP_SIGN_ABNORMAL_INFO();
                ToolKits.GetPointerData(pAlarmInfo, info);
                stuFileInfo = info.stuFileInfo;
                System.out.println(" 店招异常事件  时间(UTC)：" + info.UTC + " 通道号:" + info.nChannelID + " 商铺地址: " + new String(info.szShopAddress, Charset.forName(Utils.getPlatformEncode())).trim()
                        + " 违法持续时间: " + info.nViolationDuration + " 检测到的物体数量: " + info.nObjectNum);

                String Picture = picturePath + "\\" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);

                for (int i = 0; i < info.nObjectNum; i++) {
                    stPicInfo = info.stuObjects[i].stPicInfo;
                    if (stPicInfo != null && stPicInfo.dwFileLenth > 0) {
                        String smallPicture = picturePath + File.separator + String.format("Small_%d_%s_%d_%d_%d_%d.jpg",
                                dwAlarmType, stuFileInfo.stuFileTime.toStringTitle(), stuFileInfo.bCount,
                                stuFileInfo.bIndex, stuFileInfo.bFileType, stuFileInfo.nGroupId);
                        if (stPicInfo.dwOffSet != 0 && stPicInfo.dwFileLenth != 0) {
                            ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, smallPicture);
                        }
                    }
                }
                break;
            }
            case EVENT_IVS_STREET_SUNCURE: {//沿街晾晒事件
                DEV_EVENT_STREET_SUNCURE_INFO info = new DEV_EVENT_STREET_SUNCURE_INFO();
                ToolKits.GetPointerData(pAlarmInfo, info);
                stuFileInfo = info.stuFileInfo;
                System.out.println(" 沿街晾晒事件  时间(UTC)：" + info.UTC + " 通道号:" + info.nChannelID + " 违法持续时间: " + info.nViolationDuration + " 检测到的物体数量: " + info.nObjectNum);
                for (int i = 0; i < info.nObjectNum; i++) {
                    stPicInfo = info.stuObjects[i].stPicInfo;
                    if (stPicInfo != null && stPicInfo.dwFileLenth > 0) {
                        String smallPicture = picturePath + File.separator + String.format("Small_%d_%s_%d_%d_%d_%d.jpg",
                                dwAlarmType, stuFileInfo.stuFileTime.toStringTitle(), stuFileInfo.bCount,
                                stuFileInfo.bIndex, stuFileInfo.bFileType, stuFileInfo.nGroupId);
                        ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, smallPicture);
                    }
                }
                break;
            }
            case EVENT_IVS_OUTDOOR_ADVERTISEMENT: {//户外广告事件
                DEV_EVENT_OUTDOOR_ADVERTISEMENT_INFO info = new DEV_EVENT_OUTDOOR_ADVERTISEMENT_INFO();
                ToolKits.GetPointerData(pAlarmInfo, info);
                stuFileInfo = info.stuFileInfo;
                System.out.println(" 户外广告事件  时间(UTC)：" + info.UTC + " 通道号:" + info.nChannelID
                        + " 违法持续时间: " + info.nViolationDuration + " 检测到的物体数量: " + info.nObjectNum);
                for (int i = 0; i < info.nObjectNum; i++) {
                    stPicInfo = info.stuObjects[i].stPicInfo;
                    if (stPicInfo != null && stPicInfo.dwFileLenth > 0) {
                        String smallPicture = picturePath + File.separator + String.format("Small_%d_%s_%d_%d_%d_%d.jpg",
                                dwAlarmType, stuFileInfo.stuFileTime.toStringTitle(), stuFileInfo.bCount,
                                stuFileInfo.bIndex, stuFileInfo.bFileType, stuFileInfo.nGroupId);
                        ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, smallPicture);
                    }
                }
                break;
            }
            case EVENT_IVS_HUDDLE_MATERIAL: {//乱堆物料检测事件
                DEV_EVENT_HUDDLE_MATERIAL_INFO info = new DEV_EVENT_HUDDLE_MATERIAL_INFO();
                ToolKits.GetPointerData(pAlarmInfo, info);
                stuFileInfo = info.stuFileInfo;
                System.out.println(" 乱堆物料检测事件  时间(UTC)：" + info.UTC + " 通道号:" + info.nChannelID + " 违法持续时间: " + info.nViolationDuration + " 检测到的物体数量: " + info.nObjectNum);
                for (int i = 0; i < info.nObjectNum; i++) {
                    stPicInfo = info.stuObjects[i].stPicInfo;
                    if (stPicInfo != null && stPicInfo.dwFileLenth > 0) {
                        String smallPicture = picturePath + File.separator + String.format("Small_%d_%s_%d_%d_%d_%d.jpg",
                                dwAlarmType, stuFileInfo.stuFileTime.toStringTitle(), stuFileInfo.bCount,
                                stuFileInfo.bIndex, stuFileInfo.bFileType, stuFileInfo.nGroupId);
                        ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, smallPicture);
                    }
                }
                break;
            }
            default:{
                System.out.println("Other Event received: " + dwAlarmType);
                break;
            }
        }

        if (stuFileInfo != null) { // 保存图片
            String bigPicture = picturePath + File.separator + String.format("Big_%d_%s_%d_%d_%d_%d.jpg",
                    dwAlarmType, stuFileInfo.stuFileTime.toStringTitle(), stuFileInfo.bCount,
                    stuFileInfo.bIndex, stuFileInfo.bFileType, stuFileInfo.nGroupId);
            ToolKits.savePicture(pBuffer, 0, dwBufSize, bigPicture);
            if (stPicInfo != null && stPicInfo.dwFileLenth > 0) {
                String smallPicture = picturePath + File.separator + String.format("Small_%d_%s_%d_%d_%d_%d.jpg",
                        dwAlarmType, stuFileInfo.stuFileTime.toStringTitle(), stuFileInfo.bCount,
                        stuFileInfo.bIndex, stuFileInfo.bFileType, stuFileInfo.nGroupId);
                ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, smallPicture);
            }
        }
        return 0;
    }
}