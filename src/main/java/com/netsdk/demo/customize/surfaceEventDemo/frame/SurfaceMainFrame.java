package com.netsdk.demo.customize.surfaceEventDemo.frame;

import com.netsdk.demo.customize.surfaceEventDemo.frame.basic.GroupListPanel;
import com.netsdk.demo.customize.surfaceEventDemo.frame.basic.LoginPanel;

import javax.swing.*;
import java.awt.*;

/**
 * PTZ 主界面
 *
 * @author 47040
 * @since Created in 2021/3/25 17:32
 */
public class SurfaceMainFrame extends JFrame {

    // 登录组件
    private final LoginPanel loginPanel;
    // 播放组件
    private final RealPlayPanel realPlayPanel;
    // 控制组件
    private final ControlPanel controlPanel;
    // 事件图片组件
    private final EventPicPanel eventPicPanel;
    // 事件列表组件
    private final GroupListPanel groupListPanel;
    // 事件详情组
    private final EventGroupPanel eventGroupPanel;

    public SurfaceMainFrame(String ipAddress, Integer port, String userName, String password) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("水面监测事件 Demo");
        setMinimumSize(new Dimension(1366, 768));
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setVisible(true);

        JPanel placeHolderLeft = new JPanel();
        placeHolderLeft.setLayout(new BorderLayout());

        loginPanel = new LoginPanel(ipAddress, port, userName, password);
        placeHolderLeft.add(loginPanel, BorderLayout.NORTH);

        realPlayPanel = new RealPlayPanel();
        placeHolderLeft.add(realPlayPanel, BorderLayout.CENTER);

        JPanel placeHolderInner = new JPanel();
        placeHolderInner.setLayout(new BorderLayout());

        controlPanel = new ControlPanel();
        controlPanel.setEnabled(false);
        eventPicPanel = new EventPicPanel();
        placeHolderInner.add(controlPanel, BorderLayout.NORTH);
        placeHolderInner.add(eventPicPanel, BorderLayout.CENTER);
        placeHolderLeft.add(placeHolderInner, BorderLayout.EAST);

        groupListPanel = new GroupListPanel(
                new String[]{"EventID", "通道", "事件", "UTC", "详情"},
                new int[]{10, 10, 50, 80, 480}
        );
        placeHolderLeft.add(groupListPanel, BorderLayout.SOUTH);
        eventGroupPanel = new EventGroupPanel("事件组");

        add(placeHolderLeft, BorderLayout.CENTER);
        add(eventGroupPanel, BorderLayout.EAST);
    }

    public LoginPanel getLoginPanel() {
        return loginPanel;
    }

    public RealPlayPanel getRealPlayPanel() {
        return realPlayPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public EventPicPanel getEventPicPanel() {
        return eventPicPanel;
    }

    public GroupListPanel getGroupListPanel() {
        return groupListPanel;
    }

    public EventGroupPanel getEventGroupPanel() {
        return eventGroupPanel;
    }

    public void clearInfoAndDisableControl() {
        this.getGroupListPanel().clearTableContent();
        this.getEventPicPanel().clearPic();
        this.getEventGroupPanel().clearAll();
        this.getControlPanel().setEnabled(false);
    }
}
