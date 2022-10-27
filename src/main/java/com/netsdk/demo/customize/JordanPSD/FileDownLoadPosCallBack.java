package com.netsdk.demo.customize.JordanPSD;

import com.netsdk.demo.customize.JordanPSD.module.RecordModule;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.Utils;
import com.sun.jna.Pointer;

import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 下载进度会掉
 *
 * @author 47040
 * @since Created at 2021/5/31 16:05
 */
public class FileDownLoadPosCallBack implements NetSDKLib.fTimeDownLoadPosCallBack {


    private static volatile FileDownLoadPosCallBack instance;

    // 跨平台编码
    private final Charset encode = Charset.forName(Utils.getPlatformEncode());

    private final BlockingQueue<DownloadPosInfo> downloadPosQueue = new LinkedBlockingQueue<>(1000);

    public static FileDownLoadPosCallBack getInstance() {
        if (instance != null) return instance;
        synchronized (FileDownLoadPosCallBack.class) {
            if (instance == null) instance = new FileDownLoadPosCallBack();
        }
        return instance;
    }

    public void invoke(NetSDKLib.LLong lDownloadHandle, int dwTotalSize, int dwDownLoadSize, int index, NetSDKLib.NET_RECORDFILE_INFO.ByValue recordfileinfo, Pointer dwUser) {
        // 下载完成
        if (-1 == dwDownLoadSize) {
            System.out.println("Download finished. Download Handle: " + lDownloadHandle.longValue());
            new StopDownloadTask(lDownloadHandle).start();   // 注意这里需要另起线程
        }
        if (dwTotalSize != 0 && dwTotalSize != dwDownLoadSize) {
            DownloadPosInfo info = new DownloadPosInfo();
            info.downloadPosHandle = lDownloadHandle;
            info.totalSize = dwTotalSize;
            info.downloadSize = dwDownLoadSize;
            info.downloadPos = (double) dwDownLoadSize * 100 / (double) dwTotalSize;
            downloadPosQueue.offer(info);
            // 注意 这里的 lDownloadHandle 和调用 CLIENT_DownloadByRecordFileEx 时获取的 m_hDownLoadHandle 值相同
            // 请以这个值判断此回调信息属于哪个下载任务
            System.out.println("lDownloadHandle: " + lDownloadHandle.longValue() + ", Download Pos: [ " + dwTotalSize + " ]" + " [ " + dwDownLoadSize + " ]");
        }
    }

    private static class StopDownloadTask extends Thread {
        private final NetSDKLib.LLong lDownloadHandle;

        public StopDownloadTask(NetSDKLib.LLong lDownloadHandle) {
            this.lDownloadHandle = lDownloadHandle;
        }

        public void run() {
            RecordModule.StopDownLoadRecordFile(lDownloadHandle);
        }
    }
}
