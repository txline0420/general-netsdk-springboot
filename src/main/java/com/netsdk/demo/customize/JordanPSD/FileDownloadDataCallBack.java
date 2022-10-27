package com.netsdk.demo.customize.JordanPSD;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

/**
 * @author 47040
 * @since Created at 2021/6/9 11:05
 */
public class FileDownloadDataCallBack implements NetSDKLib.fDataCallBack {

    private FileDownloadDataCallBack() {
    }

    public static FileDownloadDataCallBack getInstance() {
        return FileDownloadDataCallBackHolder.dataCB;
    }

    public int invoke(NetSDKLib.LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, Pointer dwUser) {

        // byte[] data = pBuffer.getByteArray(0, dwBufSize);   // 这是二进制码流数据, 如果有其他用途可以从这里取出来

        // 注意 这里的 lRealHandle 和调用 CLIENT_DownloadByRecordFileEx 时获取的 m_hDownLoadHandle 值相同
        // 请以这个值判断此回调信息属于哪个下载任务 数据返回严格按照设备数据发回的顺序 但并没有序列号 所以请不要在此回调内做任何耗时甚至阻塞回调的操作
        System.out.println("lRealHandle: " + lRealHandle.longValue() + ", DownLoad DataCallBack [ " + dwDataType + " ]");

        // 转码流会用到以下数据类型 如果不使用请无视
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
        return 1;
    }

    private static class FileDownloadDataCallBackHolder {
        private static final FileDownloadDataCallBack dataCB = new FileDownloadDataCallBack();
    }
}
