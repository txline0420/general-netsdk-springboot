package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_BARELAND_DETECTION_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;


/**
 * @author 291189
 * @version 1.0
 * @description  ERR211009112 金虎/郑州市智慧工地
 * @date 2021/10/13 9:16
 */
public class BarelandDetectionDemo extends Initialization {



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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_BARELAND_DETECTION, bNeedPicture, AnalyzerDataCB.getInstance(), null, null);
    //    attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, AnalyzerDataCB.getInstance(), null, null);

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
                case NetSDKLib.EVENT_IVS_BARELAND_DETECTION: //  裸土检测事件(DEV_EVENT_BARELAND_DETECTION_INFO)

                    DEV_EVENT_BARELAND_DETECTION_INFO msg=new DEV_EVENT_BARELAND_DETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    /** 事件名称*/
                    byte[] szName = msg.szName;

                    System.out.println("szName:"+new String(szName));

                    /** 智能事件所属大类*/
                    byte[] szClass = msg.szClass;
                    System.out.println("szClass:"+new String(szClass));

                    /** GroupID事件组ID，同一物体抓拍过程内GroupID相同*/
                    int nGroupID = msg.nGroupID;
                    System.out.println("nGroupID:"+nGroupID);

                    /** CountInGroup一个事件组内的抓拍张数*/
                    int nCountInGroup = msg.nCountInGroup;
                    System.out.println("nCountInGroup:"+nCountInGroup);

                    /** IndexInGroup一个事件组内的抓拍序号，从1开始*/
                    int nIndexInGroup = msg.nIndexInGroup;
                    System.out.println("nIndexInGroup:"+nIndexInGroup);

                    /** 事件发生时间，带时区偏差的UTC时间，单位：秒*/
                    int nUTC = msg.nUTC;
                    System.out.println("nUTC:"+nUTC);

                    /** 相对事件时间戳，单位毫秒*/
                    double dbPTS = msg.dbPTS;
                    System.out.println("dbPTS:"+dbPTS);

                    /** 事件时间毫秒数*/
                    int 	nUTCMS=msg.nUTCMS;
                    System.out.println("nUTCMS:"+nUTCMS);

                    /** 事件编号，用来唯一标志一个事件*/
                   	int		nEventID=msg.nEventID;
                    System.out.println("nEventID:"+nEventID);

                    /** 裸土实际占比，取值0~100*/
                	int					nRatio=msg.nRatio;
                    System.out.println("nRatio:"+nRatio);

                    /** 全景广角图*/
                     NetSDKLib.SCENE_IMAGE_INFO stuSceneImage=msg.stuSceneImage;

                    if ( stuSceneImage.nLength > 0) {
                        String sencePicture = picturePath + "\\" + System.currentTimeMillis() + "sence.jpg";
                        ToolKits.savePicture(pBuffer, stuSceneImage.nOffSet, stuSceneImage.nLength, sencePicture);
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
        Initialization.InitTest("172.10.250.247", 37777, "admin", "admin123");

       new BarelandDetectionDemo().RunTest();
        Initialization.LoginOut();
    }
}
