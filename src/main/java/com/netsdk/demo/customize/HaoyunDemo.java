package com.netsdk.demo.customize;

import com.netsdk.demo.customize.JordanPSD.FileDownLoadPosCallBack;
import com.netsdk.demo.customize.JordanPSD.FileDownloadDataCallBack;
import com.netsdk.demo.customize.JordanPSD.module.RecordModule;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetFinalVar;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_NEW_CONFIG;
import com.netsdk.lib.enumeration.EM_STORAGE_DISK_PREDISKCHECK;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.NET_VOLUME_TYPE;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.netsdk.lib.NetSDKLib.NET_DEVSTATE_SOFTWARE;

/**
 * @author 291189
 * @version 1.0
 * @description ERR220614207 浩云 NETSDK SDK定制
 * @date 2022/6/17 9:28
 */
public class HaoyunDemo  extends Initialization {

    static Scanner sc = new Scanner(System.in);

    private TimeDownLoadDataCallBack m_DownLoadData = new TimeDownLoadDataCallBack(); // 录像下载数据回调
    // 按时间下载数据回调
    public class TimeDownLoadDataCallBack implements NetSDKLib.fDataCallBack {
        public int invoke(NetSDKLib.LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, Pointer dwUser) {
    //   System.out.println("TimeDownLoadDataCallBack [ " + dwUser +" ]");
            return 0;
        }
    }

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     *  下载录像
     * @param nStreamType 码流类型
     * @param nChannel 通道号
     * @param stTimeStart 开始时间
     * @param stTimeEnd 结束时间
     * @param savedFileName 保存录像文件名
     */

    NetSDKLib.LLong n_hDownLoadHandle =new    NetSDKLib.LLong();
    public boolean downloadRecordFile(int nStreamType, int nChannel,
                                      NetSDKLib.NET_TIME stTimeStart, NetSDKLib.NET_TIME stTimeEnd, String savedFileName) {

        if (!setStreamType(nStreamType)) {
            System.err.println("Set Stream Type Failed!." + ToolKits.getErrorCode());
            return false;
        }

        int scType = 3; 	// scType:         码流转换类型,0-DAV码流(默认); 1-PS流,3-MP4
        int nRecordFileType = NetSDKLib.EM_QUERY_RECORD_TYPE.EM_RECORD_TYPE_ALL; // 下载所有录像

         n_hDownLoadHandle
                = netSdk.CLIENT_DownloadByTimeEx2(loginHandle, nChannel, nRecordFileType,
                stTimeStart, stTimeEnd, savedFileName,
                DownloadPosCallback.getInstance(), null, m_DownLoadData, null, scType, null);

        if(n_hDownLoadHandle.longValue() != 0) {
            System.out.println("Downloading RecordFile...");
        } else {
            System.err.println("Download RecordFile Failed!" + ToolKits.getErrorCode() + ENUMERROR.getErrorMessage());
            return false;
        }

        return true;
    }
    /**
     * 下载进度回调
     * 回调建议写成单例模式, 回调里处理数据，需要另开线程
     */
    public static class DownloadPosCallback implements NetSDKLib.fDownLoadPosCallBack {

        private DownloadPosCallback() {}

        private static class CallBackHolder {
            private static final DownloadPosCallback cb = new DownloadPosCallback();
        }

        public static final DownloadPosCallback getInstance() {
            return CallBackHolder.cb;
        }

        @Override
        public void invoke(NetSDKLib.LLong lPlayHandle, int dwTotalSize, int dwDownLoadSize, Pointer dwUser) {

            System.out.println("Download pos： " + dwDownLoadSize*100 / dwTotalSize);
            if(dwDownLoadSize == -1) { // 下载结束
                System.out.println("Downloading Complete. ");
                new StopDownloadTask(lPlayHandle).start();
            }
            System.out.println(dwDownLoadSize);
        }

        private class StopDownloadTask extends Thread {
            private NetSDKLib.LLong lDownloadHandle;

            public StopDownloadTask(NetSDKLib.LLong lDownloadHandle) {
                this.lDownloadHandle = lDownloadHandle;
            }

