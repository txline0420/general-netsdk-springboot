package com.netsdk.demo.customize.JordanPSD.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.ENUMERROR;

/**
 * @author 47040
 * @since Created at 2021/5/25 20:58
 */
public class DeviceControlModule {

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;

    // 重启当前设备
    public static boolean DeviceReboot(NetSDKLib.LLong m_hLoginHandle) {

        if (!NetSdk.CLIENT_ControlDevice(m_hLoginHandle, NetSDKLib.CtrlType.CTRLTYPE_CTRL_REBOOT, null, 3000)) {
            System.err.println("ControlDevice Reboot Failed!" + ENUMERROR.getErrorCode());
            return false;
        }
        return true;
    }

    // 关闭当前设备
    public static boolean DeviceShutdown(NetSDKLib.LLong m_hLoginHandle) {

        if (!NetSdk.CLIENT_ControlDevice(m_hLoginHandle, NetSDKLib.CtrlType.CTRLTYPE_CTRL_SHUTDOWN, null, 3000)) {
            System.err.println("ControlDevice Shutdown Failed!" + ENUMERROR.getErrorCode());
            return false;
        }
        return true;
    }

    // 远程关机
    public static boolean RemoteShutDown(NetSDKLib.LLong m_hLoginHandle) {
        if (!NetSdk.CLIENT_ShutDownDev(m_hLoginHandle)) {
            System.err.println("ShutDownDev Shutdown Failed!" + ENUMERROR.getErrorCode());
            return false;
        }
        return true;
    }
}
