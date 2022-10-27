package com.netsdk.demo.example.parkingDemo.callback;


import com.netsdk.demo.util.EventTaskHandler;
import com.netsdk.demo.util.QueueGeneration;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;

import java.io.*;
import java.util.UUID;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/20 20:10
 */
public class DefaultAnalyzerDataCallBack implements NetSDKLib.fAnalyzerDataCallBack {

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

    private final String imageSaveFolder = "TrafficParkingPic/";      // 图片保存路径

    @Override
    public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved) {
        switch (dwAlarmType) {
            case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING: /// 车位有车事件
            {
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

                ////// 车位、车身抓拍图片 这个数据前端相机不一定支持 这里就先注释了
                 if (stuParkingInfo.stuParkingImage.nLength > 0) {
                     String savePath = imageSaveFolder + String.format("%s_%s_%s_%s.jpg", "Parking-ParkingImage", stuParkingInfo.stuFileInfo.nGroupId, parkingEventTime.replaceAll("[^0-9]", "-"), uuid);
                     eventCBQueueService.addEvent(new SavePicHandler(pBuffer, stuParkingInfo.stuParkingImage.nOffSet, stuParkingInfo.stuParkingImage.nLength, savePath));
                 }

                /////// 事件原图
                String picPath = imageSaveFolder + String.format("%s_%s_%s_%s.jpg", "Parking", stuParkingInfo.stuFileInfo.nGroupId, parkingEventTime.replaceAll("[^0-9]", "-"), uuid);
                eventCBQueueService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));
                break;
            }
            case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING: /// 车位无车事件
            {
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

                StringBuilder noParkingEventMainInfo = new StringBuilder()
                        .append("<<------车位无车事件主要信息------>>").append("\n")
                        .append("事件名称: ").append(noParkingSzName).append("\n")
                        .append("事件ID: ").append(noParkingEventID).append("\n")
                        .append("抓拍时间: ").append(noParkingEventTime).append("\n")
                        .append("车位号").append(noParkingSzParkingNum).append("\n")
                        .append("车牌号: ").append(noParkingSzPlateNumber);

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
                break;
            }
            case NetSDKLib.EVENT_IVS_PARKINGDETECTION:   /// 车位检测事件
            {
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
                break;
            }
            case NetSDKLib.EVENT_IVS_TRAFFIC_MANUALSNAP: /// 手动抓图
            {
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
                    String snapSzManualSnapNo = new String(stuManualSnap.szManualSnapNo, encode).trim();              // 车位号
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
                break;
            }
            default:
                System.out.printf("Get Event 0x%x\n", dwAlarmType);
                break;
        }
        return 0;
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
