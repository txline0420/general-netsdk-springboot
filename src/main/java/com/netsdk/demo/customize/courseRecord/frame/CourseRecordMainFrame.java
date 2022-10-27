package com.netsdk.demo.customize.courseRecord.frame;

import com.netsdk.demo.customize.courseRecord.CourseRecordCourse;
import com.netsdk.demo.customize.courseRecord.CourseRecordLogon;
import com.netsdk.demo.customize.courseRecord.CourseRecordRealPlay;
import com.netsdk.demo.util.DateChooserJButton;
import com.netsdk.lib.NetSDKLib;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import static com.netsdk.demo.customize.courseRecord.CourseRecordChannel.GetResourceChannel;
import static com.netsdk.demo.util.UiUtil.setBorderEx;

public class CourseRecordMainFrame extends JFrame {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    /////////////////////// Course Management ///////////////////////////
    /////////////////////////////////////////////////////////////////////
    // 通道预览
    public RealPlayPanel realPanel;
    public JPanel realPlayPanel;
    public Panel realPlayWindow = new Panel();
    public JComboBox comboBox;
    /// 搜索日期
    public DateChooserJButton dateChooserStartJButton;
    public DateChooserJButton dateChooserEndJButton;
    /// 管理按钮
    public JButton btnRealPlay;
    public JButton startQueryButton;
    public JButton addCourseButton;
    public JButton modifyCourseButton;
    public JButton deleteCourseButton;
    public JTable courseTable;
    public JButton queryPreButton;
    public JButton queryNextButton;
    /// 父窗口
    private CourseRecordLogonFrame courseRecordLogonFrame;
    /// 子窗口
    public CourseDetailDialog courseDetailDialog;
    /// 选中行
    public ArrayList<Integer> selectedNums = new ArrayList<Integer>();

    ///////////////////// resource & methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////

    public CourseRecordCourse courseRecordManage = new CourseRecordCourse(this);   // 课程相关方法
    public CourseRecordLogon courseRecordLogon;                                               // 登录相关方法
    public CourseRecordRealPlay courseRecordRealPlay = new CourseRecordRealPlay(this, realPlayWindow); // 拉流相关方法

    /**
     * Create the application with specified Logon Frame.
     */
    public CourseRecordMainFrame(CourseRecordLogonFrame logonFrame) {
        this.courseRecordLogonFrame = logonFrame;
        this.courseRecordLogon = logonFrame.courseRecordLogon;
        initialize();
    }

    public CourseRecordMainFrame() {
        initialize();
    }

