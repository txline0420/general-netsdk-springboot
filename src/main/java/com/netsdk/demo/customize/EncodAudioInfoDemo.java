package com.netsdk.demo.customize;


import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_ENCODE_AUDIO_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.ptr.IntByReference;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/8/4 10:36
 */
public class EncodAudioInfoDemo extends Initialization {

   static int nChannelID = 0; // 通道号

    //设置前先查询
    public static void set_clientAudioInfo(){

        NET_ENCODE_AUDIO_INFO msg=new NET_ENCODE_AUDIO_INFO();
        msg.emFormatType=1;

        int type= NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ENCODE_AUDIO_INFO;

        // 获取
        msg.write();

        boolean isSuccess= netSdk.CLIENT_GetConfig(loginHandle, type, nChannelID, msg.getPointer(), msg.size(), 6000, null);

        if(isSuccess){
            msg.read();
            System.out.println("emFormatType:"+msg.emFormatType+"\n nDepth:"+msg.nDepth+"\n nFrequency:"+msg.nFrequency
            +"\n nMode:"+msg.nMode+"\n nFrameType:"+msg.nFrameType+"\n nPacketPeriod:"+msg.nPacketPeriod);

            msg.nMode=1;
            msg.emFormatType=1;
            msg.nFrameType=1;
            msg.nDepth=24;
            msg.nFrequency=9000;
            msg.nPacketPeriod=10;
            /*音频采样频率：8K ~ 192K
            int							nDepth;						// 音频采样深度：8,16,24
            int							nPacketPeriod;				// 音频打包周期, [10, 250],ms*/

            msg.write();

             isSuccess= netSdk.CLIENT_SetConfig(loginHandle, type, nChannelID, msg.getPointer(), msg.size(), 6000, new IntByReference(0),null);

            if(isSuccess){

                System.out.println("setConfig isSuccess");

                msg.read();

                System.out.println("emFormatType:"+msg.emFormatType+"\n nDepth:"+msg.nDepth+"\n nFrequency:"+msg.nFrequency
                        +"\n nMode:"+msg.nMode+"\n nFrameType:"+msg.nFrameType+"\n nPacketPeriod:"+msg.nPacketPeriod);

            }else {

                System.err.println(" { get error code: ( 0x80000000|" + (netSdk.CLIENT_GetLastError() & 0x7fffffff) + " ). 参考  NetSDKLib.java }");

            }

        }else {

                System.err.println(" { set error code: ( 0x80000000|" + (netSdk.CLIENT_GetLastError() & 0x7fffffff) + " ). 参考  NetSDKLib.java }");

        }

    }



    public static void main(String[] args) {


        Initialization.InitTest("172.23.12.231", 37777, "admin", "admin123");
        set_clientAudioInfo();
       // set_clientAudioInfo(lLong);

        Initialization.LoginOut();
    }

}
