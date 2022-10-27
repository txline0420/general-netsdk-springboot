package com.netsdk.demo.customize.courseRecord;

import com.netsdk.demo.customize.courseRecord.pojo.RealPreviewChannel;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.enumeration.NET_EM_RECORD_MODE;
import com.netsdk.lib.structure.*;
import com.sun.jna.NativeLong;

import java.io.UnsupportedEncodingException;

import static com.netsdk.demo.customize.courseRecord.modules.CourseConfigModule.*;
import static com.netsdk.demo.customize.courseRecord.modules.CourseRecordModule.*;
import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/10/12 11:36
 */
public class DemoConsoleRecordOperate {

    CourseRecordLogon courseRecordLogon = new CourseRecordLogon();

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 手动开启录像全过程
     * 这里的用例是依据配置开启 合成通道与资源通道
     * 如果设定是 电影 / 合成通道 模式，则不需要操作逻辑通道 0 以外的通道
     */
    public void CourseRecordManagerOpen() {

        //////////////////////// 第一步 使用 CourseRecordManager.getInfo ////////////////////
        //////////////////////// 获取哪些通道是需要录像 ////////////////////////////////////////
        /**
         * 当前设备数据库内记录着各个资源通道的录像需求，根据这个来判断哪些通道需要录像
         * 1 录像 2 不录像 0 无效
         */
        NET_IN_COURSERECORD_GETINFO stuRecordInfoIn = new NET_IN_COURSERECORD_GETINFO();
        stuRecordInfoIn.nClassRoomID = 0;   // 房间号根本没启用, 固定写 0
        NET_OUT_COURSERECORD_GETINFO stuRecordInfoOut = new NET_OUT_COURSERECORD_GETINFO();

        boolean ret1 = GetOperateCourseRecord(courseRecordLogon.m_hLoginHandle, stuRecordInfoIn, stuRecordInfoOut, 3000);
        if (!ret1) {
            System.err.println("获取教室录像信息失败!");
            return;
        }
        // 打印信息
        StringBuilder builder = new StringBuilder()
                .append("\n——————————————当前录像信息——————————————\n")
                .append("共有逻辑通道数: ").append(stuRecordInfoOut.nChannelNum).append("\n");
        for (int i = 0; i < stuRecordInfoOut.nChannelNum; i++) {
            // 0:无效,1:录像,2不录像,下标对应为逻辑通道号
            builder.append(String.format("通道[%2d]: ", i)).append(stuRecordInfoOut.nCanRecord[i]).append("\n");
        }
        System.out.println(builder.toString());

        //////////////////////////////// 第二部 获取录像模式并改为 course 模式 ///////////////////////////
        //////////////////////// CourseRecordManager.getMode //////////////////////////////////////////
        /**
         * 开启录像的时候需要将模式设置为 course，表明当前设备已经在录像。
         * 设备将录像信息记录到它的数据库，用于后面的录像查询。
         */
        NET_IN_GET_COURSE_RECORD_MODE stuGetRecordModeIn = new NET_IN_GET_COURSE_RECORD_MODE();
        stuGetRecordModeIn.nClassRoomID = 0;    // 房间号根本没启用, 固定写 0
        NET_OUT_GET_COURSE_RECORD_MODE stuGetRecordModeOut = new NET_OUT_GET_COURSE_RECORD_MODE();

        boolean ret2 = GetCourseRecordMode(courseRecordLogon.m_hLoginHandle, stuGetRecordModeIn, stuGetRecordModeOut, 3000);
        if (!ret2) {
            System.err.println("获取课程录像模式失败!");
            return;
        }
        System.out.println("获取课程录像模式成功!");
        System.out.println("当前教室的录像模式为: " + NET_EM_RECORD_MODE.getEnum(stuGetRecordModeOut.emRecordMode).getNote());

        //-> 一般录像前获取到的都是 Normal状态, 如果已经是 Course 模式则不需要再设置了
        if (!(stuGetRecordModeOut.emRecordMode == NET_EM_RECORD_MODE.NET_EM_RECORD_MODE_COURSE.getValue())) {
            NET_IN_SET_COURSE_RECORD_MODE stuSetRecordModeIn = new NET_IN_SET_COURSE_RECORD_MODE();
            stuSetRecordModeIn.nClassRoomID = 0;    // 房间号根本没启用, 固定写 0
            stuSetRecordModeIn.emRecordMode = NET_EM_RECORD_MODE.NET_EM_RECORD_MODE_COURSE.getValue();  // 改成课程录像模式
            NET_OUT_SET_COURSE_RECORD_MODE stuSetRecordModeOut = new NET_OUT_SET_COURSE_RECORD_MODE();

            boolean ret3 = SetCourseRecordMode(courseRecordLogon.m_hLoginHandle, stuSetRecordModeIn, stuSetRecordModeOut, 3000);
            if (!ret3) {
                System.err.println("设置课程录像模式失败!");
                return;
            }
            System.out.println("设置课程录像模式成功!");
        }

        ////////////////////////// 第三步 获取逻辑通道对应的真实通道 ////////////////////////////
        //////////////////////////// CourseChannelManager.getRealChannel ////////////////////
        /**
         * 录像是对真实通道录像，所以必须获得这些数据
         */
        RealPreviewChannel realPreviewChannel = CourseRecordChannel.GetRealPreviewChannels(courseRecordLogon.m_hLoginHandle);

        ///////////////////////// 第四部 根据是否需要录像 开启录像 ////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////
        // 反复调用 SetCourseRecordState 开启录制

        // 现在有定义的逻辑通道一共只有15个，这之后的必然是无效通道
        for (int i = 0; i < 15 && realPreviewChannel != null; i++) {

            int realChannel = realPreviewChannel.getRealPreviewChannels()[i];   // 得到真实通道

            if (stuRecordInfoOut.nCanRecord[i] == 1 && realChannel != -1) {  // 需要录像
                NET_IN_SET_COURSE_RECORD_STATE stuSetCourseIn = new NET_IN_SET_COURSE_RECORD_STATE();
                stuSetCourseIn.nAction = 0;          // 0：开启录播，1：关闭录播
                stuSetCourseIn.nChannel = realChannel;
                NET_OUT_SET_COURSE_RECORD_STATE stuSetCourseOut = new NET_OUT_SET_COURSE_RECORD_STATE();

                boolean ret = SetCourseRecordState(courseRecordLogon.m_hLoginHandle, stuSetCourseIn, stuSetCourseOut, 3000);
                if (!ret) {
                    System.err.println(String.format("开启录播失败! 逻辑通道[%2d]->真实通道[%2d]", i, realChannel));
                    continue;
                }
                System.out.println(String.format("开启录播成功! 逻辑通道[%2d]->真实通道[%2d]", i, realChannel));
            }
        }
    }

