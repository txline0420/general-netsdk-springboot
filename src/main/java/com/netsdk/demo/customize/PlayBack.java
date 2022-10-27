package com.netsdk.demo.customize;


import com.netsdk.lib.LastError;
import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * @author 291189
 * @version 1.0
 * @description  时间回放
 * @date 2021/12/15 14:20
 */
 class NewPlayBackFrame {
    static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;

    private SDKEnvironment sdkEnv;

    // 登录参数
    private String m_strIp             = "172.8.88.220";
    private Integer m_nPort            = new Integer("37777");
    private String m_strUser           = "admin";
    private String m_strPassword       = "admin123";
    // 设备信息
    private NetSDKLib.NET_DEVICEINFO m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO();

    private boolean m_playFlag = false; // 播放标志位
    private boolean m_pauseFlag = true; // 暂停标志位
    private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登录句柄
    private NetSDKLib.LLong m_hPlayHandle = new NetSDKLib.LLong(0);  // 回放句柄
    private NetSDKLib.LLong m_hDownLoadHandle = new NetSDKLib.LLong(0); // 下载录像句柄

    private int m_streamType; // 码流类型
    private int m_recordType; // 录像类型
    private Integer m_channel = new Integer(0); // 通道号
    private NetSDKLib.NET_TIME m_startTime = new NetSDKLib.NET_TIME(); // 开始时间
    private NetSDKLib.NET_TIME m_stopTime = new NetSDKLib.NET_TIME(); // 结束时间

    private DownLoadPosCallBack m_PlayBackDownLoadPos = new DownLoadPosCallBack(); // 回放数据下载进度
    private TimeDownLoadPosCallBack m_DownLoadPos = new TimeDownLoadPosCallBack(); // 录像下载进度
    private TimeDownLoadDataCallBack m_DownLoadData = new TimeDownLoadDataCallBack(); // 录像下载数据回调

    /**
     * PlayDemo 构造
     */
    public NewPlayBackFrame() {
        sdkEnv = new SDKEnvironment();
        sdkEnv.init();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                InitPlayBackFrame("PlayBackByTime");
            }
        });
    }

    public void InitPlayBackFrame(String title) {
        playFrame = new JFrame(title);
        playFrame.setSize(800, 650);
        playFrame.setLayout(new BorderLayout());
        playFrame.setLocationRelativeTo(null);
        playFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        playFrame.setVisible(true);

        loginJPanel = new LoginPanel(); // 登录面板
        controlPanel = new ControlPanel(); // 主控制面板
        playWindow = new PlayWindow(); // 播放面板

        playFrame.add(controlPanel, BorderLayout.EAST);
        playFrame.add(loginJPanel, BorderLayout.NORTH);
        playFrame.add(playWindow, BorderLayout.CENTER);

        WindowAdapter closeWindowAdapter =  new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println("PlayBack Window Closing");
                LoginOutButtonPerformed(null);
                sdkEnv.cleanup();
                playFrame.dispose();
            }
        };
        playFrame.addWindowListener(closeWindowAdapter);
    }

    /**
     * 登录面板
     */
    private class LoginPanel extends JPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public LoginPanel() {
            loginBtn = new JButton("登入");
            logoutBtn = new JButton("登出");
            nameLabel = new JLabel("用户名");
            passwordLabel = new JLabel("密码");
            nameTextArea = new JTextField(m_strUser, 8);
            passwordTextArea = new JPasswordField(m_strPassword, 8);
            ipLabel = new JLabel("设备地址");
            portLabel = new JLabel("端口号");
            ipTextArea = new JTextField(m_strIp, 15);
            portTextArea = new JTextField(String.valueOf(m_nPort.intValue()), 6);

            setLayout(new FlowLayout());
            setBorderEx(this, "登录", 2);

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
            // 登录按钮. 监听事件
            loginBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LoginButtonPerformed(e);
                }
            });

            // 登出按钮. 监听事件
            logoutBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LoginOutButtonPerformed(e);
                }
            });
        }
    }

    /**
     * 主控制面板
     */
    private class ControlPanel extends JPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public ControlPanel() {
            setBorderEx(this, "控制面板", 2);
            Dimension dim = getPreferredSize();
            dim.width = 250;
            setPreferredSize(dim);

            // 设置时间
            ctrlTimeBarPanel = new TimePanel();

            // 录像信息配置
            recordInfoPanel = new RecordInfoPanel();

            // 回放操作
            ctrlPanel = new PlayBackCtrlPanel();

            // 下载操作
            downLoadRecordPanel = new DownLoadRecordPanel();

            JTabbedPane ctrlJTabbedPane = new JTabbedPane();
            ctrlJTabbedPane.addTab("回放", ctrlPanel);
            ctrlJTabbedPane.addTab("下载", downLoadRecordPanel);

            add(ctrlTimeBarPanel, BorderLayout.NORTH);
            add(recordInfoPanel, BorderLayout.CENTER);
            add(ctrlJTabbedPane, BorderLayout.SOUTH);
        }
    }

    /**
     * 控制面板 - 回放操作
     */
    private class PlayBackCtrlPanel extends JPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public PlayBackCtrlPanel() {
            setLayout(new BorderLayout());

            // 播放按钮
            JPanel ctlPanel = new JPanel();
            ctlPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            ctlPanel.setLayout(new GridLayout(0, 1));
            ctlPanel.add(playButton);
            ctlPanel.add(pauseButton);
            ctlPanel.add(normalButton);
            ctlPanel.add(fastButton);
            ctlPanel.add(slowButton);

            /// 按钮操作事件
            playButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // 播放
                    if (!m_playFlag) {
                        StartPlayBack();
                    }else {
                        StopPlayBack();
                    }
                }
            });
            pauseButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PausePlayBack(m_pauseFlag);
                    m_pauseFlag = !m_pauseFlag;
                }
            });
            normalButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    NormalPlayBack();
                }
            });
            fastButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    FastPlayBack();
                }
            });
            slowButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SlowPlayBack();
                }
            });

            add(ctlPanel,BorderLayout.CENTER);
        }
    }

    /**
     * 控制面板 - 录像下载操作
     * 按时间下载录像
     */
    private class DownLoadRecordPanel extends JPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public DownLoadRecordPanel() {
            setBorder(new EmptyBorder(5, 5, 5, 5));
            setLayout(new BorderLayout());

            downRecordButton = new JButton("下载录像");
            stopDonwloadButton = new JButton("停止下载");
            downLoadProgressLabel = new JLabel("进度:");
            downLoadProgressBar = new JProgressBar();

            JPanel progressPanel = new JPanel();
            progressPanel.setLayout(new FlowLayout());
            progressPanel.add(downLoadProgressLabel);
            progressPanel.add(downLoadProgressBar);

            JPanel bottonPanel = new JPanel();
            bottonPanel.setLayout(new GridLayout(0, 1));
            bottonPanel.add(downRecordButton);
            bottonPanel.add(stopDonwloadButton);

            add(bottonPanel, BorderLayout.SOUTH);
            add(progressPanel, BorderLayout.NORTH);

            // 下载按钮事件
            downRecordButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DownLoadRecordFile();
//					DownLoadRecordFileEx();
                }
            });

            // 停止下载事件
            stopDonwloadButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    StopDownLoadRecord();
                }
            });
        }
    }

    /**
     * 控制面板 - 时间配置
     */
    private class TimePanel extends JPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public TimePanel() {
            setBorderEx(this, "时间设置", 2);
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints gc = new GridBagConstraints();
            setLayout(layout);

            String[] timeStrings = {"年", "月", "日", "时", "分", "秒"};
            String[] strStartTime = {"2019", "1", "10", "1", "0", "0"};
            String[] strStopTime = {"2019", "1", "10", "19", "10", "0"};
            for(int i = 0; i < timeStrings.length; ++i) {
                startTimeJLabels[i] = new JLabel(timeStrings[i]);
                stopTimeJLabels[i] = new JLabel(timeStrings[i]);
                startTextFields[i] = new JTextField(strStartTime[i], 4);
                stopTextFields[i] =  new JTextField(strStopTime[i], 4);
            }

            //// first row ////
            gc.gridy = 0;
            gc.gridx = 0;
            gc.fill = GridBagConstraints.NONE;
            Insets lastInsets = new Insets(2, 2, 2, 2);
            gc.insets = lastInsets;
            layout.setConstraints(startLabel, gc);
            add(startLabel);

            //// second row ////
            gc.gridy = 1;
            for (int j = 0; j < 3; j ++) {
                gc.gridx = j*2;
                gc.fill = GridBagConstraints.NONE;
                layout.setConstraints(startTextFields[j], gc);
                add(startTextFields[j]);

                gc.gridx = 2*j + 1;
                layout.setConstraints(startTimeJLabels[j], gc);
                add(startTimeJLabels[j]);
            }

            //// third row ////
            gc.gridy = 2;
            for (int j = 3; j < 6; j ++) {
                gc.gridx = j*2 - 6;
                layout.setConstraints(startTextFields[j], gc);
                add(startTextFields[j]);

                gc.gridx = 2*j + 1 -6;
                layout.setConstraints(startTimeJLabels[j], gc);
                add(startTimeJLabels[j]);
            }

            //// fourth row ///
            gc.gridy = 3;
            gc.gridx = 0;
            layout.setConstraints(stopLabel, gc);
            add(stopLabel);

            gc.gridy = 4;
            for (int j = 0; j < 3; j ++) {
                gc.gridx = j*2;
                layout.setConstraints(stopTextFields[j], gc);
                add(stopTextFields[j]);

                gc.gridx = 2*j + 1;
                layout.setConstraints(stopTimeJLabels[j], gc);
                add(stopTimeJLabels[j]);
            }

            //// third row ////
            gc.gridy = 5;
            for (int j = 3; j < 6; j ++) {
                gc.gridx = j*2 - 6;
                layout.setConstraints(stopTextFields[j], gc);
                add(stopTextFields[j]);

                gc.gridx = 2*j + 1 -6;
                layout.setConstraints(stopTimeJLabels[j], gc);
                add(stopTimeJLabels[j]);
            }
        }
    }

    /**
     * 控制面板 - 录像信息配置
     */
    private class RecordInfoPanel extends JPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public RecordInfoPanel() {
            System.out.println("Record Info : " + getPreferredSize());
            setBorder(new TitledBorder("录像配置"));
            // 参数
            String[] strStreamType = {"0-主辅码流", "1-主码流", "2-辅码流"};
            String[] strRecordType = {"0-所有录像", "1-普通录像", "2-外部报警", "3-动检报警"};

            JPanel paramJPanel = new JPanel();
            paramJPanel.setLayout(new GridLayout(0, 2));
            paramJPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            channelLabel = new JLabel("通道号:");
            chanelTextArea = new JTextField("0", 2);
            streamTypeLabel = new JLabel("码流类型:");
            streamTypeComboBox = new JComboBox(strStreamType);
            recordTypeLabel =  new JLabel("录像类型:");
            recordTypeComboBox = new JComboBox(strRecordType);

            streamTypeComboBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    m_streamType = streamTypeComboBox.getSelectedIndex();
                    System.out.println("Steam ItemEvent: " + m_streamType);
                }
            } );

            recordTypeComboBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    m_recordType = recordTypeComboBox.getSelectedIndex();
                    System.out.println("Record ItemEvent: " + m_recordType);
                }
            } );

            paramJPanel.add(channelLabel);
            paramJPanel.add(chanelTextArea);
            paramJPanel.add(streamTypeLabel);
            paramJPanel.add(streamTypeComboBox);
            paramJPanel.add(recordTypeLabel);
            paramJPanel.add(recordTypeComboBox);

            add(paramJPanel, BorderLayout.CENTER);
        }
    }

    /**
     * 播放面板
     */
    private class PlayWindow extends JPanel {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private Pointer m_win32;
        public PlayWindow() {
            setLayout(new BorderLayout());
            setBorderEx(this, "播放窗口", 2);

            panelPlayBack = new Panel();
            playSlider = new JSlider(0, 100, 0);

            // 播放容器设置
            panelPlayBack.setBackground(new Color(153, 240, 255));
            panelPlayBack.setForeground(new Color(0, 0, 0));
            panelPlayBack.setBounds(10, 20, 390, 300);
            panelPlayBack.setSize(500, 500);

            // 进度条
            playSlider.setPaintLabels(true);
            playSlider.setPaintTicks(true);
            playSlider.setMajorTickSpacing(10);
            playSlider.setMinorTickSpacing(1);
            playSlider.setSnapToTicks(true); // 让滑块滑动刻度的整数处

            JPanel sliderJPanel = new JPanel();
            setBorderEx(sliderJPanel, "播放进度", 2);
            sliderJPanel.setLayout(new BoxLayout(sliderJPanel, BoxLayout.Y_AXIS));
            sliderJPanel.add(playSlider);

            add(sliderJPanel, BorderLayout.SOUTH);
            add(panelPlayBack, BorderLayout.CENTER);
        }

        // 返回 windows 句柄
        public Pointer getHWNDofFrame() {
            m_win32 = Native.getComponentPointer(panelPlayBack);
            return m_win32;
        }
    }

    /**
     * NetSDK 库初始化
     */
    private class SDKEnvironment {

        private boolean bInit = false;
        private boolean bLogopen = false;

        private DisConnect disConnect = new DisConnect();  // 设备断线通知回调
        private HaveReConnect haveReConnect = new HaveReConnect(); // 网络连接恢复

        // 初始化
        public boolean init() {

            // SDK 库初始化, 并设置断线回调
            bInit = NetSdk.CLIENT_Init(disConnect, null);
            if (!bInit) {
                System.err.println("Initialize SDK failed");
                return false;
            }

            // 打开日志，可选
            NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();

            File path = new File(".");
            String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\PlayBackByTime_" + System.currentTimeMillis() + ".log";

            setLog.bSetFilePath = 1;
            System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

            setLog.bSetPrintStrategy = 1;
            setLog.nPrintStrategy = 0;
            bLogopen = NetSdk.CLIENT_LogOpen(setLog);
            if (!bLogopen) {
                System.err.println("Failed to open NetSDK log !!!");
            }

            // 获取版本, 可选操作
            System.out.printf("NetSDK Version [%d]\n", NetSdk.CLIENT_GetSDKVersion());

            // 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
            // 此操作为可选操作，但建议用户进行设置
            NetSdk.CLIENT_SetAutoReconnect(haveReConnect, null);

            // 设置登录超时时间和尝试次数 , 此操作为可选操作
            int waitTime = 5000;   // 登录请求响应超时时间设置为 5s
            int tryTimes = 3;      // 登录时尝试建立链接3次
            NetSdk.CLIENT_SetConnectTime(waitTime, tryTimes);

            // 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
            // 接口设置的登录设备超时时间和尝试次数意义相同
            // 此操作为可选操作
            NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
            netParam.nConnectTime = 10000; // 登录时尝试建立链接的超时时间
            NetSdk.CLIENT_SetNetworkParam(netParam);

            return true;
        }

        // 清除环境
        public void cleanup() {
            if (bLogopen) {
                NetSdk.CLIENT_LogClose();
            }

            if (bInit) {
                NetSdk.CLIENT_Cleanup();
            }
        }

        // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
        public class DisConnect implements NetSDKLib.fDisConnect  {
            public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
            }
        }

        // 网络连接恢复，设备重连成功回调
        // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
        public class HaveReConnect implements NetSDKLib.fHaveReConnect {
            public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
            }
        }
    }

    // 回放进度回调
    public class DownLoadPosCallBack implements NetSDKLib.fDownLoadPosCallBack {
        public void invoke(NetSDKLib.LLong lPlayHandle, int dwTotalSize, int dwDownLoadSize, Pointer dwUser) {
            System.out.println("PlayBack DownLoadCallback: [ " + dwTotalSize + " ]" + " [ " + dwDownLoadSize +" ]");
            if (playPos != dwDownLoadSize) {
                playPos = dwDownLoadSize;
                System.out.println("PlayBack DownLoadCallback: [ " + dwTotalSize + " ]" + " [ " + dwDownLoadSize +" ]");
                System.out.println("Pos " + dwDownLoadSize*100/dwTotalSize);
                playSlider.setValue(dwDownLoadSize*100/dwTotalSize);
                if (-1 == dwDownLoadSize) {
                    JOptionPane.showMessageDialog(playFrame, "回放结束");
                    playSlider.setValue(100);
                }
            }
        }
    }


    // 按时间下载进度回调
    public class TimeDownLoadPosCallBack implements NetSDKLib.fTimeDownLoadPosCallBack {
        public void invoke(NetSDKLib.LLong lLoginID, int dwTotalSize, int dwDownLoadSize, int index, NetSDKLib.NET_RECORDFILE_INFO.ByValue recordfileinfo, Pointer dwUser) {
            if (downloadPos != dwDownLoadSize) {
                downloadPos = dwDownLoadSize;
                System.out.println("Download Pos: [ " + dwTotalSize + " ]" + " [ " + dwDownLoadSize + " ]");
                int pos = dwDownLoadSize*100 / dwTotalSize;
                System.out.println("Current Pos: " + pos);
                downLoadProgressBar.setValue(pos);
                if (-1 == dwDownLoadSize) {
                    StopDownLoadRecord();
                    downLoadProgressBar.setValue(100);
                    System.out.println("Download finished");
                    JOptionPane.showMessageDialog(playFrame, "下载完成");
                }
            }
        }
    }

    // 按时间下载数据回调
    public class TimeDownLoadDataCallBack implements NetSDKLib.fDataCallBack {
        public int invoke(NetSDKLib.LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, Pointer dwUser) {
//    		System.out.println("TimeDownLoadDataCallBack [ " + dwUser +" ]");
            return 0;
        }
    }

    //////////////////////// Border Extends //////////////////////
    /**
     * 设置边框
     */
    private void setBorderEx(JComponent object, String title, int width) {
        Border innerBorder = BorderFactory.createTitledBorder(title);
        Border outerBorder = BorderFactory.createEmptyBorder(width, width, width, width);
        object.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
    }

    //////////////////////// Operations of PlayBackFrame ////////////////
    /**
     * 更新回放参数
     */
    private void updatePlayBackParams() {
        // 通道号 、码流 、录像类型
        m_channel = Integer.parseInt(chanelTextArea.getText());

        m_streamType = streamTypeComboBox.getSelectedIndex();
        m_recordType = recordTypeComboBox.getSelectedIndex();

        // 开始时间
        m_startTime.dwYear = Integer.parseInt(startTextFields[0].getText());
        m_startTime.dwMonth = Integer.parseInt(startTextFields[1].getText());
        m_startTime.dwDay = Integer.parseInt(startTextFields[2].getText());
        m_startTime.dwHour = Integer.parseInt(startTextFields[3].getText());
        m_startTime.dwMinute = Integer.parseInt(startTextFields[4].getText());
        m_startTime.dwSecond = Integer.parseInt(startTextFields[5].getText());
        // 结束时间
        m_stopTime.dwYear = Integer.parseInt(stopTextFields[0].getText());
        m_stopTime.dwMonth = Integer.parseInt(stopTextFields[1].getText());
        m_stopTime.dwDay = Integer.parseInt(stopTextFields[2].getText());
        m_stopTime.dwHour = Integer.parseInt(stopTextFields[3].getText());
        m_stopTime.dwMinute = Integer.parseInt(stopTextFields[4].getText());
        m_stopTime.dwSecond = Integer.parseInt(stopTextFields[5].getText());

        System.out.println("Param m_channel: " + m_channel);
        System.out.println("Param SteamType: " + m_streamType);
        System.out.println("Param RecordType: " + m_recordType);
        System.out.println("Param StartTime: " + m_startTime);
        System.out.println("Param StopTime: " + m_stopTime);
    }

    /**
     * 登录按钮
     */
    private void LoginButtonPerformed(ActionEvent e) {
        m_strIp = ipTextArea.getText();
        m_nPort = Integer.parseInt(portTextArea.getText());
        m_strUser = nameTextArea.getText();
        m_strPassword = new String(passwordTextArea.getPassword());

        System.out.println("设备地址：" + m_strIp
                +  "\n端口号：" + m_nPort
                +  "\n用户名：" + m_strUser
                +  "\n密码：" + m_strPassword);

        // 登录设备
        IntByReference nError = new IntByReference(0);
        m_hLoginHandle = NetSdk.CLIENT_LoginEx(m_strIp, m_nPort.intValue(), m_strUser , m_strPassword , 0, null, m_stDeviceInfo, nError);
        if(m_hLoginHandle.longValue() == 0)
        {
            System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[%x]\n" , m_strIp , m_nPort , NetSdk.CLIENT_GetLastError());
            JOptionPane.showMessageDialog(playWindow, "登录失败");
        }
        else
        {
            System.out.println("Login Success [ " + m_strIp +" ]");
            JOptionPane.showMessageDialog(playWindow, "登录成功");
            logoutBtn.setEnabled(true);
            loginBtn.setEnabled(false);
        }
    }

    /**
     * 登出按钮
     */
    private void LoginOutButtonPerformed(ActionEvent e) {
        if (m_hLoginHandle.longValue() != 0) {
            System.out.println("LogOut Button Action");
            // 确保关闭播放
            StopPlayBack();

            // 确保关闭下载
            StopDownLoadRecord();

            if (NetSdk.CLIENT_Logout(m_hLoginHandle)) {
                System.out.println("Logout Success [ " + m_strIp +" ]");
                m_hLoginHandle.setValue(0);
                logoutBtn.setEnabled(false);
                loginBtn.setEnabled(true);
            }
        }
    }

    /**
     * 开启回放
     */
    private void StartPlayBack() {
        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Please Login First");
            JOptionPane.showMessageDialog(playFrame, "请先登录");
            return;
        }

        // 同一个登录句柄，回放和下载不能同时进行
        if (m_hDownLoadHandle.longValue() != 0) {
            JOptionPane.showMessageDialog(playFrame, "请先停止下载");
            return;
        }

        updatePlayBackParams(); // 更新参数

        // 设置回放时的码流类型
        IntByReference steamType = new IntByReference(m_streamType);// 0-主辅码流,1-主码流,2-辅码流
        int emType = NetSDKLib.EM_USEDEV_MODE.NET_RECORD_STREAM_TYPE;

        boolean bret = NetSdk.CLIENT_SetDeviceMode(m_hLoginHandle, emType, steamType.getPointer());
        if (!bret) {
            System.err.printf("Set Stream Type Failed, Get last error [0x%x]\n", NetSdk.CLIENT_GetLastError());
        }

        // 设置回放时的录像文件类型
        IntByReference emFileType = new IntByReference(m_recordType); // 所有录像 NET_RECORD_TYPE
        emType = NetSDKLib.EM_USEDEV_MODE.NET_RECORD_TYPE;
        bret = NetSdk.CLIENT_SetDeviceMode(m_hLoginHandle, emType, emFileType.getPointer());
        if (!bret) {
            System.err.printf("Set Record Type Failed, Get last error [0x%x]\n", NetSdk.CLIENT_GetLastError());
        }



        m_hPlayHandle = NetSdk.CLIENT_PlayBackByTime(m_hLoginHandle, m_channel.intValue(), m_startTime, m_stopTime,
                playWindow.getHWNDofFrame(), m_PlayBackDownLoadPos, null);
        if (m_hPlayHandle.longValue() == 0) {
            int error = NetSdk.CLIENT_GetLastError();
            System.err.printf("PlayBackByTimeEx Failed, Get last error [0x%x]\n", error);
            switch(error) {
                case LastError.NET_NO_RECORD_FOUND:
                    JOptionPane.showMessageDialog(playFrame, "查找不到录像");
                    break;
                default:
                    JOptionPane.showMessageDialog(playFrame, "开启失败, 错误码：" + String.format("0x%x", error));
                    break;
            }
        }
        else {
            System.out.println("PlayBackByTimeEx Successed");
            m_playFlag = true; // 打开播放标志位
            playButton.setText("停止回放");
            panelPlayBack.repaint();
            panelPlayBack.setVisible(true);
        }
    }

    /**
     * 停止回放
     */
    private void StopPlayBack() {
        if (m_hPlayHandle.longValue() == 0) {
            System.err.println("Please make sure the PlayBack Handle is valid");
            return;
        }

        if (!NetSdk.CLIENT_StopPlayBack(m_hPlayHandle)) {
            System.err.println("StopPlayBack Failed");
            return;
        }

        m_hPlayHandle.setValue(0);
        m_playFlag = false;
        m_pauseFlag = true;
        playPos = 0;
        playButton.setText("开启回放");
        pauseButton.setText("暂停");
        panelPlayBack.repaint();
    }

    /**
     * 回放暂停、播放
     * @param pause true - 暂停; false - 播放
     */
    private void PausePlayBack(boolean pause) {
        if (m_hPlayHandle.longValue() == 0) {
            System.err.println("Please make sure the PlayBack Handle is valid");
            return;
        }

        NetSdk.CLIENT_PausePlayBack(m_hPlayHandle, pause ? 1 : 0); // 1 - 暂停	0 - 恢复
        pauseButton.setText(pause ? "播放":"暂停");
    }

    /**
     * 正常速度播放
     */
    private void NormalPlayBack() {
        if (m_hPlayHandle.longValue() == 0) {
            System.err.println("Please make sure the PlayBack Handle is valid");
            return;
        }

        NetSdk.CLIENT_NormalPlayBack(m_hPlayHandle);
    }

    /**
     * 快放
     */
    private void FastPlayBack() {
        if (m_hPlayHandle.longValue() == 0) {
            System.err.println("Please make sure the PlayBack Handle is valid");
            return;
        }

        NetSdk.CLIENT_FastPlayBack(m_hPlayHandle);
    }

    /**
     * 慢放
     */
    private void SlowPlayBack() {
        if (m_hPlayHandle.longValue() == 0) {
            System.err.println("Please make sure the PlayBack Handle is valid");
            return;
        }

        NetSdk.CLIENT_SlowPlayBack(m_hPlayHandle);
    }

    /**
     * 开始下载录像
     */
    private void DownLoadRecordFile() {
        if (m_hLoginHandle.longValue() == 0) {
            if (m_hLoginHandle.longValue() == 0) {
                System.err.printf("Please Login First");
                JOptionPane.showMessageDialog(playFrame, "请先登录");
                return;
            }
        }

        // 同一个登录句柄，回放和下载不能同时进行
        if (m_hPlayHandle.longValue() != 0) {
            JOptionPane.showMessageDialog(playFrame, "请先停止回放");
            return;
        }

        updatePlayBackParams();

        // 进度清0
        downloadPos = 0;
        downLoadProgressBar.setValue(0);

        // 通道
        int nChannelId = m_channel.intValue();
        int nRecordFileType = 0; // 全部录像

        File currentPath = new File(".");
        String SavedFileName = currentPath.getAbsoluteFile().getParent() + "\\" + System.currentTimeMillis() + ".dav"; // 默认保存路径

        m_hDownLoadHandle = NetSdk.CLIENT_DownloadByTimeEx(m_hLoginHandle, nChannelId, nRecordFileType,
                m_startTime, m_stopTime, SavedFileName,
                m_DownLoadPos, null, m_DownLoadData, null, null);
        if (0 == m_hDownLoadHandle.longValue()) {
            int error = NetSdk.CLIENT_GetLastError();
            System.err.printf("DownloadByTimeEx Failed, Get last error [0x%x]\n", error);
            switch(error) {
                case LastError.NET_NO_RECORD_FOUND:
                    JOptionPane.showMessageDialog(playFrame, "查找不到录像");
                    break;
                default:
                    JOptionPane.showMessageDialog(playFrame, "下载失败, 错误码：" + String.format("0x%x", error));
                    break;
            }
        }else {
            System.out.println("DownloadByTimeEx Successed");
        }
    }

    /**
     * 开始下载录像(可以设置保存的录像码流类型)
     */
    private void DownLoadRecordFileEx() {
        if (m_hLoginHandle.longValue() == 0) {
            if (m_hLoginHandle.longValue() == 0) {
                System.err.printf("Please Login First");
                JOptionPane.showMessageDialog(playFrame, "请先登录");
                return;
            }
        }

        // 同一个登录句柄，回放和下载不能同时进行
        if (m_hPlayHandle.longValue() != 0) {
            JOptionPane.showMessageDialog(playFrame, "请先停止回放");
            return;
        }

        updatePlayBackParams();

        // 进度清0
        downloadPos = 0;
        downLoadProgressBar.setValue(0);

        // 通道
        int nChannelId = m_channel.intValue();
        int nRecordFileType = 0; // 全部录像
        int scType = 3; 	// scType:         码流转换类型,0-DAV码流(默认); 1-PS流,3-MP4

        File currentPath = new File(".");
        String SavedFileName = currentPath.getAbsoluteFile().getParent() + "\\" + System.currentTimeMillis() + ".MP4"; // 默认保存路径

        System.out.println("SavedFileName:" + SavedFileName);

        m_hDownLoadHandle = NetSdk.CLIENT_DownloadByTimeEx2(m_hLoginHandle, nChannelId, nRecordFileType,
                m_startTime, m_stopTime, SavedFileName,
                m_DownLoadPos, null, m_DownLoadData, null, scType, null);
        if (0 == m_hDownLoadHandle.longValue()) {
            int error = NetSdk.CLIENT_GetLastError();
            System.err.printf("DownloadByTimeEx Failed, Get last error [0x%x]\n", error);
            switch(error) {
                case LastError.NET_NO_RECORD_FOUND:
                    JOptionPane.showMessageDialog(playFrame, "查找不到录像");
                    break;
                default:
                    JOptionPane.showMessageDialog(playFrame, "下载失败, 错误码：" + String.format("0x%x", error));
                    break;
            }
        }else {
            System.out.println("DownloadByTimeEx Successed");
            downRecordButton.setEnabled(false);
        }
    }

    /**
     * 结束下载录像
     */
    private void StopDownLoadRecord() {
        if (m_hDownLoadHandle.longValue() == 0) {
            System.err.println("Please make sure the download handle is valid");
            return;
        }
        downloadPos = 0;
        NetSdk.CLIENT_StopDownload(m_hDownLoadHandle);
        m_hDownLoadHandle.setValue(0);
        downRecordButton.setEnabled(true);
    }

    //////////////////////// Components of PlayBackFrame ///////////////
    /**
     * 主界面
     */
    private JFrame playFrame;

    /**
     * 登录条
     */
    private JButton loginBtn;
    private JButton logoutBtn;

    private JLabel nameLabel;
    private JLabel passwordLabel;
    private JTextField nameTextArea;
    private JPasswordField passwordTextArea;

    private JLabel ipLabel;
    private JLabel portLabel;
    private JTextField ipTextArea;
    private JTextField portTextArea;

    private LoginPanel loginJPanel;

    /**
     * 控制面板
     */
    private JButton playButton = new JButton("开启回放");
    private JButton pauseButton = new JButton("暂停");
    private JButton normalButton = new JButton("正常速度");
    private JButton fastButton = new JButton("快放");
    private JButton slowButton = new JButton("慢放");

    private JLabel channelLabel;// 通道
    private JTextField chanelTextArea;
    private JLabel streamTypeLabel;// 码流类型
    private JComboBox streamTypeComboBox;
    private JLabel recordTypeLabel;// 录像文件
    private JComboBox recordTypeComboBox;

    private JLabel startLabel = new JLabel("开始时间");
    private JLabel stopLabel = new JLabel("结束时间");
    private JLabel[] startTimeJLabels = new JLabel[6];
    private JTextField[] startTextFields = new JTextField[6];
    private JLabel[] stopTimeJLabels = new JLabel[6];
    private JTextField[] stopTextFields = new JTextField[6];

    // 下载操作相关组件
    private JButton downRecordButton;
    private JButton stopDonwloadButton;
    private JLabel downLoadProgressLabel;
    private JProgressBar downLoadProgressBar;

    private RecordInfoPanel recordInfoPanel;
    private TimePanel ctrlTimeBarPanel;
    private ControlPanel controlPanel;
    private PlayBackCtrlPanel ctrlPanel;
    private DownLoadRecordPanel downLoadRecordPanel;
    private int downloadPos = 0;

    /**
     * 播放界面
     */
    private Panel panelPlayBack;
    private JSlider playSlider; // 播放进度条
    private int playPos = 0;
    private PlayWindow playWindow;
}

public class PlayBack {
    public static void main(String[] args) {
        new NewPlayBackFrame();
    }
}