            public void run() {
                stopDownLoadRecordFile(lDownloadHandle);
            }
        }
    }
    /**
     *  停止下载录像
     * @param hDownLoadHandle 下载句柄
     */
    private static void stopDownLoadRecordFile(NetSDKLib.LLong hDownLoadHandle) {
        if (hDownLoadHandle.longValue() == 0) {
            return;
        }
        netSdk.CLIENT_StopDownload(hDownLoadHandle);
    }
    /**
     *  下载录像
     */
    public void downloadRecordFile() {

        int nStreamType = 0;		// 0-主辅码流,1-主码流,2-辅码流
        int nChannel = 0; 			// 通道号
        NetSDKLib.NET_TIME stTimeStart = new NetSDKLib.NET_TIME(); 	// 开始时间
        NetSDKLib.NET_TIME stTimeEnd = new NetSDKLib.NET_TIME(); 	// 结束时间

        Calendar calendar = Calendar.getInstance();

        System.out.println("默认下载通道0主辅码流的最近10分钟录像 如果想要自己设置通道或时间段请输入1: ");
        String s
                = sc.nextLine();
        if ("1".equals(s.trim())) {
            try {
                System.out.println("请输入通道号(从0开始),开始时间,结束时间：如 0,2022-6-22 16:10:00,2022-6-22 16:30:00");

                String all=sc.nextLine();
                String[] split
                        = all.split(",");
                nChannel = Integer.parseInt(split[0]);

                Date start
                        = format.parse(split[1]);
                calendar.setTime(start);
                setTime(calendar, stTimeStart);
          //      System.out.println("startTime:"+stTimeStart);

                Date end
                        = format.parse(split[2]);
                calendar.setTime(end);
                setTime(calendar, stTimeEnd);

            // System.out.println("stTimeEnd:"+stTimeEnd);
            } catch (ParseException e) {
                System.err.println("时间输入非法");
                return;
            }

        }else {
            calendar.setTime(new Date());
            setTime(calendar, stTimeEnd);

            calendar.add(Calendar.MINUTE, -10);
            setTime(calendar, stTimeStart);

        }
        System.out.println("startTime:"+stTimeStart);
        System.out.println("stTimeEnd:"+stTimeEnd);
        System.out.println("nChannel:"+nChannel);
        String savedFileName = "dowload_" + System.currentTimeMillis() + ".mp4"; // 保存录像文件名

        boolean b
                = downloadRecordFile(nStreamType, nChannel, stTimeStart, stTimeEnd, savedFileName);
        if(!b){
            System.err.println("Start Download By File Failed :" + ENUMERROR.getErrorMessage());
        }
    }

