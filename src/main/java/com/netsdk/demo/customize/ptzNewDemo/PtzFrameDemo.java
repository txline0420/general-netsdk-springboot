package com.netsdk.demo.customize.ptzNewDemo;

import com.netsdk.demo.customize.ptzNewDemo.frame.PtzMainFrame;
import com.netsdk.demo.customize.ptzNewDemo.module.*;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Native;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * PTZ Swing Demo
 * 相比于 原来的云台 Demo, 本 Demo 对 Swing 界面和 SDK 接口做了拆分
 * {@link com.netsdk.demo.customize.PtzControl}
 * 最后再通过事件注册把两者链接起来
 * <p>
 * 读者只需要简单看下哪些按钮对应哪些事件注册函数，不需要关注任何 Swing 界面的细节
 * <p>
 * SDK 的接口代码被二次封装在 module 包内:
 * 初始化相关接口 {@link SdkUtilModule}
 * 登录相关接口 {@link LogonModule}
 * PTZ 控制相关接口 {@link PtzControlModule}
 * PTZ 配置相关接口 {@link PtzQueryModule}
 * <p>
 * PTZ关于精确定位有多个接口，各型号 PTZ 是否支持不尽相同
 * BaseMoveAbsolutely 精确绝对移动：这个精确度最高，坐标参数放大10倍，同时可以传入精准的倍率参数
 * 但由于是定制功能，在能力集中也没有相应标志位，是否支持只有下发过不报错才知道，建议在明确设备程序支持后再使用。
 * MoveAbsolutely 绝对移动：坐标参数10倍，但只能传入相对倍率，精度稍低，另外是否支持可以从能力集中得知。
 * ExactGoto 三维精确定位：这是最早的定位接口，基本所有PTZ设备都支持
 * 但部分型号实现的效果不太好，越是放大倍率上限低的设备，收/发数据间的误差越大，建议只有上两者均不支持时才使用。
 *
 * @author 47040
 * @since Created in 2021/3/25 17:30
 */
public class PtzFrameDemo {

    private final PtzMainFrame frame;

    private final LogonModule logonModule = new LogonModule();

    private final RealPlayModule realPlayModule = new RealPlayModule();

