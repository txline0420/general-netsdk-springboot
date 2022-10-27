package com.netsdk.demo.customize.videoin;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_VIDEOIN_DEFOG_INFO;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

import static com.netsdk.lib.NetSDKLib.NET_EM_DEFOG_MODE.NET_EM_DEFOG_AUTO;
import static com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VIDEOIN_DEFOG;

/**
 * className：VideoinConfigDemo
 * description：高点监控球机透雾功能打开、关闭需求定制。
 * author：251589
 * createTime：2020/12/28 14:59
 *
 * @version v1.0
 */
public class VideoinConfigDemo {

    public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    // 登陆句柄
    private static NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 设备信息扩展
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();


    public void initTest() {
        // 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);
        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);

        //打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)) {
            System.err.println("Open SDK Log Failed!!!");
        }
        login();
    }

    public void login() {
        // 登陆设备
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(ip, port, username,
                password, nSpecCap, null, deviceInfo, nError);
        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", ip);
        } else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", ip, ToolKits.getErrorCode());
            loginOut();
        }
    }


    public void loginOut() {
        System.out.println("End Test");
        if (loginHandle.longValue() != 0) {
            netSdk.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }


    /**
     * 设备断线回调
     */
    private static class DisConnectCallBack implements NetSDKLib.fDisConnect {

        private DisConnectCallBack() {
        }

        private static class CallBackHolder {
            private static DisConnectCallBack instance = new DisConnectCallBack();
        }

        public static DisConnectCallBack getInstance() {
            return CallBackHolder.instance;
        }

        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
        }
    }

    /**
     * 设备重连回调
     */
    private static class HaveReConnectCallBack implements NetSDKLib.fHaveReConnect {
        private HaveReConnectCallBack() {
        }

        private static class CallBackHolder {
            private static HaveReConnectCallBack instance = new HaveReConnectCallBack();
        }

        public static HaveReConnectCallBack getInstance() {
            return CallBackHolder.instance;
        }

        public void invoke(NetSDKLib.LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);

        }
    }


    /**
     * 获取透雾配置
     */
    public void getConfig() {
        NET_VIDEOIN_DEFOG_INFO info = new NET_VIDEOIN_DEFOG_INFO();
        info.write();
        boolean ret = netSdk.CLIENT_GetConfig(loginHandle, type, -1, info.getPointer(), info.size(), 5000, null);
        if (ret) {
            info.read();
            System.out.println("读取配置" + info.toString());
        }
    }

    /**
     * 下发透雾使能配置
     */
    public void setConfig() {
        NET_VIDEOIN_DEFOG_INFO info = new NET_VIDEOIN_DEFOG_INFO();

        info.emDefogMode = NET_EM_DEFOG_AUTO;  //自动 2
//        info.bCamDefogEnable = true;
//        info.emCfgType = NET_EM_CONFIG_NORMAL; // 普通 2
//        info.emIntensityMode = NET_EM_INTENSITY_MODE_AUTO; // 自动 1
//        info.nIntensity = 50;
//        info.nLightIntensityLevel = 2;
        info.write();
        boolean ret = netSdk.CLIENT_SetConfig(loginHandle, type, -1, info.getPointer(), info.size(), 5000, null, null);
        if (ret) {
            System.out.println("下发透雾使能配置成功！！！");
        } else {
            System.err.println("下发透雾使能配置," + ENUMERROR.getErrorMessage());
        }
    }


    public void run() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "设置透雾", "setConfig"));
        menu.addItem(new CaseMenu.Item(this, "获取透雾配置", "getConfig"));
        menu.run();
    }

    int type = NET_EM_CFG_VIDEOIN_DEFOG;
    String ip = "171.5.42.15";//"172.23.10.38";
    int port = 37777;
    String username = "admin";
    String password = "admin123";

    public static void main(String[] args) {
        VideoinConfigDemo demo = new VideoinConfigDemo();
        demo.initTest();
        demo.run();
        demo.loginOut();
    }
}
