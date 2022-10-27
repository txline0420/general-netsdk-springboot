package com.netsdk.demo.customize.XRayDetection;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * @author 291189
 * @version 1.0
 * @description GIP210826019 开包工作台接入安检机SDK配套开发
 * @date 2021/9/9 11:07
 */
public class XRayDetectionDemo extends Initialization {

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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENI_IVS_XRAY_DETECTION, bNeedPicture,
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
                case EVENI_IVS_XRAY_DETECTION : {
                    System.out.println("X光检测事件");

                    count++;
                    DEV_EVENT_XRAY_DETECTION_INFO msg = new DEV_EVENT_XRAY_DETECTION_INFO();

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
                    //危险等级
                    int emDangerGrade = msg.stuPacketInfo.emDangerGrade;
                    System.out.println("stuPacketInfo.emDangerGrade:" + emDangerGrade);
                    //主视角包裹内物品个数
                    int nObjectNum = msg.nObjectNum;
                    System.out.println("nObjectNum:" + nObjectNum);
                    // 主视角包裹内物品信息
                    for(int i = 0; i < nObjectNum; i ++){
                        NET_INSIDE_OBJECT stuInsideObj = msg.stuInsideObj[i];
                        System.out.println("stuInsideObj["+i+"]:" + stuInsideObj.toString());
                    }
                    //从视角包裹内物品个数
                    int nSlaveViewObjectNum = msg.nSlaveViewObjectNum;
                    System.out.println("nSlaveViewObjectNum:" + nSlaveViewObjectNum);
                    // 从视角包裹内物品信息
                    for(int i = 0; i < nSlaveViewObjectNum; i ++){
                        NET_INSIDE_OBJECT stuSlaveViewInsideObj = msg.stuSlaveViewInsideObj[i];
                        System.out.println("stuSlaveViewInsideObj["+i+"]:" + stuSlaveViewInsideObj.toString());
                    }
                    //图片数量
                    int nImageCount = msg.nImageCount;
                    System.out.println("nImageCount:" + nImageCount);
                    int picSizes = 0;
                    // 图片信息
                    for(int i = 0; i < nImageCount; i ++){
                        NET_XRAY_IMAGE_INFO stuImageInfo = msg.stuImageInfo[i];
                        System.out.println("stuImageInfo["+i+"]:" + stuImageInfo.toString());
                        //图片
                        if (stuImageInfo != null && stuImageInfo.nLength > 0) {

                            picSizes += stuImageInfo.nLength;

                            String picture = picturePath + "\\" + System.currentTimeMillis() + "related.jpg";
                            ToolKits.savePicture(pBuffer, stuImageInfo.nOffset, stuImageInfo.nLength, picture);
                        }
                    }

                    // 客户自定义信息个数
                    int nViewCustomInfoNum = msg.nViewCustomInfoNum;
                    System.out.println("nViewCustomInfoNum:" + nViewCustomInfoNum);
                    // 客户自定义信息, X光机定制专用
                    for(int i = 0; i < nViewCustomInfoNum; i ++){
                        NetSDKLib.NET_XRAY_CUSTOM_INFO stuViewCustomInfo = msg.stuViewCustomInfo[i];
                        System.out.println("stuViewCustomInfo["+i+"]:" + stuViewCustomInfo.toString());
                    }
                    // 包裹标识
                    String szPackageTag = Arrays.toString(msg.szPackageTag);
                    System.out.println("szPackageTag:" + szPackageTag);
                    // 包裹产生方式
                    int emPackageMode = msg.emPackageMode;
                    System.out.println("emPackageMode:" + emPackageMode);
                    // 关联图片数量
                    int nRelatedImageNum = msg.nRelatedImageNum;
                    System.out.println("nRelatedImageNum:" + nRelatedImageNum);
                    // 客户自定义信息, X光机定制专用
                    for(int i = 0; i < nRelatedImageNum; i ++){
                        NET_XRAY_RELATED_IMAGE_INFO stuRelatedImageInfo = msg.stuRelatedImageInfo[i];
                        System.out.println("stuRelatedImageInfo["+i+"]:" + stuRelatedImageInfo.toString());
                        //图片  stuRelatedImageInfo
                        if (stuRelatedImageInfo != null && stuRelatedImageInfo.nLength > 0) {

                            picSizes += stuRelatedImageInfo.nLength;

                            String picture = picturePath + "\\" + System.currentTimeMillis() + "related2.jpg";
                            ToolKits.savePicture(pBuffer, stuRelatedImageInfo.nOffset, stuRelatedImageInfo.nLength, picture);
                        }
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
    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "AttachEventRealLoadPic" , "AttachEventRealLoadPic")));
        menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));

        menu.run();
    }

    public static void main(String[] args) {
        XRayDetectionDemo XRayDetectionDemo=new XRayDetectionDemo();
        InitTest("10.35.232.160",37777,"admin","admin123");
        XRayDetectionDemo.RunTest();
        LoginOut();

    }

}