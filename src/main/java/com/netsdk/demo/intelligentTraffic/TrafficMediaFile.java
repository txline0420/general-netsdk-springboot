package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.util.Testable;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;

import static com.netsdk.lib.NetSDKLib.*;

/**
 * 适用于 ITSE 智能交通设备
 * 查询并下载抓拍图片及录像
 *
 * @author 29779
 */
public class TrafficMediaFile implements Testable {
    static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;

    /**
     * 设备信息
     */
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();

    /**
     * 登录句柄
     */
    private LLong loginHandle = new LLong(0);
    private String username = "admin";
    private String password = "dahua2020";
    private String address = "10.80.9.45";
    private int port = 37777;

    /**
     * 返回错误码, 具体值参照 NetSDKLib.java NET_NOERROR
     */
    public static void printLastError(String msg) {
        System.err.printf("[ ERROR ] %s, Error Code [%s]\n", msg, ToolKits.getErrorCode());
    }

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
            printLastError("GetTotalFileCount failed!");
            return -1;
        }

        System.out.println("The Total File: " + nCount.getValue());

        return nCount.getValue();
    }

    /**
     * 交通违章事件转化
     *
     * @param eventType 违章码
     * @return 转成字符串
     */
    public static String eventToString(int eventType) {
        switch (eventType) {
            case NetSDKLib.EVENT_IVS_TRAFFIC_TOLLGATE:
                return "卡口";    // 交通违章-卡口事件----新规则
            case NetSDKLib.EVENT_IVS_TRAFFICJUNCTION:
                return "路口"; // 交通路口事件
            case NetSDKLib.EVENT_IVS_TRAFFIC_RUNREDLIGHT:
                return "闯红灯"; // 交通违章-闯红灯事件
            case NetSDKLib.EVENT_IVS_TRAFFIC_OVERLINE:
                return "压车道"; // 交通违章-压车道线事件
            case NetSDKLib.EVENT_IVS_TRAFFIC_RETROGRADE:
                return "逆行"; // 交通违章-逆行事件
            case NetSDKLib.EVENT_IVS_TRAFFIC_OVERSPEED:
                return "超速"; // 交通违章-超速
            case NetSDKLib.EVENT_IVS_TRAFFIC_UNDERSPEED:
                return "低速"; // 交通违章-低速
            case NetSDKLib.EVENT_IVS_TRAFFIC_PARKING:
                return "违章停车"; // 交通违章-违章停车
            case NetSDKLib.EVENT_IVS_TRAFFIC_WRONGROUTE:
                return "不按车道行驶"; // 交通违章-不按车道行驶
            case NetSDKLib.EVENT_IVS_TRAFFIC_CROSSLANE:
                return "违章变道"; // 交通违章-违章变道
            case NetSDKLib.EVENT_IVS_TRAFFIC_OVERYELLOWLINE:
                return "压黄线"; // 交通违章-压黄线
            case NetSDKLib.EVENT_IVS_TRAFFIC_YELLOWPLATEINLANE:
                return "黄牌车占道"; // 交通违章-黄牌车占道事件
            case NetSDKLib.EVENT_IVS_TRAFFIC_PEDESTRAINPRIORITY:
                return "斑马线行人优先"; // 交通违章-斑马线行人优先事件
            default:
                return null;
        }
    }

    private static ConcurrentHashMap<Long, Boolean> imageDownloadStatus = new ConcurrentHashMap<>();

    /**
     * 下载进度
     */
    public static class Download implements fDownLoadPosCallBack {
        private static Download _instance = new Download();
        private static String _lock = "DownLoadPos";

        private Download() {
        }

        public static Download getInstance() {
            return _instance;
        }

        public void invoke(LLong lPlayHandle, int dwTotalSize,
                           int dwDownLoadSize, Pointer dwUser) {
            if (-1 == dwDownLoadSize) {
                try {
                    imageDownloadStatus.put(lPlayHandle.longValue(), true);
                    printMessage("Progress: Download OK");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                imageDownloadStatus.put(lPlayHandle.longValue(), false);
            }
        }

        /**
         * 下载交通图片
         *
         * @param hLogin
         * @param info
         * @param fileName
         * @return 失败返回 false 成功放回 true
         */
        public boolean downloadImage(LLong hLogin, MEDIAFILE_TRAFFICCAR_INFO info, String fileName) throws InterruptedException {
            int fileType = EM_FILE_QUERY_TYPE.NET_FILE_QUERY_TRAFFICCAR;
            return downloadMediaFile(hLogin, fileType, info, fileName);
        }

        /**
         * 根据下载的图片文件信息下载图片文件
         * （注意下载图片时，应该在一张图片下载完成后，再开始下载下一张图片）
         *
         * @param emType   文件查询条件
         * @param info     文件信息
         * @param fileName 保存的文件名
         * @return -1 error
         * @see emType EM_FILE_QUERY_TYPE
         */
        public boolean downloadMediaFile(LLong hLogin, int emType, SdkStructure info, String fileName) throws InterruptedException {
            if (hLogin.longValue() == 0) {
                printLastError("Please Login First");
                return false;
            }

            info.write();
            LLong hDownload = netsdkApi.CLIENT_DownloadMediaFile(hLogin, emType,
                    info.getPointer(), fileName, _instance, null, null);
            info.read();

            if (hDownload.longValue() == 0) {
                printLastError("Download " + fileName + "Failed");
                return false;
            }

            int count = 100;   // 最多循环100次, 即10s
            while ((!imageDownloadStatus.containsKey(hDownload.longValue()) || !imageDownloadStatus.get(hDownload.longValue()))
                    && count > 0) {
                Thread.sleep(100);
                count--;
            }
            imageDownloadStatus.remove(hDownload.longValue());

            printMessage("Download " + fileName + " Success");
            netsdkApi.CLIENT_StopDownloadMediaFile(hDownload);

            return true;
        }
    }


    public void findMediaFile() throws InterruptedException {
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

        condition.StartTime.setTime(2020, 12, 21, 21, 0, 0); // 设置 开始时间
        condition.EndTime.setTime(2020, 12, 21, 21, 30, 0); // 设置 结束时间

        condition.nEventTypeNum = 2; // 设置需要查询的违章事件类型个数
//        IntByReference nEventType = new IntByReference(EVENT_IVS_ALL);  // 全部
        int[] eventTypes = new int[condition.nEventTypeNum];
        eventTypes[0] = EVENT_IVS_TRAFFICJUNCTION;
        eventTypes[1] = EVENT_IVS_TRAFFIC_PARKINGSPACE_MANUALSNAP;
        Pointer pEventTypes = new Memory(Integer.SIZE * condition.nEventTypeNum);
        pEventTypes.write(0, eventTypes, 0, condition.nEventTypeNum);
        condition.pEventTypes = pEventTypes;

        // 设置查询类型 获取查询句柄
        condition.write();
        LLong findHandle = netsdkApi.CLIENT_FindFileEx(loginHandle, queryType, condition.getPointer(), null, 3000);
        condition.read();
        if (findHandle.longValue() == 0) {
            printLastError("Failed to Find File");
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
                printLastError("FindNextFileEx failed!");
                break;
            }

            //更多信息输出，可以查看结构体MEDIAFILE_TRAFFICCAR_INFO_EX中的MEDIAFILE_TRAFFICCAR_INFO
            for (int j = 0; j < nRet; j++) {
                ++nCurCount;

                MEDIAFILE_TRAFFICCAR_INFO info = mediaFileInfos[j];
                printMessage("-------------------------" + nCurCount + "---------------------------");
                printMessage("通道号: " + info.ch);
                printMessage("违章类型: " + eventToString(info.nEvents[0]));
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
                String fileName = String.format("Image_Channel_%d_%s_%d.jpg", info.ch, info.stSnapTime.toString(), info.nGroupID);
                if (!Download.getInstance().downloadImage(loginHandle, info, fileName)) {
                    totalCount = 0;
                    break;
                }
            }
        } while ((totalCount -= queryCount) > 0);

        netsdkApi.CLIENT_FindCloseEx(findHandle);
    }

    @Override
    public void initTest() {
        // 初始化SDK
        netsdkApi.CLIENT_Init(null, null);

        //打开日志，可选
        LOG_SET_PRINT_INFO logInfo = new LOG_SET_PRINT_INFO();
        File path = new File(".");
        String logPath = path.getAbsoluteFile().getParent() + TrafficMediaFile.class.getName().toLowerCase() + ".log";

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
        }

        System.out.printf("Login Device [%s:%d] Success. \n", address, port);
    }

    @Override
    public void runTest() throws InterruptedException {
        findMediaFile();
    }

    @Override
    public void endTest() {
        if (loginHandle.longValue() != 0) {
            netsdkApi.CLIENT_Logout(loginHandle);
            System.out.println("Logout Device " + address);
        }

        netsdkApi.CLIENT_Cleanup();
    }

    public static void main(String[] args) throws InterruptedException {

        TrafficMediaFile demo = new TrafficMediaFile();

        demo.initTest();

        demo.runTest();

        demo.endTest();
    }
}
