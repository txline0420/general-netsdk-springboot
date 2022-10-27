package com.netsdk.demo.customize.JordanPSD;

import com.netsdk.demo.customize.JordanPSD.module.LogonModule;
import com.netsdk.demo.customize.JordanPSD.module.RecordModule;
import com.netsdk.demo.customize.JordanPSD.module.SdkUtilModule;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.EM_MOBILE_ENFORCE_FILE_TYPE;
import com.netsdk.lib.enumeration.EM_MOBILE_ENFORCE_FORMAT;
import com.netsdk.lib.enumeration.EM_MOBILE_ENFORCE_UPLOAD_FLAG;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * @author 47040
 * @since Created at 2021/5/28 19:25
 */
public class Demo33C300 {

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;
    private final NetSDKLib.LLong m_hLoginHandle; // 登录句柄
    private final NetSDKLib.NET_DEVICEINFO_Ex m_hDeviceInfo; // 设备信息
    private final Charset encode = Charset.forName(Utils.getPlatformEncode()); // 跨平台编码
    private NetSDKLib.LLong m_hFindFileHandle = new NetSDKLib.LLong(0); // 文件查询句柄
    private NetSDKLib.LLong m_hDownLoadHandle = new NetSDKLib.LLong(0); // 下载句柄
    // 存储临时查询数据
    private ArrayList<NetOutMediaQueryFile> mediaRecords = new ArrayList<>();

    private class NetOutMediaQueryFile {

        public int nChannelID;
        public NetSDKLib.NET_TIME stuStartTime;
        public NetSDKLib.NET_TIME stuEndTime;
        public byte byFileType;
        public byte byPartition;
        public byte byVideoStream;
        public int nCluster;
        public byte[] szFilePath;
        public int nDriveNo;
        public long nFileSizeEx;
        public int nTotalFrame;
        public int emFileState;

    }

    public Demo33C300(DeviceInfo deviceInfo) {
        this.m_hLoginHandle = deviceInfo.m_hLoginHandle;
        this.m_hDeviceInfo = deviceInfo.m_stDeviceInfo;
    }

    public Demo33C300(NetSDKLib.LLong loginHandle, NetSDKLib.NET_DEVICEINFO_Ex deviceinfo) {
        this.m_hLoginHandle = loginHandle;
        this.m_hDeviceInfo = deviceinfo;
    }


