package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.UUID;

/**
 * @author 291189
 * @version 1.0
 * @description ERR211214075 峨眉山景区卡口相机定制
 * @date 2021/12/29 15:28
 */
public class TrafficCarDemo extends Initialization {


    public static   NetSDKLib.LLong AttachHandle=new NetSDKLib.LLong(0);

     static   int ChannelId=-1;
    /**
     * 订阅智能事件
     * @return
     */
    public static void AttachEventRealLoadPic() {
        /**
         * 说明：
         * 	通道数可以在有登录是返回的信息 m_stDeviceInfo.byChanNum 获取
         */
        //先关闭，再开启
        if(AttachHandle.intValue()!=0){
            DetachEventRealLoadPic();
        }
        int bNeedPicture = 1; // 是否需要图片

      AttachHandle =netSdk.CLIENT_RealLoadPictureEx(loginHandle, ChannelId, netSdk.EVENT_IVS_TRAFFICJUNCTION,
                bNeedPicture , AnalyzerDataCB.getInstance() , null , null);
        if( AttachHandle.longValue() != 0  ) {
            System.out.println("CLIENT_RealLoadPictureEx Success  ChannelId : \n" + ChannelId);
        } else {

            throw  new RuntimeException("CLIENT_RealLoadPictureEx Failed!" + ToolKits.getErrorCode());
        }

    }
    /**
     * 停止侦听智能事件
     */
    public static void DetachEventRealLoadPic() {
        if (AttachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(AttachHandle);
        }
    }
    /** 写成静态主要是防止被回收 */
    private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {

        private String m_imagePath;
        NetSDKLib.NET_MSG_OBJECT m_stuObject; 	// 物体信息
        NetSDKLib.NET_TIME_EX utc; // 事件时间

        String EventMsg; // 事件信息

        String bigPicture; // 大图
        String smallPicture; // 小图

        private AnalyzerDataCB() {
            m_imagePath = "./PlateNumber/";
            File path = new File(m_imagePath);
            if (!path.exists()) {
                path.mkdir();
            }
        }

        private static class AnalyzerDataCBHolder {
            private static final AnalyzerDataCB instance = new AnalyzerDataCB();
        }

        public static AnalyzerDataCB getInstance() {

            return AnalyzerDataCBHolder.instance;
        }

        public int invoke(
                NetSDKLib.LLong lAnalyzerHandle,
                int dwAlarmType,
                Pointer pAlarmInfo,
                Pointer pBuffer,
                int dwBufSize,
                Pointer dwUser,
                int nSequence,
                Pointer reserved) {
            if (lAnalyzerHandle.longValue() == 0 || pAlarmInfo == null) {
                return -1;
            }

            switch (dwAlarmType) {

                case NetSDKLib.EVENT_IVS_TRAFFICJUNCTION: // 交通路口事件----老规则(对应 DEV_EVENT_TRAFFICJUNCTION_INFO)
                {
                    NetSDKLib.DEV_EVENT_TRAFFICJUNCTION_INFO msg = new NetSDKLib.DEV_EVENT_TRAFFICJUNCTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    m_stuObject = msg.stuObject;
                    utc = msg.UTC;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "路口事件";

                    NetSDKLib.DEV_EVENT_TRAFFIC_TRAFFICCAR_INFO stTrafficCar
                            = msg.stTrafficCar;

                    //NetSDKLib.EM_VEHICLE_DIRECTION
                    int emFlowDirection
                            = stTrafficCar.emFlowDirection;

                    System.out.println("车流量方向:"+emFlowDirection);
                    // 保存图片
                    SavePlatePic(m_stuObject, pBuffer, dwBufSize);

                    break;
                }
                default:
                    System.out.println("其他事件：" + dwAlarmType);
                    break;
            }

            return 0;
        }


        // 2014年后，陆续有设备版本，支持单独传车牌小图，小图附录在pBuffer后面。
        private void SavePlatePic(NetSDKLib.NET_MSG_OBJECT stuObject, Pointer pBuffer, int dwBufSize) {

            // 保存大图
            if (pBuffer != null && dwBufSize > 0) {
                bigPicture = m_imagePath + "Big_" + UUID.randomUUID().toString() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, bigPicture);
            }

            // 保存小图
            if (stuObject == null) {
                return;
            }
            if (stuObject.bPicEnble == 1) {
                //根据pBuffer中数据偏移保存小图图片文件
                int picLength = stuObject.stPicInfo.dwFileLenth;
                if (picLength > 0) {
                    smallPicture = m_imagePath + "small_" + UUID.randomUUID().toString() + ".jpg";
                    ToolKits.savePicture(pBuffer, stuObject.stPicInfo.dwOffSet, picLength, smallPicture);
                }
            }
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
        TrafficCarDemo trafficCarDemo=new TrafficCarDemo();
        //10.11.16.216   172.24.2.7  172.24.1.243
        InitTest("10.34.3.219",37777,"admin","admin");
        trafficCarDemo.RunTest();
        LoginOut();

    }



}
