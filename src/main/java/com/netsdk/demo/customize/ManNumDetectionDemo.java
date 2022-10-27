package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description ManNumDetection事件
 * @date 2021/11/30 11:34
 */
public class ManNumDetectionDemo extends Initialization {


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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_MAN_NUM_DETECTION, bNeedPicture,AnalyzerDataCB.getInstance(), null, null);


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
                case NetSDKLib.EVENT_IVS_MAN_NUM_DETECTION: //   立体视觉区域内人数统计事件(对应DEV_EVENT_MANNUM_DETECTION_INFO)

                    NetSDKLib.DEV_EVENT_MANNUM_DETECTION_INFO msg = new NetSDKLib.DEV_EVENT_MANNUM_DETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                          int nChannelID=  msg.nChannelID;
                          System.out.println("通道号："+nChannelID);


                    try {
                     //   System.out.println("事件名称UTF："+new String(msg.szName,"UTF-8"));
                        System.out.println("事件名称GBK："+new String(msg.szName,"GBK"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    double pts = msg.PTS;
                          System.out.println("时间戳:"+pts);
                          NetSDKLib.NET_TIME_EX utc = msg.UTC;
                    System.out.println("事件发生的时间:"+utc);
                    int nEventID = msg.nEventID;
                    System.out.println("事件ID:"+nEventID);

                    int nAction = msg.nAction;
                    System.out.println("0:脉冲 1:开始 2:停止:"+nAction);
                    int nManListCount = msg.nManListCount;
                    System.out.println("区域人员列表数量:"+nManListCount);

                    NetSDKLib.MAN_NUM_LIST_INFO[] stuManList = msg.stuManList;

                    for(int i=0;i<nManListCount;i++){
                        NetSDKLib.MAN_NUM_LIST_INFO   list_info=   stuManList[i];
                        int nStature = list_info.nStature;
                            System.out.println("人员身高，单位cm:"+nStature);

                        NetSDKLib.DH_RECT stuBoudingBox = list_info.stuBoudingBox;
                        System.out.println("人员包围盒,8192坐标系:"+stuBoudingBox.right+","+stuBoudingBox.left+","+stuBoudingBox.bottom+","+stuBoudingBox.top);

                    }

                    NetSDKLib.EVENT_INTELLI_COMM_INFO stuIntelliCommInfo = msg.stuIntelliCommInfo;
                        System.out.println("智能事件所属大类:"+stuIntelliCommInfo.emClassType);
                    System.out.println("该事件触发的预置点:"+stuIntelliCommInfo.nPresetID);

                    int nAreaID = msg.nAreaID;
                    System.out.println("区域ID(一个预置点可以对应多个区域ID):"+nAreaID);

                    int nPrevNumber = msg.nPrevNumber;
                    System.out.println("变化前人数:"+nPrevNumber);

                    int nCurrentNumber = msg.nCurrentNumber;
                    System.out.println("当前人数:"+nCurrentNumber);

                    byte[] szSourceID = msg.szSourceID;
                    System.out.println("事件关联ID:"+new String(szSourceID));

                    byte[] szRuleName = msg.szRuleName;
                    try {
                        System.out.println("规则名称:"+new String(szRuleName,"GBK"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    int emDetectType = msg.emDetectType; //检测模式 EM_EVENT_DETECT_TYPE
                    System.out.println("检测模式:"+emDetectType);

                    int nAlertNum = msg.nAlertNum;
                    System.out.println("实际触发报警的人数:"+nAlertNum);
                    //报警类型. 0:未知, 1:从人数正常到人数异常, 2:从人数异常到人数正常
                    int nAlarmType = msg.nAlarmType;
                    System.out.println("报警类型:"+nAlarmType);


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
        ManNumDetectionDemo manNumDetectionDemo=new ManNumDetectionDemo();
        InitTest("172.29.2.132",37777,"admin","admin123");
        manNumDetectionDemo.RunTest();
        LoginOut();

    }


}
