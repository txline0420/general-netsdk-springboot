package com.netsdk.demo.customize.courseRecord;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_COMPOSIT_CHANNEL_BIND_MODE;
import com.netsdk.lib.structure.NET_CFG_COURSE_RECORD_DEFAULT_CONFIG;
import com.sun.jna.ptr.IntByReference;

import static com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_COURSE_RECORD_DEFAULT_CONFIG;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/27 15:04
 */
public class TestCourseRecordConfig {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    /**
     * 修改默认模式
     * 这里的用例是：修改合成通道模式为多画面模式
     */
    public void SetSteamConfigTest(NetSDKLib.LLong m_login) {
        NET_CFG_COURSE_RECORD_DEFAULT_CONFIG steamConfig = new NET_CFG_COURSE_RECORD_DEFAULT_CONFIG();
        // 先获取原先的默认配置
        steamConfig.write();
        int emCfgOpType = NET_EM_CFG_COURSE_RECORD_DEFAULT_CONFIG;  // 配置类型的枚举
        boolean ret1 = netsdk.CLIENT_GetConfig(m_login, emCfgOpType, -1, steamConfig.getPointer(), steamConfig.size(), 3000, null);
        if (!ret1) {
            System.err.println("获取录播默认配置失败：" + ToolKits.getErrorCode());
            return;
        }
        steamConfig.read();
        System.out.println("获取录播默认配置成功!");

        steamConfig.nCompositChannelMode = 2;     // 修改 CompositeChannelMode 为 2 常态模式(多画面)

        steamConfig.write();
        boolean ret2 = netsdk.CLIENT_SetConfig(m_login, emCfgOpType, -1, steamConfig.getPointer(), steamConfig.size(), 3000, new IntByReference(0), null);
        if (!ret2) {
            System.err.println("设置录播默认配置失败：" + ToolKits.getErrorCode());
        }
        System.out.println("设置录播默认配置成功！");
    }

    /**
     * 获取默认模式
     */
    public void GetSteamConfigTest(NetSDKLib.LLong m_login) {
        NET_CFG_COURSE_RECORD_DEFAULT_CONFIG steamConfig = new NET_CFG_COURSE_RECORD_DEFAULT_CONFIG();
        // 先获取原先的默认配置
        steamConfig.write();
        int emCfgOpType = NET_EM_CFG_COURSE_RECORD_DEFAULT_CONFIG;  // 配置类型的枚举
        boolean ret1 = netsdk.CLIENT_GetConfig(m_login, emCfgOpType, -1, steamConfig.getPointer(), steamConfig.size(), 3000, null);
        if (!ret1) {
            System.err.println("获取录播默认配置失败：" + ToolKits.getErrorCode());
            return;
        }
        steamConfig.read();
        System.out.println("获取录播默认配置成功!");

        StringBuilder info = new StringBuilder().append("///--->录播默认配置如下:").append("\n")
                // 0: 无效, 1: 电影模式, 2: 常态模式, 3: 精品模式, 小于0: 自定义模式
                .append("nCompositChannelMode 组合通道模式: ").append(steamConfig.emCompositChannelBindMode).append("\n")
                .append("//--> nCanStartStreamNum 可拉流逻辑通道数: ").append(steamConfig.nCanStartStreamNum).append("\n");
        for (int i = 0; i < steamConfig.nCanStartStreamNum; i++) {
            info.append(String.format("逻辑通道【%2d】: ", steamConfig.emCanStartStream[i])).append("\n");
        }
        info.append("//--> nIsRecordNum 可录像逻辑通道数: ").append(steamConfig.nIsRecordNum).append("\n");
        for (int i = 0; i < steamConfig.nIsRecordNum; i++) {
            info.append(String.format("逻辑通道【%2d】: ", steamConfig.emIsRecord[i])).append("\n");
        }
        info.append("emCompositChannelBindMode 默认组合通道绑定模式: ").append(EM_COMPOSIT_CHANNEL_BIND_MODE.getNoteByValue(steamConfig.emCompositChannelBindMode)).append("\n");
        System.out.println(info.toString());
    }
}