    /**
     * 暂停录像全过程
     * （前提是已经开启录像） 暂停只要相应的把所有刚才开启的通道关闭录像
     * 暂停和停止用的其实是同一个接口，区别是暂停是，录像模式保持 Course 状态， 而停止则是设置回 Normal
     */
    public void CourseRecordManagerSuspend() {


        /////////////// 重新获取一下录像配置和 逻辑->真实 通道对应关系

        NET_IN_COURSERECORD_GETINFO stuRecordInfoIn = new NET_IN_COURSERECORD_GETINFO();
        stuRecordInfoIn.nClassRoomID = 0;   // 房间号根本没启用, 固定写 0
        NET_OUT_COURSERECORD_GETINFO stuRecordInfoOut = new NET_OUT_COURSERECORD_GETINFO();

        boolean ret1 = GetOperateCourseRecord(courseRecordLogon.m_hLoginHandle, stuRecordInfoIn, stuRecordInfoOut, 3000);
        if (!ret1) {
            System.err.println("获取教室录像信息失败!");
            return;
        }

        // 获取逻辑通道对应的真实通道
        RealPreviewChannel realPreviewChannel = CourseRecordChannel.GetRealPreviewChannels(courseRecordLogon.m_hLoginHandle);

        /////////////////////////////////////// 暂停录像 ///////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////
        // 反复调用 SetCourseRecordState 暂停录制

        // 现在有定义的逻辑通道一共只有15个，这之后的必然是无效通道
        for (int i = 0; i < 15 && realPreviewChannel != null; i++) {

            int realChannel = realPreviewChannel.getRealPreviewChannels()[i];   // 得到真实通道

            if (stuRecordInfoOut.nCanRecord[i] == 1 && realChannel != -1) {  // 需要录像
                NET_IN_SET_COURSE_RECORD_STATE stuSetCourseIn = new NET_IN_SET_COURSE_RECORD_STATE();
                stuSetCourseIn.nAction = 1;          // 0：开启录播，1：关闭录播（暂停）
                stuSetCourseIn.nChannel = realChannel;
                NET_OUT_SET_COURSE_RECORD_STATE stuSetCourseOut = new NET_OUT_SET_COURSE_RECORD_STATE();

                boolean ret = SetCourseRecordState(courseRecordLogon.m_hLoginHandle, stuSetCourseIn, stuSetCourseOut, 5000);
                if (!ret) {
                    System.err.println(String.format("暂停录播失败! 逻辑通道[%2d]->真实通道[%2d]", i, realChannel));
                    continue;
                }
                System.out.println(String.format("暂停录播成功! 逻辑通道[%2d]->真实通道[%2d]", i, realChannel));
            }
        }
    }

