package com.netsdk.demo.customize.courseRecord;

import com.netsdk.demo.customize.courseRecord.frame.CourseRecordMainFrame;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Native;

import java.awt.*;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/28 14:10
 */
public class CourseRecordRealPlay {

    private CourseRecordMainFrame mainFrame;

    private Panel playWindow;

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    public CourseRecordRealPlay(CourseRecordMainFrame Frame, Panel playWindow) {
        this.mainFrame = Frame;
        this.playWindow = playWindow;
    }

    public NetSDKLib.LLong m_hLiveSteam = new NetSDKLib.LLong(0);

    public void play(int channel) {
        int playType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay; // 实时预览

        m_hLiveSteam = netsdk.CLIENT_RealPlayEx(mainFrame.courseRecordLogon.m_hLoginHandle,
                channel, Native.getComponentPointer(playWindow), playType);

        if (m_hLiveSteam.longValue() == 0) {
            System.err.println("开始实时监视失败! " + ToolKits.getErrorCode());
        } else {
            System.out.println("Start RealPlay Succeed!");
            playWindow.setVisible(true);
        }
    }

    //结束调用
    public void stopPlay() {
        if (m_hLiveSteam.longValue() == 0) {
            System.err.println("Please make sure the RealPlay Handle is valid!");
            return;
        }
        if (!netsdk.CLIENT_StopRealPlayEx(m_hLiveSteam)) {
            System.err.println("Stop RealPlay Failed");
        } else {
            System.out.println("Stop RealPlay Succeed!");
            m_hLiveSteam.setValue(0);
            playWindow.repaint();
        }
    }
}
