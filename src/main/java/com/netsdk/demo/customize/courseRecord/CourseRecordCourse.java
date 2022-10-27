package com.netsdk.demo.customize.courseRecord;

import com.netsdk.demo.customize.courseRecord.frame.CourseRecordMainFrame;
import com.netsdk.demo.customize.courseRecord.modules.CourseManageModule;
import com.netsdk.demo.customize.courseRecord.pojo.CourseInfo;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_COURSE_STATE;
import com.netsdk.lib.structure.*;

import javax.swing.*;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.netsdk.demo.customize.courseRecord.modules.CourseManageModule.*;
import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/18 11:39
 */
public class CourseRecordCourse {

    private final CourseRecordMainFrame mainFrame;

    public boolean isQuery = false;        // 正在查询标志位
    private int FindID = 0;                // 查询句柄
    private int totalCount = 0;            // 查询到的总数
    private int startIdx = 0;              // 获取查询数据的起始序号
    private final int maxFetch = 32;       // 一次请求最大获取量

    public final List<CourseInfo> displayQueryList = new ArrayList<CourseInfo>();            // 用于界面展示的数据在这里暂存
    private final NET_OUT_QUERY_COURSE stuQueryOut = new NET_OUT_QUERY_COURSE();    // 每一页查询到的数据在这里暂存

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    public CourseRecordCourse(CourseRecordMainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /**
     * 添加课程
     */
    public void addNewCourse(CourseInfo courseInfo) {

        NET_IN_ADD_COURSE stuIn = new NET_IN_ADD_COURSE();
        stuIn.nCourseNum = 1;

        // 课程时间
        stuIn.stuCourseInfo[0].stuStartTime = courseInfo.stuStartTime;
        stuIn.stuCourseInfo[0].stuEndTime = courseInfo.stuEndTime;

        // 课程基本资料
        byte[] bCourseName = new byte[0];
        byte[] bTeacherName = new byte[0];
        byte[] bIntroduction = new byte[0];
        try {    // 不添加编码参数中文会乱码
            bCourseName = courseInfo.courseName.getBytes(encode);
            bTeacherName = courseInfo.teacherName.getBytes(encode);
            bIntroduction = courseInfo.introduction.getBytes(encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // 请不要直接赋值，要用 System.arraycopy 确保结构体内参数的长度不被破坏
        System.arraycopy(bCourseName, 0, stuIn.stuCourseInfo[0].szCourseName, 0, bCourseName.length);
        System.arraycopy(bTeacherName, 0, stuIn.stuCourseInfo[0].szTeacherName, 0, bTeacherName.length);
        System.arraycopy(bIntroduction, 0, stuIn.stuCourseInfo[0].szIntroduction, 0, bIntroduction.length);

        // 资源通道能否拉流
        stuIn.stuCourseInfo[0].nCanStartStreamNum = courseInfo.nCanStartStreamNum;  // 共15个有定义的逻辑通道，这个值一般是15
        if (courseInfo.nCanStartStreamNum >= 0)
            System.arraycopy(courseInfo.emCanStartStream, 0, stuIn.stuCourseInfo[0].emCanStartStream, 0, courseInfo.nCanStartStreamNum);

        // 资源通道是否需要录制
        stuIn.stuCourseInfo[0].nIsRecordNum = courseInfo.nIsRecordNum;
        if (courseInfo.nIsRecordNum >= 0)
            System.arraycopy(courseInfo.emIsRecord, 0, stuIn.stuCourseInfo[0].emIsRecord, 0, courseInfo.nIsRecordNum);

        // 合成通道模式
        stuIn.stuCourseInfo[0].nCompositeChannelMode = courseInfo.nCompositeChannelMode;

        // 录制状态，默认未录制
        stuIn.stuCourseInfo[0].emCourseState = EM_COURSE_STATE.EM_COURSE_STATE_NOT_RECORD.getValue();  // 默认未录制

        NET_OUT_ADD_COURSE stuOut = new NET_OUT_ADD_COURSE();

        boolean ret = AddCourse(mainFrame.courseRecordLogon.m_hLoginHandle, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("添加课程失败！");
            JOptionPane.showMessageDialog(null, "添加课程失败 错误码: " + ToolKits.getErrorCode(), "错误信息", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int courseID = stuOut.nId[0];

        // 虽然sdk不会报错，但ID小于0时同样指添加失败。-1:数据库中无此记录, -2:记录已存在, -3:数据库已满
        switch (courseID) {
            case -1:
                JOptionPane.showMessageDialog(null, "数据库中无此记录", "错误信息", JOptionPane.ERROR_MESSAGE);
                break;
            case -2:
                JOptionPane.showMessageDialog(null, "记录已存在", "错误信息", JOptionPane.ERROR_MESSAGE);
                break;
            case -3:
                JOptionPane.showMessageDialog(null, "数据库已满", "错误信息", JOptionPane.ERROR_MESSAGE);
                break;
            default:
                JOptionPane.showMessageDialog(null, "新课程添加成功 ID: " + courseID, "提示信息", JOptionPane.INFORMATION_MESSAGE);
                break;
        }
    }

    /**
     * 修改课程
     */
    public void modifyCourse(CourseInfo courseInfo, int selectNo) {
        // 先选定要修改的课程信息，主要是 Id，它指示设备具体修改哪个课程
        NET_COURSE_RESULT modifyResult = stuQueryOut.stuCourseResult[selectNo];
        NET_COURSE oldCourse = modifyResult.stuCourseInfo;
        int courseID = modifyResult.nId;

        NET_IN_MODIFY_COURSE stuIn = new NET_IN_MODIFY_COURSE();
        stuIn.nCourseNum = 1;
        stuIn.nId[0] = courseID;               // 指定 Id
        stuIn.stuCourseInfo[0] = oldCourse;    // 先存入原来的数据

        // 课程时间
        stuIn.stuCourseInfo[0].stuStartTime = courseInfo.stuStartTime;
        stuIn.stuCourseInfo[0].stuEndTime = courseInfo.stuEndTime;

        // 课程基本资料
        byte[] bCourseName = new byte[0];
        byte[] bTeacherName = new byte[0];
        byte[] bIntroduction = new byte[0];
        try {
            bCourseName = courseInfo.courseName.getBytes(encode);
            bTeacherName = courseInfo.teacherName.getBytes(encode);
            bIntroduction = courseInfo.introduction.getBytes(encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.arraycopy(bCourseName, 0, stuIn.stuCourseInfo[0].szCourseName, 0, bCourseName.length);
        System.arraycopy(bTeacherName, 0, stuIn.stuCourseInfo[0].szTeacherName, 0, bTeacherName.length);
        System.arraycopy(bIntroduction, 0, stuIn.stuCourseInfo[0].szIntroduction, 0, bIntroduction.length);

        // 资源通道能否拉流
        stuIn.stuCourseInfo[0].nCanStartStreamNum = courseInfo.nCanStartStreamNum;
        if (courseInfo.nCanStartStreamNum >= 0)
            System.arraycopy(courseInfo.emCanStartStream, 0, stuIn.stuCourseInfo[0].emCanStartStream, 0, courseInfo.nCanStartStreamNum);

        // 资源通道是否需要录制
        stuIn.stuCourseInfo[0].nIsRecordNum = courseInfo.nIsRecordNum;
        if (courseInfo.nIsRecordNum >= 0)
            System.arraycopy(courseInfo.emIsRecord, 0, stuIn.stuCourseInfo[0].emIsRecord, 0, courseInfo.nIsRecordNum);

        // 合成通道模式
        stuIn.stuCourseInfo[0].nCompositeChannelMode = courseInfo.nCompositeChannelMode;

        // 录制状态不要修改，这个是设备自己控制的。事实上，录制中/已录制 的课程设备也不允许修改

        NET_OUT_MODIFY_COURSE stuOut = new NET_OUT_MODIFY_COURSE();

        boolean ret = ModifyCourse(mainFrame.courseRecordLogon.m_hLoginHandle, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("修改课程失败！");
            JOptionPane.showMessageDialog(null, "修改课程失败 错误码: " + ToolKits.getErrorCode(), "错误信息", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int resultID = stuOut.nResultID[0];

        // 虽然sdk不会报错，但ID小于0时同样指修改失败。-1: 数据库中无此记录, -2: 时间冲突
        switch (resultID) {
            case -1:
                JOptionPane.showMessageDialog(null, "数据库中无此记录", "错误信息", JOptionPane.ERROR_MESSAGE);
                return;
            case -2:
                JOptionPane.showMessageDialog(null, "时间冲突", "错误信息", JOptionPane.ERROR_MESSAGE);
                return;
            default:
                JOptionPane.showMessageDialog(null, "课程修改成功 ID: " + resultID, "提示信息", JOptionPane.INFORMATION_MESSAGE);
                break;
        }

        ///////////////////////////////// 界面展示 ////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////

        CourseInfo info = displayQueryList.get(selectNo);

        // 序号不修改
        info.courseDate = courseInfo.courseDate;
        info.coursePeriod = courseInfo.coursePeriod;
        info.courseName = courseInfo.courseName;
        info.teacherName = courseInfo.teacherName;
        info.strCompositeChannelMode = courseInfo.strCompositeChannelMode;
        info.strVideoCtrlType = courseInfo.strVideoCtrlType;
        // 状态不修改

        mainFrame.courseTable.setValueAt(info.idx, selectNo, 0);
        mainFrame.courseTable.setValueAt(info.courseDate, selectNo, 1);
        mainFrame.courseTable.setValueAt(info.courseName, selectNo, 2);
        mainFrame.courseTable.setValueAt(info.teacherName, selectNo, 3);
        mainFrame.courseTable.setValueAt(info.coursePeriod, selectNo, 4);
        mainFrame.courseTable.setValueAt(info.strCompositeChannelMode, selectNo, 5);
        if (courseInfo.containsResource) {
            mainFrame.courseTable.setValueAt(info.strVideoCtrlType + "+资源", selectNo, 6);
        } else {
            mainFrame.courseTable.setValueAt(info.strVideoCtrlType, selectNo, 6);
        }
        mainFrame.courseTable.setValueAt(info.strCourseState, selectNo, 7);
    }

    /**
     * 删除课程
     */
    public void deleteCourse(List<Integer> selectNums) {
        NET_IN_DELETE_COURSE stuIn = new NET_IN_DELETE_COURSE();
        stuIn.nIdNum = selectNums.size();
        for (int i = 0; i < selectNums.size(); i++) {
            stuIn.nId[i] = stuQueryOut.stuCourseResult[selectNums.get(i)].nId;    // 拿到需要删除课程的Id
        }
        NET_OUT_DELETE_COURSE stuOut = new NET_OUT_DELETE_COURSE();
        boolean ret = DeleteCourse(mainFrame.courseRecordLogon.m_hLoginHandle, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("删除课程失败！");
            JOptionPane.showMessageDialog(null, "删除课程失败 错误码: " + ToolKits.getErrorCode(), "错误信息", JOptionPane.ERROR_MESSAGE);
        }
        // 和添加/修改不同，删除的逻辑是ID = -1:删除成功, 原id:删除失败
        for (int i = 0; i < selectNums.size(); i++) {
            int resultID = stuOut.nResultId[i];
            if (resultID == -1) {
                JOptionPane.showMessageDialog(null, "课程删除成功", "提示信息", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "删除课程失败", "错误信息", JOptionPane.ERROR_MESSAGE);
            }
        }

        ///////////////////////////////// 界面展示 ////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////

        for (int selectNo : selectNums) {
            // 删除的行，数据请 0
            displayQueryList.remove(selectNo);
            displayQueryList.add(selectNo, new CourseInfo());
            // 删除的行，数据请 0
            stuQueryOut.stuCourseResult[selectNo] = new NET_COURSE_RESULT();
            stuQueryOut.stuCourseResult[selectNo].nId = -1;
        }

        // 直接刷新 Table
        updateCourseInfoTable();
    }

    /**
     * 开始查询
     */
    public void queryCourseStart() {

        ///////////////////////////// QueryCourseOpen ////////////////////////////
        //////////////////////////////////////////////////////////////////////////

        String[] startDateTimes = mainFrame.dateChooserStartJButton.getOriginalText().split("\\s+");
        String[] startDate = startDateTimes[0].split("-");
        String[] startTime = startDateTimes[1].split(":");
        NET_TIME queryStartTime = new NET_TIME(                      // 起始时间
                Integer.parseInt(startDate[0]),
                Integer.parseInt(startDate[1]),
                Integer.parseInt(startDate[2]),
                Integer.parseInt(startTime[0]),
                Integer.parseInt(startTime[1]),
                Integer.parseInt(startTime[2])
        );

        String[] endDateTimes = mainFrame.dateChooserEndJButton.getOriginalText().split("\\s+");
        String[] endDate = endDateTimes[0].split("-");
        String[] endTime = endDateTimes[1].split(":");
        NET_TIME queryEndTime = new NET_TIME(                       // 结束时间
                Integer.parseInt(endDate[0]),
                Integer.parseInt(endDate[1]),
                Integer.parseInt(endDate[2]),
                Integer.parseInt(endTime[0]),
                Integer.parseInt(endTime[1]),
                Integer.parseInt(endTime[2])
        );

        NET_IN_QUERY_COURSE_OPEN stuStartIn = new NET_IN_QUERY_COURSE_OPEN();
        stuStartIn.stuStartTime = queryStartTime;
        stuStartIn.stuEndTime = queryEndTime;

        NET_OUT_QUERY_COURSE_OPEN stuStartOut = new NET_OUT_QUERY_COURSE_OPEN();

        boolean retStart = CourseManageModule.QueryCourseOpen(mainFrame.courseRecordLogon.m_hLoginHandle,
                stuStartIn, stuStartOut, 3000);
        if (!retStart) {
            System.err.println("查询出错: " + ToolKits.getErrorCode());
            JOptionPane.showMessageDialog(null, "查询出错: " + ToolKits.getErrorCode(), "错误信息", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FindID = stuStartOut.nFindID;               // 这是本次查询的句柄，句柄和每次查询任务一一对应
        totalCount = stuStartOut.nTotalNum;         // 这是总数据条数
        System.out.println("FindID: " + FindID);
        System.out.println("查询到: " + totalCount + " 条记录");

        // 查询第一页
        queryCourse(0);

        // 更改按钮展示
        mainFrame.startQueryButton.setText("结束查询");
        isQuery = true;
    }

    /**
     * 查询下一页
     */
    public void queryCourseNext() {
        if (startIdx + maxFetch < totalCount) {
            startIdx = startIdx + maxFetch;
            queryCourse(startIdx);
        }
    }

    /**
     * 查询上一页
     */
    public void queryCoursePre() {
        if (startIdx - maxFetch >= 0) {
            startIdx = startIdx - maxFetch;
            queryCourse(startIdx);
        }
    }

    /**
     * 结束查询
     */
    public void queryCourseEnd() {
        NET_IN_QUERY_COURSE_CLOSE stuIn = new NET_IN_QUERY_COURSE_CLOSE();
        stuIn.nFindID = FindID;
        NET_OUT_QUERY_COURSE_CLOSE stuOut = new NET_OUT_QUERY_COURSE_CLOSE();

        boolean ret = QueryCourseClose(mainFrame.courseRecordLogon.m_hLoginHandle, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("结束查询失败!");
        }

        // 重置各个查询参数
        resetQueryParams();
        resetButtonStatus();
        isQuery = false;
        // 清空展示界面
        clearTableModel(mainFrame.courseTable);
    }

    /**
     * 重置查询参数
     */
    private void resetQueryParams() {
        FindID = 0;
        totalCount = 0;
        startIdx = 0;

        displayQueryList.clear();
        stuQueryOut.clear();
    }

    /**
     * 重置按钮状态
     */
    private void resetButtonStatus() {
        mainFrame.queryPreButton.setEnabled(false);
        mainFrame.queryNextButton.setEnabled(false);
        mainFrame.startQueryButton.setText("查询");
    }

    /**
     * 指定起始序号查询
     *
     * @param startIdx 查询
     */
    public void queryCourse(int startIdx) {

        ///////////////////////////// QueryCourse ////////////////////////////////
        //////////////////////////////////////////////////////////////////////////

        NET_IN_QUERY_COURSE stuQueryIn = new NET_IN_QUERY_COURSE();
        stuQueryIn.nOffset = startIdx;      // 查询起始序号的偏移量
        stuQueryIn.nFindID = FindID;        // 查询句柄
        stuQueryIn.nCount = maxFetch;       // 在最大获取数量
        stuQueryOut.clear();   // 清除原先的数据
        boolean ret = QueryCourse(mainFrame.courseRecordLogon.m_hLoginHandle,
                stuQueryIn, stuQueryOut, 3000);
        if (!ret) {
            System.err.println("获取查询数据失败！");
            this.queryCourseEnd();
            return;
        }
        /////////////////———> 更新数据
        displayQueryList.clear();

        int nRet = stuQueryOut.nCountResult;   // 实际查询到的数据量

        // 提取数据的具体内容并在界面上展示
        for (int i = 0; i < nRet; i++) {
            CourseInfo info = new CourseInfo();
            info.idx = i + startIdx;

            // 课程时间
            info.stuStartTime = stuQueryOut.stuCourseResult[i].stuCourseInfo.stuStartTime;
            info.stuEndTime = stuQueryOut.stuCourseResult[i].stuCourseInfo.stuEndTime;
            info.SetStrCourseDateTime();

            // 课程基本信息
            try {
                info.courseName = new String(stuQueryOut.stuCourseResult[i].stuCourseInfo.szCourseName, encode).trim();
                info.teacherName = new String(stuQueryOut.stuCourseResult[i].stuCourseInfo.szTeacherName, encode).trim();
                info.introduction = new String(stuQueryOut.stuCourseResult[i].stuCourseInfo.szIntroduction, encode).trim();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            info.SetStrCompositeMode(stuQueryOut.stuCourseResult[i].stuCourseInfo.nCompositeChannelMode);

            // 可拉流状态
            info.nIsRecordNum = stuQueryOut.stuCourseResult[i].stuCourseInfo.nIsRecordNum;
            if (info.nIsRecordNum >= 0)
                System.arraycopy(stuQueryOut.stuCourseResult[i].stuCourseInfo.emIsRecord, 0, info.emIsRecord, 0, info.nIsRecordNum);

            // 需不需要录制状态
            info.nCanStartStreamNum = stuQueryOut.stuCourseResult[i].stuCourseInfo.nCanStartStreamNum;
            if (info.nCanStartStreamNum >= 0)
                System.arraycopy(stuQueryOut.stuCourseResult[i].stuCourseInfo.emCanStartStream, 0, info.emCanStartStream, 0, info.nCanStartStreamNum);
            info.SetStrVideoCtrl();

            // 是否已录制的状态
            info.strCourseState = EM_COURSE_STATE.getNoteByValue(stuQueryOut.stuCourseResult[i].stuCourseInfo.emCourseState);

            displayQueryList.add(info);
        }

        mainFrame.queryNextButton.setEnabled(startIdx + nRet < totalCount);
        mainFrame.queryPreButton.setEnabled(startIdx - maxFetch >= 0);
        mainFrame.courseTable.getSelectionModel().clearSelection();

        // 更新 Table
        updateCourseInfoTable();
    }

    /**
     * 刷新 Table
     */
    private void updateCourseInfoTable() {

        clearTableModel(mainFrame.courseTable);

        for (int i = 0; i < displayQueryList.size(); i++) {
            mainFrame.courseTable.setValueAt(displayQueryList.get(i).idx, i, 0);
            mainFrame.courseTable.setValueAt(displayQueryList.get(i).courseDate, i, 1);
            mainFrame.courseTable.setValueAt(displayQueryList.get(i).courseName, i, 2);
            mainFrame.courseTable.setValueAt(displayQueryList.get(i).teacherName, i, 3);
            mainFrame.courseTable.setValueAt(displayQueryList.get(i).coursePeriod, i, 4);
            mainFrame.courseTable.setValueAt(displayQueryList.get(i).strCompositeChannelMode, i, 5);
            if (displayQueryList.get(i).containsResource) {
                mainFrame.courseTable.setValueAt(displayQueryList.get(i).strVideoCtrlType + "+资源", i, 6);
            } else {
                mainFrame.courseTable.setValueAt(displayQueryList.get(i).strVideoCtrlType, i, 6);
            }
            mainFrame.courseTable.setValueAt(displayQueryList.get(i).strCourseState, i, 7);
        }
    }

    /**
     * 清空 Table
     */
    public static void clearTableModel(JTable jTableModel) {
        int rowCount = jTableModel.getRowCount();
        int columnCount = jTableModel.getColumnCount();
        // 清空DefaultTableModel中的内容
        for (int i = 0; i < rowCount; i++)//表格中的行数
        {
            for (int j = 0; j < columnCount; j++) {//表格中的列数
                jTableModel.setValueAt(" ", i, j);//逐个清空
            }
        }
    }
}
