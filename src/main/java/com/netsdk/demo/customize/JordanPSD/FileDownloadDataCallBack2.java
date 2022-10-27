package com.netsdk.demo.customize.JordanPSD;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author 47040
 * @since Created at 2021/6/9 11:05
 */
public class FileDownloadDataCallBack2 implements NetSDKLib.fDataCallBack {

    private FileDownloadDataCallBack2() {
    }

    public static FileDownloadDataCallBack2 getInstance() {
        return FileDownloadDataCallBackHolder.dataCB;
    }

    public static final BlockingQueue<byte[]> downloadDataQueue2 = new LinkedBlockingQueue<>(10000);

    public int invoke(NetSDKLib.LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, Pointer dwUser) {

        if (dwBufSize == 0) return 1;
        // byte[] data = pBuffer.getByteArray(0, dwBufSize);   // 这是二进制码流数据, 如果有其他用途可以从这里取出来

        // 注意 这里的 lRealHandle 和调用 CLIENT_DownloadByRecordFileEx 时获取的 m_hDownLoadHandle 值相同
        // 请以这个值判断此回调信息属于哪个下载任务 数据返回严格按照设备数据发回的顺序 但并没有序列号 所以请不要在此回调内做任何耗时甚至阻塞回调的操作
        // System.out.println("lRealHandle: " + lRealHandle.longValue() + ", DownLoad DataCallBack [ " + dwDataType + " ]");

        // 取数据并强制延时 5ms, 这里延时是为了模拟上层应用取数据很慢的场景

        downloadDataQueue2.offer(pBuffer.getByteArray(0, dwBufSize));
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 注意这里 必须填 1
        return 1;
    }

    private static class FileDownloadDataCallBackHolder {
        private static final FileDownloadDataCallBack2 dataCB = new FileDownloadDataCallBack2();
    }
}
