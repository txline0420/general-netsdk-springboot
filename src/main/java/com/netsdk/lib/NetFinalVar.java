package com.netsdk.lib;
/**
 * @author 251823
 * @description 存储设备状态
 * @date 2022/05/30
 */
public class NetFinalVar {
	// 存储设备状态
	public static final int NET_STORAGE_DEV_OFFLINE                 = 0;                   // 物理硬盘脱机状态
	public static final int NET_STORAGE_DEV_RUNNING                 = 1;                   // 物理硬盘运行状态
	public static final int NET_STORAGE_DEV_ACTIVE                  = 2;                   // RAID活动
	public static final int NET_STORAGE_DEV_SYNC                    = 3;                   // RAID同步
	public static final int NET_STORAGE_DEV_SPARE                   = 4;                   // RAID热备(局部)
	public static final int NET_STORAGE_DEV_FAULTY                  = 5;                   // RAID失效
	public static final int NET_STORAGE_DEV_REBUILDING              = 6;                   // RAID重建
	public static final int NET_STORAGE_DEV_REMOVED                 = 7;                   // RAID移除
	public static final int NET_STORAGE_DEV_WRITE_ERROR             = 8;                   // RAID写错误
	public static final int NET_STORAGE_DEV_WANT_REPLACEMENT        = 9;                   // RAID需要被替换
	public static final int NET_STORAGE_DEV_REPLACEMENT             = 10;                  // RAID是替代设备
	public static final int NET_STORAGE_DEV_GLOBAL_SPARE            = 11;                  // 全局热备
	public static final int NET_STORAGE_DEV_ERROR                   = 12;                  // 错误, 部分分区可用
	public static final int NET_STORAGE_DEV_RAIDSUB                 = 13;                  // 该盘目前是单盘, 原先是块Raid子盘, 有可能在重启后自动加入Raid
	public static final int NET_STORAGE_DEV_FATAL                   = 14;                  // 严重错误,全部分区坏(DVR新增错误类型)
	public static final int NET_STORAGE_DEV_SNAPSHOT_PARENT         = 15;                  // 快照母盘
	public static final int NET_STORAGE_DEV_SNAPSHOT_CHILD          = 16;                  // 快照子盘
	public static final int NET_STORAGE_DEV_VOLUMECLONE_PARENT      = 17;                  // 卷克隆母盘
	public static final int NET_STORAGE_DEV_VOLUMECLONE_CHILD       = 18;                  // 卷克隆子盘
}
