package com.netsdk.demo.customize.courseRecord.frame;

import com.netsdk.demo.customize.courseRecord.CourseRecordInit;
import com.netsdk.demo.customize.courseRecord.CourseRecordLogon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CourseRecordLogonFrame extends JFrame {

    public CourseRecordLogon courseRecordLogon;

    private JPanel logonPane;
    private JTextField ipField;      // ip
    private JTextField portField;    // port
    private JTextField userField;    // username
    private JPasswordField pwdField; // password

    private JButton btnLogon;
    private JButton btnOpenDemo;

    /**
     * Create the frame.
     */
    public CourseRecordLogonFrame(CourseRecordLogon recordLogon) {

        courseRecordLogon = recordLogon;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("设备登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 760, 166);
        logonPane = new JPanel();
        logonPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(logonPane);
        logonPane.setLayout(null);

        JPanel panel = new JPanel();
        panel.setBounds(10, 31, 704, 27);
        logonPane.add(panel);
        panel.setLayout(new GridLayout(0, 8, 0, 0));

        JLabel lblIP = new JLabel("设备IP: ");
        lblIP.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(lblIP);

        ipField = new JTextField();
        ipField.setText(courseRecordLogon.m_strIpAddr);
        panel.add(ipField);
        ipField.setColumns(10);

        JLabel lblPort = new JLabel("设备端口: ");
        lblPort.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(lblPort);

        portField = new JTextField();
        portField.setText(String.valueOf(courseRecordLogon.m_nPort));
        panel.add(portField);
        portField.setColumns(10);

        JLabel lblUsername = new JLabel("用户名: ");
        lblUsername.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(lblUsername);

        userField = new JTextField();
        userField.setText(courseRecordLogon.m_strUser);
        panel.add(userField);
        userField.setColumns(10);

        JLabel lblPwd = new JLabel("密码: ");
        lblPwd.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(lblPwd);

        pwdField = new JPasswordField();
        pwdField.setText(courseRecordLogon.m_strPassword);
        panel.add(pwdField);

        btnLogon = new JButton("登录");
        btnLogon.setBounds(233, 82, 93, 23);
        logonPane.add(btnLogon);

        btnOpenDemo = new JButton("打开Demo");
        btnOpenDemo.setEnabled(false);
        btnOpenDemo.setBounds(404, 82, 93, 23);
        logonPane.add(btnOpenDemo);

        btnLogon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (courseRecordLogon.m_hLoginHandle.longValue() == 0) {
                    courseRecordLogon.m_strIpAddr = ipField.getText().trim();
                    courseRecordLogon.m_nPort = Integer.parseInt(portField.getText().trim());
                    courseRecordLogon.m_strUser = userField.getText().trim();
                    courseRecordLogon.m_strPassword = new String(pwdField.getPassword());
                    courseRecordLogon.loginWithHighLevel();
                    if (courseRecordLogon.m_hLoginHandle.longValue() == 0) {
                        JOptionPane.showMessageDialog(null, "登录失败", "错误信息", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    btnLogon.setText("退出");
                    btnOpenDemo.setEnabled(true);
                } else {
                    courseRecordLogon.logOut();
                    btnLogon.setText("登录");
                    btnOpenDemo.setEnabled(false);
                }
            }
        });

        // 登录按钮 登录/登出
        btnOpenDemo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            Frame owner = (Frame) SwingUtilities.getWindowAncestor(btnOpenDemo);
                            CourseRecordMainFrame mainFrame = new CourseRecordMainFrame((CourseRecordLogonFrame) owner);
                            mainFrame.setLocationRelativeTo(null);
                            mainFrame.setVisible(true);
                            owner.setVisible(false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 注册窗体清出事件 退出/清理资源
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (courseRecordLogon.m_hLoginHandle.longValue() != 0) {
                    courseRecordLogon.logOut();
                }
                CourseRecordInit.Cleanup();  // 清理 SDK
                dispose();
            }
        });
    }
}
