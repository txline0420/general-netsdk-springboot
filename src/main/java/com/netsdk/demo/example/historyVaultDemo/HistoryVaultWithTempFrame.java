package com.netsdk.demo.example.historyVaultDemo;

import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static com.netsdk.demo.example.historyVaultDemo.HistoryVaultLogon.m_hLoginHandle;
import static com.netsdk.demo.example.historyVaultDemo.HistoryVaultLogon.netsdk;

public class HistoryVaultWithTempFrame extends HistoryVaultWithTemp {

    private HistoryVaultMainFrame historyVaultMainFrame = new HistoryVaultMainFrame();

    private boolean similarlyRangeEnable = true;   // 是否启用相似度搜索
    private boolean ageRangeEnable = true;        // 是否启用年龄区间搜索

    private int nCurCount = 0;   // 查询次数的数据
    private int nFindCount = 1;  // 查询数据的序号
    private int nTotalCount = 0; // 可查询的总数
    private int selectedNo = 0;  // 表格中选取的序号

    // 存储一次搜索获取的所有数据
    private ArrayList<LinkedHashMap<String, Object>> queryList = new ArrayList<>();

    private NetSDKLib.LLong lFindHandle;  // 查询句柄

    private static String picSaveFolderPath = "./historyPic/";  // 图片保存文件夹路径

    ////////////////////////////// 发送搜索信息用的字典 ////////////////////////////
    // 性别
    private static final LinkedHashMap<String, Integer> searchSexDic = new LinkedHashMap<String, Integer>() {{
        put("所有", 0);
        put("男性", 1);
        put("女性", 2);
    }};
    // 眼镜
    private static final LinkedHashMap<String, Integer> searchGlassesDic = new LinkedHashMap<String, Integer>() {{
        put("未知", 0);
        put("不戴眼镜", 1);
        put("戴眼镜", 2);
    }};
    // 口罩
    private static final LinkedHashMap<String, Integer> searchMaskDic = new LinkedHashMap<String, Integer>() {{
        put("未知", 0);
        put("不戴口罩", 2);
        put("戴口罩", 3);
    }};
    // 胡子
    private static final LinkedHashMap<String, Integer> searchBeardDic = new LinkedHashMap<String, Integer>() {{
        put("未知", 0);
        put("没有胡子", 2);
        put("有胡子", 3);
    }};
    // 表情
    private static final LinkedHashMap<String, Integer> searchEmotionDic = new LinkedHashMap<String, Integer>() {{
        put("未知", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_UNKNOWN);
        put("微笑", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_SMILE);
        put("愤怒", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_ANGER);
        put("悲伤", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_SADNESS);
        put("厌恶", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_DISGUST);
        put("害怕", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_FEAR);
        put("惊讶", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_SURPRISE);
        put("正常", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_NEUTRAL);
        put("大笑", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_LAUGH);
        put("高兴", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_HAPPY);
        put("困惑", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_CONFUSED);
        put("尖叫", NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_SCREAM);
    }};
    // 温度单位
    private static final LinkedHashMap<String, Integer> searchTempUnitDic = new LinkedHashMap<String, Integer>() {{
        put("摄氏度", NetSDKLib.EM_TEMPERATURE_UNIT.EM_TEMPERATURE_CENTIGRADE);
        put("华氏度", NetSDKLib.EM_TEMPERATURE_UNIT.EM_TEMPERATURE_FAHRENHEIT);
        put("开尔文", NetSDKLib.EM_TEMPERATURE_UNIT.EM_TEMPERATURE_KELVIN);
    }};

    // 初始化和登录
    private void initAndLogon() {
        // 登陆初始化
        HistoryVaultLogon.init(HistoryVaultLogon.DisConnectCallBack.getInstance(),
                HistoryVaultLogon.HaveReConnectCallBack.getInstance());

        // 设备登陆，如果不支持高安全，请使用普通的 TCP 登陆函数 login(), Demo中所有涉及句柄 m_hLoginHandle 也要相应的换掉
        HistoryVaultLogon.LoginWithHighLevel();
    }

    // 登出与退出
    private void logoutAndExit() {
        // 退出登陆
        HistoryVaultLogon.logOut();
        // 清理资源并退出程序
        HistoryVaultLogon.cleanAndExit();
    }

    // Swing 界面
    private class HistoryVaultMainFrame extends JFrame {

        private static final long serialVersionUID = 1L;

        private NetSDKLib.LLong m_searchHandle = new NetSDKLib.LLong(0);  // 查询句柄

        // 用于保存对比图的图片缓存，用于多张图片显示
        private ArrayList<BufferedImage> arrayListBuffer = new ArrayList<>();

        // 用于保存对比图的数据
        private String[] candidateStr = new String[7];

        // 保存抓拍图和对比图
        private BufferedImage personBufferedImage = null;
        private BufferedImage[] candidateBufferedImages = new BufferedImage[20];

        /////////////////////////////////////////////////////////////
        ////////////////////////////组件//////////////////////////////

        ///////////////////// 搜索信息设置组件 ////////////////////////

        private HistoryVaultOptionPanel optionPanel;


        // StartTime Panel
        private StartTimePanel startTimePanel;

        // StartTime Panel 子组件
        private final JLabel startYearLabel = new JLabel("年");
        private final JLabel startMonthLabel = new JLabel("月");
        private final JLabel startDayLabel = new JLabel("日");
        private final JLabel startHourLabel = new JLabel("时");
        private final JLabel startMinuteLabel = new JLabel("分");
        private final JLabel startSecondLabel = new JLabel("秒");
        private JTextField startYearField;
        private JTextField startMonthField;
        private JTextField startDayField;
        private JTextField startHourField;
        private JTextField startMinuteField;
        private JTextField startSecondField;

        // EndTime Panel
        private EndTimePanel endTimePanel;

        // EndTime Panel 子组件
        private final JLabel endYearLabel = new JLabel("年");
        private final JLabel endMonthLabel = new JLabel("月");
        private final JLabel endDayLabel = new JLabel("日");
        private final JLabel endHourLabel = new JLabel("时");
        private final JLabel endMinuteLabel = new JLabel("分");
        private final JLabel endSecondLabel = new JLabel("秒");
        private JTextField endYearField;
        private JTextField endMonthField;
        private JTextField endDayField;
        private JTextField endHourField;
        private JTextField endMinuteField;
        private JTextField endSecondField;


