package com.netsdk.demo.test;



import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ThreadPoolUtil;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.structure.NET_DEV_EVENT_FACERECOGNITION_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author 291189
 * @version 1.0
 * @description jna性能优化 测试
 * @date 2021/7/20
 */
public class JnaXingNeng {

    private String m_strIpAddr = "10.34.3.219";

    private String m_strIpAddr1="10.34.3.220";

    private int m_nPort = 37777;

    private String m_strUser = "admin";

    private String m_strPassword = "admin";


    static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;


    // public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    // 登陆句柄
    private static NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 订阅句柄
    private static NetSDKLib.LLong AttachHandle = new NetSDKLib.LLong(0);

    // 设备信息扩展
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    /**
     * 设备断线回调
     */
    private static class DisconnectCallback implements NetSDKLib.fDisConnect {
        private static DisconnectCallback instance = new DisconnectCallback();

        private DisconnectCallback() {
        }

        public static DisconnectCallback getInstance() {
            return instance;
        }

        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s:%d] Disconnect!\n", pchDVRIP, nDVRPort);
        }
    }

    /**
     * 设备重连回调
     */
    private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
        private static HaveReconnectCallback instance = new HaveReconnectCallback();

        private HaveReconnectCallback() {
        }

        public static HaveReconnectCallback getInstance() {
            return instance;
        }

        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s:%d] HaveReconnected!\n", pchDVRIP, nDVRPort);
        }
    }


    public void InitTest() {
        // 初始化SDK库
        netSdk.CLIENT_Init(DisconnectCallback.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);

        //打开日志，可选0
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)) {
            System.err.println("Open SDK Log Failed!!!");
        }

        loginWithHighLevel();
    }

    /**
     * 高安全登录
     */
    public void loginWithHighLevel() {
        // 输入结构体参数
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
            {
                szIP = m_strIpAddr.getBytes();
                nPort = m_nPort;
                szUserName = m_strUser.getBytes();
                szPassword = m_strPassword.getBytes();
            }
        };
        // 输出结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        // 写入sdk
        loginHandle = netSdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
        if (loginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
                    netSdk.CLIENT_GetLastError());
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + m_strIpAddr);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }
  //      System.arraycopy();
    }

    /**
     * 退出清理环境
     */
    public void LoginOut() {
        System.out.println("End Test");
        if (loginHandle.longValue() != 0) {
            netSdk.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }


    // 编码格式
    public static String encode;

    static {
        String osPrefix = Utils.getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win64-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }


    /**
     * 选择通道
     */
    private int channel = 0;


    /**
     * 订阅智能任务
     */
    public void attachIVSEvent() {

        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        this.DetachEventRealLoadPic();
        // 需要图片
        int bNeedPicture = 1;
        //订阅所有事件
        AttachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
                AnalyzerDataCB.getInstance(), null, null);
        if (AttachHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
                    ToolKits.getErrorCode());
        }

    }

    static  int count=0;

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

          /*  if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }*/

            /*NET_DEV_EVENT_FACERECOGNITION_INFO msg = new NET_DEV_EVENT_FACERECOGNITION_INFO();

            ToolKits.GetPointerData(pAlarmInfo, msg);*/

                try {
                    if(dwAlarmType==NetSDKLib.EVENT_IVS_FACERECOGNITION){

                        NET_DEV_EVENT_FACERECOGNITION_INFO msg1 = new NET_DEV_EVENT_FACERECOGNITION_INFO();


                        byte[] byteArray
                                = pAlarmInfo.getByteArray(0, msg1.size());

                        ThreadPoolUtil.getThreadPool().execute(new MyThread(dwAlarmType,byteArray));
                    }


                }catch (Exception e){
                    e.printStackTrace();
                }




            return 0;
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




    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this, "attachIVSEvent", "attachIVSEvent")));
        menu.addItem((new CaseMenu.Item(this, "DetachEventRealLoadPic", "DetachEventRealLoadPic")));
        menu.run();
    }

    public static void main(String[] args) {
       JnaXingNeng TD = new JnaXingNeng();
        TD.InitTest();
        TD.RunTest();
        TD.LoginOut();
    }


    public static class MyThread extends Thread
    {
      public int  dwAlarmType;
       public   byte[] pointer;
        public MyThread(int  dwAlarmType,  byte[] pointer)
        {
           this.dwAlarmType=dwAlarmType;
           this.pointer=pointer;
        }

        public MyThread() {
        }

        public void run()
        {

                    new JnaXingNeng().handleData(dwAlarmType,pointer);


        }

    }

    public   void handleData( int  dwAlarmType, byte[] pointer)  {




        Pointer m=new Memory(pointer.length);

        m.write(0, pointer, 0, pointer.length);

        Calendar cal = Calendar.getInstance();
        count++;
        System.out.println("count:"+count);
        Date date1 = cal.getTime();
        System.out.println("start:"+new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(date1));


        System.out.println("dwAlarmType:" + dwAlarmType);
        switch (dwAlarmType) {

     /*       case NetSDKLib.EVENT_IVS_FACEDETECT:
                    System.out.println("人脸事件");
                    NetSDKLib.DEV_EVENT_FACEDETECT_INFO msg = new NetSDKLib.DEV_EVENT_FACEDETECT_INFO();
                    ToolKits.GetPointerData(pointer, msg);
                    System.out.println("info:"+msg.nChannelID);
                    Calendar cal1 = Calendar.getInstance();
                    Date date2 = cal1.getTime();
                    System.out.println("end:"+new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(date2));
                    break;*/

            case NetSDKLib.EVENT_IVS_FACERECOGNITION:

                NET_DEV_EVENT_FACERECOGNITION_INFO msg1 = new NET_DEV_EVENT_FACERECOGNITION_INFO();

                ToolKits.GetPointerData(m, msg1);
              //  int bGlobalScenePic = msg1.bGlobalScenePic;

                NetSDKLib.CANDIDATE_INFOEX[] stuCandidatesEx = msg1.stuCandidatesEx;
                for (NetSDKLib.CANDIDATE_INFOEX candidate_infoex: stuCandidatesEx
                ) {
                    System.out.println("candidate_infoex:"+candidate_infoex.stTime);
                }

                NetSDKLib.NET_PIC_INFO stuGlobalScenePicInfo = msg1.stuGlobalScenePicInfo;
                System.out.println("stuGlobalScenePicInfo:"+stuGlobalScenePicInfo.dwFileLenth);

                //两种方式都要将结构体对象作为全局变量放在外面；避免每次进入回调都要进行初始化；
                //1:选择想要的字段进行复制，不再将结构体中所有的字段全都拷贝下来

                //2:当前线程只进行不耗时write()操作，将read()放到另一条线程中进行

					/*long time1=System.currentTimeMillis();
					msg.getPointer().write(0, pAlarmInfo.getByteArray(0, msg.size()), 0, pAlarmInfo.getByteArray(0, msg.size()).length);

					new Thread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							msg.read();
							System.out.println("szName : " + new String(msg.szName).trim() + "\n" );
						}
					}).start();

					long time2=System.currentTimeMillis();
					System.out.println(time1-time2);*/

                break;
            default:
                System.out.println("其他事件--------------------" + dwAlarmType);
                break;
        }


        Calendar cal2 = Calendar.getInstance();
        Date date3 = cal2.getTime();
        System.out.println("end:"+new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(date3));
    //    lock.unlock();
    }


}