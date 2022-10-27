package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_RECORD_STATUS;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 查找文件信息demo
 */
public class FindFilePictureAndRecord {

    static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

    private final NetSDKLib.NET_DEVICEINFO_Ex deviceinfo = new NetSDKLib.NET_DEVICEINFO_Ex();
    private static LLong loginHandle = new LLong(0);   //登陆句柄

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }


    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    public static class fDisConnectCB implements NetSDKLib.fDisConnect {
        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
        }
    }

    // 网络连接恢复，设备重连成功回调
    // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
    public static class HaveReConnect implements NetSDKLib.fHaveReConnect {
        public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
        }
    }

    private final fDisConnectCB m_DisConnectCB = new fDisConnectCB();
    private final HaveReConnect haveReConnect = new HaveReConnect();

    /**
     * 查找录像，需要指定查询录像的最大个数 除非 FindFileEx 无法使用 否则不建议使用这个接口
     */
    public void QueryRecordFile() {
        NetSDKLib.NET_TIME stTimeStart = new NetSDKLib.NET_TIME();
        stTimeStart.dwYear = 2021;
        stTimeStart.dwMonth = 1;
        stTimeStart.dwDay = 1;
        stTimeStart.dwHour = 0;
        stTimeStart.dwMinute = 0;
        stTimeStart.dwSecond = 0;

        NetSDKLib.NET_TIME stTimeEnd = new NetSDKLib.NET_TIME();
        stTimeEnd.dwYear = 2021;
        stTimeEnd.dwMonth = 1;
        stTimeEnd.dwDay = 31;
        stTimeEnd.dwHour = 23;
        stTimeEnd.dwMinute = 59;
        stTimeEnd.dwSecond = 59;

        //**************按时间查找视频文件**************
        int nFileCount = 50; //每次查询的最大文件个数
        NetSDKLib.NET_RECORDFILE_INFO[] numberFile = (NetSDKLib.NET_RECORDFILE_INFO[]) new NetSDKLib.NET_RECORDFILE_INFO().toArray(nFileCount);
        int maxlen = nFileCount * numberFile[0].size();

        IntByReference outFileCoutReference = new IntByReference(0);

        boolean cRet = netsdkApi.CLIENT_QueryRecordFile(loginHandle, 0, 0, stTimeStart, stTimeEnd, null, numberFile, maxlen, outFileCoutReference, 5000, false);

        if (cRet) {
            System.out.println("QueryRecordFile  Succeed! \n" + "查询到的视频个数：" + outFileCoutReference.getValue()
                    + "\n" + "码流类型：" + numberFile[0].bRecType);
            for (int i = 0; i < outFileCoutReference.getValue(); i++) {
                System.out.println("【" + i + "】：");
                System.out.println("开始时间：" + numberFile[i].starttime);
                System.out.println("结束时间：" + numberFile[i].endtime);
                System.out.println("通道号：" + numberFile[i].ch);
            }

        } else {
            System.err.println("QueryRecordFile  Failed!" + netsdkApi.CLIENT_GetLastError());
        }
    }

    /**
     * 查询图片
     */
    public void QueryPictureByFindFileEx() {

        System.out.println("查询设备保存的jpg图片");

        Scanner sc = new Scanner(System.in);

        System.out.println("请输入通道号 从0开始 -1表示全部:");
        int channel = Integer.parseInt(sc.nextLine());

        System.out.println("请输入起始时间 yyyy-MM-dd HH:mm:ss");
        String startStr = sc.nextLine().trim();
        NetSDKLib.NET_TIME startTime = GetNetTime(startStr);
        if (startTime == null) return;

        System.out.println("请输入结束时间 yyyy-MM-dd HH:mm:ss");
        String endStr = sc.nextLine().trim();
        NetSDKLib.NET_TIME endTime = GetNetTime(endStr);
        if (endTime == null) return;

        // 文件类型 1:查询jpg图片,2:查询dav
        FindFileEx(channel, 1, startTime, endTime);
    }

    /**
     * 查询录像
     */
    public void QueryRecordByFindFileEx() {

        System.out.println("查询设备保存的dev录像文件");

        Scanner sc = new Scanner(System.in);

        System.out.println("请输入通道号 从0开始 -1表示全部:");
        int channel = Integer.parseInt(sc.nextLine());

        System.out.println("请输入起始时间 yyyy-MM-dd HH:mm:ss");
        String startStr = sc.nextLine().trim();
        NetSDKLib.NET_TIME startTime = GetNetTime(startStr);
        if (startTime == null) return;

        System.out.println("请输入结束时间 yyyy-MM-dd HH:mm:ss");
        String endStr = sc.nextLine().trim();
        NetSDKLib.NET_TIME endTime = GetNetTime(endStr);
        if (endTime == null) return;

        // 文件类型 1:查询jpg图片,2:查询dav
        FindFileEx(channel, 2, startTime, endTime);
    }

    private static NetSDKLib.NET_TIME GetNetTime(String dateTime) {
        NetSDKLib.NET_TIME start = new NetSDKLib.NET_TIME();
        try {
            int[] date = Arrays.stream(dateTime.split("\\s+")[0].split("-")).mapToInt(Integer::valueOf).toArray();
            int[] time = Arrays.stream(dateTime.split("\\s+")[1].split(":")).mapToInt(Integer::valueOf).toArray();
            start.setTime(date[0], date[1], date[2], time[0], time[1], time[2]);
        } catch (Exception e) {
            System.err.println("输入格式错误");
            return null;
        }
        return start;
    }

    /**
     * 查询 图片/录像 文件
     */
    public void FindFileEx(int channel, int fileType, NetSDKLib.NET_TIME startTime, NetSDKLib.NET_TIME endTime) {

        int type = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FILE;

        // 查询条件
        NetSDKLib.NET_IN_MEDIA_QUERY_FILE queryCondition = new NetSDKLib.NET_IN_MEDIA_QUERY_FILE();

        // 工作目录列表,一次可查询多个目录,为空表示查询所有目录。
        // 目录之间以分号分隔,如“/mnt/dvr/sda0;/mnt/dvr/sda1”,szDirs==null 或"" 表示查询所有
        queryCondition.szDirs = "";

        // 文件类型 1:查询jpg图片,2:查询dav
        queryCondition.nMediaType = fileType;

        // 通道号从0开始,-1表示查询所有通道
        queryCondition.nChannelID = channel;

        // 开始时间
        queryCondition.stuStartTime = startTime;

        // 结束时间
        queryCondition.stuEndTime = endTime;

        queryCondition.write();
        LLong lFindHandle = netsdkApi.CLIENT_FindFileEx(loginHandle, type, queryCondition.getPointer(), null, 3000);
        if (lFindHandle.longValue() == 0) {
            System.err.println("FindFileEx Failed!" + getErrorCode());
            return;
        }
        queryCondition.read();

        ///////////////////////////
        int nMaxCount = 10;  // 每次查询的个数，循环查询
        NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[] pMediaQueryFile = new NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[nMaxCount];
        for (int i = 0; i < pMediaQueryFile.length; ++i) {
            pMediaQueryFile[i] = new NetSDKLib.NET_OUT_MEDIA_QUERY_FILE();
        }

        int MemorySize = pMediaQueryFile[0].size() * nMaxCount;
        Pointer mediaFileInfo = new Memory(MemorySize);
        mediaFileInfo.clear(MemorySize);

        ToolKits.SetStructArrToPointerData(pMediaQueryFile, mediaFileInfo);

        //循环查询
        int nCurCount = 0;
        int nFindCount = 0;
        while (true) {
            int nRet = netsdkApi.CLIENT_FindNextFileEx(lFindHandle, nMaxCount, mediaFileInfo, MemorySize, null, 3000);
            ToolKits.GetPointerDataToStructArr(mediaFileInfo, pMediaQueryFile);
            System.out.println("nRet : " + nRet);

            if (nRet <= 0) {
                break;
            }

            for (int i = 0; i < nRet; i++) {
                nFindCount = i + nCurCount * nMaxCount;
                System.out.println("[" + nFindCount + "]通道号 :" + pMediaQueryFile[i].nChannelID);
                System.out.println("[" + nFindCount + "]开始时间 :" + pMediaQueryFile[i].stuStartTime.toStringTime());
                System.out.println("[" + nFindCount + "]结束时间 :" + pMediaQueryFile[i].stuEndTime.toStringTime());
                if (pMediaQueryFile[i].byFileType == 1) {
                    System.out.println("[" + nFindCount + "]文件类型 : jpg图片");
                } else if ((pMediaQueryFile[i].byFileType == 2)) {
                    System.out.println("[" + nFindCount + "]文件类型 : dav");
                }
                System.out.println("[" + nFindCount + "]文件路径 :" + new String(pMediaQueryFile[i].szFilePath).trim());
            }

            if (nRet < nMaxCount) {
                break;
            } else {
                nCurCount++;
            }
        }

        // 关闭查询
        netsdkApi.CLIENT_FindCloseEx(lFindHandle);
    }

    /**
     * 指定月份 查询当月每日的录像状况 录像类型指定: 所有类型
     */
    public void QueryAllRecordStatusInSpecifiedMonth() {

        Scanner sc = new Scanner(System.in);

        System.out.println("请输入通道号 从0开始 -1表示全部:");
        int channel = Integer.parseInt(sc.nextLine());

        System.out.println("请指定月份 yyyy-MM");
        String monthStr = sc.nextLine().trim();
        NetSDKLib.NET_TIME month = GetNetMonth(monthStr);
        if (month == null) return;

        int recordType = NetSDKLib.EM_QUERY_RECORD_TYPE.EM_RECORD_TYPE_ALL;   // 查询所有录像

        try {
            QueryRecordStatus(channel, month, recordType, null);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定月份 查询当月每日的录像状况 录像类型指定: 卡号查询
     * 只有部分 HB-U、NVS设备的定制版本支持
     */
    public void QueryCardRecordStatusInSpecifiedMonth() {

        Scanner sc = new Scanner(System.in);

        System.out.println("请输入通道号 从0开始 -1表示全部:");
        int channel = sc.nextInt();

        System.out.println("请指定月份 yyyy-MM");
        String monthStr = sc.nextLine().trim();
        NetSDKLib.NET_TIME month = GetNetMonth(monthStr);
        if (month == null) return;

        System.out.println("请输入卡号 最长59个字节");
        String cardId = sc.nextLine().trim();
        if (cardId.length() > 59) {
            System.err.println("卡号长度超过59字节");
            return;
        }

        int recordType = NetSDKLib.EM_QUERY_RECORD_TYPE.EM_RECORD_TYPE_CARD;   // 卡号查询

        try {
            QueryRecordStatus(channel, month, recordType, cardId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定月份 查询当月每日的录像状况 录像类型指定: 按字段查询
     * 只有部分 HB-U、NVS设备的定制版本支持
     */
    public void QueryFieldRecordStatusInSpecifiedMonth() {

        Scanner sc = new Scanner(System.in);

        System.out.println("请输入通道号 从0开始 -1表示全部:");
        int channel = sc.nextInt();

        System.out.println("请指定月份 yyyy-MM");
        String monthStr = sc.nextLine().trim();
        NetSDKLib.NET_TIME month = GetNetMonth(monthStr);
        if (month == null) return;

        System.out.println("请输入字段 最长256个字节");
        String field = sc.nextLine().trim();
        if (field.length() > 256) {
            System.err.println("字段长度超过256字节");
            return;
        }

        int recordType = NetSDKLib.EM_QUERY_RECORD_TYPE.EM_RECORD_TYPE_FIELD;   // 按字段查询

        try {
            QueryRecordStatus(channel, month, recordType, field);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void QueryRecordStatus(int channel, NetSDKLib.NET_TIME monthTime, int recordType, String cardId) throws UnsupportedEncodingException {

        // recordFileType 枚举不为 EM_RECORD_TYPE_CARD 或 EM_RECORD_TYPE_FIELD 时 pCardId 无效
        Pointer pCardId = null;
        if (recordType == NetSDKLib.EM_QUERY_RECORD_TYPE.EM_RECORD_TYPE_CARD) {
            byte[] bCardId = cardId.getBytes(encode);
            pCardId = new Memory(59);
            pCardId.clear(59);
            pCardId.write(0, bCardId, 0, bCardId.length);
        } else if (recordType == NetSDKLib.EM_QUERY_RECORD_TYPE.EM_RECORD_TYPE_FIELD) {
            byte[] bCardId = cardId.getBytes(encode);
            pCardId = new Memory(256);
            pCardId.clear(256);
            pCardId.write(0, bCardId, 0, bCardId.length);
        }

        NET_RECORD_STATUS status = new NET_RECORD_STATUS();

        monthTime.write();
        status.write();
        boolean ret = netsdkApi.CLIENT_QueryRecordStatus(loginHandle, channel, recordType,
                monthTime.getPointer(), pCardId, status.getPointer(), 3000);
        if (!ret) {
            System.err.println("查询月录像状态失败:" + getErrorCode());
            return;
        }
        status.read();

        StringBuilder recordStatus = new StringBuilder().append(String.format("%4d年%2d月的录像状态:", monthTime.dwYear, monthTime.dwMonth)).append("\n");
        for (int i = 0; i < status.flag.length; i++) {
            // 请注意 flag[0] 是当月的第1天
            recordStatus.append(String.format("    第%2d天: %s", i + 1, (status.flag[i] == 0 ? "不存在" : "存在"))).append("\n");
        }
        System.out.println(recordStatus.toString());

    }

    private static NetSDKLib.NET_TIME GetNetMonth(String month) {
        NetSDKLib.NET_TIME start = new NetSDKLib.NET_TIME();
        try {
            int[] date = Arrays.stream(month.split("-")).mapToInt(Integer::valueOf).toArray();
            start.setTime(date[0], date[1], 0, 0, 0, 0);
        } catch (Exception e) {
            System.err.println("输入格式错误");
            return null;
        }
        return start;
    }

    /**
     * 获取接口错误码
     */
    public static String getErrorCode() {
        return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff) + " ). 参考  NetSDKLib.java }";
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
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
        netsdkApi.CLIENT_SetNetworkParam(netParam);

        // 打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();

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

        // 向设备登入
        int nSpecCap = 0;
        Pointer pCapParam = null;
        IntByReference nError = new IntByReference(0);
        loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
                m_strPassword, nSpecCap, pCapParam, deviceinfo, nError);

        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Port[%d]Success!\n", m_strIp, m_nPort);
        } else {
            System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[0x%x]\n", m_strIp, m_nPort, netsdkApi.CLIENT_GetLastError());
            EndTest();
        }
    }

    public void RunTest() {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();

        menu.addItem(new CaseMenu.Item(this, "QueryPictureByFindFileEx               通过FindFileEx接口查询图片", "QueryPictureByFindFileEx"));
        menu.addItem(new CaseMenu.Item(this, "QueryRecordByFindFileEx                通过FindFileEx接口查询录像", "QueryRecordByFindFileEx"));
        menu.addItem(new CaseMenu.Item(this, "QueryAllRecordStatusInSpecifiedMonth   查询当月录像状况: 所有录像", "QueryAllRecordStatusInSpecifiedMonth"));
        menu.run();
    }

    public void EndTest() {
        System.out.println("End Test");
        if (loginHandle.longValue() != 0) {
            netsdkApi.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netsdkApi.CLIENT_Cleanup();
        System.exit(0);
    }

    ////////////////////////////////////////////////////////////////
    String m_strIp = "172.23.12.29";
    int m_nPort = 37777;
    String m_strUser = "admin";
    String m_strPassword = "admin111";
    ////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        FindFilePictureAndRecord demo = new FindFilePictureAndRecord();

        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