        // Other Option Panel
        private OtherOptionPanel otherOptionPanel;
        private final JLabel channelLabel = new JLabel("历史库来源通道号");
        private final JLabel personNameLabel = new JLabel("候选人中包含此姓名");
        private final JLabel personSexLabel = new JLabel("指定特定的性别");
        private final JLabel personGlassesLabel = new JLabel("指定戴眼镜状态");
        private final JLabel personMaskLabel = new JLabel("指定戴口罩状态");
        private final JLabel personBeardLabel = new JLabel("指定胡子的状态");
        private final JLabel personEmotionLabel = new JLabel("指定特殊的表情");
        private JComboBox channelCombo;
        private JTextField personNameField;
        private JComboBox personSexCombo;
        private JComboBox personGlassesCombo;
        private JComboBox personMaskCombo;
        private JComboBox personBeardCombo;
        private JComboBox personEmotionCombo;

        // Other Option Panel 子组件 Similarity panel
        private SimilarityPanel similarityPanel;

        // Similarity panel 子组件
        private final JLabel similarityLowLabel = new JLabel("下界");
        private final JLabel similarityUpLabel = new JLabel("上界");
        private JRadioButton similarityRadio;
        private JTextField similarityLowField;
        private JTextField similarityUpField;

        // Other Option Panel 子组件 Age range panel
        private AgeRangePanel ageRangePanel;

        // Age range panel 子组件
        private final JLabel ageRangeLowLabel = new JLabel("下界");
        private final JLabel ageRangeUpLabel = new JLabel("上界");
        private JRadioButton ageRangeRadio;
        private JTextField ageRangeLowField;
        private JTextField ageRangeUpField;

        ///////////////////// 搜索性息面板 ////////////////////////

        private HistoryVaultSearchDataPanel searchDataPanel;
        private SearchOperatePanel opratePanel;
        // 搜索指令面板
        private JButton startSearchButton;
        private JButton findNextButton;
        private JButton endSearchButton;
        private JLabel totalCountLabel;

        // 搜索数据面板
        private GroupListPanel groupListPanel;
        private DefaultTableModel groupInfoModel;
        private JTable groupInfoTable;

        // 详情展示面板
        private DetailDataPanel detailDataPanel;

        // 详情展示面板 子面板
        private DisplayPanel snapPicDisplayPanel;
        private DisplayPanel candidatePicDisplayPanel;
        private DefaultTableModel detailInfoModel;
        private DetailDataTablePanel detailDataTablePanel;
        private JTable detailInfoTable;


