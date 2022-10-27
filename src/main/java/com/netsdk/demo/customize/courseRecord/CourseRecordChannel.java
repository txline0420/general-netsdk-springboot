package com.netsdk.demo.customize.courseRecord;

import com.netsdk.demo.customize.courseRecord.modules.CourseChannelModule;
import com.netsdk.demo.customize.courseRecord.pojo.RealPreviewChannel;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.EM_CAN_START_STREAM;
import com.netsdk.lib.enumeration.EM_IS_RECORD;
import com.netsdk.lib.enumeration.NET_EM_LOGIC_CHANNEL;
import com.netsdk.lib.structure.NET_IN_GET_DEFAULT_REAL_CHANNEL;
import com.netsdk.lib.structure.NET_IN_GET_REAL_PREVIEW_CHANNEL;
import com.netsdk.lib.structure.NET_OUT_GET_DEFAULT_REAL_CHANNEL;
import com.netsdk.lib.structure.NET_OUT_GET_REAL_PREVIEW_CHANNEL;

/**
 * 这里存放设备的一些信息
 *
 * @author ： 47040
 * @since ： Created in 2020/9/18 10:18
 */
public class CourseRecordChannel {

    /**
     * 获取指定房间的所有真实通道号
     *
     * @param lLoginID 登录句柄
     * @return 真实通道信息
     */
    public static RealPreviewChannel GetRealPreviewChannels(NetSDKLib.LLong lLoginID) {
        return reloadRealPreviewChannels(lLoginID);
    }

    /**
     * 获取默认的资源通道对应的真实通道映射关系
     *
     * @param lLoginID 登录句柄
     * @return 映射关系
     */
    public static RealPreviewChannel GetDefaultRealChannels(NetSDKLib.LLong lLoginID) {
        return getDefaultRealChannels(lLoginID);
    }

    /**
     * 获取指定序号的资源通道的真实通道号
     *
     * @param lLoginID 登录句柄
     * @return 合成通道号
     */
    public static int GetResourceChannel(NetSDKLib.LLong lLoginID, int resourceOrder) {
        NET_IN_GET_REAL_PREVIEW_CHANNEL stuIn = new NET_IN_GET_REAL_PREVIEW_CHANNEL();
        stuIn.nChannelCount = 1;
        stuIn.stuChannelInfo[0].emLogicChannel = resourceOrder;   // 视频逻辑通道号
        NET_OUT_GET_REAL_PREVIEW_CHANNEL stuOut = new NET_OUT_GET_REAL_PREVIEW_CHANNEL();
        stuOut.nChannelNum = 1;

        boolean ret = CourseChannelModule.GetRealPreviewChannel(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("获取房间真实预览通道失败");
            return -1;   // 表示无效
        }
        System.out.println(stuOut.nChannel[0]);
        return stuOut.nChannel[0];
    }

