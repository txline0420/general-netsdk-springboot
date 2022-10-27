package com.netsdk.demo.customize.ptzNewDemo.frame.basic;

import com.netsdk.demo.customize.ptzNewDemo.frame.SwingUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 登录面板
 *
 * @author 47040
 * @since Created in 2021/3/25 17:09
 */
public class LoginPanel extends JPanel {

    private final JButton loginBtn = new JButton("登录");
    private final JButton logoutBtn = new JButton("登出");

    private final JTextField ipTextArea = new JTextField("", 15);
    private final JTextField portTextArea = new JTextField("", 8);
    private final JTextField nameTextArea = new JTextField("", 12);
    private final JPasswordField passwordTextArea = new JPasswordField("", 12);

    public LoginPanel(String ipAddress, Integer port, String userName, String password) {
        SwingUtil.setBorderEx(this, "登录", 2);
        setLayout(new FlowLayout());

        JLabel ipLabel = new JLabel("设备地址");
        ipTextArea.setText(ipAddress);

        JLabel portLabel = new JLabel("端口号");
        portTextArea.setText(String.valueOf(port));

        JLabel nameLabel = new JLabel("用户名");
        nameTextArea.setText(userName);

        JLabel passwordLabel = new JLabel("密码");
        passwordTextArea.setText(password);

        add(ipLabel);
        add(ipTextArea);
        add(portLabel);
        add(portTextArea);
        add(nameLabel);
        add(nameTextArea);
        add(passwordLabel);
        add(passwordTextArea);
        add(loginBtn);
        add(logoutBtn);

        logoutBtn.setEnabled(false);
    }

    public JButton getLoginBtn() {
        return loginBtn;
    }

    public JButton getLogoutBtn() {
        return logoutBtn;
    }

    public String getIpAddress() {
        return ipTextArea.getText().trim();
    }

    public Integer getPort() {
        return Integer.parseInt(portTextArea.getText().trim());
    }

    public String getUsername() {
        return nameTextArea.getText().trim();
    }

    public String getPassword() {
        return new String(passwordTextArea.getPassword());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