    /**
     * 关闭录像全过程
     * （前提是已经开启录像） 关闭需要相应的把所有刚才开启的通道关闭录像并设置录像模式为 Normal
     * 暂停和停止用的其实是同一个接口，区别是暂停是，录像模式保持 Course 状态， 而停止则是设置回 Normal
     */
    public void CourseRecordManagerStop() {


        /////////////// 重新获取一下录像配置和 逻辑->真实 通道对应关系

        NET_IN_COURSERECORD_GETINFO stuRecordInfoIn = new NET_IN_COURSERECORD_GETINFO();
        stuRecordInfoIn.nClassRoomID = 0;   // 房间号根本没启用, 固定写 0
        NET_OUT_COURSERECORD_GETINFO stuRecordInfoOut = new NET_OUT_COURSERECORD_GETINFO();

        boolean ret1 = GetOperateCourseRecord(courseRecordLogon.m_hLoginHandle, stuRecordInfoIn, stuRecordInfoOut, 5000);
        if (!ret1) {
            System.err.println("获取教室录像信息失败!");
            return;
        }

        // 获取逻辑通道对应的真实通道
        RealPreviewChannel realPreviewChannel = CourseRecordChannel.GetRealPreviewChannels(courseRecordLogon.m_hLoginHandle);

        /////////////////////////////////////// 暂停录像 ///////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////
        // 反复调用 SetCourseRecordState 暂停录制

        // 现在有定义的逻辑通道一共只有15个，这之后的必然是无效通道
        for (int i = 0; i < 15 && realPreviewChannel != null; i++) {

            int realChannel = realPreviewChannel.getRealPreviewChannels()[i];   // 得到真实通道

            if (stuRecordInfoOut.nCanRecord[i] == 1 && realChannel != -1) {  // 需要录像
                NET_IN_SET_COURSE_RECORD_STATE stuSetCourseIn = new NET_IN_SET_COURSE_RECORD_STATE();
                stuSetCourseIn.nAction = 1;          // 0：开启录播，1：关闭录播（暂停）
                stuSetCourseIn.nChannel = realChannel;
                NET_OUT_SET_COURSE_RECORD_STATE stuSetCourseOut = new NET_OUT_SET_COURSE_RECORD_STATE();

                boolean ret = SetCourseRecordState(courseRecordLogon.m_hLoginHandle, stuSetCourseIn, stuSetCourseOut, 5000);
                if (!ret) {
                    System.err.println(String.format("停止录播失败! 逻辑通道[%2d]->真实通道[%2d]", i, realChannel));
                    continue;
                }
                System.out.println(String.format("停止录播成功! 逻辑通道[%2d]->真实通道[%2d]", i, realChannel));
            }
        }
        //////////////////////////////////// 设置状态回 Normal //////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////

        NET_IN_SET_COURSE_RECORD_MODE stuSetRecordModeIn = new NET_IN_SET_COURSE_RECORD_MODE();
        stuSetRecordModeIn.nClassRoomID = 0;    // 房间号根本没启用, 固定写 0
        stuSetRecordModeIn.emRecordMode = NET_EM_RECORD_MODE.NET_EM_RECORD_MODE_NORMAL.getValue();  // 改成课程录像模式
        NET_OUT_SET_COURSE_RECORD_MODE stuSetRecordModeOut = new NET_OUT_SET_COURSE_RECORD_MODE();

        boolean ret3 = SetCourseRecordMode(courseRecordLogon.m_hLoginHandle, stuSetRecordModeIn, stuSetRecordModeOut, 3000);
        if (!ret3) {
            System.err.println("设置课程录像模式失败!");
            return;
        }
        System.out.println("设置课程录像模式成功!");
    }


