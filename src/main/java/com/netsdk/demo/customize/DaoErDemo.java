package com.netsdk.demo.customize;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_TRAFFIC_PARKINGSPACEOVERLINE_INFO;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Date;

/**
 * @author 291189
 * @version 1.0
 * @description ERR211018164 道尔-NETSDK--定制Java语言
 * @date 2021/10/26 13:57
 */
public class DaoErDemo extends Initialization {

    // 订阅句柄
    private static NetSDKLib.LLong AttachHandle = new NetSDKLib.LLong(0);
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
        //订阅所有事件 #define EVENT_IVS_TRAFFIC_PARKINGSPACEOVERLINE  0x00000134        // 车位压线事件(对应 DEV_EVENT_TRAFFIC_PARKINGSPACEOVERLINE_INFO)
        AttachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEOVERLINE , bNeedPicture, AnalyzerDataCB.getInstance(), null, null);
        if (AttachHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
                    ToolKits.getErrorCode());
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

    /**
     * 智能事件回调
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

            switch (dwAlarmType) {

                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEOVERLINE:

                    DEV_EVENT_TRAFFIC_PARKINGSPACEOVERLINE_INFO msg = new DEV_EVENT_TRAFFIC_PARKINGSPACEOVERLINE_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);


                    //通道号
                    int nChannelID = msg.nChannelID;

                    System.out.println("nChannelID:"+nChannelID);

                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName:"+new String(szName));

                    //时间戳(单位是毫秒)
                    int pts = msg.PTS;
                    System.out.println("pts:"+new Date(pts));

                    // 事件发生的时间
                    NET_TIME_EX utc = msg.UTC;
                    System.out.println("utc:"+utc);

                    //  事件ID
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:"+nEventID);

                    //对应车道号
                    int nLane = msg.nLane;
                    System.out.println("nLane:"+nLane);

                    //检测到的物体
                    NetSDKLib.NET_MSG_OBJECT stuObject = msg.stuObject;

                    System.out.println(stuObject.toString());
                  //  物体对应图片信息
                    NetSDKLib.NET_PIC_INFO stPicInfo = stuObject.stPicInfo;
                    System.out.println(" stPicInfo.dwFileLenth:"+ stPicInfo.dwFileLenth);
                    if ( stPicInfo.dwFileLenth > 0) {
                        String picture = picturePath + "\\" + System.currentTimeMillis() + "pic.jpg";
                        ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, picture);
                    }
                    /** 车身信息*/
                    NetSDKLib.NET_MSG_OBJECT stuVehicle = msg.stuVehicle;
                    System.out.println(stuObject.toString());
                    NetSDKLib.NET_PIC_INFO stPicInfo1 = stuVehicle.stPicInfo;
                    System.out.println(" stPicInfo1.dwFileLenth:"+ stPicInfo1.dwFileLenth);
                    if ( stPicInfo1.dwFileLenth > 0) {
                        String picture = picturePath + "\\" + System.currentTimeMillis() + "msg.jpg";
                        ToolKits.savePicture(pBuffer, stPicInfo1.dwOffSet, stPicInfo1.dwFileLenth, picture);
                    }
                    /** 事件对应文件信息*/
                    NetSDKLib.NET_EVENT_FILE_INFO stuFileInfo = msg.stuFileInfo;

                    System.out.println(stuFileInfo.toString());
                    /** 表示抓拍序号,如3,2,1,1表示抓拍结束,0表示异常结束*/
                    int nSequence1 = msg.nSequence;
                    System.out.println("nSequence1:"+nSequence1);

                    /** 事件动作,0表示脉冲事件,1表示持续性事件开始,2表示持续性事件结束;*/
                    byte byEventAction = msg.byEventAction;
                    System.out.println("byEventAction:"+byEventAction);

                    /** 图片的序号, 同一时间内(精确到秒)可能有多张图片, 从0开始*/
                    byte byImageIndex = msg.byImageIndex;
                    System.out.println("byImageIndex:"+byImageIndex);
                    /** 抓图标志(按位),具体见NET_RESERVED_COMMON*/
                    int dwSnapFlagMask = msg.dwSnapFlagMask;
                    System.out.println("dwSnapFlagMask:"+dwSnapFlagMask);
                    /** 对应图片的分辨率*/
                    NetSDKLib.NET_RESOLUTION_INFO stuResolution = msg.stuResolution;
                    System.out.println("stuResolution:"+stuResolution.toString());
                    /** 交通车辆信息*/
                    NetSDKLib.DEV_EVENT_TRAFFIC_TRAFFICCAR_INFO stuTrafficCar = msg.stuTrafficCar;
                    System.out.println("stuTrafficCar.szPlateNumber:"+new String(stuTrafficCar.szPlateNumber));
                    System.out.println("stuTrafficCar.szPlateType:"+new String(stuTrafficCar.szPlateType));
                    System.out.println("stuTrafficCar.szPlateColor:"+new String(stuTrafficCar.szPlateColor));

                    System.out.println("stuTrafficCar.szVehicleColor:"+new String(stuTrafficCar.szVehicleColor));

                    System.out.println("stuTrafficCar.nSpeed:"+stuTrafficCar.nSpeed);

                    System.out.println("stuTrafficCar.nVehicleSize:"+stuTrafficCar.nVehicleSize);

                    System.out.println("stuTrafficCar.fVehicleLength:"+stuTrafficCar.fVehicleLength);

                    System.out.println("stuTrafficCar.nSnapshotMode:"+stuTrafficCar.nSnapshotMode);


                    /** 停车场信息*/
                    NetSDKLib.DEV_TRAFFIC_PARKING_INFO stTrafficParingInfo = msg.stTrafficParingInfo;
                    System.out.println("nFeaturePicAreaPointNum:"+stTrafficParingInfo.nFeaturePicAreaPointNum);
                    /** 公共信息*/
                    NetSDKLib.EVENT_COMM_INFO stCommInfo = msg.stCommInfo;

                    System.out.println("stCommInfo.szCountry:"+new String(stCommInfo.szCountry));

                    // 二值化车牌抠图
                    NetSDKLib.EVENT_PIC_INFO stuBinarizedPlateInfo = stCommInfo.stuBinarizedPlateInfo;
                    System.out.println("stuBinarizedPlateInfo.nOffset:"+stuBinarizedPlateInfo.nOffset);
                    if ( stuBinarizedPlateInfo.nOffset > 0) {
                        String picture = picturePath + "\\" + System.currentTimeMillis() + "pla.jpg";
                        ToolKits.savePicture(pBuffer, stuBinarizedPlateInfo.nOffset, stuBinarizedPlateInfo.nLength, picture);
                    }
                    // 车身特写抠图
                    NetSDKLib.EVENT_PIC_INFO stuVehicleBodyInfo = stCommInfo.stuVehicleBodyInfo;
                    System.out.println("stuVehicleBodyInfo.nOffset:"+stuVehicleBodyInfo.nOffset);
                    if ( stuVehicleBodyInfo.nOffset > 0) {
                        String picture = picturePath + "\\" + System.currentTimeMillis() + "bodyInfo.jpg";
                        ToolKits.savePicture(pBuffer, stuVehicleBodyInfo.nOffset, stuVehicleBodyInfo.nLength, picture);
                    }





                    break;
                default:
                    System.out.println("其他事件--------------------" + dwAlarmType);
                    break;
            }
            return 0;
        }
    }

    public  void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this , "订阅智能任务" , "attachIVSEvent"));
        menu.addItem(new CaseMenu.Item(this , "停止侦听智能事件" , "DetachEventRealLoadPic"));
        menu.run();
    }

    public static void main(String[] args) {
        InitTest("172.24.32.126", 37777, "admin", "admin123");
       new DaoErDemo().RunTest();

        LoginOut();
    }
}
