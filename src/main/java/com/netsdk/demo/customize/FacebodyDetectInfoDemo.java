package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Objects;

/**
 * @author 291189
 * @version 1.0
 * @description  商汤/人脸抓拍&识别摄像机SDK Java 库定制
 * @date 2021/8/24 14:04
 */
public class FacebodyDetectInfoDemo extends Initialization {


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

            /**
             EVENT_IVS_FACEBODY_DETECT 0x00000281 // 人像检测事件（对应 DEV_EVENT_FACEBODY_DETECT_INFO）
             #define EVENT_IVS_FACEBODY_ANALYSE 0x00000282 // 人像识别事件（对应 DEV_EVENT_FACEBODY_ANALYSE_INFO）



             */

            switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
                case EVENT_IVS_FACEBODY_DETECT : // 人像检测事件 DEV_EVENT_FACEBODY_DETECT_INFO
                    System.out.println("人像检测事件");

                    DEV_EVENT_FACEBODY_DETECT_INFO detect=new DEV_EVENT_FACEBODY_DETECT_INFO();


                    ToolKits.GetPointerData(pAlarmInfo, detect);

                        System.out.println("detect:"+detect);

                    FACE_TRAIT stuFaceTrait = detect.stuFaceTrait;// 人脸特征
                    System.out.println("stuFaceTrait:"+stuFaceTrait);

                    BODY_TRAIT stuBodyTrait = detect.stuBodyTrait;// 人体特征
                    System.out.println("stuBodyTrait:"+stuBodyTrait);

                    NetSDKLib.NET_EVENT_IMAGE_OFFSET_INFO stuBodyImage = detect.stuBodyImage;	// 人体图片信息
                    System.out.println("stuBodyImage:"+stuBodyImage);

                    NetSDKLib.NET_EVENT_IMAGE_OFFSET_INFO stuFaceImage = detect.stuFaceImage;	// 人脸图片信息
                    System.out.println("stuFaceImage:"+stuFaceImage);

                    NetSDKLib.NET_EVENT_IMAGE_OFFSET_INFO stuSceneImage = detect.stuSceneImage;// 全景图片信息
                    System.out.println("stuSceneImage:"+stuSceneImage);

                    OBJECT_RELATED_INFO stuFaceObject = detect.stuFaceObject;// 人脸关联信息
                    System.out.println("stuFaceObject:"+stuFaceObject);

                    OBJECT_RELATED_INFO stuBodyObject = detect.stuBodyObject;	// 人体关联信息
                    System.out.println("stuBodyObject:"+stuBodyObject);
                    break;

                case EVENT_IVS_FACEBODY_ANALYSE:{ // 人像识别事件（对应 DEV_EVENT_FACEBODY_ANALYSE_INFO）
                    System.out.println("人像识别事件");
                    DEV_EVENT_FACEBODY_ANALYSE_INFO analyse=new DEV_EVENT_FACEBODY_ANALYSE_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, analyse);
                    System.out.println("analyse:"+analyse);

                    FACE_TRAIT stuFaceTrait1 = analyse.stuFaceTrait;// 人脸特征
                    System.out.println("stuFaceTrait:"+stuFaceTrait1);
                    BODY_TRAIT stuBodyTrait1 = analyse.stuBodyTrait;// 人体特征
                    System.out.println("stuBodyTrait1:"+stuBodyTrait1);

                    NetSDKLib.NET_EVENT_IMAGE_OFFSET_INFO stuBodyImage1 = analyse.stuBodyImage;

                    //人体图片信息
                    if ( stuBodyImage1.nLength > 0) {
                        String bodyPicture = picturePath + "\\" + System.currentTimeMillis() + "body.jpg";
                        ToolKits.savePicture(pBuffer, stuBodyImage1.nOffSet, stuBodyImage1.nLength, bodyPicture);
                    }

                    NetSDKLib.NET_EVENT_IMAGE_OFFSET_INFO stuFaceImage1 = analyse.stuFaceImage;// 人脸图片信息

                    if ( stuFaceImage1.nLength > 0) {
                        String facePicture = picturePath + "\\" + System.currentTimeMillis() + "face.jpg";
                        ToolKits.savePicture(pBuffer, stuFaceImage1.nOffSet, stuFaceImage1.nLength, facePicture);
                    }
                    NetSDKLib.NET_EVENT_IMAGE_OFFSET_INFO stuSceneImage1 = analyse.stuSceneImage;// 全景图片信息

                    if ( stuSceneImage1.nLength > 0) {
                        String sencePicture = picturePath + "\\" + System.currentTimeMillis() + "sence.jpg";
                        ToolKits.savePicture(pBuffer, stuSceneImage1.nOffSet, stuSceneImage1.nLength, sencePicture);
                    }


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
        FacebodyDetectInfoDemo facebodyDetectInfoDemo=new FacebodyDetectInfoDemo();

        Initialization.InitTest("172.29.5.1", 37777, "admin", "admin123");

        facebodyDetectInfoDemo.RunTest();

        Initialization.LoginOut();
    }


}
