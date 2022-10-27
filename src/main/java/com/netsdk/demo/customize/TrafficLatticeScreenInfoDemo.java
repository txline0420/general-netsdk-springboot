package com.netsdk.demo.customize;


import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * @author 291189
 * @version 1.0
 * @description  点阵屏显示信息配置
 * @date 2021/8/9 14:30
 */
public class TrafficLatticeScreenInfoDemo extends Initialization {

    static int nChannelID=0;
    public static void TrafficLatticeScreen( ) {
        //获取 点阵屏显示信息配置, 对应结构体 NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO
        int emCfgOpType= NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TRAFFIC_LATTICE_SCREEN;
        //入参
        NetSDKLib.NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO msg=new NetSDKLib.NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO();
      //  msg.emShowType=-1;

        int dwOutBufferSize=msg.size();

        Pointer szOutBuffer =new Memory(dwOutBufferSize);

        ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);

        boolean ret=netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID, szOutBuffer, dwOutBufferSize, 3000, null);

        if(!ret) {
            System.err.printf("TrafficLatticeScreen getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
            return;
        }

        ToolKits.GetPointerData(szOutBuffer, msg);

        // 资源文件播放列表,支持视频文件和图片文件播放,按照数组顺序循环播放
        byte[] szPlayList = msg.szPlayList;
        //资料文件个数
        int nPlayListNum = msg.nPlayListNum;
        System.out.println("资料文件个数:"+nPlayListNum);
        int length=64;
       //重新下发的列表
       byte[] szPlaynew=new byte[10*64];


        for(int i=0;i<nPlayListNum;i++){
            String s
                    = new String(szPlayList, length * i, length);

         if(i<nPlayListNum){//根据需求重置列表下发
             System.out.println(s);
             byte[] bytes = s.getBytes();
             //重新下发列表赋值
             System.arraycopy (bytes,0,szPlaynew,i*64,bytes.length);
         }



        }
        //将off的文件变成on状态，最后追加，将文件名用64字节的数组转换，
            byte[] bytes =new byte[64];
            byte[] byte1= "6.jpg".getBytes();
            System.arraycopy (byte1,0,bytes,0,byte1.length);
            System.out.println("leng:"+bytes.length);
            System.arraycopy (bytes,0,szPlaynew,nPlayListNum*64,bytes.length);


        //重置之后的播放列表
        msg.szPlayList=szPlaynew;
        msg.nPlayListNum=nPlayListNum+1;//数量和下发设置的on文件夹数量相同。
        //下发
        //NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO szInBuffer=new NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO();
        /*msg.nStatusChangeTime=40;
        msg.stuNormal.nContentsNum=1;
        msg.stuNormal.stuContents[0].emContents=1;*/
        IntByReference restart = new IntByReference(0);
        int dwInBufferSize=msg.size();
        Pointer szInBuffer =new Memory(dwInBufferSize);
        ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
        boolean result=netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID, szInBuffer, dwInBufferSize, 3000, restart, null);

        if(result) {
            System.out.println("CLIENT_SetConfig success");
        }else {
            System.err.println("CLIENT_SetConfig field");
        }
    }



    public static void main(String[] args) {
       InitTest("172.24.0.108", 37777, "admin", "admin123");

        TrafficLatticeScreen();


       LoginOut();
    }


}
