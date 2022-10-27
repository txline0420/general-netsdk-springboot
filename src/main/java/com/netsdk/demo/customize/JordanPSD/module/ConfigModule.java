package com.netsdk.demo.customize.JordanPSD.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.sun.jna.Structure;

/**
 * @author 47040
 * @since Created at 2021/5/26 13:50
 */
public class ConfigModule {

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;

    static NetSDKLib NetConfig = NetSDKLib.CONFIG_INSTANCE;

    public static boolean GetConfig(NetSDKLib.LLong m_hLoginHandle, int nType, int channel, Structure stuObj, int nWaitTime) {

        stuObj.write();
        boolean ret = NetSdk.CLIENT_GetConfig(m_hLoginHandle, nType, channel, stuObj.getPointer(), stuObj.size(), nWaitTime, null);
        if (!ret) {
            System.err.println("GetConfig Filed:" + ENUMERROR.getErrorCode());
            return false;
        }
        stuObj.read();
        return true;
    }

    public static boolean SetConfig(NetSDKLib.LLong m_hLoginHandle, int nType, int channel, Structure stuObj, int nWaitTime) {

        stuObj.write();
        boolean ret = NetSdk.CLIENT_SetConfig(m_hLoginHandle, nType, channel, stuObj.getPointer(), stuObj.size(), nWaitTime, null, null);
        if (!ret) {
            System.err.println("SetConfig Filed:" + ENUMERROR.getErrorCode());
            return false;
        }
        stuObj.read();
        return true;
    }

    /**
     * 获取设备能力集
     *
     * @param m_hLoginHandle 登录句柄
     * @param nType          能力集枚举
     * @param stuIn          入参
     * @param stuOut         出参
     * @param nWaitTime      超时时间
     */
    public static boolean GetDeviceCapability(NetSDKLib.LLong m_hLoginHandle, int nType, Structure stuIn, Structure stuOut, int nWaitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = NetSdk.CLIENT_GetDevCaps(m_hLoginHandle, nType, stuIn.getPointer(), stuOut.getPointer(), nWaitTime);
        if (!ret) {
            System.err.println("GetDevCaps Filed:" + ENUMERROR.getErrorCode());
            return false;
        }
        stuOut.read();
        return true;
    }


    /**
     * 打包配置数据
     *
     * @param strCmd     配置枚举
     * @param bufferSize 缓冲区大小
     * @param cmdObject  配置结构体
     * @return jsonByte
     */
    public static byte[] PackageData2JsonByte(String strCmd, int bufferSize, Structure cmdObject) {
        byte[] strBuffer = new byte[bufferSize];
        boolean ret = NetConfig.CLIENT_PacketData(strCmd, cmdObject.getPointer(), cmdObject.size(), strBuffer, bufferSize);
        if (!ret) {
            System.err.println("PacketData Failed:" + ENUMERROR.getErrorCode());
            return null;
        }
        return strBuffer;
    }

}