    public PtzFrameDemo() {

        // sdk 初始化
        SdkUtilModule.init();

        frame = new PtzMainFrame(m_strIp, m_nPort, m_strUser, m_strPassword);
        // 主界面退出 事件
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println("Window Closing");
                realPlayModule.stopPlay(m_hPlayHandle);  // 停止播放
                logonModule.logout(m_hLoginHandle);      // 设备登出
                SdkUtilModule.cleanup();                 // 清理 SDK 资源
                frame.dispose();
                System.exit(0);
            }
        });

        /////////////// 登录相关 事件 ///////////////
        ///////////////////////////////////////////

        // 登入按钮 事件
        frame.getLoginPanel().getLoginBtn().addActionListener(e -> {
            m_strIp = frame.getLoginPanel().getIpAddress();
            m_nPort = frame.getLoginPanel().getPort();
            m_strUser = frame.getLoginPanel().getUsername();
            m_strPassword = frame.getLoginPanel().getPassword();
            // 登录
            m_hLoginHandle = logonModule.login(m_strIp, m_nPort, m_strUser, m_strPassword, m_stDeviceInfo);
            if (m_hLoginHandle.longValue() == 0) {
                JOptionPane.showMessageDialog(frame, "登录失败，错误码 ：" + String.format("[%s]", ToolKits.getErrorCode()));
                return;
            }
            frame.getLoginPanel().getLoginBtn().setEnabled(false); // 更新登录按钮状态
            frame.getLoginPanel().getLogoutBtn().setEnabled(true); // 更新登出按钮状态
            frame.getRealPlayControlPanel().setEnabled(true);      // 启用播放控制组件
            frame.getControlPanel().setEnabled(true);              // 启用PTZ控制组件
            frame.getRealPlayControlPanel().resetChannelComboBox(m_stDeviceInfo.byChanNum); // 更新通道下拉框
        });
        // 登出按钮 事件
        frame.getLoginPanel().getLogoutBtn().addActionListener(e -> {
            realPlayModule.stopPlay(m_hPlayHandle);               // 停止播放
            frame.getRealPlayPanel().getCanvasPanel().repaint();  // 刷新画布
            frame.getRealPlayControlPanel().setEnabled(false);    // 禁用播放控制组件
            frame.getControlPanel().setEnabled(false);            // 禁用PTZ控制组件
            logonModule.logout(m_hLoginHandle);
            if (m_hLoginHandle.longValue() == 0) {    // 登出成功则更新 登录/登出 按钮状态
                frame.getLoginPanel().getLoginBtn().setEnabled(true);
                frame.getLoginPanel().getLogoutBtn().setEnabled(false);
            }
        });

        /////////////// 播放控制 事件 ///////////////
        ///////////////////////////////////////////
        // 通道号切换 切换通道下拉框选项时触发
        frame.getRealPlayControlPanel().getChannelComboBox().addActionListener(e -> {
            channel = ((JComboBox<?>) e.getSource()).getSelectedIndex();
            System.out.println("selected channel changed , now it is : " + channel);
        });
        // 开始播放 事件
        frame.getRealPlayControlPanel().getPlayBtn().addActionListener(e -> {
            // 开始预览
            m_hPlayHandle = realPlayModule.realPlay(m_hLoginHandle, channel, playType,
                    Native.getComponentPointer(frame.getRealPlayPanel().getCanvasPanel()));
            if (m_hPlayHandle.longValue() == 0) {
                JOptionPane.showMessageDialog(frame, "播放失败，错误码 ：" + String.format("[%s]", ToolKits.getErrorCode()));
            }
        });
        // 停止播放 事件
        frame.getRealPlayControlPanel().getStopPlayBtn().addActionListener(e -> {
            realPlayModule.stopPlay(m_hPlayHandle);               // 停止播放
            frame.getRealPlayPanel().getCanvasPanel().repaint();  // 刷新画布
        });

        //////////// 方向控制相关 事件 ////////////
        /////////////////////////////////////////
        // 向上
        frame.getControlPanel().getPtzDirControlPanel().getUpBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzDirControlPanel().getSpeed();
                PtzControlModule.ptzControlUpStart(m_hLoginHandle, channel, 0, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlUpEnd(m_hLoginHandle, channel);
            }
        });
        // 左上
        frame.getControlPanel().getPtzDirControlPanel().getLeftUpBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzDirControlPanel().getSpeed();
                PtzControlModule.ptzControlLeftUpStart(m_hLoginHandle, channel, speedLevel, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlLeftUpEnd(m_hLoginHandle, channel);
            }
        });
        // 右上
        frame.getControlPanel().getPtzDirControlPanel().getRightUpBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzDirControlPanel().getSpeed();
                PtzControlModule.ptzControlRightUpStart(m_hLoginHandle, channel, speedLevel, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlRightUpEnd(m_hLoginHandle, channel);
            }
        });
        // 向下
        frame.getControlPanel().getPtzDirControlPanel().getDownBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzDirControlPanel().getSpeed();
                PtzControlModule.ptzControlDownStart(m_hLoginHandle, channel, 0, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlDownEnd(m_hLoginHandle, channel);
            }
        });
        // 左下
        frame.getControlPanel().getPtzDirControlPanel().getLeftDownBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzDirControlPanel().getSpeed();
                PtzControlModule.ptzControlLeftDownStart(m_hLoginHandle, channel, speedLevel, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlLeftDownEnd(m_hLoginHandle, channel);
            }
        });
        // 右下
        frame.getControlPanel().getPtzDirControlPanel().getRightDownBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzDirControlPanel().getSpeed();
                PtzControlModule.ptzControlRightDownStart(m_hLoginHandle, channel, speedLevel, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlRightDownEnd(m_hLoginHandle, channel);
            }
        });
        // 左
        frame.getControlPanel().getPtzDirControlPanel().getLeftBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzDirControlPanel().getSpeed();
                PtzControlModule.ptzControlLeftStart(m_hLoginHandle, channel, 0, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlLeftEnd(m_hLoginHandle, channel);
            }
        });
        // 右
        frame.getControlPanel().getPtzDirControlPanel().getRightBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzDirControlPanel().getSpeed();
                PtzControlModule.ptzControlRightStart(m_hLoginHandle, channel, 0, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlRightEnd(m_hLoginHandle, channel);
            }
        });

        //////////// 变焦、变倍 相关事件 ///////////
        //////////////////////////////////////////
        // 变倍 增大
        frame.getControlPanel().getPtzModifyPanel().getZoomAddBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzModifyPanel().getSpeed();
                PtzControlModule.ptzControlZoomAddStart(m_hLoginHandle, channel, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlZoomAddEnd(m_hLoginHandle, channel);
            }
        });
        // 变倍 减小
        frame.getControlPanel().getPtzModifyPanel().getZoomDecBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzModifyPanel().getSpeed();
                PtzControlModule.ptzControlZoomDecStart(m_hLoginHandle, channel, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlZoomDecEnd(m_hLoginHandle, channel);
            }
        });
        // 变焦 增大
        frame.getControlPanel().getPtzModifyPanel().getFocusAddBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzModifyPanel().getSpeed();
                PtzControlModule.ptzControlFocusAddStart(m_hLoginHandle, channel, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlFocusAddEnd(m_hLoginHandle, channel);
            }
        });
        // 变焦 减小
        frame.getControlPanel().getPtzModifyPanel().getFocusDecBtn().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int speedLevel = frame.getControlPanel().getPtzModifyPanel().getSpeed();
                PtzControlModule.ptzControlFocusDecStart(m_hLoginHandle, channel, speedLevel);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PtzControlModule.ptzControlFocusDecEnd(m_hLoginHandle, channel);
            }
        });

        //////////// 精确绝对移动控制 (BaseMoveAbsolutely 协议)相关事件 ///////////
        ///////////////////////////////////////////////////////////////////
        // 获取位置
        frame.getControlPanel().getPtzBaseMoveAbsolutePanel().getGetLocationBtn().addActionListener(e -> {
            NetSDKLib.NET_PTZ_LOCATION_INFO locationInfo = PtzQueryModule.ptzQueryPTZLocationStatus(m_hLoginHandle, channel);
            if (locationInfo != null) {
                frame.getControlPanel().getPtzBaseMoveAbsolutePanel().setPtzMoveAbsoluteParams(
                        locationInfo.nPTZPan,
                        locationInfo.nPTZTilt,
                        locationInfo.nZoomMapValue,   // 变倍映射值
                        locationInfo.nFocusMapValue); // 聚焦映射值
            }
        });
        // 精确绝对控制
        frame.getControlPanel().getPtzBaseMoveAbsolutePanel().getPtzMoveAbsoluteBtn().addActionListener(e -> {
            int X = frame.getControlPanel().getPtzBaseMoveAbsolutePanel().getXParam();          // X Param 10倍参数
            int Y = frame.getControlPanel().getPtzBaseMoveAbsolutePanel().getYParam();          // Y Param 10被参数
            int Z = frame.getControlPanel().getPtzBaseMoveAbsolutePanel().getZoomMapValue();    // ZoomMapValue 变倍映射值
            boolean ret = PtzControlModule.ptzControlBaseMoveAbsolutely(m_hLoginHandle, channel, X, Y, Z);
            if (!ret) {
                JOptionPane.showMessageDialog(frame, "精确绝对控制失败:" + ToolKits.getErrorCode(), "下发配置", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(frame, "精确绝对控制成功", "下发配置", JOptionPane.INFORMATION_MESSAGE);
        });
        // 设聚焦值
        frame.getControlPanel().getPtzBaseMoveAbsolutePanel().getPtzSetFocusMapBtn().addActionListener(e -> {
            int focusMapValue = frame.getControlPanel().getPtzBaseMoveAbsolutePanel().getFocusMapValue();  // ZoomMapValue 聚焦映射值
            boolean ret = PtzControlModule.ptzControlSetFocusMapValue(m_hLoginHandle, channel, focusMapValue);
            if (!ret) {
                JOptionPane.showMessageDialog(frame, "设置聚焦值失败:" + ToolKits.getErrorCode(), "下发配置", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(frame, "设置聚焦值成功", "下发配置", JOptionPane.INFORMATION_MESSAGE);
        });
        //////////// 绝对移动控制 (MoveAbsolutely 协议)相关事件 ///////////
        ////////////////////////////////////////////////////////////////
        // 获取位置
        frame.getControlPanel().getPtzMoveAbsolutePanel().getLocationBtn().addActionListener(e -> {
            NetSDKLib.NET_PTZ_LOCATION_INFO locationInfo = PtzQueryModule.ptzQueryPTZLocationStatus(m_hLoginHandle, channel);
            if (locationInfo != null) {
                frame.getControlPanel().getPtzMoveAbsolutePanel().setPtzMoveAbsoluteParams(
                        locationInfo.nPTZPan,
                        locationInfo.nPTZTilt,
                        locationInfo.nPTZZoom);
            }
        });
        // 精确控制
        frame.getControlPanel().getPtzMoveAbsolutePanel().getPtzMoveAbsoluteBtn().addActionListener(e -> {
            int X = frame.getControlPanel().getPtzMoveAbsolutePanel().getXParam();
            int Y = frame.getControlPanel().getPtzMoveAbsolutePanel().getYParam();
            int Z = frame.getControlPanel().getPtzMoveAbsolutePanel().getZoomParam();
            PtzControlModule.ptzControlMoveAbsolutely(m_hLoginHandle, channel, X, Y, Z);
        });
        // 查询能力
        frame.getControlPanel().getPtzMoveAbsolutePanel().getPtzMoveAbsoluteCheck().addActionListener(e -> {
            Boolean ret = PtzQueryModule.ptzQueryCapsForMoveAbsolutely(m_hLoginHandle, channel);
            if (ret == null) {
                JOptionPane.showMessageDialog(frame, "查询绝对移动(MoveAbsolutely)能力失败:" + ToolKits.getErrorCode(), "查询能力", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ret) {
                JOptionPane.showMessageDialog(frame, "设备不支持绝对移动(MoveAbsolutely)", "查询能力", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(frame, "设备支持绝对移动(MoveAbsolutely)", "查询能力", JOptionPane.INFORMATION_MESSAGE);
        });

        //////////// 三维定位(ExactGoto 协议)相关事件 ///////////
        //////////////////////////////////////////////////////
        // 位置获取
        frame.getControlPanel().getPtzExactGotoPanel().getLocationBtn().addActionListener(e -> {
            NetSDKLib.NET_PTZ_LOCATION_INFO locationInfo = PtzQueryModule.ptzQueryPTZLocationStatus(m_hLoginHandle, channel);
            if (locationInfo != null) {
                frame.getControlPanel().getPtzExactGotoPanel().setExactGotoParams(
                        locationInfo.nPTZPan,
                        locationInfo.nPTZTilt,
                        locationInfo.nPTZZoom);
            }
        });
        // 精确控制
        frame.getControlPanel().getPtzExactGotoPanel().getPtzExactGotoBtn().addActionListener(e -> {
            int X = frame.getControlPanel().getPtzExactGotoPanel().getXParam();
            int Y = frame.getControlPanel().getPtzExactGotoPanel().getYParam();
            int Z = frame.getControlPanel().getPtzExactGotoPanel().getZoomParam();
            PtzControlModule.ptzControlExactGotoControl(m_hLoginHandle, channel, X, Y, Z);
        });
        // 确定能力
        frame.getControlPanel().getPtzExactGotoPanel().getPtzExactCheckBtn().addActionListener(e -> {
            Boolean ret = PtzQueryModule.ptzQueryCapsForExactGoto(m_hLoginHandle, channel);
            if (ret == null) {
                JOptionPane.showMessageDialog(frame, "查询三维精确定位(ExactGoto)能力失败:" + ToolKits.getErrorCode(), "查询能力", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ret) {
                JOptionPane.showMessageDialog(frame, "设备不支持三维精确定位(ExactGoto)", "查询能力", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(frame, "设备支持三维精确定位(ExactGoto)", "查询能力", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // 设备信息
    private final NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    // 登录句柄
    private static NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0);

    // 播放句柄
    private static NetSDKLib.LLong m_hPlayHandle = new NetSDKLib.LLong(0);

    // 预览通道号
    int channel = 0;

    // 预览类型
    int playType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay; // 实时预览模式

    ////////////////// 登陆参数 //////////////////
    /////////////////////////////////////////////
    private String m_strIp = "172.32.0.45";
    private Integer m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PtzFrameDemo::new);
    }
}
