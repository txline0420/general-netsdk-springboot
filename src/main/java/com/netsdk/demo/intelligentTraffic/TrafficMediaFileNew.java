package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_DOWNLOAD_FILE_TYPE;
import com.netsdk.lib.structure.NET_DOWNLOADFILE_INFO;
import com.netsdk.lib.structure.NET_IN_DOWNLOAD_MULTI_FILE;
import com.netsdk.lib.structure.NET_OUT_DOWNLOAD_MULTI_FILE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.netsdk.lib.NetSDKLib.*;

/**
 * 智能交通设备
 * 查询并下载抓拍图片
 *
 * @author 47040
 */
public class TrafficMediaFileNew {

    static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;

    /**
     * 登录句柄
     */
    private LLong loginHandle = new LLong(0);

    /**
     * 设备信息
     */
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();

    public static void printMessage(String msg) {
        System.out.println("[ INFO ] >> " + msg);
    }

    /**
     * 获取文件数量
     *
     * @param findHandle 查询句柄
     * @return 失败返回  -1 成功返回 实际数量
     */
    public int getTotalFiles(LLong findHandle) {
        IntByReference nCount = new IntByReference(0);
        boolean bRet = netsdkApi.CLIENT_GetTotalFileCount(findHandle, nCount, null, 3000);
        if (!bRet) {
            System.err.println("GetTotalFileCount failed! " + ToolKits.getErrorCode());
            return -1;
        }

        System.out.println("The Total File: " + nCount.getValue());

        return nCount.getValue();
    }

    /**
     * 下载进度
     */
    public static class Download implements fMultiFileDownLoadPosCB {
        private static final Download _instance = new Download();
        private static final String _lock = "DownLoadPos";

        public static Download getInstance() {
            return _instance;
        }

        private Download() {
        }

        @Override
        public void invoke(LLong lDownLoadHandle, int dwID, int dwFileTotalSize, int dwDownLoadSize, int nError, Pointer dwUser, Pointer pReserved) {
            System.out.println(String.format("lDownLoadHandle-dwID:%d-%d Total:%d, downloaded:%d nErrorCode:%d",
                    lDownLoadHandle.longValue(), dwID, dwFileTotalSize, dwDownLoadSize, nError));
            if (dwDownLoadSize == -1) {
                System.out.println(String.format("lDownLoadHandle-dwID:%d-%d download completed.",
                        lDownLoadHandle.longValue(), dwID));
            }
        }
    }

    /**
     * 批量下载交通图片
     */
    public boolean downloadImage(LLong hLogin, List<String> fetchPaths, List<String> savePaths) {

        // 入参
        NET_IN_DOWNLOAD_MULTI_FILE stuMultiIn = new NET_IN_DOWNLOAD_MULTI_FILE();
        stuMultiIn.emDownloadType = EM_DOWNLOAD_FILE_TYPE.EM_DOWNLOAD_BY_FILENAME.getValue();
        stuMultiIn.nFileCount = fetchPaths.size();

        NET_DOWNLOADFILE_INFO[] downloadInfos = new NET_DOWNLOADFILE_INFO[stuMultiIn.nFileCount];
        for (int i = 0; i < stuMultiIn.nFileCount; i++) {
            downloadInfos[i] = new NET_DOWNLOADFILE_INFO();
            downloadInfos[i].dwFileID = i + 1;       // 和回调函数里的ID一一对应
            byte[] bFetchPath = fetchPaths.get(i).getBytes();
            byte[] bSavePath = savePaths.get(i).getBytes();
            System.arraycopy(bFetchPath, 0, downloadInfos[i].szSourceFilePath, 0, bFetchPath.length);
            System.arraycopy(bSavePath, 0, downloadInfos[i].szSavedFileName, 0, bSavePath.length);
        }

        Pointer pFileInfos = new Memory(downloadInfos[0].size() * stuMultiIn.nFileCount);
        ToolKits.SetStructArrToPointerData(downloadInfos, pFileInfos);

        stuMultiIn.pFileInfos = pFileInfos;
        stuMultiIn.cbPosCallBack = Download.getInstance();

        // 出参
        NET_OUT_DOWNLOAD_MULTI_FILE stuMultiOut = new NET_OUT_DOWNLOAD_MULTI_FILE();

        stuMultiIn.write();
        stuMultiOut.write();
        boolean ret = netsdkApi.CLIENT_DownLoadMultiFile(hLogin, stuMultiIn.getPointer(), stuMultiOut.getPointer(), 5000);
        if (!ret) {
            System.err.println("下载接口调用失败: " + ToolKits.getErrorCode());
            return false;
        }
        stuMultiOut.read();
        LLong hDownload = stuMultiOut.lDownLoadHandle;      // 和回调函数里的 lDownLoadHandle 一一对应
        System.out.println("下载句柄: " + hDownload.longValue());

        return true;
    }