    /**
     * 查询采集站和手持终端文件信息 Demo里的查询条件: 2021-06-02 12:00:00 至 2021-06-02 12:30:00 文件类型：未上传
     */
    public void findFileExQueryMobileEnforce() {
        if (m_hLoginHandle.longValue() == 0) {
            System.err.println("Please Login First");
            return;
        }

        ///////////////////////////// 开始查询 /////////////////////////////
        ///////////////////////////////////////////////////////////////////

        final int queryType = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_MOBILE_ENFORCE;
        MEDIAFILE_MOBILE_ENFORCE_PARAM condition = new MEDIAFILE_MOBILE_ENFORCE_PARAM();

        condition.nChannelID = -1; // 采集站没有通道号概念  使用-1代表所有通道
        condition.stuBeginTime = new NET_TIME(2021, 7, 1, 2, 0, 0); // 设置 开始时间
        condition.stuEndTime = new NET_TIME(2021, 7, 2, 23, 30, 0); // 设置 结束时间
        condition.emUploadFlag =
                EM_MOBILE_ENFORCE_UPLOAD_FLAG.EM_MOBILE_ENFORCE_UPLOAD_FLAG_NOTUPLOAD.getValue(); // 未上传

        // 设置查询类型 获取查询句柄
        m_hFindFileHandle = RecordModule.FindFileEx(m_hLoginHandle, queryType, condition, 3000);
        if (m_hFindFileHandle.longValue() == 0) {
            System.err.println("查询采集站和手持终端文件信息失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("查询采集站和手持终端文件信息成功");

        ///////////////////////////// 文件总数 /////////////////////////////
        ///////////////////////////////////////////////////////////////////

        // 获取违章总数
        int totalCount = RecordModule.GetTotalFileCount(m_hFindFileHandle, 3000);
        if (totalCount == -1) {
            System.err.println("获取查询总数失败:" + ENUMERROR.getErrorMessage());
            // 查询异常，直接退出查询
            RecordModule.FindCloseEx(m_hFindFileHandle);
            return;
        }

        ///////////////////////////// 获取查询结果 /////////////////////////////
        //////////////////////////////////////////////////////////////////////

        int queryCount = Math.min(10, totalCount); // 每次获取的违章数量
        MEDIAFILE_MOBILE_ENFORCE_INFO[] mediaFileInfos = new MEDIAFILE_MOBILE_ENFORCE_INFO[queryCount];
        for (int i = 0; i < mediaFileInfos.length; ++i) {
            mediaFileInfos[i] = new MEDIAFILE_MOBILE_ENFORCE_INFO();
        }

        final int memorySize = mediaFileInfos[0].size() * queryCount;
        Pointer mediaFileMemory = new Memory(memorySize);
        mediaFileMemory.clear(memorySize);

        // 循环查询
        int nCurrIndex = 0;
        do {
            // 查询
            int nRet =
                    RecordModule.FindNextFileEx(m_hFindFileHandle, mediaFileInfos, mediaFileMemory, 3000);
            if (nRet < 0) {
                System.err.println("获取查询信息失败:" + ENUMERROR.getErrorMessage());
                break;
            }

            // 打印一下获取到的信息内容
            StringBuilder printInfo = new StringBuilder();
            for (int j = 0; j < nRet; j++) {
                ++nCurrIndex;

                MEDIAFILE_MOBILE_ENFORCE_INFO info = mediaFileInfos[j];
                printInfo
                        .append("-------------------------")
                        .append(nCurrIndex)
                        .append("---------------------------\n")
                        .append("  开始时间: ")
                        .append(info.stuStartTime.toString())
                        .append("\n")
                        .append("  结束时间: ")
                        .append(info.stuEndTime.toString())
                        .append("\n")
                        .append("  文件路径: ")
                        .append(new String(info.szFilePath, encode))
                        .append("\n")
                        .append("  文件名称: ")
                        .append(new String(info.szFileName, encode))
                        .append("\n")
                        .append("  文件长度: ")
                        .append(info.nLength)
                        .append("\n")
                        .append("  文件标识: ")
                        .append(new String(info.szUniqueID, encode))
                        .append("\n")
                        .append("  文件序列: ")
                        .append(new String(info.szOriginalDeviceID, encode))
                        .append("\n")
                        .append("  文件类型: ")
                        .append(EM_MOBILE_ENFORCE_FORMAT.getEnum(info.emFormat).getNote())
                        .append("\n")
                        .append("  文件后缀: ")
                        .append(EM_MOBILE_ENFORCE_FILE_TYPE.getEnum(info.emFileType).getNote())
                        .append("\n");
            }
            System.out.println(printInfo.toString());
        } while ((totalCount -= queryCount) > 0);

        ///////////////////////////// 结束查询 /////////////////////////////
        ///////////////////////////////////////////////////////////////////

        RecordModule.FindCloseEx(m_hFindFileHandle);
    }

    /**
     * 通用录像文件下载 按文件信息筛选
     */
    public void DownloadRecordByFile() {

        if (m_hLoginHandle.longValue() == 0) {
            System.err.println("请先登录");
            return;
        }

        List<NetSDKLib.NET_RECORDFILE_INFO> files = QueryRecordFile();
        if (files.isEmpty()) {
            System.err.println("查询不到录像文件");
            return;
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("请选择需要下载的文件序号");
        int idx = sc.nextInt();
        NetSDKLib.NET_RECORDFILE_INFO fileInfo = files.get(idx);

        // LPNET_RECORDFILE_INFO 和 NET_RECORDFILE_INFO 内部字段完全一样 可以互相替换 因此我加了个重载接口
        // 这里直接使用 fileInfo 作查询条件

        // 默认保存路径
        String savePath = new File(".").getAbsoluteFile().getParent() + "\\" + System.currentTimeMillis() + ".MP4";
        System.out.println("SavedFileName:" + savePath);
        NetSDKLib.fTimeDownLoadPosCallBack posCallBack = FileDownLoadPosCallBack.getInstance();   // 进度回调
        NetSDKLib.fDataCallBack dataCallBack = FileDownloadDataCallBack.getInstance();            // 下载数据回调

        m_hDownLoadHandle = RecordModule.DownloadRecordFileByFileEx(m_hLoginHandle, fileInfo, savePath, posCallBack, dataCallBack);
        if (m_hDownLoadHandle.longValue() == 0) {
            System.err.println("Start Download By File Failed :" + ENUMERROR.getErrorMessage());
        } else {
            System.out.println("Start Download By File Succeed");
        }
    }

    /**
     * 通用录像文件下载 FindFile 查询文件
     */
    public void DownloadRecordByFileAndFindFile() {

        if (m_hLoginHandle.longValue() == 0) {
            System.err.println("请先登录");
            return;
        }

        findFileExMediaRecord();
        if (mediaRecords.isEmpty()) {
            System.err.println("查询不到录像文件");
            return;
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("请选择需要下载的文件序号");
        int idx = sc.nextInt();
        NetOutMediaQueryFile record = mediaRecords.get(idx);

        // 设置查询条件
        NetSDKLib.NET_RECORDFILE_INFO fileCondition = new NetSDKLib.NET_RECORDFILE_INFO();
        fileCondition.ch = record.nChannelID;
        fileCondition.bRecType = record.byVideoStream;
        // 两个 byte[] 长度并不相同 必须使用 arraycopy
        System.arraycopy(record.szFilePath, 0, fileCondition.filename, 0, fileCondition.filename.length);
        System.out.println("filePath:" + new String(fileCondition.filename, encode));
        fileCondition.size = (int) record.nFileSizeEx / 1024;
        fileCondition.framenum = record.nTotalFrame;
        fileCondition.driveno = record.nDriveNo;
        fileCondition.startcluster = record.nCluster;
        fileCondition.starttime = record.stuStartTime;
        fileCondition.endtime = record.stuEndTime;

        // 默认保存路径
        String savePath = new File(".").getAbsoluteFile().getParent() + "\\" + System.currentTimeMillis() + ".MP4";
        System.out.println("SavedFileName:" + savePath);
        NetSDKLib.fTimeDownLoadPosCallBack posCallBack = FileDownLoadPosCallBack.getInstance();   // 进度回调
        NetSDKLib.fDataCallBack dataCallBack = FileDownloadDataCallBack.getInstance();            // 下载数据回调

        m_hDownLoadHandle = RecordModule.DownloadRecordFileByFileEx(m_hLoginHandle, fileCondition, savePath, posCallBack, dataCallBack);
        if (m_hDownLoadHandle.longValue() == 0) {
            System.err.println("Start Download By File Failed :" + ENUMERROR.getErrorMessage());
        } else {
            System.out.println("Start Download By File Succeed");
        }
    }

    /**
     * 录像文件下载 CLIENT_DownloadByFileSelfAdapt自适应下载 + FindFile 查询文件
     */
    public void DownloadRecordWithSelfAdaptByFileAndFindFile() {

        FileDownLoadPosCallBack2.downloadPosQueue2.clear();
        FileDownloadDataCallBack2.downloadDataQueue2.clear();

        if (m_hLoginHandle.longValue() == 0) {
            System.err.println("请先登录");
            return;
        }

        findFileExMediaRecord();
        if (mediaRecords.isEmpty()) {
            System.err.println("查询不到录像文件");
            return;
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("请选择需要下载的文件序号");
        int idx = sc.nextInt();
        NetOutMediaQueryFile record = mediaRecords.get(idx);

        // 设置查询条件
        NET_IN_DOWNLOAD_BYFILE_SELFADAPT stuParamIn = new NET_IN_DOWNLOAD_BYFILE_SELFADAPT();
        stuParamIn.nChannelID = record.nChannelID;
        stuParamIn.emRecordType = 0;
        // 两个 byte[] 长度并不相同 必须使用 arraycopy
        System.arraycopy(record.szFilePath, 0, stuParamIn.szFileName, 0, Math.min(record.szFilePath.length, stuParamIn.szFileName.length));
        stuParamIn.nFrameNum = record.nTotalFrame;
        stuParamIn.size = (int) record.nFileSizeEx / 1024;
        stuParamIn.stuStartTime = record.stuStartTime;
        stuParamIn.stuEndTime = record.stuEndTime;
        stuParamIn.nDriveno = record.nDriveNo;
        stuParamIn.nStartCluster = record.nCluster;

        stuParamIn.cbDownLoadPos = FileDownLoadPosCallBack2.getInstance();   // 进度回调  注意和 CLIENT_DownloadByRecordFileEx 的不一样 继承的接口不一样
        stuParamIn.dwDataUser = null;
        stuParamIn.fDownLoadDataCallBack = FileDownloadDataCallBack2.getInstance(); // 下载数据回调 注意 这个下载接口不能保存本地文件 所以数据只能从回调取
        stuParamIn.dwPosUser = null;

        NET_OUT_DOWNLOAD_BYFILE_SELFADAPT stuParamOut = new NET_OUT_DOWNLOAD_BYFILE_SELFADAPT();

        m_hDownLoadHandle = RecordModule.DownloadRecordFileWithSelfAdapt(m_hLoginHandle, stuParamIn, stuParamOut, 5000);

        if (m_hDownLoadHandle.longValue() == 0) {
            System.err.println("Start Download By File Failed :" + ENUMERROR.getErrorMessage());
        } else {
            System.out.println("Start Download By File Succeed");
        }

        // 起一个线程获取数据
        downloadTaskOpen = true;
        new Thread(this::downloadDataTask).start();
        new Thread(this::downloadPosTask).start();
    }


    /**
     * 33C300测试专用 用例
     * {
     * "CollectTime": "2021-07-05 10:30:07",
     * "DeviceID": "3G05E10YAJ2FED0",
     * "EndTime": "2021-07-05 10:19:12",
     * "FileName": "20210705101910.dav",
     * "FilePath": "/recno/463.dav",
     * "FileRemark": {
     * "CaseAddress": "",
     * "CaseID": "",
     * "CaseRemark": "",
     * "CaseType": 2,
     * "PicTurn": 0,
     * "UpLoadState": 1
     * },
     * "Format": 1,
     * "Length": 2983163,
     * "StartTime": "2021-07-05 10:19:10",
     * "Type": 1
     * }
     */
    public void DownloadRecord33C300SelfAdaptByFileAndFindFile() {

        FileDownLoadPosCallBack2.downloadPosQueue2.clear();
        FileDownloadDataCallBack2.downloadDataQueue2.clear();

        if (m_hLoginHandle.longValue() == 0) {
            System.err.println("请先登录");
            return;
        }

        // 设置查询条件
        NET_IN_DOWNLOAD_BYFILE_SELFADAPT stuParamIn = new NET_IN_DOWNLOAD_BYFILE_SELFADAPT();
        stuParamIn.emRecordType = 1;
        // 两个 byte[] 长度并不相同 必须使用 arraycopy
        System.arraycopy("/recno/463.dav".getBytes(), 0, stuParamIn.szFileName, 0, Math.min("/recno/463.dav".length(), stuParamIn.szFileName.length));
        stuParamIn.size = (int) 2983163 / 1024;
        stuParamIn.stuStartTime = new NetSDKLib.NET_TIME();
        stuParamIn.stuStartTime.setTime(2021,7,5,10,19,10);

        stuParamIn.stuEndTime = new NetSDKLib.NET_TIME();
        stuParamIn.stuEndTime.setTime(2021,7,5,10,19,12);

        stuParamIn.cbDownLoadPos = FileDownLoadPosCallBack2.getInstance();   // 进度回调  注意和 CLIENT_DownloadByRecordFileEx 的不一样 继承的接口不一样
        stuParamIn.dwDataUser = null;
        stuParamIn.fDownLoadDataCallBack = FileDownloadDataCallBack2.getInstance(); // 下载数据回调 注意 这个下载接口不能保存本地文件 所以数据只能从回调取
        stuParamIn.dwPosUser = null;

        NET_OUT_DOWNLOAD_BYFILE_SELFADAPT stuParamOut = new NET_OUT_DOWNLOAD_BYFILE_SELFADAPT();

        m_hDownLoadHandle = RecordModule.DownloadRecordFileWithSelfAdapt(m_hLoginHandle, stuParamIn, stuParamOut, 5000);

        if (m_hDownLoadHandle.longValue() == 0) {
            System.err.println("Start Download By File Failed :" + ENUMERROR.getErrorMessage());
        } else {
            System.out.println("Start Download By File Succeed");
        }

        // 起一个线程获取数据
        downloadTaskOpen = true;
        new Thread(this::downloadDataTask).start();
        new Thread(this::downloadPosTask).start();
    }

    private boolean downloadTaskOpen = false;

    // 获取监听回调数据并放入缓存
    public void downloadDataTask() {

        String savePath = new File(".").getAbsoluteFile().getParent() + "\\" + System.currentTimeMillis() + ".MP4";
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(new File(savePath), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int offset = 0;
        while (downloadTaskOpen || !FileDownloadDataCallBack2.downloadDataQueue2.isEmpty()) {
            try {
                // 阻塞获取数据
                byte[] data = FileDownloadDataCallBack2.downloadDataQueue2.poll(50, TimeUnit.MILLISECONDS);
                if (file == null || data == null) continue;
                // 数据存入文件
                file.seek(offset);
                file.write(data);
                offset += data.length;
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                try {
                    if (file != null) file.close(); // 关闭文件
                    break;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    break;
                }
            }
        }

        try {
            if (file != null) file.close(); // 关闭文件
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取监听回调进度
    public void downloadPosTask() {

        DecimalFormat df = new DecimalFormat("######0.00");

        while (downloadTaskOpen || !FileDownLoadPosCallBack2.downloadPosQueue2.isEmpty()) {
            try {
                // 判断下载进度
                DownloadPosInfo pos = FileDownLoadPosCallBack2.downloadPosQueue2.poll(50, TimeUnit.MILLISECONDS);
                if (pos != null) {
                    if (pos.downloadSize == -1) {
                        downloadTaskOpen = false;
                        System.out.println(String.format("download pos: %d%%  totalSize(kb): %d task completed",
                                100, pos.totalSize));
                    } else {
                        System.out.println(String.format("download pos: %2s%% downloadSize(kb): %d totalSize(kb): %d",
                                df.format(pos.downloadPos), pos.totalSize, pos.downloadSize));
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 停止下载录像
     */
    public void StopDownLoadRecordFile() {
        RecordModule.StopDownLoadRecordFile(m_hDownLoadHandle);
        downloadTaskOpen = false;
    }

    /**
     * 查找录像 需要指定查询录像的最大个数
     * 这个接口只是用于协助演示录像下载用的 比较简单 需要手动预填最大文件数量 实际开发请走 FindFile 接口
     */
    public List<NetSDKLib.NET_RECORDFILE_INFO> QueryRecordFile() {
        NetSDKLib.NET_TIME stTimeStart = new NetSDKLib.NET_TIME();
        stTimeStart.setTime(2021, 6, 8, 0, 0, 0);

        NetSDKLib.NET_TIME stTimeEnd = new NetSDKLib.NET_TIME();
        stTimeEnd.setTime(2021, 7, 2, 10, 15, 0);

        //////////// 按时间查找视频文件 //////////
        ////////////////////////////////////////

        int maxFileCount = 50; // 查询的最大文件个数
        NetSDKLib.NET_RECORDFILE_INFO[] fileInfos = new NetSDKLib.NET_RECORDFILE_INFO[maxFileCount];
        for (int i = 0; i < maxFileCount; i++) {
            fileInfos[i] = new NetSDKLib.NET_RECORDFILE_INFO();
        }
        int retCount = RecordModule.QueryRecordFileByTimeLimit(m_hLoginHandle, maxFileCount, fileInfos, stTimeStart, stTimeEnd);
        if (retCount == -1) {
            System.err.println("查询录像文件失败!" + ENUMERROR.getErrorMessage());
        }
        System.out.println("查询录像文件成功 共查询到录像数: " + retCount);

        // 打印看一下
        StringBuilder info = new StringBuilder()
                .append("QueryRecordFile  Succeed! \n" + "查询到的视频个数：")
                .append(retCount).append("\n")
                .append("码流类型：").append(fileInfos[0].bRecType).append("\n");
        for (int i = 0; i < retCount; i++) {
            info.append("【").append(i).append("】：").append("\n")
                    .append("文件名：").append(new String(fileInfos[i].filename, encode)).append("\n")
                    .append("文件长度：").append(fileInfos[i].size).append("\n")
                    .append("开始时间：").append(fileInfos[i].starttime).append("\n")
                    .append("结束时间：").append(fileInfos[i].endtime).append("\n")
                    .append("通道号：").append(fileInfos[i].ch).append("\n");
        }
        System.out.println(info.toString());

        return new ArrayList<>(Arrays.asList(fileInfos).subList(0, retCount));
    }

    /**
     * 查询 录像文件
     */
    public void findFileExMediaRecord() {

        if (m_hLoginHandle.longValue() == 0) {
            System.err.println("Please Login First");
            return;
        }
        mediaRecords.clear();

        ///////////////////////////// 开始查询 /////////////////////////////
        ///////////////////////////////////////////////////////////////////

        NetSDKLib.NET_TIME stTimeStart = new NetSDKLib.NET_TIME();
        stTimeStart.setTime(2021, 7, 1, 0, 0, 0);

        NetSDKLib.NET_TIME stTimeEnd = new NetSDKLib.NET_TIME();
        stTimeEnd.setTime(2021, 7, 20, 23, 15, 0);

        int type = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FILE;
        // 查询条件
        NetSDKLib.NET_IN_MEDIA_QUERY_FILE queryCondition = new NetSDKLib.NET_IN_MEDIA_QUERY_FILE();
        // 工作目录列表,一次可查询多个目录,为空表示查询所有目录。
        // 目录之间以分号分隔,如“/mnt/dvr/sda0;/mnt/dvr/sda1”,szDirs==null 或 "" 表示查询所有
        queryCondition.szDirs = null;         // 查询所有
        queryCondition.nMediaType = 2;      // 文件类型 1:查询jpg图片,2:查询dav
        queryCondition.nChannelID = -1;     // 通道号从0开始,-1表示查询所有通道
        queryCondition.stuStartTime = stTimeStart;    // 开始时间
        queryCondition.stuEndTime = stTimeEnd;        // 结束时间

        NetSDKLib.LLong m_lFindMediaHandle = RecordModule.FindFileEx(m_hLoginHandle, type, queryCondition, 5000);
        if (m_lFindMediaHandle.longValue() == 0) {
            System.err.println("FindFileEx Failed!" + ENUMERROR.getErrorMessage());
            return;
        }

        ///////////////////////////// 文件总数 /////////////////////////////
        ///////////////////////////////////////////////////////////////////

        // 获取违章总数
        int totalCount = RecordModule.GetTotalFileCount(m_lFindMediaHandle, 3000);
        if (totalCount == -1) {
            System.err.println("获取查询总数失败:" + ENUMERROR.getErrorMessage());
            // 查询异常，直接退出查询
            RecordModule.FindCloseEx(m_lFindMediaHandle);
            return;
        }

        if (totalCount == 0) {
            System.out.println("没有查询到任何录像数据");
            RecordModule.FindCloseEx(m_lFindMediaHandle);
            return;
        }

        ///////////////////////////// 获取查询结果 /////////////////////////////
        //////////////////////////////////////////////////////////////////////

        int queryCount = Math.min(10, totalCount); // 每次获取的违章数量
        NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[] mediaFileInfos = new NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[queryCount];
        for (int i = 0; i < mediaFileInfos.length; ++i) {
            mediaFileInfos[i] = new NetSDKLib.NET_OUT_MEDIA_QUERY_FILE();
        }

        final int memorySize = mediaFileInfos[0].size() * queryCount;
        Pointer mediaFileMemory = new Memory(memorySize);
        mediaFileMemory.clear(memorySize);

        // 循环查询
        int nCurrIndex = 0;
        do {
            // 查询
            // int nRet = RecordModule.FindNextFileEx(m_lFindMediaHandle, mediaFileInfos, mediaFileMemory, 5000);
            int nRet = RecordModule.FindNextMediaFileEx(m_lFindMediaHandle, mediaFileInfos, mediaFileMemory, 5000);
            if (nRet < 0) {
                System.err.println("获取查询信息失败:" + ENUMERROR.getErrorMessage());
                break;
            }

            // 打印一下获取到的信息内容
            StringBuilder printInfo = new StringBuilder();
            for (int j = 0; j < nRet; j++) {

                NetOutMediaQueryFile info = new NetOutMediaQueryFile();
                // 注意：循环中 mediaFileInfos 一直是同一组对象 所以获取数据必须使用深拷贝
                info.nChannelID = mediaFileInfos[j].nChannelID;
                info.stuStartTime = new NetSDKLib.NET_TIME(mediaFileInfos[j].stuStartTime);
                info.stuEndTime = new NetSDKLib.NET_TIME(mediaFileInfos[j].stuEndTime);
                info.byFileType = mediaFileInfos[j].byFileType;
                info.byPartition = mediaFileInfos[j].byPartition;
                info.byVideoStream = mediaFileInfos[j].byVideoStream;
                info.nCluster = mediaFileInfos[j].nCluster;
                info.szFilePath = new byte[mediaFileInfos[j].szFilePath.length];
                System.arraycopy(mediaFileInfos[j].szFilePath, 0, info.szFilePath, 0, mediaFileInfos[j].szFilePath.length);
                info.nDriveNo = mediaFileInfos[j].nDriveNo;
                info.nFileSizeEx = mediaFileInfos[j].nFileSizeEx;
                info.nTotalFrame = mediaFileInfos[j].nTotalFrame;
                info.emFileState = mediaFileInfos[j].emFileState;

                mediaRecords.add(info);
                printInfo
                        .append("-------------------------")
                        .append(nCurrIndex)
                        .append("---------------------------\n")
                        .append("  开始时间: ")
                        .append(info.stuStartTime.toString())
                        .append("\n")
                        .append("  结束时间: ")
                        .append(info.stuEndTime.toString())
                        .append("\n")
                        .append("  文件路径: ")
                        .append(new String(info.szFilePath, encode))
                        .append("\n")
                        .append("  文件类型: ")
                        .append(info.byFileType == 1 ? "jpg图片" : "dav文件")
                        .append("\n")
                        .append("  文件长度Ex: ")
                        .append(info.nFileSizeEx)
                        .append("\n");
                ++nCurrIndex;
            }
            System.out.println(printInfo.toString());
        } while ((totalCount -= queryCount) > 0);

        ///////////////////////////// 结束查询 /////////////////////////////
        ///////////////////////////////////////////////////////////////////

        RecordModule.FindCloseEx(m_lFindMediaHandle);
    }

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 初始化测试
     */
    public void InitTest() {
        SdkUtilModule.Init();  // 初始化SDK库
        NetSDKLib.LLong loginHandle = LogonModule.TcpLoginWithHighSecurity(m_ipAddr, m_nPort, m_username, m_password, m_hDeviceInfo);  // 高安全登录
        if (loginHandle.intValue() == 0) {
            SdkUtilModule.cleanup();
        }
        m_hLoginHandle.setValue(loginHandle.longValue());
    }

    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "查询采集站和手持终端文件信息", "findFileExQueryMobileEnforce"));
        menu.addItem(new CaseMenu.Item(this, "查询录像信息", "findFileExMediaRecord"));
        menu.addItem(new CaseMenu.Item(this, "下载录像", "DownloadRecordByFile"));
        menu.addItem(new CaseMenu.Item(this, "下载录像(FindFileEx)", "DownloadRecordByFileAndFindFile"));
        menu.addItem(new CaseMenu.Item(this, "下载录像sdk自适应(FindFileEx)", "DownloadRecordWithSelfAdaptByFileAndFindFile"));
        menu.addItem(new CaseMenu.Item(this, "下载录像33C300测试sdk自适应(FindFileEx)", "DownloadRecord33C300SelfAdaptByFileAndFindFile"));
        menu.addItem(new CaseMenu.Item(this, "停止下载录像", "StopDownLoadRecordFile"));
        menu.run();
        System.out.println("子测试已退出");
    }

    /**
     * 结束测试
     */
    public void EndTest() {
        System.out.println("End Test");
        LogonModule.logout(m_hLoginHandle);  // 登出
        System.out.println("See You...");
        SdkUtilModule.cleanup();             // 清理资源
        System.exit(0);
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
//    private String m_ipAddr = "172.8.2.99";  // MPT
    private String m_ipAddr = "172.11.2.20"; // 33C300
    private int m_nPort = 37777;
    private String m_username = "admin";
    //    private String m_password = "qqqqq1";   // MPT
    private String m_password = "admin123";   // 33C300
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {

        NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
        NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);
        Demo33C300 demo = new Demo33C300(loginHandle, deviceInfo);
        if (args.length == 4) {
            demo.m_ipAddr = args[0];
            demo.m_nPort = Integer.parseInt(args[1]);
            demo.m_username = args[2];
            demo.m_password = args[3];
        }
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
