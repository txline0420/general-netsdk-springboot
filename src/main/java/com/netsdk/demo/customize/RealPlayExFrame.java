package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import static com.netsdk.demo.util.UiUtil.setBorderEx;

public class RealPlayExFrame extends JFrame {

    //////////////////////////////////// GUI /////////////////////////////////////
    ////////////////////////////// 这部分不需要关注 ////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    private final JPanel mainContentPane = new JPanel();

    private JPanel LogonPanel;     // 登录面板
    private JTextField textField_IP;        // IP
    private JTextField textField_Port;      // Port
    private JTextField textField_username;  // username
    private JTextField textField_password;  // password

    private JButton btn_logon;     // 登录
    private JButton btn_realPlay;  // 预览
    private JButton btn_audio;     // 音频

    private JPanel controlPanel;   // 控制面板
    private JComboBox<String> cbx_channel;    // 通道

    public RealPlayPanel realPanel;   // 预览容器
    public JPanel realPlayPanel;      // 画布容器
    public Panel realPlayWindow = new Panel(); // 画布


    // 带背景的面板组件
    private class PaintPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private Image image; //背景图片

        public PaintPanel() {
            super();
            setOpaque(true); //非透明
            setLayout(null);
            setBackground(new Color(220, 220, 220));
            setForeground(new Color(0, 0, 0));
        }

        //设置图片的方法
        public void setImage(Image image) {
            this.image = image;
        }

