package com.netsdk.demo.customize.JordanPSD.module;

import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_IN_ADAPTIVE_DOWNLOAD_BY_TIME;
import com.netsdk.lib.structure.NET_IN_DOWNLOAD_BYFILE_SELFADAPT;
import com.netsdk.lib.structure.NET_OUT_ADAPTIVE_DOWNLOAD_BY_TIME;
import com.netsdk.lib.structure.NET_OUT_DOWNLOAD_BYFILE_SELFADAPT;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import static com.netsdk.demo.util.StructFieldChooser.GetSelectedSingleFieldValue;

/**
 * @author 47040
 * @since Created at 2021/5/27 11:44
 */
public class RecordModule {

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;

    /**
     * 开始查询文件信息
     *
     * @param m_hLoginHandle 登录句柄
     * @param type           查询类型
     * @param condition      查询条件
     * @param waitTime       超时时间(毫秒)
     * @return 查询句柄
     */
    public static NetSDKLib.LLong FindFileEx(NetSDKLib.LLong m_hLoginHandle, int type, Structure condition, int waitTime) {

        condition.write();
        NetSDKLib.LLong m_hFindFileHandle = NetSdk.CLIENT_FindFileEx(m_hLoginHandle, type, condition.getPointer(), null, waitTime);
        condition.read();
        if (m_hFindFileHandle.longValue() == 0) {
            System.err.println("FindFileEx Failed: " + ENUMERROR.getErrorCode());
        }
        return m_hFindFileHandle;
    }

    /**
     * 获取文件数量
     *
     * @param m_hFindFileHandle 查询句柄
     * @param waitTime          超时时间(毫秒)
     * @return 失败返回  -1 成功返回 实际数量
     */
    public static int GetTotalFileCount(NetSDKLib.LLong m_hFindFileHandle, int waitTime) {
        IntByReference nCount = new IntByReference(0);
        boolean ret = NetSdk.CLIENT_GetTotalFileCount(m_hFindFileHandle, nCount, null, waitTime);
        if (!ret) {
            System.err.println("GetTotalFileCount Failed:" + ENUMERROR.getErrorCode());
            return -1;
        }
        return nCount.getValue();
    }

    /**
     * 获取文件查询信息
     *
     * @param m_hFindFileHandle 查询句柄
     * @param structures        查询信息数组
     * @param memoryHolder      与信息数组大小相同的 Native 内存
     * @param waitTime          超时时间
     * @return 查询到的数量
     */
    public static int FindNextFileEx(NetSDKLib.LLong m_hFindFileHandle, Structure[] structures, Pointer memoryHolder, int waitTime) {

        ToolKits.SetStructArrToPointerData(structures, memoryHolder);
        int memorySize = structures[0].size() * structures.length;
        int nRet = NetSdk.CLIENT_FindNextFileEx(m_hFindFileHandle, structures.length, memoryHolder, memorySize, null, waitTime);
        if (nRet < 0) {
            System.err.println("FindNextFileEx Failed:" + ENUMERROR.getErrorCode());
        } else if (nRet > 0) {
            ToolKits.GetPointerDataToStructArr(memoryHolder, structures);
        }
        return nRet;
    }

    // FindNextFileEx 查询录像专用
    public static int FindNextMediaFileEx(NetSDKLib.LLong m_hFindFileHandle, NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[] mediaInfos, Pointer memoryHolder, int waitTime) {
        if (mediaInfos == null || mediaInfos.length == 0) return -1;
        long offset = 0;
        int sizeOfStruct = mediaInfos[0].dwSize;
        for (NetSDKLib.NET_OUT_MEDIA_QUERY_FILE mediaInfo : mediaInfos) {
            mediaInfo.writeField("dwSize");
            memoryHolder.write(offset, mediaInfo.getPointer().getByteArray(0, sizeOfStruct), 0, sizeOfStruct);
            offset += sizeOfStruct;
        }

        int memorySize = sizeOfStruct * mediaInfos.length;
        int nRet = NetSdk.CLIENT_FindNextFileEx(m_hFindFileHandle, mediaInfos.length, memoryHolder, memorySize, null, waitTime);
        if (nRet < 0) {
            System.err.println("FindNextFileEx Failed:" + ENUMERROR.getErrorCode());
        } else if (nRet > 0) {
            GetMediaInfosFromPointer(mediaInfos, memoryHolder);
        }
        return nRet;
    }