    /**
     * 获取指定教室号的所有资源通道对应的真实通道
     *
     * @param lLoginID 登录句柄
     */
    private static RealPreviewChannel reloadRealPreviewChannels(NetSDKLib.LLong lLoginID) {

        NET_IN_GET_REAL_PREVIEW_CHANNEL stuIn = new NET_IN_GET_REAL_PREVIEW_CHANNEL();
        stuIn.nChannelCount = 15;
        for (int i = 0; i < 15; i++) {
            stuIn.stuChannelInfo[i].nRoomID = 0;
        }
        stuIn.stuChannelInfo[0].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_COMPOSITE.getValue();                             // 视频组合通道号
        stuIn.stuChannelInfo[1].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_PPT.getValue();                                   // PPT显示逻辑通道号
        stuIn.stuChannelInfo[2].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARD.getValue();                            // 板书特写逻辑通道号
        stuIn.stuChannelInfo[3].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_STUDENTFEATURE.getValue();                        // 学生特写逻辑通道号
        stuIn.stuChannelInfo[4].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_STUDENTFULLVIEW.getValue();                       // 学生全景逻辑通道号
        stuIn.stuChannelInfo[5].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERFEATURE.getValue();                        // 教师特写逻辑通道号
        stuIn.stuChannelInfo[6].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERFULLVIEW.getValue();                       // 教师全景逻辑通道号
        stuIn.stuChannelInfo[7].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERDETECT.getValue();                         // 教师检测逻辑通道号
        stuIn.stuChannelInfo[8].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARDDETECT.getValue();                      // 板书检测逻辑通道号
        stuIn.stuChannelInfo[9].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARD1.getValue();                           // 板书特写1逻辑通道号
        stuIn.stuChannelInfo[10].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARDDETECT1.getValue();                    // 板书检测1逻辑通道号
        stuIn.stuChannelInfo[11].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VISUALPRESENTER.getValue();                      // 展台显示逻辑通道号
        stuIn.stuChannelInfo[12].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEOSURVEILLANCE.getValue();                    // 视频监控逻辑通道号
        stuIn.stuChannelInfo[13].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEOCONFERENCE.getValue();                      // 互动会议逻辑通道号
        stuIn.stuChannelInfo[14].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEO_PRESENTATION.getValue();                   // 互动演示逻辑通道

        NET_OUT_GET_REAL_PREVIEW_CHANNEL stuOut = new NET_OUT_GET_REAL_PREVIEW_CHANNEL();
        stuOut.nChannelNum = 15;

        boolean ret = CourseChannelModule.GetRealPreviewChannel(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("获取真实预览通道失败");
            return null;
        }

        RealPreviewChannel realChannel = new RealPreviewChannel();
        realChannel.roomID = 0;
        realChannel.CHANNEL_COMPOSITE = stuOut.nChannel[0];
        realChannel.CHANNEL_PPT = stuOut.nChannel[1];
        realChannel.CHANNEL_BLACKBOARD = stuOut.nChannel[2];
        realChannel.CHANNEL_STUDENTFEATURE = stuOut.nChannel[3];
        realChannel.CHANNEL_STUDENTFULLVIEW = stuOut.nChannel[4];
        realChannel.CHANNEL_TEACHERFEATURE = stuOut.nChannel[5];
        realChannel.CHANNEL_TEACHERFULLVIEW = stuOut.nChannel[6];
        realChannel.CHANNEL_TEACHERDETECT = stuOut.nChannel[7];
        realChannel.CHANNEL_BLACKBOARDDETECT = stuOut.nChannel[8];
        realChannel.CHANNEL_BLACKBOARD1 = stuOut.nChannel[9];
        realChannel.CHANNEL_BLACKBOARDDETECT1 = stuOut.nChannel[10];
        realChannel.CHANNEL_VISUALPRESENTER = stuOut.nChannel[11];
        realChannel.CHANNEL_VIDEOSURVEILLANCE = stuOut.nChannel[12];
        realChannel.CHANNEL_VIDEOCONFERENCE = stuOut.nChannel[13];
        realChannel.CHANNEL_VIDEO_PRESENTATION = stuOut.nChannel[14];
        realChannel.setRealPreviewChannels();

        return realChannel;
    }


    /**
     * 获取默认的资源通道对应的真实通道映射关系
     *
     * @param lLoginID 登录句柄
     */
    public static RealPreviewChannel getDefaultRealChannels(NetSDKLib.LLong lLoginID) {

        NET_IN_GET_DEFAULT_REAL_CHANNEL stuIn = new NET_IN_GET_DEFAULT_REAL_CHANNEL();
        stuIn.nChannelCount = 15;
        for (int i = 0; i < 15; i++) {
            stuIn.stuChannelInfo[i].nRoomID = 0;
        }
        stuIn.stuChannelInfo[0].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_COMPOSITE.getValue();                             // 视频组合通道号
        stuIn.stuChannelInfo[1].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_PPT.getValue();                                   // PPT显示逻辑通道号
        stuIn.stuChannelInfo[2].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARD.getValue();                            // 板书特写逻辑通道号
        stuIn.stuChannelInfo[3].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_STUDENTFEATURE.getValue();                        // 学生特写逻辑通道号
        stuIn.stuChannelInfo[4].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_STUDENTFULLVIEW.getValue();                       // 学生全景逻辑通道号
        stuIn.stuChannelInfo[5].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERFEATURE.getValue();                        // 教师特写逻辑通道号
        stuIn.stuChannelInfo[6].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERFULLVIEW.getValue();                       // 教师全景逻辑通道号
        stuIn.stuChannelInfo[7].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERDETECT.getValue();                         // 教师检测逻辑通道号
        stuIn.stuChannelInfo[8].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARDDETECT.getValue();                      // 板书检测逻辑通道号
        stuIn.stuChannelInfo[9].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARD1.getValue();                           // 板书特写1逻辑通道号
        stuIn.stuChannelInfo[10].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARDDETECT1.getValue();                    // 板书检测1逻辑通道号
        stuIn.stuChannelInfo[11].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VISUALPRESENTER.getValue();                      // 展台显示逻辑通道号
        stuIn.stuChannelInfo[12].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEOSURVEILLANCE.getValue();                    // 视频监控逻辑通道号
        stuIn.stuChannelInfo[13].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEOCONFERENCE.getValue();                      // 互动会议逻辑通道号
        stuIn.stuChannelInfo[14].emLogicChannel = NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEO_PRESENTATION.getValue();                   // 互动演示逻辑通道

        NET_OUT_GET_DEFAULT_REAL_CHANNEL stuOut = new NET_OUT_GET_DEFAULT_REAL_CHANNEL();
        stuOut.nChannelNum = 15;

        boolean ret = CourseChannelModule.GetDefaultRealChannel(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("获取默认的资源通道对应的真实通道映射关系");
            return null;
        }

        RealPreviewChannel defaultChannel = new RealPreviewChannel();
        defaultChannel.roomID = 0;
        defaultChannel.CHANNEL_COMPOSITE = stuOut.nChannel[0];
        defaultChannel.CHANNEL_PPT = stuOut.nChannel[1];
        defaultChannel.CHANNEL_BLACKBOARD = stuOut.nChannel[2];
        defaultChannel.CHANNEL_STUDENTFEATURE = stuOut.nChannel[3];
        defaultChannel.CHANNEL_STUDENTFULLVIEW = stuOut.nChannel[4];
        defaultChannel.CHANNEL_TEACHERFEATURE = stuOut.nChannel[5];
        defaultChannel.CHANNEL_TEACHERFULLVIEW = stuOut.nChannel[6];
        defaultChannel.CHANNEL_TEACHERDETECT = stuOut.nChannel[7];
        defaultChannel.CHANNEL_BLACKBOARDDETECT = stuOut.nChannel[8];
        defaultChannel.CHANNEL_BLACKBOARD1 = stuOut.nChannel[9];
        defaultChannel.CHANNEL_BLACKBOARDDETECT1 = stuOut.nChannel[10];
        defaultChannel.CHANNEL_VISUALPRESENTER = stuOut.nChannel[11];
        defaultChannel.CHANNEL_VIDEOSURVEILLANCE = stuOut.nChannel[12];
        defaultChannel.CHANNEL_VIDEOCONFERENCE = stuOut.nChannel[13];
        defaultChannel.CHANNEL_VIDEO_PRESENTATION = stuOut.nChannel[14];
        defaultChannel.setRealPreviewChannels();

        return defaultChannel;
    }

