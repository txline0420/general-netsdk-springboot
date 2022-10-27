package com.netsdk.demo.customize.querySystemState;

import com.netsdk.demo.customize.queryFaceDetection.QueryFaceDetectionUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;

/**
 * className：QuerySystemState
 * description：
 * author：251589
 * createTime：2021/2/25 14:37
 *
 * @version v1.0
 */

public class QuerySystemStateDemo {
    // The constant net sdk
    public static final NetSDKLib netSDK = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configSDK = NetSDKLib.CONFIG_INSTANCE;

    ////////////////////////////////////// 登录相关 ///////////////////////////////

    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0); // 登录句柄

    /**
     * 登录设备
     *
     * @return
     */
//    public boolean login() {
//        System.out.println("Function: Login Device." + ip);
//        final int tcpSpecCap = 0;
//        final IntByReference errorReference = new IntByReference(0);
//        final NetSDKLib.NET_DEVICEINFO deviceinfo = new NetSDKLib.NET_DEVICEINFO();
//        loginHandle = netSDK.CLIENT_LoginEx(ip, port, username, password, tcpSpecCap, null, deviceinfo, errorReference);
//        if (loginHandle.longValue() == 0) {
//            System.err.println("Failed to Login " + ip + getErrorCode());
//            return false;
//        }
//        System.out.println("Success to Login " + ip);
//        return true;
//    }

    /**
     * login with high level 高安全级别登陆
     */
    public void loginWithHighLevel() {
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {{
                    szIP = ip.getBytes();
                    nPort = port;
                    szUserName = username.getBytes();
                    szPassword = password.getBytes();
                }};   // 输入结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输结构体参数

        // 写入sdk
        loginHandle = netSDK.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

        if (loginHandle.longValue() == 0L) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", ip, port,
                    netSDK.CLIENT_GetLastError());
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + ip);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }
    }

    /**
     * logout 退出
     */
    public void logOut() {
        if (loginHandle.longValue() != 0L) {
            netSDK.CLIENT_Logout(loginHandle);
            System.out.println("LogOut Success");
        }
    }

    public void querySystemStatus() {

        NET_SYSTEM_STATUS netSystemStatus = new NET_SYSTEM_STATUS();
        // CPU 状态
        NET_CPU_STATUS netCpuStatus = new NET_CPU_STATUS();
        netCpuStatus.write();

        // 内存状态
        NET_MEMORY_STATUS netMemoryStatus = new NET_MEMORY_STATUS();
        netMemoryStatus.write();

        // 风扇状态
        NET_FAN_STATUS netFanStatus = new NET_FAN_STATUS();
        netFanStatus.write();

        // 电源状态
        NET_POWER_STATUS netPowerStatus = new NET_POWER_STATUS();
        netPowerStatus.write();

        // 温度状态
        NET_TEMPERATURE_STATUS netTemperatureStatus = new NET_TEMPERATURE_STATUS();
        netTemperatureStatus.write();

        netSystemStatus.pstuCPU = netCpuStatus.getPointer();
        netSystemStatus.pstuMemory = netMemoryStatus.getPointer();
        netSystemStatus.pstuFan = netFanStatus.getPointer();
        netSystemStatus.pstuPower = netPowerStatus.getPointer();
        netSystemStatus.pstuTemp = netTemperatureStatus.getPointer();

        netSystemStatus.write();
        boolean ret = netSDK.CLIENT_QuerySystemStatus(loginHandle, netSystemStatus.getPointer(), 1000);
        if (ret) {
            System.out.println("查询成功！");

            netSystemStatus.read();

            netCpuStatus.read();
            netMemoryStatus.read();
            netFanStatus.read();
            netPowerStatus.read();
            netTemperatureStatus.read();

            // cpu信息
            System.out.println("cpu状态-> 数量: " + netCpuStatus.nCount + " 查询是否成功: " + netCpuStatus.bEnable);

            for (int i = 0; i < netCpuStatus.nCount; i++) {
                System.out.println("CPU使用率 :" + netCpuStatus.stuCPUs[i].nUsage);
            }

            // 内存信息
            System.out.println("内存信息：" + netMemoryStatus.bEnable + "  总量： " + netMemoryStatus.stuMemory.dwTotal);

            // 电源、池信息
            for (int i = 0; i < netPowerStatus.nCount; i++) {
                System.out.printf("电源[%d] 电源状态: %d, 电池状态: %d%n", i + 1,
                        netPowerStatus.stuPowers[i].bPowerOn,
                        netPowerStatus.stuBatteries[i].bCharging);
            }

            System.err.println("个数：" + netFanStatus.nCount);
            for (int i = 0; i < netFanStatus.nCount; i++) {
                try {
                    System.err.println("stuFans: " + netFanStatus.stuFans[i].nSpeed + " 名称：" +
                            new String(netFanStatus.stuFans[i].szName, "gbk"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }


            System.out.println("温度信息：" + netTemperatureStatus.bEnable + " 数量： " + netTemperatureStatus.nCount);

            for (int i = 0; i < netTemperatureStatus.nCount; i++) {
                try {
                    System.out.println("温度信息： " + netTemperatureStatus.stuTemps[i].fTemperature + new String(netTemperatureStatus.stuTemps[i].szName, "gbk"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }


        } else {
            System.err.println("系统状态查询失败！！！" + netSDK.CLIENT_GetLastError());
        }

    }

    /**
     * 报警信息回调函数原形,建议写成单例模式
     */
    private static class fAlarmDataCB implements NetSDKLib.fMessCallBack {
        private fAlarmDataCB() {
        }

        private static class fAlarmDataCBHolder {
            private static fAlarmDataCB callback = new fAlarmDataCB();
        }

        public static fAlarmDataCB getCallBack() {
            return fAlarmDataCBHolder.callback;
        }

        public boolean invoke(int lCommand, NetSDKLib.LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, Pointer dwUser) {
            switch (lCommand) {
                case NetSDKLib.NET_DISKFULL_ALARM_EX: { // 硬盘满报警，数据为1个字节，
                    System.out.println("硬盘满报警");
                    if (dwBufLen > 0) {
                        byte[] channelState = pStuEvent.getByteArray(0, dwBufLen);  // 将缓存pStuEvent的值，按byte[]输出
                        for (int i = 0; i < dwBufLen; i++) {
                            System.out.print(" Channel[" + i + "] State = " + (channelState[i]));  // 1为有硬盘满报警，0为无报警。
                        }
                    }
                    break;
                }
                case NetSDKLib.NET_DISKERROR_ALARM_EX: {
                    System.out.println("坏硬盘报警");
                    byte[] msg = pStuEvent.getByteArray(0, dwBufLen);
                    for (int i = 0; i < msg.length; i++) {
                        System.out.print("channel : " + i + " state : " + msg[i]);  // i : channel;
                    }                                                              // msg[i]  1:alarm  0:no alarm
                    break;
                }
                case NetSDKLib.NET_ALARM_RAID_STATE_EX: {
                    System.out.println("RAID异常报警");
                    ALARM_RAID_INFO_EX msg = new ALARM_RAID_INFO_EX();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println("错误类型: 0未知，1RAID错误，2RAID降级: " + msg.emErrorType);
                    System.out.println("开始/结束，0开始，1结束: " + msg.nAction);
                    break;
                }
                default:
                    break;
            }
            return true;
        }
    }

    public void startListenAlarm() {
        // 设置报警回调函数
        //
        netSDK.CLIENT_SetDVRMessCallBack(fAlarmDataCB.getCallBack(), null);

        // 向设备订阅报警--扩展
        if (netSDK.CLIENT_StartListenEx(loginHandle)) {
            System.out.printf("CLIENT_StartListenEx Success\n");
        } else {
            System.out.printf("CLIENT_StartListenEx Failed!LastError = %x\n", netSDK.CLIENT_GetLastError());
        }
    }

    public void stopListenAlarm() {
        // 向设备停止报警--扩展
        if (netSDK.CLIENT_StopListen(loginHandle)) {
            System.out.printf("CLIENT_StopListen Success\n");
        } else {
            System.out.printf("CLIENT_StopListen Failed!LastError = %x\n", netSDK.CLIENT_GetLastError());
        }
    }

    // 结束测试
    public void endTest() {
        System.out.println("End Test");
        this.logOut();  // 退出
        System.out.println("See You...");
        QueryFaceDetectionUtils.cleanAndExit();  // 清理资源并退出
    }

    // 加载测试内容
    public void runTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "查询系统状态", "querySystemStatus"));
        menu.addItem(new CaseMenu.Item(this, "事件监听", "startListenAlarm"));
        menu.addItem(new CaseMenu.Item(this, "退订", "stopListenAlarm"));
        menu.run();
    }

    // 初始化测试
    public void initTest() {
        QuerySystemStatusUtils.Init();         // 初始化SDK库
        this.loginWithHighLevel();   // 高安全登录
    }


    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    private String ip = "171.35.0.25";
    private int port = 37777;
    private String username = "admin";
    private String password = "admin1234";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        QuerySystemStateDemo demo = new QuerySystemStateDemo();

        demo.initTest();
        demo.runTest();
        demo.endTest();
    }
}