    // 选择性读取
    private static void GetMediaInfosFromPointer(NetSDKLib.NET_OUT_MEDIA_QUERY_FILE[] mediaInfos, Pointer bufferData) {
        if (mediaInfos == null || mediaInfos.length == 0) return;
        long offset = 0;
        int sizeOfStruct = mediaInfos[0].size();
        Pointer tmpBuffer = new Memory(sizeOfStruct);
        for (NetSDKLib.NET_OUT_MEDIA_QUERY_FILE info : mediaInfos) {
            tmpBuffer.clear(sizeOfStruct);
            tmpBuffer.write(0, bufferData.getByteArray(offset, sizeOfStruct), 0, sizeOfStruct);
            offset += sizeOfStruct;

            NetSDKLib.NET_OUT_MEDIA_QUERY_FILE mediaInfo = info;
            if (mediaInfo == null) mediaInfo = new NetSDKLib.NET_OUT_MEDIA_QUERY_FILE();

            // todo 参考下面的方式自由添加字段
            mediaInfo.nChannelID = (int) GetSelectedSingleFieldValue("nChannelID", mediaInfo, tmpBuffer);
            mediaInfo.stuStartTime = (NetSDKLib.NET_TIME) GetSelectedSingleFieldValue("stuStartTime", mediaInfo, tmpBuffer);
            mediaInfo.stuEndTime = (NetSDKLib.NET_TIME) GetSelectedSingleFieldValue("stuEndTime", mediaInfo, tmpBuffer);
            mediaInfo.byFileType = (byte) GetSelectedSingleFieldValue("byFileType", mediaInfo, tmpBuffer);
            mediaInfo.byPartition = (byte) GetSelectedSingleFieldValue("byPartition", mediaInfo, tmpBuffer);
            mediaInfo.byVideoStream = (byte) GetSelectedSingleFieldValue("byVideoStream", mediaInfo, tmpBuffer);
            mediaInfo.nCluster = (int) GetSelectedSingleFieldValue("nCluster", mediaInfo, tmpBuffer);
            mediaInfo.szFilePath = (byte[]) GetSelectedSingleFieldValue("szFilePath", mediaInfo, tmpBuffer);
            mediaInfo.nDriveNo = (int) GetSelectedSingleFieldValue("nDriveNo", mediaInfo, tmpBuffer);
            mediaInfo.nFileSizeEx = (long) GetSelectedSingleFieldValue("nFileSizeEx", mediaInfo, tmpBuffer);
            mediaInfo.nTotalFrame = (int) GetSelectedSingleFieldValue("nTotalFrame", mediaInfo, tmpBuffer);
            mediaInfo.emFileState = (int) GetSelectedSingleFieldValue("emFileState", mediaInfo, tmpBuffer);
        }
    }

    /**
     * 结束查询
     *
     * @param m_hFindFileHandle 查询句柄
     */
    public static boolean FindCloseEx(NetSDKLib.LLong m_hFindFileHandle) {
        if (m_hFindFileHandle.longValue() == 0) {
            System.err.println("FindFileHandle invalid");
            return false;
        }

        if (!NetSdk.CLIENT_FindCloseEx(m_hFindFileHandle)) {
            System.err.println("FindCloseEx Failed:" + ENUMERROR.getErrorCode());
            return false;
        } else {
            m_hFindFileHandle.setValue(0);
            return true;
        }
    }

    /**
     * 查询录像文件
     *
     * @return 查询到的文件数量 -1 表示失败
     */
    public static int QueryRecordFileByTimeLimit(NetSDKLib.LLong m_hLoginHandle, int maxFileCount, Structure[] fileInfos,
                                                 NetSDKLib.NET_TIME stTimeStart, NetSDKLib.NET_TIME stTimeEnd) {
        int bufferSize = maxFileCount * fileInfos[0].size();
        IntByReference retCountRef = new IntByReference(0);
        Pointer pFileInfo = new Memory(bufferSize);
        ToolKits.SetStructArrToPointerData(fileInfos, pFileInfo);

        // channel 查全部
        boolean ret = NetSdk.CLIENT_QueryRecordFile(m_hLoginHandle, -1, 0,
                stTimeStart, stTimeEnd, null, pFileInfo, bufferSize, retCountRef, 5000, false);
        if (!ret) {
            System.err.println("QueryRecordFile  Failed!" + ENUMERROR.getErrorCode());
            return -1;
        }
        ToolKits.GetPointerDataToStructArr(pFileInfo, fileInfos);
        return retCountRef.getValue();
    }

