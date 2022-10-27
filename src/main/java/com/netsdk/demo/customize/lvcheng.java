package com.netsdk.demo.customize;


import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Date;
import java.util.Scanner;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/12/10 11:19
 */
public class lvcheng extends Initialization {

        /**
         * 手动抓图按钮事件
         */

    static    int count =0;

    /**
     * 手动抓图
      * @return
     */
    public static boolean manualSnapPicture() {
        NetSDKLib.MANUAL_SNAP_PARAMETER snapParam = new NetSDKLib.MANUAL_SNAP_PARAMETER();
        snapParam.nChannel = 0;
        String sequence = (++count)+""; // 抓图序列号，必须用数组拷贝
        System.arraycopy(sequence.getBytes(), 0, snapParam.bySequence, 0, sequence.getBytes().length);

        snapParam.write();
        boolean bRet = netSdk.CLIENT_ControlDeviceEx(loginHandle, NetSDKLib.CtrlType.CTRLTYPE_MANUAL_SNAP, snapParam.getPointer(), null, 500);
        if (!bRet) {
            System.err.println("Failed to manual snap, last error " + ToolKits.getErrorCode());
            return false;
        } else {
            System.out.println("SeccessedCount:"+count+":manualSnap:"+new Date());
            System.out.println("Seccessed to manual snap");
        }
        snapParam.read();
        return true;
    }


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
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_TRAFFIC_MANUALSNAP, bNeedPicture, AnalyzerDataCB.getInstance(), null, null);
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
    static int  countGetManualsnap=0;
    static int  countGetAll=0;

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

            countGetAll++;
            System.out.println("countGetAll:"+countGetAll);
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }


            switch (dwAlarmType) {
                case NetSDKLib.EVENT_IVS_TRAFFIC_MANUALSNAP: //

                    NetSDKLib.DEV_EVENT_TRAFFIC_MANUALSNAP_INFO msg=new NetSDKLib.DEV_EVENT_TRAFFIC_MANUALSNAP_INFO();

                     System.out.println("szManualSnapNo:"+new String(msg.szManualSnapNo));

                    countGetManualsnap++;
                    System.out.println("countGetManualsnap:"+countGetManualsnap);
                    System.out.println("get:"+new Date());
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



    public static void main(String[] args) {

        InitTest("10.11.17.191",37777,"admin","admin123");

        Scanner scanner=new Scanner(System.in);
        lvcheng lc=new lvcheng();

        while (true){
            System.out.println("1 订阅任务");
            System.out.println("2 手动抓图");
            System.out.println("3 退订任务");
            System.out.println("4 退出");

            int nextInt = scanner.nextInt();
            if(nextInt==1){
                lc.AttachEventRealLoadPic();
            }else if(nextInt==2) {

                for (int i = 0; i < 1000; i++){
                    manualSnapPicture();
                }

            }else if(nextInt==3){
                lc.DetachEventRealLoadPic();
            }else {
                break;
            }

        }
        LoginOut();
    }


}