    //////////////////////////////////// GUI 相关 ///////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    // 带背景的面板组件
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
            realPlayPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            realPlayWindow.setBackground(Color.GRAY);
            realPlayWindow.setSize(480, 480);
            realPlayPanel.add(realPlayWindow, BorderLayout.CENTER);
        }
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        this.setResizable(false);
        this.setTitle("录播主机Demo");
        this.getContentPane().setBackground(Color.LIGHT_GRAY);
        this.getContentPane().setLayout(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JTabbedPane mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        mainTabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        mainTabbedPane.setBounds(10, 10, 1000, 680);
        this.getContentPane().add(mainTabbedPane);

        JPanel guideBroadcastPanel = new JPanel();
        mainTabbedPane.addTab("  直   播   ", null, guideBroadcastPanel, null);
        guideBroadcastPanel.setLayout(null);

        JPanel realPlayPanel = new JPanel();
        realPanel = new RealPlayPanel();
        realPlayPanel.setLayout(new BorderLayout(0, 0));
        realPlayPanel.add(realPanel, BorderLayout.CENTER);

        realPlayPanel.setBounds(44, 37, 746, 558);
        guideBroadcastPanel.add(realPlayPanel);

        JLabel lblNewLabel = new JLabel("逻辑通道");
        lblNewLabel.setBounds(816, 37, 72, 18);
        guideBroadcastPanel.add(lblNewLabel);

        comboBox = new JComboBox();
        comboBox.setModel(new DefaultComboBoxModel(new String[]{
                "组合通道",
                "PPT显示",
                "板书特写",
                "学生特写",
                "学生全景",
                "教师特写",
                "教师全景",
                "教师检测",
                "板书检测",
                "板书特写1",
                "板书检测1",
                "展台显示",
                "视频监控",
                "互动会议",
                "互动演示"}));
        comboBox.setSelectedIndex(0);
        comboBox.setBounds(816, 68, 165, 24);
        guideBroadcastPanel.add(comboBox);

        btnRealPlay = new JButton("预览");
        btnRealPlay.setBounds(816, 111, 113, 27);
        guideBroadcastPanel.add(btnRealPlay);

        JPanel courseManagePanel = new JPanel();
        courseManagePanel.setBackground(Color.WHITE);
        mainTabbedPane.addTab("  课 程 表   ", null, courseManagePanel, null);
        courseManagePanel.setLayout(null);

        JPanel courseQueryPanel = new JPanel();
        courseQueryPanel.setBackground(Color.WHITE);
        courseQueryPanel.setBounds(10, 20, 558, 24);
        courseManagePanel.add(courseQueryPanel);
        courseQueryPanel.setLayout(new GridLayout(0, 4, 0, 0));

        JLabel queryStartLabel = new JLabel("开始时间: ");
        queryStartLabel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        courseQueryPanel.add(queryStartLabel);

        dateChooserStartJButton = new DateChooserJButton();
        courseQueryPanel.add(dateChooserStartJButton);

        JLabel label = new JLabel("结束时间: ");
        label.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        courseQueryPanel.add(label);

        dateChooserEndJButton = new DateChooserJButton();
        courseQueryPanel.add(dateChooserEndJButton);

        JPanel CouseMangePanel = new JPanel();
        CouseMangePanel.setBackground(Color.WHITE);
        CouseMangePanel.setBounds(150, 54, 418, 23);
        courseManagePanel.add(CouseMangePanel);
        CouseMangePanel.setLayout(new GridLayout(0, 5, 0, 0));

        addCourseButton = new JButton("添加");
        CouseMangePanel.add(addCourseButton);

        Component horizontalGlue01 = Box.createHorizontalGlue();
        CouseMangePanel.add(horizontalGlue01);

        modifyCourseButton = new JButton("修改");
        CouseMangePanel.add(modifyCourseButton);

        Component horizontalGlue02 = Box.createHorizontalGlue();
        CouseMangePanel.add(horizontalGlue02);

        deleteCourseButton = new JButton("删除");
        CouseMangePanel.add(deleteCourseButton);

        JPanel courseTablePanel = new JPanel();
        courseTablePanel.setBackground(Color.WHITE);
        courseTablePanel.setBounds(52, 87, 879, 537);
        courseManagePanel.add(courseTablePanel);
        courseTablePanel.setLayout(new GridLayout(0, 1, 0, 0));

        final String[] groupName = {
                "序号",
                "日期",
                "课程名称",
                "教师名称",
                "时间段",
                "录制模式",
                "视频控制",
                "状态"
        };
        Object[][] statisticData = new Object[32][8];
        DefaultTableModel groupInfoModel = new DefaultTableModel(statisticData, groupName);

        courseTable = new JTable(groupInfoModel) {
            @Override    // 不可编辑
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        courseTable.getColumnModel().getColumn(0).setPreferredWidth(10);
        courseTable.getColumnModel().getColumn(1).setPreferredWidth(10);
        courseTable.getColumnModel().getColumn(2).setPreferredWidth(10);
        courseTable.getColumnModel().getColumn(3).setPreferredWidth(10);
        courseTable.getColumnModel().getColumn(4).setPreferredWidth(10);
        courseTable.getColumnModel().getColumn(5).setPreferredWidth(10);
        courseTable.getColumnModel().getColumn(6).setPreferredWidth(10);
        courseTable.getColumnModel().getColumn(7).setPreferredWidth(10);

        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // 只能选中一行

        // 列表显示居中
        DefaultTableCellRenderer dCellRenderer = new DefaultTableCellRenderer();
        dCellRenderer.setHorizontalAlignment(JLabel.CENTER);
        courseTable.setDefaultRenderer(Object.class, dCellRenderer);
        JScrollPane scrollPane = new JScrollPane(courseTable);
        scrollPane.setBackground(Color.WHITE);

        courseTablePanel.add(scrollPane);

        JPanel queryPanel = new JPanel();
        queryPanel.setBackground(Color.WHITE);
        queryPanel.setBounds(596, 20, 333, 24);
        courseManagePanel.add(queryPanel);
        queryPanel.setLayout(null);
        startQueryButton = new JButton("查询");
        startQueryButton.setBounds(0, 0, 94, 24);
        queryPanel.add(startQueryButton);

        queryPreButton = new JButton("上一页");
        queryPreButton.setEnabled(false);
        queryPreButton.setBounds(123, 1, 93, 23);
        queryPanel.add(queryPreButton);

        queryNextButton = new JButton("下一页");
        queryNextButton.setEnabled(false);
        queryNextButton.setBounds(240, 1, 93, 23);
        queryPanel.add(queryNextButton);

        mainTabbedPane.setSelectedIndex(0);
        LiveSteamManageAddActions();
        CourseManageAddActions();

        this.setBackground(Color.WHITE);
        this.setBounds(100, 100, 1028, 728);
    }

    Point getAppropriateLocation(Frame owner, Point position, Window awtWindow) {
        Point result = new Point(position);
        Point p = owner.getLocation();
        int offsetX = (position.x + awtWindow.getWidth()) - (p.x + owner.getWidth());
        int offsetY = (position.y + awtWindow.getHeight()) - (p.y + owner.getHeight());

        if (offsetX > 0) {
            result.x -= offsetX;
        }

        if (offsetY > 0) {
            result.y -= offsetY;
        }

        return result;
    }

    //////////////////////////////////// 按钮方法注册 ///////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    private void LiveSteamManageAddActions() {
        btnRealPlay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (courseRecordRealPlay.m_hLiveSteam.longValue() == 0) {
                    // 选择特定逻辑通道
                    int resourceOrder = comboBox.getSelectedIndex();
                    // 获取真实通道号
                    int channel = GetResourceChannel(courseRecordLogon.m_hLoginHandle, resourceOrder);
                    if (channel == -1) {
                        System.out.println("资源通道无效, 没有绑定真实通道");
                        JOptionPane.showMessageDialog(null, "资源通道无效, 没有绑定真实通道", "警告信息", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    // 开启预览
                    courseRecordRealPlay.play(channel);
                    realPlayPanel.setOpaque(false);
                    realPlayPanel.repaint();
                    System.out.println("资源通道预览成功, 真实通道号:" + channel);
                    // GUI状态修正
                    comboBox.setEnabled(false);
                    btnRealPlay.setText("停止预览");
                } else {
                    // 关闭预览
                    courseRecordRealPlay.stopPlay();
                    // GUI状态修正
                    comboBox.setEnabled(true);
                    btnRealPlay.setText("开始预览");
                }
            }
        });
    }

    private void CourseManageAddActions() {
        // 添加课程
        addCourseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Frame owner = (Frame) SwingUtilities.getWindowAncestor(addCourseButton);
                if (courseDetailDialog == null) {
                    courseDetailDialog = new CourseDetailDialog((CourseRecordMainFrame) owner);
                }
                // 定位子窗体的开启坐标并打开
                Point p = addCourseButton.getLocationOnScreen();
                p.y = p.y + 30;
                courseDetailDialog.setLocation(getAppropriateLocation(owner, p, courseDetailDialog));
                courseDetailDialog.setVisible(true);
                // 这里会阻塞子窗体，直到添加/修改的窗口被关闭
                if (courseDetailDialog.courseInfo != null) {
                    // 新建课程
                    courseRecordManage.addNewCourse(courseDetailDialog.courseInfo);
                    courseDetailDialog.courseInfo = null;
                }
            }
        });
        // 修改课程
        modifyCourseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Frame owner = (Frame) SwingUtilities.getWindowAncestor(modifyCourseButton);
                // 定位子窗体的开启坐标并打开
                Point p = modifyCourseButton.getLocationOnScreen();
                p.y = p.y + 30;

                // 只选中一行，且不是空行
                if (selectedNums.size() == 0) {
                    JOptionPane.showMessageDialog(null, "请先至少选中一行数据", "提示信息", JOptionPane.INFORMATION_MESSAGE);
                } else if (selectedNums.size() == 1) {
                    int selectNo = selectedNums.get(0);
                    if (courseDetailDialog == null) {
                        courseDetailDialog = new CourseDetailDialog((CourseRecordMainFrame) owner);
                    }
                    // 先读取原先的课程配置
                    courseDetailDialog.fillCourseInfo(courseRecordManage.displayQueryList.get(selectNo));
                    // 定位子窗体的开启坐标并打开
                    courseDetailDialog.setLocation(getAppropriateLocation(owner, p, courseDetailDialog));
                    courseDetailDialog.setVisible(true);
                    // 这里会阻塞子窗体，直到添加/修改的窗口被关闭
                    if (courseDetailDialog.courseInfo != null) {
                        // 修改课程
                        courseRecordManage.modifyCourse(courseDetailDialog.courseInfo, selectNo);
                        courseDetailDialog.courseInfo = null;
                    }
                }
            }
        });

        deleteCourseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 只选中一行，且不是空行
                if (selectedNums.size() == 0) {
                    JOptionPane.showMessageDialog(null, "请先至少选中一行数据", "提示信息", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                // 删除课程
                courseRecordManage.deleteCourse(selectedNums);
            }
        });
        // 查询/结束查询课程
        startQueryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!courseRecordManage.isQuery) {
                    courseRecordManage.queryCourseStart();  // 查询开始
                } else {
                    courseRecordManage.queryCourseEnd();    // 查询结束
                }
            }
        });
        // 查询上一页
        queryPreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                courseRecordManage.queryCoursePre();
            }
        });
        // 查询下一页
        queryNextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                courseRecordManage.queryCourseNext();
            }
        });

        // Table 选中事件
        courseTable.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        int[] rowSelected = courseTable.getSelectedRows();
                        selectedNums.clear();
                        for (int rowNum : rowSelected) {
                            if (courseTable.getValueAt(rowNum, 0) == null ||
                                    String.valueOf(courseTable.getValueAt(rowNum, 0)).trim().equals("")
                            ) {
                                System.out.println("请不要选中空行");
                                JOptionPane.showMessageDialog(null, "请不要选中空行", "错误信息", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            selectedNums.add(rowNum);
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

        // 注册窗体清出事件
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (courseDetailDialog != null) courseDetailDialog.dispose();
                dispose();
                courseRecordLogonFrame.setVisible(true);
            }
        });
    }
}
