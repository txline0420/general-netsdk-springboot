package com.netsdk.demo.customize.courseRecord;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_COURSE_LOCK_TYPE;
import com.netsdk.lib.enumeration.EM_COURSE_RECORD_COMPRESSION_TYPE;
import com.netsdk.lib.enumeration.EM_COURSE_RECORD_TYPE;
import com.netsdk.lib.structure.*;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

import static com.netsdk.demo.customize.courseRecord.modules.CourseRecordModule.*;
import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/10/14 21:00
 */
public class DemoConsoleRecordManage {

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

    /////////////////////////////////////// 录像查询//////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    public int FindID = 0;     // 查询句柄
    public int total = 0;      // 总数

    // 这个结构体很大，new的特别慢，所以我写成静态
    private NET_OUT_QUERY_COURSEMEDIA_FILE stuQueryOut = new NET_OUT_QUERY_COURSEMEDIA_FILE();

    /**
     * 这里的录像查询条件为：
     * 1) 不区分是否锁定，查询全部
     * 2）不区分导播/互动，查询全部
     * 3）模糊查询关键字 "" 即全部
     * 4）时间 2020/10/10 0:0:0 - 2020/10/20 23:59:59
     */
    public void OpenQueryCourseMediaFileTest() {

        if (FindID != 0) {
            CloseQueryCourseMediaFileTest();     // 如果上一次查询没有关闭，那先关闭它
            return;
        }

        NET_IN_QUERY_COURSEMEDIA_FILEOPEN stuIn = new NET_IN_QUERY_COURSEMEDIA_FILEOPEN();
        stuIn.emCourseLockType = EM_COURSE_LOCK_TYPE.EM_COURSE_LOCK_TYPE_ALL.getValue();            // 所有类型，不区分是否锁定
        stuIn.emCourseRecordType = EM_COURSE_RECORD_TYPE.EM_COURSE_RECORD_TYPE_ALL.getValue();      // 所有类型，不区分是导播还是互动
        byte[] keyWords = new byte[0];
        try {
            keyWords = "".getBytes(encode);      // 模糊关键字
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.arraycopy(keyWords, 0, stuIn.szKeyWord, 0, keyWords.length);

        // 起止时间
        stuIn.stuStartTime = new NET_TIME(2020, 12, 1, 0, 0, 0);
        stuIn.stuEndTime = new NET_TIME(2020, 12, 1, 23, 59, 59);

        NET_OUT_QUERY_COURSEMEDIA_FILEOPEN stuOut = new NET_OUT_QUERY_COURSEMEDIA_FILEOPEN();

        boolean ret = OpenQueryCourseMediaFile(courseRecordLogon.m_hLoginHandle, stuIn, stuOut, 3000);

        if (!ret) {
            System.err.println("查询记录失败");
            return;
        }
        FindID = stuOut.nfindID;
        total = stuOut.ntotalNum;
        System.out.printf("开始查询成功,FindID:%d, 共查询到记录数:%d\n", FindID, total);
    }


    /**
     * 从设备获取查询数据
     */
    public void DoQueryCourseMediaFileTest() {

        if (FindID == 0) {
            System.err.println("请先开启查询");
            return;
        }

        NET_IN_QUERY_COURSEMEDIA_FILE stuIn = new NET_IN_QUERY_COURSEMEDIA_FILE();
        int maxCount = 10;          // 一次性获取记录的最大数量，这个参数要根据带宽状态和超时时间自行调整

        int offset = 0;
        for (int i = 0; i < (total / maxCount) + 1; i++) {
            stuIn.nfindID = FindID;     // 填写查询句柄
            stuIn.nOffset = offset;     // 查询偏移量
            stuIn.nCount = maxCount;    // 最大获取个数
            stuIn.write();
            stuQueryOut.writeField("dwSize");
            boolean ret = netsdk.CLIENT_DoQueryCourseMediaFile(courseRecordLogon.m_hLoginHandle, stuIn.getPointer(), stuQueryOut.getPointer(), 3000);
            if (!ret) {
                System.err.println("Query Course Media File failed!" + ToolKits.getErrorCode());
                System.err.println("获取记录失败！");
                return;
            }
            GetPointerDataToCourseMediaInfo(stuQueryOut);

            int retCount = stuQueryOut.nCountResult;     // 实际获取到的数量
            for (int j = 0; j < retCount; j++) {

                NET_COURSEMEDIA_FILE_INFO fileInfo = stuQueryOut.stuCourseMediaFile[j];
                int nID = fileInfo.nID;
                NET_COURSE_INFO courseInfo = fileInfo.stuCourseInfo;

                StringBuilder mediaInfo = new StringBuilder();
                try {
                    mediaInfo.append(String.format("\n————————————视频记录[%s]————————————\n", i * maxCount + j + 1))
                            .append("ID: ").append(nID).append("\n")
                            .append("CourseInfo->szCourseName: ").append(new String(courseInfo.szCourseName, encode).trim()).append("\n")
                            .append("CourseInfo->szTeacherName").append(new String(courseInfo.szTeacherName, encode).trim()).append("\n")
                            .append("CourseInfo->szIntroduction").append(new String(courseInfo.szIntroduction, encode).trim()).append("\n");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                int nChannelNum = fileInfo.nChannelNum;
                int[] nRecordNum = fileInfo.nRecordNum;
                mediaInfo.append("///--->共有通道数: ").append(nChannelNum).append("\n");

                for (int k = 0; k < nChannelNum; k++) {

                    int recordNum = nRecordNum[k]; // 通道下录像分段数(1-16)
                    mediaInfo.append(String.format("//-->第[%2d]个通道, 有[%2d]段录像\n", k + 1, recordNum));

                    for (int m = 0; m < recordNum; m++) {
                        NET_RECORD_INFO recordInfo = fileInfo.stuRecordInfo_1[k].stuRecordInfo_2[m];
                        mediaInfo.append(String.format("/->第[%2d]段录像详情:\n", m + 1))
                                .append("recordInfo->nRealChannel 真实通道: ").append(recordInfo.nRealChannel).append("\n")
                                .append("recordInfo->stuStartTime 开始时间: ").append(recordInfo.stuStartTime.toStringTimeEx()).append("\n")
                                .append("recordInfo->stuEndTime 结束时间: ").append(recordInfo.stuEndTime.toStringTimeEx()).append("\n")
                                .append("recordInfo->nFileLen 文件长度: ").append(combineInt2Long(recordInfo.nFileLen, recordInfo.nFileLenEx)).append("\n")
                                .append("recordInfo->nTime 录像时常: ").append(recordInfo.nTime).append("\n")
                                .append("recordInfo->nFileType 文件类型: ").append(recordInfo.nFileType == 0 ? "裁剪文件" : "原始文件").append("\n")
                                .append("recordInfo->emCompression 课程录像压缩类型").append(EM_COURSE_RECORD_COMPRESSION_TYPE.getEnum(recordInfo.emCompression).getNote()).append("\n");
                    }
                }
                System.out.println(mediaInfo.toString());
            }
            offset = offset + stuQueryOut.nCountResult;
        }
    }

    // 合并高低int成long
    private static long combineInt2Long(int low, int high) {
        return ((long) low & 0xFFFFFFFFl) | (((long) high << 32) & 0xFFFFFFFF00000000l);
    }

    /**
     * 从指针地址获取结构体数据
     * 不要使用 modules.CourseRecordModule 内的二次封装方法，由于 NET_OUT_QUERY_COURSEMEDIA_FILE 特别大
     * 直接使用 read() 和 write() 会极其耗时。请使用本方法，只拷贝必须的数据
     */
    public static void GetPointerDataToCourseMediaInfo(NET_OUT_QUERY_COURSEMEDIA_FILE stuQueryOut) {

        stuQueryOut.readField("nCountResult");

        long offset = stuQueryOut.fieldOffset("stuCourseMediaFile");
        Pointer pQueryOut = stuQueryOut.getPointer();

        int sizeOfMediaFile = stuQueryOut.stuCourseMediaFile[0].size();
        for (int i = 0; i < stuQueryOut.nCountResult; i++) {
            Pointer pMediaFile = stuQueryOut.stuCourseMediaFile[i].getPointer();
            pMediaFile.write(0, pQueryOut.getByteArray(offset, sizeOfMediaFile), 0, sizeOfMediaFile);
            GetPointerDataToStructMediaFile(stuQueryOut.stuCourseMediaFile[i]);
            offset += sizeOfMediaFile;
        }
    }

    /**
     * 从指针地址获取结构体数据
     */
    public static void GetPointerDataToStructMediaFile(NET_COURSEMEDIA_FILE_INFO courseMediaFile) {
        courseMediaFile.readField("nID");
        courseMediaFile.readField("stuCourseInfo");
        courseMediaFile.readField("nChannelNum");
        courseMediaFile.readField("nRecordNum");

        long offset = courseMediaFile.fieldOffset("stuRecordInfo_1");
        Pointer pMediaFile = courseMediaFile.getPointer();

        int sizeOfRecordInfo_1 = courseMediaFile.stuRecordInfo_1[0].size();
        int nChannelNum = courseMediaFile.nChannelNum;  // 通道数量 (1-64)
        for (int i = 0; i < nChannelNum; i++) {
            Pointer pRecordRecordInfo_1 = courseMediaFile.stuRecordInfo_1[i].getPointer();
            pRecordRecordInfo_1.write(0, pMediaFile.getByteArray(offset, sizeOfRecordInfo_1), 0, sizeOfRecordInfo_1);
            int recordNum = courseMediaFile.nRecordNum[i]; // 通道下录像分段数(1-16)
            GetPointerDataToStructRecordInfoArray(courseMediaFile.stuRecordInfo_1[i], recordNum);
            offset += sizeOfRecordInfo_1;
        }
    }

    /**
     * 从指针地址获取结构体数据
     */
    public static void GetPointerDataToStructRecordInfoArray(NET_RECORD_INFO_ARRAY recordInfoArray, int recordNum) {

        long offset = 0;
        Pointer pRecordInfo_1 = recordInfoArray.getPointer();

        int sizeOfRecordInfo_2 = recordInfoArray.stuRecordInfo_2[0].size();
        for (int i = 0; i < recordNum; i++) {
            Pointer pRecordInfo_2 = recordInfoArray.stuRecordInfo_2[i].getPointer();
            pRecordInfo_2.write(0, pRecordInfo_1.getByteArray(offset, sizeOfRecordInfo_2), 0, sizeOfRecordInfo_2);
            recordInfoArray.stuRecordInfo_2[i].read();
            offset += sizeOfRecordInfo_2;
        }
    }

    /**
     * 关闭查询
     */
    public void CloseQueryCourseMediaFileTest() {
        if (FindID == 0) {
            System.err.println("请不要重复关闭");
            return;
        }

        NET_IN_QUERY_COURSEMEDIA_FILECLOSE stuIn = new NET_IN_QUERY_COURSEMEDIA_FILECLOSE();
        stuIn.nFindID = FindID;     // 填写查询句柄
        NET_OUT_QUERY_COURSEMEDIA_FILECLOSE stuOut = new NET_OUT_QUERY_COURSEMEDIA_FILECLOSE();
        boolean ret = CloseQueryCourseMediaFile(courseRecordLogon.m_hLoginHandle, stuIn, stuOut, 3000);
        if (!ret) {
            System.err.println("关闭查询记录失败!");
            return;
        }
        System.out.println("关闭查询记录成功!");
        FindID = 0;
        total = 0;
    }

    /////////////////////////////////////// 下载录像 ////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    // 下载句柄
    private NetSDKLib.LLong m_hDownLoadHandle = new NetSDKLib.LLong(0);

    /**
     * 下载数据回调，这里可以拿到原始的二进制码流数据
     * 回调写成单例模式, 如果回调里需要处理数据，请另开线程
     */
    public static class DownLoadDataCallBack implements NetSDKLib.fDataCallBack {

        private DownLoadDataCallBack() {
        }

        private static class DownloadDataCallBackHolder {
            private static final DownLoadDataCallBack dataCB = new DownLoadDataCallBack();
        }

        public static DownLoadDataCallBack getInstance() {
            return DownloadDataCallBackHolder.dataCB;
        }

        public int invoke(NetSDKLib.LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, Pointer dwUser) {

            // byte[] data = pBuffer.getByteArray(0, dwBufSize);   // 这是二进制码流数据, 如果有其他用途可以从这里取出来

            // 不同的封装类型，回调里返回的 dwDataType 是不同的，它们遵循下面的逻辑
            if (dwDataType == (NetSDKLib.NET_DATA_CALL_BACK_VALUE + NetSDKLib.EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_PRIVATE)) {
                System.out.println("DownLoad DataCallBack [ " + dwDataType + " ]");
            } else if (dwDataType == (NetSDKLib.NET_DATA_CALL_BACK_VALUE + NetSDKLib.EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_GBPS)) {
                System.out.println("DownLoad DataCallBack [ " + dwDataType + " ]");
            } else if (dwDataType == (NetSDKLib.NET_DATA_CALL_BACK_VALUE + NetSDKLib.EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_TS)) {
                System.out.println("DownLoad DataCallBack [ " + dwDataType + " ]");
            } else if (dwDataType == (NetSDKLib.NET_DATA_CALL_BACK_VALUE + NetSDKLib.EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_MP4)) {
                System.out.println("DownLoad DataCallBack [ " + dwDataType + " ]");
            } else if (dwDataType == (NetSDKLib.NET_DATA_CALL_BACK_VALUE + NetSDKLib.EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_H264)) {
                System.out.println("DownLoad DataCallBack [ " + dwDataType + " ]");
            } else if (dwDataType == (NetSDKLib.NET_DATA_CALL_BACK_VALUE + NetSDKLib.EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_FLV_STREAM)) {
                System.out.println("DownLoad DataCallBack [ " + dwDataType + " ]");
            }
            return 0;
        }
    }

    /**
     * 下载进度回调函数
     * 回调写成单例模式, 如果回调里需要处理数据，请另开线程
     */
    public static class DownloadPosCallback implements NetSDKLib.fDownLoadPosCallBack {

        private DownloadPosCallback() {
        }

        private static class CallBackHolder {
            private static final DownloadPosCallback callback = new DownloadPosCallback();
        }

        public static DownloadPosCallback getInstance() {
            return CallBackHolder.callback;
        }

        @Override
        public void invoke(NetSDKLib.LLong lPlayHandle, int dwTotalSize, int dwDownLoadSize, Pointer dwUser) {

            System.out.println(String.format("dwDownLoadSize: %d || dwTotalSize: %d ", dwDownLoadSize, dwTotalSize));
            if (dwDownLoadSize == -1) {         // 下载结束
                System.out.println("Downloading Complete. ");
                new StopDownloadTask(lPlayHandle).start();   // 注意这里需要另起线程
            }
        }

        private static class StopDownloadTask extends Thread {
            private final NetSDKLib.LLong lDownloadHandle;

            public StopDownloadTask(NetSDKLib.LLong lDownloadHandle) {
                this.lDownloadHandle = lDownloadHandle;
            }

            public void run() {
                stopDownLoadRecordFile(lDownloadHandle);
            }
        }
    }

    /*************************************************************************************
     *								下载/停止下载 录像							 *
     *************************************************************************************/

    /**
     * 设置回放时的码流类型: 主码流/辅码流
     *
     * @param m_streamType 码流类型
     */
    public boolean setStreamType(int m_streamType) {
        int emType = NetSDKLib.EM_USEDEV_MODE.NET_RECORD_STREAM_TYPE; // 回放录像枚举
        IntByReference steamType = new IntByReference(m_streamType);  // 0-主辅码流,1-主码流,2-辅码流
        return netsdk.CLIENT_SetDeviceMode(courseRecordLogon.m_hLoginHandle, emType, steamType.getPointer());
    }

    /**
     * 下载录像，通用接口(把原始码流转换成其他封装格式的码流)
     */
    public void downloadRecordFileConverted(int nStreamType, int nChannel,
                                            NetSDKLib.NET_TIME stTimeStart, NetSDKLib.NET_TIME stTimeEnd,
                                            String savedFileName, int nType) {

        if (!setStreamType(nStreamType)) {
            System.err.println("Set Stream Type Failed!." + ToolKits.getErrorCode());
            return;
        }

        NetSDKLib.NET_IN_DOWNLOAD_BY_DATA_TYPE stIn = new NetSDKLib.NET_IN_DOWNLOAD_BY_DATA_TYPE();

        stIn.emDataType = nType;            // 封装类型
        stIn.emRecordType = NetSDKLib.EM_QUERY_RECORD_TYPE.EM_RECORD_TYPE_ALL; // 所有录像
        stIn.nChannelID = nChannel;
        stIn.stStartTime = stTimeStart;     // 开始时间
        stIn.stStopTime = stTimeEnd;        // 结束时间
        stIn.cbDownLoadPos = DownloadPosCallback.getInstance();           // 下载监控回调函数
        stIn.dwPosUser = null;
        stIn.fDownLoadDataCallBack = DownLoadDataCallBack.getInstance();  // 下载数据回调函数
        stIn.dwDataUser = null;
        stIn.szSavedFileName = savedFileName;

        NetSDKLib.NET_OUT_DOWNLOAD_BY_DATA_TYPE stOut = new NetSDKLib.NET_OUT_DOWNLOAD_BY_DATA_TYPE();
        stIn.write();
        stOut.write();
        m_hDownLoadHandle = netsdk.CLIENT_DownloadByDataType(courseRecordLogon.m_hLoginHandle, stIn.getPointer(), stOut.getPointer(), 5000);
        if (m_hDownLoadHandle.longValue() != 0) {
            System.out.println("DownloadByDataType Succeed!");
        } else {
            System.err.println("DownloadByDataType Failed! " + ToolKits.getErrorCode());
        }
    }

    /**
     * 停止下载录像
     *
     * @param hDownLoadHandle 下载句柄
     */
    private static void stopDownLoadRecordFile(NetSDKLib.LLong hDownLoadHandle) {
        if (hDownLoadHandle.longValue() == 0) {
            return;
        }
        netsdk.CLIENT_StopDownload(hDownLoadHandle);
    }

    /*************************************************************************************
     *								下载录像                             				 *
     *************************************************************************************/

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private void setTime(Calendar calendar, NetSDKLib.NET_TIME stuTime) {
        stuTime.setTime(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    /**
     * 下载录像 转码流 通用接口 可以从回调获取转码流数据
     */
    public void downloadRecordFileWithConvertedDataType() {

        int nStreamType = 0;         // 0-主辅码流,1-主码流,2-辅码流
        int nChannel = 0;            // 通道号
        int nType = 0;               // 文件类型
        NetSDKLib.NET_TIME stTimeStart = new NetSDKLib.NET_TIME();    // 开始时间
        NetSDKLib.NET_TIME stTimeEnd = new NetSDKLib.NET_TIME();      // 结束时间

        Calendar calendar = Calendar.getInstance();

        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);
        try {
            // 请选择要下载的码流 0-主辅码流,1-主码流,2-辅码流
            nStreamType = 1;   // 默认主码流

            System.out.println("请输入真实通道号(注意sdk从0开始计数): ");
            nChannel = scanner.nextInt();

            //        0 私有码流
            //        1 国标PS码流
            //        2 TS码流
            //        3 MP4文件(从回调函数出来的是私有码流数据,参数dwDataType值为0)
            //        4 裸H264码流
            //        5 流式FLV
            // 请输入保存的文件类型 码流转换类型 0 私有码流; 1 国标PS码流; 2 TS码流; 3 MP4文件; 4 裸H264码流; 5 流式FLV");
            nType = 5;     // 转成 FLV

            System.out.println("请输入录像开始时间(格式:yyyy-MM-dd HH:mm:ss): ");
            String startTime = scanner.next().trim() + " " + scanner.next().trim();
            calendar.setTime(format.parse(startTime));
            setTime(calendar, stTimeStart);

            System.out.println("请输入录像结束时间(格式:yyyy-MM-dd HH:mm:ss): ");
            String endTime = scanner.next().trim() + " " + scanner.next().trim();
            calendar.setTime(format.parse(endTime));
            setTime(calendar, stTimeEnd);
        } catch (ParseException e) {
            System.err.println("时间输入非法");
            return;
        }

        File dir = new File("RecordFiles");
        if (!dir.exists()) {// 判断目录是否存在
            dir.mkdir();
        }

        String savedFileName = "RecordFiles/RecordCovertTest" + System.currentTimeMillis() + ".flv"; // 保存录像文件名
        this.downloadRecordFileConverted(nStreamType, nChannel, stTimeStart, stTimeEnd, savedFileName, nType);
    }


    /**
     * 停止下载录像
     */
    public void stopDownLoadRecordFile() {
        stopDownLoadRecordFile(m_hDownLoadHandle);
    }

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

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
        menu.addItem(new CaseMenu.Item(this, "开始查询录像记录测试", "OpenQueryCourseMediaFileTest"));
        menu.addItem(new CaseMenu.Item(this, "获取录像记录数据测试", "DoQueryCourseMediaFileTest"));
        menu.addItem(new CaseMenu.Item(this, "关闭查询录像记录测试", "CloseQueryCourseMediaFileTest"));
        menu.addItem(new CaseMenu.Item(this, "按时间下载录像(通用转码流接口)", "downloadRecordFileWithConvertedDataType"));

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
    private String m_strPassword = "admin123";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        DemoConsoleRecordManage demo = new DemoConsoleRecordManage();

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
