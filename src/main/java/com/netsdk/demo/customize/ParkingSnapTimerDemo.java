package com.netsdk.demo.customize;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_PARKINGSPACE_MANUALSNAP_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;


/**
 * @author 291189
 * @version 1.0
 * @description  GIP220224028 秒图主动推送SDK协议定制
 * @date 2022/3/17 14:33
 */
public class ParkingSnapTimerDemo extends Initialization {

    private static String ipAddr = "172.27.1.55";
    private static int port = 37777;
    private static String user = "admin";
    private static String password = "admin123";

    int channel=0;
    NetSDKLib.LLong    attachHandle=new NetSDKLib.LLong(0);

    public NetSDKLib.LLong AttachEventRealLoadPic() {
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        if (attachHandle.longValue() != 0) {
            this.DetachEventRealLoadPic();
        }

        // 需要图片
        int bNeedPicture = 1;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACE_MANUALSNAP, bNeedPicture,
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
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }

            switch (dwAlarmType) {
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACE_MANUALSNAP : {// 路侧停车位手动抓图 (对应 DEV_EVENT_PARKINGSPACE_MANUALSNAP_INFO )

                    ParsingTrafficParkingSpaceManualSnap(pAlarmInfo, pBuffer, dwBufSize);
                    break;
                } default:
                    System.out.println("其他事件："+dwAlarmType);
                    break;

            }
            return 0;
        }

        private void ParsingTrafficParkingSpaceManualSnap(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize) {

            System.out.println("<<------路侧停车位手动抓图------>>");

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

                    String picture = picturePath + "/" + System.currentTimeMillis() + "ParkingSpaceManualSnap.jpg";
                    ToolKits.savePicture(pBuffer, 0, dwBufSize, picture);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
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
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this , "AttachEventRealLoadPic" , "AttachEventRealLoadPic")));
        menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));
        menu.run();
    }

    public static void main(String[] args){
        InitTest(ipAddr, port, user, password);
        ParkingSnapTimerDemo scd=new ParkingSnapTimerDemo();
        scd.RunTest();
        LoginOut();
    }
}
