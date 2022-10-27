package com.netsdk.demo.accessControl.accessFaceQuality;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/8/26 20:29
 */
public class FaceQualityDemo {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    public static String encode;  // 编码格式

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    ////////////////////////////////////// 登录相关 ///////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登录句柄

    /**
     * login with high level 高安全级别登陆
     */
    public void loginWithHighLevel() {

        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstInParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();   // 输入结构体参数
        System.arraycopy(m_strIpAddr.getBytes(), 0, pstInParam.szIP, 0, m_strIpAddr.length());
        pstInParam.nPort = m_nPort;
        System.arraycopy(m_strUser.getBytes(), 0, pstInParam.szUserName, 0, m_strUser.length());
        System.arraycopy(m_strPassword.getBytes(), 0, pstInParam.szPassword, 0, m_strPassword.length());

        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输结构体参数

        // 登录并获取句柄
        m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstInParam, pstOutParam);

        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort, ToolKits.getErrorCode());
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("登录成功 句柄: " + m_hLoginHandle);
            System.out.println("设备地址：" + m_strIpAddr);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }
    }

    /**
     * logout 退出
     */
    public void logOut() {
        if (m_hLoginHandle.longValue() != 0) {
            netsdk.CLIENT_Logout(m_hLoginHandle);
            System.out.println("退出登录成功");
        }
    }

    //////////////////////////////////////// 智能事件 订阅/退订 //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    private NetSDKLib.LLong m_IVSAttachHandle = new NetSDKLib.LLong(0); // 智能订阅句柄

    private final NetSDKLib.fAnalyzerDataCallBack analyzerDataCB = FaceQualityAnalyzerCallBack.getSingleInstance();

    private int channel = 0;

    public void setChannelID() {
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入通道号:");
        this.channel = sc.nextInt();
    }

    public void AttachEventRealLoadPic() {

        this.DetachEventRealLoadPic();   // 先退订,部分设备不会对重复订阅作校验，重复订阅后会有重复的事件返回

        int bNeedPicture = 1;   // 需要图片

        // Demo里只模拟了一个通道的订阅所以把订阅句柄写成了全局, 如果订阅多个通道, 每个通道订阅后返回的句柄都不一样，请一一对应妥善保存
        m_IVSAttachHandle = netsdk.CLIENT_RealLoadPictureEx(m_hLoginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture, analyzerDataCB, null, null);
        if (m_IVSAttachHandle.longValue() != 0) {
            System.out.printf("Channel [%d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Channel [%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel, ToolKits.getErrorCode());
        }
    }

    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (m_IVSAttachHandle.longValue() != 0) {
            netsdk.CLIENT_StopLoadPic(m_IVSAttachHandle);
        }
    }

    //////////////////////////////////////// 报警事件 订阅/退订 //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    private final NetSDKLib.fMessCallBackEx1 messCallBackEx1 = FaceQualityMessageCallBack.getSingleInstance();

    /**
     * 订阅任务， start listen
     */
    public void AttachEventStartListen() {
        boolean bRet = netsdk.CLIENT_StartListenEx(m_hLoginHandle);
        if (bRet) {
            System.out.println("CLIENT_StartListenEx success.");
        } else {
            System.out.printf("CLIENT_StartListenEx fail, error:%s\n", ToolKits.getErrorCode());
        }
    }

    /**
     * 退订任务， stop listen
     */
    public void DetachEventStopListen() {
        boolean bRet = netsdk.CLIENT_StopListen(m_hLoginHandle);
        if (bRet) {
            System.out.println("CLIENT_StopListen success");
        } else {
            System.out.printf("CLIENT_StopListen fail, error:%s\n", ToolKits.getErrorCode());
        }
    }

    //////////////////////////////////////// 记录查询 ///////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 按卡号查询 刷卡记录 注意并不是所有设备都支持此条件查询
     */
    public void TestQueryRecordByNo() {
        final NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
        queryCondition.bCardNoEnable = 1;

        Scanner sc = new Scanner(System.in);
        System.out.println("请输入查询的卡号: ");
        final String cardNo = sc.next().trim();

        System.arraycopy(cardNo.getBytes(), 0, queryCondition.szCardNo, 0, cardNo.length());

        queryAccessRecord(queryCondition);
    }

    /**
     * 按时间查询刷卡记录 注意并不是所有设备都支持此条件查询
     */
    public void TestQueryRecordByTime() {
        final NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
        queryCondition.bTimeEnable = 1;

        Scanner sc = new Scanner(System.in);

        System.out.println("请输入起始时间(空格隔开): 年 月 日 时 分 秒");
        queryCondition.stStartTime.setTime(sc.nextInt(), sc.nextInt(), sc.nextInt(), sc.nextInt(), sc.nextInt(), sc.nextInt());

        System.out.println("请输入结束时间(空格隔开): 年 月 日 时 分 秒");
        queryCondition.stEndTime.setTime(sc.nextInt(), sc.nextInt(), sc.nextInt(), sc.nextInt(), sc.nextInt(), sc.nextInt());

        queryAccessRecord(queryCondition);
    }

    /**
     * 不按时间或卡号查询刷卡记录 即查询全部
     */
    public void TestQueryAllRecords() {

        final NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
        queryAccessRecord(queryCondition);

    }

    /**
     * 查询门禁刷卡记录
     */
    public void queryAccessRecord(NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX queryCondition) {

        if (queryCondition == null) {
            queryCondition = new NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
        }

        ///////////////////////////////////// FindRecord ///////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////

        /// CLIENT_FindRecord 入参
        NetSDKLib.NET_IN_FIND_RECORD_PARAM findRecordIn = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
        findRecordIn.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX; // 指定类型
        findRecordIn.pQueryCondition = queryCondition.getPointer(); // 这里写入搜索条件

        ///CLIENT_FindRecord出参
        NetSDKLib.NET_OUT_FIND_RECORD_PARAM findRecordOut = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();

        // 指针类型在调用前必须 write
        queryCondition.write();
        findRecordIn.write();
        findRecordOut.write();
        boolean success = netsdk.CLIENT_FindRecord(m_hLoginHandle, findRecordIn, findRecordOut, 5000);
        // 获取后指针类型需要 read
        findRecordOut.read();
        findRecordIn.read();
        queryCondition.read();

        if (!success) {
            System.err.println("Find Record failed: " + ToolKits.getErrorCode());
            return;
        }
        System.out.println("Find Record Succeed" + "\n" + "FindHandle :" + findRecordOut.lFindeHandle);

        ///////////////////////////////////// GetCount ///////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////

        int totalRecord = GetTotalRecordCount(findRecordOut.lFindeHandle);
        System.out.println("Total Record Count: " + totalRecord);

        if(totalRecord <= 0) {
            System.out.println("No Record Found.");
            FindRecordClose(findRecordOut.lFindeHandle);
            return;
        }

        ///////////////////////////////////// 是否展示数据 /////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("是否查看记录? Y/N");
            String input = sc.next();

            if (input.toUpperCase().equals("Y"))
                break;
            else if (input.toUpperCase().equals("N")) {
                FindRecordClose(findRecordOut.lFindeHandle);
                return;
            } else {
                System.out.println("输入不合法");
            }
        }

        ///////////////////////////////////// 如何展示数据 /////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////

        System.out.println("单次获取记录数量?( 小于等于0或大于总记录数 表示默认值 10)");
        int singleFetch = sc.nextInt();
        final int nRecordCount = (singleFetch > 0 && singleFetch <= totalRecord) ? singleFetch : 10;         // 单次获取数量

        int maxFetchTimes = (totalRecord / nRecordCount) + 1; // 最大获取次数
        System.out.println("最多获取几次数据?( 小于等于0或大于最大获取次数 表示不限次数全部获取)");
        int totalTimes = sc.nextInt();
        final int nFetchCount = (totalTimes > 0 && totalTimes <= maxFetchTimes) ? totalTimes : maxFetchTimes; // 总的获取数量

        ///门禁刷卡记录记录集信息
        NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[] records = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[nRecordCount];
        for (int i = 0; i < nRecordCount; i++) {
            records[i] = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC();
        }

        ///CLIENT_FindNextRecord 入参
        NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM findNextRecordIn = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
        findNextRecordIn.lFindeHandle = findRecordOut.lFindeHandle;
        findNextRecordIn.nFileCount = nRecordCount;  //想查询的记录条数

        ///CLIENT_FindNextRecord 出参
        NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM findNextRecordOut = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
        findNextRecordOut.nMaxRecordNum = nRecordCount;
        findNextRecordOut.pRecordList = new Memory(records[0].dwSize * nRecordCount);
        findNextRecordOut.pRecordList.clear(records[0].dwSize * nRecordCount);

        // 将  native 数据初始化
        ToolKits.SetStructArrToPointerData(records, findNextRecordOut.pRecordList);

        int count = 0;  //循环的次数
        int recordIndex = 0;

        while (count < nFetchCount) {      //循环查询

            if (!netsdk.CLIENT_FindNextRecord(findNextRecordIn, findNextRecordOut, 5000)) {
                System.err.println("FindNextRecord Failed: " + ToolKits.getErrorCode());
                break;
            }

            /// 将 native 数据转为 java 数据
            ToolKits.GetPointerDataToStructArr(findNextRecordOut.pRecordList, records);
            for (int i = 0; i < findNextRecordOut.nRetRecordNum; i++) {
                recordIndex = i + count * nRecordCount + 1;

                StringBuilder builder = new StringBuilder();
                try {
                    builder.append("<<----------------[").append(recordIndex).append("]---------------->>\n")
                            .append("刷卡时间:").append(records[i].stuTime.toStringTime()).append("\n")
                            .append("卡号:").append(new String(records[i].szCardNo).trim()).append("\n")
                            .append("卡类型:").append(records[i].emCardType).append("\n")
                            .append("门号:").append(records[i].nDoor).append("\n")
                            .append("密码:").append(new String(records[i].szPwd).trim()).append("\n")
                            .append("开门方式:").append(records[i].emMethod).append("\n")
                            .append("开门结果：").append(records[i].bStatus == 1 ? "成功" : "失败").append("\n")
                            .append("开门用户: ").append(new String(records[i].szUserID, encode).trim()).append("\n")
                            .append("开门失败原因错误码: ").append(records[i].nErrorCode).append("\n")
                            .append("考勤状态: ").append(records[i].emAttendanceState).append("\n")
                            .append("卡命名: ").append(new String(records[i].szCardName, encode).trim()).append("\n")
                            .append("身份证号: ").append(new String(records[i].szCitizenIDNo, encode).trim()).append("\n")
                            .append("人证比对结果: ").append(records[i].bCitizenIDResult == 0 ? "失败" : "成功").append("\n")
                            .append("性别: ").append((records[i].emSex == 1) ? "男" : (records[i].emSex == 2 ? "女" : "未知或未说明")).append("\n")
                            .append("姓名: ").append(new String(records[i].szCitizenIDName, encode).trim()).append("\n")
                            .append("住址:").append(new String(records[i].szCitizenIDAddress, encode).trim()).append("\n")
                            .append("签发机关: ").append(new String(records[i].szCitizenIDAuthority, encode).trim()).append("\n")
                            .append("起始日期: ").append(records[i].stuCitizenIDStart.toStringTime()).append("\n");

                    if (records[i].bIsEndless != 0) { // 是否长期有效
                        builder.append("截止日期: ").append("永久").append("\n");
                    } else {
                        builder.append("截止日期: ").append(records[i].stuCitizenIDEnd.toStringTime()).append("\n");
                    }
                    builder.append("人脸质量: ").append(records[i].nScore).append("\n")
                            .append("人脸图片地址: ").append(new String(records[i].szSnapFaceURL, encode).trim()).append("\n")
                            .append("身份证图片地址: ").append(new String(records[i].szCitizenPictureURL, encode).trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                System.out.println(builder.toString());
            }
            // 当 nRetRecordNum < nRecordCount 时说明已获取到最后一个批次
            if (findNextRecordOut.nRetRecordNum < nRecordCount || recordIndex == totalRecord) {
                break;
            }
            count++;
        }
        FindRecordClose(findRecordOut.lFindeHandle);
    }

    /**
     * 结束查找记录
     *
     * @param m_lFindHandle 查询句柄
     */
    private static void FindRecordClose(NetSDKLib.LLong m_lFindHandle) {
        boolean success;
        success = netsdk.CLIENT_FindRecordClose(m_lFindHandle);
        if (!success) {
            System.err.println("Failed to Close: " + String.format("0x%x", netsdk.CLIENT_GetLastError()));
        }
    }

    /**
     * 获取查询总记录条数
     *
     * @param findHandle 查询句柄
     * @return 记录数目
     */
    public static int GetTotalRecordCount(NetSDKLib.LLong findHandle) {
        // 入参
        NetSDKLib.NET_IN_QUEYT_RECORD_COUNT_PARAM paramIn = new NetSDKLib.NET_IN_QUEYT_RECORD_COUNT_PARAM();
        paramIn.lFindeHandle.setValue(findHandle.longValue());
        // 出参
        NetSDKLib.NET_OUT_QUEYT_RECORD_COUNT_PARAM paramOut = new NetSDKLib.NET_OUT_QUEYT_RECORD_COUNT_PARAM();

        boolean bRet = netsdk.CLIENT_QueryRecordCount(paramIn, paramOut, 3000);
        if (!bRet) {
            System.err.println("Can't FindNextRecord" + ToolKits.getErrorCode());
            return -1;
        }
        return paramOut.nRecordCount;
    }

    // 从远程下载图片
    public void DownloadRemotePic() {

        Scanner sc = new Scanner(System.in);
        System.out.println("远程地址：");
        String remotePath = sc.next();
        System.out.println("文件名: ");
        String saveName = sc.next();

        File path = new File("AccessFacePic/RemoteFile");
        if (!path.exists()) path.mkdir();

        NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE pInParam = new NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE();
        pInParam.pszFileName = new NativeString(remotePath).getPointer();
        pInParam.pszFileDst = new NativeString(path + "/" + saveName).getPointer();

        NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE pOutParam = new NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE();

        if (!netsdk.CLIENT_DownloadRemoteFile(m_hLoginHandle, pInParam, pOutParam, 3000)) {
            System.err.printf("CLIENT_DownloadRemoteFile failed, ErrCode=%s\n", ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_DownloadRemoteFile success");
        }
    }


    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {

        FaceQualityUtils.Init();         // 初始化SDK库
        netsdk.CLIENT_SetDVRMessCallBackEx1(messCallBackEx1, null);   // 注册一般事件回调函数
        this.loginWithHighLevel();       // 高安全登录
    }

    /**
     * 加载测试内容
     */
    public void RunTest() {

        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "智能事件订阅", "AttachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "智能事件退订", "DetachEventRealLoadPic"));
        menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
        // menu.addItem(new CaseMenu.Item(this, "报警事件订阅", "AttachEventStartListen"));
        // menu.addItem(new CaseMenu.Item(this, "报警事件退订", "DetachEventStopListen"));
        menu.addItem(new CaseMenu.Item(this, "按卡号查询刷卡记录", "TestQueryRecordByNo"));
        menu.addItem(new CaseMenu.Item(this, "按时间查询刷卡记录", "TestQueryRecordByTime"));
        menu.addItem(new CaseMenu.Item(this, "查询全部刷卡记录", "TestQueryAllRecords"));
        menu.addItem(new CaseMenu.Item(this, "下载设备图片", "DownloadRemotePic"));
        menu.run();

    }

    /**
     * 结束测试
     */
    public void EndTest() {

        System.out.println("End Test");
        this.logOut();  // 退出

        System.out.println("See You...");
        FaceQualityUtils.cleanAndExit();  // 清理资源并退出
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
//    private String m_strIpAddr = "10.34.3.63";
    private String m_strIpAddr = "172.5.9.98";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";
//    private String m_strPassword = "admin";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        /**
         * 本次需求要求增加的 “人脸质量” 字段涉及到 2个事件组和1个查询接口，分别是：
         * 门禁事件(智能)、认证比对事件(智能)、门禁记录查询。
         * Demo中 “智能事件订阅/退订” 包含 “门禁”、“认证比对” 的智能事件，由于是按通道订阅，所以还提供了 "选择通道" 菜单项。
         * Demo中 “按卡号/按时间/全部 查询” 包含了 “门禁记录查询” 的三种主要查询方式。
         * Demo 最后还提供了一个下载远程图片的接口，用于查看查询记录中的图片信息。
         *
         * 注意：sdk对普通 “门禁” 和 “人证比对” 事件（无图事件）也扩充了 “人脸质量” 字段，但这次的需求中并不涉及所以设备暂不支持。
         *      Demo 中这一部分相关示例请见 RunTest()中被注释的部分，以后如果有需要可以参考。
         */
        FaceQualityDemo demo = new FaceQualityDemo();

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
