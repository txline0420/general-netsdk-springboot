package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_IMAGE_INFO_EX2;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220121043 巴中市巴州区交警违停需求：定制Java SDK进行测试，测试预违停功能
 * @date 2022/1/25 9:44
 */
public class CityMotorparkingDemo extends Initialization {

    int channel=0;
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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
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
                case NetSDKLib.EVENT_IVS_CITY_MOTORPARKING : {//城市机动车违停事件(对应 DEV_EVENT_CITY_MOTORPARKING_INFO)
                    NetSDKLib.DEV_EVENT_CITY_MOTORPARKING_INFO msg = new NetSDKLib.DEV_EVENT_CITY_MOTORPARKING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    //  是否为违规预警图片(预警触发后一定时间，违规物体还没有离开，才判定为违规)，参考com.netsdk.lib.enumeration.EM_PREALARM
                    int emPreAlarm = msg.emPreAlarm;

                    System.out.println("城市机动车违停事件 emPreAlarm:"+emPreAlarm);

                    int nImageInfoNum = msg.nImageInfoNum;

                    NET_IMAGE_INFO_EX2[] stuImageInfo
                            = msg.stuImageInfo;


                    for(int i=0;i>nImageInfoNum;i++){
                        NET_IMAGE_INFO_EX2 image= stuImageInfo[i];
                        if (image != null && image.nLength > 0) {
                            String picture = picturePath + "/" + System.currentTimeMillis() + "related.jpg";
                            ToolKits.savePicture(pBuffer, image.nOffset, image.nLength, picture);
                        }

                    }

                    System.out.println(" 城市机动车违停事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 是否为违规预警图片:"
                            + msg.emPreAlarm + " 事件ID:" + msg.nEventID);
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKING: ///< 交通违章-违章停车
                {
                    NetSDKLib.DEV_EVENT_TRAFFIC_PARKING_INFO msg = new NetSDKLib.DEV_EVENT_TRAFFIC_PARKING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    //  是否为违规预警图片(预警触发后一定时间，违规物体还没有离开，才判定为违规)，参考com.netsdk.lib.enumeration.EM_PREALARM
                    int emPreAlarm = msg.byPreAlarm;

                    System.out.println(" 交通违章停车 emPreAlarm:"+emPreAlarm);

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
        CityMotorparkingDemo cityMotorparkingDemo=new CityMotorparkingDemo();
        InitTest("172.31.8.189",37777,"admin","admin123");
        cityMotorparkingDemo.RunTest();
        LoginOut();

    }

}
