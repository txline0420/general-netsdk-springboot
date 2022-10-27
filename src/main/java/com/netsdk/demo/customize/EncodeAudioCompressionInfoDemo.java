package com.netsdk.demo.customize;

import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_ENCODE_AUDIO_COMPRESSION_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.ptr.IntByReference;
/**
 * @author 291189
 * @version 1.0
 * @description 万里目+设备SDK+想要调用sdk设置摄像头音频编码和使能
 * @date 2021/8/4 14:27
 */
public class EncodeAudioCompressionInfoDemo extends Initialization {

    static int nChannelID = 0; // 通道号

    public static void set_clientAudioCompressionInf(){

        NET_ENCODE_AUDIO_COMPRESSION_INFO msg=new NET_ENCODE_AUDIO_COMPRESSION_INFO();
        msg.emFormatType=1;

        int type= NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ENCODE_AUDIO_COMPRESSION;


        // 获取
        msg.write();

        boolean isSuccess= netSdk.CLIENT_GetConfig(loginHandle, type, nChannelID, msg.getPointer(), msg.size(), 6000, null);
        if(isSuccess){
            msg.read();
            System.out.println("bAudioEnable:"+msg.bAudioEnable+"\n emFormatType:"+msg.emFormatType +
                    "\n emCompression:"+msg.emCompression);


            msg.emFormatType=1;
            msg.bAudioEnable=1;
            msg.emCompression=1;


            msg.write();

            isSuccess= netSdk.CLIENT_SetConfig(loginHandle, type, nChannelID, msg.getPointer(), msg.size(), 6000, new IntByReference(0),null);

            if(isSuccess){

                System.out.println("setConfig isSuccess");

                msg.read();

                System.out.println("bAudioEnable:"+msg.bAudioEnable+"\n emFormatType:"+msg.emFormatType +
                        "\n emCompression:"+msg.emCompression);

            }else {

                System.err.println(" {set  error code: ( 0x80000000|" + (netSdk.CLIENT_GetLastError() & 0x7fffffff) + " ). 参考  NetSDKLib.java }");

            }

        }else {

            System.err.println(" { get error code: ( 0x80000000|" + (netSdk.CLIENT_GetLastError() & 0x7fffffff) + " ). 参考  NetSDKLib.java }");

        }
    }

    public static void main(String[] args) {
        Initialization.InitTest("172.23.12.231", 37777, "admin", "admin123");
        set_clientAudioCompressionInf();

        Initialization.LoginOut();
    }
}
