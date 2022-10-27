package com.netsdk.demo.customize.courseRecord.frame;

import com.netsdk.demo.customize.courseRecord.pojo.CourseInfo;
import com.netsdk.demo.util.DateChooserJButton;
import com.netsdk.lib.structure.NET_TIME;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CourseDetailDialog extends JDialog {

    private final JPanel contentPanel = new JPanel();

    public JTextField szNameField;         // 课程名称
    public JTextField szTeacherField;      // 教师名称
    public JTextArea szInfoTextArea;       // 课程介绍
    public JComboBox cbxRecord;            // 录制播放模式
    public JComboBox cbxCompose;           // 合成通道模式
    public JPanel dateStartPanel;
    public JPanel dateEndPanel;
    public DateChooserJButton dateChooserStartJButton;
    public DateChooserJButton dateChooserEndJButton;
    public JRadioButton resourceRadioButton;

    private JButton okButton;
    private JButton cancelButton;

    public CourseInfo courseInfo;  // 保存课程信息

    private CourseRecordMainFrame mainFrame;   // 父窗口

    /**
     * Create the dialog.
     */
    public CourseDetailDialog(CourseRecordMainFrame frame) {
        this.mainFrame = frame;
        setTitle("课程详情");
        setBounds(100, 100, 569, 442);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(null);
        {
            JLabel lblSzName = new JLabel("课程名称: ");
            lblSzName.setBounds(31, 23, 81, 15);
            contentPanel.add(lblSzName);
        }
        {
            JLabel lblSzTeacher = new JLabel("教师姓名:");
            lblSzTeacher.setBounds(31, 58, 81, 15);
            contentPanel.add(lblSzTeacher);
        }
        {
            JLabel lblSzInfo = new JLabel("简    介:");
            lblSzInfo.setBounds(31, 95, 81, 15);
            contentPanel.add(lblSzInfo);
        }
        {
            szNameField = new JTextField();
            szNameField.setBounds(116, 20, 180, 21);
            contentPanel.add(szNameField);
            szNameField.setColumns(10);
        }
        {
            szTeacherField = new JTextField();
            szTeacherField.setColumns(10);
            szTeacherField.setBounds(116, 55, 180, 21);
            contentPanel.add(szTeacherField);
        }

        szInfoTextArea = new JTextArea();
        szInfoTextArea.setBounds(116, 91, 412, 108);
        contentPanel.add(szInfoTextArea);

        JLabel lblStartTime = new JLabel("开始时间:");
        lblStartTime.setBounds(31, 223, 81, 15);
        contentPanel.add(lblStartTime);

        JLabel lblEndTime = new JLabel("结束时间:");
        lblEndTime.setBounds(31, 260, 81, 15);
        contentPanel.add(lblEndTime);

        JLabel lblEmCompose = new JLabel("视频控制:");
        lblEmCompose.setBounds(31, 297, 81, 15);
        contentPanel.add(lblEmCompose);

        cbxCompose = new JComboBox();
        cbxCompose.setModel(new DefaultComboBoxModel(new String[]{"直播", "录制", "录播"}));
        cbxCompose.setBounds(115, 294, 181, 21);
        contentPanel.add(cbxCompose);

        JLabel lblEmRecord = new JLabel("录制模式:");
        lblEmRecord.setBounds(31, 337, 81, 15);
        contentPanel.add(lblEmRecord);

        cbxRecord = new JComboBox();
        cbxRecord.setModel(new DefaultComboBoxModel(new String[]{"电影模式", "多画面模式"}));
        cbxRecord.setSelectedIndex(0);
        cbxRecord.setBounds(116, 334, 180, 21);
        contentPanel.add(cbxRecord);

        dateStartPanel = new JPanel();
        dateStartPanel.setBounds(116, 223, 180, 21);
        contentPanel.add(dateStartPanel);
        dateStartPanel.setLayout(new BorderLayout(0, 0));

        dateChooserStartJButton = new DateChooserJButton();
        dateStartPanel.add(dateChooserStartJButton, BorderLayout.CENTER);

        dateEndPanel = new JPanel();
        dateEndPanel.setBounds(116, 254, 180, 21);
        contentPanel.add(dateEndPanel);
        dateEndPanel.setLayout(new BorderLayout(0, 0));

        dateChooserEndJButton = new DateChooserJButton();
        dateEndPanel.add(dateChooserEndJButton, BorderLayout.CENTER);

        resourceRadioButton = new JRadioButton("操作资源通道");
        resourceRadioButton.setBounds(327, 291, 157, 27);
        contentPanel.add(resourceRadioButton);
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                okButton = new JButton("确认");
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        getCourseInfo();
                        Dialog owner = (Dialog) SwingUtilities.getWindowAncestor(okButton);
                        owner.setVisible(false);
                    }
                });
            }
            {
                cancelButton = new JButton("取消");
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
                cancelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        courseInfo = null;
                        Dialog owner = (Dialog) SwingUtilities.getWindowAncestor(cancelButton);
                        owner.setVisible(false);
                    }
                });
            }
        }
        this.setModalityType(ModalityType.APPLICATION_MODAL);
    }

    // 载入窗体时写入旧的数据
    public void fillCourseInfo(CourseInfo info) {
        courseInfo = info;
        szNameField.setText(courseInfo.courseName);         // 课程名称
        szTeacherField.setText(courseInfo.teacherName);     // 教师名称
        szInfoTextArea.setText(courseInfo.introduction);    // 课程介绍

        dateStartPanel.remove(dateChooserStartJButton);
        dateChooserStartJButton = new DateChooserJButton(info.stuStartTime.toStringTimeEx());
        dateStartPanel.add(dateChooserStartJButton, BorderLayout.CENTER);

        dateEndPanel.remove(dateChooserEndJButton);
        dateChooserEndJButton = new DateChooserJButton(info.stuEndTime.toStringTimeEx());
        dateEndPanel.add(dateChooserEndJButton, BorderLayout.CENTER);

        cbxRecord.setSelectedItem(courseInfo.strCompositeChannelMode);      // 播放模式
        cbxCompose.setSelectedItem(courseInfo.strVideoCtrlType);            // 合成模式
        resourceRadioButton.setSelected(courseInfo.containsResource);       // 是否是资源模式
    }

    // 读取当前页面上填写的数据
    private void getCourseInfo() {
        if (courseInfo == null) courseInfo = new CourseInfo();

        String[] startDateTimes = dateChooserStartJButton.getOriginalText().split("\\s+");
        String[] startDate = startDateTimes[0].split("-");
        String[] startTime = startDateTimes[1].split(":");
        courseInfo.stuStartTime = new NET_TIME(
                Integer.parseInt(startDate[0]),
                Integer.parseInt(startDate[1]),
                Integer.parseInt(startDate[2]),
                Integer.parseInt(startTime[0]),
                Integer.parseInt(startTime[1]),
                Integer.parseInt(startTime[2])
        );

        String[] endDateTimes = dateChooserEndJButton.getOriginalText().split("\\s+");
        String[] endDate = endDateTimes[0].split("-");
        String[] endTime = endDateTimes[1].split(":");
        courseInfo.stuEndTime = new NET_TIME(
                Integer.parseInt(endDate[0]),
                Integer.parseInt(endDate[1]),
                Integer.parseInt(endDate[2]),
                Integer.parseInt(endTime[0]),
                Integer.parseInt(endTime[1]),
                Integer.parseInt(endTime[2])
        );
        courseInfo.SetStrCourseDateTime();
        courseInfo.courseName = szNameField.getText().trim();
        courseInfo.teacherName = szTeacherField.getText().trim();
        courseInfo.introduction = szInfoTextArea.getText().trim();

        // 这里对应的是页面上两个下拉框的枚举，并不是接口的参数，所以需要转换下
        courseInfo.strCompositeChannelMode = (String) cbxRecord.getSelectedItem();
        courseInfo.strVideoCtrlType = (String) cbxCompose.getSelectedItem();
        // 这里暂时不区分具体挑选哪一个资源通道，所有的资源通道统一处理
        courseInfo.containsResource = resourceRadioButton.isSelected();
        courseInfo.ParseStrCompositeMode();
        courseInfo.ParseStrVideoCtrl();
    }
}
