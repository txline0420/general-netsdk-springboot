package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_TRAFFIC_DRIVER_ABNORMAL_INFO;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description ERR211022043 周口永胜机动车驾驶员培训有限公司车载设备MDJ5100需定制java版本SDK开发包封装（驾驶员身份异常报警事件）需求
 * @date 2021/11/4 9:22
 */
public class TrafficDriverAbnormalDemo  extends Initialization {

    int channel=-1;
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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_TRAFFIC_DRIVER_ABNORMAL, bNeedPicture, AnalyzerDataCB.getInstance(), null, null);
        // attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, AnalyzerDataCB.getInstance(), null, null);

        if (attachHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
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

        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }


            switch (dwAlarmType) {
                case NetSDKLib.EVENT_IVS_TRAFFIC_DRIVER_ABNORMAL: //   驾驶员异常报警事件(对应 DEV_EVENT_TRAFFIC_DRIVER_ABNORMAL_INFO)


                    DEV_EVENT_TRAFFIC_DRIVER_ABNORMAL_INFO msg=new DEV_EVENT_TRAFFIC_DRIVER_ABNORMAL_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    /** 通道号*/
                    int nChannelID = msg.nChannelID;

                    System.out.println("nChannelID:"+nChannelID);

                    /** 事件动作, 0表示脉冲事件, 1表示持续性事件开始, 2表示持续性事件结束*/
                    int nAction = msg.nAction;
                    System.out.println("nAction:"+nAction);

                    /** GPS信息*/
                    NetSDKLib.NET_GPS_STATUS_INFO stuGPSStatusInfo = msg.stuGPSStatusInfo;
                        //定位时间
                        NetSDKLib.NET_TIME revTime = stuGPSStatusInfo.revTime;

                        System.out.println("revTime:"+revTime);

                    byte[] dvrSerial = stuGPSStatusInfo.DvrSerial;

                    String serial = new String(dvrSerial);// 设备序列号
                             System.out.println("serial:"+serial);

                    double longitude = stuGPSStatusInfo.longitude;//经度(单位是百万分之度,范围0-360度)
                    System.out.println("longitude:"+longitude);

                    double  latidude = stuGPSStatusInfo.latidude; // 纬度(单位是百万分之度,范围0-180度)
                    System.out.println("latidude:"+latidude);

                    double height=stuGPSStatusInfo.height;        // 高度(米)
                    System.out.println("height:"+height);

                    double  angle=stuGPSStatusInfo.angle;         // 方向角(正北方向为原点,顺时针为正)
                    System.out.println("angle:"+angle);

                    double   speed=stuGPSStatusInfo.speed;       // 速度(单位km/H)
                    System.out.println("speed:"+speed);

                    /** 事件发生的时间*/
                     NET_TIME_EX UTC= msg.UTC;
                    System.out.println("UTC:"+UTC);
                    /**  违章关联视频FTP上传路径 */
                    try {
                        String videoPath = new String(msg.szVideoPath, "GBK");

                        System.out.println("videoPath:"+videoPath);

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }


                    break;
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


    /**
     * 加载测试内容
     */
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "订阅智能事件", "AttachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "停止侦听智能事件", "DetachEventRealLoadPic"));
        menu.run();
    }

    public static void main(String[] args) {


        Initialization.InitTest("10.35.221.232", 37777, "admin", "admin123");
        new TrafficDriverAbnormalDemo().RunTest();
        Initialization.LoginOut();
    }
}