    /**
     * 查看录像状态
     * 设置了录像开启并不意味着录像状态不会变化（例如磁盘坏了、满了等意外）
     * 需要通过这个接口查看各通道的录像状态
     */
    public void CourseRecordManagerGetState() {

        /////////////// 重新获取一下录像配置和 逻辑->真实 通道对应关系

        NET_IN_COURSERECORD_GETINFO stuRecordInfoIn = new NET_IN_COURSERECORD_GETINFO();
        stuRecordInfoIn.nClassRoomID = 0;   // 房间号根本没启用, 固定写 0
        NET_OUT_COURSERECORD_GETINFO stuRecordInfoOut = new NET_OUT_COURSERECORD_GETINFO();

        boolean ret1 = GetOperateCourseRecord(courseRecordLogon.m_hLoginHandle, stuRecordInfoIn, stuRecordInfoOut, 3000);
        if (!ret1) {
            System.err.println("获取教室录像信息失败!");
            return;
        }

        // 获取逻辑通道对应的真实通道
        RealPreviewChannel realPreviewChannel = CourseRecordChannel.GetRealPreviewChannels(courseRecordLogon.m_hLoginHandle);

        /////////////////////////////////////// 获取录像状态 ///////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////

        // 现在有定义的逻辑通道一共只有15个，这之后的必然是无效通道
        for (int i = 0; i < 15 && realPreviewChannel != null; i++) {

            int realChannel = realPreviewChannel.getRealPreviewChannels()[i];   // 得到真实通道

            if (stuRecordInfoOut.nCanRecord[i] == 1 && realChannel != -1) {  // 需要录像
                NET_IN_GET_RECORD_STATE stuGetStateIn = new NET_IN_GET_RECORD_STATE();
                stuGetStateIn.nChannel = realChannel;
                NET_OUT_GET_RECORD_STATE stuGetStateOut = new NET_OUT_GET_RECORD_STATE();

                boolean ret = GetCourseRecordState(courseRecordLogon.m_hLoginHandle, stuGetStateIn, stuGetStateOut, 3000);
                if (!ret) {
                    System.err.println(String.format("获取录制状态失败! 逻辑通道[%2d]->真实通道[%2d]", i, realChannel));
                    continue;
                }
                System.out.println(String.format("获取录制状态成功! 逻辑通道[%2d]->真实通道[%2d], 状态: %s", i, realChannel, stuGetStateOut.bState == 0 ? "不录制" : "录制"));
            }
        }
    }

