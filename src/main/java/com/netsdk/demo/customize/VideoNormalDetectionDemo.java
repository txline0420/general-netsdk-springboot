package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_VIDEO_NORMAL_DETECTION_INFO;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;

import static com.netsdk.lib.NetSDKLib.EVENT_IVS_VIDEO_NORMAL_DETECTION;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220325026 乌鲁木齐道路交通安全智能管理建设项目VQ8000产品提供JAVA封装SDK包
 * @date 2022/3/29 11:50
 */
public class VideoNormalDetectionDemo extends Initialization {
    int channel = -1;
    NetSDKLib.LLong attachHandle = new NetSDKLib.LLong(0);

    /**
     * 订阅智能任务
     */

    public NetSDKLib.LLong AttachEventRealLoadPic() {
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        if (attachHandle.longValue() != 0) {
            this.DetachEventRealLoadPic();
        }

        // 需要图片
        int bNeedPicture = 1;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, EVENT_IVS_VIDEO_NORMAL_DETECTION, bNeedPicture,
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
                case EVENT_IVS_VIDEO_NORMAL_DETECTION: { // 视频正常事件,在视频诊断检测周期结束时,将未报错的诊断项上报正常事件
                    System.out.println("视频正常事件,在视频诊断检测周期结束时,将未报错的诊断项上报正常事件");
                    DEV_EVENT_VIDEO_NORMAL_DETECTION_INFO info = new DEV_EVENT_VIDEO_NORMAL_DETECTION_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, info);
                    int nChannelID = info.nChannelID;
                    System.out.println("nChannelID:" + nChannelID);

                    int nAction = info.nAction;
                    System.out.println("nAction:" + nAction);

                    int nEventID = info.nEventID;
                    System.out.println("nEventID:" + nEventID);

                    NET_TIME_EX utc = info.UTC;
                    System.out.printf("utc:" + utc);

                    double dbPTS = info.dbPTS;
                    System.out.printf("dbPTS:" + dbPTS);

                    int emDetectType = info.emDetectType;
                    System.out.printf("emDetectType:" + emDetectType);

                    break;
                }
                default:
                    System.out.println("其他事件--------------------" + dwAlarmType);
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

    public void RunTest() {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this, "AttachEventRealLoadPic", "AttachEventRealLoadPic")));
        menu.addItem((new CaseMenu.Item(this, "DetachEventRealLoadPic", "DetachEventRealLoadPic")));

        menu.run();
    }

    public static void main(String[] args) {
        VideoNormalDetectionDemo videoNormalDetectionDemo = new VideoNormalDetectionDemo();
        InitTest("20.2.36.32", 37777, "admin", "admin123");
        videoNormalDetectionDemo.RunTest();
        LoginOut();

    }
}