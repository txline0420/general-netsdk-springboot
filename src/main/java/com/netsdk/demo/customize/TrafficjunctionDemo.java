package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_EXTRA_PLATES;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220526188 第三方平台通过SDK接大华前端港澳车牌识别，JAVA语言开发SDK包
 * @date 2022/5/30 14:32
 */
public class TrafficjunctionDemo extends Initialization {
    private static String ipAddr = "172.24.8.21";
    private static int port = 37777;
    private static String user = "admin";
    private static String password = "admin123";

    static int channel=0;
    NetSDKLib.LLong   attachHandle=new NetSDKLib.LLong(0);


    public NetSDKLib.LLong AttachEventRealLoadPic() {
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        if (attachHandle.longValue() != 0) {
            this.DetachEventRealLoadPic();
        }

        // 需要图片
        int bNeedPicture = 1;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL , bNeedPicture,
                AnalyzerDataCB.getInstance(), null, null);
        if (attachHandle.longValue() != 0) {
            System.out.println("Chn"+channel+" CLIENT_RealLoadPictureEx Success");
        } else {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
                    ToolKits.getErrorCode());
        }

        return attachHandle;
    }

    /**
     * 报警事件（智能）回调  该回调使用时，其创建对象建议为持久化对象，避免被Gc回收，可使用静态方式
     */
    private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        private final File picturePath;
        private static AnalyzerDataCB instance;

        @Override
        public void finalize() throws Throwable {
            super.finalize();
            System.out.println("AnalyzerDataCB 对象已经被回收");
        }

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
                case NetSDKLib.EVENT_IVS_TRAFFICJUNCTION : {

                    NetSDKLib.DEV_EVENT_TRAFFICJUNCTION_INFO  msg=new NetSDKLib.DEV_EVENT_TRAFFICJUNCTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    NetSDKLib.EVENT_COMM_INFO stCommInfo
                            = msg.stCommInfo;
                    int nExtraPlatesCount
                            = stCommInfo.nExtraPlatesCount;
                    System.out.println("nExtraPlatesCount:"+nExtraPlatesCount);

                    NET_EXTRA_PLATES[] stuExtraPlates
                            = stCommInfo.stuExtraPlates;
                    for(int i=0;i<nExtraPlatesCount;i++){
                        NET_EXTRA_PLATES stuExtraPlate = stuExtraPlates[i];


                        try {
                            System.out.println("szText utf-8:"+new String(stuExtraPlate.szText,"UTF-8"));
                            System.out.println("szText GBK:"+new String(stuExtraPlate.szText,"GBK"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        /**
                         车牌类型 {@link com.netsdk.lib.enumeration.EM_NET_PLATE_TYPE}
                         */
                        System.out.println("emCategory:"+stuExtraPlate.emCategory);
                        /**
                         车牌颜色 {@link com.netsdk.lib.enumeration.EM_NET_PLATE_COLOR_TYPE}
                         */
                        System.out.println("emColor:"+stuExtraPlate.emColor);

                        String picture = picturePath + "\\" + System.currentTimeMillis() + "related.jpg";
                        ToolKits.savePicture(pBuffer, stuExtraPlate.nOffset, stuExtraPlate.nLength, picture);
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
        TrafficjunctionDemo trafficjunctionDemo=new TrafficjunctionDemo();
        trafficjunctionDemo.RunTest();
        LoginOut();
    }


}
