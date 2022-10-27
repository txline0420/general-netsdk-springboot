package com.netsdk.demo.customize.surfaceEventDemo;

import com.netsdk.demo.util.EventTaskHandler;
import com.netsdk.demo.util.QueueGeneration;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.structure.customize.DEV_EVENT_CROSSLINE_INFO;
import com.netsdk.lib.structure.customize.DEV_EVENT_CROSSREGION_INFO;
import com.netsdk.lib.structure.customize.DEV_EVENT_PARKINGDETECTION_INFO;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author 47040
 * @since Created in 2021/5/11 14:30
 */
public class SurfaceAnalyzerDataCallBack implements NetSDKLib.fAnalyzerDataCallBack {

    private static SurfaceAnalyzerDataCallBack instance;

    public static SurfaceAnalyzerDataCallBack getInstance() {
        if (instance == null) {
            instance = new SurfaceAnalyzerDataCallBack();
        }
        return instance;
    }

    private static final String imageSaveRoot = "surfaceEventPic";      // 图片保存根路径

    static {
        // 创建图片根文件夹
        File path = new File(imageSaveRoot);
        if (!path.exists()) {
            if (!path.mkdir()) {
                System.out.println("创建文件夹失败");
            }
        }
    }

    // 多平台 文字编码
    private final Charset encode = Charset.forName(Utils.getPlatformEncode());

    private final QueueGeneration imageSaveService = new QueueGeneration();    // 保存图片用队列

    private final LinkedBlockingQueue<SurfaceEventInfo> eventQueue = EventResource.getSurfaceEventQueue();

    public SurfaceAnalyzerDataCallBack() {
        imageSaveService.init();
    }

