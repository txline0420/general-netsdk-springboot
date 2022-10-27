package com.netsdk.demo.customize.courseRecord.pojo;

import com.netsdk.lib.enumeration.EM_CAN_START_STREAM;
import com.netsdk.lib.enumeration.EM_COURSE_STATE;
import com.netsdk.lib.enumeration.EM_IS_RECORD;
import com.netsdk.lib.structure.NET_TIME;

import static com.netsdk.demo.customize.courseRecord.CourseRecordChannel.*;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/21 13:43
 */
public class CourseInfo {
    /**
     * 序号 查询时用
     */
    public int idx = 0;

    /////////////////////////////////// 起始时间 ////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * 课程开始时间
     */
    public NET_TIME stuStartTime;
    /**
     * 课程结束时间
     */
    public NET_TIME stuEndTime;
    /**
     * 课程日期 (展示用)
     */
    public String courseDate = "";
    /**
     * 课程时间段 (展示用)
     */
    public String coursePeriod = "";

    //////////////////////////////// 其他属性 /////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    /**
     * 课程名称
     */
    public String courseName = "";
    /**
     * 教师名称
     */
    public String teacherName = "";
    /**
     * 课程简介
     */
    public String introduction;

    /////////////////////////////////// 控制模式 /////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    /**
     * 能否被拉流逻辑通道数
     */
    public int nCanStartStreamNum;
    /**
     * 是否要录像逻辑通道号数
     */
    public int nIsRecordNum;
    /**
     * 能否被拉流 {@link EM_CAN_START_STREAM}
     */
    public int[] emCanStartStream = new int[64];
    /**
     * 是否要录像 {@link EM_IS_RECORD}
     */
    public int[] emIsRecord = new int[64];
    /**
     * 视频控制模式(展示用) 直播", "录制", "录播
     */
    public String strVideoCtrlType = "";

    ////////////////////////////////////// 录制模式 ///////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * 组合通道模式; 0: 无效, 1: 电影模式, 2: 常态模式, 3: 精品模式, < 0:自定义模式
     */
    public int nCompositeChannelMode;
    /**
     * 合成模式 (展示用) "电影模式", "多画面模式"
     */
    public String strCompositeChannelMode = "";

    /**
     * 是否是资源模式 (展示用)
     */
    public boolean containsResource;

    ////////////////////////////////////// 课程状态 ///////////////////////////////////
    /**
     * 课程录像状态 {@link EM_COURSE_STATE}
     */
    public int emCourseState;
    /**
     * 课程状态 (展示用)
     */
    public String strCourseState = "";

    /**
     * 依据课程起始时间给出日期和时间段注释
     */
    public void SetStrCourseDateTime() {
        if (stuStartTime == null || stuEndTime == null) {
            return;
        }
        String[] startTimeInfo = stuStartTime.toStringTime().split("\\s+");
        String[] endTimeInfo = stuEndTime.toStringTime().split("\\s+");
        this.courseDate = startTimeInfo[0];
        this.coursePeriod = startTimeInfo[1] + " - " + endTimeInfo[1];
    }

    /**
     * 依据拉流和录像配置设置视频控制模式注释
     */
    public void SetStrVideoCtrl() {
        if (emCanStartStream[0] == EM_CAN_START_STREAM.EM_CAN_START_STREAM_ON.getValue() &&
                emIsRecord[0] == EM_IS_RECORD.EM_IS_RECORD_OFF.getValue()) {
            strVideoCtrlType = "直播";
        } else if (emCanStartStream[0] == EM_CAN_START_STREAM.EM_CAN_START_STREAM_OFF.getValue() &&
                emIsRecord[0] == EM_IS_RECORD.EM_IS_RECORD_ON.getValue()) {
            strVideoCtrlType = "录制";
        } else if (emCanStartStream[0] == EM_CAN_START_STREAM.EM_CAN_START_STREAM_ON.getValue() &&
                emIsRecord[0] == EM_IS_RECORD.EM_IS_RECORD_ON.getValue()) {
            strVideoCtrlType = "录播";
        }

        // 检查下是不是资源模式
        containsResource = false;
        for (int i = 1; i < 15; i++) {
            if (emCanStartStream[i] == EM_CAN_START_STREAM.EM_CAN_START_STREAM_ON.getValue() ||
                    emCanStartStream[i] == EM_IS_RECORD.EM_IS_RECORD_ON.getValue()) {
                // 是资源模式
                containsResource = true;
                break;
            }
        }
    }

    /**
     * 依据下拉框枚举设置正确的录制参数
     */
    public void ParseStrVideoCtrl() {
        // "直播(仅拉流)", "录制(仅录制)", "录播(拉流并录制)"
        nCanStartStreamNum = 15;
        nIsRecordNum = 15;

        if ("直播".equals(strVideoCtrlType)) {   // 拉流(全部)，不录制(仅合成通道)

            CompositeChannelSteamButNoRecord(emCanStartStream, emIsRecord);

            if (containsResource) {
                // 资源模式下 拉流/不录制
                ResourceChannelSteamNoRecord(emCanStartStream, emIsRecord);
            } else {
                // 非资源模式下 不拉流/不录制
                ResourceChannelNoSteamNoRecord(emCanStartStream, emIsRecord);
            }
        } else if ("录制".equals(strVideoCtrlType)) {  // 不拉流(全部)，录制(仅合成通道)
            CompositeChannelNoSteamButRecord(emCanStartStream, emIsRecord);

            if (containsResource) {
                // 资源模式下 不拉流/录制
                ResourceChannelNoSteamRecord(emCanStartStream, emIsRecord);
            } else {
                // 非资源模式下 不拉流/不录制
                ResourceChannelNoSteamNoRecord(emCanStartStream, emIsRecord);
            }
        } else if ("录播".equals(strVideoCtrlType)) { // 拉流(全部)，录制(仅合成通道)
            CompositeChannelSteamAndRecord(emCanStartStream, emIsRecord);

            if (containsResource) {
                // 资源模式下 拉流/录制
                ResourceChannelSteamRecord(emCanStartStream, emIsRecord);
            } else {
                // 非资源模式下 不拉流/不录制
                ResourceChannelNoSteamNoRecord(emCanStartStream, emIsRecord);
            }
        }
    }

    /**
     * 根据合成通道模式数字给出具体注释
     *
     * @param mode 合成通道模式数字
     */
    public void SetStrCompositeMode(int mode) {
        String strMode = "无效";
        switch (mode) {
            // 0: 无效, 1: 电影模式, 2: 常态模式, 3: 精品模式, <0:自定义模式
            case 0:
                strMode = "无效";
                break;
            case 1:
                strMode = "电影模式";
                break;
            case 2:
                strMode = "多画面模式";  // 常态模式
                break;
            case 3:
                strMode = "精品模式";
                break;
            default:
                if (mode < 0) strMode = "自定义模式";
                break;
        }
        strCompositeChannelMode = strMode;
    }

    /**
     * 依据下拉框枚举填写正确的参数
     */
    public void ParseStrCompositeMode() {
        // "电影模式", "多画面模式"
        if ("电影模式".equals(strCompositeChannelMode)) {
            nCompositeChannelMode = 1;   // 电影枚举
        } else if ("多画面模式".equals(strCompositeChannelMode)) {
            nCompositeChannelMode = 2;  // 多画面枚举
        }
    }
}
