package com.netsdk.demo.intelligentTraffic.parkingDemo;

import com.netsdk.demo.util.EventTaskHandler;
import com.netsdk.demo.util.QueueGeneration;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_TRIGGER_TYPE;
import com.netsdk.lib.structure.DEV_EVENT_CAR_DRIVING_IN_INFO;
import com.netsdk.lib.structure.DEV_EVENT_CAR_DRIVING_OUT_INFO;
import com.netsdk.lib.structure.DEV_EVENT_PARKINGSPACE_MANUALSNAP_INFO;
import com.netsdk.lib.structure.DEV_EVENT_PARKING_LOT_STATUS_DETECTION_INFO;
import com.sun.jna.Pointer;

import java.io.*;
import java.util.UUID;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 47040
 * @update 2020/11/19 舟曲项目
 * 1) EVENT_IVS_TRAFFIC_PARKINGSPACE_MANUALSNAP 路侧停车位手动抓图
 * 2) EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING 新增字段 emTriggerType 触发类型
 * 3) EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING 新增字段 emTriggerType 触发类型
 * @since Created in 2020/7/20 20:10
 */
public class ParkingAnalyzerDataCallBack implements NetSDKLib.fAnalyzerDataCallBack {

    private static ParkingAnalyzerDataCallBack singleInstance;

    public static ParkingAnalyzerDataCallBack getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new ParkingAnalyzerDataCallBack();
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

    public ParkingAnalyzerDataCallBack() {
        eventCBQueueService.init();
    }

    private final String imageSaveFolder = "TrafficParkingPic/";      // 图片保存路径

