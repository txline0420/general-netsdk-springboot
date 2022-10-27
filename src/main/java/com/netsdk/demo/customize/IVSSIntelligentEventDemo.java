package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220210123 IVSS支持的后智能事件需要支持linux64/java的SDK
 * @date 2022/2/15 16:11
 */
public class IVSSIntelligentEventDemo extends Initialization {

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
                case NetSDKLib.EVENT_IVS_CROSSLINEDETECTION_EX  : { //警戒线扩展事件(对应 DEV_EVENT_CROSSLINE_INFO_EX)
                    System.out.println("警戒线扩展事件");

                    DEV_EVENT_CROSSLINE_INFO_EX msg = new DEV_EVENT_CROSSLINE_INFO_EX();

                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    //智能事件所属大类， 取值为  EM_CLASS_TYPE 中的值
                    int emClassType
                            = msg.stuIntelliCommInfo.emClassType;
                    System.out.println("emClassType :"+emClassType);


                    //检测到的物体
                    NetSDKLib.NET_MSG_OBJECT_EX stuObject = msg.stuObject;
                    //物体类型
                    byte[] szObjectType = stuObject.szObjectType;

                    //物体子类别,根据不同的物体类型
                    byte[] szObjectSubType = stuObject.szObjectSubType;

                    try {
                        System.out.println("szObjectType UTF-8:"+new String(szObjectType,"UTF-8"));
                        System.out.println("szObjectType GBK:"+new String(szObjectType,"GBK"));

                        System.out.println("szObjectSubType UTF-8:"+new String(szObjectSubType,"UTF-8"));
                        System.out.println("szObjectSubType GBK:"+new String(szObjectSubType,"GBK"));


                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                }



                case NetSDKLib.EVENT_IVS_CROSSLINEDETECTION  : { //警戒线事件(对应 DEV_EVENT_CROSSLINE_INFO)
                    System.out.println("警戒线事件");

                    NetSDKLib.DEV_EVENT_CROSSLINE_INFO msg = new NetSDKLib.DEV_EVENT_CROSSLINE_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);


                    //智能事件所属大类， 取值为  EM_CLASS_TYPE 中的值
                    int emClassType
                            = msg.stuIntelliCommInfo.emClassType;
                    System.out.println("emClassType :"+emClassType);


                    //检测到的物体
                    NetSDKLib.NET_MSG_OBJECT stuObject = msg.stuObject;
                    //物体类型
                    byte[] szObjectType = stuObject.szObjectType;

                    //物体子类别,根据不同的物体类型
                    byte[] szObjectSubType = stuObject.szObjectSubType;


                    try {
                        System.out.println("szObjectType UTF-8:"+new String(szObjectType,"UTF-8"));
                        System.out.println("szObjectType GBK:"+new String(szObjectType,"GBK"));

                        System.out.println("szObjectSubType UTF-8:"+new String(szObjectSubType,"UTF-8"));
                        System.out.println("szObjectSubType GBK:"+new String(szObjectSubType,"GBK"));


                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                }

                case NetSDKLib.EVENT_IVS_CROSSREGIONDETECTION:{ //警戒区事件(对应 DEV_EVENT_CROSSREGION_INFO)
                    System.out.println("警戒区事件");

                    NetSDKLib.DEV_EVENT_CROSSREGION_INFO msg=new NetSDKLib.DEV_EVENT_CROSSREGION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    //智能事件所属大类， 取值为  EM_CLASS_TYPE 中的值
                    int emClassType
                            = msg.stuIntelliCommInfo.emClassType;
                    System.out.println("emClassType :"+emClassType);

                    //检测到的物体
                    NetSDKLib.NET_MSG_OBJECT stuObject = msg.stuObject;
                    //物体类型
                    byte[] szObjectType = stuObject.szObjectType;

                    //物体子类别,根据不同的物体类型
                    byte[] szObjectSubType = stuObject.szObjectSubType;

                    try {
                        System.out.println("szObjectType UTF-8:"+new String(szObjectType,"UTF-8"));
                        System.out.println("szObjectType GBK:"+new String(szObjectType,"GBK"));

                        System.out.println("szObjectSubType UTF-8:"+new String(szObjectSubType,"UTF-8"));
                        System.out.println("szObjectSubType GBK:"+new String(szObjectSubType,"GBK"));


                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case NetSDKLib.EVENT_IVS_PARKINGDETECTION:  {// 非法停车事件(对应 DEV_EVENT_PARKINGDETECTION_INFO)

                    System.out.println("非法停车事件");

                    NetSDKLib.DEV_EVENT_PARKINGDETECTION_INFO msg=new NetSDKLib.DEV_EVENT_PARKINGDETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    //智能事件所属大类， 取值为  EM_CLASS_TYPE 中的值
                    int emClassType
                            = msg.stuIntelliCommInfo.emClassType;
                    System.out.println("emClassType :"+emClassType);

                    //检测到的物体
                    NetSDKLib.NET_MSG_OBJECT stuObject = msg.stuObject;
                    //物体类型
                    byte[] szObjectType = stuObject.szObjectType;

                    //物体子类别,根据不同的物体类型
                    byte[] szObjectSubType = stuObject.szObjectSubType;


                    try {
                        System.out.println("szObjectType UTF-8:"+new String(szObjectType,"UTF-8"));
                        System.out.println("szObjectType GBK:"+new String(szObjectType,"GBK"));

                        System.out.println("szObjectSubType UTF-8:"+new String(szObjectSubType,"UTF-8"));
                        System.out.println("szObjectSubType GBK:"+new String(szObjectSubType,"GBK"));


                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case NetSDKLib.EVENT_IVS_BREAK_RULE_BUILDING_DETECTION:  {// 违章建筑检测事件(对应 DEV_EVENT_BREAK_RULE_BUILDIING_DETECTION_INFO)

                    System.out.println("违章建筑检测事件");

                   DEV_EVENT_BREAK_RULE_BUILDIING_DETECTION_INFO msg=new DEV_EVENT_BREAK_RULE_BUILDIING_DETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    byte[] szName = msg.szName;
                    try {
                    System.out.println("szName UTF-8:"+new String(szName,"UTF-8"));
                    System.out.println("szName GBK:"+new String(szName,"GBK"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case NetSDKLib.EVENT_IVS_FISHING_DETECTION:  {//钓鱼检测事件(对应 DEV_EVENT_FISHING_DETECTION_INFO )

                    System.out.println("钓鱼检测事件");

                    DEV_EVENT_FISHING_DETECTION_INFO msg=new DEV_EVENT_FISHING_DETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    byte[] szName = msg.szName;
                    try {
                        System.out.println("szName UTF-8:"+new String(szName,"UTF-8"));
                        System.out.println("szName GBK:"+new String(szName,"GBK"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    //检测目标物体的个数
                    int nObjectCount = msg.nObjectCount;

                    NetSDKLib.NET_MSG_OBJECT_EX2[] stuObjects
                            = msg.stuObjects;
                        for(int i=0;i<nObjectCount;i++){
                            NetSDKLib.NET_MSG_OBJECT_EX2 stuObject
                                    = stuObjects[i];
                            NetSDKLib.NET_PIC_INFO stPicInfo
                                    = stuObject.stPicInfo;
                            //图片
                            if (stPicInfo != null && stPicInfo.dwFileLenth > 0) {
                                String picture = picturePath + "/" + System.currentTimeMillis() + "related.jpg";
                                ToolKits.savePicture(pBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, picture);
                            }
                        }

                    break;
                }
                case NetSDKLib.EVENT_IVS_WATER_LEVEL_DETECTION:  {// 水位检测事件 (对应 DEV_EVENT_WATER_LEVEL_DETECTION_INFO)

                    System.out.println("水位检测事件");
                    NetSDKLib.DEV_EVENT_WATER_LEVEL_DETECTION_INFO msg=new NetSDKLib.DEV_EVENT_WATER_LEVEL_DETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);


                    byte[] szName = msg.szName;
                    try {
                        System.out.println("szName UTF-8:"+new String(szName,"UTF-8"));
                        System.out.println("szName GBK:"+new String(szName,"GBK"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    // 事件数据类型,详见NET_EM_EVENT_DATA_TYPE
                    int emEventType = msg.emEventType;
                    System.out.println("emEventType:"+emEventType);

                    break;
                }
                case NetSDKLib.EVENT_IVS_SEWAGE_DETECTION:  {// 排污检测事件（对应 DEV_EVENT_SEWAGE_DETECTION_INFO)
                    System.out.println("排污检测事件");
                    DEV_EVENT_SEWAGE_DETECTION_INFO msg=new DEV_EVENT_SEWAGE_DETECTION_INFO();

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

                    break;
                }
                case NetSDKLib.EVENT_IVS_WATERCOLOR_DETECTION:  {// 水体颜色事件（对应 DEV_EVENT_WATERCOLOR_DETECTION_INFO）
                    System.out.println("水体颜色事件");
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
        IVSSIntelligentEventDemo ivssIntelligentEventDemo=new IVSSIntelligentEventDemo();
      //  InitTest("172.12.2.116",37777,"admin","admin123");
        //20.2.35.57
        InitTest("20.2.36.57",37777,"admin","admin123");
        ivssIntelligentEventDemo.RunTest();
        LoginOut();

    }

}
