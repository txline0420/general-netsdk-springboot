package com.netsdk.demo.customize.courseRecord;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.NET_EM_RECORD_MODE;
import com.netsdk.lib.structure.*;

import java.io.UnsupportedEncodingException;

import static com.netsdk.demo.customize.courseRecord.modules.CourseRecordModule.*;
import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/29 9:18
 */
public class TestCourseRecordRecord {

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    /**
     * 提供视频: 文件查 (ok) 下载功能 ()
     * 可以查询录播主机已录制的视频
     * 并且可以下载到第三方平台上
     */

    /**************************** 录像查询 ***************************************/

    /**
     * 获取教室录像信息
     *
     * @param lLoginID 登录句柄
     */
    public void GetOperateCourseRecordInfoTest(NetSDKLib.LLong lLoginID) {
        NET_IN_COURSERECORD_GETINFO stuIn = new NET_IN_COURSERECORD_GETINFO();
        stuIn.nClassRoomID = 0;   // 房间号根本没启用, 固定写 0
        NET_OUT_COURSERECORD_GETINFO stuOut = new NET_OUT_COURSERECORD_GETINFO();

        boolean ret = GetOperateCourseRecord(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("获取教室录像信息失败!");
            return;
        }

        StringBuilder builder = new StringBuilder()
                .append("\n——————————————当前录像信息——————————————\n")
                .append("共有逻辑通道数: ").append(stuOut.nChannelNum).append("\n");
        for (int i = 0; i < stuOut.nChannelNum; i++) {
            // 0:无效,1:录像,2不录像,下标对应为逻辑通道号
            builder.append(String.format("通道[%2d]: ", i)).append(stuOut.nCanRecord[i]).append("\n");
        }
        System.out.println(builder.toString());
    }

    /**
     * 设置教室录像信息
     *
     * @param lLoginID 登录句柄
     */
    public void SetOperateCourseRecordInfoTest(NetSDKLib.LLong lLoginID) {
        /**
         * 这里的用例是: 先获取教室录像信息, 再设置 合成通道(逻辑通道 0) 为录像状态
         */
        //////////////////////// 获取教室录像信息 ////////////////////////

        NET_IN_COURSERECORD_GETINFO stuInGet = new NET_IN_COURSERECORD_GETINFO();
        stuInGet.nClassRoomID = 0;   // 房间号根本没启用, 固定写 0
        NET_OUT_COURSERECORD_GETINFO stuOutGet = new NET_OUT_COURSERECORD_GETINFO();

        boolean ret1 = GetOperateCourseRecord(lLoginID, stuInGet, stuOutGet, 3000);
        if (!ret1) {
            System.err.println("获取教室录像信息失败!");
            return;
        }

        //////////////////////// 设置教室录像信息 ////////////////////////

        NET_IN_COURSERECORD_SETINFO stuInSet = new NET_IN_COURSERECORD_SETINFO();
        stuInSet.nClassRoomID = 0;                       // 房间号根本没启用, 固定写 0
        stuInSet.nChannelNum = stuOutGet.nChannelNum;    // 逻辑通道数，复用获取到的信息，理论上有64个，不过大部分都是无效的
        stuInSet.nCanRecord = stuOutGet.nCanRecord;      // 录像状态，复用获取到的信息
        // 0:无效,1:录像,2不录像,下标对应为逻辑通道号
        stuInSet.nCanRecord[0] = 1;                      // 设置逻辑通道 0 为录像状态
        NET_OUT_COURSERECORD_SETINFO stuOutSet = new NET_OUT_COURSERECORD_SETINFO();

        boolean ret2 = SetOperateCourseRecord(lLoginID, stuInSet, stuOutSet, 3000);
        if (!ret2) {
            System.err.println("设置教室录像信息失败!");
            return;
        }
        System.out.println("设置教室录像信息成功!");
    }

