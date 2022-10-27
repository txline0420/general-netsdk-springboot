package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Date;
import java.util.Objects;

/**
 * @author 291189
 * @version 1.0
 * @description GIP210826019 开包工作台接入安检机SDK配套开发
 * @date 2021/9/9 11:07
 */
public class PackageOpeningWorkbenchAccessSecurityDemo extends Initialization {

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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_XRAY_UNPACKING_CHECK, bNeedPicture,
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
        int count=0;

        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }

            switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
                case EVENT_IVS_XRAY_UNPACKING_CHECK : {// X光开包检查事件 对应DEV_EVENT_XRAY_UPACKING_CHECK_INFO
                    System.out.println("X光开包检查事件");

                    count++;
                    DEV_EVENT_XRAY_UNPACKING_CHECK_INFO msg = new DEV_EVENT_XRAY_UNPACKING_CHECK_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    // 通道号
                    int nChannelID = msg.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);
                    //事件动作,0表示脉冲事件
                    int nAction = msg.nAction;
                    System.out.println("nAction:" + nAction);
                    //事件名称
                    byte[] szName = msg.szName;
                    System.out.println("szName:" + new String(szName));
                    //智能事件所属大类
                    byte[] szClass = msg.szClass;
                    System.out.println("szClass:" + new String(szClass));
                    //相对事件时间戳(单位是毫秒)
                    double pts = msg.PTS;
                    System.out.println("pts:" + new Date((long) pts));
                    //开包检查信息
                    UNPACKING_CHECK_INFO stuCheckInfo = msg.stuCheckInfo;
                    //事件发生时间，带时区偏差的UTC时间，单位秒
                    int nUTC = stuCheckInfo.nUTC;
                    System.out.println("nUTC:" + new Date(nUTC * 1000));
                    //事件时间毫秒数
                    int nUTCMS = stuCheckInfo.nUTCMS;
                    System.out.println("nUTCMS:" + new Date(nUTCMS));
                    //表示包裹标识，用来唯一标识一个包裹
                    byte[] szPackageTag = stuCheckInfo.szPackageTag;
                    System.out.println("szPackageTag:" + new String(szPackageTag));
                    //关联图片
                    RELATED_IMAGE[] stuRelatedImages = stuCheckInfo.stuRelatedImage;

                    int nRelatedImageRetNum = stuCheckInfo.nRelatedImageRetNum;


                    int picSizes = 0;

                    for (int i = 0; i < nRelatedImageRetNum; i++) {
                        RELATED_IMAGE stuRelatedImage = stuRelatedImages[i];
                        //图片
                        if (stuRelatedImage != null && stuRelatedImage.nLength > 0) {

                            picSizes += stuRelatedImage.nLength;

                            String picture = picturePath + "\\" + System.currentTimeMillis() + "related.jpg";
                            ToolKits.savePicture(pBuffer, stuRelatedImage.nOffset, stuRelatedImage.nLength, picture);
                        }

                    }

                    //身份信息
                    IDENTITY_INFO stuIdentityInfo = stuCheckInfo.stuIdentityInfo;
                    //法定姓名
                    byte[] szCitizenName = stuIdentityInfo.szCitizenName;
                    System.out.println("szCitizenName:" + new String(szCitizenName));
                    //身份证号18位
                    byte[] szCitizenIDNo = stuIdentityInfo.szCitizenIDNo;
                    System.out.println("szCitizenIDNo:" + new String(szCitizenIDNo));
                    //性别
                    int emSex = stuIdentityInfo.emSex;
                    System.out.println("emSex:" + emSex);
                    //年龄
                    int nAge = stuIdentityInfo.nAge;
                    System.out.println("nAge:" + nAge);

                    //违禁物品个数
                    int nContrabandRetNum
                            = stuCheckInfo.nContrabandRetNum;
                    System.out.println("nContrabandRetNum:" + nContrabandRetNum);

                    //  违禁物品分类  com.netsdk.lib.NetSDKLib.EM_INSIDE_OBJECT_TYPE*/
                    int[] emContrabandClass
                            = stuCheckInfo.emContrabandClass;

                    for (int i = 0; i < nContrabandRetNum; i++) {

                        System.out.println("emContrabandClass:" + emContrabandClass[i]);
                    }

                    //处理方式
                    int emProcessMode = stuCheckInfo.emProcessMode;
                    System.out.println("emProcessMode:" + emProcessMode);
                    //检出人，即开包人员
                    byte[] szChecker = stuCheckInfo.szChecker;
                    System.out.println("szChecker:" + new String(szChecker));
                    //报警地点
                    byte[] szAlarmLocation = stuCheckInfo.szAlarmLocation;
                    System.out.println("szAlarmLocation:" + new String(szAlarmLocation));
                    //报警时间（即拍摄违禁品时间），UTC时间，单位秒
                    int nAlarmTime = stuCheckInfo.nAlarmTime;
                    System.out.println("nAlarmTime:" + new Date(nAlarmTime * 1000));
                    //行程出发地
                    byte[] szPlaceOfDeparture = stuCheckInfo.szPlaceOfDeparture;
                    System.out.println("szPlaceOfDeparture:" + new String(szPlaceOfDeparture));
                    //行程目的地
                    byte[] szDestination = stuCheckInfo.szDestination;
                    System.out.println("szDestination:" + new String(szDestination));



                    // 开包工作台上报开包检查信息(测试使用)

                    if(count==1){//选取一条数据，作为测试：调用CLIENT_UploadUnpackingCheckInfo：该方法为 会将数据上报到 EVENT_IVS_XRAY_UNPACKING_CHECK事件中
                        NET_IN_UPLOAD_UPPACKING_CHECK_INFO input=new NET_IN_UPLOAD_UPPACKING_CHECK_INFO();

                        input.nImageDataLen=dwBufSize;

                        input.pImageData=pBuffer;

                        input.stuCheckInfo=stuCheckInfo;

                        ClientUploadUnpackingCheckInfo(input);
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
    //开包工作台上报开包检查信息
    public static void ClientUploadUnpackingCheckInfo(     NET_IN_UPLOAD_UPPACKING_CHECK_INFO input){

        Pointer pinput=new Memory(input.size());
        ToolKits.SetStructDataToPointer(input, pinput, 0);

        //出参
        NET_OUT_UPLOAD_UPPACKING_CHECK_INFO output=new NET_OUT_UPLOAD_UPPACKING_CHECK_INFO();
        Pointer poutput=new Memory(output.size());
        ToolKits.SetStructDataToPointer(output, poutput, 0);


        boolean isSucess
                = netSdk.CLIENT_UploadUnpackingCheckInfo(loginHandle, pinput, poutput, 5000);

        if(isSucess){
            System.out.println("ClientUploadUnpackingCheckInfo ok");
        }else {

            System.err.println("ClientUploadUnpackingCheckInfo fail:"+ToolKits.getErrorCode());

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
        menu.addItem((new CaseMenu.Item(this , "ClientUploadUnpackingCheckInfo" , "ClientUploadUnpackingCheckInfo")));

        menu.run();
    }

    public static void main(String[] args) {
        PackageOpeningWorkbenchAccessSecurityDemo packageOpeningWorkbenchAccessSecurityDemo=new PackageOpeningWorkbenchAccessSecurityDemo();
        InitTest("10.35.232.160",37777,"admin","admin123");
        packageOpeningWorkbenchAccessSecurityDemo.RunTest();
        LoginOut();

    }

}
