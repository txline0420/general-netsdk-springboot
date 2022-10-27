package com.netsdk.demo.customize.courseRecord.pojo;

/**
 * 逻辑通道号对应的实际通道
 *
 * @author ： 47040
 * @since ： Created in 2020/9/18 10:36
 */
public class RealPreviewChannel {
    /**
     * 教室编号
     */
    public int roomID;
    /**
     * 视频组合通道号
     */
    public int CHANNEL_COMPOSITE;
    /**
     * PPT显示逻辑通道号
     */
    public int CHANNEL_PPT;
    /**
     * 板书特写逻辑通道号
     */
    public int CHANNEL_BLACKBOARD;
    /**
     * 学生特写逻辑通道号
     */
    public int CHANNEL_STUDENTFEATURE;
    /**
     * 学生全景逻辑通道号
     */
    public int CHANNEL_STUDENTFULLVIEW;
    /**
     * 教师特写逻辑通道号
     */
    public int CHANNEL_TEACHERFEATURE;
    /**
     * 教师全景逻辑通道号
     */
    public int CHANNEL_TEACHERFULLVIEW;
    /**
     * 教师检测逻辑通道号
     */
    public int CHANNEL_TEACHERDETECT;
    /**
     * 板书检测逻辑通道号
     */
    public int CHANNEL_BLACKBOARDDETECT;
    /**
     * 板书特写1逻辑通道号
     */
    public int CHANNEL_BLACKBOARD1;
    /**
     * 板书检测1逻辑通道号
     */
    public int CHANNEL_BLACKBOARDDETECT1;
    /**
     * 展台显示逻辑通道号
     */
    public int CHANNEL_VISUALPRESENTER;
    /**
     * 视频监控逻辑通道号
     */
    public int CHANNEL_VIDEOSURVEILLANCE;
    /**
     * 互动会议逻辑通道号
     */
    public int CHANNEL_VIDEOCONFERENCE;
    /**
     * 互动演示逻辑通道
     */
    public int CHANNEL_VIDEO_PRESENTATION;

    /**
     * 按顺序排列
     */
    public int[] RealPreviewChannels = new int[15];

    public int[] getRealPreviewChannels() {
        return RealPreviewChannels;
    }

    public void setRealPreviewChannels() {
        RealPreviewChannels[0] = CHANNEL_COMPOSITE;
        RealPreviewChannels[1] = CHANNEL_PPT;
        RealPreviewChannels[2] = CHANNEL_BLACKBOARD;
        RealPreviewChannels[3] = CHANNEL_STUDENTFEATURE;
        RealPreviewChannels[4] = CHANNEL_STUDENTFULLVIEW;
        RealPreviewChannels[5] = CHANNEL_TEACHERFEATURE;
        RealPreviewChannels[6] = CHANNEL_TEACHERFULLVIEW;
        RealPreviewChannels[7] = CHANNEL_TEACHERDETECT;
        RealPreviewChannels[8] = CHANNEL_BLACKBOARDDETECT;
        RealPreviewChannels[9] = CHANNEL_BLACKBOARD1;
        RealPreviewChannels[10] = CHANNEL_BLACKBOARDDETECT1;
        RealPreviewChannels[11] = CHANNEL_VISUALPRESENTER;
        RealPreviewChannels[12] = CHANNEL_VIDEOSURVEILLANCE;
        RealPreviewChannels[13] = CHANNEL_VIDEOCONFERENCE;
        RealPreviewChannels[14] = CHANNEL_VIDEO_PRESENTATION;
    }

    @Override
    public String toString() {
        return "RealPreviewChannel{" + "\n" +
                "CHANNEL_COMPOSITE=" + CHANNEL_COMPOSITE + "\n" +
                ", CHANNEL_PPT=" + CHANNEL_PPT + "\n" +
                ", CHANNEL_BLACKBOARD=" + CHANNEL_BLACKBOARD + "\n" +
                ", CHANNEL_STUDENTFEATURE=" + CHANNEL_STUDENTFEATURE + "\n" +
                ", CHANNEL_STUDENTFULLVIEW=" + CHANNEL_STUDENTFULLVIEW + "\n" +
                ", CHANNEL_TEACHERFEATURE=" + CHANNEL_TEACHERFEATURE + "\n" +
                ", CHANNEL_TEACHERFULLVIEW=" + CHANNEL_TEACHERFULLVIEW + "\n" +
                ", CHANNEL_TEACHERDETECT=" + CHANNEL_TEACHERDETECT + "\n" +
                ", CHANNEL_BLACKBOARDDETECT=" + CHANNEL_BLACKBOARDDETECT + "\n" +
                ", CHANNEL_BLACKBOARD1=" + CHANNEL_BLACKBOARD1 + "\n" +
                ", CHANNEL_BLACKBOARDDETECT1=" + CHANNEL_BLACKBOARDDETECT1 + "\n" +
                ", CHANNEL_VISUALPRESENTER=" + CHANNEL_VISUALPRESENTER + "\n" +
                ", CHANNEL_VIDEOSURVEILLANCE=" + CHANNEL_VIDEOSURVEILLANCE + "\n" +
                ", CHANNEL_VIDEOCONFERENCE=" + CHANNEL_VIDEOCONFERENCE + "\n" +
                ", CHANNEL_VIDEO_PRESENTATION=" + CHANNEL_VIDEO_PRESENTATION + "\n" +
                '}';
    }
}