    ///////////////////////////////////////////////// 合成通道 拉流/录象 设置 ///////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 合成通道 拉流/不录象
     */
    public static void CompositeChannelSteamButNoRecord(int[] emCanStartStream, int[] emIsRecord) {
        // 合成通道拉流、不录象
        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_COMPOSITE.getValue()] = EM_CAN_START_STREAM.EM_CAN_START_STREAM_ON.getValue();
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_COMPOSITE.getValue()] = EM_IS_RECORD.EM_IS_RECORD_OFF.getValue();
    }

    /**
     * 合成通道 不拉流/录象
     */
    public static void CompositeChannelNoSteamButRecord(int[] emCanStartStream, int[] emIsRecord) {
        // 合成通道拉流、不录象
        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_COMPOSITE.getValue()] = EM_CAN_START_STREAM.EM_CAN_START_STREAM_OFF.getValue();
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_COMPOSITE.getValue()] = EM_IS_RECORD.EM_IS_RECORD_ON.getValue();
    }

    /**
     * 合成通道 拉流/录象
     */
    public static void CompositeChannelSteamAndRecord(int[] emCanStartStream, int[] emIsRecord) {
        // 合成通道拉流、不录象
        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_COMPOSITE.getValue()] = EM_CAN_START_STREAM.EM_CAN_START_STREAM_ON.getValue();
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_COMPOSITE.getValue()] = EM_IS_RECORD.EM_IS_RECORD_ON.getValue();
    }

    ///////////////////////////////////////////////////// 设置资源通道 /////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 具体哪些资源通道是设备启用的，请参考网页，这里我也以网页上的可选的资源通道作为填写标准：
     * 网页上还可以选择不同的资源通道，Demo里简单起见就统一处理了，现在网页上可操作的资源通道具体包括：
     *
     *  (0, "视频组合通道号"),
     *  (1, "PPT显示逻辑通道号"),
     *  (2,"板书特写逻辑通道号"),
     *  (3,"学生特写逻辑通道号"),
     *  (4,"学生全景逻辑通道号"),
     *  (5,"教师特写逻辑通道号"),
     *  (6,"教师全景逻辑通道号"),
     *  (9,"板书特写1逻辑通道号"),
     *  (11,"展台显示逻辑通道号"),
     *  (12,"视频监控逻辑通道号"),
     *
     *  以后如果设备有了调整，请自行调整下面的代码
     *  另外：不用考虑是否绑定了真实通道
     */

    /**
     * 资源通道 拉流/不录象
     */
    public static void ResourceChannelSteamNoRecord(int[] emCanStartStream, int[] emIsRecord) {
        // 有效的的资源通道拉流/不录象，无效的填 0
        int STEAM_STATUS = EM_CAN_START_STREAM.EM_CAN_START_STREAM_ON.getValue();     // 拉流
        int RECORD_STATUS = EM_IS_RECORD.EM_IS_RECORD_OFF.getValue();                 // 不录制

        SetResourceChannelMode(emCanStartStream, emIsRecord, STEAM_STATUS, RECORD_STATUS);
    }


    /**
     * 资源通道 不拉流/不录象
     */
    public static void ResourceChannelNoSteamNoRecord(int[] emCanStartStream, int[] emIsRecord) {
        // 有效的的资源通道 不拉流/不录象，无效的填 0
        int STEAM_STATUS = EM_CAN_START_STREAM.EM_CAN_START_STREAM_OFF.getValue();     // 不拉流
        int RECORD_STATUS = EM_IS_RECORD.EM_IS_RECORD_OFF.getValue();                 // 不录制

        SetResourceChannelMode(emCanStartStream, emIsRecord, STEAM_STATUS, RECORD_STATUS);
    }

    /**
     * 资源通道 拉流/录象
     */
    public static void ResourceChannelSteamRecord(int[] emCanStartStream, int[] emIsRecord) {
        // 有效的的资源通道 拉流/录象，无效的填 0
        int STEAM_STATUS = EM_CAN_START_STREAM.EM_CAN_START_STREAM_ON.getValue();     // 拉流
        int RECORD_STATUS = EM_IS_RECORD.EM_IS_RECORD_ON.getValue();                 // 录制

        SetResourceChannelMode(emCanStartStream, emIsRecord, STEAM_STATUS, RECORD_STATUS);
    }

    /**
     * 资源通道 不拉流/录象
     */
    public static void ResourceChannelNoSteamRecord(int[] emCanStartStream, int[] emIsRecord) {
        // 有效的的资源通道 不拉流/录象，无效的填 0
        int STEAM_STATUS = EM_CAN_START_STREAM.EM_CAN_START_STREAM_OFF.getValue();     // 不拉流
        int RECORD_STATUS = EM_IS_RECORD.EM_IS_RECORD_ON.getValue();                 // 不录制

        SetResourceChannelMode(emCanStartStream, emIsRecord, STEAM_STATUS, RECORD_STATUS);
    }

    private static void SetResourceChannelMode(int[] emCanStartStream, int[] emIsRecord, int STEAM_STATUS, int RECORD_STATUS) {

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_PPT.getValue()] = STEAM_STATUS;                      // PPT显示逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_PPT.getValue()] = RECORD_STATUS;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARD.getValue()] = STEAM_STATUS;               // 板书特写逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARD.getValue()] = RECORD_STATUS;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_STUDENTFEATURE.getValue()] = STEAM_STATUS;           // 学生特写逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_STUDENTFEATURE.getValue()] = RECORD_STATUS;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_STUDENTFULLVIEW.getValue()] = STEAM_STATUS;          // 学生全景逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_STUDENTFULLVIEW.getValue()] = RECORD_STATUS;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERFEATURE.getValue()] = STEAM_STATUS;           // 教师特写逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERFEATURE.getValue()] = RECORD_STATUS;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERFULLVIEW.getValue()] = STEAM_STATUS;          // 教师全景逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERFULLVIEW.getValue()] = RECORD_STATUS;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERDETECT.getValue()] = 0;                       // 教师检测逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_TEACHERDETECT.getValue()] = 0;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARDDETECT.getValue()] = 0;                    // 板书检测逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARDDETECT.getValue()] = 0;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARD1.getValue()] = STEAM_STATUS;              // 板书特写1逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARD1.getValue()] = RECORD_STATUS;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARDDETECT1.getValue()] = 0;                   // 板书检测1逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_BLACKBOARDDETECT1.getValue()] = 0;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VISUALPRESENTER.getValue()] = STEAM_STATUS;          // 展台显示逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VISUALPRESENTER.getValue()] = RECORD_STATUS;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEOSURVEILLANCE.getValue()] = STEAM_STATUS;        // 视频监控逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEOSURVEILLANCE.getValue()] = RECORD_STATUS;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEOCONFERENCE.getValue()] = 0;                     // 互动会议逻辑通道号
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEOCONFERENCE.getValue()] = 0;

        emCanStartStream[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEO_PRESENTATION.getValue()] = 0;                  // 互动演示逻辑通道
        emIsRecord[NET_EM_LOGIC_CHANNEL.NET_EM_LOGIC_CHANNEL_VIDEO_PRESENTATION.getValue()] = 0;
    }
}