    @Override
    public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved) {
        switch (dwAlarmType) {
            case NetSDKLib.EVENT_IVS_CROSSLINEDETECTION:  /// 警戒线事件 (船只的绊线入侵)
            {
                // 注意，这里使用的是定制结构体
                DEV_EVENT_CROSSLINE_INFO msg = new DEV_EVENT_CROSSLINE_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);

                CrossLineDetectionInfo eventInfo = new CrossLineDetectionInfo();
                eventInfo.setEventID(msg.nEventID);                     // eventId
                eventInfo.setUTC(msg.UTC.toString());                   // UTC
                eventInfo.setChannel(msg.nChannelID);                   // channel
                eventInfo.setFileCount((int) msg.stuFileInfo.bCount);   // count
                eventInfo.setFileIndex((int) msg.stuFileInfo.bIndex);   // index

                eventInfo.setPresentID(msg.stuIntelliCommInfo.nPresetID);  // presetID
                eventInfo.setPositionInfo(new Integer[]{
                        msg.stuIntelliCommInfo.stuPostion.nHorizontalAngle,
                        msg.stuIntelliCommInfo.stuPostion.nVerticalAngle,
                        msg.stuIntelliCommInfo.stuPostion.nMagnification
                });                                                        // position info

                //msg.
                eventInfo.setThremoHFOV(msg.nThermoHFOV);           // ThermoHFOV
                eventInfo.setThremoVFOV(msg.nThermoVFOV);           // ThermoVFOV
                eventInfo.setBoatHeight(msg.nBoatHeight);           // 船高
                eventInfo.setBoatLength(msg.nBoatLength);           // 船长
                eventInfo.setBoatSpeed(msg.nBoatSpeed);             // 船速
                eventInfo.setBoatDistance(msg.nBoatDistance);       // 船距

                eventInfo.setImagesData(dwBufSize > 0 ? pBuffer.getByteArray(0, dwBufSize) : null);  // 大图
                eventQueue.offer(eventInfo);
                System.out.println(eventInfo.getBriefInfo());   // 打印下看看

                // 检测物体图 不一定有 Demo这里仅作保存 不在界面上展示
                if ((msg.stuObject.bPicEnble == 1) && (msg.stuObject.stPicInfo != null)) {
                    int offset = msg.stuObject.stPicInfo.dwOffSet;
                    int length = msg.stuObject.stPicInfo.dwFileLenth;
                    imageSaveService.addEvent(new SavePicHandler(pBuffer.getByteArray(offset, length), length, eventInfo.getRelativeFolder(), eventInfo.getObjPicName()));
                }

                // 大图
                imageSaveService.addEvent(new SavePicHandler(eventInfo.getImagesData(), dwBufSize, eventInfo.getRelativeFolder(), eventInfo.getPicName()));
                break;
            }
            case NetSDKLib.EVENT_IVS_CROSSREGIONDETECTION: /// 区域入侵事件 (船只区域入侵)
            {
                // 注意，这里使用的是定制结构体
                DEV_EVENT_CROSSREGION_INFO msg = new DEV_EVENT_CROSSREGION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);

                CrossRegionDetectionInfo eventInfo = new CrossRegionDetectionInfo();
                eventInfo.setEventID(msg.nEventID);                     // eventId
                eventInfo.setUTC(msg.UTC.toString());                   // UTC
                eventInfo.setChannel(msg.nChannelID);                   // channel
                eventInfo.setFileCount((int) msg.stuFileInfo.bCount);   // count
                eventInfo.setFileIndex((int) msg.stuFileInfo.bIndex);   // index

                eventInfo.setPresentID(msg.stuIntelliCommInfo.nPresetID);  // presetID
                eventInfo.setPositionInfo(new Integer[]{
                        msg.stuIntelliCommInfo.stuPostion.nHorizontalAngle,
                        msg.stuIntelliCommInfo.stuPostion.nVerticalAngle,
                        msg.stuIntelliCommInfo.stuPostion.nMagnification
                });                                                        // position info

                eventInfo.setThremoHFOV(msg.nThermoHFOV);           // ThermoHFOV
                eventInfo.setThremoVFOV(msg.nThermoVFOV);           // ThermoVFOV
                eventInfo.setBoatHeight(msg.nBoatHeight);           // 船高
                eventInfo.setBoatLength(msg.nBoatLength);           // 船长
                eventInfo.setBoatSpeed(msg.nBoatSpeed);             // 船速
                eventInfo.setBoatDistance(msg.nBoatDistance);       // 船距

                eventInfo.setImagesData(dwBufSize > 0 ? pBuffer.getByteArray(0, dwBufSize) : null);
                eventQueue.offer(eventInfo);
                System.out.println(eventInfo.getBriefInfo());   // 打印下看看

                // 检测物体图 不一定有 Demo这里仅作保存 不在界面上展示
                if ((msg.stuObject.bPicEnble == 1) && (msg.stuObject.stPicInfo != null)) {
                    int offset = msg.stuObject.stPicInfo.dwOffSet;
                    int length = msg.stuObject.stPicInfo.dwFileLenth;
                    imageSaveService.addEvent(new SavePicHandler(pBuffer.getByteArray(offset, length), length, eventInfo.getRelativeFolder(), eventInfo.getObjPicName()));
                }

                // 大图
                imageSaveService.addEvent(new SavePicHandler(eventInfo.getImagesData(), dwBufSize, eventInfo.getRelativeFolder(), eventInfo.getPicName()));
                break;
            }
            case NetSDKLib.EVENT_IVS_PARKINGDETECTION:    /// 停靠位检测事件 (船只停靠检测)
            {
                // 注意，这里使用的是定制结构体
                DEV_EVENT_PARKINGDETECTION_INFO msg = new DEV_EVENT_PARKINGDETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);

                ParkingDetectionInfo eventInfo = new ParkingDetectionInfo();
                eventInfo.setEventID(msg.nEventID);                     // eventId
                eventInfo.setUTC(msg.UTC.toString());                   // UTC
                eventInfo.setChannel(msg.nChannelID);                   // channel
                eventInfo.setFileCount((int) msg.stuFileInfo.bCount);   // count
                eventInfo.setFileIndex((int) msg.stuFileInfo.bIndex);   // index

                eventInfo.setPresentID(msg.stuIntelliCommInfo.nPresetID);  // presetID
                eventInfo.setPositionInfo(new Integer[]{
                        msg.stuIntelliCommInfo.stuPostion.nHorizontalAngle,
                        msg.stuIntelliCommInfo.stuPostion.nVerticalAngle,
                        msg.stuIntelliCommInfo.stuPostion.nMagnification
                });                                                        // position info

                eventInfo.setThremoHFOV(msg.nThermoHFOV);           // ThermoHFOV
                eventInfo.setThremoVFOV(msg.nThermoVFOV);           // ThermoVFOV
                eventInfo.setBoatHeight(msg.nBoatHeight);           // 船高
                eventInfo.setBoatLength(msg.nBoatLength);           // 船长
                eventInfo.setBoatSpeed(msg.nBoatSpeed);             // 船速
                eventInfo.setBoatDistance(msg.nBoatDistance);       // 船距

                eventInfo.setImagesData(dwBufSize > 0 ? pBuffer.getByteArray(0, dwBufSize) : null);
                eventQueue.offer(eventInfo);
                System.out.println(eventInfo.getBriefInfo());   // 打印下看看

                // 检测物体图 不一定有 Demo这里仅作保存 不在界面上展示
                if ((msg.stuObject.bPicEnble == 1) && (msg.stuObject.stPicInfo != null)) {
                    int offset = msg.stuObject.stPicInfo.dwOffSet;
                    int length = msg.stuObject.stPicInfo.dwFileLenth;
                    imageSaveService.addEvent(new SavePicHandler(pBuffer.getByteArray(offset, length), length, eventInfo.getRelativeFolder(), eventInfo.getObjPicName()));
                }

                // 大图
                imageSaveService.addEvent(new SavePicHandler(eventInfo.getImagesData(), dwBufSize, eventInfo.getRelativeFolder(), eventInfo.getPicName()));
                break;
            }
            default: {
                System.out.printf("Other Event Received with Event Code of 0x%x\n", dwAlarmType);
                break;
            }
        }
        return 0;
    }

    // 保存图片的异步队列
    private static class SavePicHandler implements EventTaskHandler {

        private final byte[] imgBuffer;
        private final int length;
        private final String folder;
        private final String fileName;

        public SavePicHandler(byte[] imgBuffer, int dwBufSize, String folder, String fileName) {
            this.imgBuffer = imgBuffer;
            this.length = dwBufSize;
            this.folder = folder;
            this.fileName = fileName;
        }

        @Override
        public void eventCallBackProcess() {
            if (imgBuffer == null) return;

            // 创建文件夹
            String saveFolder = imageSaveRoot + File.separator + folder;
            File path = new File(saveFolder);
            if (!path.exists()) {
                if (!path.mkdir()) {
                    System.out.println("创建文件夹失败");
                }
            }
            // 文件路径
            String filePath = saveFolder + File.separator + fileName;
            System.out.println("保存图片中...路径：" + filePath);
            // 保存图片
            FileOutputStream fOutStream = null;
            try {
                fOutStream = new FileOutputStream(filePath);
                fOutStream.write(imgBuffer, 0, length);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fOutStream != null) fOutStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
