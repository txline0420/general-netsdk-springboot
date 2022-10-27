package com.netsdk.demo.customize.JordanPSD.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 通用高安全登录接口
 *
 * @author 47040
 * @since Created in 2021/5/25 11:05
 */
public class LogonModule {

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;
    
    // 编码格式
 	public static String encode;
 	
    static {
		String osPrefix = getOsPrefix();
		if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
			encode = "GBK";
		} else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
			encode = "UTF-8";
		}
	}
    

    // 登录 高安全 TCP
    public static NetSDKLib.LLong TcpLoginWithHighSecurity(String ipAddress, Integer port, String userName, String password,
                                                           NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo) {
        System.out.println("设备地址：" + ipAddress + "\n端口号：" + port + "\n用户名：" + userName + "\n密码：" + password);

        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstInParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();   // 输入结构体参数
        System.arraycopy(ipAddress.getBytes(), 0, pstInParam.szIP, 0, ipAddress.length());
        pstInParam.nPort = port;
        System.arraycopy(userName.getBytes(), 0, pstInParam.szUserName, 0, userName.length());
        System.arraycopy(password.getBytes(), 0, pstInParam.szPassword, 0, password.length());
        pstInParam.emSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;

        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输出结构体参数
        pstOutParam.stuDeviceInfo = m_stDeviceInfo;                     // 设备信息 登陆成功后会刷新这个实例

        NetSDKLib.LLong m_hLoginHandle = NetSdk.CLIENT_LoginWithHighLevelSecurity(pstInParam, pstOutParam);
        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[%s]\n", ipAddress, port, ENUMERROR.getErrorMessage());
        } else {
            System.out.println("Login Success [ " + ipAddress + " ]");
        }
        return m_hLoginHandle;
    }

    // 登录 主动注册
    public static NetSDKLib.LLong AutoRegisterLogin(String sn, String ipAddress, Integer port, String userName, String password,
                                                    NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo) {
        // 登陆设备
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_SERVER_CONN;    // 主动注册
        // 将 序列号 转化为 pointer 类型
        com.netsdk.lib.NativeString serial = new com.netsdk.lib.NativeString(sn);
        IntByReference errorReference = new IntByReference(0);
        NetSDKLib.LLong m_hLoginHandle = NetSdk.CLIENT_LoginEx2(ipAddress, port, userName, password, nSpecCap, serial.getPointer(), m_stDeviceInfo, errorReference);
        if (m_hLoginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Succeed!\n", ipAddress);
        } else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", ipAddress, ENUMERROR.getErrorCode());
        }
        return m_hLoginHandle;

    }
    
    //高安全登录  主动注册  TLS加密
    public static NetSDKLib.LLong AutoRegisterLoginWithHighSecurity(String sn,String ipAddress, Integer port, String userName, String password,
            NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo) {
		
		NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstInParam =
		new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();   // 输入结构体参数
		System.arraycopy(ipAddress.getBytes(), 0, pstInParam.szIP, 0, ipAddress.length());
		pstInParam.nPort = port;
		System.arraycopy(userName.getBytes(), 0, pstInParam.szUserName, 0, userName.length());
		System.arraycopy(password.getBytes(), 0, pstInParam.szPassword, 0, password.length());
		
		//登录模式 参考EM_LOGIN_SPAC_CAP_TYPE   0 - TCP登陆, 默认方式      2 -主动注册的登入
		pstInParam.emSpecCap = 2; 
		//获取设备序列号指针对象
		Pointer deviceId = GetStringToPointer(sn);
		pstInParam.pCapParam = deviceId;
		//登录的TLS模式，参考EM_LOGIN_TLS_TYPE  0-不走tls加密, 默认方式    1- 自适应tls加密  2-强制tls加密  3-部分tls加密
		pstInParam.emTLSCap = 3;		
		
		NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
		new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输出结构体参数
		pstOutParam.stuDeviceInfo = m_stDeviceInfo;                     // 设备信息 登陆成功后会刷新这个实例
		
		NetSDKLib.LLong m_hLoginHandle = NetSdk.CLIENT_LoginWithHighLevelSecurity(pstInParam, pstOutParam);
		if (m_hLoginHandle.longValue() == 0) {
		System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[%s]\n", ipAddress, port, ENUMERROR.getErrorMessage());
		} else {
		System.out.println("Login Success [ " + ipAddress + " ]");
		}
		return m_hLoginHandle;
   }
       
    public static Pointer GetStringToPointer(String src) {	
    	Pointer pointer = null;
    	try {
			byte[] b = src.getBytes(encode);
			pointer = new Memory(b.length+1);
			pointer.clear(b.length+1);
			
			pointer.write(0, b, 0, b.length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	return pointer;
    }

    // 登出
    public static boolean logout(NetSDKLib.LLong m_hLoginHandle) {
        if (m_hLoginHandle.longValue() == 0) {
            System.err.println("LoginHandle invalid");
            return false;
        }
        if (!NetSdk.CLIENT_Logout(m_hLoginHandle)) {
            System.err.println("Logout Failed:" + ENUMERROR.getErrorCode());
            return false;
        } else {
            System.out.println("Logout Succeed.");
            m_hLoginHandle.setValue(0);
            return true;
        }
    }
}
