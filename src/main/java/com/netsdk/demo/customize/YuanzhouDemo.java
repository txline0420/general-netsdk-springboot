package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_OBJECT_PLACEMENT_DETECTION_INFO;
import com.netsdk.lib.structure.DEV_EVENT_OBJECT_REMOVAL_DETECTION_INFO;
import com.netsdk.lib.structure.DEV_EVENT_PRESERVATION_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;


/**
 * @author 291189
 * @version 1.0
 * @description  ERR220401187 远州+netsdk+java封装
 * @date 2022/4/7 11:21
 */
public class YuanzhouDemo extends Initialization {
    int channel=3;
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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel,NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
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

                case NetSDKLib.EVENT_IVS_SMOKING_DETECT : {    // 吸烟检测事件(对应 DEV_EVENT_SMOKING_DETECT_INFO)
                    System.out.printf("吸烟检测事件");
                    NetSDKLib.DEV_EVENT_SMOKING_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_SMOKING_DETECT_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    String Picture = picturePath + "\\" + "smoking_" + System.currentTimeMillis() + ".jpg";



                    if (dwBufSize > 0) {
                        ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                    }
                    System.out.println("吸烟检测事件 时间(UTC)：" + msg.UTC + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:"
                            + msg.stuObject.stuEndTime);
                    break;
                }
                case NetSDKLib.EVENT_IVS_OBJECT_REMOVAL_DETECTION: {        // 物品拿取检测事件(对应DEV_EVENT_OBJECT_REMOVAL_DETECTION_INFO)
                    System.out.printf("物品拿取检测事件");
                    DEV_EVENT_OBJECT_REMOVAL_DETECTION_INFO msg = new DEV_EVENT_OBJECT_REMOVAL_DETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    String Picture = picturePath + "\\" + "remove_" + System.currentTimeMillis() + ".jpg";
                    if (dwBufSize > 0) {
                        ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                    }
                    System.out.println("物品拿取检测事件 时间(UTC)：" + msg.UTC + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:"
                            + msg.stuObject.stuEndTime);
                    break;
                }
                case NetSDKLib.EVENT_IVS_OBJECT_PLACEMENT_DETECTION:{   // 物品放置检测事件(对应DEV_EVENT_OBJECT_PLACEMENT_DETECTION_INFO)
                    System.out.printf("物品放置检测事件");
                    DEV_EVENT_OBJECT_PLACEMENT_DETECTION_INFO msg=new DEV_EVENT_OBJECT_PLACEMENT_DETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);


                    String Picture = picturePath + "\\" + "placement_" + System.currentTimeMillis() + ".jpg";
                    if (dwBufSize > 0) {
                        ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                    }
                    System.out.println("物品放置检测事件 时间(UTC)：" + msg.UTC + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:"
                            + msg.stuObject.stuEndTime);
                    break;
                }
                case NetSDKLib.EVENT_IVS_PRESERVATION:{ // 物品保全事件(对应 DEV_EVENT_PRESERVATION_INFO)
                    System.out.printf("物品保全事件");
                    DEV_EVENT_PRESERVATION_INFO msg=new DEV_EVENT_PRESERVATION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    String Picture = picturePath + "\\" + "preservation_" + System.currentTimeMillis() + ".jpg";
                    if (dwBufSize > 0) {
                        ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                    }
                    System.out.println("物品保全事件 时间(UTC)：" + msg.UTC + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:"
                            + msg.stuObject.stuEndTime);

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
        YuanzhouDemo yuanzhouDemo=new YuanzhouDemo();
        InitTest("172.12.66.245",37777,"admin","admin123");
        yuanzhouDemo.RunTest();
        LoginOut();

    }

}
