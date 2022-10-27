package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.demo.util.EventTaskHandler;
import com.netsdk.demo.util.QueueGeneration;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.DEV_EVENT_NONMOTOR_ENTRYING_INFO;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * className：NonMotorEntryingDemo
 * description： 智慧社区项目，我们的前端电梯电动车禁入半球、IVSS708，SDK对接小区睿家科技第三方平台，实现报警信息在小区平台上展现
 * author：251589
 * createTime：2020/12/21 11:26
 *
 * @version v1.0
 */
public class NonMotorEntryingDemo {
    static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }


    // 设备信息
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    //登录句柄
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 智能订阅句柄
    private NetSDKLib.LLong attachHandle = new NetSDKLib.LLong(0);


    /**
     * 通用服务
     */
    static class SDKGeneralService {

        // 网络断线回调
        // 调用 CLIENT_Init设置此回调, 当设备断线时会自动调用.
        public static class DisConnect implements NetSDKLib.fDisConnect {

            private DisConnect() {
            }

            private static class CallBackHolder {
                private static final DisConnect cb = new DisConnect();
            }

            public static final DisConnect getInstance() {
                return CallBackHolder.cb;
            }

            public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
            }
        }

        // 网络连接恢复回调
        // 调用 CLIENT_SetAutoReconnect设置此回调, 当设备重连时会自动调用.
        public static class HaveReConnect implements NetSDKLib.fHaveReConnect {

            private HaveReConnect() {
            }

            private static class CallBackHolder {
                private static final HaveReConnect cb = new HaveReConnect();
            }

            public static final HaveReConnect getInstance() {
                return CallBackHolder.cb;
            }

            public void invoke(NetSDKLib.LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
            }
        }

        /**
         * SDK初始化
         */
        public static void init() {
            // SDK资源初始化
            netSdk.CLIENT_Init(SDSnap.SDKGeneralService.DisConnect.getInstance(), null);

            // 设置断线重连回调，当设备断线后会自动重连并在重连后自动调用HaveReConnect(可选)
            netSdk.CLIENT_SetAutoReconnect(SDSnap.SDKGeneralService.HaveReConnect.getInstance(), null);

            // 打开SDK日志（可选）
            NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
            setLog.bSetFilePath = 1;
            setLog.bSetPrintStrategy = 1;
            setLog.nPrintStrategy = 0;
            String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";

            System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

            boolean bLogopen = netSdk.CLIENT_LogOpen(setLog);
            if (!bLogopen)
                System.err.println("Failed to open NetSDK log !!!");

            netSdk.CLIENT_SetSnapRevCallBack(SDSnap.SnapCallback.getInstance(), null);
        }

        /**
         * SDK——释放资源
         */
        public static void cleanup() {
            netSdk.CLIENT_Cleanup();
        }
    }

    /**
     * 登录设备
     */
    public boolean login() {
        int nSpecCap = 0;
        Pointer pCapParam = null;
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(mIp, mPort, mUserName,
                mPassword, nSpecCap, pCapParam, deviceInfo, nError);

        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Port[%d] Success!\n", mIp, mPort);
        } else {
            System.out.printf("Login Device[%s] Port[%d] Failed. %s\n", mIp, mPort, ToolKits.getErrorCode() + ENUMERROR.getErrorMessage());
        }

        return loginHandle.longValue() != 0;
    }


    /**
     * 登出设备
     */
    public void logout() {
        if (loginHandle.longValue() != 0)
            netSdk.CLIENT_Logout(loginHandle);
    }

    public void InitTest() {
        SDKGeneralService.init(); // SDK初始化
        if (!login())  // 登陆设备
            EndTest();
    }


    /**
     * 智能事件回调
     */
    public static class AnalyzerDataCallBack implements NetSDKLib.fAnalyzerDataCallBack {
        private static AnalyzerDataCallBack singleInstance;

        public static AnalyzerDataCallBack getSingleInstance() {
            if (singleInstance == null) {
                singleInstance = new AnalyzerDataCallBack();
            }
            return singleInstance;
        }


        private QueueGeneration eventCBQueueService = new QueueGeneration();    // 保存图片用队列

        public AnalyzerDataCallBack() {
            eventCBQueueService.init();
        }

        private final String imageSaveFolder = "pic/";      // 图片保存路径


        @Override
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved) {
            File path = new File("./EventPicture/");
            if (!path.exists())
                path.mkdir();
            //public static final int EVENT_IVS_NONMOTOR_ENTRYING = 0x0000030C; // 非机动车进入电梯(对应 DEV_EVENT_NONMOTOR_ENTRYING_INFO)
            switch (dwAlarmType) {
                case NetSDKLib.EVENT_IVS_NONMOTOR_ENTRYING: {
                    System.out.println("非机动车进入电梯事件！");
                    DEV_EVENT_NONMOTOR_ENTRYING_INFO info = new DEV_EVENT_NONMOTOR_ENTRYING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, info);

                    ////////////////////////////////// 事件主要信息 ///////////////////////////
                    StringBuilder mainInfo = new StringBuilder()
                            .append("事件ID: ").append(info.nEventID).append("\n")
                            .append("过车时间(毫秒时间戳): ").append(info.PTS).append("\n")
                            .append("规则ID: ").append(info.nRuleID).append("\n")
                            .append("智能事件所属大类: ").append(info.emClassType).append("\n");
                    System.out.println("事件主要信息" + mainInfo.toString());
                    //保存全景图
                    if (info.stuSceneImage.nLength > 0) {
                        String strFileName = path + "\\" + System.currentTimeMillis() + "全景图.jpg";
                        ToolKits.savePicture(pBuffer, info.stuSceneImage.nOffSet, info.stuSceneImage.nLength, strFileName);
                    } else {
                        System.out.println("非机动车进入电梯事件无全景图");
                    }
                    break;
                }
                default:
                    System.out.println("其他事件-> " + dwAlarmType);
                    break;
            }
            return 0;
        }

        // 处理保存图片
        private class SavePicHandler implements EventTaskHandler {
            private static final long serialVersionUID = 1L;
            private final byte[] imgBuffer;
            private final int length;
            private final String savePath;

            public SavePicHandler(Pointer pBuf, int dwBufOffset, int dwBufSize, String sDstFile) {
                this.imgBuffer = pBuf.getByteArray(dwBufOffset, dwBufSize);
                this.length = dwBufSize;
                this.savePath = sDstFile;
            }

            @Override
            public void eventCallBackProcess() {
                System.out.println("保存图片中...路径：" + savePath);
                File path = new File(imageSaveFolder);
                if (!path.exists()) path.mkdir();
                try {
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(savePath)));
                    out.write(imgBuffer, 0, length);
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private NetSDKLib.fAnalyzerDataCallBack analyzerDataCB = AnalyzerDataCallBack.getSingleInstance();

    /**
     * 订阅智能事件
     */
    public void attachEventRealLoadPic() {
        int bNeedPicture = 1;
        int channel = 0; // 订阅全通道
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, analyzerDataCB, null, null);
        if (attachHandle.longValue() != 0) {
            System.out.println("订阅： " + channel + " 通道号成功！");
        } else {
            System.out.println("订阅： " + channel + " 通道号失败！" + ToolKits.getErrorCode() + ENUMERROR.getErrorMessage());
        }
    }

    /**
     * 退订智能事件
     */
    public void detachEventRealLoadPic() {
        if (attachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(attachHandle);
        }
    }

    public void EndTest() {
        detachEventRealLoadPic();
        logout(); //	登出设备
        System.out.println("See You...");
        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }

    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "事件订阅", "attachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "退订事件", "detachEventRealLoadPic"));
        menu.run();
    }

    /**
     * Parameter for login
     */
    ////////////////////////////////////////////////////////////////
    private String mIp = "10.34.3.110";//"172.12.66.45"; //"172.12.66.45"; //"10.11.16.168"; // 172.23.12.179
    private int mPort = 37777;
    private String mUserName = "admin";
    private String mPassword = "admin";

    ////////////////////////////////////////////////////////////////
    public static void main(String[] args) {
        NonMotorEntryingDemo demo = new NonMotorEntryingDemo();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }

}
