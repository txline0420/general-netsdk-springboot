package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220106092
 * 【易华录集成】人证核验终端SDK需求定制
 * @date 2022/1/10 10:06
 */
public class CitizenPictureCompareDemo extends Initialization {



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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_CITIZEN_PICTURE_COMPARE, bNeedPicture, AnalyzerDataCB.getInstance(), null, null);
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


        @Override
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }

            switch (dwAlarmType) {

                case NetSDKLib.EVENT_IVS_CITIZEN_PICTURE_COMPARE:  //人证比对事件(对应  DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO )
                {
                    System.out.println("----------------------------人证比对事件");
                    NetSDKLib.DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO msg = new NetSDKLib.DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    try {
                        System.out.println("事件名称 :" + new String(msg.szName).trim());
                        System.out.println("比对结果:" + msg.bCompareResult);
                        System.out.println("两张图片的相似度:" + msg.nSimilarity);
                        System.out.println("检测阈值:" + msg.nThreshold);
                        System.out.print("性别:");
                        if (msg.emSex == 1) {
                            System.out.println("男");
                        } else if (msg.emSex == 2) {
                            System.out.println("女");
                        } else {
                            System.out.println("未知");
                        }
                        System.out.println("民族:" + msg.nEthnicity + "(参照 DEV_EVENT_ALARM_CITIZENIDCARD_INFO 的 nEthnicity 定义)");
                        System.out.println("居民姓名:" + new String(msg.szCitizen, "GBK").trim());
                        System.out.println("住址:" + new String(msg.szAddress, "GBK").trim());
                        System.out.println("身份证号:" + new String(msg.szNumber).trim());
                        System.out.println("签发机关:" + new String(msg.szAuthority, "GBK").trim());

                        SimpleDateFormat orignalDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
                        SimpleDateFormat convertDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
                        System.out.println("出生日期:" + convertDateFormat.format(orignalDateFormat.parse(msg.stuBirth.toString())));
                        System.out.println("起始日期:" + convertDateFormat.format(orignalDateFormat.parse(msg.stuValidityStart.toString())));
                        if (msg.bLongTimeValidFlag == 1) {
                            System.out.println("截止日期：永久");
                        } else {
                            System.out.println("截止日期:" + convertDateFormat.format(orignalDateFormat.parse(msg.stuValidityEnd.toString())));
                        }
                    } catch (Exception e) {
                        System.err.println("转GBK编码失败！");
                    }

                    // 拍摄照片
                    String strFileName = picturePath + "\\" + System.currentTimeMillis() + "citizen_shoot.jpg";
                    ToolKits.savePicture(pBuffer, msg.stuImageInfo[0].dwOffSet, msg.stuImageInfo[0].dwFileLenth, strFileName);
                    // 身份证照片
                    strFileName = picturePath + "\\" + System.currentTimeMillis() + "citizen_card.jpg";
                    ToolKits.savePicture(pBuffer, msg.stuImageInfo[1].dwOffSet, msg.stuImageInfo[1].dwFileLenth, strFileName);
                    // 访客人数
                    System.out.println("访客人数:"+ msg.nVisitorNumber);
                    // 车牌
                    try {
                        System.out.println("车牌:"+ new String (msg.szTrafficPlate,encode));

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
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
        CitizenPictureCompareDemo citizenPictureCompareDemo=new CitizenPictureCompareDemo();
        InitTest("172.10.3.159",37777,"admin","admin123");
        citizenPictureCompareDemo.RunTest();
        LoginOut();
    }
}
