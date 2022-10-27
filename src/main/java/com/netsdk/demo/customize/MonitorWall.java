package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_IPC_TYPE;
import com.netsdk.lib.enumeration.NET_MONITORWALL_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_IN_MONITORWALL_GET_ENABLE;
import com.netsdk.lib.structure.NET_IN_MONITORWALL_SET_ENABLE;
import com.netsdk.lib.structure.NET_OUT_MONITORWALL_GET_ENABLE;
import com.netsdk.lib.structure.NET_OUT_MONITORWALL_SET_ENABLE;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
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
public class MonitorWall {
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
            System.out.printf("Login Device[%s] Port[%d]Success!\n", m_strIp, m_nPort);
        } else {
            System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%x]\n", m_strIp, m_nPort, netsdkApi.CLIENT_GetLastError());
            EndTest();
        }
    }

    /**
     * 配置视频上墙
     */
    public void SetSplitSource() {
        NET_SPLIT_SOURCE setsplitSource = new NET_SPLIT_SOURCE();
        setsplitSource.bEnable = 1;  // 使能

        // 输入前端设备IP
        String ip = "171.2.2.101";
        System.arraycopy(ip.getBytes(), 0, setsplitSource.szIp, 0, ip.getBytes().length);

        // 输入前端设备端口号
        setsplitSource.nPort = 37777;

        // 输入前端设备上的用户名
        String username = "admin";
        System.arraycopy(username.getBytes(), 0, setsplitSource.szUserEx, 0, username.getBytes().length);

        // 输入前端设备上的密码
        String passwd = "admin12";
        System.arraycopy(passwd.getBytes(), 0, setsplitSource.szPwdEx, 0, passwd.getBytes().length);

        // 输入前端设备上的通道号
        setsplitSource.nChannelID = 0;

        // 输入前端设备上的通道数量
        setsplitSource.nVideoChannel = 12;

        // 码流
        setsplitSource.nStreamType = 0; //主码流

        // 选择连接方式: -1: auto, 0：TCP；1：UDP；2：组播
        setsplitSource.byConnType = 0; // TCP

        // Rtsp端口号, 0-65535
        setsplitSource.dwRtspPort = 554;

        // 协议类型
        setsplitSource.emProtocol = NET_DEVICE_PROTOCOL.NET_PROTOCOL_PRIVATE2;  //默认采用大华私有协议，如有需要可自行设置

        // 控制台输入输出通道和窗口号
        Scanner scanner = new Scanner(System.in);
        String line = "";
        while (true) {
            System.out.println("请输入输出通道号：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }

        // 获取当前M70上的输出通道号，一个输出通道号对应一个屏幕
        // 视频上墙用到的是融合通道，需要在web上配置。    设置->显示管理->电视墙配置   双击添加的电视墙进行配置
        int nOutChannel = Integer.parseInt(line); // 输出通道号，根据 GetVideoOutChannelInfo() 获取的列表选择需要的输出通道

        line = "";
        while (true) {
            System.out.println("请输入输出通道号对应的窗口号：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }


        // 根据输出通道获取输出通道上的窗口总数量， 如果数量是2，那么窗口ID为 0 1 ,通过 GetWhnCount() 查询，要对应上面的输出通道
        int nWindow = Integer.parseInt(line);  // 输出通道对应的窗口号， -1表示所有窗口

        int nSrcCount = 1;  // 设置显示源数组的个数(此接口用于设置一个显示源)

        boolean bSetSplit = netsdkApi.CLIENT_SetSplitSource(loginHandle, nOutChannel, nWindow, setsplitSource, nSrcCount, 3000);
        if (bSetSplit) {
            System.out.println("视频上墙成功!");
        } else {
            System.err.println("视频上墙失败!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 查询前端相机信息，用于视频上墙
     */
    public void GetMatrixCamerasInfo() {
        // 可用的显示源信息
        int cameraCount = 60;
        NET_MATRIX_CAMERA_INFO[] cameraInfo = new NET_MATRIX_CAMERA_INFO[cameraCount];
        for (int i = 0; i < cameraCount; i++) {
            cameraInfo[i] = new NET_MATRIX_CAMERA_INFO();
        }

        // 入参
        NET_IN_MATRIX_GET_CAMERAS inMatrix = new NET_IN_MATRIX_GET_CAMERAS();

        // 出参
        NET_OUT_MATRIX_GET_CAMERAS outMatrix = new NET_OUT_MATRIX_GET_CAMERAS();
        outMatrix.nMaxCameraCount = cameraCount;
        outMatrix.pstuCameras = new Memory(cameraInfo[0].size() * cameraCount);
        outMatrix.pstuCameras.clear(cameraInfo[0].size() * cameraCount);

        ToolKits.SetStructArrToPointerData(cameraInfo, outMatrix.pstuCameras);  // 将数组内存拷贝到Pointer

        if (netsdkApi.CLIENT_MatrixGetCameras(loginHandle, inMatrix, outMatrix, 5000)) {
            ToolKits.GetPointerDataToStructArr(outMatrix.pstuCameras, cameraInfo);  // 将 Pointer 的内容 输出到   数组

            for (int j = 0; j < outMatrix.nRetCameraCount; j++) {
                if (cameraInfo[j].bRemoteDevice == 0) {   // 过滤远程设备
                    continue;
                }
                System.out.println("通道号：" + cameraInfo[j].nChannelID);
                System.out.println("IP : " + new String(cameraInfo[j].stuRemoteDevice.szIp).trim());
                System.out.println("nPort : " + cameraInfo[j].stuRemoteDevice.nPort);
                System.out.println("szUser : " + new String(cameraInfo[j].stuRemoteDevice.szUser).trim());
                System.out.println("szPwd : " + new String(cameraInfo[j].stuRemoteDevice.szPwd).trim());
                System.out.println("通道个数 : " + cameraInfo[j].stuRemoteDevice.nVideoInputChannels);
            }
        } else {
            System.err.println("MatrixGetCameras Failed." + ToolKits.getErrorCode());
        }
    }

    /**
     * 查询输出通道号(物理通道号和融合通道号)，用于视频上墙
     */
    public void GetVideoOutChannelInfo() {
        int i = 0;
        int j = 0;

        // 列表清空
        if (arrayList.size() > 0) {
            arrayList.clear();
        }

        // 查询物理屏通道
        NET_MATRIX_CARD_LIST pstuCardList = new NET_MATRIX_CARD_LIST();
        if (netsdkApi.CLIENT_QueryMatrixCardInfo(loginHandle, pstuCardList, 5000)) {
            for (i = 0; i < pstuCardList.nCount; i++) {
                if (pstuCardList.stuCards[i].bEnable == 0) { // 未使能跳过
                    continue;
                }
                if (((pstuCardList.stuCards[i].dwCardType & NetSDKLib.NET_MATRIX_CARD_DECODE) == 8)
                        && (pstuCardList.stuCards[i].nVideoDecChn > 0)) {   // 解码卡
                    for (j = pstuCardList.stuCards[i].nVideoOutChnMin; j <= pstuCardList.stuCards[i].nVideoOutChnMax; j++) {
                        addChn(j);
                    }

                } else if (((pstuCardList.stuCards[i].dwCardType & NetSDKLib.NET_MATRIX_CARD_OUTPUT) == 2)
                        && (pstuCardList.stuCards[i].nVideoOutChn > 0)) {   // / 输出卡
                    for (j = pstuCardList.stuCards[i].nVideoOutChnMin; j <= pstuCardList.stuCards[i].nVideoOutChnMax; j++) {
                        addChn(j);
                    }
                }
            }
        }

        System.out.print("所有物理通道[");
        for (i = 0; i < arrayList.size(); i++) {
            System.out.print(arrayList.get(i) + " ");
        }
        System.out.println("]");


        // 获取电视墙配置, 把组成拼接屏的各个TV标识出来
        int nMaxMonitorWall = 10;  // 电视墙数量，自己设置
        AV_CFG_MonitorWall[] monitorWall = new AV_CFG_MonitorWall[nMaxMonitorWall];
        for (i = 0; i < nMaxMonitorWall; i++) {
            monitorWall[i] = new AV_CFG_MonitorWall();
        }

        int retLength = ToolKits.GetDevConfig(loginHandle, -1, NetSDKLib.CFG_CMD_MONITORWALL, monitorWall);
        if (retLength != 0) {
            System.out.print("添加到电视墙配置的物理通道[");
            for (i = 0; i < retLength; i++) {
                for (j = 0; j < monitorWall[i].nBlockCount; j++) {
                    for (int k = 0; k < monitorWall[i].stuBlocks[j].nTVCount; k++) {
                        filterChn(monitorWall[i].stuBlocks[j].stuTVs[k].nChannelID);
                        System.out.print(monitorWall[i].stuBlocks[j].stuTVs[k].nChannelID + " ");
                    }
                }
            }
        }
        System.out.println("]");

        System.out.print("未添加到电视墙配置的物理通道[");
        for (i = 0; i < arrayList.size(); i++) {
            System.out.print(arrayList.get(i) + " ");
        }
        System.out.println("]");


        /************************ 查询融合屏通道 ******************************/
        int nComposite = 512; //拼接屏数量
        int nType = NET_DEVSTATE_COMPOSITE_CHN;
        NET_COMPOSITE_CHANNEL[] compositeChn = new NET_COMPOSITE_CHANNEL[nComposite];
        for (i = 0; i < nComposite; i++) {
            compositeChn[i] = new NET_COMPOSITE_CHANNEL();
        }

        int memorySize = nComposite * compositeChn[0].size();
        Memory memory = new Memory(memorySize);
        memory.clear(memorySize);

        ToolKits.SetStructArrToPointerData(compositeChn, memory);

        IntByReference intRetLen = new IntByReference();

        if (netsdkApi.CLIENT_QueryDevState(loginHandle, nType, memory, memorySize, intRetLen, 3000)) {
            int nSpliceCount = intRetLen.getValue() / compositeChn[0].size();   // 拼接屏数量

            if (nSpliceCount > nComposite) {
                nSpliceCount = nComposite;
            }

            ToolKits.GetPointerDataToStructArr(memory, compositeChn);
            System.out.print("融合通道[");
            for (i = 0; i < nSpliceCount; i++) {
                if (new String(compositeChn[i].szCompositeID).trim().equals("")) {
                    continue;
                }
                System.out.print(compositeChn[i].nVirtualChannel + " ");
            }

        } else {
            System.err.println("QueryDev Failed!" + netsdkApi.CLIENT_GetLastError());
        }
        System.out.println("]");
    }

    // 存放物理输出通道的列表
    ArrayList<Integer> arrayList = new ArrayList<Integer>();

    // 添加输出通道到列表
    public void addChn(int chn) {
        arrayList.add(chn);
    }

    // 过滤列表中用于融合通道的物理通道
    public void filterChn(int chn) {
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i) == chn) {
                arrayList.remove(i);
            }
        }
    }

    /**
     * 获取某个输出通道上的窗口总数量
     */
    public void GetWhnCount() {
        Scanner scanner = new Scanner(System.in);

        String line = "";
        while (true) {
            System.out.println("请输入输出通道号：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }


        int nChn = Integer.parseInt(line); // 通道
        int nWindow = 0; // 窗口数量，初始化

        NET_SPLIT_MODE_INFO splitMode = new NET_SPLIT_MODE_INFO();

        boolean bRet = netsdkApi.CLIENT_GetSplitMode(loginHandle, nChn, splitMode, 3000);

        if (!bRet) {
            nWindow = 0;
            System.err.println("CLIENT_GetSplitMode Filed" + netsdkApi.CLIENT_GetLastError());
        }

        if (splitMode.emSplitMode == NET_SPLIT_MODE.NET_SPLIT_FREE) {
            // 入参
            NET_IN_SPLIT_GET_WINDOWS stuInGetWhn = new NET_IN_SPLIT_GET_WINDOWS();
            stuInGetWhn.nChannel = nChn;

            // 出参
            NET_OUT_SPLIT_GET_WINDOWS stuOutGetWhn = new NET_OUT_SPLIT_GET_WINDOWS();

            if (netsdkApi.CLIENT_GetSplitWindowsInfo(loginHandle, stuInGetWhn, stuOutGetWhn, 3000)) {
                nWindow = stuOutGetWhn.stuWindows.nWndsCount;
            } else {
                nWindow = 0;
            }
        } else {
            nWindow = splitMode.emSplitMode;
        }

        System.out.println(">>> 输出通道" + line + "的窗口数量：" + nWindow);
    }

    /**
     * 开窗
     * 以下示例是设置单个全屏的窗口。  如果设置多个窗口，bDirectable = 0，然后根据具体的情况，设置窗口位置 stuRect
     */
    public void OpenSplitWindow() {
        Scanner scanner = new Scanner(System.in);

        String line = "";
        while (true) {
            System.out.println("请输入输出通道号：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }

        // 入参
        NET_IN_SPLIT_OPEN_WINDOW stIn = new NET_IN_SPLIT_OPEN_WINDOW();
        stIn.nChannel = Integer.parseInt(line); // 通道

        // 窗口位置, 0~8192
        stIn.stuRect.left = new NativeLong(0);
        stIn.stuRect.top = new NativeLong(0);
        stIn.stuRect.right = new NativeLong(8192);
        stIn.stuRect.bottom = new NativeLong(8192);

        stIn.bDirectable = 1;   // 坐标是否满足直通条件, 直通是指拼接屏方式下,此窗口区域正好为物理屏区域

        // 出参
        NET_OUT_SPLIT_OPEN_WINDOW stOut = new NET_OUT_SPLIT_OPEN_WINDOW();

        if (netsdkApi.CLIENT_OpenSplitWindow(loginHandle, stIn, stOut, 3000)) {
            System.out.println(">>> 输出通道" + line + "开窗成功！ 窗口序号:" + stOut.nWindowID);
        } else {
            System.err.println(">>> 输出通道" + line + "开窗失败！ " + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 关窗
     */
    public void CloseSplitWindow() {
        Scanner scanner = new Scanner(System.in);

        // 入参
        NET_IN_SPLIT_CLOSE_WINDOW stIn = new NET_IN_SPLIT_CLOSE_WINDOW();
        String line = "";
        while (true) {
            System.out.println("请输入输出通道号：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }

        stIn.nChannel = Integer.parseInt(line);  // 输出通道号或融合屏虚拟通道号, pszCompositeID为NULL时有效

        line = "";
        while (true) {
            System.out.println("请输入窗口序号：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }

        stIn.nWindowID = Integer.parseInt(line); // 窗口序号
        stIn.pszCompositeID = "";                 // 融合屏ID

        // 出参
        NET_OUT_SPLIT_CLOSE_WINDOW stOut = new NET_OUT_SPLIT_CLOSE_WINDOW();

        if (netsdkApi.CLIENT_CloseSplitWindow(loginHandle, stIn, stOut, 3000)) {
            System.out.println(">>> 输出通道" + line + "关窗成功！");
        } else {
            System.err.println(">>> 输出通道" + line + "关窗失败！ " + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 获取电视墙配置
     */
    public void GetMonitorWall() {
        int i = 0;
        // 获取电视墙配置, 把组成拼接屏的各个TV标识出来
        int nMaxMonitorWall = 15;  // 电视墙数量，自己设置
        AV_CFG_MonitorWall[] monitorWall = new AV_CFG_MonitorWall[nMaxMonitorWall];
        for (i = 0; i < nMaxMonitorWall; i++) {
            monitorWall[i] = new AV_CFG_MonitorWall();
        }

        int retLength = ToolKits.GetDevConfig(loginHandle, -1, NetSDKLib.CFG_CMD_MONITORWALL, monitorWall);
        if (retLength != 0) {
            // 如果获取到的电视墙个数为2，那么电视墙ID为[0, 1],  电视墙有效时，才能查询到预案
            System.out.println("电视墙个数：" + retLength);
            for (i = 0; i < retLength; i++) {
                try {
                    System.out.print("[电视墙名称:" + new String(monitorWall[i].szName, "GBK").trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                System.out.print("  使能：" + (monitorWall[i].bDisable == 0 ? "有效" : "无效"));
                System.out.print("  电视墙ID：" + i + "]" + "\n");
            }
        }
    }

    /**
     * 根据拼接屏ID查询融合通道
     *
     * @param szCompositeID 拼接屏ID
     */
    public int GetCompositeChannel(String szCompositeID) {
        int i = 0;
        int nVirtualChn = 0;  // 融合通道
        int nComposite = 512; //拼接屏数量
        int nType = NET_DEVSTATE_COMPOSITE_CHN;
        NET_COMPOSITE_CHANNEL[] compositeChn = new NET_COMPOSITE_CHANNEL[nComposite];
        for (i = 0; i < nComposite; i++) {
            compositeChn[i] = new NET_COMPOSITE_CHANNEL();
        }

        int memorySize = nComposite * compositeChn[0].size();
        Memory memory = new Memory(memorySize);
        memory.clear(memorySize);

        ToolKits.SetStructArrToPointerData(compositeChn, memory);

        IntByReference intRetLen = new IntByReference();

        if (netsdkApi.CLIENT_QueryDevState(loginHandle, nType, memory, memorySize, intRetLen, 3000)) {
            int nSpliceCount = intRetLen.getValue() / compositeChn[0].size();   // 拼接屏数量

            if (nSpliceCount > nComposite) {
                nSpliceCount = nComposite;
            }

            ToolKits.GetPointerDataToStructArr(memory, compositeChn);

            for (i = 0; i < nSpliceCount; i++) {
                if (new String(compositeChn[i].szCompositeID).trim().equals(szCompositeID)) {
                    nVirtualChn = compositeChn[i].nVirtualChannel;
                }
            }

        } else {
            System.err.println("QueryDev Failed!" + netsdkApi.CLIENT_GetLastError());
        }

        return nVirtualChn;
    }

    /**
     * 获取电视墙预案
     */
    public void GetMonitorWallCollections() {
        int i = 0;
        Scanner scanner = new Scanner(System.in);

        String line = "";
        while (true) {
            System.out.println("请输入电视墙ID：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }


        // 电视墙预案数组
        int nCollecCount = 10; // 查询的预案数量的最大值,如果设置的过大，会溢出，需要在eclipse里设置，增加堆内存
        NET_MONITORWALL_COLLECTION[] monitorwallArr = new NET_MONITORWALL_COLLECTION[nCollecCount];
        for (i = 0; i < monitorwallArr.length; i++) {
            monitorwallArr[i] = new NET_MONITORWALL_COLLECTION();
        }

        // 入参
        NET_IN_WM_GET_COLLECTIONS stIn = new NET_IN_WM_GET_COLLECTIONS();
        stIn.nMonitorWallID = Integer.parseInt(line); //电视墙ID

        // 出参
        NET_OUT_WM_GET_COLLECTIONS stOut = new NET_OUT_WM_GET_COLLECTIONS();
        // 电视墙预案指针初始化
        stOut.nMaxCollectionsCount = monitorwallArr[0].size() * nCollecCount;           // 电视墙预案数组大小
        stOut.pCollections = new Memory(monitorwallArr[0].size() * nCollecCount);       // 初始化Pointer指针
        stOut.pCollections.clear(monitorwallArr[0].size() * nCollecCount);

        ToolKits.SetStructArrToPointerData(monitorwallArr, stOut.pCollections);        // 将数组内存拷贝给指针

        if (netsdkApi.CLIENT_GetMonitorWallCollections(loginHandle, stIn, stOut, 5000)) {
            ToolKits.GetPointerDataToStructArr(stOut.pCollections, monitorwallArr);     // 将指针的内容放入数组

            System.out.printf("[电视墙预案数量  %d]\n\n", stOut.nCollectionsCount);
            int nCount = Math.min(stOut.nCollectionsCount, nCollecCount);

            for (i = 0; i < nCount; i++) {
                try {
                    System.out.printf("[电视墙ID: %d   电视墙名称: %s   预案名称:%s]\n", Integer.parseInt(line),
                            new String(monitorwallArr[i].stuMonitorWall.szName, "GBK").trim(),
                            new String(monitorwallArr[i].szName, "GBK").trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                // nBlocksCount 融合屏个数
                for (int j = 0; j < monitorwallArr[i].nBlocksCount; j++) {
                    System.out.printf("{拼接屏ID: %s   分割模式:%d}\n",
                            new String(monitorwallArr[i].stuBlocks[j].szCompositeID).trim(), monitorwallArr[i].stuBlocks[j].emSplitMode);
                }
                System.out.println();
            }
        } else {
            System.err.println("GetMonitorWallCollections Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 根据拼接屏ID获取窗口数量
     * 获取到的融合通道和窗口数量，是从当前电视墙获取的。要先加载预案
     */
    public void getCollectWindowCount() {
        Scanner scanner = new Scanner(System.in);

        String line = "";
        while (true) {
            System.out.println("请输入拼接屏ID：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }

        // 以下是一个融合屏对应的窗口ID
        int nChn = 0;     // 融合通道初始化
        int nWindow = 0;  // 窗口数量初始化

        nChn = GetCompositeChannel(line);
        System.out.println("融合通道:" + nChn);
        while (true) {
            System.out.println("请输入分割模式：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }

        if (Integer.parseInt(line) == NET_SPLIT_MODE.NET_SPLIT_FREE) {
            // 入参
            NET_IN_SPLIT_GET_WINDOWS stuInGetWhn = new NET_IN_SPLIT_GET_WINDOWS();
            stuInGetWhn.nChannel = nChn;

            // 出参
            NET_OUT_SPLIT_GET_WINDOWS stuOutGetWhn = new NET_OUT_SPLIT_GET_WINDOWS();

            if (netsdkApi.CLIENT_GetSplitWindowsInfo(loginHandle, stuInGetWhn, stuOutGetWhn, 3000)) {
                nWindow = stuOutGetWhn.stuWindows.nWndsCount;
            } else {
                nWindow = 0;
            }
        } else {
            nWindow = Integer.parseInt(line);
        }
        System.out.println("窗口数量:" + nWindow);
    }

    /**
     * 载入电视墙预案
     */
    public void LoadMonitorWallCollection() {
        Scanner scanner = new Scanner(System.in);

        NET_IN_WM_LOAD_COLLECTION inLoad = new NET_IN_WM_LOAD_COLLECTION();
        String line = "";
        while (true) {
            System.out.println("请输入电视墙ID：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }

        inLoad.nMonitorWallID = Integer.parseInt(line);  //电视墙ID

        // 预案名称
        String name = "预案1";   // 根据查到的预案名称来载入
        inLoad.pszName = ToolKits.GetGBKStringToPointer(name);

        NET_OUT_WM_LOAD_COLLECTION outLoad = new NET_OUT_WM_LOAD_COLLECTION();

        boolean bLoad = netsdkApi.CLIENT_LoadMonitorWallCollection(loginHandle, inLoad, outLoad, 3000);

        if (bLoad) {
            System.out.println("LoadMonitorWallCollection Succeed!");
        } else {
            System.err.println("LoadMonitorWallCollection Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 保存电视墙预案
     */
    public void SaveMonitorWallCollection() {
        Scanner scanner = new Scanner(System.in);

        NET_IN_WM_SAVE_COLLECTION stIn = new NET_IN_WM_SAVE_COLLECTION();
        //电视墙ID
        String line = "";
        while (true) {
            System.out.println("请输入电视墙ID：");
            line = scanner.nextLine();
            if (!line.equals("")) break;
        }


        stIn.nMonitorWallID = Integer.parseInt(line);

        // 预案名称
        String name = "预案666666";    // 在保存预案中，预案名称，自己设置
        stIn.pszName = ToolKits.GetGBKStringToPointer(name);

        NET_OUT_WM_SAVE_COLLECTION stOut = new NET_OUT_WM_SAVE_COLLECTION();

        boolean bSave = netsdkApi.CLIENT_SaveMonitorWallCollection(loginHandle, stIn, stOut, 3000);

        if (bSave) {
            System.out.println("SaveMonitorWallCollection Succeed!");
        } else {
            System.err.println("SaveMonitorWallCollection Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    //////////////////////////////////////////// 电视墙使能 ///////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 查看电视墙使能状态
     */
    public void MonitorWallGetEnable() {

        NET_IN_MONITORWALL_GET_ENABLE stuGetIn = new NET_IN_MONITORWALL_GET_ENABLE();
        stuGetIn.nMonitorWallNum = -1;  // 查询全部
        NET_OUT_MONITORWALL_GET_ENABLE stuGetOut = new NET_OUT_MONITORWALL_GET_ENABLE();

        int emType = NET_MONITORWALL_OPERATE_TYPE.NET_MONITORWALL_OPERATE_GET_ENABLE.getValue();
        stuGetIn.write();
        stuGetOut.write();
        boolean ret = netsdkApi.CLIENT_OperateMonitorWall(loginHandle, emType, stuGetIn.getPointer(), stuGetOut.getPointer(), 3000);
        if (!ret) {
            System.err.println("Get Monitor Wall Enable failed! " + ToolKits.getErrorCode());
            return;
        }
        stuGetOut.read();
        System.out.println("Get Monitor Wall Enable succeed!");

        int retNum = stuGetOut.nMonitorWallNum;
        StringBuilder info = new StringBuilder().append(String.format("——————————共有电视墙数量[%2d]——————————\n", retNum));

        for (int i = 0; i < retNum; i++) {
            try {
                info.append(String.format("//——>第[%2d]个电视墙:\n", i))
                        .append("szName 名称: ").append(new String(stuGetOut.stuEnable[i].szName, encode)).append("\n")
                        .append("bEnable 使能状态: ").append(stuGetOut.stuEnable[i].bEnable == 0 ? "未使能" : "使能").append("\n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println(info.toString());
    }

    /**
     * 设置电视墙使能状态
     * 电视墙设置时以 电视墙名称szName 作为匹配项
     * 本用例先获取到所有电视墙，再根据控制台输入的szName名称修改对应电视墙的启用状态
     */
    public void MonitorWallSetEnable() {
        // 获取电视墙使能状态
        MonitorWallGetEnable();

        Scanner sc = new Scanner(System.in);
        System.out.println("请输入电视墙名称:");
        byte[] szName = new byte[0];
        try {
            szName = sc.next().trim().getBytes(encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("使能/不使能? y/n");
        int enable = sc.next().toLowerCase().equals("y") ? 1 : 0;

        NET_IN_MONITORWALL_SET_ENABLE stuSetIn = new NET_IN_MONITORWALL_SET_ENABLE();
        stuSetIn.nMonitorWallNum = 1;
        stuSetIn.stuEnable[0].bEnable = enable;
        System.arraycopy(szName, 0, stuSetIn.stuEnable[0].szName, 0, szName.length);

        NET_OUT_MONITORWALL_SET_ENABLE stuSetOut = new NET_OUT_MONITORWALL_SET_ENABLE();

        int emType = NET_MONITORWALL_OPERATE_TYPE.NET_MONITORWALL_OPERATE_SET_ENABLE.getValue();

        stuSetIn.write();
        stuSetOut.write();
        boolean ret = netsdkApi.CLIENT_OperateMonitorWall(loginHandle, emType, stuSetIn.getPointer(), stuSetOut.getPointer(), 3000);
        if (!ret) {
            System.err.println("Set Monitor Wall Enable failed! " + ToolKits.getErrorCode());
        }
        System.out.println("Set Monitor Wall Enable succeed!");
    }

    ///////////////////////////////////////// 获取电视墙配置 优化方法 //////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static AV_CFG_MonitorWall monitorWall = new AV_CFG_MonitorWall();

    static {
        monitorWall.write();
    }

    /**
     * 获取电视墙配置
     */
    public void GetMonitorWallNew() {

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
        for (int i = 0; i < retNum; i++) {
            GetMonitorWallConfigData(monitorWall, sizeOfWall, dataPointer, offset);
            offset += sizeOfWall;

            // 如果获取到的电视墙个数为2，那么电视墙ID为[0, 1],  电视墙有效时，才能查询到预案
            try {
                System.out.print("[电视墙名称:" + new String(monitorWall.szName, "GBK").trim());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            System.out.print("  使能：" + (monitorWall.bDisable == 0 ? "有效" : "无效"));
            System.out.print("  电视墙ID：" + i + "]" + "\n");
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

    /////////////////////////////////////////// 电视墙RTSP推流命令 ////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 输出通道中的每个窗口都可以设置一个视频输入通道, 使视频在输出通道的对应位置输出.
     * 设置显示源可以针对一个窗口也可以是一组窗口.
     * 窗口序号大于等于0时,表示设置其中一个窗口; 窗口序号小于0时, 就意味着设置一组窗口.
     */
    public void SetSplitSourceByRTSP() {

        boolean bRet;

        NET_SPLIT_SOURCE stSplitSource = new NET_SPLIT_SOURCE();
        stSplitSource.bEnable = 1; // true 使能

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
        // rtsp://admin:admin123@10.11.16.71:554/cam/realmonitor?channel=1&subtype=0
        byte[] szMainStreamUrl = sc.next().trim().getBytes();
        System.arraycopy(szMainStreamUrl, 0, stSplitSource.szMainStreamUrl, 0, szMainStreamUrl.length);

        // 获取解码器视频输出通道号
        GetChannelInfo(loginHandle);

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
    public void TestGetChannelInfo() {
        GetChannelInfo(loginHandle);
    }

    // 获取解码器视频输出通道号
    public void GetChannelInfo(LLong loginHandle) {

        boolean bRet;

        NET_PRODUCTION_DEFNITION stuDef = new NET_PRODUCTION_DEFNITION();
        bRet = netsdkApi.CLIENT_QueryProductionDefinition(loginHandle, stuDef, 5000);
        if (!bRet) {
            System.err.println("CLIENT_QueryProductionDefinition failed. ErrorCode:" + ToolKits.getErrorCode());
            return;
        }

        System.out.println("视频输出通道总数: " + stuDef.nVideoOutChannel);

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
                return;
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
                System.out.println(String.format("拼接屏[%03d]包含虚拟通道数:%d", i, stComposite.nVirtualChannel));
            }
        } else {
            System.err.println("查询融合屏通道信息失败: " + ToolKits.getErrorCode());
        }
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


    ////////////////////////////////////////////////////////////////
    // M70的登陆信息
    String m_strIp = "172.26.1.57";
    int m_nPort = 37777;
    String m_strUser = "admin";
    String m_strPassword = "admin111";
    ////////////////////////////////////////////////////////////////

    public void RunTest() {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "查询前端相机信息", "GetMatrixCamerasInfo"));
        menu.addItem(new CaseMenu.Item(this, "查询通道号列表", "GetVideoOutChannelInfo"));
        menu.addItem(new CaseMenu.Item(this, "查询窗口总数量", "GetWhnCount"));
        menu.addItem(new CaseMenu.Item(this, "开窗", "OpenSplitWindow"));
        menu.addItem(new CaseMenu.Item(this, "关窗", "CloseSplitWindow"));
        menu.addItem(new CaseMenu.Item(this, "配置视频上墙", "SetSplitSource"));
        menu.addItem(new CaseMenu.Item(this, "获取电视墙ID", "GetMonitorWall"));
        menu.addItem(new CaseMenu.Item(this, "获取电视墙预案", "GetMonitorWallCollections"));
        menu.addItem(new CaseMenu.Item(this, "载入电视墙预案", "LoadMonitorWallCollection"));
        menu.addItem(new CaseMenu.Item(this, "获取加载预案的每个屏的窗口数量", "getCollectWindowCount"));
        menu.addItem(new CaseMenu.Item(this, "保存电视墙预案", "SaveMonitorWallCollection"));

        menu.addItem(new CaseMenu.Item(this, "查看电视墙使能状态", "MonitorWallGetEnable"));
        menu.addItem(new CaseMenu.Item(this, "设置电视墙使能状态", "MonitorWallSetEnable"));

        menu.addItem(new CaseMenu.Item(this, "获取电视墙配置Ex(速度优化)", "GetMonitorWallNew"));

        menu.addItem(new CaseMenu.Item(this, "电视墙RTSP推流命令", "SetSplitSourceByRTSP"));
        menu.addItem(new CaseMenu.Item(this, "获取解码器视频输出通道号", "TestGetChannelInfo"));
        menu.addItem(new CaseMenu.Item(this, "获取某个输出通道上的窗口数量", "TestGetWndsCount"));
        menu.run();
    }

    public static void main(String[] args) {
        MonitorWall demo = new MonitorWall();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