    /**
     * 获取合成通道模式
     */
    public void GetCourseRecordCompositeMode() {

        NET_IN_COURSECOMPOSITE_CHANNEL_MODE_GET stuCompositeModeIn = new NET_IN_COURSECOMPOSITE_CHANNEL_MODE_GET();
        stuCompositeModeIn.nCount = 10;
        NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_GET stuCompositeModeOut = new NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_GET();

        boolean ret = GetCourseCompositeMode(courseRecordLogon.m_hLoginHandle, stuCompositeModeIn, stuCompositeModeOut, 3000);
        if (!ret) {
            System.err.println("获取合成通道模式配置失败");
            return;
        }
        int retNum = stuCompositeModeOut.nReturnNum;  // 实际返回个数

        StringBuilder builder = new StringBuilder()
                .append(String.format("///////————————————>共获取到:%2d 条配置\n", retNum));
        for (int i = 0; i < retNum; i++) {
            try {
                builder.append("//——————————编号: ").append(stuCompositeModeOut.nMode[i]).append("——————————————\n")
                        .append("名称: ").append(new String(stuCompositeModeOut.stModeInfo[i].szName, encode)).append("\n")
                        .append("共有窗体数: ").append(stuCompositeModeOut.stModeInfo[i].nWindowNum).append("\n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            for (int j = 0; j < stuCompositeModeOut.stModeInfo[i].nWindowNum; j++) {
                builder.append("[窗体: ").append(j).append("详情]:\n")
                        .append("逻辑通道 nLogicChannel: ").append(stuCompositeModeOut.stModeInfo[i].stWindows[j].nLogicChannel).append("\n")
                        .append("Z序 nZOrder").append(stuCompositeModeOut.stModeInfo[i].stWindows[j].nZOrder).append("\n")
                        .append("8192坐标体系").append(String.format("left->%4d : right->%4d : top->%4d : button->%4d",
                        stuCompositeModeOut.stModeInfo[i].stWindows[j].stRect.left.longValue(),
                        stuCompositeModeOut.stModeInfo[i].stWindows[j].stRect.right.longValue(),
                        stuCompositeModeOut.stModeInfo[i].stWindows[j].stRect.top.longValue(),
                        stuCompositeModeOut.stModeInfo[i].stWindows[j].stRect.bottom.longValue())).append("\n");
            }
        }

        System.out.println(builder.toString());
    }

    /**
     * 添加新的合成通道组合配置
     */
    public void AddCourseRecordCompositeMode() {
        /**
         * szName: 测试合成通道组合自定义配置
         *
         *****************************************
         *  logic5          *       logic1
         *                  *
         *************(4096,4096)*****************
         *  logic4          *       logic6
         *                  *
         **********************************(8192,8192)
         */
        NET_IN_COURSECOMPOSITE_CHANNEL_MODE_ADD stuCompositeModeIn = new NET_IN_COURSECOMPOSITE_CHANNEL_MODE_ADD();
        stuCompositeModeIn.nCount = 1;
        byte[] name = new byte[0];
        try {
            name = "测试合成通道组合自定义配置".getBytes(encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.arraycopy(name, 0, stuCompositeModeIn.stModeInfo[0].szName, 0, name.length);
        stuCompositeModeIn.stModeInfo[0].nWindowNum = 4;

        /**
         {
         *                             "logicChannel": 5,
         *                             "rect": [
         *                                 0,
         *                                 0,
         *                                 4096,
         *                                 4096
         *                             ],
         *                             "zOrder": 0
         *                         }
         */
        stuCompositeModeIn.stModeInfo[0].stWindows[0].nZOrder = 0;
        stuCompositeModeIn.stModeInfo[0].stWindows[0].nLogicChannel = 5;
        stuCompositeModeIn.stModeInfo[0].stWindows[0].stRect.left = new NativeLong(0);
        stuCompositeModeIn.stModeInfo[0].stWindows[0].stRect.top = new NativeLong(0);
        stuCompositeModeIn.stModeInfo[0].stWindows[0].stRect.right = new NativeLong(4096);
        stuCompositeModeIn.stModeInfo[0].stWindows[0].stRect.bottom = new NativeLong(4096);
        /**
         {
         *                             "logicChannel": 4,
         *                             "rect": [
         *                                 0,
         *                                 4096,
         *                                 4096,
         *                                 8192
         *                             ],
         *                             "zOrder": 0
         *                         },
         */
        stuCompositeModeIn.stModeInfo[0].stWindows[1].nZOrder = 0;
        stuCompositeModeIn.stModeInfo[0].stWindows[1].nLogicChannel = 4;
        stuCompositeModeIn.stModeInfo[0].stWindows[1].stRect.left = new NativeLong(0);
        stuCompositeModeIn.stModeInfo[0].stWindows[1].stRect.top = new NativeLong(4096);
        stuCompositeModeIn.stModeInfo[0].stWindows[1].stRect.right = new NativeLong(4096);
        stuCompositeModeIn.stModeInfo[0].stWindows[1].stRect.bottom = new NativeLong(8192);
        /**
         {
         *                             "logicChannel": 1,
         *                             "rect": [
         *                                 4096,
         *                                 0,
         *                                 8192,
         *                                 4096
         *                             ],
         *                             "zOrder": 0
         *                         }
         */
        stuCompositeModeIn.stModeInfo[0].stWindows[2].nZOrder = 0;
        stuCompositeModeIn.stModeInfo[0].stWindows[2].nLogicChannel = 1;
        stuCompositeModeIn.stModeInfo[0].stWindows[2].stRect.left = new NativeLong(4096);
        stuCompositeModeIn.stModeInfo[0].stWindows[2].stRect.top = new NativeLong(0);
        stuCompositeModeIn.stModeInfo[0].stWindows[2].stRect.right = new NativeLong(8192);
        stuCompositeModeIn.stModeInfo[0].stWindows[2].stRect.bottom = new NativeLong(4096);
        /**
         {
         *                             "logicChannel": 6,
         *                             "rect": [
         *                                 4096,
         *                                 4096,
         *                                 8192,
         *                                 8192
         *                             ],
         *                             "zOrder": 0
         *                         }
         */
        stuCompositeModeIn.stModeInfo[0].stWindows[3].nZOrder = 0;
        stuCompositeModeIn.stModeInfo[0].stWindows[3].nLogicChannel = 6;
        stuCompositeModeIn.stModeInfo[0].stWindows[3].stRect.left = new NativeLong(4096);
        stuCompositeModeIn.stModeInfo[0].stWindows[3].stRect.top = new NativeLong(4096);
        stuCompositeModeIn.stModeInfo[0].stWindows[3].stRect.right = new NativeLong(8192);
        stuCompositeModeIn.stModeInfo[0].stWindows[3].stRect.bottom = new NativeLong(8192);

        //////////////////////////////////// stuOut //////////////////////////////////////

        NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_ADD stuCompositeModeOut = new NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_ADD();

        boolean ret = AddCourseCompositeMode(courseRecordLogon.m_hLoginHandle, stuCompositeModeIn, stuCompositeModeOut, 3000);
        if (!ret) {
            System.err.println("添加通道模式配置失败");
            return;
        }
        int retCount = stuCompositeModeOut.nCount;
        System.out.println("添加通道模式配置成功, 成功添加配置 " + retCount + "条配置");
        for (int i = 0; i < retCount; i++) {
            // 返回码, 1 成功, 2 失败, 3 已满, 4 资源不足
            System.out.println(String.format("第[%2d]条配置返回码: %d", i + 1, stuCompositeModeOut.stResult[i].nReturnCode));
        }
    }

    /**
     * 修改合成通道组合配置
     */
    public void ModifyCourseRecordCompositeMode() {
        /**
         * 修改特定编号的合成通道组合配置 nMode，数据可以通过查询接口获得
         *
         * 比如这里我假设上面添加后的配置实际 编号为-4，我通过查询获取到它的原先配置，然后修改其中一个窗口的逻辑通道号，再重新下发
         */
        ///////////////////////////////////// 先获取配置 //////////////////////////////////////////////////////////

        NET_IN_COURSECOMPOSITE_CHANNEL_MODE_GET stuCompositeModeIn = new NET_IN_COURSECOMPOSITE_CHANNEL_MODE_GET();
        stuCompositeModeIn.nCount = 10;
        NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_GET stuCompositeModeOut = new NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_GET();

        boolean ret = GetCourseCompositeMode(courseRecordLogon.m_hLoginHandle, stuCompositeModeIn, stuCompositeModeOut, 3000);
        if (!ret) {
            System.err.println("获取合成通道模式配置失败");
            return;
        }
        int retNum = stuCompositeModeOut.nReturnNum;  // 实际返回个数

        // 找到编号为 -4 的合成配置
        NET_COMPOSITECHANNELMODE_INFO modifyTarget = new NET_COMPOSITECHANNELMODE_INFO();

        for (int i = 0; i < retNum; i++) {
            if (stuCompositeModeOut.nMode[i] == -4) {
                modifyTarget = stuCompositeModeOut.stModeInfo[i];
            }
        }

        //////////////////////////////////////////// 修改配置并下发 //////////////////////////////////////////////////////

        NET_IN_COURSECOMPOSITE_CHANNEL_MODE_MODIFY stuCompositeModifyIn = new NET_IN_COURSECOMPOSITE_CHANNEL_MODE_MODIFY();
        stuCompositeModifyIn.nModeNum = 1;  // 只修改一条配置
        stuCompositeModifyIn.nMode[0] = -4;                                  // 针对编号-4修改
        stuCompositeModifyIn.stModeInfo[0] = modifyTarget;                  // 先赋予原先的配置
        stuCompositeModifyIn.stModeInfo[0].stWindows[0].nLogicChannel = 3;  // 修改第一个窗口的逻辑通道为 3

        NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_MODIFY stuCompositeModifyOut = new NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_MODIFY();

        boolean ret2 = ModifyCourseCompositeMode(courseRecordLogon.m_hLoginHandle, stuCompositeModifyIn, stuCompositeModifyOut, 3000);
        if (!ret2) {
            System.err.println("修改合成通道模式配置失败");
            return;
        }
        int retNum2 = stuCompositeModifyOut.nReturnNum;
        System.out.println("修改合成通道模式配置成功, 共返回 " + retNum2 + " 条结果");
        for (int i = 0; i < retNum2; i++) {
            // 返回码: 1 成功, 2 失败, 3, 已满, 4 资源不足
            System.out.println(String.format("第[%2d]条配置返回码: %d", i + 1, stuCompositeModifyOut.nReturnCode[i]));
        }
    }

    /**
     * 删除合成通道组合配置
     */
    public void DeleteCourseRecordCompositeMode() {
        /**
         * 这里假设存在编号为 -4 的合成通道组合配置，删除它
         */
        NET_IN_COURSECOMPOSITE_CHANNEL_MODE_DELETE stuCompositeModeIn = new NET_IN_COURSECOMPOSITE_CHANNEL_MODE_DELETE();
        stuCompositeModeIn.nModeNum = 1;
        stuCompositeModeIn.nMode[0] = -4;
        NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_DELETE stuCompositeModeOut = new NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_DELETE();
        boolean ret = DeleteCourseCompositeMode(courseRecordLogon.m_hLoginHandle, stuCompositeModeIn, stuCompositeModeOut, 3000);
        if (!ret) {
            System.err.println("删除合成通道模式配置失败");
            return;
        }
        int retNum = stuCompositeModeOut.nReturnNum;
        System.out.println("删除合成通道模式配置成功, 共返回 " + retNum + " 条结果");
        for (int i = 0; i < retNum; i++) {
            // 返回码: 1 成功, 2 失败
            System.out.println(String.format("第[%2d]条配置返回码: %d", i + 1, stuCompositeModeOut.nReturnCode[i]));
        }
    }


    /**
     * 设置组合通道模式
     * 上面创建的自定义合成通道组合配置(网页上配置也一样)，获取到它的 nMode 值后，可以用于配置合成通道的样式
     */
    public void SetInfoCourseRecordCompositeChannel() {
        /**
         * 这里的用例是: 先获取到原先的组合通道信息，再把合成通道设置为 自定义模式 -4, 课程名称 "手动课程"
         */
        ////////////////////// 获取原先的合成通道信息 //////////////////////

        NET_IN_COURSECOMPOSITE_GET_INFO stuInGet = new NET_IN_COURSECOMPOSITE_GET_INFO();
        stuInGet.nClassRoomId = 0;    // 房间号根本没启用, 固定写 0
        NET_OUT_COURSECOMPOSITE_GET_INFO stuOutGet = new NET_OUT_COURSECOMPOSITE_GET_INFO();

        boolean ret1 = GetInfoOperateCourseCompositeChannel(courseRecordLogon.m_hLoginHandle, stuInGet, stuOutGet, 3000);
        if (!ret1) {
            System.err.println("获取组合通道信息失败!");
            return;
        }
        System.out.println("获取组合通道信息成功!");

        ////////////////////// 设置新的合成通道信息 ///////////////////////

        NET_IN_COURSECOMPOSITE_SET_INFO stuInSet = new NET_IN_COURSECOMPOSITE_SET_INFO();
        stuInSet.nClassRoomId = 0;                                 // 房间号根本没启用, 固定写 0
        stuInSet.stuChannelInfo = stuOutGet.stuChannelInfo;        // 使用获取到的合成通道信息
        stuInSet.stuChannelInfo.nCompositeChannelMode = -4;        // 用户自定义 编号 -4 模式 (前提是存在这个自定义模式)
        byte[] newCourseName = new byte[0];
        try {
            newCourseName = "手动课程".getBytes(encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.arraycopy(newCourseName, 0, stuInSet.stuChannelInfo.szCourseName, 0, newCourseName.length);
        NET_OUT_COURSECOMPOSITE_SET_INFO stuOutSet = new NET_OUT_COURSECOMPOSITE_SET_INFO();

        boolean ret2 = SetInfoOperateCourseCompositeChannel(courseRecordLogon.m_hLoginHandle, stuInSet, stuOutSet, 3000);
        if (!ret2) {
            System.err.println("设置组合通道信息失败失败!");
            return;
        }
        System.out.println("设置组合通道信息成功! 当前模式: " + -4);
    }

    /**
     * 获取当前课程教室已录制时间
     */
    public void GetTimeOperateCourseRecordInfo() {
        NET_IN_COURSERECORD_GET_TIME stuIn = new NET_IN_COURSERECORD_GET_TIME();
        stuIn.nClassRoomID = 0;    // 房间号根本没启用, 固定写 0
        NET_OUT_COURSERECORD_GET_TIME stuOut = new NET_OUT_COURSERECORD_GET_TIME();

        boolean ret = GetTimeOperateCourseRecord(courseRecordLogon.m_hLoginHandle, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("获取当前课程教室已录制时间失败!");
            return;
        }
        System.out.println("获取当前课程教室已录制时间成功!");
        System.out.println("已录制时间(秒): " + stuOut.nTime);
    }

    //////////////////////////////////////////////// 简易控制台 /////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // 初始化测试
    public void InitTest() {

        CourseRecordInit.Init();                 // 初始化SDK库
        courseRecordLogon.m_strIpAddr = m_strIpAddr;
        courseRecordLogon.m_nPort = m_nPort;
        courseRecordLogon.m_strUser = m_strUser;
        courseRecordLogon.m_strPassword = m_strPassword;
        courseRecordLogon.loginWithHighLevel();   // 高安全登录
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "更换当前合成通道模式测试", "SetInfoCourseRecordCompositeChannel"));
        menu.addItem(new CaseMenu.Item(this, "手动开启录像测试", "CourseRecordManagerOpen"));
        menu.addItem(new CaseMenu.Item(this, "手动暂停录像测试", "CourseRecordManagerSuspend"));
        menu.addItem(new CaseMenu.Item(this, "手动停止录像测试", "CourseRecordManagerStop"));
        menu.addItem(new CaseMenu.Item(this, "查看录像状态测试", "CourseRecordManagerGetState"));
        menu.addItem(new CaseMenu.Item(this, "获取当前课程教室已录制时间", "GetTimeOperateCourseRecordInfo"));

        menu.addItem(new CaseMenu.Item(this, "获取合成通道模式测试", "GetCourseRecordCompositeMode"));
        menu.addItem(new CaseMenu.Item(this, "添加合成通道模式测试", "AddCourseRecordCompositeMode"));
        menu.addItem(new CaseMenu.Item(this, "修改合成通道模式测试", "ModifyCourseRecordCompositeMode"));
        menu.addItem(new CaseMenu.Item(this, "删除合成通道模式测试", "DeleteCourseRecordCompositeMode"));
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        courseRecordLogon.logOut();  // 退出
        System.out.println("See You...");
        CourseRecordInit.CleanAndExit();  // 清理资源并退出
    }


    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    private String m_strIpAddr = "172.8.3.137";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin1234";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        DemoConsoleRecordOperate demo = new DemoConsoleRecordOperate();

        if (args.length == 4) {
            demo.m_strIpAddr = args[0];
            demo.m_nPort = Integer.parseInt(args[1]);
            demo.m_strUser = args[2];
            demo.m_strPassword = args[3];
        }

        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