    /**
     * 将录像信息更新到 time 时的信息
     *
     * @param lLoginID 登录句柄
     */
    public void UpdateOperateCourseRecordInfoTest(NetSDKLib.LLong lLoginID) {
        /**
         * 这里的用例是: 录像信息跟新到 2020/9/28 0:0:0 时的信息
         */
        NET_IN_COURSERECORD_UPDATE_INFO stuIn = new NET_IN_COURSERECORD_UPDATE_INFO();
        stuIn.nClassRoomID = 0;    // 房间号根本没启用, 固定写 0
        stuIn.stuTime = new NET_TIME(2020, 9, 28, 0, 0, 0);
        NET_OUT_COURSERECORD_UPDATE_INFO stuOut = new NET_OUT_COURSERECORD_UPDATE_INFO();

        boolean ret = UpdateOperateCourseRecord(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("更新教室录像信息失败!");
            return;
        }
        System.out.println("更新教室录像信息成功!");
    }

    /**
     * 控制组合通道与逻辑通道  (锁定/解锁)
     *
     * @param lLoginID 登录句柄
     *                 <p>
     *                 调用接口的前提：
     *                 1.	该逻辑通道号必须是已绑定的通道
     *                 2.	调用unlock接口时，组合通道要处于锁定状态
     *                 3.	传入的逻辑通道号也必须是处于锁定状态的通道
     */
    public void LockControlOperateCourseCompositeChannelTest(NetSDKLib.LLong lLoginID) {
        /**
         * 这里的用例是: 锁/解锁(依据获取到的状态取反) 教师特写逻辑 5
         */
        NET_IN_COURSECOMPOSITE_GET_LOCKINFO stuGetIn = new NET_IN_COURSECOMPOSITE_GET_LOCKINFO();
        stuGetIn.nClassRoomID = 0;    // 房间号根本没启用, 固定写 0
        stuGetIn.nLogicChannel = 5;   // 教师特写逻辑 5
        NET_OUT_COURSECOMPOSITE_GET_LOCKINFO stuGetOut = new NET_OUT_COURSECOMPOSITE_GET_LOCKINFO();

        boolean ret1 = GetLockInfoOperateCourseCompositeChannel(lLoginID, stuGetIn, stuGetOut, 3000);
        if (!ret1) {
            System.err.println("获取组合通道与逻辑通道的锁定信息失败!");
            return;
        }

        //////////////////////////////////////

        NET_IN_COURSECOMPOSITE_LOCK_CONTROL stuSetIn = new NET_IN_COURSECOMPOSITE_LOCK_CONTROL();
        stuSetIn.nClassRoomID = stuGetIn.nClassRoomID;            // 房间号根本没启用, 固定写 0
        stuSetIn.bLock = stuGetOut.bState == 0 ? 1 : 0;           // 锁/加锁（取反）
        stuSetIn.nLogicChannel = stuGetIn.nLogicChannel;          // 教师特写逻辑 5 (合成通道)
        NET_OUT_COURSECOMPOSITE_LOCK_CONTROL stuSetOut = new NET_OUT_COURSECOMPOSITE_LOCK_CONTROL();

        boolean ret2 = LockControlOperateCourseCompositeChannel(lLoginID, stuSetIn, stuSetOut, 3000);
        if (!ret2) {
            System.err.println("当前逻辑通道  (锁定/解锁) 失败!");
            return;
        }
        System.out.println("当前逻辑通道  (锁定/解锁) 成功!");
    }

    /**
     * 获取组合通道与逻辑通道的锁定信息
     *
     * @param lLoginID 登录句柄
     */
    public void GetLockInfoOperateCourseCompositeChannelTest(NetSDKLib.LLong lLoginID) {
        /**
         * 这里的用例是: 获取 教师特写逻辑 5 的 锁定状态
         */
        NET_IN_COURSECOMPOSITE_GET_LOCKINFO stuIn = new NET_IN_COURSECOMPOSITE_GET_LOCKINFO();
        stuIn.nClassRoomID = 0;    // 房间号根本没启用, 固定写 0
        stuIn.nLogicChannel = 5;   // 教师特写逻辑 5
        NET_OUT_COURSECOMPOSITE_GET_LOCKINFO stuOut = new NET_OUT_COURSECOMPOSITE_GET_LOCKINFO();

        boolean ret = GetLockInfoOperateCourseCompositeChannel(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("获取组合通道与逻辑通道的锁定信息失败!");
            return;
        }
        System.out.println("获取组合通道与逻辑通道的锁定信息成功!");
        System.out.println("当前通道: " + (stuOut.bState == 0 ? "未锁定" : "被锁定"));
    }

