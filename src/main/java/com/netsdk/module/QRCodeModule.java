package com.netsdk.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.NET_EM_2DCODE_TYPE;
import com.netsdk.lib.structure.NET_IN_SET_2DCODE;
import com.netsdk.lib.structure.NET_OUT_SET_2DCODE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.nio.charset.Charset;

/**
 * @author 47081
 * @version 1.0
 * @description 二维码信息模块, 二次封装类
 * @date 2020/9/14
 */
public class QRCodeModule {
    private final NetSDKLib netsdkApi;

    public QRCodeModule(NetSDKLib netsdkApi) {
        this.netsdkApi = netsdkApi;
    }

    public QRCodeModule() {
        this(NetSDKLib.NETSDK_INSTANCE);
    }

    /**
     * 下发二维码到设备
     *
     * @param loginHandler 登录句柄
     * @param qrCodeType   二维码类型
     * @param qrCode       二维码信息
     * @return 是否下发成功
     */
    public boolean sendQrCode(long loginHandler, NET_EM_2DCODE_TYPE qrCodeType, String qrCode) {
        NET_IN_SET_2DCODE code = new NET_IN_SET_2DCODE();
        code.em2DCodeType = qrCodeType.getType();
        System.arraycopy(qrCode.getBytes(Charset.forName(Utils.getPlatformEncode())), 0, code.sz2DCode, 0, qrCode.getBytes(Charset.forName(Utils.getPlatformEncode())).length);
        Pointer pointer = new Memory(code.size());
        ToolKits.SetStructDataToPointer(code, pointer, 0);
        return netsdkApi.CLIENT_Set2DCode(new NetSDKLib.LLong(loginHandler), pointer, new NET_OUT_SET_2DCODE().getPointer(), 5000);
    }

}
