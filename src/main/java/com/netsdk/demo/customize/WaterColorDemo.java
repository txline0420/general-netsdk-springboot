package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_WATERCOLOR_DETECTION_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.NetSDKLib.EVENT_IVS_FLOATINGOBJECT_DETECTION;
import static com.netsdk.lib.NetSDKLib.EVENT_IVS_WATERCOLOR_DETECTION;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220505133  四川省生态环境厅企业固定污染源项目SDK报警接口开发
 * @date 2022/5/9 10:39
 */
public class WaterColorDemo extends Initialization {

    public static   NetSDKLib.LLong AttachHandle=new NetSDKLib.LLong(0);

    static   int ChannelId=0;
    /**
     * 订阅智能事件
     * @return
     */
    public static void AttachEventRealLoadPic() {

        //先关闭，再开启
        if(AttachHandle.intValue()!=0){
            DetachEventRealLoadPic();
        }
        int bNeedPicture = 1; // 是否需要图片

        AttachHandle =netSdk.CLIENT_RealLoadPictureEx(loginHandle, ChannelId, netSdk.EVENT_IVS_ALL,
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
                case EVENT_IVS_WATERCOLOR_DETECTION: { //水体颜色事件（对应 DEV_EVENT_WATERCOLOR_DETECTION_INFO）
                    System.out.println("水体颜色事件" );
                    DEV_EVENT_WATERCOLOR_DETECTION_INFO msg=new DEV_EVENT_WATERCOLOR_DETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    byte[] szName = msg.szName;
                    try {
                        System.out.println("szName UTF-8:"+new String(szName,"UTF-8"));
                        System.out.println("szName GBK:"+new String(szName,"GBK"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    // 智能事件所属大类 EM_CLASS_TYPE
                    int emEventType = msg.emClassType;
                    System.out.println("emEventType:"+emEventType);

                        //保存原始图
                    if (msg.stuOriginalImage.nLength > 0) {
                        String strFileName = picturePath + "\\" + System.currentTimeMillis() + "Original.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuOriginalImage.nOffSet, msg.stuOriginalImage.nLength, strFileName);
                    } else {
                        System.out.println("水体颜色事件原始图");
                    }

                    //保存球机变到最小倍下的抓图
                    if (msg.stuSceneImage.nLength > 0) {
                        String strFileName = picturePath + "\\" + System.currentTimeMillis() + "Scene.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, strFileName);
                    } else {
                        System.out.println("水体颜色事件球机变到最小倍下的抓图");
                    }
                    break;
                } case EVENT_IVS_FLOATINGOBJECT_DETECTION:{ // 漂浮物检测事件 (对应 DEV_EVENT_FLOATINGOBJECT_DETECTION_INFO)

                    System.out.println("漂浮物检测事件" );

                    NetSDKLib.DEV_EVENT_FLOATINGOBJECT_DETECTION_INFO   msg=new   NetSDKLib.DEV_EVENT_FLOATINGOBJECT_DETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    try {
                        System.out.println("漂浮物检测事件: 事件名称[" + new String(msg.szName, "GBK").trim() + "] 预置点名称[" + new String(msg.szPresetName, "GBK").trim() + "]");
                    } catch (Exception e) {
                        // Exception
                    }


                    //保存原始图
                    if (msg.stuOriginalImage.nLength > 0) {
                        String strFileName = picturePath + "\\" + System.currentTimeMillis() + "Original_漂浮物检测图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuOriginalImage.nOffSet, msg.stuOriginalImage.nLength, strFileName);
                    } else {
                        System.out.println("漂浮物检测事件无原始图");
                    }

                    //保存球机变到最小倍下的抓图
                    if (msg.stuSceneImage.nLength > 0) {
                        String strFileName = picturePath + "\\" + System.currentTimeMillis() + "Scene_漂浮物检测图.jpg";
                        ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, strFileName);
                    } else {
                        System.out.println("漂浮物检测事件无球机变到最小倍下的抓图");
                    }

                    int emDetectSenceType
                            = msg.emDetectSenceType;
                    // 四川省生态环境厅企业固定污染源项目, 用来区分是普通漂浮物场景还是泡沫检测场景 {@link com.netsdk.lib.enumeration.EM_FLOATINGOBJECT_DETECTION_SENCE_TYPE}

                    System.out.println("用来区分是普通漂浮物场景还是泡沫检测场景:"+emDetectSenceType);


                    break;
                }

                default:
                    System.out.println("其他事件--------------------"+ dwAlarmType);
                    break;
            }
            return 0;
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
        WaterColorDemo waterColorDemo=new WaterColorDemo();

        InitTest("172.11.1.109",37777,"admin","admin123");
        waterColorDemo.RunTest();
        LoginOut();

    }



}
