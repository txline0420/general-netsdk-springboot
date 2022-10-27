package com.netsdk.demo.customize;


import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_IN_RADAR_MANUAL_TRACK;
import com.netsdk.lib.structure.NET_OUT_RADAR_MANUAL_TRACK;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * @author 291189
 * @version 1.0
 * @description GIP211009014 手动选择轨迹目标让球机跟踪
 * @date 2021/10/22 9:37
 */
public class TheBallMillOrbitDemo extends Initialization {


/*   "SDIP": "192.168.1.108",
           "TrackID": 1,
           "Time": 30,
           "Action": 1*/
       //手动选择球机要跟踪的轨迹目标
    public void   radarManualTrack(){

        NET_IN_RADAR_MANUAL_TRACK  input=new NET_IN_RADAR_MANUAL_TRACK();

        String sdIp="192.168.1.108";
        input.szSDIP=sdIp.getBytes();
        input.nTime=30;
        input.nTrackID=1;
        input.nAction=1;

        Pointer pInput=new Memory(input.dwSize);

        ToolKits.SetStructDataToPointer(input, pInput, 0);

        NET_OUT_RADAR_MANUAL_TRACK output=new NET_OUT_RADAR_MANUAL_TRACK();

        Pointer pOutput=new Memory(output.dwSize);
        ToolKits.SetStructDataToPointer(output, pOutput, 0);

        boolean isSucess = netSdk.CLIENT_RadarManualTrack(loginHandle, pInput, pOutput, 5000);

        if(!isSucess){

        System.err.printf("CLIENT_RadarManualTrack  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
      }else {
        System.out.println("CLIENT_RadarManualTrack success");
    }
    }

    public static void main(String[] args) {
       InitTest("172.13.0.100",37777,"admin","admin123");
new TheBallMillOrbitDemo().radarManualTrack();
        LoginOut();

    }
}
