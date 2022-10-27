package com.netsdk.demo.customize.turkcell;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;


/**
 *
 * @author 47081
 * @version 1.0
 * @description GPS消息订阅的实现
 * @date 2020/6/12
 */
public class TurkcellGPSCallback implements NetSDKLib.fGPSRevEx{
    private static TurkcellGPSCallback INSTANCE;
    private TurkcellGPSCallback(){

    }
    public static TurkcellGPSCallback getInstance(){
        if(INSTANCE==null){
            INSTANCE=new TurkcellGPSCallback();
        }
        return INSTANCE;
    }

    /**
     *
     * @param lLoginID
     * @param GpsInfo
     *                 longitude;// 经度(单位是百万分之度,范围0-360度)
     *                 latidude;// 纬度(单位是百万分之度,范围0-180度)
     *                 height; // 高度(米)
     *                 angle; // 方向角(正北方向为原点,顺时针为正)
     *                 speed;// 速度(单位是海里,speed/1000*1.852公里/小时)
     *                 starCount;// 定位星数,无符号
     *        	       antennaState;// 天线状态(true 好,false 坏)
     *                 orientationState;// 定位状态(true 定位,false 不定位)
     * @param stAlarmInfo
     * @param dwUserData
     * @param reserved
     */
    @Override
    public void invoke(NetSDKLib.LLong lLoginID, NetSDKLib.GPS_Info.ByValue GpsInfo, NetSDKLib.ALARM_STATE_INFO.ByValue stAlarmInfo, Pointer dwUserData, Pointer reserved) {
        System.out.println(GpsInfo.revTime.toStringTime()+" Gps info: "+"is orientation:"+GpsInfo.orientationState+",longitude: "+GpsInfo.longitude/1000000.0 +"度, latitude: "+GpsInfo.latidude/1000000.0+"度, height: "+GpsInfo.height+"angle: "+GpsInfo.angle+",speed: "+GpsInfo.speed/1000.0*1.852+" km/h");
    }

}
