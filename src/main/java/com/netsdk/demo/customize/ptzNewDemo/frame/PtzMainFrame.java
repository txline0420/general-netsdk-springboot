package com.netsdk.demo.customize.ptzNewDemo.frame;

import com.netsdk.demo.customize.ptzNewDemo.frame.basic.LoginPanel;

import javax.swing.*;
import java.awt.*;

/**
 * PTZ 主界面
 *
 * @author 47040
 * @since Created in 2021/3/25 17:32
 */
public class PtzMainFrame extends JFrame {

    // 登录组件
    private final LoginPanel loginPanel;
    // 播放组件
    private final RealPlayPanel realPlayPanel;
    // 播放控制组件
    private final RealPlayControlPanel realPlayControlPanel;
    // 控制组件
    private final PtzControlPanel controlPanel;

    public PtzMainFrame(String ipAddress, Integer port, String userName, String password) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("云台控制 Demo");
        setMinimumSize(new Dimension(1280, 720));
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setVisible(true);

        loginPanel = new LoginPanel(ipAddress, port, userName, password);
        add(loginPanel, BorderLayout.NORTH);

        realPlayPanel = new RealPlayPanel();
        add(realPlayPanel, BorderLayout.CENTER);

        JPanel placeHolder = new JPanel();
        placeHolder.setLayout(new BorderLayout());

        realPlayControlPanel = new RealPlayControlPanel();
        controlPanel = new PtzControlPanel();
        placeHolder.add(realPlayControlPanel, BorderLayout.NORTH);
        placeHolder.add(controlPanel, BorderLayout.CENTER);

        add(placeHolder, BorderLayout.EAST);

        realPlayControlPanel.setEnabled(false);
        controlPanel.setEnabled(false);
    }

    public LoginPanel getLoginPanel() {
        return loginPanel;
    }

    public RealPlayPanel getRealPlayPanel() {
        return realPlayPanel;
    }

    public RealPlayControlPanel getRealPlayControlPanel() {
        return realPlayControlPanel;
    }

    public PtzControlPanel getControlPanel() {
        return controlPanel;
    }
}
