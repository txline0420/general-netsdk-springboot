package com.netsdk.lib.structure;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.EM_REAL_DATA_TYPE;
import com.sun.jna.Pointer;

/**
 * 自适应下载录像 按时间 {@link NetSDKLib#CLIENT_AdaptiveDownloadByTime} 入参
 * 出参 {@link NET_OUT_ADAPTIVE_DOWNLOAD_BY_TIME}
 *
 * @author 47040
 * @since Created at 2021/6/19 18:35
 */
public class NET_IN_ADAPTIVE_DOWNLOAD_BY_TIME extends NetSDKLib.SdkStructure {

    /**
     * 结构体大小
     */
    public int dwSize;
    /**
     * 通道号
     */
    public int nChannelID;
    /**
     * 录像类型枚举值 {@link NetSDKLib.EM_QUERY_RECORD_TYPE}
     */
    public int emRecordType;
    /**
     * 下载的文件路径
     */
    public Pointer szSavedFileName;
    /**
     * 开始时间
     */
    public NetSDKLib.NET_TIME stStartTime;
    /**
     * 结束时间
     */
    public NetSDKLib.NET_TIME stStopTime;
    /**
     * 进度回调
     */
    public NetSDKLib.fTimeDownLoadPosCallBack cbDownLoadPos;
    /**
     * 进度回调用户信息
     */
    public Pointer dwPosUser;
    /**
     * 数据回调
     */
    public NetSDKLib.fDataCallBack fDownLoadDataCallBack;
    /**
     * 回调的数据类型 枚举值 {@link EM_REAL_DATA_TYPE}
     */
    public int emDataType;
    /**
     * 数据回调用户信息
     */
    public Pointer dwDataUser;
    /**
     * 加密录像密码，密码长度不小于8
     */
    public byte[] szEncryptPasswd = new byte[64];

    public NET_IN_ADAPTIVE_DOWNLOAD_BY_TIME() {
        dwSize = this.size();
    }

}