        protected void paintComponent(Graphics g) {    //重写绘制组件外观
            if (image != null) {
                g.drawImage(image, 0, 0, getWidth(), getHeight(), this);//绘制图片与组件大小相同
            }
            super.paintComponent(g); // 执行超类方法
        }
    }

    // 预览面板
    private class RealPlayPanel extends PaintPanel {
        private static final long serialVersionUID = 1L;

        public RealPlayPanel() {
            setBorderEx(this, "实时预览", 4);
            setLayout(new BorderLayout());
            Dimension dim = getPreferredSize();
            setPreferredSize(dim);

            realPlayPanel = new JPanel();
            add(realPlayPanel, BorderLayout.CENTER);

            /************ 预览面板 **************/
            realPlayPanel.setLayout(new BorderLayout());
            realPlayPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
            realPlayWindow.setBackground(Color.GRAY);
            realPlayPanel.add(realPlayWindow, BorderLayout.CENTER);
        }
    }

    /**
     * Create the frame.
     */
    public RealPlayExFrame() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("预览Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 767, 631);
        mainContentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(mainContentPane);
        mainContentPane.setLayout(null);

        LogonPanel = new JPanel();
        LogonPanel.setToolTipText("");
        LogonPanel.setBounds(10, 26, 719, 22);
        mainContentPane.add(LogonPanel);
        LogonPanel.setLayout(null);

        JLabel label_IP = new JLabel("IP:");
        label_IP.setPreferredSize(new Dimension(30, 20));
        label_IP.setBounds(13, 1, 30, 20);
        LogonPanel.add(label_IP);

        textField_IP = new JTextField(m_strIp);
        textField_IP.setPreferredSize(new Dimension(50, 21));
        textField_IP.setColumns(10);
        textField_IP.setBounds(48, 1, 88, 21);
        LogonPanel.add(textField_IP);

        JLabel label_Port = new JLabel("Port:");
        label_Port.setPreferredSize(new Dimension(30, 20));
        label_Port.setBounds(160, 1, 30, 20);
        LogonPanel.add(label_Port);

        textField_Port = new JTextField(String.valueOf(m_nPort));
        textField_Port.setPreferredSize(new Dimension(50, 21));
        textField_Port.setColumns(10);
        textField_Port.setBounds(195, 1, 88, 21);
        LogonPanel.add(textField_Port);

        JLabel label_username = new JLabel("账户:");
        label_username.setPreferredSize(new Dimension(30, 20));
        label_username.setBounds(312, 1, 30, 20);
        LogonPanel.add(label_username);

        textField_username = new JTextField(m_strUser);
        textField_username.setPreferredSize(new Dimension(50, 21));
        textField_username.setColumns(10);
        textField_username.setBounds(347, 1, 88, 21);
        LogonPanel.add(textField_username);

        JLabel label_password = new JLabel("密码:");
        label_password.setPreferredSize(new Dimension(30, 20));
        label_password.setBounds(465, 1, 30, 20);
        LogonPanel.add(label_password);

        textField_password = new JTextField(m_strPassword);
        textField_password.setPreferredSize(new Dimension(50, 21));
        textField_password.setColumns(10);
        textField_password.setBounds(500, 1, 88, 21);
        LogonPanel.add(textField_password);

        btn_logon = new JButton("登录");
        btn_logon.setBounds(602, 0, 112, 23);
        LogonPanel.add(btn_logon);

        JPanel realPanelBox = new JPanel();
        realPanelBox.setBounds(11, 61, 587, 510);
        realPanelBox.setLayout(new BorderLayout(0, 0));
        realPanel = new RealPlayPanel();
        realPanelBox.add(realPanel);
        mainContentPane.add(realPanelBox);

        controlPanel = new JPanel();
        controlPanel.setBounds(605, 61, 128, 508);
        mainContentPane.add(controlPanel);
        controlPanel.setLayout(null);

        btn_realPlay = new JButton("完整预览");
        btn_realPlay.setBounds(10, 185, 113, 36);
        controlPanel.add(btn_realPlay);

        btn_audio = new JButton("仅取音频");
        btn_audio.setBounds(11, 250, 113, 35);
        controlPanel.add(btn_audio);

        JLabel label_channel = new JLabel("通道");
        label_channel.setBounds(8, 10, 111, 15);
        controlPanel.add(label_channel);

        cbx_channel = new JComboBox<String>();
        cbx_channel.setBounds(9, 34, 111, 21);
        controlPanel.add(cbx_channel);

        SetInnerComponentEnable(controlPanel, false);

        // 绑定按钮事件
        this.btnActionRegister();
    }

    // 获取 Swing Container 内所有的非 Container 组件
    public static Component[] getComponents(Component container) {
        ArrayList<Component> list = null;

        try {
            list = new ArrayList<Component>(Arrays.asList(
                    ((Container) container).getComponents()));
            for (int index = 0; index < list.size(); index++) {
                list.addAll(Arrays.asList(getComponents(list.get(index))));
            }
        } catch (ClassCastException e) {
            list = new ArrayList<Component>();
        }

        return list.toArray(new Component[0]);
    }

    // 启用/禁用 Component 内所有组件
    public static void SetInnerComponentEnable(Component container, boolean enable) {
        for (Component component : getComponents(container)) {
            component.setEnabled(enable);
        }
    }

    //////////////////////////////////// 按钮事件 ////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////

    private boolean bLogin = Boolean.FALSE;   // 是否登录
    private boolean bRealPlay = Boolean.FALSE;// 是否预览

    private Vector<String> chnlist = new Vector<String>();

    private void btnActionRegister() {
        // 窗体请出事件
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (m_lRealHandle.longValue() != 0) {
                    StopRealPlay();
                }
                if (m_loginHandle.longValue() != 0) {
                    LoginOut();      // 登出
                }
                SdkCleanUp();        // 推出前需要清理 SDK
                dispose();
            }
        });

        // 登录/登出事件
        btn_logon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!bLogin) {
                    Login(  // 登录
                            textField_IP.getText().trim(),                          // IP
                            Integer.parseInt(textField_Port.getText().trim()),      // Port
                            textField_username.getText().trim(),                    // username
                            textField_password.getText().trim()                     // password);
                    );
                    // 句柄不为 0 说明是有效句柄
                    if (m_loginHandle.longValue() != 0) {

                        // 一些UI和状态调整
                        SetInnerComponentEnable(controlPanel, true);
                        btn_logon.setText("登出");
                        bLogin = true;

                        for (int i = 1; i < deviceInfo.byChanNum + 1; i++) {
                            chnlist.add("通道" + " " + String.valueOf(i));
                        }
                        cbx_channel.setModel(new DefaultComboBoxModel<>(chnlist));
                    }
                } else {
                    // 登出前，先停止拉流
                    if (bRealPlay) {
                        StopRealPlay();

                        // 一些UI和状态调整
                        bRealPlay = false;
                        btn_realPlay.setText("完整预览");
                        btn_audio.setText("仅取音频");
                        realPlayWindow.repaint();
                    }
                    LoginOut();  // 登出

                    // 一些UI和状态调整
                    bLogin = false;
                    btn_logon.setText("登入");
                    cbx_channel.setSelectedIndex(-1);  // 清空通道
                    cbx_channel.removeAllItems();
                    SetInnerComponentEnable(controlPanel, false);
                }
            }
        });

        // 预览按钮事件
        btn_realPlay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!bRealPlay) {
                    // 开始预览
                    realPlayWithDefaultType(cbx_channel.getSelectedIndex(),
                            Native.getComponentPointer(realPlayWindow));
                    if (m_lRealHandle.longValue() != 0) {

                        // 一些UI和状态调整
                        realPlayWindow.setVisible(true);
                        realPlayPanel.setOpaque(false);
                        realPlayPanel.repaint();
                        bRealPlay = true;
                        cbx_channel.setEnabled(false);
                        btn_audio.setEnabled(false);
                        btn_realPlay.setText("停止预览");
                    }
                } else {
                    // 停止预览
                    StopRealPlay();

                    // 一些UI和状态调整
                    realPlayWindow.repaint();
                    bRealPlay = false;
                    cbx_channel.setEnabled(true);
                    btn_audio.setEnabled(true);
                    btn_realPlay.setText("完整预览");
                }
            }
        });

        // 音频按钮事件
        btn_audio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!bRealPlay) {
                    // 开始取音频
                    realPlayWithAudioOnly(cbx_channel.getSelectedIndex(),
                            Native.getComponentPointer(realPlayWindow));
                    if (m_lRealHandle.longValue() != 0) {

                        // 一些UI和状态调整
                        realPlayWindow.setVisible(true);
                        realPlayPanel.setOpaque(false);
                        realPlayPanel.repaint();
                        bRealPlay = true;
                        cbx_channel.setEnabled(false);
                        btn_realPlay.setEnabled(false);
                        btn_audio.setText("停止音频");
                    }
                } else {
                    // 停止取音频
                    StopRealPlay();

                    // 一些UI和状态调整
                    realPlayWindow.repaint();
                    bRealPlay = false;
                    cbx_channel.setEnabled(true);
                    btn_realPlay.setEnabled(true);
                    btn_audio.setText("仅取音频");
                }
            }
        });

    }


    ////////////////////////////////// SDK Params and Functions //////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    private static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;

    // 登陆句柄
    private NetSDKLib.LLong m_loginHandle = new NetSDKLib.LLong(0);

    // 设备信息扩展
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    // 监视预览句柄
    private NetSDKLib.LLong m_lRealHandle = new NetSDKLib.LLong(0);


    ///////////////////////////////////////////// 登录登出 //////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 高安全登陆
    private void Login(String strIp, int port, String strUser, String strPassword) {
        // 入参
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();
        System.arraycopy(strIp.getBytes(), 0, pstlnParam.szIP, 0, strIp.length());               // IP
        pstlnParam.nPort = port;                                                                                // Port
        System.arraycopy(strUser.getBytes(), 0, pstlnParam.szUserName, 0, strUser.length());           // Username
        System.arraycopy(strPassword.getBytes(), 0, pstlnParam.szPassword, 0, strPassword.length());   // Password

        // 出参
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        // 高安全登陆
        m_loginHandle = netSdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

        if (m_loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Succeed!\n", strIp);
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("Device Address: " + strIp + " Port: " + port);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        } else {
            System.err.printf("Login Device[%s] Failed.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
            LoginOut();
        }
    }

    // 登出
    private void LoginOut() {
        if (m_loginHandle.longValue() != 0) {
            netSdk.CLIENT_Logout(m_loginHandle);
            m_loginHandle.setValue(0);
        }
    }

    ///////////////////////////////////////////// 预览 //////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 正常拉流
    public void realPlayWithDefaultType(int channel, Pointer hWnd) {

        int rType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay;    // 默认实时主码流

        m_lRealHandle = netSdk.CLIENT_RealPlayEx(m_loginHandle, channel, hWnd, rType);
        if (m_lRealHandle.longValue() != 0) {

            netSdk.CLIENT_OpenSound(m_lRealHandle); // 要打开声音，不然听不见

            System.out.println("realPlayWithDefaultType succeed");
            // 设置拉流回调函数，可以从内直接取流，注意回调要写成静态单例
            netSdk.CLIENT_SetRealDataCallBackEx(m_lRealHandle, CbfRealDataCallBackEx.getInstance(), null, 31);
        }
    }

    // 仅拉音频
    public void realPlayWithAudioOnly(int channel, Pointer hWnd) {

        int rType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay_Audio;    // 仅拉音频

        m_lRealHandle = netSdk.CLIENT_RealPlayEx(m_loginHandle, channel, hWnd, rType);

        if (m_lRealHandle.longValue() != 0) {

            netSdk.CLIENT_OpenSound(m_lRealHandle);   // 要打开声音，不然听不见
            System.out.println("realPlayWithAudioOnly succeed");
            // 设置拉流回调函数，可以从内直接取流，注意回调要写成静态单例
            netSdk.CLIENT_SetRealDataCallBackEx(m_lRealHandle, CbfRealDataCallBackEx.getInstance(), null, 31);
        }
    }

    // 停止拉流
    public void StopRealPlay() {
        if (netSdk.CLIENT_StopRealPlayEx(m_lRealHandle)) {
            System.out.println("StopRealPlay succeed");
            m_lRealHandle.setValue(0);
        }
    }

    /**
     * 实时监视数据回调函数
     */
    private static class CbfRealDataCallBackEx implements NetSDKLib.fRealDataCallBackEx {
        private CbfRealDataCallBackEx() {
        }

        private static class CallBackHolder {
            private static final CbfRealDataCallBackEx instance = new CbfRealDataCallBackEx();
        }

        public static CbfRealDataCallBackEx getInstance() {
            return CallBackHolder.instance;
        }

        @Override
        public void invoke(NetSDKLib.LLong lRealHandle, int dwDataType, Pointer pBuffer,
                           int dwBufSize, int param, Pointer dwUser) {

            // 数据在 pBuffer 里，二进制字节数据，长度 dwBufSize，需要的话可以取出来
            if (0 != lRealHandle.longValue()) {
                switch (dwDataType) {
                    case 0:
                        System.out.println("码流大小为" + dwBufSize + "\n" + "码流类型为原始音视频混合数据");
                        break;
                    case 1:
                        //标准视频数据
                        System.out.println("码流大小为" + dwBufSize + "\n" + "码流类型为标准视频数据");
                        break;
                    case 2:
                        //yuv 数据
                        System.out.println("码流大小为" + dwBufSize + "\n" + "码流类型为 yuv 数据");
                        break;
                    case 3:
                        //pcm 音频数据
                        System.out.println("码流大小为" + dwBufSize + "\n" + "码流类型为 pcm 音频数据");
                        break;
                    case 4:
                        //原始音频数据
                        System.out.println("码流大小为" + dwBufSize + "\n" + "码流类型为原始音频数据");
                        break;
                    default:
                        break;
                }
            }
        }
    }

    ////////////////////////////////////////// SDK 初始化 //////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private static void SdkInit() {
        // 初始化SDK库
        netSdk.CLIENT_Init(DefaultDisconnectCallback.getINSTANCE(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(DefaultHaveReconnectCallBack.getINSTANCE(), null);

        //打开playsdk日志
        // netPlay.PLAY_SetPrintLogLevel(PlaySDKLib.LOG_LEVEL.LOG_LevelDebug);
        //打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)) {
            System.err.println("Open SDK Log Failed!!!");
        }
    }

    private static void SdkCleanUp() {
        netSdk.CLIENT_Cleanup();
    }

    ///////////////////////////////////////////// Demo 启动 //////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////
    private String m_strIp = "172.8.4.159";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";
////////////////////////////////////////////////////////////////

    /**
     * Demo出于对比，提供了 正常预览 和 仅取音频 两个按钮
     * 如果要关注具体调用方法，可以只关注函数： realPlayWithAudioOnly
     */
    public static void main(String[] args) {
        // SDK 初始化
        SdkInit();

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    RealPlayExFrame frame = new RealPlayExFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