    private void setTime(Calendar calendar, NetSDKLib.NET_TIME stuTime) {
        stuTime.setTime(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }
    NetSDKLib.LLong n_hDownLoadFileHandle =new    NetSDKLib.LLong();

    /**
     * 通用录像文件下载 按文件信息筛选
     */
    public void DownloadRecordByFile() throws UnsupportedEncodingException {

        if (loginHandle.longValue() == 0) {
            System.err.println("请先登录");
            return;
        }

        List<NetSDKLib.NET_RECORDFILE_INFO> files = QueryRecordFile();
        if (files.isEmpty()) {
            System.err.println("查询不到录像文件");
            return;
        }


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

        n_hDownLoadFileHandle = RecordModule.DownloadRecordFileByFileEx(loginHandle, fileInfo, savePath, posCallBack, dataCallBack);
        if (n_hDownLoadFileHandle.longValue() == 0) {
            System.err.println("Start Download By File Failed :" + ENUMERROR.getErrorMessage());
        } else {
            System.out.println("Start Download By File Succeed");
        }
    }

    class LockFileInfo {
        public int  nCluster; // 簇号
        public int	nDriveNo; // 磁盘号
        public int type;//0 普通（非加密） 1重要（加密）

        public LockFileInfo(int nCluster, int nDriveNo,int type) {
            this.nCluster = nCluster;
            this.nDriveNo = nDriveNo;
            this.type=type;
        }

        @Override
        public String toString() {
            return "LockFileInfo{" +
                    "nCluster=" + nCluster +
                    ", nDriveNo=" + nDriveNo +
                    ", type=" + type +
                    '}';
        }
    }

    // 加锁文件列表
    private List<LockFileInfo> lstLockFile = new ArrayList<LockFileInfo>();
    private List<LockFileInfo> unlstLockFile = new ArrayList<LockFileInfo>();

    /**
     *  按文件录像解锁/加密
     */
    public void lockRecordByFile() {
        if (lstLockFile.isEmpty()) {
            queryRecordFile();
        }
        Scanner scanner=new Scanner(System.in);
        System.out.println("1 解密 ，其他加密");
        int next = scanner.nextInt();

    if(next==1){//解密
if(lstLockFile.size()==0){
    System.out.println("没有找到加密的文件");
    return;
}
        for (int i=0;i<lstLockFile.size();i++) {
            LockFileInfo lockFileInfo
                    = lstLockFile.get(i);
            System.out.println("index:"+i+";lockFileInfo:"+lockFileInfo.toString());

        }
        System.out.println("选择需要解密的下标：如1,2,3");
        String indexs
                = sc.nextLine();
        String[] split
                = indexs.split(",");
        for(int i=0;i<split.length;i++){

            lockRecordByFile(lstLockFile.get(Integer.parseInt(split[i])),0);

        }

    }else {//加密
        if(unlstLockFile.size()==0){
            System.out.println("没有找到未加密的文件");
            return;
        }

        for (int i=0;i<unlstLockFile.size();i++) {

            LockFileInfo unlockFileInfo
                    = unlstLockFile.get(i);
            System.out.println("index:"+i+";unlockFileInfo:"+unlockFileInfo.toString());

        }

        System.out.println("选择需要加密的下标：如1,2,3");
        String indexs
                = sc.nextLine();
        String[] split
                = indexs.split(",");
        for(int i=0;i<split.length;i++){

            lockRecordByFile(unlstLockFile.get(Integer.parseInt(split[i])),1);

        }

    }

        lstLockFile.clear();
        unlstLockFile.clear();
    }




    /**
     *  按文件解锁录像
     * @param stFileInfo 文件信息 (queryMarkFile获取)
     */
    public boolean lockRecordByFile(LockFileInfo stFileInfo,int key) {

        NetSDKLib.NET_IN_SET_MARK_FILE stuIn = new NetSDKLib.NET_IN_SET_MARK_FILE();
        stuIn.emLockMode = NetSDKLib.EM_MARKFILE_MODE.EM_MARK_FILE_BY_NAME_MODE;  // 通过文件名方式对录像加锁
        stuIn.emFileNameMadeType = NetSDKLib.EM_MARKFILE_NAMEMADE_TYPE.EM_MARKFILE_NAMEMADE_JOINT; // 拼接文件名方式
        stuIn.nDriveNo = stFileInfo.nDriveNo;
        stuIn.nStartCluster = stFileInfo.nCluster;
     //   stuIn.byImportantRecID = (byte)0; // 清除  0:普通录像（清除） 1:重要录像（加密）
        stuIn.byImportantRecID = (byte)key; //   0:普通录像（清除） 1:重要录像（加密）
        NetSDKLib.NET_OUT_SET_MARK_FILE stuOut = new NetSDKLib.NET_OUT_SET_MARK_FILE();

        boolean bRet = netSdk.CLIENT_SetMarkFile(loginHandle, stuIn, stuOut, 3000);
        if(bRet) {
            System.out.printf("Unlock Record DriveNo[%d] Cluster[%d] Success. \n", stFileInfo.nDriveNo, stFileInfo.nCluster);
        } else {
            System.err.printf("Unlock Record DriveNo[%d] Cluster[%d] Failed! %s \n", stFileInfo.nDriveNo, stFileInfo.nCluster, ToolKits.getErrorCode());
            return false;
        }
        return bRet;
    }


    // 查询录像视频
    public void queryRecordFile() {
        NetSDKLib.NET_TIME stTimeStart = new NetSDKLib.NET_TIME();

        NetSDKLib.NET_TIME stTimeEnd = new NetSDKLib.NET_TIME();

        Calendar calendar = Calendar.getInstance();

        System.out.println("请输入通道号(从0开始),开始时间,结束时间：如 0,2022-6-22 16:10:00,2022-6-22 16:30:00");

        String all=sc.nextLine();
        String[] split
                = all.split(",");

        int nchannel
                =  Integer.parseInt(split[0]) ;


        Date start
                = null;
        try {
            start = format.parse(split[1]);
            calendar.setTime(start);
            setTime(calendar, stTimeStart);

            Date end
                    = format.parse(split[2]);
            calendar.setTime(end);
            setTime(calendar, stTimeEnd);

        } catch (ParseException e) {
            e.printStackTrace();
        }


        //**************按时间查找视频文件**************
        int nFileCount = 200; //每次查询的最大文件个数
        NetSDKLib.NET_RECORDFILE_INFO[] numberFile = (NetSDKLib.NET_RECORDFILE_INFO[]) new NetSDKLib.NET_RECORDFILE_INFO().toArray(nFileCount);
        int maxlen = nFileCount * numberFile[0].size();

        IntByReference outFileCoutReference = new IntByReference(0);

        boolean cRet = netSdk.CLIENT_QueryRecordFile(
                loginHandle, nchannel, 0, stTimeStart, stTimeEnd, null, numberFile, maxlen, outFileCoutReference, 5000, false);

        if (cRet) {
            System.out.println("QueryRecordFile  Succeed! \n" + "查询到的视频个数：" + outFileCoutReference.getValue()
                    + "\n" + "码流类型：" + numberFile[0].bRecType);
            for(int i=0;i<outFileCoutReference.getValue();i++){
                NetSDKLib.NET_RECORDFILE_INFO info
                        = numberFile[i];
                System.out.println("fileName"+ new String(info.filename));
                System.out.println("bImportantRecID:"+info.bImportantRecID);// 0:普通录像(未加密) 1:重要录像（已加密）
                if(info.bImportantRecID==0){
                    unlstLockFile.add(new LockFileInfo(info.startcluster,info .driveno,info.bImportantRecID));
                }else {
                    lstLockFile.add(new LockFileInfo(info.startcluster,info .driveno,info.bImportantRecID));
                }

            }

        } else {
            System.err.println("QueryRecordFile  Failed!" + netSdk.CLIENT_GetLastError());
        }


    }


    /**
     * 查找录像 需要指定查询录像的最大个数
     * 这个接口只是用于协助演示录像下载用的 比较简单 需要手动预填最大文件数量 实际开发请走 FindFile 接口
     */
    public List<NetSDKLib.NET_RECORDFILE_INFO> QueryRecordFile() throws UnsupportedEncodingException {
        NetSDKLib.NET_TIME stTimeStart = new NetSDKLib.NET_TIME();
      //  stTimeStart.setTime(2022, 6, 21, 0, 0, 0);

        NetSDKLib.NET_TIME stTimeEnd = new NetSDKLib.NET_TIME();
    //    stTimeEnd.setTime(2022, 6, 21, 10, 15, 0);
        Calendar calendar = Calendar.getInstance();
        System.out.println("开始时间,结束时间：如 2022-6-22 16:10:00,2022-6-22 16:30:00");

        String all=sc.nextLine();
        String[] split
                = all.split(",");


        Date start
                = null;
        Date end
                = null;
        try {
            start = format.parse(split[0]);
            end = format.parse(split[1]);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.setTime(start);
        setTime(calendar, stTimeStart);

        calendar.setTime(end);
        setTime(calendar, stTimeEnd);


        //////////// 按时间查找视频文件 //////////
        ////////////////////////////////////////

        int maxFileCount = 50; // 查询的最大文件个数
        NetSDKLib.NET_RECORDFILE_INFO[] fileInfos = new NetSDKLib.NET_RECORDFILE_INFO[maxFileCount];
        for (int i = 0; i < maxFileCount; i++) {
            fileInfos[i] = new NetSDKLib.NET_RECORDFILE_INFO();
        }
        int retCount = RecordModule.QueryRecordFileByTimeLimit(loginHandle, maxFileCount, fileInfos, stTimeStart, stTimeEnd);
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
                    .append("通道号：").append(fileInfos[i].ch).append("\n")
                    .append("加密：").append(fileInfos[i].bImportantRecID).append("\n");//0:普通录像（清除） 1:重要录像（加密）
        }
        System.out.println(info.toString());

        return new ArrayList<>(Arrays.asList(fileInfos).subList(0, retCount));
    }

    /**
     * 设置回放时的码流类型
     * @param m_streamType 码流类型
     */
    public boolean setStreamType(int m_streamType) {
        int emType = NetSDKLib.EM_USEDEV_MODE.NET_RECORD_STREAM_TYPE; // 回放录像枚举
        IntByReference steamType = new IntByReference(m_streamType); // 0-主辅码流,1-主码流,2-辅码流
        return netSdk.CLIENT_SetDeviceMode(loginHandle, emType, steamType.getPointer());
    }

    /**
     * 获取设备软件版本
     *
     */
    public void QueryDevDeviceVersionStateTest() {
        NetSDKLib.NETDEV_VERSION_INFO info = new NetSDKLib.NETDEV_VERSION_INFO();
        info.write();
        boolean bRet = netSdk.CLIENT_QueryDevState(loginHandle, NET_DEVSTATE_SOFTWARE, info.getPointer(), info.size(), new IntByReference(0), 3000);
        if (!bRet) {
            System.err.println("QueryDevState DEV STATE of SOFTWARE failed: " + ToolKits.getErrorCode());
            return;
        }
        info.read();
        System.out.println("QueryDevState DEV STATE of SOFTWARE succeed");

        System.out.println("szSoftWareVersion 软件版本: " + new String(info.szSoftWareVersion).trim());
        System.out.println("szDevSerialNo 序列号: " + new String(info.szDevSerialNo).trim());
        int buildData = info.dwSoftwareBuildDate;
        int day = buildData & 0xff;
        buildData >>= 8;
        int month = buildData & 0xff;
        int year = buildData >> 8;
        System.out.println("BuildData 编译日期: " + year + "-" + month + "-" + day);
    }

    /**
     * 获取远程设备编码配置
     * 三代协议
     * 注：
     * 1) 我们建议把主码流配置看成一个整体, 即主码流组统一做相同配置
     * 2) 辅码流在不同设备上能力集差异较大 我们建议只使用辅码流 1, 即只关注辅码流组的第一个配置
     */
    public void getDeviceEncode() {
        final String cfgCmd = EM_NEW_CONFIG.CFG_CMD_ENCODE.getValue();
        NetSDKLib.CFG_ENCODE_INFO encodeInfo = new NetSDKLib.CFG_ENCODE_INFO();
        encodeInfo.nChannelID = 0;
        boolean ret = ToolKits.GetDevConfig(loginHandle, 0, cfgCmd, encodeInfo);
        if (!ret) {
            System.err.println("获取远程设备编码配置失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        // 打印下当前的设备参数: 主/辅 码流的分辨率, 帧率和码率
        StringBuilder info = null;
        try {
            info = new StringBuilder()
                    .append(String.format("SN:%s, Chn:%03d, Encode Config:\n", new String(m_stDeviceInfo.sSerialNumber, encode).trim(), 0));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // 主码流
        if (encodeInfo.nValidCountMainStream > 0) {
            info.append("Main Stream 主码流:\n")
                    .append("    Resolution 分辨率: ").append(String.format("( %04d x %04d )\n",
                    encodeInfo.stuMainStream[0].stuVideoFormat.nWidth,
                    encodeInfo.stuMainStream[0].stuVideoFormat.nHeight))
                    .append("    BitRate 视频码流(kbps): ").append(encodeInfo.stuMainStream[0].stuVideoFormat.nBitRate).append("\n")
                    .append("    FrameRate 帧率: ").append(encodeInfo.stuMainStream[0].stuVideoFormat.nFrameRate).append("\n")
                    .append("    IFrameInterval I帧间隔: ").append(encodeInfo.stuMainStream[0].stuVideoFormat.nIFrameInterval).append("\n")
                    .append("    音频编码模式: ").append(encodeInfo.stuMainStream[0].stuAudioFormat.nMode).append("\n")
                    .append("    音频压缩模式: ").append(encodeInfo.stuMainStream[0].stuAudioFormat.emCompression).append("\n")
                    .append("    音频使能: ").append(encodeInfo.stuMainStream[0].bAudioEnable).append("\n");
        }

        // 辅码流
        if (encodeInfo.nValidCountExtraStream > 0) {
            info.append("Sub Stream 辅码流(1):\n")
                    .append("    Resolution 分辨率: ").append(String.format("( %04d x %04d )\n",
                    encodeInfo.stuExtraStream[0].stuVideoFormat.nWidth,
                    encodeInfo.stuExtraStream[0].stuVideoFormat.nHeight))
                    .append("    BitRate 视频码流(kbps): ").append(encodeInfo.stuExtraStream[0].stuVideoFormat.nBitRate).append("\n")
                    .append("    FrameRate 帧率: ").append(encodeInfo.stuExtraStream[0].stuVideoFormat.nFrameRate).append("\n")
                    .append("    IFrameInterval I帧间隔: ").append(encodeInfo.stuExtraStream[0].stuVideoFormat.nIFrameInterval).append("\n")
                    .append("    音频编码模式: ").append(encodeInfo.stuMainStream[0].stuAudioFormat.nMode).append("\n")
                    .append("    音频压缩模式: ").append(encodeInfo.stuMainStream[0].stuAudioFormat.emCompression).append("\n")
                    .append("    音频使能: ").append(encodeInfo.stuMainStream[0].bAudioEnable).append("\n");


        }

        System.out.println(info.toString());
    }


    public void GetIVSSDisk() {
        Scanner scanner=new Scanner(System.in);
        NET_IN_STORAGE_DEV_INFOS inInfo = new NET_IN_STORAGE_DEV_INFOS();
        //  * 6、阵列信息：CLIENT_QueryDevInfo+NET_QUERY_DEV_STORAGE_INFOS，emVolumeType=VOLUME_TYPE_RAID
        //     * 7、硬盘容量：CLIENT_QueryDevInfo+NET_QUERY_DEV_STORAGE_INFOS，emVolumeType=VOLUME_TYPE_ALL
        System.out.println("输入1  VOLUME_TYPE_RAID，输入2 VOLUME_TYPE_ALL");
        int nextInt
                = scanner.nextInt();

        if(nextInt==1){
            inInfo.emVolumeType=NET_VOLUME_TYPE.VOLUME_TYPE_RAID;
        }else {
            inInfo.emVolumeType = NET_VOLUME_TYPE.VOLUME_TYPE_ALL; // 单盘可以查询支持的工作组
        }

        NET_OUT_STORAGE_DEV_INFOS outInfo = new NET_OUT_STORAGE_DEV_INFOS();
        for (int i = 0; i < outInfo.stuStoregeDevInfos.length; i ++)
        {
            outInfo.stuStoregeDevInfos[i].dwSize = outInfo.stuStoregeDevInfos[0].dwSize;
        }
        inInfo.write();
        outInfo.write();
        boolean bRet = netSdk.CLIENT_QueryDevInfo(loginHandle, NetSDKLib.NET_QUERY_DEV_STORAGE_INFOS, inInfo.getPointer(), outInfo.getPointer(), null, 5000);
        inInfo.read();
        outInfo.read();
        if (!bRet) {
            System.err.println("CLIENT_QueryDevInfo Failed!" + ToolKits.getErrorCode());
            return;
        }
        for (int i = 0; i < outInfo.nDevInfosNum; i ++)
        {
            NET_STORAGE_DEVICE device = outInfo.stuStoregeDevInfos[i];
            System.out.println("槽位:"+device.nPhysicNo); // 槽位
            System.out.println("名称:"+new String(device.szName).trim());    // 名称
            System.out.println("总空间, byte:"+device.nTotalSpace);
            System.out.println("剩余空间, byte:"+device.nFreeSpace);
            System.out.println("型号:"+new String(device.szModule).trim()); // 型号
            String valume[] = {"0-物理卷", "1-Raid卷", "2-VG虚拟卷", "3-ISCSI", "4-独立物理卷", "5-全局热备卷", "6-NAS卷(包括FTP, SAMBA, NFS)", "7-独立RAID卷（指没有加入到，虚拟卷组等组中）"};
            System.out.println("卷类型:"+valume[device.byVolume]); //
            if (device.byVolume == 1) {
                System.out.println("Raid Stat: " + device.stuRaid.nState);
            } else { // 单盘问题
                String arState[] =
                        {
                                "Offline-物理硬盘脱机状态", "Running-物理硬盘运行状态", "Active-RAID活动", "Sync-RAID同步", "Spare-RAID热备(局部)",
                                "Faulty-RAID失效", "Rebuilding-RAID重建", "Removed-RAID移除", "WriteError-RAID写错误", "WantReplacement-RAID需要被替换",
                                "Replacement-RAID是替代设备", "GlobalSpare-全局热备", "Error-一般错误，部分分区可用", "RaidSub-该盘目前是单盘，原先是块Raid子盘,有可能在重启后自动加入Raid",
                        };
                if ((int)device.byState != NetFinalVar.NET_STORAGE_DEV_RUNNING) {//物理硬盘运行状态
                    System.out.println("物理硬盘运行状态:"+arState[(int)device.byState]); // 状态
                    continue;
                }
                if (device.emPreDiskCheck != EM_STORAGE_DISK_PREDISKCHECK.EM_STORAGE_DISK_PREDISKCHECK_UNKNOWN) {
                    System.out.println("device.emPreDiskCheck " + device.emPreDiskCheck);
                }
            }
        }
    }
//8、移动侦测配置


    public void SetVideoAnalyseModule() {
        int channel = 0;
        String command = NetSDKLib.CFG_CMD_MOTIONDETECT;

        CFG_MOTION_INFO msg = new CFG_MOTION_INFO();

        // 获取
        if (ToolKits.GetDevConfig(loginHandle, channel, command, msg)) {
            System.out.println("bEnable:"+msg.bEnable);
                    msg.bEnable = 1;

            if (ToolKits.SetDevConfig(loginHandle, channel, command, msg)) {
                System.out.println("设置使能成功!");
            } else {
                System.err.println("设置使能失败!" + ToolKits.getErrorCode());
            }
        }
    }

    /**
     * StartListen
     */
    public static void startListenAlarm() {
        /// Alarm CallBack
        netSdk.CLIENT_SetDVRMessCallBack(MessCallBack.getInstance(), null);

        boolean b
                = netSdk.CLIENT_StartListenEx(loginHandle);
        if(!b){
            System.err.println("CLIENT_StartListenEx Failed." + ToolKits.getErrorCode());
        }
    }

    /**
     * StopListen
     */
    public static void stopListenAlarm() {

        boolean b
                = netSdk.CLIENT_StopListen(loginHandle);
        if(!b){
            System.err.println("CLIENT_StopListen Failed." + ToolKits.getErrorCode());
        }
    }

    // 获取摄像机通道状态
    public void QueryChannelState() {
        int nQueryType = NetSDKLib.NET_QUERY_GET_CAMERA_STATE;

        // 入参
        NetSDKLib.NET_IN_GET_CAMERA_STATEINFO stIn = new NetSDKLib.NET_IN_GET_CAMERA_STATEINFO();
        stIn.bGetAllFlag = 1; // 1-true,查询所有摄像机状态

        // 摄像机通道信息
        int chnCount = m_stDeviceInfo.byChanNum;  // 通道个数
        NetSDKLib.NET_CAMERA_STATE_INFO[] cameraInfo = new NetSDKLib.NET_CAMERA_STATE_INFO[chnCount];
        for(int i = 0; i < chnCount; i++) {
            cameraInfo[i] = new NetSDKLib.NET_CAMERA_STATE_INFO();
        }

        // 出参
        NetSDKLib.NET_OUT_GET_CAMERA_STATEINFO stOut = new NetSDKLib.NET_OUT_GET_CAMERA_STATEINFO();
        stOut.nMaxNum = chnCount;
        stOut.pCameraStateInfo = new Memory(cameraInfo[0].size() * chnCount);
        stOut.pCameraStateInfo.clear(cameraInfo[0].size() * chnCount);

        ToolKits.SetStructArrToPointerData(cameraInfo, stOut.pCameraStateInfo);  // 将数组内存拷贝到Pointer

        stIn.write();
        stOut.write();
        boolean bRet = netSdk.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
        stIn.read();
        stOut.read();

        if(bRet) {
            ToolKits.GetPointerDataToStructArr(stOut.pCameraStateInfo, cameraInfo);  // 将 Pointer 的内容 输出到   数组

            System.out.println("查询到的摄像机通道状态有效个数：" + stOut.nValidNum);

            for(int i = 0; i < stOut.nValidNum; i++) {
                System.out.println("通道号：" + cameraInfo[i].nChannel);
                //对应 EM_CAMERA_STATE_TYPE
                System.out.println("连接状态：" +cameraInfo[i].emConnectionState);
            }
        } else {
            System.err.println("QueryDev Failed!" + ToolKits.getErrorCode());
        }
    }
    private static class MessCallBack implements NetSDKLib.fMessCallBack {
        private MessCallBack() {
        }

        private static class MessCallBackHolder {
            private static MessCallBack msgCallBack = new MessCallBack();
        }

        public static MessCallBack getInstance() {
            return MessCallBackHolder.msgCallBack;
        }

        public boolean invoke(int lCommand, final NetSDKLib.LLong lLoginID,
                              Pointer pStuEvent, int dwBufLen, String strDeviceIP,
                              NativeLong nDevicePort, Pointer dwUser) {


            if (pStuEvent == null || dwBufLen <= 0) {
                return false;
            }

            /// 具体事件类,
            switch (lCommand) {
                case NetSDKLib.NET_ALARM_VIDEOBLIND: // alarm of video blind  视频遮挡事件
                {
                    NetSDKLib.ALARM_VIDEO_BLIND_INFO msg = new NetSDKLib.ALARM_VIDEO_BLIND_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println("视频遮挡事件");
                    System.out.println("nAction : " + msg.nAction);
                    System.out.println("Time : " + msg.stuTime.toString());
                    break;
                }
                case NetSDKLib.NET_ALARM_AUDIO_ANOMALY: // 音频异常事件
                {
                    ALARM_AUDIO_ANOMALY  msg= new ALARM_AUDIO_ANOMALY();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println("音频异常事件");
                    System.out.println("nAction : " + msg.dwAction);
                    System.out.println("dwChannelID : " + msg.dwChannelID);
                    System.out.println("nDecibel : " + msg.nDecibel);
                    System.out.println("nFrequency : " + msg.nFrequency);
                    break;
                }
                case NetSDKLib.NET_EVENT_VIDEOABNORMALDETECTION: // 视频异常事件  对应ALARM_VIDEOABNORMAL_DETECTION_INFO
                {
                    NetSDKLib.ALARM_VIDEOABNORMAL_DETECTION_INFO msg= new NetSDKLib.ALARM_VIDEOABNORMAL_DETECTION_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                    System.out.println("视频异常事件");
                    System.out.println("nChannelID : " + msg.nChannelID);
                    System.out.println("PTS : " + msg.PTS);
                    System.out.println("UTC : " + msg.UTC);
                    System.out.println("nEventAction : " + msg.nEventAction);//事件动作,0表示脉冲事件,1表示持续性事件开始,2表示持续性事件结束;
                    System.out.println("nType : " + msg.nType);//检测类型,0-视频丢失, 1-视频遮挡, 2-画面冻结, 3-过亮, 4-过暗, 5-场景变化
                    System.out.println("nValue : " + msg.nValue);//检测值,值越高表示视频质量越差, GB30147定义
                    break;
                }
                default:
                    System.out.println("lCommand:"+lCommand);
                    break;
            }

            return true;

        }

    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this , "下载录像" , "downloadRecordFile")));
        menu.addItem((new CaseMenu.Item(this , "录像锁状态查询" , "DownloadRecordByFile")));
        menu.addItem((new CaseMenu.Item(this , "录像文件加锁/解锁" , "lockRecordByFile")));
        menu.addItem((new CaseMenu.Item(this , "查询录像视频" , "queryRecordFile")));
        menu.addItem((new CaseMenu.Item(this , "获取设备软件版本" , "QueryDevDeviceVersionStateTest")));
        menu.addItem((new CaseMenu.Item(this , "分辨率、码流、音视频编码" , "getDeviceEncode"))); //  是否有声音
        menu.addItem((new CaseMenu.Item(this , "阵列信息 硬盘容量" , "GetIVSSDisk")));
        menu.addItem((new CaseMenu.Item(this , "移动侦测配置" , "SetVideoAnalyseModule")));
        menu.addItem((new CaseMenu.Item(this , "通道是否在线" , "QueryChannelState")));
        menu.addItem((new CaseMenu.Item(this , "开始监听" , "startListenAlarm")));
        menu.addItem((new CaseMenu.Item(this , "停止监听" , "stopListenAlarm")));
        menu.run();
    }

    public static void main(String[] args) {
        HaoyunDemo haoyunDemo=new HaoyunDemo();
        System.out.println("输入 ip,port,user, password 格式如：172.11.243.30,37777,admin,admin123");
        String s
                = sc.nextLine();
        String[] split
                = s.split(",");
        InitTest(split[0],Integer.parseInt(split[1]),split[2],split[3]);
        haoyunDemo.RunTest();
        LoginOut();
    }
}
