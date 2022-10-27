package com.netsdk.demo.customize.heatmap;

import com.netsdk.demo.util.DateChooserJButton;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_IN_GET_HEATMAPS_INFO;
import com.netsdk.lib.structure.NET_OUT_GET_HEATMAPS_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;


/**
 * 热度图demo
 */
class HeatMapFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib ConfigSdk = NetSDKLib.CONFIG_INSTANCE;

    //登陆参数
//	private String m_strIp2         = "172.31.17.12";
//	private String m_strIp         = "10.34.3.35";
//	private Integer m_nPort        = new Integer("37777");
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";

    // 登录参数 热成像设备
    private String m_strIp = "172.32.102.57";
    private Integer m_nPort = new Integer("37777");


    //设备信息
    private NET_DEVICEINFO_Ex m_stDeviceInfo = new NET_DEVICEINFO_Ex();

    private LLong m_hLoginHandle = new LLong(0);   //登陆句柄

    private boolean bInit = false;
    private boolean bLogopen = false;

    private DisConnect disConnect = new DisConnect();    //设备断线通知回调
    private HaveReConnect haveReConnect = new HaveReConnect(); //网络连接恢复

    // 热度图宽高
    private int width = 0;
    private int height = 0;

    BufferedImage snapBufferedImage = null;   // 抓图缓存

    // 通道
    private Vector<String> chnlist = new Vector<String>();

    //设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    public class DisConnect implements fDisConnect {
        public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
        }
    }

    //网络连接恢复，设备重连成功回调
    // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
    public class HaveReConnect implements fHaveReConnect {
        public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
        }
    }

    //初始化
    public boolean init() {

        bInit = NetSdk.CLIENT_Init(disConnect, null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }

        //打开日志，可选
        LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
        File path = new File(".");
        String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\" + System.currentTimeMillis() + ".log";

        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        bLogopen = NetSdk.CLIENT_LogOpen(setLog);
        if (!bLogopen) {
            System.err.println("Failed to open NetSDK log");
        }

        // 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
        // 此操作为可选操作，但建议用户进行设置
        NetSdk.CLIENT_SetAutoReconnect(haveReConnect, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 5000; //登录请求响应超时时间设置为5S
        int tryTimes = 3;    //登录时尝试建立链接3次
        NetSdk.CLIENT_SetConnectTime(waitTime, tryTimes);

        // 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NET_PARAM netParam = new NET_PARAM();
        netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
        NetSdk.CLIENT_SetNetworkParam(netParam);

        //设置抓图回调函数， 图片主要在m_SnapReceiveCB中返回
        NetSdk.CLIENT_SetSnapRevCallBack(m_SnapReceiveCB, null);

        return true;
    }

    //清除环境
    public void cleanup() {
        if (bLogopen) {
            NetSdk.CLIENT_LogClose();
        }

        if (bInit) {
            NetSdk.CLIENT_Cleanup();
        }
    }

    public HeatMapFrame() {
        init();
        setTitle("获取热度图");
        setSize(700, 670);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        loginPanel = new LoginPanel();
        heatMapPanel = new HeatMapPanel();

        add(loginPanel, BorderLayout.NORTH);
        add(heatMapPanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        logout();
                        cleanup();
                        System.exit(0);
                    }
                });
            }
        });
    }

    //设置边框
    private void setBorderEx(JComponent object, String title, int width) {
        Border innerBorder = BorderFactory.createTitledBorder(title);
        Border outerBorder = BorderFactory.createEmptyBorder(width, width, width, width);
        object.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
    }

    //登录面板
    private class LoginPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        public LoginPanel() {
            setLayout(new FlowLayout());
            setBorderEx(this, "登录", 2);
            Dimension dimension = new Dimension();
            dimension.height = 60;
            setPreferredSize(dimension);

            loginBtn = new JButton("登录");
            logoutBtn = new JButton("登出");
            nameLabel = new JLabel("用户名");
            passwordLabel = new JLabel("密码");
            nameTextArea = new JTextField(m_strUser, 8);
            passwordTextArea = new JPasswordField(m_strPassword, 8);
            ipLabel = new JLabel("设备地址");
            portLabel = new JLabel("端口号");
            ipTextArea = new JTextField(m_strIp, 8);
            portTextArea = new JTextField(m_nPort.toString(), 5);

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

            //登录按钮，监听事件
            loginBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    login();
                }
            });

            //登出按钮，监听事件
            logoutBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    logout();
                }
            });
        }
    }

    // 热度图获取
    private class HeatMapPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        public HeatMapPanel() {
            setLayout(new BorderLayout());
            setBorderEx(this, "热度图获取", 2);

            JPanel panel1 = new JPanel();
            showHeatMapPanel = new PaintPanel();

            add(panel1, BorderLayout.NORTH);
            add(showHeatMapPanel, BorderLayout.CENTER);

            //////////////////////////
            startTimeLabel = new JLabel("开始时间:");
            endTimeLabel = new JLabel("结束时间:");
            startTimeBtn = new DateChooserJButton("2018-5-23 00:00:00");
            endTimeBtn = new DateChooserJButton();
            snapPicBtn = new JButton("获取热度图");
            chnLabel = new JLabel("通道号:");
            chnComoBox = new JComboBox(chnlist);
            planIdLabel = new JLabel("计划ID:");
            String[] planIdStr = {"1", "2", "3", "4"};
            planIdComoBox = new JComboBox(planIdStr);
            resultLabel = new JLabel("获取热度图状态 ：", JLabel.CENTER);

            chnComoBox.setPreferredSize(new Dimension(80, 23));
            planIdComoBox.setPreferredSize(new Dimension(80, 23));
            resultLabel.setPreferredSize(new Dimension(200, 23));

            Dimension dimension = new Dimension();
            dimension.height = 90;
            panel1.setPreferredSize(dimension);

            panel1.setLayout(new GridLayout(3, 1));

            JPanel panel2 = new JPanel();
            JPanel panel3 = new JPanel();

            panel1.add(panel2);
            panel1.add(panel3);
            panel1.add(resultLabel);

            //
            panel2.setLayout(new FlowLayout());
            panel3.setLayout(new FlowLayout());

            panel2.add(startTimeLabel);
            panel2.add(startTimeBtn);
            panel2.add(endTimeLabel);
            panel2.add(endTimeBtn);

            panel3.add(chnLabel);
            panel3.add(chnComoBox);
            panel3.add(planIdLabel);
            panel3.add(planIdComoBox);
            panel3.add(snapPicBtn);

            startTimeBtn.setEnabled(false);
            endTimeBtn.setEnabled(false);
            snapPicBtn.setEnabled(false);
            chnComoBox.setEnabled(false);
            planIdComoBox.setEnabled(false);

            snapPicBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            showHeatMapPanel.setOpaque(true);
                            showHeatMapPanel.repaint();
                            snapBufferedImage = null;
                            BufferedImage heatBufferedImage = null;
                            resultLabel.setText("获取热度图状态 ：");

                            ///////////////// 抓图，并保存图片 ///////////////
                            resultLabel.setText("获取热度图状态 ：正在抓图.");
                            snapPicture(chnComoBox.getSelectedIndex());

                            //////////////// 查询热度图，并保存图片 ////////////////////
                            resultLabel.setText("获取热度图状态 ：正在查询热度图信息.");
                            byte[] buffer = queryHeatMap(chnComoBox.getSelectedIndex(), planIdComoBox.getSelectedIndex() + 1, startTimeBtn.getText(), endTimeBtn.getText());

                            if (buffer == null) {
                                return;
                            }

                            resultLabel.setText("获取热度图状态 ：正在生成热度图.");

                            // 将buffer 归一化，线性拉伸
                            EnlargeGrayRange(buffer, 0, 255);

                            // 伪彩色
                            heatBufferedImage = PGrayToPseudoColor(buffer, width, height);

                            // 热度图路径
                            File currentPath = new File(".");
                            String strFileHeatPic = currentPath.getAbsoluteFile().getParent() + "\\" + "HeatPicture.jpg";

                            if (heatBufferedImage == null) {
                                JOptionPane.showMessageDialog(null, "没查到热度图");
                                return;
                            }

                            try {
                                // 热度图
                                ImageIO.write(heatBufferedImage, "jpg", new File(strFileHeatPic));
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                            //////////////// 以抓图为背景，将热度图叠加上去。合成图片 /////////////////////
                            while (true) {
                                if (snapBufferedImage != null) {
                                    break;
                                }

                                try {
                                    Thread.sleep(5000);
                                    break;
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                            }

                            resultLabel.setText("获取热度图状态 ：正在合成热度图.");

                            // 合成图路径
                            String strFileComposePic = currentPath.getAbsoluteFile().getParent() + "\\" + "ComposePicture.jpg";

                            Graphics2D graphics2d = snapBufferedImage.createGraphics();
                            graphics2d.drawImage(heatBufferedImage, 0, 0, snapBufferedImage.getWidth(), snapBufferedImage.getHeight(), null);

                            try {
                                ImageIO.write(snapBufferedImage, "jpg", new File(strFileComposePic));
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                            resultLabel.setText("获取热度图状态 ：已合成热度图.");

                            if (snapBufferedImage != null) {
                                showHeatMapPanel.setOpaque(false);
                                showHeatMapPanel.setImage(snapBufferedImage);
                                showHeatMapPanel.repaint();
                            }
                        }
                    }).start();
                }
            });
        }
    }

    ////////////////////////////////////// 接口实现 ///////////////////////////////////////////////


    //登录按钮事件
    private void login() {
        m_strIp = ipTextArea.getText();
        m_nPort = Integer.parseInt(portTextArea.getText());
        m_strUser = nameTextArea.getText();
        m_strPassword = new String(passwordTextArea.getPassword());

        System.out.println("设备地址：" + m_strIp + "\n端口号：" + m_nPort
                + "\n用户名：" + m_strUser + "\n密码：" + m_strPassword);

        IntByReference nError = new IntByReference(0);
        System.out.println("设备信息:" + m_stDeviceInfo);
        loginForHeatMapDirectly();
        m_hLoginHandle = NetSdk.CLIENT_LoginEx2(m_strIp, m_nPort.intValue(), m_strUser, m_strPassword, 0, null, m_stDeviceInfo, nError);
        if (m_hLoginHandle.longValue() == 0) {
            System.err.println("Login Device[%s] Port[%d]Failed." + ToolKits.getErrorCode());
        } else {
            System.out.println("Login Success [ " + m_strIp + " ]");
            logoutBtn.setEnabled(true);
            loginBtn.setEnabled(false);
            startTimeBtn.setEnabled(true);
            endTimeBtn.setEnabled(true);
            snapPicBtn.setEnabled(true);
            chnComoBox.setEnabled(true);
            planIdComoBox.setEnabled(true);

            for (int i = 0; i < m_stDeviceInfo.byChanNum; i++) {
                chnlist.add("通道号 " + i);
            }

            if (m_stDeviceInfo.byChanNum > 0) {
                chnComoBox.setSelectedIndex(0);
            }
        }
    }


    //登出按钮事件
    private void logout() {
        if (m_hLoginHandle.longValue() != 0) {
            System.out.println("Logout Button Action");


            if (NetSdk.CLIENT_Logout(m_hLoginHandle)) {
                System.out.println("Logout Success [ " + m_strIp + " ]");
                m_hLoginHandle.setValue(0);
                logoutBtn.setEnabled(false);
                loginBtn.setEnabled(true);
                startTimeBtn.setEnabled(false);
                endTimeBtn.setEnabled(false);
                snapPicBtn.setEnabled(false);
                chnComoBox.setEnabled(false);
                planIdComoBox.setEnabled(false);
                showHeatMapPanel.setOpaque(true);
                showHeatMapPanel.repaint();
                chnlist.clear();
                resultLabel.setText("获取热度图状态 ：");
            }
        }
    }

    // 远程抓图
    public void snapPicture(int chn) {
        // 发送抓图命令给前端设备，抓图的信息
        SNAP_PARAMS stuSnapParams = new SNAP_PARAMS();
        stuSnapParams.Channel = chn;  //抓图通道
        stuSnapParams.mode = 0;     //表示请求一帧
        stuSnapParams.Quality = 3;
        stuSnapParams.InterSnap = 5;
        stuSnapParams.CmdSerial = 100; // 请求序列号，有效值范围 0~65535，超过范围会被截断为

        IntByReference reserved = new IntByReference(0);

        if (false == NetSdk.CLIENT_SnapPictureEx(m_hLoginHandle, stuSnapParams, reserved)) {
            System.err.println("CLIENT_SnapPictureEx Failed!" + ToolKits.getErrorCode());
            return;
        } else {
            System.out.println("CLIENT_SnapPictureEx success");
        }
    }


    private fSnapReceiveCB m_SnapReceiveCB = new fSnapReceiveCB();

    public class fSnapReceiveCB implements fSnapRev {
        public void invoke(LLong lLoginID, Pointer pBuf, int RevLen, int EncodeType, int CmdSerial, Pointer dwUser) {
            //pBuf收到的图片数据
            File currentPath = new File(".");
            String strFileSnapPic = currentPath.getAbsoluteFile().getParent() + "\\" + "SnapPicture.jpg";

            byte[] buf = pBuf.getByteArray(0, RevLen);

            ByteArrayInputStream byteArrInput = new ByteArrayInputStream(buf);
            try {
                snapBufferedImage = ImageIO.read(byteArrInput);

                if (snapBufferedImage == null) {
                    return;
                }
                ImageIO.write(snapBufferedImage, "jpg", new File(strFileSnapPic));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 查询热度图
    public byte[] queryHeatMap(int chn, int planId, String startTime, String endTime) {
        byte[] buffer = null;

        // 开始时间
        String[] startStr = startTime.split(" ");
        String[] startTimeStr1 = startStr[0].split("-");
        String[] startTimeStr2 = startStr[1].split(":");

        // 结束时间
        String[] endStr = endTime.split(" ");
        String[] endTimeStr1 = endStr[0].split("-");
        String[] endTimeStr2 = endStr[1].split(":");


        NET_QUERY_HEAT_MAP heatMap = new NET_QUERY_HEAT_MAP();
        heatMap.stuIn.nChannel = chn; // 通道

        heatMap.stuIn.nPlanID = planId;  // 计划ID

        // 开始时间
        heatMap.stuIn.stuBegin.dwYear = Integer.parseInt(startTimeStr1[0]);
        heatMap.stuIn.stuBegin.dwMonth = Integer.parseInt(startTimeStr1[1]);
        heatMap.stuIn.stuBegin.dwDay = Integer.parseInt(startTimeStr1[2]);
        heatMap.stuIn.stuBegin.dwHour = Integer.parseInt(startTimeStr2[0]);
        heatMap.stuIn.stuBegin.dwMinute = Integer.parseInt(startTimeStr2[1]);
        heatMap.stuIn.stuBegin.dwSecond = Integer.parseInt(startTimeStr2[2]);

        // 结束时间
        heatMap.stuIn.stuEnd.dwYear = Integer.parseInt(endTimeStr1[0]);
        heatMap.stuIn.stuEnd.dwMonth = Integer.parseInt(endTimeStr1[1]);
        heatMap.stuIn.stuEnd.dwDay = Integer.parseInt(endTimeStr1[2]);
        heatMap.stuIn.stuEnd.dwHour = Integer.parseInt(endTimeStr2[0]);
        heatMap.stuIn.stuEnd.dwMinute = Integer.parseInt(endTimeStr2[1]);
        heatMap.stuIn.stuEnd.dwSecond = Integer.parseInt(endTimeStr2[2]);

        // 指针申请内存
        int size = 4000 * 4000;
        heatMap.stuOut.pBufData = new Memory(size);
        heatMap.stuOut.pBufData.clear(size);

        heatMap.stuOut.nBufLen = size;

        heatMap.stuOut.emDataType = 1; // 1-灰度数据   2-原始数据

        IntByReference pRetLen = new IntByReference(0);

        heatMap.write();
        boolean bRet = NetSdk.CLIENT_QueryDevState(m_hLoginHandle, NetSDKLib.NET_DEVSTATE_GET_HEAT_MAP,
                heatMap.getPointer(), heatMap.size(), pRetLen, 5000);
        heatMap.read();
        if (bRet) {
            width = heatMap.stuOut.nWidth;
            height = heatMap.stuOut.nHeight;

            buffer = heatMap.stuOut.pBufData.getByteArray(0, heatMap.stuOut.nBufRet);
        } else {
            JOptionPane.showMessageDialog(null, "查到热度图失败， 错误码 ：" + ToolKits.getErrorCode());
            System.err.println("Query HeatMap Failed!" + ToolKits.getErrorCode());
        }
        return buffer;
    }


    // 伪彩色处理
    public BufferedImage PGrayToPseudoColor(byte[] data, int width, int height) {
        int nLimitColor = 5;

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int R, G, B;
        byte[] buffer = new byte[4];      // 一个像素点
        int temp = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                R = buffer[0];
                G = buffer[1];
                B = buffer[2];

                buffer[3] = (byte) 0.7 * 255;// 不透明度

                temp = 0xff & data[x + y * width];

                if (temp < nLimitColor) {
                    buffer[3] = 0; // 太小的直接透明，不覆盖

                    bufferedImage.setRGB(x, y, 0);
                    continue;
                } else if (temp >= nLimitColor && temp <= 51) {
                    buffer[0] = 0;
                    buffer[1] = (byte) (5 * temp);
                    buffer[2] = (byte) 255;
                } else if (temp > 51 && temp <= 102) {
                    int k = temp - 51;
                    buffer[0] = 0;
                    buffer[1] = (byte) 255;
                    buffer[2] = (byte) (255 - 5 * k);
                } else if (temp > 102 && temp <= 153) {
                    int k = temp - 102;
                    buffer[0] = (byte) (k * 5);
                    buffer[1] = (byte) 255;
                    buffer[2] = 0;
                } else if (temp > 153 && temp <= 204) {
                    int k = temp - 153;
                    buffer[0] = (byte) 255;
                    buffer[1] = (byte) (255 - (128.0 * k / 51 + 0.5));
                    buffer[2] = 0;
                } else if (temp > 204) {
                    int k = temp - 204;
                    buffer[0] = (byte) 255;
                    buffer[1] = (byte) (127 - (127.0 * k / 51 + 0.5));
                    buffer[2] = 0;
                }

                bufferedImage.setRGB(x, y, (170 << 24) | (new Color((0xff & buffer[0]), (0xff & buffer[1]), (0xff & buffer[2])).getRGB() & 0x00ffffff));
            }
        }

        return bufferedImage;
    }


    // 线性拉伸
    public byte[] EnlargeGrayRange(byte[] data, int min, int max) {
        if (data == null) {
            return null;
        }

        byte srcMax = getByteArrMax(data);
        byte srcMin = getByteArrMin(data);

        double k = (max - min) * 1.0 / ((srcMax & 0xff) - (srcMin & 0xff));

        byte[] buffer = new byte[(srcMax & 0xff) - (srcMin & 0xff) + 1];

        for (int i = (srcMin & 0xff); i < buffer.length; i++) {
            buffer[i] = (byte) ((i - (srcMin & 0xff)) * k + min);
        }

        for (int i = 0; i < data.length; i++) {
            data[i] = buffer[data[i] & 0xff];
        }

        return data;
    }

    // 获取byte[] 最大值
    public byte getByteArrMax(byte[] buffer) {
        if (buffer == null) {
            return 0;
        }

        byte max = 0;

        for (int i = 0; i < buffer.length; i++) {
            // 取出最大值
            if ((max & 0xff) < (buffer[i] & 0xff)) {
                max = buffer[i];
            }
        }
        return max;
    }

    // 获取byte[] 最小值
    public byte getByteArrMin(byte[] buffer) {
        if (buffer == null) {
            return 0;
        }

        byte min = 0;

        for (int i = 0; i < buffer.length; i++) {
            // 取出最大值
            if ((min & 0xff) > (buffer[i] & 0xff)) {
                min = buffer[i];
            }
        }
        return min;
    }

    public static void savePicture(byte[] pBuf, String sDstFile) {
        try {
            FileOutputStream fos = new FileOutputStream(sDstFile);
            fos.write(pBuf);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int[] GetPointerDataToIntArray(Pointer pointer, int length) {
        int[] buffer = null;
        if (pointer == null) {
            return null;
        }

        if (length > 0) {
            buffer = new int[length];
            pointer.read(0, buffer, 0, length);
        }

        return buffer;
    }

    //带背景的面板组件
    private class PaintPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private Image image; //背景图片

        public PaintPanel() {
            super();
            setOpaque(true); //非透明
            setLayout(null);
            setBackground(new Color(153, 240, 255));
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

    /*
     * 登录
     */
    private LoginPanel loginPanel;
    private JButton loginBtn;
    private JButton logoutBtn;

    private JLabel nameLabel;
    private JLabel passwordLabel;
    private JLabel ipLabel;
    private JLabel portLabel;
    private JTextField ipTextArea;
    private JTextField portTextArea;
    private JTextField nameTextArea;
    private JPasswordField passwordTextArea;

    private HeatMapPanel heatMapPanel;
    private DateChooserJButton startTimeBtn;
    private DateChooserJButton endTimeBtn;
    private JLabel startTimeLabel;
    private JLabel endTimeLabel;
    private JButton snapPicBtn;
    private JLabel chnLabel;
    private JComboBox chnComoBox;
    private JLabel planIdLabel;
    private JComboBox planIdComoBox;
    private JLabel resultLabel;
    private PaintPanel showHeatMapPanel;


    String ip = m_strIp;
    int port = m_nPort;
    String username = m_strUser;
    String password = m_strPassword;

    // 热成像设备登录
    LLong lLong = new LLong(0);

    public void loginForHeatMapDirectly() {
        IntByReference nError = new IntByReference(0);
        System.out.println("Login ip: " + ip + " port: " + port + " username: " + username + " password: " + password + "\n" + " deviceInfo" + m_stDeviceInfo);
        lLong = NetSdk.CLIENT_LoginEx2(ip, port, username, password, 0, null, m_stDeviceInfo, nError);
        if (lLong.longValue() == 0L) {
            System.err.println("登录失败！" + ENUMERROR.getErrorMessage());
        } else {
            System.out.println(" username: " + username + "成功登录！" + "\n" + "通道号个数： " + m_stDeviceInfo.byChanNum);
            for (int i = 0; i < m_stDeviceInfo.byChanNum; i++) {
                chnlist.add("通道号 " + i);
            }
            if (m_stDeviceInfo.byChanNum > 0) {
                chnComoBox.setSelectedIndex(0);
            }
        }
    }

    int channel = 1;
    // TODO 参数不合法
    public void getHeatMapDirectlyTest() {
        // 获取热成像温度的入参
        NET_OUT_GET_HEATMAPS_INFO outParam = new NET_OUT_GET_HEATMAPS_INFO();
        NET_IN_GET_HEATMAPS_INFO inParam = new NET_IN_GET_HEATMAPS_INFO();
        System.out.println("inParam Size --> " + inParam.dwSize + "\n" + "outParam size --> " + outParam.size());
        inParam.nChannel = channel;
        inParam.write();
        outParam.write();
        if (null == inParam.getPointer() || null == outParam.getPointer()){
            System.err.println("用户参数不合法！");
        }
        boolean ret = NetSdk.CLIENT_GetHeatMapsDirectly(lLong, inParam.getPointer(), outParam.getPointer(), 3000);
        if (ret) {
            System.out.println("GetHeatMapsDirectly success");
            outParam.read();
            // todo
        } else {
            System.err.println("GetHeatMapsDirectly false!" + ENUMERROR.getErrorMessage());
        }
    }




}


public class HeatMap {
    public static void main(String[] args) {
        HeatMapFrame heatMapFrame = new HeatMapFrame();
        heatMapFrame.loginForHeatMapDirectly();
        heatMapFrame.getHeatMapDirectlyTest();
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				HeatMapFrame demo = new HeatMapFrame();
//				demo.setVisible(true);
//			}
//		});
    }
}