    /**
     * 获取组合通道信息
     *
     * @param lLoginID 登录句柄
     */
    public void GetInfoOperateCourseCompositeChannelTest(NetSDKLib.LLong lLoginID) {

        NET_IN_COURSECOMPOSITE_GET_INFO stuIn = new NET_IN_COURSECOMPOSITE_GET_INFO();
        stuIn.nClassRoomId = 0;    // 房间号根本没启用, 固定写 0
        NET_OUT_COURSECOMPOSITE_GET_INFO stuOut = new NET_OUT_COURSECOMPOSITE_GET_INFO();

        boolean ret = GetInfoOperateCourseCompositeChannel(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("获取组合通道信息失败!");
            return;
        }
        System.out.println("获取组合通道信息成功!");

        StringBuilder compositeInfo = new StringBuilder();
        try {
            compositeInfo.append("\n——————————当前房间合成通道信息——————————\n")
                    .append("szCourseName 课程名称: ").append(new String(stuOut.stuChannelInfo.szCourseName, encode).trim()).append("\n")
                    .append("szTeacherName 教师名称: ").append(new String(stuOut.stuChannelInfo.szTeacherName, encode).trim()).append("\n")
                    // 录制模式; 0:无效；1:电影模式； 2:常态模式； 3:精品模式； <0:用户自定义模式
                    .append("nCompositeChannelMode 录制模式: ").append(stuOut.stuChannelInfo.nCompositeChannelMode).append("\n");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        compositeInfo.append("//--->nChannelNum 逻辑通道数量: ").append(stuOut.stuChannelInfo.nChannelNum).append("\n");
        for (int i = 0; i < stuOut.stuChannelInfo.nChannelNum; i++) {
            compositeInfo.append("逻辑通道: ").append(i).append("->")
                    .append(stuOut.stuChannelInfo.bCanStream[i] == 0 ? "不拉流" : "拉流").append("\n");
        }
        System.out.println(compositeInfo);
    }

    /**
     * 设置组合通道信息
     *
     * @param lLoginID 登录句柄
     */
    public void SetInfoOperateCourseCompositeChannelTest(NetSDKLib.LLong lLoginID) {
        /**
         * 这里的用例是: 先获取到原先的组合通道信息，再把课程名称修改为 "手动课程", 用户自定义 编号 -4 模式
         */
        ////////////////////// 获取合成通道信息 //////////////////////

        NET_IN_COURSECOMPOSITE_GET_INFO stuInGet = new NET_IN_COURSECOMPOSITE_GET_INFO();
        stuInGet.nClassRoomId = 0;    // 房间号根本没启用, 固定写 0
        NET_OUT_COURSECOMPOSITE_GET_INFO stuOutGet = new NET_OUT_COURSECOMPOSITE_GET_INFO();

        boolean ret1 = GetInfoOperateCourseCompositeChannel(lLoginID, stuInGet, stuOutGet, 3000);
        if (!ret1) {
            System.err.println("获取组合通道信息失败!");
            return;
        }
        System.out.println("获取组合通道信息成功!");

        ////////////////////// 设置合成通道信息 ///////////////////////

        NET_IN_COURSECOMPOSITE_SET_INFO stuInSet = new NET_IN_COURSECOMPOSITE_SET_INFO();
        stuInSet.nClassRoomId = 0;                                 // 房间号根本没启用, 固定写 0
        stuInSet.stuChannelInfo = stuOutGet.stuChannelInfo;        // 使用获取到的合成那个通道信息
        stuInSet.stuChannelInfo.nCompositeChannelMode = -4;        // 用户自定义 编号 -4 模式
        byte[] newCourseName = new byte[0];
        try {
            newCourseName = "手动课程".getBytes(encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.arraycopy(newCourseName, 0, stuInSet.stuChannelInfo.szCourseName, 0, newCourseName.length);
        NET_OUT_COURSECOMPOSITE_SET_INFO stuOutSet = new NET_OUT_COURSECOMPOSITE_SET_INFO();

        boolean ret2 = SetInfoOperateCourseCompositeChannel(lLoginID, stuInSet, stuOutSet, 3000);
        if (!ret2) {
            System.err.println("设置组合通道信息失败失败!");
            return;
        }
        System.out.println("设置组合通道信息成功!");
    }

    /**
     * 将组合通道信息更新到 time 时的信息
     *
     * @param lLoginID 登录句柄
     */
    public void UpdateInfoOperateCourseCompositeChannelTest(NetSDKLib.LLong lLoginID) {
        /**
         * 这里的用例是: 将组合通道信息更新到 2020/09/30 10:0:0 时的信息
         */
        NET_IN_COURSECOMPOSITE_UPDATE_INFO stuIn = new NET_IN_COURSECOMPOSITE_UPDATE_INFO();
        stuIn.nClassRoomId = 0;    // 房间号根本没启用, 固定写 0
        stuIn.stuTime = new NET_TIME(2020, 9, 30, 10, 0, 0);
        NET_OUT_COURSECOMPOSITE_UPDATE_INFO stuOut = new NET_OUT_COURSECOMPOSITE_UPDATE_INFO();

        boolean ret = UpdateInfoOperateCourseCompositeChannel(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("将组合通道信息更新到 time 时的信息失败!");
            return;
        }
        System.out.println("将组合通道信息更新到 time 时的信息成功!");
    }

    /**
     * 设置课程录像模式 注意如果重复设置相同的录像模式，设备将会返回失败
     *
     * @param lLoginID 登录句柄
     */
    public void SetCourseRecordModeTest(NetSDKLib.LLong lLoginID) {
        /**
         * 这里的用例是: 设置当前教室为 普通模式
         */
        NET_IN_SET_COURSE_RECORD_MODE stuIn = new NET_IN_SET_COURSE_RECORD_MODE();
        stuIn.nClassRoomID = 0;    // 房间号根本没启用, 固定写 0
        stuIn.emRecordMode = NET_EM_RECORD_MODE.NET_EM_RECORD_MODE_NORMAL.getValue();
        NET_OUT_SET_COURSE_RECORD_MODE stuOut = new NET_OUT_SET_COURSE_RECORD_MODE();

        boolean ret = SetCourseRecordMode(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("设置课程录像模式失败!");
            return;
        }
        System.out.println("设置课程录像模式成功!");
    }

    /**
     * 获取课程录像模式
     *
     * @param lLoginID 登录句柄
     */
    public void GetCourseRecordModeTest(NetSDKLib.LLong lLoginID) {

        NET_IN_GET_COURSE_RECORD_MODE stuIn = new NET_IN_GET_COURSE_RECORD_MODE();
        stuIn.nClassRoomID = 0;    // 房间号根本没启用, 固定写 0
        NET_OUT_GET_COURSE_RECORD_MODE stuOut = new NET_OUT_GET_COURSE_RECORD_MODE();

        boolean ret = GetCourseRecordMode(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("获取课程录像模式失败!");
            return;
        }
        System.out.println("获取课程录像模式成功!");
        System.out.println("当前教室的录像模式为: " + NET_EM_RECORD_MODE.getEnum(stuOut.emRecordMode).getNote());
    }

    /**
     * 开启/关闭录播
     * <p>
     * 首先必须注意，这里开启的通道指 真实通道 而非逻辑通道
     * 注意，只有 开启/关闭 状态的通道才能 关闭/开启
     *
     * @param lLoginID 登录句柄
     */
    public void SetCourseRecordStateTest(NetSDKLib.LLong lLoginID) {
        /**
         * 这里的用例是: 设置 逻辑通道 5 开启录播
         */
        /////////////////////// 先找到 逻辑通道5对应的真实通道 /////////////////////

        int channel = CourseRecordChannel.GetResourceChannel(lLoginID, 0);

        NET_IN_SET_COURSE_RECORD_STATE stuIn = new NET_IN_SET_COURSE_RECORD_STATE();
        stuIn.nAction = 0;          // 0：开启录播，1：关闭录播
        stuIn.nChannel = channel;
        NET_OUT_SET_COURSE_RECORD_STATE stuOut = new NET_OUT_SET_COURSE_RECORD_STATE();

        boolean ret = SetCourseRecordState(lLoginID, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("开启/关闭录播失败!");
            return;
        }
        System.out.println("开启/关闭录播成功!");
    }
}
