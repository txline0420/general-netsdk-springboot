package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_IPC_TYPE;
import com.netsdk.lib.enumeration.NET_SPLIT_OPERATE_TYPE;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Scanner;

import static com.netsdk.lib.NetSDKLib.*;
import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 视频上墙
 * 注意：在获取窗口号以及视频上墙时，如果用融合屏的通道，都需要重新查一遍
 */
public class MonitorWallNew {
    static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux")) {
            encode = "UTF-8";
        }
    }

    private final NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
    private LLong loginHandle = new LLong(0);   //登陆句柄

    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    public static class fDisConnectCB implements fDisConnect {
        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
        }
    }

    // 网络连接恢复，设备重连成功回调
    // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
    public static class HaveReConnect implements fHaveReConnect {
        public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
        }
    }

    private final fDisConnectCB m_DisConnectCB = new fDisConnectCB();
    private final HaveReConnect haveReConnect = new HaveReConnect();

    public void EndTest() {
        System.out.println("End Test");
        if (loginHandle.longValue() != 0) {
            netsdkApi.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netsdkApi.CLIENT_Cleanup();
        System.exit(0);
    }

    public void InitTest() {
        //初始化SDK库
        netsdkApi.CLIENT_Init(m_DisConnectCB, null);

        // 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
        // 此操作为可选操作，但建议用户进行设置
        netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 5000; //登录请求响应超时时间设置为5S
        int tryTimes = 3;    //登录时尝试建立链接3次
        netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);

        // 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NET_PARAM netParam = new NET_PARAM();
        netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
        netsdkApi.CLIENT_SetNetworkParam(netParam);

        // 打开日志，可选
        LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();

        File path = new File(".");
        String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\" + System.currentTimeMillis() + ".log";

        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        boolean bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
        if (!bLogopen) {
            System.err.println("Failed to open NetSDK log !!!");
        }

        // 向设备登入,登陆M70
        int nSpecCap = 0;
        Pointer pCapParam = null;
        IntByReference nError = new IntByReference(0);
        loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
                m_strPassword, nSpecCap, pCapParam, deviceinfo, nError);

        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Port[%d]  channels[%d] Success!\n", m_strIp, m_nPort,deviceinfo.byChanNum);
        } else {
            System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%x]\n", m_strIp, m_nPort, netsdkApi.CLIENT_GetLastError());
            EndTest();
        }
    }

    /////////////////////////////////////////// 电视墙RTSP推流命令 ////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 输出通道中的每个窗口都可以设置一个视频输入通道, 使视频在输出通道的对应位置输出.
     */
    public void SetSplitSourceByRTSP() throws UnsupportedEncodingException {

        boolean bRet;

        NET_SPLIT_SOURCE stSplitSource = new NET_SPLIT_SOURCE();
        stSplitSource.bEnable = 1;              // true 使能

        Scanner sc = new Scanner(System.in);

        System.out.println("请输入视频源设备IP");
        byte[] szIp = sc.next().trim().getBytes();
        System.arraycopy(szIp, 0, stSplitSource.szIp, 0, szIp.length);

        System.out.println("请输入视频源登录用户名");
        byte[] szUserEx = sc.next().trim().getBytes();
        System.arraycopy(szUserEx, 0, stSplitSource.szUserEx, 0, szUserEx.length);

        System.out.println("请输入视频源登录密码");
        byte[] szPwdEx = sc.next().trim().getBytes();
        System.arraycopy(szPwdEx, 0, stSplitSource.szPwdEx, 0, szPwdEx.length);

        // 视频源协议类型 自定义
        stSplitSource.emProtocol = NET_DEVICE_PROTOCOL.NET_PROTOCOL_OTHER;

        // 视频源设备类型 自定义
        stSplitSource.byManuFactory = (byte) EM_IPC_TYPE.NET_IPC_OTHER.getValue();

        // 视频源码流类型 主码流
        stSplitSource.nStreamType = 0;

        // RTSP 端口号 默认554
        stSplitSource.dwRtspPort = 554;

        // 主码流 RTSP url 地址
        System.out.println("请输入视频源主码流的RTSP地址");
        /* 格式举例:
         * rtsp://admin:admin123@10.11.16.71:554/cam/realmonitor?channel=1&subtype=0
         * rtsp://<用户名>:<密码>@<IP地址>:<端口>/cam/realmonitor?channel=1&subtype=0
         * 端口：一般是554；通道, 1-128 （和sdk不同，从1开始的，sdk从0开始）; subtype: 码流类型 0 指主码流. 
         */
        byte[] szMainStreamUrl = sc.next().trim().getBytes();
        System.arraycopy(szMainStreamUrl, 0, stSplitSource.szMainStreamUrl, 0, szMainStreamUrl.length);

        // 获取解码器视频输出通道号
        GetChannelInfo();

        System.out.println("请输入输出通道号");
        int nOutChannel = sc.nextInt();

        // 获取nOutChannel上的窗口数量
        int nWndsCount = GetWndsCount(loginHandle, nOutChannel);
        if (0 == nWndsCount) {
            System.err.println("当前输出通道上没有窗口");
            return;
        }

        System.out.println(String.format("请指定该输出通道上的窗口号[0 ~ %d]", nWndsCount - 1));
        int nWndsID = sc.nextInt();
        if (nWndsID < 0 || nWndsID >= nWndsCount) {
            System.err.println("输入的窗口号有误");
            return;
        }

        // 设置视频源
        bRet = netsdkApi.CLIENT_SetSplitSource(loginHandle, nOutChannel, nWndsID, stSplitSource, 1, 5000);
        if (!bRet) {
            System.err.println("CLIENT_SetSplitSource failed: " + ToolKits.getErrorCode());
            return;
        }

        System.out.println("CLIENT_SetSplitSource succeed");
    }

    // 测试 获取解码器视频输出通道号
    public void TestGetChannelInfo() throws UnsupportedEncodingException {
        GetChannelInfo();
    }

    // 获取解码器视频输出通道号
    public void GetChannelInfo() throws UnsupportedEncodingException {

        ///////////////////////////////////// 总物理输出通道数(控制编号) ////////////////////////////

        GetTotalVideoOutChannel();
        System.out.println("——————————————————————————————————————————");

        ///////////////////////////////////// 在用的物理输出通道(控制编号) //////////////////////////

        GetOnUseVideoOutChannel();
        System.out.println("——————————————————————————————————————————");

        ///////////////////////////////////// 各个屏幕的虚拟通道号 /////////////////////////////////

        GetVirtualChannel();
        System.out.println("——————————————————————————————————————————");

        //////////////////////////////////// 各个屏幕的物理输出通道(控制编号) ///////////////////////

        GetMonitorWallDetail();
    }

    // 总物理输出通道数(控制编号)
    private boolean GetTotalVideoOutChannel() {
        boolean bRet;
        NET_PRODUCTION_DEFNITION stuDef = new NET_PRODUCTION_DEFNITION();
        bRet = netsdkApi.CLIENT_QueryProductionDefinition(loginHandle, stuDef, 5000);
        if (!bRet) {
            System.err.println("CLIENT_QueryProductionDefinition failed. ErrorCode:" + ToolKits.getErrorCode());
            return true;
        }

        System.out.println("视频输出物理通道总数: " + stuDef.nVideoOutChannel);
        return false;
    }

    // 在用的物理输出通道(控制编号)
    public void GetOnUseVideoOutChannel() {
        // 存放物理输出通道的列表
        ArrayList<Integer> arrayList = new ArrayList<Integer>();

        // 查询物理屏通道
        NET_MATRIX_CARD_LIST stuCardList = new NET_MATRIX_CARD_LIST();
        if (netsdkApi.CLIENT_QueryMatrixCardInfo(loginHandle, stuCardList, 5000)) {
            for (int i = 0; i < stuCardList.nCount; i++) {
                if (stuCardList.stuCards[i].bEnable == 0) {     // 未使能跳过
                    continue;
                }
                if (((stuCardList.stuCards[i].dwCardType & NetSDKLib.NET_MATRIX_CARD_DECODE) == 8)
                        && (stuCardList.stuCards[i].nVideoDecChn > 0)) {   // 解码卡
                    for (int j = stuCardList.stuCards[i].nVideoOutChnMin; j <= stuCardList.stuCards[i].nVideoOutChnMax; j++) {
                        arrayList.add(j);
                    }

                } else if (((stuCardList.stuCards[i].dwCardType & NetSDKLib.NET_MATRIX_CARD_OUTPUT) == 2)
                        && (stuCardList.stuCards[i].nVideoOutChn > 0)) {   // 输出卡
                    for (int j = stuCardList.stuCards[i].nVideoOutChnMin; j <= stuCardList.stuCards[i].nVideoOutChnMax; j++) {
                        arrayList.add(j);
                    }
                }
            }
        }

        System.out.print("正在使用的物理输出通道: ");
        for (Integer integer : arrayList) {
            System.out.print(integer + " ");
        }
        System.out.print("\n");
    }

    // 各个屏幕的虚拟通道号
    public boolean GetVirtualChannel() throws UnsupportedEncodingException {
        int nComposite = 512; // 拼接屏数量上限

        NET_COMPOSITE_CHANNEL stComposite = new NET_COMPOSITE_CHANNEL();
        stComposite.write();

        int sizeOfOneComposite = stComposite.size();
        int sizeOfStComposites = sizeOfOneComposite * nComposite;
        Pointer pstComposites = new Memory(sizeOfStComposites);

        int offset = 0;
        for (int i = 0; i < nComposite; i++) {
            pstComposites.write(offset, stComposite.getPointer().getByteArray(0, sizeOfOneComposite), 0, sizeOfOneComposite);
            offset += sizeOfOneComposite;     // 更新偏移量
        }

        IntByReference nRetRef = new IntByReference(0);   // 查询返回的数据长度

        // 查询融合屏通道信息
        if (netsdkApi.CLIENT_QueryDevState(
                loginHandle,                        // 登录句柄
                NET_DEVSTATE_COMPOSITE_CHN,         // 查询融合屏通道信息
                pstComposites,                      // 入参
                sizeOfStComposites,                 // 入参长度
                nRetRef,                            // 返回长度引用 int* 类型
                5000)
        ) {
            int nRetLen = nRetRef.getValue();      // 返回长度
            int nSpliceCount = nRetLen / sizeOfOneComposite; // 拼接屏数量
            if (nSpliceCount > nComposite) {
                nSpliceCount = nComposite;
            }
            if (nSpliceCount > 0) {
                System.out.println("本设备包含拼接屏数量: " + nSpliceCount);
            } else {
                System.err.println("本设备没有拼接屏");
                return true;
            }

            offset = 0;
            for (int i = 0; i < nSpliceCount; i++) {
                stComposite.getPointer().write(0, pstComposites.getByteArray(offset, sizeOfOneComposite), 0, sizeOfOneComposite);
                stComposite.read();
                offset += sizeOfOneComposite;  // 更新偏移量
                // 拼接屏ID无效的忽略
                if (new String(stComposite.szCompositeID).equals("")) {
                    continue;
                }
                System.out.println(String.format("拼接屏[%03d] ID: %s, 虚拟通道号是:%d", i,
                        new String(stComposite.szCompositeID, "GBK"), stComposite.nVirtualChannel));
            }
        } else {
            System.err.println("查询融合屏通道信息失败: " + ToolKits.getErrorCode());
        }
        return false;
    }

    private static final AV_CFG_MonitorWall monitorWall = new AV_CFG_MonitorWall();

    static {
        monitorWall.write();
    }

    // 各个屏幕的物理通道
    public void GetMonitorWallDetail() throws UnsupportedEncodingException {

        // 获取电视墙配置
        int nMaxMonitorWall = 15;  // 电视墙最大数量
        int sizeOfWall = monitorWall.size();

        IntByReference error = new IntByReference(0);
        IntByReference retLen = new IntByReference(0);
        int nBufferLen = 2 * 1024 * 1024;
        byte[] strBuffer = new byte[nBufferLen];

        int totalSize = sizeOfWall * nMaxMonitorWall;
        Pointer dataPointer = new Memory(totalSize);

        String strCmd = NetSDKLib.CFG_CMD_MONITORWALL;

        if (!netsdkApi.CLIENT_GetNewDevConfig(loginHandle, strCmd, -1, strBuffer, nBufferLen, error, 5000)) {
            System.err.printf("Get %s Config Failed!Last Error = %s\n", strCmd, ToolKits.getErrorCode());
            return;
        }

        int offset = 0;
        for (int i = 0; i < nMaxMonitorWall; i++) {
            dataPointer.write(offset, monitorWall.getPointer().getByteArray(0, sizeOfWall), 0, sizeOfWall);
            offset += sizeOfWall;
        }

        if (!configApi.CLIENT_ParseData(strCmd, strBuffer, dataPointer, totalSize, retLen.getPointer())) {
            System.err.println("Parse " + strCmd + " Config Failed!" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("获取配置成功");

        int retNum = retLen.getValue() / sizeOfWall;
        System.out.println("电视墙个数：" + retNum);

        offset = 0;
        for (int m = 0; m < retNum; m++) {
            GetMonitorWallConfigData(monitorWall, sizeOfWall, dataPointer, offset);
            offset += sizeOfWall;
            System.out.println(String.format("[电视墙名称: %s, 使能: %s",
                    new String(monitorWall.szName, "GBK").trim(),
                    (monitorWall.bDisable == 0 ? "有效" : "无效")));
            for (int i = 0; i < monitorWall.nBlockCount; i++) {
                System.out.println(String.format("区块名称: %s, TV数量: %d",
                        new String(monitorWall.stuBlocks[i].szName, "GBK"),
                        monitorWall.stuBlocks[i].nTVCount));
                for (int j = 0; j < monitorWall.stuBlocks[i].nTVCount; j++) {
                    System.out.println(String.format("  TV[%03d], 名称: %s, 物理输出通道: %d", j,
                            new String(monitorWall.stuBlocks[i].stuTVs[j].szName, "GBK"),
                            monitorWall.stuBlocks[i].stuTVs[j].nChannelID));
                }
            }
        }
    }

    private void GetMonitorWallConfigData(AV_CFG_MonitorWall monitorWall, int sizeOfWall, Pointer dataPointer, int offset) {
        Pointer pMonitor = monitorWall.getPointer();
        pMonitor.write(0, dataPointer.getByteArray(offset, sizeOfWall), 0, sizeOfWall);
        monitorWall.readField("szName");        // 名称
        monitorWall.readField("nLine");         // 网络行数
        monitorWall.readField("nColumn");       // 名称
        monitorWall.readField("nBlockCount");   // 区块数量

        int nBlockCount = monitorWall.nBlockCount;
        int blockOffset = offset + monitorWall.fieldOffset("stuBlocks");
        int sizeBlock = monitorWall.stuBlocks[0].size();
        for (int j = 0; j < nBlockCount; j++) {
            AV_CFG_MonitorWallBlock stuBlock = monitorWall.stuBlocks[j];
            Pointer pBlock = stuBlock.getPointer();
            pBlock.write(0, dataPointer.getByteArray(blockOffset, sizeBlock), 0, sizeBlock);
            stuBlock.readField("nStructSize");
            stuBlock.readField("nLine");         // 单个TV占的网格行数
            stuBlock.readField("nColumn");       // 单个TV占的网格列数
            stuBlock.readField("stuRect");       // 区块的区域坐标
            stuBlock.readField("nTVCount");      // TV数量

            int nTVCount = stuBlock.nTVCount;
            int TVOffset = blockOffset + stuBlock.fieldOffset("stuTVs");
            int sizeTV = stuBlock.stuTVs[0].size();
            for (int k = 0; k < nTVCount; k++) {
                AV_CFG_MonitorWallTVOut stuTV = stuBlock.stuTVs[k];
                Pointer pTV = stuTV.getPointer();
                pTV.write(0, dataPointer.getByteArray(TVOffset, sizeTV), 0, sizeTV);
                stuTV.readField("nStructSize");
                stuTV.readField("szDeviceID");     // 设备ID, 为空或"Local"表示本地设备
                stuTV.readField("nChannelID");     // 通道ID
                stuTV.readField("szName");         // 屏幕名称

                TVOffset += sizeTV;
            }

            stuBlock.readField("stuTimeSectionWeekDay");    // 开关机时间
            stuBlock.readField("szName");                   // 区块名称
            stuBlock.readField("szCompositeID");            // 融合屏ID
            stuBlock.readField("szBlockType");              // 显示单元组类型

            blockOffset += sizeBlock;
        }

        monitorWall.readField("bDisable");      // 是否禁用, 0-该电视墙有效, 1-该电视墙无效
        monitorWall.readField("szDesc");        // 电视墙描述信息
    }

    // 测试 获取某个输出通道上的窗口数量
    public void TestGetWndsCount() {
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入输出通道号");
        int nChannel = sc.nextInt();
        GetWndsCount(loginHandle, nChannel);
    }

    // 获取某个输出通道上的窗口数量
    public int GetWndsCount(LLong lLoginHandle, int nChannel) {
        int nWndsCount; // 窗口数量
        // 获取当前TV的分割模式
        NET_SPLIT_MODE_INFO stuInfo = new NET_SPLIT_MODE_INFO();
        boolean bRet = netsdkApi.CLIENT_GetSplitMode(lLoginHandle, nChannel, stuInfo, 5000);
        if (!bRet) {
            System.err.println("Get split mode failed: " + ToolKits.getErrorCode());
            return 0;
        }

        if (stuInfo.emSplitMode == NET_SPLIT_MODE.NET_SPLIT_FREE) {
            NET_IN_SPLIT_GET_WINDOWS stuInGetWhn = new NET_IN_SPLIT_GET_WINDOWS();
            NET_OUT_SPLIT_GET_WINDOWS stuOutGetWhn = new NET_OUT_SPLIT_GET_WINDOWS();
            stuInGetWhn.nChannel = nChannel;
            bRet = netsdkApi.CLIENT_GetSplitWindowsInfo(lLoginHandle, stuInGetWhn, stuOutGetWhn, 5000);
            if (!bRet) {
                System.err.println("Get split windows info failed: " + ToolKits.getErrorCode());
                return 0;
            }
            nWndsCount = stuOutGetWhn.stuWindows.nWndsCount;
        } else {
            nWndsCount = stuInfo.emSplitMode;   // 其他分割模式下 枚举值就是分屏数量
        }
        System.out.println(String.format("输出通道[%2d]现有%2d个窗口", nChannel, nWndsCount));
        return nWndsCount;
    }

    // 叠加和清除OSD
    public void GetAndSetSplitOSDEx() {
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入输出通道号");
        int nChannel = sc.nextInt();
        System.out.println("请输入窗口号");
        int nWindow = sc.nextInt();
        NET_IN_SPLIT_GET_OSD_EX stIn = new NET_IN_SPLIT_GET_OSD_EX();
        stIn.nChannel = nChannel;
        stIn.nWindow = nWindow;
        stIn.write();
        NET_OUT_SPLIT_GET_OSD_EX stOut = new NET_OUT_SPLIT_GET_OSD_EX();
        stOut.write();
        boolean bRet = netsdkApi.CLIENT_GetSplitOSDEx(loginHandle, stIn.getPointer(), stOut.getPointer(), 5000);
        stOut.read();
        if (!bRet) {
            System.err.println("CLIENT_GetSplitOSDEx failed: " + ToolKits.getErrorCode());
            return;
        } else {
            System.out.println("CLIENT_GetSplitOSDEx success!");
            System.out.println("nOSDNum = " + stOut.nOSDNum);
            for(int i = 0; i < stOut.nOSDNum; i ++){
                System.out.println("stuOSD[" + i + "].bEnable = " + stOut.stuOSD[i].bEnable);
//                stOut.stuOSD[i].bEnable = (stOut.stuOSD[i].bEnable == 1 ? 0: 1);
                System.out.println("stuOSD[" + i + "].stuBackColor = " + stOut.stuOSD[i].stuBackColor.toString());
                System.out.println("stuOSD[" + i + "].stuBackRect = " + stOut.stuOSD[i].stuBackRect.toString());
                System.out.println("stuOSD[" + i + "].stuFrontColor = " + stOut.stuOSD[i].stuFrontColor.toString());
                System.out.println("stuOSD[" + i + "].stuFrontRect = " + stOut.stuOSD[i].stuFrontRect.toString());
                System.out.println("stuOSD[" + i + "].bRoll = " + stOut.stuOSD[i].bRoll);
//                stOut.stuOSD[i].bRoll = (stOut.stuOSD[i].bRoll == 1 ? 0: 1);
                System.out.println("stuOSD[" + i + "].byFontSize = " + stOut.stuOSD[i].byFontSize);
                System.out.println("stuOSD[" + i + "].byRollMode = " + stOut.stuOSD[i].byRollMode);
                System.out.println("stuOSD[" + i + "].byRoolSpeed = " + stOut.stuOSD[i].byRoolSpeed);
                System.out.println("stuOSD[" + i + "].byTextAlign = " + stOut.stuOSD[i].byTextAlign);
                System.out.println("stuOSD[" + i + "].byType = " + stOut.stuOSD[i].byType);
                System.out.println("stuOSD[" + i + "].szContent = " + new String(stOut.stuOSD[i].szContent).trim());
//                stOut.stuOSD[i].szContent = (new String(stOut.stuOSD[i].szContent).trim() + "add1").getBytes();
                System.out.println("stuOSD[" + i + "].szFontType = " + new String(stOut.stuOSD[i].szFontType).trim());
                System.out.println("stuOSD[" + i + "].szPattern = " + new String(stOut.stuOSD[i].szPattern).trim());
            }
            NET_IN_SPLIT_SET_OSD_EX stIn1 = new NET_IN_SPLIT_SET_OSD_EX();
            stIn1.nChannel = stIn.nChannel;
            stIn1.nWindow = stIn.nWindow;
            stIn1.nOSDNum = 2;
            for(int i = 0; i < 2; i ++){
                stIn1.stuOSD[i].dwSize = stIn1.stuOSD[i].size();
                stIn1.stuOSD[i].bEnable = (stOut.stuOSD[i].bEnable == 1 ? 0: 1);
                stIn1.stuOSD[i].bRoll = (stOut.stuOSD[i].bRoll == 1 ? 0: 1);
                stIn1.stuOSD[i].stuBackColor.setRGBA(10*(i + 1),10*(i + 1),10*(i + 1),10*(i + 1));
                stIn1.stuOSD[i].stuFrontColor.setRGBA(10*(i + 1),10*(i + 1),10*(i + 1),10*(i + 1));
                stIn1.stuOSD[i].szContent = ("test" + i).getBytes();
                stIn1.stuOSD[i].szPattern = ("test" + i).getBytes();
//                System.out.println("stuOSD[" + i + "].bEnable = " + stIn1.stuOSD[i].bEnable);
//                System.out.println("stuOSD[" + i + "].szContent = " + new String(stIn1.stuOSD[i].szContent).trim());
            }
            stIn1.write();
            NET_OUT_SPLIT_SET_OSD_EX stOut1 = new NET_OUT_SPLIT_SET_OSD_EX();
            stOut1.write();
            bRet = netsdkApi.CLIENT_SetSplitOSDEx(loginHandle, stIn1.getPointer(), stOut1.getPointer(), 5000);
            if (!bRet) {
                System.err.println("CLIENT_SetSplitOSDEx failed: " + ToolKits.getErrorCode());
                return;
            } else {
                System.out.println("CLIENT_SetSplitOSDEx success!");
            }
        }
    }

    // 设置源边框高亮使能开关
    public void OperateSplit_SetHighLight() {
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入输出通道号");
        int nChannel = sc.nextInt();
        System.out.println("请输入窗口号");
        int nWindow = sc.nextInt();
        NET_IN_SPLIT_SET_HIGHLIGHT stIn = new NET_IN_SPLIT_SET_HIGHLIGHT();
        stIn.nChannel = nChannel;
        stIn.nWindow = nWindow;
        stIn.bHighLightEn = 1;
        stIn.stuColor.setRGBA(255,255,255,255);
        stIn.nBlinkTimes = 3;
        stIn.nBlinkInterval = 1000;
        stIn.write();
        NET_OUT_SPLIT_SET_HIGHLIGHT stOut = new NET_OUT_SPLIT_SET_HIGHLIGHT();
        stOut.write();
        boolean bRet = netsdkApi.CLIENT_OperateSplit(loginHandle, NET_SPLIT_OPERATE_TYPE.NET_SPLIT_OPERATE_SET_HIGHLIGHT.getValue(), stIn.getPointer(), stOut.getPointer(), 5000);
        if (!bRet) {
            System.err.println("CLIENT_OperateSplit-NET_SPLIT_OPERATE_SET_HIGHLIGHT failed: " + ToolKits.getErrorCode());
            return;
        } else {
            System.out.println("CLIENT_OperateSplit-NET_SPLIT_OPERATE_SET_HIGHLIGHT success!");
        }

    }

//ERR220704088
//获取和设置电视墙配置
    public void MonitorWallSet() {

        int channl=-1;
        // 获取电视墙配置, 把组成拼接屏的各个TV标识出来
        int nMaxMonitorWall = 2;  // 电视墙数量，自己设置
        AV_CFG_MonitorWall[] monitorWall = new AV_CFG_MonitorWall[nMaxMonitorWall];
        for (int  i = 0; i < nMaxMonitorWall; i++) {
            monitorWall[i] = new AV_CFG_MonitorWall();
        }
        System.out.println("monitorWall.size:"+monitorWall[0].size()*nMaxMonitorWall);
        int retLength = ToolKits.GetDevConfig(loginHandle, channl, NetSDKLib.CFG_CMD_MONITORWALL , monitorWall);
        if (retLength != 0) {
            System.out.println("GET CFG_CMD_MONITORWALL success!");
            // 如果获取到的电视墙个数为2，那么电视墙ID为[0, 1],  电视墙有效时，才能查询到预案
            System.out.println("电视墙个数：" + retLength);
            for (int  i = 0; i < retLength; i++) {
                try {
                    System.out.print("[电视墙名称:" + new String(monitorWall[i].szName, "GBK").trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                System.out.print("  使能：" + (monitorWall[i].bDisable == 0 ? "有效" : "无效"));
                System.out.print("  电视墙ID：" + i + "]" + "\n");
            }
        }else {
            System.err.println("GET CFG_CMD_MONITORWALL failed: " + ToolKits.getErrorCode());

        }

        boolean b
                = ToolKits.SetDevConfig(loginHandle, channl, CFG_CMD_MONITORWALL, monitorWall);

        if(b){
            System.out.println("SET CFG_CMD_MONITORWALL success!");
        }else {
            System.err.println("SET CFG_CMD_MONITORWALL failed: " + ToolKits.getErrorCode());
        }

    }



    ////////////////////////////////////////////////////////////////
    // M70的登陆信息
    String m_strIp = "172.26.1.98";
    int m_nPort = 37777;
    String m_strUser = "admin";
    String m_strPassword = "lsga5220";
    ////////////////////////////////////////////////////////////////

    public void RunTest() {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "电视墙RTSP推流命令", "SetSplitSourceByRTSP"));
        menu.addItem(new CaseMenu.Item(this, "获取电视墙配置Ex(速度优化)", "GetMonitorWallDetail"));
        menu.addItem(new CaseMenu.Item(this, "获取解码器视频输出通道信息", "TestGetChannelInfo"));
        menu.addItem(new CaseMenu.Item(this, "获取某个输出通道上的窗口数量", "TestGetWndsCount"));
        menu.addItem(new CaseMenu.Item(this, "叠加和清除OSD", "GetAndSetSplitOSDEx"));
        menu.addItem(new CaseMenu.Item(this, "设置源边框高亮使能开关", "OperateSplit_SetHighLight"));
        menu.addItem(new CaseMenu.Item(this, "获取和设置电视墙配置", "MonitorWallSet"));
        menu.addItem(new CaseMenu.Item(this, "在用的物理输出通道", "GetOnUseVideoOutChannel"));
        menu.run();
    }

    public static void main(String[] args) {
        MonitorWallNew demo = new MonitorWallNew();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
