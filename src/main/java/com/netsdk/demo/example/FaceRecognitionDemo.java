package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_PERSON_FREQUENCY_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/7/21 11:05
 */
public class FaceRecognitionDemo extends Initialization {


  //  private static String m_strIpAddr 				= "10.34.3.219";
    private static String m_strIpAddr="172.12.5.69";
    private static int    m_nPort 				    = 37777;
    private static String m_strUser 			    = "admin";
    private static String m_strPassword 		    = "admin123";
    // 订阅句柄
    private static NetSDKLib.LLong AttachHandle = new NetSDKLib.LLong(0);
    /**
     * 选择通道
     */
    private int channel = 12;
    /**
     * 订阅智能任务
     */
    public void attachIVSEvent() {

        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        this.DetachEventRealLoadPic();
        // 需要图片
        int bNeedPicture = 1;
        //订阅所有事件
        AttachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_FACERECOGNITION , bNeedPicture, AnalyzerDataCB.getInstance(), null, null);
        if (AttachHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
                    ToolKits.getErrorCode());
        }

    }
    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (AttachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(AttachHandle);
        }
    }

    /**
     * 智能事件回调
     */

    private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {

        private static AnalyzerDataCB instance;

        private AnalyzerDataCB() {


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

                case NetSDKLib.EVENT_IVS_FACERECOGNITION:

                    NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO msg = new NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    NetSDKLib.CANDIDATE_INFOEX[] stuCandidatesEx
                            = msg.stuCandidatesEx;

                    for (int i=0;i<stuCandidatesEx.length;i++
                         ) {
                        NetSDKLib.FACERECOGNITION_PERSON_INFOEX stPersonInfo
                                = stuCandidatesEx[i].stPersonInfo;

                        NET_PERSON_FREQUENCY_INFO stuFrequencyInfo = stPersonInfo.stuFrequencyInfo;

                        //报警类型
                        int emAlarmType = stuFrequencyInfo.emAlarmType;
                        //频次
                        int nTimes = stuFrequencyInfo.nTimes;
                        System.out.println("emAlarmType:"+emAlarmType);
                        System.out.println("nTimes:"+nTimes);
                    }
                    break;
                default:
                    System.out.println("其他事件--------------------" + dwAlarmType);
                    break;
            }
            return 0;
        }
    }

    public  void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this , "订阅智能任务" , "attachIVSEvent"));
        menu.addItem(new CaseMenu.Item(this , "停止侦听智能事件" , "DetachEventRealLoadPic"));
        menu.run();
    }

    public static void main(String[] args){
        InitTest(m_strIpAddr,m_nPort,m_strUser,m_strPassword);
        new FaceRecognitionDemo().RunTest();
        LoginOut();
    }

}
