package com.netsdk.demo.customize.courseRecord;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import static com.netsdk.lib.NetSDKLib.NET_DEVSTATE_SOFTWARE;
import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 这里写一些特殊操作用的接口
 *
 * @author ： 47040
 * @since ： Created in 2020/9/30 10:45
 */
public class TestCourseRecordStatus {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    /**
     * 获取设备软件版本
     *
     * @param lLoginID 登录句柄
     */
    public void QueryDevDeviceVersionStateTest(NetSDKLib.LLong lLoginID) {
        NetSDKLib.NETDEV_VERSION_INFO info = new NetSDKLib.NETDEV_VERSION_INFO();
        info.write();
        boolean bRet = netsdk.CLIENT_QueryDevState(lLoginID, NET_DEVSTATE_SOFTWARE, info.getPointer(), info.size(), new IntByReference(0), 3000);
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
     * 查询设备在线状态
     *
     * @param lLoginID 登录句柄
     */
    public void QueryOnlineStateTest(NetSDKLib.LLong lLoginID) {

        Pointer p = new Memory(Integer.SIZE);
        p.clear(Integer.SIZE);
        boolean ret = netsdk.CLIENT_QueryDevState(lLoginID, NetSDKLib.NET_DEVSTATE_ONLINE, p, Integer.SIZE, new IntByReference(0), 3000);
        if (!ret) {
            System.err.println("查询设备在线状态失败, " + ToolKits.getErrorCode());
            return;
        }
        int[] buffer = new int[1];
        p.read(0, buffer, 0, 1);
        // 1 表示在线, 0 表示断线
        System.out.println(buffer[0] == 1 ? "设备在线" : "设备断线");
    }

    /**
     * 查询硬盘状态
     */
    public boolean QueryHardDiskStateTest(NetSDKLib.LLong m_hLoginHandle) {
        IntByReference intRetLen = new IntByReference();
        NetSDKLib.NET_DEV_HARDDISK_STATE diskInfo = new NetSDKLib.NET_DEV_HARDDISK_STATE();
        if (netsdk.CLIENT_QueryDevState(m_hLoginHandle,
                NetSDKLib.NET_DEVSTATE_DISK,
                diskInfo.getPointer(),
                diskInfo.size(),
                intRetLen,
                5000)) {
            diskInfo.read();

            String[] diskType = {"读写驱动器", "只读驱动器", "备份驱动器或媒体驱动器", "冗余驱动器", "快照驱动器"};
            String[] diskStatus = {"休眠", "活动", "故障"};
            String[] diskSignal = {"本地", "远程"};
            for (int i = 0; i < diskInfo.dwDiskNum; ++i) {
                System.out.printf("硬盘[%d] 硬盘号:%d 分区号:%d 容量:%dMB 剩余空间:%dMB 标识:%s 类型:%s 状态:%s \n",
                        i + 1, diskInfo.stDisks[i].bDiskNum, diskInfo.stDisks[i].bSubareaNum,
                        diskInfo.stDisks[i].dwVolume, diskInfo.stDisks[i].dwFreeSpace, diskSignal[diskInfo.stDisks[i].bSignal],
                        diskType[(diskInfo.stDisks[i].dwStatus & 0xF0) >> 4], diskStatus[diskInfo.stDisks[i].dwStatus & 0x0F]);
            }
        } else {
            System.err.println("Query Hard Disk State Failed!" + ToolKits.getErrorCode());
            return false;
        }
        return true;
    }

    /**
     * 获取录播主机所有配置的前端摄像头连接状态
     *
     * @param m_hLoginHandle 登录句柄
     * @param chanNum        设备总通道数
     */
    public boolean QueryCameraStateTest(NetSDKLib.LLong m_hLoginHandle, int chanNum) {

        NetSDKLib.NET_CAMERA_STATE_INFO[] arrCameraStatus = new NetSDKLib.NET_CAMERA_STATE_INFO[chanNum];
        for (int i = 0; i < arrCameraStatus.length; i++) {
            arrCameraStatus[i] = new NetSDKLib.NET_CAMERA_STATE_INFO();
        }

        // 入参
        NetSDKLib.NET_IN_GET_CAMERA_STATEINFO stIn = new NetSDKLib.NET_IN_GET_CAMERA_STATEINFO();
        stIn.bGetAllFlag = 1; // 全部

        // 出参
        NetSDKLib.NET_OUT_GET_CAMERA_STATEINFO stOut = new NetSDKLib.NET_OUT_GET_CAMERA_STATEINFO();
        stOut.nMaxNum = chanNum;
        stOut.pCameraStateInfo = new Memory(arrCameraStatus[0].size() * chanNum);
        stOut.pCameraStateInfo.clear(arrCameraStatus[0].size() * chanNum);
        ToolKits.SetStructArrToPointerData(arrCameraStatus, stOut.pCameraStateInfo);  // 将数组内存拷贝到Pointer

        stIn.write();
        stOut.write();

        boolean bRet = netsdk.CLIENT_QueryDevInfo(m_hLoginHandle, NetSDKLib.NET_QUERY_GET_CAMERA_STATE,
                stIn.getPointer(), stOut.getPointer(), null, 3000);
        if (bRet) {
            stOut.read();
            ToolKits.GetPointerDataToStructArr(stOut.pCameraStateInfo, arrCameraStatus);  // 将Pointer拷贝到数组内存
            final String[] connectionState = {"未知", "正在连接", "已连接", "未连接", "通道未配置,无信息", "通道有配置,但被禁用"};
            // 注意: 前6个通道无法配置前端摄像头，真实通道也是设备内部的通道
            // 另外sdk通道从0开始计数，设备却是从1开始的
            for (int i = 6; i < stOut.nValidNum; ++i) {
                System.out.printf("真实通道[%2d]: %s \n",
                        arrCameraStatus[i].nChannel + 1,  // 为了和网页端一致，这里 +1
                        connectionState[arrCameraStatus[i].emConnectionState]);
            }

        } else {
            System.err.println("Query Camera State Failed!" + ToolKits.getErrorCode());
        }

        return bRet;
    }
}
