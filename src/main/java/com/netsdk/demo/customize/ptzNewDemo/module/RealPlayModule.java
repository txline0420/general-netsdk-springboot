package com.netsdk.demo.customize.ptzNewDemo.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;

/**
 * 通用预览接口
 *
 * @author 47040
 * @since Created in 2021/3/25 19:49
 */
public class RealPlayModule {

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;

    // 开始预览
    public NetSDKLib.LLong realPlay(NetSDKLib.LLong m_hLoginHandle, int channel, int playType, Pointer pWinHandle) {

        NetSDKLib.LLong m_hPlayHandle = NetSdk.CLIENT_RealPlayEx(m_hLoginHandle, channel, pWinHandle, playType);

        if (m_hPlayHandle.longValue() == 0) {
            System.err.println("开始实时监视失败，错误码" + String.format("[0x%x]", NetSdk.CLIENT_GetLastError()));
        } else {
            System.out.println("Success to start RealPlay");
        }
        return m_hPlayHandle;
    }

    // 结束预览
    public boolean stopPlay(NetSDKLib.LLong m_hPlayHandle) {
        System.out.println("Stopping RealPlay...");
        if (m_hPlayHandle.longValue() == 0) {
            System.err.println("Stop RealPlay failed. Please make sure the RealPlay Handle is valid!");
            return false;
        }

        boolean ret = NetSdk.CLIENT_StopRealPlayEx(m_hPlayHandle);
        if (!ret) {
            System.err.println("StopRealPlay Failed: " + ToolKits.getErrorCode());
        } else {
            System.out.println("StopRealPlay Succeed!");
            m_hPlayHandle.setValue(0);
        }
        return ret;
    }

}
