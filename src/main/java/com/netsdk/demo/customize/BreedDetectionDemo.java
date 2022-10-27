package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_BREED_DETECTION_INFO;
import com.netsdk.lib.structure.NET_VAOBJECT_ANIMAL_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220413169 智慧养殖产业化平台+DH-NVR504-I+对接第三方平台
 * @date 2022/4/15 15:18
 */
public class BreedDetectionDemo extends Initialization {

    private static String ipAddr = "172.11.1.113";
    private static int port = 37777;
    private static String user = "admin";
    private static String password = "admin123";

    int channel=-1;
    NetSDKLib.LLong    attachHandle=new NetSDKLib.LLong(0);


    public NetSDKLib.LLong AttachEventRealLoadPic() {
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        if (attachHandle.longValue() != 0) {
            this.DetachEventRealLoadPic();
        }

        // 需要图片
        int bNeedPicture = 1;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_BREED_DETECTION, bNeedPicture,
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
                case NetSDKLib.EVENT_IVS_BREED_DETECTION : {
                    System.out.println("智慧养殖检测事件");
                    //智慧养殖检测事件 (对应 DEV_EVENT_BREED_DETECTION_INFO)
                    DEV_EVENT_BREED_DETECTION_INFO msg=new DEV_EVENT_BREED_DETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    //通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID："+nChannelID);
                    //0:脉冲 1:开始 2:停止
                    int nAction = msg.nAction;
                    System.out.println("nAction："+nAction);
                    //事件名称
                    byte[] szName = msg.szName;
                    try {
                        System.out.println("szName utf-8："+new String(szName,"utf-8"));
                        System.out.println("szName gbk："+new String(szName,"gbk"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
//  事件发生的时间
                    System.out.println("UTC："+msg.UTC);
// 事件ID
                    int nEventID = msg.nEventID;
                    System.out.println("事件ID："+nEventID);
//智能事件所属大类 {@link com.netsdk.lib.NetSDKLib.EM_CLASS_TYPE}
                    System.out.println("emClassType："+msg.emClassType);
//全景广角图
                    NetSDKLib.SCENE_IMAGE_INFO_EX stuSceneImage = msg.stuSceneImage;

                    if ( stuSceneImage.nLength > 0) {
                        String senceExPicture = picturePath + "\\" + System.currentTimeMillis() + "senceEx.jpg";
                        ToolKits.savePicture(pBuffer, stuSceneImage.nOffSet, stuSceneImage.nLength, senceExPicture);
                    }
                    //检测到的物体个数
                    int nObjectNum = msg.nObjectNum;
                    System.out.println("nObjectNum:"+nObjectNum);
//检测到的物体信息
                    NET_VAOBJECT_ANIMAL_INFO[] stuObjects
                            = msg.stuObjects;

                    for (int i=0;i<nObjectNum;i++){
                        NET_VAOBJECT_ANIMAL_INFO stuObject = stuObjects[i];
//物体ID，每个ID表示一个唯一的物体
                        System.out.println("nObjectID："+stuObject.nObjectID);
//智慧养殖检测目标子类型 {@link com.netsdk.lib.enumeration.EM_BREED_DETECT_CATEGORY_TYPE}
                        System.out.println("emCategoryType："+stuObject.emCategoryType);
//对象重量, 单位: g
                        System.out.println("nObjectWeight："+stuObject.nObjectWeight);

                        NetSDKLib.SCENE_IMAGE_INFO_EX stuImageData = stuObject.stuImageData;
                            System.out.println("stuImageData.nLength:"+stuImageData.nLength);
                        System.out.println("stuImageData.szFilePath:"+new String(stuImageData.szFilePath));

                        if ( stuImageData.nLength > 0) {
                            String sencePicture = picturePath + "\\" + System.currentTimeMillis() + "sence.jpg";
                            ToolKits.savePicture(pBuffer, stuImageData.nOffSet, stuImageData.nLength, sencePicture);
                        }
                    }


                    break;
                } default:
                    System.out.println("其他事件："+dwAlarmType);
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
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this , "AttachEventRealLoadPic" , "AttachEventRealLoadPic")));
        menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));
        menu.run();
    }

    public static void main(String[] args){
        InitTest(ipAddr, port, user, password);
        BreedDetectionDemo breedDetectionDemo=new BreedDetectionDemo();
        breedDetectionDemo.RunTest();
        LoginOut();
    }

}