    public void findMediaFile() {
        if (loginHandle.longValue() == 0) {
            System.err.println("Please Login First");
            return;
        }

        final int queryType = EM_FILE_QUERY_TYPE.NET_FILE_QUERY_TRAFFICCAR;
        MEDIA_QUERY_TRAFFICCAR_PARAM condition = new MEDIA_QUERY_TRAFFICCAR_PARAM();

        condition.nChannelID = -1;     // 通道号  -1代表所有通道
        condition.nMediaType = 1;      // 文件类型 0:任意类型, 1:jpg图片, 2:dav文件
        condition.byFileFlag = 0;      // 设置 文件标志, 0xFF-使用nFileFlagEx, 0-表示所有录像, 1-定时文件, 2-手动文件, 3-事件文件, 4-重要文件, 5-合成文件
        condition.byRandomAccess = 1;  // 设置 是否需要在查询过程中随意跳转，0-不需要，1-需要
        condition.byLane = -1;         // 查询相应车道信息  -1代表所有车道
        condition.nDirection = -1;     // 查询车开往的方向  -1代表所有方向

        condition.StartTime.setTime(2020, 12, 23, 20, 30, 0); // 设置 开始时间
        condition.EndTime.setTime(2020, 12, 23, 20, 39, 59); // 设置 结束时间

        condition.nEventTypeNum = 1; // 设置需要查询的违章事件类型个数
        IntByReference nEventType = new IntByReference(EVENT_IVS_ALL);  // 全部
        condition.pEventTypes = nEventType.getPointer();

        // 设置查询类型 获取查询句柄
        condition.write();
        LLong findHandle = netsdkApi.CLIENT_FindFileEx(loginHandle, queryType, condition.getPointer(), null, 3000);
        condition.read();
        if (findHandle.longValue() == 0) {
            System.err.println("Failed to Find File! " + ToolKits.getErrorCode());
            return;
        }

        // 获取违章总数
        int totalCount = getTotalFiles(findHandle);
        int queryCount = Math.min(10, totalCount); //每次获取的违章数量
        MEDIAFILE_TRAFFICCAR_INFO[] mediaFileInfos = new MEDIAFILE_TRAFFICCAR_INFO[queryCount];
        for (int i = 0; i < mediaFileInfos.length; ++i) {
            mediaFileInfos[i] = new MEDIAFILE_TRAFFICCAR_INFO();
        }

        final int memorySize = mediaFileInfos[0].size() * queryCount;
        Pointer mediaFileMemory = new Memory(memorySize);
        mediaFileMemory.clear(memorySize);

        //循环查询
        int nCurCount = 0;
        do {
            // 查询
            ToolKits.SetStructArrToPointerData(mediaFileInfos, mediaFileMemory);
            int nRet = netsdkApi.CLIENT_FindNextFileEx(findHandle, queryCount, mediaFileMemory, memorySize, null, 3000);
            ToolKits.GetPointerDataToStructArr(mediaFileMemory, mediaFileInfos);
            if (nRet < 0) {
                System.err.println("FindNextFileEx failed! " + ToolKits.getErrorCode());
                break;
            }

            //更多信息输出，可以查看结构体MEDIAFILE_TRAFFICCAR_INFO_EX中的MEDIAFILE_TRAFFICCAR_INFO

            // 地址列表
            List<String> fetchPaths = new ArrayList<>();
            List<String> savePaths = new ArrayList<>();

            for (int j = 0; j < nRet; j++) {
                ++nCurCount;

                MEDIAFILE_TRAFFICCAR_INFO info = mediaFileInfos[j];
                printMessage("-------------------------" + nCurCount + "---------------------------");
                printMessage("通道号: " + info.ch);
                printMessage("违章类型: " + info.nEvents[0]);
                printMessage("车道号: " + info.byLane);
                String plateNumber = "";
                try {
                    plateNumber = new String(info.szPlateNumber, "GBK").trim();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                plateNumber = plateNumber.isEmpty() ? "无牌车" : plateNumber;
                printMessage("车牌号码: " + plateNumber);
                printMessage("车牌颜色: " + new String(info.szPlateColor).trim());
                printMessage("车牌类型: " + new String(info.szPlateType).trim());
                printMessage("车身颜色: " + new String(info.szVehicleColor).trim());
                printMessage("抓拍时间: " + info.stSnapTime.toStringTime());
                printMessage("事件组ID: " + info.nGroupID);
                printMessage("该组ID抓怕张数: " + info.byCountInGroup);
                printMessage("该组ID抓怕索引: " + info.byIndexInGroup);
                printMessage("图片地址: " + new String(info.szFilePath));
                printMessage("包含事件数: " + info.nEventsNum);
                printMessage("事件枚举: " + String.format("0x%08x", info.nEvents[0]));

                // 下载当前图片
                fetchPaths.add(new String(info.szFilePath));
                savePaths.add(String.format("Image_Channel_%d_%s_%d.jpg", info.ch, info.stSnapTime.toString(), info.nGroupID));
            }
            if (!downloadImage(loginHandle, fetchPaths, savePaths)) {
                System.err.println("download filed! " + ToolKits.getErrorCode());
                break;
            }
        } while ((totalCount -= queryCount) > 0);

        netsdkApi.CLIENT_FindCloseEx(findHandle);
    }

    public void initTest() {
        // 初始化SDK
        netsdkApi.CLIENT_Init(null, null);

        //打开日志，可选
        LOG_SET_PRINT_INFO logInfo = new LOG_SET_PRINT_INFO();
        File path = new File(".");
        String logPath = path.getAbsoluteFile().getParent() + TrafficMediaFileNew.class.getName().toLowerCase() + ".log";

        logInfo.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, logInfo.szLogFilePath, 0, logPath.getBytes().length);

        logInfo.bSetPrintStrategy = 1;
        logInfo.nPrintStrategy = 0;
        boolean bLogopen = netsdkApi.CLIENT_LogOpen(logInfo);
        if (!bLogopen) {
            System.err.println("Failed to open NetSDK log");
        }

        int nSpecCap = EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP; // TCP 方式登录
        final IntByReference error = new IntByReference();
        loginHandle = netsdkApi.CLIENT_LoginEx2(address, (short) port, username,
                password, nSpecCap, null, deviceInfo, error);
        if (loginHandle.longValue() == 0) {
            System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
            return;
        }

        System.out.printf("Login Device [%s:%d] Success. \n", address, port);
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "findMediaFile", "findMediaFile"));

        menu.run();
    }

    public void endTest() {
        if (loginHandle.longValue() != 0) {
            netsdkApi.CLIENT_Logout(loginHandle);
            System.out.println("Logout Device " + address);
        }

        netsdkApi.CLIENT_Cleanup();
    }


    private final String username = "admin";
    private final String password = "dahua2020";
    private final String address = "10.80.9.45";
    private final int port = 37777;

    public static void main(String[] args) {

        TrafficMediaFileNew demo = new TrafficMediaFileNew();
        demo.initTest();
        demo.RunTest();
        demo.endTest();
    }
}