    @Override
    public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved) {
        switch (dwAlarmType) {
            case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING: /// 车位有车事件 (新增舟曲项目字段)
            {
                ParsingParkingSpaceParkingEvent(pAlarmInfo, pBuffer, dwBufSize);
                break;
            }
            case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING: /// 车位无车事件 (新增舟曲项目字段)
            {
                ParsingParkingSpaceNoParkingEvent(pAlarmInfo, pBuffer, dwBufSize);
                break;
            }
            case NetSDKLib.EVENT_IVS_PARKINGDETECTION:   /// 车位检测事件
            {
                ParsingParkingDetectionEvent(pAlarmInfo, pBuffer, dwBufSize);
                break;
            }
            case NetSDKLib.EVENT_IVS_TRAFFIC_MANUALSNAP: /// 手动抓图事件
            {
                ParsingManualSnapEvent(pAlarmInfo, pBuffer, dwBufSize);
                break;
            }
            case NetSDKLib.EVENT_IVS_PARKING_LOT_STATUS_DETECTION: /// 停车位状态检测事件
            {
                ParsingParkingLotStatusDetectionEvent(pAlarmInfo, pBuffer);
                break;
            }
            case NetSDKLib.EVENT_IVS_CAR_DRIVING_IN: /// 车辆驶入事件
            {
                ParsingCarDrivingInEvent(pAlarmInfo, pBuffer, dwBufSize);
                break;
            }
            case NetSDKLib.EVENT_IVS_CAR_DRIVING_OUT: /// 车辆驶入事件
            {
                ParsingCarDrivingOutEvent(pAlarmInfo, pBuffer, dwBufSize);
                break;
            }
            case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACE_MANUALSNAP: /// 路侧停车位手动抓图 (舟曲项目新增)
            {
                ParsingTrafficParkingSpaceManualSnap(pAlarmInfo, pBuffer, dwBufSize);
                break;
            }
            default:
                System.out.printf("Get Other Event 0x%x\n", dwAlarmType);
                break;
        }
        return 0;
    }

    // 车位有车事件 (新增舟曲项目字段)
    private void ParsingParkingSpaceParkingEvent(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) {
        System.out.println("\n\n<Event> TRAFFIC [ PARKING SPACE PARKING]");

        // 展示：车位，车牌号，抓拍时间，事件名称，一位多车信息（车位号，车牌）

        ///////////////// <<------车位有车主要信息------>> /////////////////
        NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO stuParkingInfo = new NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO();
        ToolKits.GetPointerData(pAlarmInfo, stuParkingInfo);

        // 同一组图片会以多个事件返回，但 nGroupId 会保持一致
        System.out.println("EventID:" + stuParkingInfo.nEventID + " GroupId:" + stuParkingInfo.stuFileInfo.nGroupId);

        String parkingSzName = new String(stuParkingInfo.szName).trim();                              // 事件名称
        int parkingEventId = stuParkingInfo.nEventID;                                                 // 事件ID
        String parkingEventTime = stuParkingInfo.UTC.toString();                                      // 事件抓拍时间
        String parkingSzParkingNum = new String(stuParkingInfo.szParkingNum).trim();                  // 车位号
        int parkingEmAcrossParking = stuParkingInfo.emAcrossParking;                                  // 是否跨位 (0:未知, 1:未跨位, 2:跨位)
        int parkingEmParkingDirection = stuParkingInfo.emParkingDirection;                            // 停车方向 (0:未知, 1:逆向, 2:正向)
        int forbidParkingStatus = stuParkingInfo.emForbidParkingStatus;                               // 禁停状态 (0:未知, 1:未禁止, 2:禁止)
        int triggerType = stuParkingInfo.emTriggerType;                                               // 触发类型 (-1:未知, 0:非手动, 1:手动) 舟曲项目新增
        String parkingSzPlateNumber = null;   // 车牌号
        try {
            parkingSzPlateNumber = new String(stuParkingInfo.stTrafficCar.szPlateNumber, encode).trim();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        StringBuilder parkingEventMainInfo = new StringBuilder()
                .append("<<------车位有车事件主要信息------>>").append("\n")
                .append("事件名称: ").append(parkingSzName).append("\n")
                .append("事件ID: ").append(parkingEventId).append("\n")
                .append("抓拍时间: ").append(parkingEventTime).append("\n")
                .append("车位号").append(parkingSzParkingNum).append("\n")
                .append("是否跨位 (0:未知, 1:未跨位, 2:跨位): ").append(parkingEmAcrossParking).append("\n")
                .append("停车方向 (0:未知, 1:逆向, 2:正向): ").append(parkingEmParkingDirection).append("\n")
                .append("禁停状态 (0:未知, 1:未禁止, 2:禁止): ").append(forbidParkingStatus).append("\n")
                .append("触发类型 (-1:未知, 0:非手动, 1:手动): ").append(triggerType).append("\n")
                .append("车牌号: ").append(parkingSzPlateNumber);

        System.out.println(parkingEventMainInfo.toString());

        ///////////////// <<------一位多车信息------>> /////////////////
        NetSDKLib.DEV_OCCUPIED_WARNING_INFO stuOccupiedWarningInfo = stuParkingInfo.stuOccupiedWarningInfo;
        String szParkingNo = new String(stuOccupiedWarningInfo.szParkingNo).trim();    // 车位号

        StringBuilder parkingEventOccupiedInfo = new StringBuilder()
                .append("<<------一位多车信息------>>").append("\n")
                .append("车位号: ").append(szParkingNo);

        for (int i = 0; i < stuOccupiedWarningInfo.nPlateNumber; ++i) {
            String occupiedPlate = null;
            try {
                occupiedPlate = new String(stuOccupiedWarningInfo.szPlateNumber[i].plateNumber, encode).trim();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            parkingEventOccupiedInfo.append("\n").append("第 ").append(i + 1).append(" 辆车: ").append(occupiedPlate);
        }

        System.out.println(parkingEventOccupiedInfo.toString());

        //////////////// <<-----保存图片----->> ////////////////
        ////// 抓拍到物体的信息
        NetSDKLib.NET_PIC_INFO parkingObjPicInfo = stuParkingInfo.stuObject.stPicInfo;
        String uuid = UUID.randomUUID().toString();
        if ((stuParkingInfo.stuObject.bPicEnble == 1) && (parkingObjPicInfo != null)) {
            int parkingPicOffset = parkingObjPicInfo.dwOffSet;
            int parkingPicLength = parkingObjPicInfo.dwFileLenth;
            String parkingObjPicName = String.format("%s_%s_%s_%s.jpg", "Parking-Obj", stuParkingInfo.stuFileInfo.nGroupId, parkingEventTime.replaceAll("[^0-9]", "-"), uuid);
            String parkingObjPath = imageSaveFolder + parkingObjPicName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, parkingPicOffset, parkingPicLength, parkingObjPath));
        }

        /////// 公共信息图片
        NetSDKLib.EVENT_COMM_INFO parkingCommInfo = stuParkingInfo.stCommInfo;
        int nPictureNum = parkingCommInfo.nPictureNum;    // 原始图片张数

        for (int i = 0; i < nPictureNum; i++) {
            int length = parkingCommInfo.stuPicInfos[i].nLength;
            int offSet = parkingCommInfo.stuPicInfos[i].nOffset;
            String parkingCommPicName = String.format("%s_%02d_%s_%s_%s.jpg", "Parking-comm", i, stuParkingInfo.stuFileInfo.nGroupId, parkingEventTime.replaceAll("[^0-9]", "-"), uuid);
            String savePath = imageSaveFolder + parkingCommPicName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, offSet, length, savePath));
        }

        ////// 车位、车身抓拍图片 这个数据前端相机不一定支持
        if (stuParkingInfo.stuParkingImage.nLength > 0) {
            String savePath = imageSaveFolder + String.format("%s_%s_%s_%s.jpg", "Parking-ParkingImage", stuParkingInfo.stuFileInfo.nGroupId, parkingEventTime.replaceAll("[^0-9]", "-"), uuid);
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, stuParkingInfo.stuParkingImage.nOffSet, stuParkingInfo.stuParkingImage.nLength, savePath));
        }

        /////// 事件原图
        String picPath = imageSaveFolder + String.format("%s_%s_%s_%s.jpg", "Parking", stuParkingInfo.stuFileInfo.nGroupId, parkingEventTime.replaceAll("[^0-9]", "-"), uuid);
        eventCBQueueService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));
    }

    // 车位无车事件 (新增舟曲项目字段)
    private void ParsingParkingSpaceNoParkingEvent(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) {
        System.out.println("\n\n<Event> TRAFFIC [ PARKING SPACE NO PARKING]");

        // 展示：车位，车牌号，抓拍时间，事件名称，匹配的入场车辆信息（车位号，车牌，相似度）

        ///////////////// <<------车位无车主要信息------>> /////////////////
        NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO stuNoParkingInfo = new NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO();
        ToolKits.GetPointerData(pAlarmInfo, stuNoParkingInfo);

        // 同一组图片会以多个事件返回，但 nGroupId 会保持一致
        System.out.println("EventID:" + stuNoParkingInfo.nEventID + " GroupId:" + stuNoParkingInfo.stuFileInfo.nGroupId);

        String noParkingSzName = new String(stuNoParkingInfo.szName).trim();                              // 事件名称
        int noParkingEventID = stuNoParkingInfo.nEventID;                                                 // 事件ID
        String noParkingEventTime = stuNoParkingInfo.UTC.toString();                                      // 事件抓拍时间
        String noParkingSzParkingNum = new String(stuNoParkingInfo.szParkingNum).trim();                  // 车位号
        String noParkingSzPlateNumber = null;   // 车牌号
        try {
            noParkingSzPlateNumber = new String(stuNoParkingInfo.stTrafficCar.szPlateNumber, encode).trim();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int triggerType = stuNoParkingInfo.emTriggerType;                      // 触发类型 (-1:未知, 0:非手动, 1:手动) 舟曲项目新增

        StringBuilder noParkingEventMainInfo = new StringBuilder()
                .append("<<------车位无车事件主要信息------>>").append("\n")
                .append("事件名称: ").append(noParkingSzName).append("\n")
                .append("事件ID: ").append(noParkingEventID).append("\n")
                .append("抓拍时间: ").append(noParkingEventTime).append("\n")
                .append("车位号").append(noParkingSzParkingNum).append("\n")
                .append("车牌号: ").append(noParkingSzPlateNumber).append("\n")
                .append("触发类型(-1:未知, 0:非手动, 1:手动): ").append(triggerType);

        System.out.println(noParkingEventMainInfo.toString());

        ///////////////// <<------匹配入场车辆信息------>> /////////////////
        StringBuilder noParkingEventMatchInfo = new StringBuilder()
                .append("<<------匹配入场车辆------>>");

        NetSDKLib.DEV_MATCH_PARKING_INFO matchParkingInfo;
        for (int i = 0; i < stuNoParkingInfo.nMatchParkingNum; i++) {
            matchParkingInfo = stuNoParkingInfo.stuMatchParkingInfo[i];
            try {
                noParkingEventMatchInfo
                        .append("\n").append("第 ").append(i + 1).append(" 个车辆驶入信息:")
                        .append("\n").append("车位号: ").append(new String(matchParkingInfo.szParkingNo).trim())
                        .append("\n").append("车牌号: ").append(new String(matchParkingInfo.szPlateNum, encode).trim())
                        .append("\n").append("相似度: ").append(matchParkingInfo.nSimilarity);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println(noParkingEventMatchInfo.toString());

        //////////////// <<-----保存图片----->> ////////////////
        ////// 抓拍到物体的信息
        NetSDKLib.NET_PIC_INFO noParkingObjPicInfo = stuNoParkingInfo.stuObject.stPicInfo;
        String uuid = UUID.randomUUID().toString();
        if ((stuNoParkingInfo.stuObject.bPicEnble == 1) && (noParkingObjPicInfo != null)) {
            int noParkingPicOffset = noParkingObjPicInfo.dwOffSet;
            int noParkingPicLength = noParkingObjPicInfo.dwFileLenth;
            String noParkingObjPicName = String.format("%s_%s_%s_%s.jpg", "NoParking-Obj", stuNoParkingInfo.stuFileInfo.nGroupId, noParkingEventTime.replaceAll("[^0-9]", "-"), uuid);
            String noParkingObjPath = imageSaveFolder + noParkingObjPicName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, noParkingPicOffset, noParkingPicLength, noParkingObjPath));
        }

        /////// 公共信息图片
        NetSDKLib.EVENT_COMM_INFO noParkingCommInfo = stuNoParkingInfo.stCommInfo;
        int nPictureNum = noParkingCommInfo.nPictureNum;    // 原始图片张数

        for (int i = 0; i < nPictureNum; i++) {
            int length = noParkingCommInfo.stuPicInfos[i].nLength;
            int offSet = noParkingCommInfo.stuPicInfos[i].nOffset;
            String noParkingCommPicName = String.format("%s_%02d_%s_%s_%s.jpg", "NoParking-comm", i, stuNoParkingInfo.stuFileInfo.nGroupId, noParkingEventTime.replaceAll("[^0-9]", "-"), uuid);
            String savePath = imageSaveFolder + noParkingCommPicName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, offSet, length, savePath));
        }

        //// 车位、车身抓拍图片 这个数据前端相机不一定支持 这里就先注释了
        if (stuNoParkingInfo.stuParkingImage.nLength > 0) {
            String savePath = imageSaveFolder + String.format("%s_%s_%s_%s.jpg", "NoParking-ParkingImage", stuNoParkingInfo.stuFileInfo.nGroupId, noParkingEventTime.replaceAll("[^0-9]", "-"), uuid);
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, stuNoParkingInfo.stuParkingImage.nOffSet, stuNoParkingInfo.stuParkingImage.nLength, savePath));
        }

        /////// 事件原图
        String picPath = imageSaveFolder + String.format("%s_%s_%s_%s.jpg", "NoParking", stuNoParkingInfo.stuFileInfo.nGroupId, noParkingEventTime.replaceAll("[^0-9]", "-"), uuid);
        eventCBQueueService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));
    }

    // 车位检测事件
    private void ParsingParkingDetectionEvent(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) {
        System.out.println("\n\n<Event> TRAFFIC [ PARKING DETECTION]");

        // 能展示ParkingDetection事件中的内容，车位号、时间

        ///////////////// <<------车位检测主要信息------>> /////////////////
        NetSDKLib.DEV_EVENT_PARKINGDETECTION_INFO stuDetectionInfo = new NetSDKLib.DEV_EVENT_PARKINGDETECTION_INFO();
        ToolKits.GetPointerData(pAlarmInfo, stuDetectionInfo);

        String detectSzName = new String(stuDetectionInfo.szName).trim();    // 事件名
        int detectEvent = stuDetectionInfo.nEventID;                         // 事件ID
        String detectTime = stuDetectionInfo.UTC.toString();                 // 发生时间
        String detectCustomParkNo = new String(stuDetectionInfo.szCustomParkNo).trim();  // 车位号

        StringBuilder noDetectEventMainInfo = new StringBuilder()
                .append("<<------车位检测主要信息------>>").append("\n")
                .append("事件名: ").append(detectSzName).append("\n")
                .append("事件ID: ").append(detectEvent).append("\n")
                .append("发生时间: ").append(detectTime).append("\n")
                .append("车位号").append(detectCustomParkNo);
        System.out.println(noDetectEventMainInfo.toString());

        ///////////////// <<------检测到的物体信息------>> /////////////////
        NetSDKLib.NET_MSG_OBJECT detectObjInfo = stuDetectionInfo.stuObject;
        String szObjectType = new String(detectObjInfo.szObjectType).trim();       // 物体类型
        String szObjectSubType = new String(detectObjInfo.szObjectSubType).trim(); // 物体子类型

        StringBuilder noDetectEventObjInfo = new StringBuilder()
                .append("<<------检测到的物体信息------>>").append("\n")
                .append("物体类型: ").append(szObjectType).append("\n")
                .append("物体子类型: ").append(szObjectSubType);
        System.out.println(noDetectEventObjInfo.toString());

        ///////////////// <<------保存图片------>> /////////////////
        ////// 抓拍到物体的信息
        NetSDKLib.NET_PIC_INFO detectObjPicInfo = stuDetectionInfo.stuObject.stPicInfo;
        String uuid = UUID.randomUUID().toString();

        if ((stuDetectionInfo.stuObject.bPicEnble == 1) && (detectObjPicInfo != null)) {
            int detectPicOffset = detectObjPicInfo.dwOffSet;
            int detectPicLength = detectObjPicInfo.dwFileLenth;
            String detectObjPicName = String.format("%s_%s_%s.jpg", "ParkingDetection-Obj", detectTime.replaceAll("[^0-9]", "-"), uuid);
            String detectObjPath = imageSaveFolder + detectObjPicName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, detectPicOffset, detectPicLength, detectObjPath));
        }

        /////// 公共信息图片 没有使能合成的话只有一张图返回，可以保存整个pBuffer
        String picPath = imageSaveFolder + String.format("%s_%s_%s.jpg", "ParkingDetection", detectTime.replaceAll("[^0-9]", "-"), uuid);
        eventCBQueueService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));
    }

    // 手动抓图事件
    private void ParsingManualSnapEvent(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) {
        System.out.println("\n\n<Event> TRAFFIC [ MANUAL SNAP]");

        // （与RPC接口snapshot对应）

        ///////////////// <<------手动抓拍事件主要信息------>> /////////////////
        NetSDKLib.DEV_EVENT_TRAFFIC_MANUALSNAP_INFO stuManualSnap = new NetSDKLib.DEV_EVENT_TRAFFIC_MANUALSNAP_INFO();
        ToolKits.GetPointerDataToStruct(pAlarmInfo, 0, stuManualSnap);

        String snapEventTime = null;
        try {
            String snapSzName = new String(stuManualSnap.szName, encode).trim();
            int snapEventID = stuManualSnap.nEventID;                                                 // 事件ID
            snapEventTime = stuManualSnap.UTC.toString();                                      // 事件抓拍时间
            String snapSzPlateNumber = new String(stuManualSnap.stTrafficCar.szPlateNumber, encode).trim();   // 车牌号
            String snapSzManualSnapNo = new String(stuManualSnap.szManualSnapNo, encode).trim();    // 抓拍序号
            int snapByImageIndex = stuManualSnap.byImageIndex;
            StringBuilder snapEventMainInfo = new StringBuilder()
                    .append("<<------手动抓拍事件主要信息------>>").append("\n")
                    .append("事件名: ").append(snapSzName).append("\n")
                    .append("事件ID: ").append(snapEventID).append("\n")
                    .append("时间: ").append(snapEventTime).append("\n")
                    .append("车牌名").append(snapSzPlateNumber).append("\n")
                    .append("手动抓拍序号: ").append(snapSzManualSnapNo).append("\n")
                    .append("图片的序号").append(snapByImageIndex);
            System.out.println(snapEventMainInfo.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //////////////// <<-----保存图片----->> ////////////////
        ////// 抓拍到物体的信息
        NetSDKLib.NET_PIC_INFO snapObjPicInfo = stuManualSnap.stuObject.stPicInfo;
        String uuid = UUID.randomUUID().toString();

        if ((stuManualSnap.stuObject.bPicEnble == 1) && (snapObjPicInfo != null)) {
            int snapPicOffset = snapObjPicInfo.dwOffSet;
            int snapPicLength = snapObjPicInfo.dwFileLenth;
            String snapObjPicName = String.format("%s_%s_%s.jpg", "MANUAL-SNAP-Obj", snapEventTime.replaceAll("[^0-9]", "-"), uuid);
            String snapObjPath = imageSaveFolder + snapObjPicName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, snapPicOffset, snapPicLength, snapObjPath));
        }
        ////// 抓拍图片只有一张原图，直接把整个缓存保存就行
        String picPath = imageSaveFolder + String.format("%s_%s_%s.jpg", "MANUAL-SNAP", snapEventTime.replaceAll("[^0-9]", "-"), uuid);
        eventCBQueueService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));
    }

    // 停车位状态检测事件
    private void ParsingParkingLotStatusDetectionEvent(Pointer pAlarmInfo, Pointer pBuffer) {
        System.out.println("\n\n<Event> TRAFFIC [ PARKING LOT STATUS DETECTION]");

        ///////////////// <<------手动抓拍事件主要信息------>> /////////////////
        DEV_EVENT_PARKING_LOT_STATUS_DETECTION_INFO stuPLSDetection = new DEV_EVENT_PARKING_LOT_STATUS_DETECTION_INFO();
        ToolKits.GetPointerDataToStruct(pAlarmInfo, 0, stuPLSDetection);

        String plsSzName = new String(stuPLSDetection.szName).trim();
        String plsTime = stuPLSDetection.UTC.toString();
        int plsStatusNum = stuPLSDetection.nParkingStatusNum;
        StringBuilder plsDetectMainInfo = new StringBuilder()
                .append("<<------停车位状态检测事件------>>").append("\n")
                .append("事件名: ").append(plsSzName).append("\n")
                .append("发生时间: ").append(plsTime).append("\n")
                .append("室外停车位个数: ").append(plsStatusNum);
        System.out.println(plsDetectMainInfo.toString());

        ////////////// 每个停车位信息
        StringBuilder plsDetectDetailInfo = new StringBuilder()
                .append("<<------每个停车位信息------>>");
        for (int i = 0; i < stuPLSDetection.nParkingStatusNum; i++) {
            try {
                plsDetectDetailInfo.append("\n").append("第 ").append(i + 1).append(" 个停车位信息")
                        .append("\n").append("车位名称: ").append(new String(stuPLSDetection.stuParkingStatus[i].szName, encode).trim())
                        .append("\n").append("车位ID: ").append(stuPLSDetection.stuParkingStatus[i].nID)
                        .append("\n").append("车位内已停车位数量: ").append(stuPLSDetection.stuParkingStatus[i].nParkedNumber)
                        .append("\n").append("相对上次上报的变化状态: ").append(stuPLSDetection.stuParkingStatus[i].emChangeStatus);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println(plsDetectDetailInfo.toString());

        ////// 抓拍图片(如果有)
        String uuid = UUID.randomUUID().toString();
        if (stuPLSDetection.stuSceneImage.nLength > 0) {
            String picPath = imageSaveFolder + String.format("%s_%s_%s.jpg", "PARKING_LOT_STATUS", plsTime.replaceAll("[^0-9]", "-"), uuid);
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, stuPLSDetection.stuSceneImage.nOffSet, stuPLSDetection.stuSceneImage.nLength, picPath));
        }
    }

    // 车辆驶入事件
    private void ParsingCarDrivingInEvent(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) {
        System.out.println("\n\n<Event> TRAFFIC [ CAR DRIVING IN]");

        ///////////////// <<------车辆驶入事件主要信息------>> /////////////////
        DEV_EVENT_CAR_DRIVING_IN_INFO stuCarDrivingIn = new DEV_EVENT_CAR_DRIVING_IN_INFO();
        ToolKits.GetPointerDataToStruct(pAlarmInfo, 0, stuCarDrivingIn);

        int channel = stuCarDrivingIn.nChannelID;
        int nAction = stuCarDrivingIn.nAction;
        String szName = null;
        try {
            szName = new String(stuCarDrivingIn.szName, encode).trim();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        double PTS = stuCarDrivingIn.PTS;
        String UTC = stuCarDrivingIn.UTC.toString();

        String emTriggerTypeNote = EM_TRIGGER_TYPE.getNoteByValue(stuCarDrivingIn.emTriggerType);
        int nMark = stuCarDrivingIn.nMark;
        int nSource = stuCarDrivingIn.nSource;
        int nFrameSequence = stuCarDrivingIn.nFrameSequence;
        int nLaneID = stuCarDrivingIn.nLaneID;
        int nSpeed = stuCarDrivingIn.nSpeed;

        // 文件信息
        String stuFileInfo = String.format("" +
                        "bFileType 类型(0-普通1-合成2-抠图):%d\n" +
                        "bCount 数量:%d\nbIndex 编号:%d\n" +
                        "nGroupId 标识ID:%d",
                stuCarDrivingIn.stuFileInfo.bFileType, stuCarDrivingIn.stuFileInfo.bCount,
                stuCarDrivingIn.stuFileInfo.bIndex, stuCarDrivingIn.stuFileInfo.nGroupId);


        // 车牌信息
        String stuObjectInfo = null;
        try {
            stuObjectInfo = String.format("" +
                            "szObjectType 类型:%s\n" +
                            "szObjectSubType 子类型:%s\n" +
                            "szText 车牌:%s",
                    new String(stuCarDrivingIn.stuObject.szObjectType, encode).trim(),
                    new String(stuCarDrivingIn.stuObject.szObjectSubType, encode).trim(),
                    new String(stuCarDrivingIn.stuObject.szText, encode).trim());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 车辆信息
        String stuVehicleInfo = null;
        try {
            stuVehicleInfo = String.format("" +
                            "szObjectType 类型:%s\n" +
                            "szObjectSubType 子类型:%s\n" +
                            "szText 车型:%s",
                    new String(stuCarDrivingIn.stuVehicle.szObjectType, encode).trim(),
                    new String(stuCarDrivingIn.stuVehicle.szObjectSubType, encode).trim(),
                    new String(stuCarDrivingIn.stuVehicle.szText, encode).trim());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 交通信息
        String stuTrafficInfo = null;
        try {
            stuTrafficInfo = String.format("" +
                            "szPlateNumber 车牌号:%s\n" +
                            "szPlateColor 车牌颜色:%s\n" +
                            "szVehicleColor 车辆颜色:%s",
                    new String(stuCarDrivingIn.stuTrafficCar.szPlateNumber, encode).trim(),
                    new String(stuCarDrivingIn.stuTrafficCar.szPlateColor, encode).trim(),
                    new String(stuCarDrivingIn.stuTrafficCar.szVehicleColor, encode).trim()
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 公共信息
        String stuCommonInfo = String.format("emVehicleTypeByFunc 车辆类型 参考 EM_VEHICLE_TYPE_BY_FUNC:%d\n" +
                        "emStandardVehicleType 标准车辆类型 EM_STANDARD_VEHICLE_TYPE:%d",
                stuCarDrivingIn.stuCommInfo.emVehicleTypeByFunc,
                stuCarDrivingIn.stuCommInfo.emStandardVehicleType);


        StringBuilder carDrivingInInfo = new StringBuilder()
                .append("<<------车辆驶入事件信息------>>").append("\n")
                .append("channel 通道号: ").append(channel).append("\n")
                .append("nAction 事件状态(0:脉冲): ").append(nAction).append("\n")
                .append("szName 事件名称: ").append(szName).append("\n")
                .append("PTS 时间戳: ").append(PTS).append("\n")
                .append("UTC 发生时间: ").append(UTC).append("\n")
                .append("emTriggerType 触发类新(EM_TRIGGER_TYPE): ").append(emTriggerTypeNote).append("\n")
                .append("nMark 抓拍帧: ").append(nMark).append("\n")
                .append("nSource 视频分析数据源地址: ").append(nSource).append("\n")
                .append("nFrameSequence 视频分析帧序号: ").append(nFrameSequence).append("\n")
                .append("nLaneID 发生时间: ").append(nLaneID).append("\n")
                .append("nSpeed 车速: ").append(nSpeed).append("\n")
                .append("//-->stuFileInfo 文件信息: ").append("\n")
                .append(stuFileInfo).append("\n")
                .append("//-->stuObjectInfo 车牌信息").append("\n")
                .append(stuObjectInfo).append("\n")
                .append("//-->stuVehicleInfo 车辆信息").append("\n")
                .append(stuVehicleInfo).append("\n")
                .append("//-->stuTrafficInfo 交通信息").append("\n")
                .append(stuTrafficInfo).append("\n")
                .append("//-->stuCommonInfo 公共信息").append("\n")
                .append(stuCommonInfo);
        System.out.println(carDrivingInInfo.toString());

        //////////////// <<-----保存图片 合成图、车牌抠图----->> ////////////////

        String uuid = UUID.randomUUID().toString();

        ////// 抓拍到物体的信息-->车牌
        NetSDKLib.NET_PIC_INFO objPicInfo = stuCarDrivingIn.stuObject.stPicInfo;
        if ((stuCarDrivingIn.stuObject.bPicEnble == 1) && (objPicInfo != null)) {
            int offset = objPicInfo.dwOffSet;
            int length = objPicInfo.dwFileLenth;
            String picName = String.format("%s_%s_%s_%s.jpg", "CarDrivingIn-Obj", stuCarDrivingIn.stuFileInfo.nGroupId, UTC.replaceAll("[^0-9]", "-"), uuid);
            String path = imageSaveFolder + picName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, offset, length, path));
        }
        ////// 抓拍到车辆的信息(现在相机并没有这张图)
        NetSDKLib.NET_PIC_INFO vehiclePicInfo = stuCarDrivingIn.stuVehicle.stPicInfo;
        if ((stuCarDrivingIn.stuVehicle.bPicEnble == 1) && (vehiclePicInfo != null)) {
            int offset = vehiclePicInfo.dwOffSet;
            int length = vehiclePicInfo.dwFileLenth;
            String picName = String.format("%s_%s_%s_%s.jpg", "CarDrivingIn-Vehicle", stuCarDrivingIn.stuFileInfo.nGroupId, UTC.replaceAll("[^0-9]", "-"), uuid);
            String path = imageSaveFolder + picName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, offset, length, path));
        }
        ////// 公共信息(现在相机也没有这里的图)
        NetSDKLib.EVENT_COMM_INFO commInfo = stuCarDrivingIn.stuCommInfo;
        int nPictureNum = commInfo.nPictureNum;    // 原始图片张数

        for (int i = 0; i < nPictureNum; i++) {
            int length = commInfo.stuPicInfos[i].nLength;
            int offSet = commInfo.stuPicInfos[i].nOffset;
            String picName = String.format("%s_%02d_%s_%s_%s.jpg", "CarDrivingIn-Comm", i, stuCarDrivingIn.stuFileInfo.nGroupId, UTC.replaceAll("[^0-9]", "-"), uuid);
            String savePath = imageSaveFolder + picName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, offSet, length, savePath));
        }
        ////// 原始图片(相机只会回传原始图, 盒子会绑定GroupID从下一个事件中传回合成图)
        String picPath = imageSaveFolder + String.format("%s_%s_%s_%s.jpg", "CarDrivingIn-Original", stuCarDrivingIn.stuFileInfo.nGroupId, UTC.replaceAll("[^0-9]", "-"), uuid);
        eventCBQueueService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));
    }

    // 车辆驶出事件
    private void ParsingCarDrivingOutEvent(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) {

        ///////////////// <<------车辆驶出事件主要信息------>> /////////////////
        DEV_EVENT_CAR_DRIVING_OUT_INFO stuCarDrivingOut = new DEV_EVENT_CAR_DRIVING_OUT_INFO();
        ToolKits.GetPointerDataToStruct(pAlarmInfo, 0, stuCarDrivingOut);

        int channel = stuCarDrivingOut.nChannelID;
        int nAction = stuCarDrivingOut.nAction;
        String szName = null;
        try {
            szName = new String(stuCarDrivingOut.szName, encode).trim();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        double PTS = stuCarDrivingOut.PTS;
        String UTC = stuCarDrivingOut.UTC.toString();

        String emTriggerTypeNote = EM_TRIGGER_TYPE.getNoteByValue(stuCarDrivingOut.emTriggerType);
        int nMark = stuCarDrivingOut.nMark;
        int nSource = stuCarDrivingOut.nSource;
        int nFrameSequence = stuCarDrivingOut.nFrameSequence;
        int nLaneID = stuCarDrivingOut.nLaneID;
        int nSpeed = stuCarDrivingOut.nSpeed;

        // 文件信息
        String stuFileInfo = String.format("" +
                        "bFileType 类型(0-普通1-合成2-抠图):%d\n" +
                        "bCount 数量:%d\nbIndex 编号:%d\n" +
                        "nGroupId 标识ID:%d",
                stuCarDrivingOut.stuFileInfo.bFileType, stuCarDrivingOut.stuFileInfo.bCount,
                stuCarDrivingOut.stuFileInfo.bIndex, stuCarDrivingOut.stuFileInfo.nGroupId);


        // 车牌信息
        String stuObjectInfo = null;
        try {
            stuObjectInfo = String.format("" +
                            "szObjectType 类型:%s\n" +
                            "szObjectSubType 子类型:%s\n" +
                            "szText 车牌:%s",
                    new String(stuCarDrivingOut.stuObject.szObjectType, encode).trim(),
                    new String(stuCarDrivingOut.stuObject.szObjectSubType, encode).trim(),
                    new String(stuCarDrivingOut.stuObject.szText, encode).trim());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 车辆信息
        String stuVehicleInfo = null;
        try {
            stuVehicleInfo = String.format("" +
                            "szObjectType 类型:%s\n" +
                            "szObjectSubType 子类型:%s\n" +
                            "szText 车型:%s",
                    new String(stuCarDrivingOut.stuVehicle.szObjectType, encode).trim(),
                    new String(stuCarDrivingOut.stuVehicle.szObjectSubType, encode).trim(),
                    new String(stuCarDrivingOut.stuVehicle.szText, encode).trim());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 交通信息
        String stuTrafficInfo = null;
        try {
            stuTrafficInfo = String.format("" +
                            "szPlateNumber 车牌号:%s\n" +
                            "szPlateColor 车牌颜色:%s\n" +
                            "szVehicleColor 车辆颜色:%s",
                    new String(stuCarDrivingOut.stuTrafficCar.szPlateNumber, encode).trim(),
                    new String(stuCarDrivingOut.stuTrafficCar.szPlateColor, encode).trim(),
                    new String(stuCarDrivingOut.stuTrafficCar.szVehicleColor, encode).trim()
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 公共信息
        String stuCommonInfo = null;
        try {
            stuCommonInfo = String.format("emVehicleTypeByFunc 车辆类型 参考 EM_VEHICLE_TYPE_BY_FUNC:%d\n" +
                            "emStandardVehicleType 标准车辆类型 EM_STANDARD_VEHICLE_TYPE:%d\n" +
                            "szCountry 国家:%s",
                    stuCarDrivingOut.stuCommInfo.emVehicleTypeByFunc,
                    stuCarDrivingOut.stuCommInfo.emStandardVehicleType,
                    new String(stuCarDrivingOut.stuCommInfo.szCountry, encode).trim());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        StringBuilder carDrivingOutInfo = new StringBuilder()
                .append("<<------车辆驶入事件信息------>>").append("\n")
                .append("channel 通道号: ").append(channel).append("\n")
                .append("nAction 事件状态(0:脉冲): ").append(nAction).append("\n")
                .append("szName 事件名称: ").append(szName).append("\n")
                .append("PTS 时间戳: ").append(PTS).append("\n")
                .append("UTC 发生时间: ").append(UTC).append("\n")
                .append("emTriggerType 触发类新(EM_TRIGGER_TYPE): ").append(emTriggerTypeNote).append("\n")
                .append("nMark 抓拍帧: ").append(nMark).append("\n")
                .append("nSource 视频分析数据源地址: ").append(nSource).append("\n")
                .append("nFrameSequence 视频分析帧序号: ").append(nFrameSequence).append("\n")
                .append("nLaneID 发生时间: ").append(nLaneID).append("\n")
                .append("nSpeed 车速: ").append(nSpeed).append("\n")
                .append("//-->stuFileInfo 文件信息: ").append("\n")
                .append(stuFileInfo).append("\n")
                .append("//-->stuObjectInfo 车牌信息").append("\n")
                .append(stuObjectInfo).append("\n")
                .append("//-->stuVehicleInfo 车辆信息").append("\n")
                .append(stuVehicleInfo).append("\n")
                .append("//-->stuTrafficInfo 交通信息").append("\n")
                .append(stuTrafficInfo).append("\n")
                .append("//-->stuCommonInfo 公共信息").append("\n")
                .append(stuCommonInfo);
        System.out.println(carDrivingOutInfo.toString());

        //////////////// <<-----保存图片 合成图、车牌抠图----->> ////////////////

        String uuid = UUID.randomUUID().toString();

        ////// 抓拍到物体的信息-->车牌
        NetSDKLib.NET_PIC_INFO objPicInfo = stuCarDrivingOut.stuObject.stPicInfo;
        if ((stuCarDrivingOut.stuObject.bPicEnble == 1) && (objPicInfo != null)) {
            int offset = objPicInfo.dwOffSet;
            int length = objPicInfo.dwFileLenth;
            String picName = String.format("%s_%s_%s_%s.jpg", "CarDrivingOut-Obj", stuCarDrivingOut.stuFileInfo.nGroupId, UTC.replaceAll("[^0-9]", "-"), uuid);
            String path = imageSaveFolder + picName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, offset, length, path));
        }
        ////// 抓拍到车辆的信息(现在相机并没有这张图)
        NetSDKLib.NET_PIC_INFO vehiclePicInfo = stuCarDrivingOut.stuVehicle.stPicInfo;
        if ((stuCarDrivingOut.stuVehicle.bPicEnble == 1) && (vehiclePicInfo != null)) {
            int offset = vehiclePicInfo.dwOffSet;
            int length = vehiclePicInfo.dwFileLenth;
            String picName = String.format("%s_%s_%s_%s.jpg", "CarDrivingOut-Vehicle", stuCarDrivingOut.stuFileInfo.nGroupId, UTC.replaceAll("[^0-9]", "-"), uuid);
            String path = imageSaveFolder + picName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, offset, length, path));
        }
        ////// 公共信息(现在相机也没有这里的图)
        NetSDKLib.EVENT_COMM_INFO commInfo = stuCarDrivingOut.stuCommInfo;
        int nPictureNum = commInfo.nPictureNum;    // 原始图片张数

        for (int i = 0; i < nPictureNum; i++) {
            int length = commInfo.stuPicInfos[i].nLength;
            int offSet = commInfo.stuPicInfos[i].nOffset;
            String picName = String.format("%s_%02d_%s_%s_%s.jpg", "CarDrivingOut-Comm", i, stuCarDrivingOut.stuFileInfo.nGroupId, UTC.replaceAll("[^0-9]", "-"), uuid);
            String savePath = imageSaveFolder + picName;
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, offSet, length, savePath));
        }
        ////// 原始图片(相机只会回传原始图, 盒子会绑定GroupID从下一个事件中传回合成图)
        String picPath = imageSaveFolder + String.format("%s_%s_%s_%s.jpg", "CarDrivingOut-Original", stuCarDrivingOut.stuFileInfo.nGroupId, UTC.replaceAll("[^0-9]", "-"), uuid);
        eventCBQueueService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));
    }

    // 路侧停车位手动抓图 (舟曲项目新增)
    private void ParsingTrafficParkingSpaceManualSnap(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) {

        System.out.println("<<------路侧停车位手动抓图------>>");

        ///////////////// <<------路侧停车位手动抓图信息------>> /////////////////
        DEV_EVENT_PARKINGSPACE_MANUALSNAP_INFO stuManualSnap = new DEV_EVENT_PARKINGSPACE_MANUALSNAP_INFO();
        ToolKits.GetPointerDataToStruct(pAlarmInfo, 0, stuManualSnap);

        try {
            ////// 主要数据提取
            String eventName = new String(stuManualSnap.szName, encode).trim();     // "Name" 事件名称
            int channel = stuManualSnap.nChannel;                                   // "Channel" 通道号
            double PTS = stuManualSnap.PTS;                                         // "PTS" 相对事件时间戳 (单位毫秒)
            String UTC = stuManualSnap.UTC.toString();                              // "UTC" 事件发生的时间 (单位秒)
            int eventID = stuManualSnap.nEventID;                                   // "EventID" 事件ID号 用来唯一标志一个事件
            int groupID = stuManualSnap.stuFileInfo.nGroupId;                       // "GroupID" 事件组ID，同一辆车抓拍过程内 GroupID 相同
            byte countInGroup = stuManualSnap.stuFileInfo.bCount;                   // "CountInGroup" 一个事件组内应有的抓拍张数
            byte index = stuManualSnap.stuFileInfo.bIndex;                          // "IndexInGroup" 一个事件组内的抓拍序号，从1开始
            String serialNo = new String(stuManualSnap.szSerialNo, encode).trim();  // "SerialNo" 客户端请求的抓图序列号对应

            // 主要数据打印
            String mainInfo = "///——————事件主要信息——————" + "\n" +
                    "Name 事件名称: " + eventName + "\n" +
                    "Channel 通道号: " + channel + "\n" +
                    "PTS 相对事件时间戳: " + PTS + "\n" +
                    "UTC 事件发生的时间: " + UTC + "\n" +
                    "EventID 事件ID号: " + eventID + "\n" +
                    "GroupID 事件组ID: " + groupID + "\n" +
                    "CountInGroup 应有张数: " + countInGroup + "\n" +
                    "Index 抓拍序号: " + index + "\n" +
                    "SerialNo 客户端抓图序列号: " + serialNo;
            System.out.println(mainInfo);

            ////// 提取并打印停车位信息
            StringBuilder parkingInfo = new StringBuilder().append("///——————车位信息——————").append("\n");

            int parkingNum = stuManualSnap.nParkingNum;
            parkingInfo.append("车位总数: ").append(parkingNum).append("\n");

            for (int i = 0; i < parkingNum; i++) {
                int nStatus = stuManualSnap.stuParkingInfo[i].nStatus;
                String parkingStatus = (nStatus == 0) ? "未知" : (nStatus == 1) ? "有车" : "无车";          // "Status" 是否有车
                String plateNumber = new String(stuManualSnap.stuParkingInfo[i].szPlateNumber, encode).trim(); // "PlateNumber" 车牌号
                String parkingNo = new String(stuManualSnap.stuParkingInfo[i].szParkingNo, encode).trim();     // "ParkingNo" 车位号

                parkingInfo.append(String.format("//——>第[%2d]个车位", i)).append("\n")
                        .append("Status 是否有车: ").append(parkingStatus).append("\n")
                        .append("PlateNumber 车牌号: ").append(plateNumber).append("\n")
                        .append("ParkingNo 车位号: ").append(parkingNo).append("\n");
            }
            System.out.println(parkingInfo.toString());

            //////////////// <<-----保存图片 没有子图 直接保存整个 pBuffer 即可 ----->> ////////////////

            String uuid = UUID.randomUUID().toString();
            String picPath = imageSaveFolder + String.format("%s_%s_%s_%s.jpg",
                    "ParkingSpaceManualSnap-Original", stuManualSnap.stuFileInfo.nGroupId,
                    UTC.replaceAll("[^0-9]", "-"), uuid);
            eventCBQueueService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    // 保存图片
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
