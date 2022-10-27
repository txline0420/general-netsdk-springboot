package com.netsdk.demo.customize.surfaceEventDemo.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Callback;

/**
 * @author 47040
 * @since Created in 2021/5/11 14:09
 */
public class AttachModule {

    private static final NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;

    public NetSDKLib.LLong AttachEventRealLoadPic(NetSDKLib.LLong m_hLoginHandle, int channel, Callback analyzerDataCB) {
        int bNeedPicture = 1;   // 需要图片

        NetSDKLib.LLong m_hAttachHandle = NetSdk.CLIENT_RealLoadPictureEx(m_hLoginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, analyzerDataCB, null, null);
        if (m_hAttachHandle.longValue() != 0) {
            System.out.printf("Chn[%3d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Ch[%3d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel, ToolKits.getErrorCode());
        }
        return m_hAttachHandle;
    }

    /**
     * 停止侦听智能事件
     */
    public NetSDKLib.LLong DetachEventRealLoadPic(NetSDKLib.LLong m_hAttachHandle) {
        if (m_hAttachHandle.longValue() != 0) {
            NetSdk.CLIENT_StopLoadPic(m_hAttachHandle);
            System.out.print("CLIENT_StopLoadPictureEx Success\n");
            m_hAttachHandle.setValue(0);
        }
        return m_hAttachHandle;
    }
}
