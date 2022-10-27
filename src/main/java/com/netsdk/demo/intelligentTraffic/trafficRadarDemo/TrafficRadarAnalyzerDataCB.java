package com.netsdk.demo.intelligentTraffic.trafficRadarDemo;

import com.netsdk.demo.util.EventTaskHandler;
import com.netsdk.demo.util.QueueGeneration;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_TRAFFIC_CARDISTANCESHORT_INFO;
import com.netsdk.lib.structure.DEV_EVENT_TRAFFIC_ROAD_ALERT_INFO;
import com.sun.jna.Pointer;

import java.io.*;
import java.util.UUID;

/**
 * @author 47040
 * @since Created in 2020/12/14 11:10
 */
public class TrafficRadarAnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {

    private static TrafficRadarAnalyzerDataCB singleInstance;

    public static TrafficRadarAnalyzerDataCB getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new TrafficRadarAnalyzerDataCB();
        }
        return singleInstance;
    }

    public TrafficRadarAnalyzerDataCB() {
        ImgSaveService.init();
    }

    public static final String encode = TrafficRadarUtils.GetSystemEncode();        // 获取此平台的字符串编码
    private final QueueGeneration ImgSaveService = new QueueGeneration();           // 保存图片用的线程池阻塞队列
    private final String imageSaveFolder = "TrafficRadarPic/";                      // 图片保存路径

    @Override
    public int invoke(NetSDKLib.LLong lAnalyzerHandle,      // 订阅句柄
                      int dwAlarmType,                      // 事件枚举
                      Pointer pAlarmInfo,                   // 事件数据指针
                      Pointer pBuffer,                      // 事件图片数据指针
                      int dwBufSize,                        // 事件图片数据长度
                      Pointer dwUser, int nSequence, Pointer reserved) {

        try {
            switch (dwAlarmType) {
                case NetSDKLib.EVENT_IVS_TRAFFIC_CARDISTANCESHORT: /// 车距过小报警 (未与前车保持安全距离)
                {
                    ParseEventTrafficCarDistanceShort(pAlarmInfo, pBuffer, dwBufSize);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_ROAD_ALERT: /// 道路安全预警
                {
                    ParseTrafficRoadAlert(pAlarmInfo, pBuffer, dwBufSize);
                    break;
                }
                default:
                    System.out.printf("Get Other Event 0x%x\n", dwAlarmType);
                    break;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return 0;
    }

    // 车距过小事件
    private void ParseEventTrafficCarDistanceShort(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) throws UnsupportedEncodingException {
        System.out.println("\n\n<IVS Event> TRAFFIC [ TRAFFIC CAR DISTANCE SHORT 车距过小报警 ]");

        ////////////////////////////// <<-----获取事件信息----->> //////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////

        DEV_EVENT_TRAFFIC_CARDISTANCESHORT_INFO eventInfo = new DEV_EVENT_TRAFFIC_CARDISTANCESHORT_INFO();
        ToolKits.GetPointerData(pAlarmInfo, eventInfo);

        int nGroupId = eventInfo.stuFileInfo.nGroupId;      // 同一组抓拍会以多个事件回调返回，但 nGroupId 会保持一致
        int countInGroup = eventInfo.stuFileInfo.bCount;    // 此 Group 下共有几个事件
        int indexInGroup = eventInfo.stuFileInfo.bIndex;    // 本次事件在 Group 内的序号
        int nSequence = eventInfo.nSequence;                // 车距过小事件特有的抓拍序号 如果最后一个事件上报为 0 说明抓拍异常
        int nChannel = eventInfo.nChannelID;                                                    // 事件发生通道
        String szName = new String(eventInfo.szName, encode).trim();                            // 事件名称
        double dbPTS = eventInfo.dbPTS;                                                         // 时间戳(单位毫秒)
        String UTC = eventInfo.UTC.toString();                                                  // 事件抓拍时间
        int nEventId = eventInfo.nEventID;                                                      // 事件ID
        int nLane = eventInfo.nLane;                                                            // 车道号
        String szPlateNumber = new String(eventInfo.stTrafficCar.szPlateNumber, encode).trim(); // 车牌号

        /// 打印看一下
        System.out.println(String.format("GroupId:%s Count:%d Index:%d Sequence:%d ",
                nGroupId, countInGroup, indexInGroup, nSequence));
        String eventMainInfo = "\n<<------事件主要信息------>>" + "\n" +
                "   nChannel(事件通道): " + nChannel + "\n" +
                "   szName(事件名称): " + szName + "\n" +
                "   dbPTS(时间戳 毫秒): " + dbPTS + "\n" +
                "   UTC(抓拍时间): " + UTC + "\n" +
                "   nEventId(事件ID): " + nEventId + "\n" +
                "   nLane(车道号): " + nLane + "\n" +
                "   szPlateNumber(车牌号): " + szPlateNumber;
        System.out.println(eventMainInfo);

        //////////////////////////////// <<-----保存图片----->> ////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////
        String uuid = UUID.randomUUID().toString();

        ////// 车牌图片 不一定有
        NetSDKLib.NET_PIC_INFO carPlatePicInfo = eventInfo.stuObject.stPicInfo;
        if ((eventInfo.stuObject.bPicEnble == 1) && (carPlatePicInfo != null)) {
            int offset = carPlatePicInfo.dwOffSet;
            int length = carPlatePicInfo.dwFileLenth;
            String picName = String.format("%s_%s_%d_%s_%s.jpg",
                    "CarDistanceShort-ObjPlate", nGroupId, indexInGroup, UTC.replaceAll("[^0-9]", "-"), uuid);
            String picPath = imageSaveFolder + picName;
            ImgSaveService.addEvent(new SavePicHandler(pBuffer, offset, length, picPath));
        }

        ////// 车身图片 不一定有
        NetSDKLib.NET_PIC_INFO carVehiclePicInfo = eventInfo.stuObject.stPicInfo;
        if ((eventInfo.stuObject.bPicEnble == 1) && (carVehiclePicInfo != null)) {
            int offset = carVehiclePicInfo.dwOffSet;
            int length = carVehiclePicInfo.dwFileLenth;
            String picName = String.format("%s_%s_%d_%s_%s.jpg",
                    "CarDistanceShort-ObjVehicle", nGroupId, indexInGroup, UTC.replaceAll("[^0-9]", "-"), uuid);
            String picPath = imageSaveFolder + picName;
            ImgSaveService.addEvent(new SavePicHandler(pBuffer, offset, length, picPath));
        }

        /////// 公共信息图片 不一定有
        NetSDKLib.EVENT_COMM_INFO commInfo = eventInfo.stCommInfo;
        int nPictureNum = commInfo.nPictureNum;    // 原始图片张数

        for (int i = 0; i < nPictureNum; i++) {
            int length = commInfo.stuPicInfos[i].nLength;
            int offSet = commInfo.stuPicInfos[i].nOffset;
            String parkingCommPicName = String.format("%s_%02d_%s_%d_%s_%s.jpg",
                    "CarDistanceShort-Common", i, nGroupId, indexInGroup, UTC.replaceAll("[^0-9]", "-"), uuid);
            String savePath = imageSaveFolder + parkingCommPicName;
            ImgSaveService.addEvent(new SavePicHandler(pBuffer, offSet, length, savePath));
        }

        /////// 事件原图 这个一定有
        String picPath = imageSaveFolder + String.format("%s_%s_%d_%s_%s.jpg",
                "CarDistanceShort-Original", nGroupId, indexInGroup, UTC.replaceAll("[^0-9]", "-"), uuid);
        ImgSaveService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));
    }

    // 道路安全预警
    private void ParseTrafficRoadAlert(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) throws UnsupportedEncodingException {
        System.out.println("\n\n<IVS Event> TRAFFIC [ TRAFFIC ROAD ALERT 道路安全预警 ]");

        ////////////////////////////// <<-----获取事件信息----->> //////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////

        DEV_EVENT_TRAFFIC_ROAD_ALERT_INFO eventInfo = new DEV_EVENT_TRAFFIC_ROAD_ALERT_INFO();
        ToolKits.GetPointerData(pAlarmInfo, eventInfo);

        int nGroupId = eventInfo.nGroupID;              // 同一组抓拍会以多个事件回调返回，但 nGroupId 会保持一致
        int countInGroup = eventInfo.nCountInGroup;     // 此 Group 下共有几个事件
        int indexInGroup = eventInfo.nIndexInGroup;     // 本次事件在 Group 内的序号
        int nSequence = eventInfo.nSequence;            // 车距过小事件特有的抓拍序号 如果最后一个事件上报为 0 说明抓拍异常
        int nChannel = eventInfo.nChannelID;                                                    // 事件发生通道
        String szName = new String(eventInfo.szName, encode).trim();                            // 事件名称
        double PTS = eventInfo.PTS;                                                             // 时间戳(单位毫秒)
        String UTC = eventInfo.UTC.toString();                                                  // 事件抓拍时间
        int nEventId = eventInfo.nEventID;                                                      // 事件ID
        int nLane = eventInfo.nLane;                                                            // 车道号
        String szPlateNumber = new String(eventInfo.stTrafficCar.szPlateNumber, encode).trim(); // 车牌号

        /// 打印看一下
        System.out.println(String.format("GroupId:%s Count:%d Index:%d Sequence:%d ",
                nGroupId, countInGroup, indexInGroup, nSequence));
        String eventMainInfo = "\n<<------事件主要信息------>>" + "\n" +
                "   nChannel(事件通道): " + nChannel + "\n" +
                "   szName(事件名称): " + szName + "\n" +
                "   PTS(时间戳 毫秒): " + PTS + "\n" +
                "   UTC(抓拍时间): " + UTC + "\n" +
                "   nEventId(事件ID): " + nEventId + "\n" +
                "   nLane(车道号): " + nLane + "\n" +
                "   szPlateNumber(车牌号): " + szPlateNumber;
        System.out.println(eventMainInfo);

        //////////////////////////////// <<-----保存图片----->> ////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////
        String uuid = UUID.randomUUID().toString();

        ////// 车牌图片 不一定有
        NetSDKLib.NET_PIC_INFO carPlatePicInfo = eventInfo.stuObject.stPicInfo;
        if ((eventInfo.stuObject.bPicEnble == 1) && (carPlatePicInfo != null)) {
            int offset = carPlatePicInfo.dwOffSet;
            int length = carPlatePicInfo.dwFileLenth;
            String picName = String.format("%s_%s_%d_%s_%s.jpg",
                    "RoadAlert-ObjPlate", nGroupId, indexInGroup, UTC.replaceAll("[^0-9]", "-"), uuid);
            String picPath = imageSaveFolder + picName;
            ImgSaveService.addEvent(new SavePicHandler(pBuffer, offset, length, picPath));
        }

        ////// 车身图片 不一定有
        NetSDKLib.NET_PIC_INFO carVehiclePicInfo = eventInfo.stuObject.stPicInfo;
        if ((eventInfo.stuObject.bPicEnble == 1) && (carVehiclePicInfo != null)) {
            int offset = carVehiclePicInfo.dwOffSet;
            int length = carVehiclePicInfo.dwFileLenth;
            String picName = String.format("%s_%s_%d_%s_%s.jpg",
                    "RoadAlert-ObjVehicle", nGroupId, indexInGroup, UTC.replaceAll("[^0-9]", "-"), uuid);
            String picPath = imageSaveFolder + picName;
            ImgSaveService.addEvent(new SavePicHandler(pBuffer, offset, length, picPath));
        }

        /////// 公共信息图片 不一定有
        NetSDKLib.EVENT_COMM_INFO commInfo = eventInfo.stCommInfo;
        int nPictureNum = commInfo.nPictureNum;    // 原始图片张数

        for (int i = 0; i < nPictureNum; i++) {
            int length = commInfo.stuPicInfos[i].nLength;
            int offSet = commInfo.stuPicInfos[i].nOffset;
            String parkingCommPicName = String.format("%s_%02d_%s_%d_%s_%s.jpg",
                    "RoadAlert-Common", i, nGroupId, indexInGroup, UTC.replaceAll("[^0-9]", "-"), uuid);
            String savePath = imageSaveFolder + parkingCommPicName;
            ImgSaveService.addEvent(new SavePicHandler(pBuffer, offSet, length, savePath));
        }

        /////// 事件原图 这个一定有
        String picPath = imageSaveFolder + String.format("%s_%s_%d_%s_%s.jpg",
                "RoadAlert-Original", nGroupId, indexInGroup, UTC.replaceAll("[^0-9]", "-"), uuid);
        ImgSaveService.addEvent(new SavePicHandler(pBuffer, 0, dwBufSize, picPath));
    }

    // 保存图片
    private class SavePicHandler implements EventTaskHandler {

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
            if (!path.exists()) {
                if (!path.mkdir()) {
                    System.err.println("创建文件夹失败.");
                    return;
                }
            }
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
