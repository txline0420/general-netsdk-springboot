package com.netsdk.demo.customize;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_CLASS_TYPE;
import com.netsdk.lib.enumeration.EM_DETECTION_SCENE_TYPE;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.enumeration.EM_EVENT_TYPE;
import com.netsdk.lib.structure.DEV_EVENT_ANIMAL_DETECTION_INFO;
import com.netsdk.lib.structure.NET_ANIMAL_SCENE_IMAGE_INFO;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Date;
import java.util.Objects;

public class AnimalDetectionDemo extends Initialization {

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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, EM_EVENT_TYPE.EVENT_IVS_ANIMAL_DETECTION.getType(), bNeedPicture,
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
        int count=0;

        @Override
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }

            switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
                case EVENT_IVS_ANIMAL_DETECTION : {
                    System.out.println("动物检测事件");

                    count++;
                    DEV_EVENT_ANIMAL_DETECTION_INFO msg = new DEV_EVENT_ANIMAL_DETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName:" + new String(szName));
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //UTC时间
                    NET_TIME_EX UTC = msg.UTC;
                    System.out.println("UTC:" + UTC.toStringTime());
                    //所属大类
                    int emClassType = msg.emClassType;
                    System.out.println("emClassType:" + emClassType);
                    //事件id
                    int nEventID = msg.nEventID;
                    System.out.println("nEventID:" + nEventID);
                    //智能事件规则编号
                    int nRuleID = msg.nRuleID;
                    System.out.println("nRuleID:" + nRuleID);
                    //智能事件规则编号
                    int Sequence = msg.nSequence;
                    System.out.println("nSequence:" + Sequence);
                    //全景广角图
                    int picSizes = 0;
                    NET_ANIMAL_SCENE_IMAGE_INFO stuSceneImage = msg.stuSceneImage;
                    System.out.println("stuImageInfo:" + stuSceneImage.toString());
                    //图片
                    if (stuSceneImage != null && stuSceneImage.nLength > 0) {
                        String picture = picturePath + "/" + System.currentTimeMillis() + "related.jpg";
                        ToolKits.savePicture(pBuffer, stuSceneImage.nOffSet, stuSceneImage.nLength, picture);
                    }
                    //动物总数
                    int nAnimalsAmount = msg.stuObjectsStatistics.nAnimalsAmount;
                    System.out.println("nAnimalsAmount:" + nAnimalsAmount);
                    //智能事件所属大类
                    int ClassType = msg.emClassType;
                    System.out.println("emClassType:" + EM_CLASS_TYPE.getNoteByValue(ClassType));
                    //动物检测规则下的场景类型
                    int emDetectionSceneType = msg.emDetectionSceneType;
                    System.out.println("emDetectionSceneType:" + EM_DETECTION_SCENE_TYPE.getNoteByValue(emDetectionSceneType));
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
        AnimalDetectionDemo AnimalDetectionDemo=new AnimalDetectionDemo();
        InitTest("20.2.36.32",37777,"admin","admin123");
        AnimalDetectionDemo.RunTest();
        LoginOut();

    }

}
