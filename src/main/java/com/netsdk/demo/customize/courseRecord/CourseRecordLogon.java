package com.netsdk.demo.customize.courseRecord;

import com.netsdk.lib.NetSDKLib;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/17 15:13
 */
public class CourseRecordLogon {

    //////////////////////// 登陆地址，端口，用户名，密码  //////////////////////////
    public String m_strIpAddr = "";
    public int m_nPort = 0;
    public String m_strUser = "";
    public String m_strPassword = "";
    /////////////////////////////////////////////////////////////////////////////

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    ////////////////////////////////////// 登录相关 ///////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    public NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    public NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登录句柄

    /**
     * login with high level 高安全级别登陆
     */
    public void loginWithHighLevel() {

        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {{
                    szIP = m_strIpAddr.getBytes();
                    nPort = m_nPort;
                    szUserName = m_strUser.getBytes();
                    szPassword = m_strPassword.getBytes();
                }};   // 输入结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输结构体参数

        // 写入sdk
        m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
                    netsdk.CLIENT_GetLastError());
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("Login Succeed");
            System.out.println("Device Address：" + m_strIpAddr);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }
    }

    /**
     * logout 退出
     */
    public void logOut() {
        if (m_hLoginHandle.longValue() != 0) {
            netsdk.CLIENT_Logout(m_hLoginHandle);
            m_hLoginHandle.setValue(0);
            System.out.println("LogOut Succeed");
        }
    }

}