    /**
     * 按文件下载录像
     *
     * @return 下载句柄
     */
    public static NetSDKLib.LLong DownloadRecordFileByFileEx(NetSDKLib.LLong m_hLoginHandle,
                                                             NetSDKLib.NET_RECORDFILE_INFO fileInfo, String savePath,
                                                             NetSDKLib.fTimeDownLoadPosCallBack posCallBack, NetSDKLib.fDataCallBack dataCallBack) {
        Pointer pSavePath = new NativeString(savePath).getPointer();

        NetSDKLib.LLong m_hDownLoadHandle = NetSdk.CLIENT_DownloadByRecordFileEx(
                m_hLoginHandle, fileInfo, pSavePath, posCallBack, null, dataCallBack, null, null);
        if (m_hDownLoadHandle.longValue() == 0) {
            System.err.println("DownloadRecordFileByFileEx: " + ENUMERROR.getErrorCode());
        }
        return m_hDownLoadHandle;
    }

    /**
     * 按文件下载录像 sdk内部缓存控制自适应
     *
     * @return 下载句柄
     */
    public static NetSDKLib.LLong DownloadRecordFileWithSelfAdapt(NetSDKLib.LLong m_hLoginHandle,
                                                                  NET_IN_DOWNLOAD_BYFILE_SELFADAPT stuParamIn,
                                                                  NET_OUT_DOWNLOAD_BYFILE_SELFADAPT stuParamOut,
                                                                  int waitTime) {
        stuParamIn.write();
        stuParamOut.write();
        NetSDKLib.LLong m_hDownLoadHandle = NetSdk.CLIENT_DownloadByFileSelfAdapt(
                m_hLoginHandle, stuParamIn.getPointer(), stuParamOut.getPointer(), waitTime);
        if (m_hDownLoadHandle.longValue() == 0) {
            System.err.println("DownloadRecordFileWithSelfAdapt: " + ENUMERROR.getErrorCode());
        }
        return m_hDownLoadHandle;
    }

    /**
     * 按时间下载录像 sdk内部缓存控制自适应
     *
     * @return 下载句柄
     */
    public static NetSDKLib.LLong DownloadRecordAdaptiveDownloadByTime(NetSDKLib.LLong m_hLoginHandle,
                                                                       NET_IN_ADAPTIVE_DOWNLOAD_BY_TIME stuParamIn,
                                                                       NET_OUT_ADAPTIVE_DOWNLOAD_BY_TIME stuParamOut,
                                                                       int waitTime) {
        stuParamIn.write();
        stuParamOut.write();
        NetSDKLib.LLong m_hDownLoadHandle = NetSdk.CLIENT_AdaptiveDownloadByTime(
                m_hLoginHandle, stuParamIn.getPointer(), stuParamOut.getPointer(), waitTime);
        if (m_hDownLoadHandle.longValue() == 0) {
            System.err.println("DownloadRecordFileWithSelfAdapt: " + ENUMERROR.getErrorCode());
        }
        return m_hDownLoadHandle;
    }

    /**
     * 停止下载录像
     *
     * @param m_hDownLoadHandle 下载句柄
     */
    public static boolean StopDownLoadRecordFile(NetSDKLib.LLong m_hDownLoadHandle) {

        if (m_hDownLoadHandle.longValue() == 0) {
            System.err.println("DownLoadHandle invalid");
            return false;
        }
        if (!NetSdk.CLIENT_StopDownload(m_hDownLoadHandle)) {
            System.err.println("Stop DownLoad Failed:" + ENUMERROR.getErrorCode());
            return false;
        } else {
            System.out.println("Stop DownLoad Succeed.");
            m_hDownLoadHandle.setValue(0);
            return true;
        }
    }
}