        public HistoryVaultMainFrame() {
            initAndLogon();  // 先初始化再登录，登录IP等信息请在 HistoryVaultLogon 里设置

            setTitle("HistoryVaultViaSDK");
            setSize(1080, 756);
            setLayout(new BorderLayout());
            setLocationRelativeTo(null);
            setVisible(true);

            // 加载搜索参数设置面板
            optionPanel = new HistoryVaultOptionPanel();
            add(optionPanel, BorderLayout.WEST);

            // 加载数据展示面板
            searchDataPanel = new HistoryVaultSearchDataPanel();
            add(searchDataPanel, BorderLayout.CENTER);

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.out.println("Window Closing");
                    dispose();
                    // Todo dispose the child Frames
                    logoutAndExit();  // 登出并推出
                }
            });
        }


        /***********************************************************************************************
         * 									界面布局面板												   *
         ***********************************************************************************************/
        // 设置边框
        private void setBorderEx(JComponent object, String title, int[] widths) {
            Border innerBorder = BorderFactory.createTitledBorder(title);
            Border outerBorder = BorderFactory.createEmptyBorder(widths[0], widths[1], widths[2], widths[3]);
            object.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
        }

        // 历史库下载参数操作面板
        public class HistoryVaultOptionPanel extends JPanel {

            private static final long serialVersionUID = 1L;

            public HistoryVaultOptionPanel() {
                setBorderEx(this, "搜索参数设置", new int[]{2, 2, 2, 2});
                Dimension dim = this.getPreferredSize();
                dim.width = 260;
                this.setPreferredSize(dim);
                setLayout(new FlowLayout());

                // 开始时间面板
                startTimePanel = new StartTimePanel();
                add(startTimePanel);

                // 结束时间面板
                endTimePanel = new EndTimePanel();
                add(endTimePanel);

                // 其他参数设置面板
                otherOptionPanel = new OtherOptionPanel();
                add(otherOptionPanel);
            }
        }

        // 开始时间设置面板
        public class StartTimePanel extends JPanel {

            public StartTimePanel() {
                setBorderEx(this, "起始时间", new int[]{2, 2, 2, 2});
                this.setPreferredSize(new Dimension(245, 90));
                this.setLayout(new FlowLayout(FlowLayout.LEFT));

                startYearField = new JTextField("2020", 5);
                startMonthField = new JTextField("5", 4);
                startDayField = new JTextField("11", 4);
                startHourField = new JTextField("0", 4);
                startMinuteField = new JTextField("0", 4);
                startSecondField = new JTextField("0", 4);

                this.add(startYearLabel);
                this.add(startYearField);
                this.add(startMonthLabel);
                this.add(startMonthField);
                this.add(startDayLabel);
                this.add(startDayField);
                this.add(startHourLabel);
                this.add(startHourField);
                this.add(startMinuteLabel);
                this.add(startMinuteField);
                this.add(startSecondLabel);
                this.add(startSecondField);

            }

        }

        // 结束时间设置面板
        public class EndTimePanel extends JPanel {

            public EndTimePanel() {
                setBorderEx(this, "结束时间", new int[]{2, 2, 2, 2});
                this.setPreferredSize(new Dimension(245, 90));
                this.setLayout(new FlowLayout(FlowLayout.LEFT));

                endYearField = new JTextField("2020", 5);
                endMonthField = new JTextField("5", 4);
                endDayField = new JTextField("11", 4);
                endHourField = new JTextField("23", 4);
                endMinuteField = new JTextField("59", 4);
                endSecondField = new JTextField("59", 4);

                this.add(endYearLabel);
                this.add(endYearField);
                this.add(endMonthLabel);
                this.add(endMonthField);
                this.add(endDayLabel);
                this.add(endDayField);
                this.add(endHourLabel);
                this.add(endHourField);
                this.add(endMinuteLabel);
                this.add(endMinuteField);
                this.add(endSecondLabel);
                this.add(endSecondField);

            }

        }

        // 设置人其他搜索条件
        public class OtherOptionPanel extends JPanel {

            public OtherOptionPanel() {
                setBorderEx(this, "其他参数", new int[]{2, 2, 2, 2});
                this.setPreferredSize(new Dimension(245, 450));
                this.setLayout(new FlowLayout(FlowLayout.LEFT));

                similarityRadio = new JRadioButton("启动相似度查询", true);
                similarityPanel = new SimilarityPanel();
                EnableAllInnerComponent(similarityPanel);

                ageRangeRadio = new JRadioButton("启动年龄区间查询", true);
                ageRangePanel = new AgeRangePanel();
                EnableAllInnerComponent(ageRangePanel);

                personNameField = new JTextField("", 5);

                Integer[] channelComboList = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
                channelCombo = new JComboBox(channelComboList);

                String[] personSexList = searchSexDic.keySet().toArray(new String[0]);
                personSexCombo = new JComboBox(personSexList);

                String[] personGlassesList = searchGlassesDic.keySet().toArray(new String[0]);
                personGlassesCombo = new JComboBox(personGlassesList);

                String[] personMaskList = searchMaskDic.keySet().toArray(new String[0]);
                personMaskCombo = new JComboBox(personMaskList);

                String[] personBeardList = searchBeardDic.keySet().toArray(new String[0]);
                personBeardCombo = new JComboBox(personBeardList);

                String[] personEmotionList = searchEmotionDic.keySet().toArray(new String[0]);
                personEmotionCombo = new JComboBox(personEmotionList);

                this.add(similarityRadio);
                this.add(similarityPanel);
                this.add(ageRangeRadio);
                this.add(ageRangePanel);
                this.add(channelLabel);
                this.add(channelCombo);
                // this.add(personNameLabel);
                // this.add(personNameField);
                this.add(personSexLabel);
                this.add(personSexCombo);
                this.add(personGlassesLabel);
                this.add(personGlassesCombo);
                this.add(personMaskLabel);
                this.add(personMaskCombo);
                this.add(personBeardLabel);
                this.add(personBeardCombo);
                this.add(personEmotionLabel);
                this.add(personEmotionCombo);

                similarityRadio.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (similarityRadio.isSelected()) {
                            similarlyRangeEnable = true;
                            EnableAllInnerComponent(similarityPanel);
                        } else {
                            similarlyRangeEnable = false;
                            DisableAllInnerComponent(similarityPanel);
                        }
                    }
                });

                ageRangeRadio.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (ageRangeRadio.isSelected()) {
                            ageRangeEnable = true;
                            EnableAllInnerComponent(ageRangePanel);
                        } else {
                            ageRangeEnable = false;
                            DisableAllInnerComponent(ageRangePanel);
                        }
                    }
                });
            }
        }

        // 相似度设置面板
        public class SimilarityPanel extends JPanel {

            public SimilarityPanel() {
                setBorderEx(this, "相似度设置", new int[]{2, 2, 2, 2});
                this.setPreferredSize(new Dimension(180, 60));
                this.setLayout(new FlowLayout(FlowLayout.LEFT));

                similarityLowField = new JTextField("80", 3);
                similarityUpField = new JTextField("100", 3);

                this.add(similarityLowLabel);
                this.add(similarityLowField);
                this.add(similarityUpLabel);
                this.add(similarityUpField);
            }
        }

        // 年龄设置面板
        public class AgeRangePanel extends JPanel {


            public AgeRangePanel() {
                setBorderEx(this, "相似度设置", new int[]{2, 2, 2, 2});
                this.setPreferredSize(new Dimension(180, 60));
                this.setLayout(new FlowLayout(FlowLayout.LEFT));

                ageRangeLowField = new JTextField("20", 3);
                ageRangeUpField = new JTextField("39", 3);

                this.add(ageRangeLowLabel);
                this.add(ageRangeLowField);
                this.add(ageRangeUpLabel);
                this.add(ageRangeUpField);
            }

        }

        // 设置搜索和数据展示面板
        public class HistoryVaultSearchDataPanel extends JPanel {

            public HistoryVaultSearchDataPanel() {
                setBorderEx(this, "搜集及数据展示", new int[]{2, 2, 2, 2});
                setLayout(new BorderLayout());

                opratePanel = new SearchOperatePanel();
                groupListPanel = new GroupListPanel();
                detailDataPanel = new DetailDataPanel();

                add(opratePanel, BorderLayout.NORTH);
                add(groupListPanel, BorderLayout.CENTER);
                add(detailDataPanel, BorderLayout.SOUTH);
            }

        }

        // 查询控制台
        public class SearchOperatePanel extends JPanel {

            public SearchOperatePanel() {
                setBorderEx(this, "搜索面板", new int[]{2, 2, 2, 2});
                setLayout(new BorderLayout());
                Dimension dim = this.getPreferredSize();
                dim.height = 80;
                this.setPreferredSize(dim);
                setLayout(new FlowLayout());

                startSearchButton = new JButton("发送搜索指令");
                findNextButton = new JButton("获取下一组数据");
                endSearchButton = new JButton("发送结束搜索");
                totalCountLabel = new JLabel("共获取到：0个数据");

                add(startSearchButton);
                add(findNextButton);
                add(endSearchButton);
                add(totalCountLabel);

                findNextButton.setEnabled(false);
                endSearchButton.setEnabled(false);

                startSearchButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        if (nCurCount == 0) startSearchButton.setEnabled(false);

                        queryList = new ArrayList<>();
                        resetSearchedData();
                        findNextButton.setEnabled(true);
                        endSearchButton.setEnabled(true);
                        requestForSearch();
                    }
                });

                findNextButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {

                        listTheFileNextData();
                        nCurCount = nCurCount + 1;

                        nTotalCount = nTotalCount - nMaxCount;

                        if (nTotalCount <= 0) {
                            findNextButton.setEnabled(false);
                        }
                    }
                });

                endSearchButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        endTheSearch();
                        resetSearchedData();
                        clearTableModel(groupInfoModel);
                        clearTableModel(detailInfoModel);
                        clearCaptureOnPanel(snapPicDisplayPanel);
                        clearCaptureOnPanel(candidatePicDisplayPanel);

                        startSearchButton.setEnabled(true);
                        findNextButton.setEnabled(false);
                        endSearchButton.setEnabled(false);
                    }
                });
            }

        }

        // 搜索数据列表
        public class GroupListPanel extends JPanel {

            Object[][] groupData = null;    // 人脸库列表

            private String[] groupName = {"序号", "性别", "年龄", "表情", "口罩", "胡子", "魅力", "眼镜", "温度", "最匹配候选人"};

            public GroupListPanel() {
                setBorderEx(this, "历史库数据", new int[]{2, 2, 2, 2});
                setLayout(new BorderLayout());

                groupData = new Object[20][10];
                groupInfoModel = new DefaultTableModel(groupData, groupName);
                groupInfoTable = new JTable(groupInfoModel) {
                    @Override // 不可编辑
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

                groupInfoModel = (DefaultTableModel) groupInfoTable.getModel();

                groupInfoTable.getColumnModel().getColumn(0).setMinWidth(10);
                groupInfoTable.getColumnModel().getColumn(1).setMinWidth(10);
                groupInfoTable.getColumnModel().getColumn(2).setMinWidth(10);
                groupInfoTable.getColumnModel().getColumn(4).setMinWidth(10);
                groupInfoTable.getColumnModel().getColumn(5).setMinWidth(10);
                groupInfoTable.getColumnModel().getColumn(6).setMinWidth(10);
                groupInfoTable.getColumnModel().getColumn(7).setMinWidth(10);
                groupInfoTable.getColumnModel().getColumn(8).setMinWidth(10);

                groupInfoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 只能选中一行

                // 列表显示居中
                DefaultTableCellRenderer dCellRenderer = new DefaultTableCellRenderer();
                dCellRenderer.setHorizontalAlignment(JLabel.CENTER);
                groupInfoTable.setDefaultRenderer(Object.class, dCellRenderer);

                JScrollPane scrollPane = new JScrollPane(groupInfoTable);
                add(scrollPane, BorderLayout.CENTER);

                groupInfoTable.addMouseListener(new MouseListener() {


                    @Override
                    public void mouseClicked(MouseEvent arg0) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                int rowSelect = groupInfoTable.getSelectedRow();

                                if (rowSelect < 0) {
                                    return;
                                }
                                if (String.valueOf(groupInfoModel.getValueAt(rowSelect, 0)).trim().equals("")
                                        || groupInfoModel.getValueAt(rowSelect, 0) == null) {
//                                    // Todo 清空
                                    testLocalDataDisplay();
                                } else {
                                    selectedNo = rowSelect;
                                    downloadPicAndShowDetail();
                                }
                            }
                        });
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {

                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {

                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {

                    }

                    @Override
                    public void mouseExited(MouseEvent e) {

                    }
                });
            }

        }

        // 详细信息展示
        public class DetailDataPanel extends JPanel {

            DetailDataPanel() {
                setBorderEx(this, "详情面板", new int[]{2, 2, 2, 2});
                Dimension dim = this.getPreferredSize();
                dim.height = 260;
                this.setPreferredSize(dim);
                setLayout(new BorderLayout());

                snapPicDisplayPanel = new SnapPicDisplayPanel();
                candidatePicDisplayPanel = new CandidatePicDisplayPanel();
                detailDataTablePanel = new DetailDataTablePanel();

                add(snapPicDisplayPanel, BorderLayout.WEST);
                add(candidatePicDisplayPanel, BorderLayout.EAST);
                add(detailDataTablePanel, BorderLayout.CENTER);
            }
        }

        // 展示图片的抽象类
        public abstract class DisplayPanel extends JPanel {

            private BufferedImage image;   // 图片，用于作背景


            public DisplayPanel() {
                setLayout(new BorderLayout());
                Dimension dim = this.getPreferredSize();
                dim.width = 208;
                dim.height = 225;
                this.setPreferredSize(dim);
                setLayout(new FlowLayout());

                setOpaque(true);   // 非透明
                setLayout(null);
                setBackground(Color.GRAY);
                setForeground(new Color(0, 0, 0));
            }
            //设置图片的方法

            public void setImage(BufferedImage image) {
                this.image = image;
            }

            protected void paintComponent(Graphics g) {    //重写绘制组件外观
                if (image != null) {
                    g.drawImage(image, 0, 0, getWidth(), getHeight(), this);//绘制图片与组件大小相同
                }
                super.paintComponent(g); // 执行超类方法
            }

        }

        // 抓拍图展示
        public class SnapPicDisplayPanel extends DisplayPanel {

            SnapPicDisplayPanel() {
                super();
                setBorderEx(this, "抓拍图", new int[]{2, 2, 2, 2});
            }
        }

        // 对比图片展示
        public class CandidatePicDisplayPanel extends DisplayPanel {
            CandidatePicDisplayPanel() {
                super();
                setBorderEx(this, "候选图", new int[]{2, 2, 2, 2});
            }
        }

        // 详细数据展示
        public class DetailDataTablePanel extends JPanel {

            Object[][] detailData;
            String[] detailName = new String[]{"数据名称", "数据值"};

            public DetailDataTablePanel() {
                setBorderEx(this, "历史库数据", new int[]{2, 2, 2, 2});
                setLayout(new BorderLayout());

                detailData = new Object[11][2];
                detailInfoModel = new DefaultTableModel(detailData, detailName);
                detailInfoTable = new JTable(detailInfoModel) {
                    @Override // 不可编辑
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

                detailInfoModel = (DefaultTableModel) detailInfoTable.getModel();
                detailInfoTable.getColumnModel().getColumn(0).setPreferredWidth(30);

                detailInfoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 只能选中一行
                // 列表显示居中
                DefaultTableCellRenderer dCellRenderer = new DefaultTableCellRenderer();
                dCellRenderer.setHorizontalAlignment(JLabel.CENTER);
                detailInfoTable.setDefaultRenderer(Object.class, dCellRenderer);
                JScrollPane scrollPane = new JScrollPane(detailInfoTable);
                add(scrollPane, BorderLayout.CENTER);
            }

        }
    }

    // 初始化数据
    public void resetSearchedData() {
        queryList = new ArrayList<>();
        nCurCount = 0;
        nFindCount = 1;
        nTotalCount = 0;
        this.historyVaultMainFrame.totalCountLabel.setText("共获取到：0个数据");

        lFindHandle = new NetSDKLib.LLong(0);  // 检索句柄清空
    }

    // 下发搜索指令
    public void requestForSearch() {
        /////////////////////////// 【人脸库AI历史库检索】 下发检索条件，获取检索句柄 ///////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////
        // 选择查询类型->人脸历史库
        int type = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FACE;

        // <<<<<-----定义检索条件----->>>>>

        NetSDKLib.MEDIAFILE_FACERECOGNITION_PARAM findContent = new NetSDKLib.MEDIAFILE_FACERECOGNITION_PARAM();

        // 查询文件类型 jpg，这条必须写，且IVSS设备固定为1不可修改
        findContent.nFileType = 1;

        // 历史库开始时间->"StartTime"

        int startYear = Integer.valueOf(this.historyVaultMainFrame.startYearField.getText().trim());
        int startMonth = Integer.valueOf(this.historyVaultMainFrame.startMonthField.getText().trim());
        int startDay = Integer.valueOf(this.historyVaultMainFrame.startDayField.getText().trim());
        int startHour = Integer.valueOf(this.historyVaultMainFrame.startHourField.getText().trim());
        int startMinute = Integer.valueOf(this.historyVaultMainFrame.startMinuteField.getText().trim());
        int startSecond = Integer.valueOf(this.historyVaultMainFrame.startSecondField.getText().trim());
        findContent.stStartTime = new NetSDKLib.NET_TIME() {{
            setTime(startYear, startMonth, startDay, startHour, startMinute, startSecond);
        }};

        int endYear = Integer.valueOf(this.historyVaultMainFrame.endYearField.getText().trim());
        int endMonth = Integer.valueOf(this.historyVaultMainFrame.endMonthField.getText().trim());
        int endDay = Integer.valueOf(this.historyVaultMainFrame.endDayField.getText().trim());
        int endHour = Integer.valueOf(this.historyVaultMainFrame.endHourField.getText().trim());
        int endMinute = Integer.valueOf(this.historyVaultMainFrame.endMinuteField.getText().trim());
        int endSecond = Integer.valueOf(this.historyVaultMainFrame.endSecondField.getText().trim());
        // 历史库结束时间->"EndTime"
        findContent.stEndTime = new NetSDKLib.NET_TIME() {{
            setTime(endYear, endMonth, endDay, endHour, endMinute, endSecond);
        }};

        // AI历史库来源通道号
        int channel = (Integer) (this.historyVaultMainFrame.channelCombo.getSelectedItem());
        findContent.nChannelId = channel;

        if (similarlyRangeEnable) {
            findContent.bSimilaryRangeEnable = 1;      // 启用相似度检索
            int similaryLow = Integer.valueOf(this.historyVaultMainFrame.similarityLowField.getText().trim());
            int similaryUp = Integer.valueOf(this.historyVaultMainFrame.similarityUpField.getText().trim());
            // 设置检测相似度区间
            findContent.nSimilaryRange[0] = similaryLow;
            findContent.nSimilaryRange[1] = similaryUp;
        }

        // 人脸检测事件类型 查询所有类型，NetSDKLib.EM_FACERECOGNITION_ALARM_TYPE
        findContent.nAlarmType = NetSDKLib.EM_FACERECOGNITION_ALARM_TYPE.NET_FACERECOGNITION_ALARM_TYPE_ALL;

        // 启用扩展信息检索
        findContent.abPersonInfoEx = 1;    // 如果要设置下面的搜索信息，这项必须要写，且不可修改

        //        try {
        //            String name = this.historyVaultMainFrame.personNameField.getText().trim();
        //            findContent.stPersonInfoEx.szPersonName = name.getBytes(encode);   // 姓名
        //        } catch (UnsupportedEncodingException e) {
        //            e.printStackTrace();
        //        }

        int sex = searchSexDic.get(String.valueOf(this.historyVaultMainFrame.personSexCombo.getSelectedItem()).trim());
        findContent.stPersonInfoEx.bySex = (byte) sex;                            // 性别 0: 所有 1: 男 2: 女


        if (ageRangeEnable) {
            findContent.stPersonInfoEx.bAgeEnable = 1;                       // 启用年龄检索
            int ageRangeLow = Integer.valueOf(this.historyVaultMainFrame.ageRangeLowField.getText().trim());
            int ageRangeUp = Integer.valueOf(this.historyVaultMainFrame.ageRangeUpField.getText().trim());
            findContent.stPersonInfoEx.nAgeRange[0] = ageRangeLow;                    // 年龄下区间
            findContent.stPersonInfoEx.nAgeRange[1] = ageRangeUp;                    // 年龄上区间
        }

        int glasses = searchGlassesDic.get(String.valueOf(this.historyVaultMainFrame.personGlassesCombo.getSelectedItem()).trim());
        findContent.stPersonInfoEx.byGlasses = (byte) glasses;                        // 是否戴眼镜 0：未知 1：不戴 2：戴
        int mask = searchMaskDic.get(String.valueOf(this.historyVaultMainFrame.personMaskCombo.getSelectedItem()).trim());
        findContent.stPersonInfoEx.emMask = mask;                           // 是否带口罩 0: 未知 1: 未识别【一般不用】 2: 没戴口罩 3：戴口罩
        int beard = searchBeardDic.get(String.valueOf(this.historyVaultMainFrame.personBeardCombo.getSelectedItem()).trim());
        findContent.stPersonInfoEx.emBeard = beard;                          // 是否有胡子 0: 未知 1： 未识别【一般不用】 2: 没胡子 3: 有胡子

        findContent.stPersonInfoEx.nEmotionValidNum = 1;                 // 人脸特征数组有效个数, 如果为 0 则表示查询所有表情
        // 查询一种表情，惊讶 EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE
        int emotion = searchEmotionDic.get(String.valueOf(this.historyVaultMainFrame.personEmotionCombo.getSelectedItem()).trim());
        findContent.stPersonInfoEx.emEmotions[0] = emotion;

        // 特别指出，sdk的协议现在不支持设置测温检索条件

        // 参数写入内存
        findContent.write();
        // 调用 SDK FindFile(FaceRecognition) 接口，成功了会获取检索结果集的句柄 lFindHandle
        lFindHandle = netsdk.CLIENT_FindFileEx(m_hLoginHandle, type, findContent.getPointer(), null, 2000);
        if (lFindHandle.longValue() == 0) {
            System.err.println("FindFile(FaceRecognition) Failed!" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("检索指令下发成功，检索句柄：" + lFindHandle.longValue());
        findContent.read();

        ///////////////////////////  【人脸库AI历史库检索】 从结果集查询数据 ///////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////

        IntByReference pCount = new IntByReference();

        boolean rt = netsdk.CLIENT_GetTotalFileCount(lFindHandle, pCount, null, 2000);
        if (!rt) {
            System.err.println("获取搜索句柄：" + lFindHandle + " 的搜索内容量失败。");
            this.historyVaultMainFrame.findNextButton.setEnabled(false);
            this.historyVaultMainFrame.endSearchButton.setEnabled(true);
            return;
        }
        nTotalCount = pCount.getValue();
        this.historyVaultMainFrame.totalCountLabel.setText("共获取到：" + nTotalCount + "个数据");
        System.out.println("搜索句柄：" + lFindHandle + " 共获取到：" + nTotalCount + " 条数据。");
    }

    // 获取下一组数据
    public void listTheFileNextData() {

        DefaultTableModel fileModel = this.historyVaultMainFrame.groupInfoModel;
        clearTableModel(fileModel);

        System.out.println("在检索句柄：" + lFindHandle + " 内查询");
        int nRetCount = netsdk.CLIENT_FindNextFileEx(lFindHandle, nMaxCount, filePointer, fileMemorySize, null, 5000);

        // 从指针自定义获取数据，由于结构体非常大，为了速度考虑，我只能把需要的数据拷贝出指针地址
        // 下面列出的数据基本包括了所有 IVSS 设备支持返回的数据，部分返回是空的数据没有列出
        // 修改 GetPointerDataToStructArrFaceInfo 及它的附属方法一定要小心，偏移量一旦有误获取的数据会混乱。
        GetPointerDataToStructArrFaceInfo(filePointer, faceRecognitionInfos);

        for (int i = 0; i < nRetCount; i++) {
            // 处理数据，如何转存数据依据实际情况调整，这里就保存起来然后打印一下
            LinkedHashMap<String, Object> queryMap = new LinkedHashMap<>();

            // <<<<<-----查询序列----->>>>>
            nFindCount = i + nCurCount * nMaxCount;
            queryMap.put("FindCount", nFindCount);
            queryMap.put("SearchCount", nCurCount + 1);
            fileModel.setValueAt(String.valueOf(nFindCount + 1), i, 0); // 序号

            // <<<<<-----抓拍图的事件数据----->>>>>
            // 抓拍图来源通道
            queryMap.put("Channel", faceRecognitionInfos[i].nChannelId);
            // 抓拍图事件发生时间
            queryMap.put("EventTime", faceRecognitionInfos[i].stTime.toStringTime());
            // 抓拍人脸图长度(这个数据其实没有用处，因为返回的是图片地址。但由于设备有返回这个数据所以这里列一下)
            queryMap.put("FacePicLength", faceRecognitionInfos[i].stObjectPic.dwFileLenth);
            // 抓拍人脸图路径
            queryMap.put("FacePicPath", new String(faceRecognitionInfos[i].stObjectPic.szFilePath).trim());

            // 抓拍人脸图 图片在屏幕的型心, 0-8191相对坐标
            int[] FacePicCenter = new int[2];
            FacePicCenter[0] = faceRecognitionInfos[i].stuFaceCenter.nx;
            FacePicCenter[1] = faceRecognitionInfos[i].stuFaceCenter.ny;
            queryMap.put("FacePicCenter", FacePicCenter);

            // <<<<<-----抓拍图的人脸数据 普通数据----->>>>>

            // 抓拍图 性别 0 "未知", 1 "男", 2 "女"
            queryMap.put("Sex", faceRecognitionInfos[i].stuFaceInfoObject.emSex);
            fileModel.setValueAt(getKeyWithValue(searchSexDic, faceRecognitionInfos[i].stuFaceInfoObject.emSex), i, 1);
            // 抓拍图 年龄
            queryMap.put("Age", faceRecognitionInfos[i].stuFaceInfoObject.nAge);
            fileModel.setValueAt(String.valueOf(faceRecognitionInfos[i].stuFaceInfoObject.nAge), i, 2);
            // 抓拍图 表情 参考 NetSDKLib.EM_EMOTION_TYPE
            queryMap.put("Emotion", faceRecognitionInfos[i].stuFaceInfoObject.emEmotion);
            fileModel.setValueAt(getKeyWithValue(searchEmotionDic, faceRecognitionInfos[i].stuFaceInfoObject.emEmotion), i, 3);
            // 抓拍图 眼睛 参考 NetSDKLib.EM_EYE_STATE_TYPE
            queryMap.put("Eye", faceRecognitionInfos[i].stuFaceInfoObject.emEye);
            // 抓拍图 嘴巴 参考 NetSDKLib.EM_MOUTH_STATE_TYPE
            queryMap.put("Mouth", faceRecognitionInfos[i].stuFaceInfoObject.emMouth);
            // 抓拍图 口罩 参考 NetSDKLib.EM_MASK_STATE_TYPE
            queryMap.put("Mask", faceRecognitionInfos[i].stuFaceInfoObject.emMask);
            fileModel.setValueAt(getKeyWithValue(searchMaskDic, faceRecognitionInfos[i].stuFaceInfoObject.emMask), i, 4);
            // 抓拍图 胡子 参考 NetSDKLib.EM_BEARD_STATE_TYPE
            queryMap.put("Beard", faceRecognitionInfos[i].stuFaceInfoObject.emBeard);
            fileModel.setValueAt(getKeyWithValue(searchBeardDic, faceRecognitionInfos[i].stuFaceInfoObject.emBeard), i, 5);
            // 抓拍图 魅力(0-100) 越高越有魅力
            queryMap.put("Attractive", faceRecognitionInfos[i].stuFaceInfoObject.nAttractive);
            fileModel.setValueAt(String.valueOf(faceRecognitionInfos[i].stuFaceInfoObject.nAttractive), i, 6);
            // 抓拍图 眼镜 参考 0-未知 1-不戴 2-戴 其他值默认 0
            queryMap.put("Glasses", faceRecognitionInfos[i].stuFaceInfoObject.emGlasses);
            fileModel.setValueAt(getKeyWithValue(searchGlassesDic, faceRecognitionInfos[i].stuFaceInfoObject.emGlasses), i, 7);
            // <<<<<-----抓拍图的人脸数据 温度数据----->>>>>

            // 抓拍图 温度信息
            queryMap.put("MaxTemp", faceRecognitionInfos[i].stuFaceInfoObject.fMaxTemp);
            fileModel.setValueAt(String.valueOf(faceRecognitionInfos[i].stuFaceInfoObject.fMaxTemp), i, 8);
            // 抓拍图 是否超温
            queryMap.put("IsOverTemp", faceRecognitionInfos[i].stuFaceInfoObject.nIsOverTemp);
            // 抓拍图 是否低温
            queryMap.put("IsUnderTemp", faceRecognitionInfos[i].stuFaceInfoObject.nIsUnderTemp);
            // 温度单位 参考 NetSDKLib.EM_TEMPERATURE_UNIT
            queryMap.put("TempUnit", faceRecognitionInfos[i].stuFaceInfoObject.emTempUnit);

            // <<<<<----- 候选人的数据 ----->>>>>
            queryMap.put("nCandidateNum", faceRecognitionInfos[i].nCandidateExNum); // 匹配到的候选人数量

            ArrayList<LinkedHashMap<String, Object>> candidates = new ArrayList<>();
            // 保存候选人信息
            // 除了以下列出的条目，其他的信息设备都没有解析，如果想要知道，可以根据获取的 szUID，用 findFaceRecognitionDB() 来查询人员信息。
            // 具体请参见 src/com/netsdk/demo/example/FaceRecognition 中的 findFaceRecognitionDB() 方法
            // 需要指出 findFaceRecognitionDB() 示例是根据 GroupId 来查询的，使用的时候，GroupId不填，根据 szUID 来查询
            double currentSimilarity = 0;  // 存储一下相似值，找到最匹配的
            int maxSimilarlyNo = 0;
            for (int j = 0; j < faceRecognitionInfos[i].nCandidateExNum; j++) {
                LinkedHashMap<String, Object> candidate = new LinkedHashMap<>();

                // 候选人 匹配的相似度
                double thisSimilarity = faceRecognitionInfos[i].stuCandidatesEx[j].bySimilarity;
                candidate.put("candidateSimilarity", thisSimilarity);
                if (thisSimilarity > currentSimilarity) {
                    currentSimilarity = thisSimilarity;
                    maxSimilarlyNo = j;
                }
                // 候选人 姓名
                try {
                    candidate.put("candidateName", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szPersonName, encode).trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                // 候选人 UID
                candidate.put("candidateUID", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szUID).trim());
                // 候选人 所在注册库
                candidate.put("candidateGroup", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szGroupName));
                // 候选人 ID
                candidate.put("candidateID", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szID).trim());
                // 候选人 备注
                candidate.put("candidateComment", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szComment).trim());
                // 候选人 图片地址 可能有多张
                ArrayList<String> candidatePicPath = new ArrayList<>();
                for (int k = 0; k < faceRecognitionInfos[i].stuCandidatesPic[j].nFileCount; k++) {
                    candidatePicPath.add(new String(faceRecognitionInfos[i].stuCandidatesPic[j].stFiles[k].szFilePath).trim());
                }
                candidate.put("candidatePicPath", candidatePicPath);

                candidates.add(candidate);
            }

            queryMap.put("maxSimilarlyNo", maxSimilarlyNo);

            try {
                fileModel.setValueAt(new String(faceRecognitionInfos[i].stuCandidatesEx[maxSimilarlyNo].stPersonInfo.szPersonName, encode).trim(), i, 9);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            queryMap.put("Candidate", candidates);

            queryList.add(queryMap);
        }
    }

    // 下发停止搜索
    public void endTheSearch() {
        boolean ret = netsdk.CLIENT_FindCloseEx(lFindHandle);

        if (!ret) {
            System.err.println("FindCloseEx failed!" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("结束检索句柄：" + lFindHandle + " 指令下发成功");
    }

    // 下载图片并展示信息
    public void downloadPicAndShowDetail() {
        int number = Integer.valueOf(String.valueOf(this.historyVaultMainFrame.groupInfoModel.getValueAt(selectedNo, 0))) - 1;
        LinkedHashMap<String, Object> selectedData = queryList.get(number);
        String snapPicRemotePath = (String) selectedData.get("FacePicPath");
        String snapPicName = "channel"
                + String.valueOf(selectedData.get("Channel")) + "_"
                + String.valueOf(selectedData.get("EventTime"))
                .replace('/', '_')
                .replace(' ', '_')
                .replace(':', '_') + "_"
                + "snap.jpg";
        boolean snapFlag = DownloadRemotePic(snapPicRemotePath, snapPicName);

        int maxSimilarlyNo = (int) selectedData.get("maxSimilarlyNo");
        LinkedHashMap<String, Object> maxSimilarlyCandidate = ((ArrayList<LinkedHashMap<String, Object>>) selectedData.get("Candidate")).get(maxSimilarlyNo);
        String candidateRemotePicPath = ((ArrayList<String>) maxSimilarlyCandidate.get("candidatePicPath")).get(0);
        String maxSimilarlyPicName = "channel"
                + String.valueOf(selectedData.get("Channel")) + "_"
                + String.valueOf(selectedData.get("EventTime"))
                .replace('/', '_')
                .replace(' ', '_')
                .replace(':', '_') + "_"
                + "candidate.jpg";
        boolean candidateFlag = DownloadRemotePic(candidateRemotePicPath, maxSimilarlyPicName);

        if (snapFlag) {
            localCapturePicture(picSaveFolderPath + snapPicName,
                    this.historyVaultMainFrame.snapPicDisplayPanel);
        } else {
            clearCaptureOnPanel(this.historyVaultMainFrame.snapPicDisplayPanel);
            System.out.println("抓拍图下载失败，无法显示");
        }

        if (candidateFlag) {
            localCapturePicture(picSaveFolderPath + maxSimilarlyPicName,
                    this.historyVaultMainFrame.candidatePicDisplayPanel);
        } else {
            clearCaptureOnPanel(this.historyVaultMainFrame.candidatePicDisplayPanel);
            System.out.println("抓拍图下载失败，无法显示");
        }
        // 显示数据
        showSelectedInfoDetail(selectedData);
    }

    // 测试用
    public void testLocalDataDisplay() {

        LinkedHashMap<String, Object> queryMap = new LinkedHashMap<>();
        queryMap.put("FindCount", 1);
        queryMap.put("SearchCount", 1);
        queryMap.put("Channel", 2);
        NetSDKLib.NET_TIME eventTime = new NetSDKLib.NET_TIME() {{
            setTime(2020, 5, 15, 21, 0, 0);
        }};
        queryMap.put("EventTime", eventTime.toStringTime());
        queryMap.put("FacePicLength", 0);
        queryMap.put("FacePicPath", "test snap pic path");

        int[] FacePicCenter = new int[2];
        FacePicCenter[0] = 2000;
        FacePicCenter[1] = 3000;
        queryMap.put("FacePicCenter", FacePicCenter);

        queryMap.put("Sex", 1);
        queryMap.put("Age", 29);
        queryMap.put("Emotion", 7);
        queryMap.put("Mask", 2);
        queryMap.put("Beard", 2);
        queryMap.put("Attractive", 80);
        queryMap.put("Glasses", 0);

        queryMap.put("MaxTemp", 37.001);
        queryMap.put("IsOverTemp", 0);
        queryMap.put("IsUnderTemp", 0);
        queryMap.put("TempUnit", 0);

        queryMap.put("nCandidateNum", 1);
        ArrayList<LinkedHashMap<String, Object>> candidates = new ArrayList<>();
        double currentSimilarity = 0;  // 存储一下相似值，找到最匹配的
        int maxSimilarlyNo = 0;
        for (int j = 0; j < 1; j++) {
            LinkedHashMap<String, Object> candidate = new LinkedHashMap<>();

            // 候选人 匹配的相似度
            double thisSimilarity = 99;
            candidate.put("candidateSimilarity", thisSimilarity);
            if (thisSimilarity > currentSimilarity) {
                currentSimilarity = thisSimilarity;
                maxSimilarlyNo = j;
            }
            // 候选人 姓名
            candidate.put("candidateName", "好人");
            // 候选人 UID
            candidate.put("candidateUID", "20200515");
            // 候选人 所在注册库
            candidate.put("candidateGroup", "test group");
            // 候选人 ID
            candidate.put("candidateID", "330724xxxxxxxx1213");
            // 候选人 备注
            candidate.put("candidateComment", "test comment");
            // 候选人 图片地址 可能有多张
            ArrayList<String> candidatePicPath = new ArrayList<>();
            for (int k = 0; k < 1; k++) {
                candidatePicPath.add("candidate test pic path");
            }
            candidate.put("candidatePicPath", candidatePicPath);

            candidates.add(candidate);
        }

        queryMap.put("maxSimilarlyNo", maxSimilarlyNo);
        queryMap.put("Candidate", candidates);

        showSelectedInfoDetail(queryMap);

        localCapturePicture(picSaveFolderPath + "channel2_2020_05_11_15_38_03_snap.jpg",
                this.historyVaultMainFrame.snapPicDisplayPanel);
        localCapturePicture(picSaveFolderPath + "channel2_2020_05_11_15_38_03_snap.jpg",
                this.historyVaultMainFrame.candidatePicDisplayPanel);

        System.out.println(queryMap);
    }

    // 展示选定条目的数据
    public void showSelectedInfoDetail(LinkedHashMap<String, Object> queryMap) {

        DefaultTableModel detailModel = this.historyVaultMainFrame.detailInfoModel;
        clearTableModel(detailModel);

        detailModel.setValueAt("发生时间", 0, 0);
        detailModel.setValueAt(queryMap.get("EventTime"), 0, 1);

        detailModel.setValueAt("性别", 1, 0);
        detailModel.setValueAt(getKeyWithValue(searchSexDic, (int) queryMap.get("Sex")), 1, 1);

        detailModel.setValueAt("年龄", 2, 0);
        detailModel.setValueAt(String.valueOf(queryMap.get("Age")), 2, 1);

        detailModel.setValueAt("表情", 3, 0);
        detailModel.setValueAt(getKeyWithValue(searchEmotionDic, (int) queryMap.get("Emotion")), 3, 1);

        detailModel.setValueAt("温度", 4, 0);
        detailModel.setValueAt(String.valueOf(queryMap.get("MaxTemp")), 4, 1);

        detailModel.setValueAt("温度单位", 5, 0);
        detailModel.setValueAt(getKeyWithValue(searchTempUnitDic, (int) queryMap.get("TempUnit")), 5, 1);

        detailModel.setValueAt("是否超温", 6, 0);
        detailModel.setValueAt(queryMap.get("IsOverTemp").equals(0) ? "否" : "是", 6, 1);

        detailModel.setValueAt("是否低温", 7, 0);
        detailModel.setValueAt(queryMap.get("IsUnderTemp").equals(0) ? "否" : "是", 7, 1);

        Object thisCandidates = queryMap.get("Candidate");
        int thisMaxSimilarlyNo = (int) queryMap.get("maxSimilarlyNo");
        LinkedHashMap<String, Object> similarlyCandidate = (LinkedHashMap<String, Object>) ((ArrayList<Object>) thisCandidates).get(thisMaxSimilarlyNo);

        detailModel.setValueAt("候选人", 8, 0);
        detailModel.setValueAt(similarlyCandidate.get("candidateName"), 8, 1);

        detailModel.setValueAt("候选人ID", 9, 0);
        detailModel.setValueAt(similarlyCandidate.get("candidateID"), 9, 1);

        detailModel.setValueAt("候选人相似度", 10, 0);
        detailModel.setValueAt(similarlyCandidate.get("candidateSimilarity"), 10, 1);
    }

    // 本地抓图
    public static void localCapturePicture(String capturePath, HistoryVaultMainFrame.DisplayPanel showPanel) {
        // 从本地读取出来
        BufferedImage buffImg = null;
        try {
            buffImg = ImageIO.read(new File(capturePath));
            if (buffImg == null) return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 显示读取的图片
        showCaptureOnPanel(showPanel, buffImg);
    }

    // 展示图片
    public static void showCaptureOnPanel(HistoryVaultMainFrame.DisplayPanel displayPanel, BufferedImage bufferImg) {
        if (displayPanel != null) {
            displayPanel.setOpaque(false);
            displayPanel.setImage(bufferImg);
            displayPanel.repaint();
        } else {
            System.out.println("No display panel.");
        }
    }

    // 清除图片
    public static void clearCaptureOnPanel(HistoryVaultMainFrame.DisplayPanel displayPanel) {
        if (displayPanel != null) {
            displayPanel.setOpaque(false);
            displayPanel.setImage(null);
            displayPanel.repaint();
        } else {
            System.out.println("No display panel.");
        }
    }

    // 从远程下载图片
    public static Boolean DownloadRemotePic(String remotePath, String saveName) {

        File path = new File(picSaveFolderPath);
        if (!path.exists()) path.mkdir();

        NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE pInParam = new NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE();
        pInParam.pszFileName = new NativeString(remotePath).getPointer();

        pInParam.pszFileDst = new NativeString(path + "/" + saveName).getPointer();
        NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE pOutParam = new NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE();
        if (!netsdk.CLIENT_DownloadRemoteFile(m_hLoginHandle, pInParam, pOutParam, 3000)) {
            System.err.printf("CLIENT_DownloadRemoteFile failed, ErrCode=%s\n", ToolKits.getErrorCode());
            return false;
        } else {
            System.out.println("CLIENT_DownloadRemoteFile success");
            return true;
        }
    }

    // 清空 DefaultTableModel
    public static void clearTableModel(DefaultTableModel jTableModel) {
        int rowCount = jTableModel.getRowCount();
        int columnCount = jTableModel.getColumnCount();
        //清空DefaultTableModel中的内容
        for (int i = 0; i < rowCount; i++)//表格中的行数
        {
            for (int j = 0; j < columnCount; j++) {//表格中的列数
                jTableModel.setValueAt(" ", i, j);//逐个清空
            }
        }
    }

    // 启用 Container 内所有组件
    public static void EnableAllInnerComponent(Component container) {
        for (Component component : getComponents(container)) {
            component.setEnabled(true);
        }
    }

    // 禁用 Container 内所有组件
    public static void DisableAllInnerComponent(Component container) {
        for (Component component : getComponents(container)) {
            component.setEnabled(false);
        }
    }

    // 获取 Swing Container 内所有的非 Container 组件
    public static Component[] getComponents(Component container) {
        ArrayList<Component> list = null;

        try {
            list = new ArrayList<>(Arrays.asList(
                    ((Container) container).getComponents()));
            for (int index = 0; index < list.size(); index++) {
                list.addAll(Arrays.asList(getComponents(list.get(index))));
            }
        } catch (ClassCastException e) {
            list = new ArrayList<>();
        }

        return list.toArray(new Component[0]);
    }

    // 通过value获取字典key, value不重复
    public String getKeyWithValue(LinkedHashMap<String, Integer> map, Integer k) {
        for (String str : map.keySet()) {
            if (k.equals(map.get(str))) {
                return str;
            }
        }
        return "";
    }

    // 启动界面
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                HistoryVaultWithTempFrame demo = new HistoryVaultWithTempFrame();
                demo.historyVaultMainFrame.setVisible(true);
            }
        });
    }
}
