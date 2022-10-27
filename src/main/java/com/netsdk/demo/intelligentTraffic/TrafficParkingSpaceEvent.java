package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.util.EventTaskHandler;
import com.netsdk.demo.util.QueueGeneration;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Vector;

import static com.netsdk.lib.Utils.getOsPrefix;


class ITSParkingSpaceEventMsg {

    static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

    private SDKEnvironment sdkEnv;

    // 登录参数
    private String address = "172.24.1.162";
    private Integer port = new Integer("37777");
    private String username = "admin";
    private String password = "admin123";

    // 设备信息
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
    // 句柄信息
    private LLong loginHandle = new LLong(0); // 登录句柄
    private LLong playHandle = new LLong(0);  // 预览句柄
    private LLong attachHandle = new LLong(0); // 智能事件订阅句柄
    // 事件回调
    private fAnalyzerDataCB m_AnalyzerDataCB = new fAnalyzerDataCB(); // 智能事件回调
    // Flags
    private boolean bTriggerBtnClick = false;
    private boolean bAttachFlag = false;
    private boolean bRealplayFlags = false;

    // 通道
    private Vector<String> chnlist = new Vector<String>();

    public ITSParkingSpaceEventMsg() {
        sdkEnv = new SDKEnvironment();
        sdkEnv.init();     // 初始化

        // 启动回调的消息队列
        eventCBQueueService.init();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainFrame = new JFrame("有车无车事件处理Demo");
                mainFrame.setSize(900, 600);
                mainFrame.setLayout(new BorderLayout());
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                mainFrame.setVisible(true);

                loginJPanel = new LoginPanel(); // 登录面板
                messagePanel = new MessagePanel(); // 事件信息提示
                realPlayPanel = new RealPlayPanel(); // 实时监视

                mainFrame.add(loginJPanel, BorderLayout.NORTH);
                mainFrame.add(realPlayPanel, BorderLayout.WEST);
                mainFrame.add(messagePanel, BorderLayout.CENTER);

                WindowAdapter closeWindowAdapter = new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        System.out.println("Window Closing");
                        logoutBtnPerformed(null); // 登出
                        sdkEnv.cleanup();
                        mainFrame.dispose();
                    }
                };
                mainFrame.addWindowListener(closeWindowAdapter);
            }
        });
    }

    /**
     * NetSDK 库初始化相关
     */
    private class SDKEnvironment {

        private boolean bInit = false;
        private boolean bLogopen = false;

        private DisConnect disConnect = new DisConnect();  // 设备断线通知回调
        private HaveReConnect haveReConnect = new HaveReConnect(); // 网络连接恢复

        // 初始化
        public boolean init() {

            // SDK 库初始化, 并设置断线回调
            bInit = netsdkApi.CLIENT_Init(disConnect, null);
            if (!bInit) {
                System.err.println("Initialize SDK failed");
                return false;
            }

            // 打开日志，可选
            NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();

            File path = new File(".");
            String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\ITSParkingEventMsg_" + System.currentTimeMillis() + ".log";

            setLog.bSetFilePath = 1;
            System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

            setLog.bSetPrintStrategy = 1;
            setLog.nPrintStrategy = 0;
            bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
            if (!bLogopen) {
                System.err.println("Failed to open NetSDK log !!!");
            }

            // 获取版本, 可选操作
            System.out.printf("NetSDK Version [%d]\n", netsdkApi.CLIENT_GetSDKVersion());

            // 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
            // 此操作为可选操作，但建议用户进行设置
            netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);

            // 设置登录超时时间和尝试次数 , 此操作为可选操作
            int waitTime = 5000;   // 登录请求响应超时时间设置为 5s
            int tryTimes = 1;      // 登录时尝试建立链接3次
            netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);

            // 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
            // 接口设置的登录设备超时时间和尝试次数意义相同
            // 此操作为可选操作
            NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
            netParam.nConnectTime = 10000;              // 登录时尝试建立链接的超时时间
            netParam.nGetConnInfoTime = 3000;           // 设置子连接的超时时间
            netParam.nPicBufSize = 8 * 1024 * 1024;
            netsdkApi.CLIENT_SetNetworkParam(netParam);

            return true;
        }

        // 清除环境
        public void cleanup() {
            if (bLogopen) {
                netsdkApi.CLIENT_LogClose();
            }

            if (bInit) {
                netsdkApi.CLIENT_Cleanup();
            }
        }

        // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
        public class DisConnect implements NetSDKLib.fDisConnect {
            public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
                messageTextArea.append(String.format("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort));

                // 取消订阅
                detachIVSEvent();
            }
        }

        // 网络连接恢复，设备重连成功回调
        // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
        public class HaveReConnect implements NetSDKLib.fHaveReConnect {
            public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
                System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
                messageTextArea.append(String.format("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort));

                // 重新订阅
                attachIVSEvent();
            }
        }
    }

    ///////////////// 事件动作相关接口 ///////////////////////////

    /**
     * 登录按钮
     */
    private void loginBtnPerformed(ActionEvent e) {
        address = ipTextArea.getText();
        port = Integer.parseInt(portTextArea.getText());
        username = nameTextArea.getText();
        password = new String(passwordTextArea.getPassword());

        System.out.println("设备地址：" + address
                + "\n端口号：" + port
                + "\n用户名：" + username
                + "\n密码：" + password);
        messageTextArea.append("设备地址：" + address
                + "\n端口号：" + port
                + "\n用户名：" + username
                + "\n密码：" + password + "\n");

        // 登录设备
        IntByReference nError = new IntByReference(0);

        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {{
                    szIP = address.getBytes();
                    nPort = port;
                    szUserName = username.getBytes();
                    szPassword = password.getBytes();
                }};   // 输入结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();    // 输结构体参数
        pstOutParam.stuDeviceInfo = deviceInfo;

        // 高安全登陆
        loginHandle = netsdkApi.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
        //  loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, username, password, 0, null, deviceInfo, nError);
        if (loginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
            JOptionPane.showMessageDialog(mainFrame, "登录失败");
        } else {
            System.out.println("Login Success [ " + address + " ]");
            messageTextArea.append(String.format("Login Success [ %s ]\n", address));

            JOptionPane.showMessageDialog(mainFrame, "登录成功");
            loginJPanel.enableCompents(true);

            for (int i = 0; i < deviceInfo.byChanNum; i++) {
                chnlist.add("通道 " + i);
            }
            // 预选 0 通道
            if (deviceInfo.byChanNum > 0) {
                chnComboBox.setSelectedIndex(0);
            }
        }
    }

    /**
     * 登出按钮
     */
    private void logoutBtnPerformed(ActionEvent e) {
        if (loginHandle.longValue() != 0) {
            System.out.println("LogOut Button Action");
            messageTextArea.append(String.format("LogOut Button Action\n"));

            // 确保关闭监视
            stopRealPlay();
            realplayButton.setText("监视");
            bRealplayFlags = false;

            // 停止订阅事件
            detachIVSEvent();
            attachButton.setText("订阅");
            bAttachFlag = false;

            if (netsdkApi.CLIENT_Logout(loginHandle)) {
                System.out.println("Logout Success [ " + address + " ]");
                messageTextArea.append(String.format("Logout Success [%s]\n", address));
                loginHandle.setValue(0);
                loginJPanel.enableCompents(true);
                bTriggerBtnClick = false;
                chnlist.clear();
                chnComboBox.setModel(new DefaultComboBoxModel(chnlist));
            }
        }
    }

    /**
     * 开始实时监视按钮事件
     */
    private void startRealPlay() {
        if (loginHandle.longValue() == 0) {
            System.err.println("Please login first");
            JOptionPane.showMessageDialog(mainFrame, "请先登录");
            return;
        }

        int channel = chnComboBox.getSelectedIndex(); // 预览通道号
        int playType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay; // 实时预览

        playHandle = netsdkApi.CLIENT_RealPlayEx(loginHandle, channel, Native.getComponentPointer(realplayWindow), playType);
        if (playHandle.longValue() == 0) {
            int error = netsdkApi.CLIENT_GetLastError();
            JOptionPane.showMessageDialog(mainFrame, "开始实时监视失败，错误码：" + String.format("[0x%x]", error));
        } else {
            System.out.println("[通道" + channel + "] 拉流成功！");
            messageTextArea.append(String.format("[通道 %2d ] 拉流成功！\n", channel));
        }
    }

    /**
     * 结束实时预览按钮事件
     */
    private void stopRealPlay() {
        if (playHandle.longValue() == 0) {
            System.out.println("Make sure the realplay Handle is valid");
            messageTextArea.append("Make sure the realplay Handle is valid\n");
            return;
        }

        if (netsdkApi.CLIENT_StopRealPlayEx(playHandle)) {
            System.out.println("Success to stop realplay");
            messageTextArea.append("Success to stop realplay\n");

            playHandle.setValue(0);
            realplayWindow.repaint();
        }
    }

    // 订阅实时上传智能分析数据
    private void attachIVSEvent() {

        /**
         * 说明：
         * 	通道数可以在有登录是返回的信息 m_stDeviceInfo.byChanNum 获取
         *  下列仅订阅了 0 通道的智能事件.
         */
        int bNeedPicture = 1; // 是否需要图片
        int ChannelId = chnComboBox.getSelectedIndex(); //  通道

        attachHandle = netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, ChannelId, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, m_AnalyzerDataCB, null, null);
        if (attachHandle.longValue() != 0) {
            System.out.println("[通道" + ChannelId + "] 订阅成功！");
            messageTextArea.append(String.format("[通道 %2d ] 订阅成功！\n", ChannelId));

        } else {
            System.out.printf("CLIENT_RealLoadPictureEx Failed!LastError = %x\n", netsdkApi.CLIENT_GetLastError());
            messageTextArea.append(String.format("CLIENT_RealLoadPictureEx Failed!LastError = %x\n", netsdkApi.CLIENT_GetLastError()));
        }

        eventCBQueueService.activeService(); // 启动队列
    }

    // 停止上传智能分析数据－图片
    private void detachIVSEvent() {
        if (0 != attachHandle.longValue()) {
            netsdkApi.CLIENT_StopLoadPic(attachHandle);
            System.out.println("Stop detach IVS event");
            messageTextArea.append("Stop detach IVS even\n");

            attachHandle.setValue(0);
        }

        eventCBQueueService.destroy();  // 关闭队列
    }

    /* 智能报警事件回调 */
    public class fAnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack/*, StdCallCallback*/ {
        private final String m_imagePath = "./PlateNumber/";
        NET_MSG_OBJECT plateObject; // 车牌信息
        NET_MSG_OBJECT vehicleObject; // 车辆信息
        NET_TIME_EX utc; // 事件时间

        int lane = 0; // 车道号
        String EventMsg; // 事件信息
        int nConfide = 0; // 置信度, 只有特定设备才支持，一般设备默认都是0不填充

        private String encode;

        public fAnalyzerDataCB() {
            File path = new File(m_imagePath);
            if (!path.exists()) {
                path.mkdir();
            }
            initRecord();

            initEncodeType();

        }

        // 解码格式
        private void initEncodeType() {
            String osPrefix = getOsPrefix();
            if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
                encode = "GBK";
            } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
                encode = "UTF-8";
            }
        }

        // 初始化
        private void initRecord() {
            plateObject = new NET_MSG_OBJECT(); // 车牌信息
            vehicleObject = new NET_MSG_OBJECT(); // 车辆信息
            utc = new NET_TIME_EX(); // 事件时间
            lane = -1; // 车道号
            EventMsg = ""; // 事件信息
            nConfide = 0; // 置信度, 只有特定设备才支持，一般设备默认都是0不填充
        }

        // 回调
        public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
                          Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            initRecord(); // init record
            if (lAnalyzerHandle.longValue() == 0) return -1;
            if (pAlarmInfo == null) return 0;

            System.out.println("--------dwAlarmType-------------" + dwAlarmType);
            messageTextArea.append("--------------Event Received----------------\n");
            // 获取事件信息
            NetSDKLib.NET_EVENT_FILE_INFO fileInfo = new NetSDKLib.NET_EVENT_FILE_INFO();

            switch (dwAlarmType) {
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING: /// 车位有车事件
                {
                    messageTextArea.append("<Event> TRAFFIC PARKING SPACE [ PARKING ]\n");

                    NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO msg = new NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;
                    fileInfo = msg.stuFileInfo;

                    // 抓拍到物体的数据
                    NET_MSG_OBJECT msgObject = msg.stuObject;
                    NET_PIC_INFO msgObjPicInfo = msgObject.stPicInfo;
                    if (msgObjPicInfo != null) {
                        int msgPicOffset = msgObjPicInfo.dwOffSet;
                        int msgPicLength = msgObjPicInfo.dwFileLenth;
                        String msgPicName = String.format("%s_%s_%s.jpg", "Parking", utc.toStringTime().replaceAll("[^0-9]", "-"), "plate");
                        String msgPicPath = m_imagePath + msgPicName;
                        eventCBQueueService.addEvent(new SaveTrafficPicHandler(pBuffer, msgPicOffset, msgPicLength, msgPicPath));
                    }

                    // 公共信息图片
                    EVENT_COMM_INFO commInfo = msg.stCommInfo;
                    int nPictureNum = commInfo.nPictureNum;    // 原始图片张数

                    for (int i = 0; i < nPictureNum; i++) {
                        int length = commInfo.stuPicInfos[i].nLength;
                        int offSet = commInfo.stuPicInfos[i].nOffset;
                        String fileName = String.format("%s_%s_%02d.jpg", "Parking", utc.toStringTime().replaceAll("[^0-9]", "-"), i);
                        String savePath = m_imagePath + fileName;
                        // ToolKits.savePicture(pBuffer, offSet, length, savePath);

                        eventCBQueueService.addEvent(new SaveTrafficPicHandler(pBuffer, offSet, length, savePath));
                    }

                    printInfo(fileInfo, msg.stTrafficCar);

                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING: /// 车位无车事件
                {
                    messageTextArea.append("<Event> TRAFFIC PARKING SPACE [ NO PARKING ]\n");

                    NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO msg = new NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    plateObject = msg.stuObject;
                    vehicleObject = msg.stuVehicle;
                    utc = msg.UTC;
                    lane = msg.nLane;
                    fileInfo = msg.stuFileInfo;
                    EventMsg = "[ " + utc.toStringTime() + " ] " + "车位无车事件";

                    // 抓拍到物体的图片
                    NET_MSG_OBJECT msgObject = msg.stuObject;
                    NET_PIC_INFO msgObjPicInfo = msgObject.stPicInfo;
                    if (msgObjPicInfo != null) {
                        int msgPicOffset = msgObjPicInfo.dwOffSet;
                        int msgPicLength = msgObjPicInfo.dwFileLenth;
                        String msgPicName = String.format("%s_%s_%s.jpg", "NoParking", utc.toStringTime().replaceAll("[^0-9]", "-"), "plate");
                        String msgPicPath = m_imagePath + msgPicName;
                        eventCBQueueService.addEvent(new SaveTrafficPicHandler(pBuffer, msgPicOffset, msgPicLength, msgPicPath));
                    }
                    // 公共图片
                    EVENT_COMM_INFO commInfo = msg.stCommInfo;
                    int nPictureNum = commInfo.nPictureNum;

                    for (int i = 0; i < nPictureNum; i++) {
                        int length = commInfo.stuPicInfos[i].nLength;
                        int offSet = commInfo.stuPicInfos[i].nOffset;
                        String fileName = String.format("%s_%s_%02d.jpg", "NoParking", utc.toStringTime().replaceAll("[^0-9]", "-"), i);
                        String savePath = m_imagePath + fileName;
                        // ToolKits.savePicture(pBuffer, offSet, length, savePath);

                        eventCBQueueService.addEvent(new SaveTrafficPicHandler(pBuffer, offSet, length, savePath));
                    }

                    printInfo(fileInfo, msg.stTrafficCar);

                    break;
                }
                default:
                    System.out.printf("Get Event 0x%x\n", dwAlarmType);
                    EventMsg = "未处理事件 dwAlarmType = " + String.format("0x%x", dwAlarmType);
                    break;
            }
            return 0;
        }

        private void printInfo(NetSDKLib.NET_EVENT_FILE_INFO fileInfo, NetSDKLib.DEV_EVENT_TRAFFIC_TRAFFICCAR_INFO stTrafficCar) {
            // Todo 展示 EVENT_FILE_INFO 的内容

            String message = null;
            try {
                message = "SnapShotTime: " + stTrafficCar.stSnapTime.toStringTime()
                        + "\nParkingPlaceNo：" + new String(stTrafficCar.szCustomParkNo, encode).trim()
                        + "\nVehicleDirection: " + stTrafficCar.byDirection
                        + "\nVehicleSize: " + stTrafficCar.nVehicleSize
                        + "\nszPlateColor: " + new String(stTrafficCar.szPlateColor).trim()
                        + "\nszVehicleColor: " + new String(stTrafficCar.szVehicleColor).trim();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            System.out.println(message);
            messageTextArea.append(message + "\n");
        }
    }

    ///////////////////////////////////// 异步图片存储队列 ////////////////////////////////////

    private QueueGeneration eventCBQueueService = new QueueGeneration();

    private class SaveTrafficPicHandler implements EventTaskHandler {
        private static final long serialVersionUID = 1L;

        private final byte[] imgBuffer;
        private final int length;
        private final String savePath;

        public SaveTrafficPicHandler(Pointer pBuf, int dwBufOffset, int dwBufSize, String sDstFile) {

            this.imgBuffer = pBuf.getByteArray(dwBufOffset, dwBufSize);
            this.length = dwBufSize;
            this.savePath = sDstFile;
        }

        @Override
        public void eventCallBackProcess() {

            System.out.println("保存图片中...路径：" + savePath);
            messageTextArea.append("保存图片中...路径：" + savePath + "\n");

            try {
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(savePath)));
                out.write(imgBuffer, 0, length);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /////////////////////////////////界面组件/////////////////////////////////////////////////
    ///////////////////////////// 界面组件相关成员变量  ////////////////////////////////////////

    /**
     * 设置边框
     */
    private void setBorderEx(JComponent object, String title, int width) {
        Border innerBorder = BorderFactory.createTitledBorder(title);
        Border outerBorder = BorderFactory.createEmptyBorder(width, width, width, width);
        object.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
    }

    /**
     * 登录面板
     */
    private class LoginPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        public LoginPanel() {
            loginBtn = new JButton("登入");
            logoutBtn = new JButton("登出");
            nameLabel = new JLabel("用户名");
            passwordLabel = new JLabel("密码");
            nameTextArea = new JTextField(username, 8);
            passwordTextArea = new JPasswordField(password, 8);
            ipLabel = new JLabel("设备地址");
            portLabel = new JLabel("端口号");
            ipTextArea = new JTextField(address, 15);
            portTextArea = new JTextField("37777", 6);

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

            enableCompents(false);

            // 登录按钮. 监听事件
            loginBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loginBtnPerformed(e);
                }
            });

            // 登出按钮. 监听事件
            logoutBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    logoutBtnPerformed(e);
                    enableCompents(false);
                }
            });
        }

        public void enableCompents(boolean enable) {
            loginBtn.setEnabled(!enable);
            logoutBtn.setEnabled(enable);
        }

        private JButton loginBtn;
        private JButton logoutBtn;
        private JLabel nameLabel;
        private JLabel passwordLabel;
        private JLabel ipLabel;
        private JLabel portLabel;
    }

    /**
     * 事件信息显示面板
     */
    private class MessagePanel extends JPanel {

        private static final long serialVersionUID = 1L;

        public MessagePanel() {
            setBorderEx(this, "事件信息提示", 2);

            Dimension dim = getPreferredSize();
            dim.height = 226;
            setPreferredSize(dim);
            setLayout(new BorderLayout());

            messageTextArea = new JTextArea();

            add(new JScrollPane(messageTextArea), BorderLayout.CENTER);
        }
    }

    /**
     * 预览面板
     */
    private class RealPlayPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        public RealPlayPanel() {
            setBorderEx(this, "实时预览", 2);
            setLayout(new BorderLayout());
            Dimension dim = getPreferredSize();
            dim.height = 367;
            dim.width = 480;
            setPreferredSize(dim);

            realplayWindow = new Panel();
            chnComboBox = new JComboBox(chnlist);
            attachButton = new JButton("订阅");
            realplayButton = new JButton("监视");

            realplayWindow.setBackground(new Color(153, 240, 255));
            realplayWindow.setForeground(new Color(0, 0, 0));
            realplayWindow.setBounds(5, 5, 350, 290);
            realplayWindow.setSize(358, 294);

            JPanel btnPanel = new JPanel();
            btnPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            btnPanel.setLayout(new GridLayout(1, 3, 5, 5));
            btnPanel.add(chnComboBox);
            btnPanel.add(attachButton);
            btnPanel.add(realplayButton);

            add(realplayWindow, BorderLayout.CENTER);
            add(btnPanel, BorderLayout.SOUTH);

            // 订阅按钮动作监听
            attachButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!bAttachFlag) {
                        attachIVSEvent();
                        attachButton.setText("停止订阅");
                        bAttachFlag = true;
                    } else {
                        detachIVSEvent();
                        attachButton.setText("订阅");
                        bAttachFlag = false;
                    }
                }
            });

            // 监视按钮监听
            realplayButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!bRealplayFlags) {
                        startRealPlay();
                        realplayButton.setText("停止监视");
                        bRealplayFlags = true;
                    } else {
                        stopRealPlay();
                        realplayButton.setText("监视");
                        bRealplayFlags = false;
                    }
                }
            });
        }
    }

    /**
     * 主界面组件
     */
    private JFrame mainFrame;

    /**
     * 登录条组件
     */
    private JTextField nameTextArea;
    private JPasswordField passwordTextArea;

    private JTextField ipTextArea;
    private JTextField portTextArea;

    private LoginPanel loginJPanel;

    /**
     * 事件信息显示组件
     */
    private MessagePanel messagePanel;
    private JTextArea messageTextArea;

    /**
     * 实时预览组件
     */
    private RealPlayPanel realPlayPanel;
    private Panel realplayWindow;
    private JButton attachButton;
    private JButton realplayButton;
    private JComboBox chnComboBox;
}

public class TrafficParkingSpaceEvent {

    public static void main(String[] args) throws SecurityException, IllegalArgumentException {

        ITSParkingSpaceEventMsg demo = new ITSParkingSpaceEventMsg();
    }
}
