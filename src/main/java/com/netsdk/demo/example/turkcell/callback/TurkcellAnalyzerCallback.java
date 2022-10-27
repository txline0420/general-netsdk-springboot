package com.netsdk.demo.example.turkcell.callback;

import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;

import java.io.File;
import java.nio.charset.Charset;

import static com.netsdk.lib.NetSDKLib.*;


/**
 * @author 47081
 * @version 1.0
 * @description 智能事件分析回调
 * @date 2020/6/12
 */
public class TurkcellAnalyzerCallback implements fAnalyzerDataCallBack {
    private String imagePath="D:\\trafficPath";
    private static TurkcellAnalyzerCallback INSTANCE;
    private TurkcellAnalyzerCallback(){
        File file=new File(imagePath);
        if(!file.exists()){
            file.mkdirs();
        }
    }
    public static TurkcellAnalyzerCallback getInstance(){
        if(INSTANCE==null){
            INSTANCE=new TurkcellAnalyzerCallback();
        }
        return INSTANCE;
    }
    @Override
    public int invoke(LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved) {
        if(lAnalyzerHandle.longValue()==-1){
            return 0;
        }
        String date=null;
        String imageName;
        String savePath;
        if(dwAlarmType== EVENT_IVS_TRAFFICJUNCTION){
            //路口事件，抓拍车牌
            DEV_EVENT_TRAFFICJUNCTION_INFO trafficJunction=new DEV_EVENT_TRAFFICJUNCTION_INFO();
            //获取车牌信息
            ToolKits.GetPointerDataToStruct(pAlarmInfo,0,trafficJunction);
            date=trafficJunction.UTC.toStringTime();
            DEV_EVENT_TRAFFIC_TRAFFICCAR_INFO trafficCar=trafficJunction.stTrafficCar;
            System.out.println(date+" traffic number:"+new String(trafficCar.szPlateNumber,Charset.forName("GBK"))+",plate color:"+new String(trafficCar.szPlateColor,Charset.forName("GBK")));
            System.out.println("emVehicleTypeByFunc = " + trafficJunction.stTrafficCar.emVehicleTypeByFunc);
            System.out.println("nSunBrand = " + trafficJunction.stTrafficCar.nSunBrand);
            System.out.println("nBrandYear = " + trafficJunction.stTrafficCar.nBrandYear);
                    //save the picture
            imageName="trafficJunction_"+trafficJunction.UTC.toStringTitle()+".jpg";
            savePath=imagePath+"/"+imageName;

            ToolKits.savePicture(pBuffer,dwBufSize,savePath);
            System.out.println("save picture to "+savePath);
        }else if(dwAlarmType==EVENT_IVS_TRAFFIC_DRIVER_SMOKING){
            //驾驶员抽烟事件
            DEV_EVENT_TRAFFIC_DRIVER_SMOKING driverSmoking=new DEV_EVENT_TRAFFIC_DRIVER_SMOKING();
            ToolKits.GetPointerData(pAlarmInfo,driverSmoking);
            //事件时间
            date=driverSmoking.UTC.toStringTime();
            System.out.println(date+" event type: TRAFFIC_DRIVER_SMOKING");
            //save picture
            imageName="driverSmoking_"+driverSmoking.UTC.toStringTitle()+".jpg";
            savePath=imagePath+"/"+imageName;

            ToolKits.savePicture(pBuffer,dwBufSize,savePath);
            System.out.println("save picture to "+savePath);


        }else if(dwAlarmType==EVENT_IVS_TRAFFIC_DRIVER_CALLING){
            //驾驶员打电话事件
            DEV_EVENT_TRAFFIC_DRIVER_CALLING driverCalling=new DEV_EVENT_TRAFFIC_DRIVER_CALLING();
            ToolKits.GetPointerData(pAlarmInfo,driverCalling);
            //事件时间
            date=driverCalling.UTC.toStringTime();
            System.out.println(date+" event type: TRAFFIC_DRIVER_CALLING");
            //save picture
            imageName="driverCalling_"+driverCalling.UTC.toStringTitle()+".jpg";
            savePath=imagePath+"/"+imageName;

            ToolKits.savePicture(pBuffer,dwBufSize,savePath);
            System.out.println("save picture to "+savePath);

        }else if(dwAlarmType==EVENT_IVS_TRAFFIC_TIREDPHYSIOLOGICAL){
            //驾驶员疲劳驾驶事件
            DEV_EVENT_TIREDPHYSIOLOGICAL_INFO tiredPhysiological=new DEV_EVENT_TIREDPHYSIOLOGICAL_INFO();
            ToolKits.GetPointerDataToStruct(pAlarmInfo,0,tiredPhysiological);
            //事件时间
            date=tiredPhysiological.UTC.toStringTime();
            System.out.println(date+" event type: TIREDPHYSIOLOGICAL");
            //save picture
            imageName="tiredPhysiological_"+tiredPhysiological.UTC.toStringTitle()+".jpg";
            savePath=imagePath+"/"+imageName;

            ToolKits.savePicture(pBuffer,dwBufSize,savePath);
            System.out.println("save picture to "+savePath);

        }else if(dwAlarmType==EVENT_IVS_TRAFFIC_TIREDLOWERHEAD){
            //开车低头事件
            DEV_EVENT_TIREDLOWERHEAD_INFO tiredLowerHead=new DEV_EVENT_TIREDLOWERHEAD_INFO();
            ToolKits.GetPointerDataToStruct(pAlarmInfo,0,tiredLowerHead);
            //事件时间
            date=tiredLowerHead.UTC.toStringTime();
            System.out.println(date+" event type: TiredLowerHead");
            //save picture
            imageName="tiredLowerHead_"+tiredLowerHead.UTC.toStringTitle()+".jpg";
            savePath=imagePath+"/"+imageName;

            ToolKits.savePicture(pBuffer,dwBufSize,savePath);
            System.out.println("save picture to "+savePath);

        }else if(dwAlarmType==EVENT_IVS_TRAFFIC_DRIVERLOOKAROUND){
            //开车左顾右盼事件
            DEV_EVENT_DRIVERLOOKAROUND_INFO driverLookAround=new DEV_EVENT_DRIVERLOOKAROUND_INFO();
            ToolKits.GetPointerDataToStruct(pAlarmInfo,0,driverLookAround);
            date=driverLookAround.UTC.toStringTime();
            System.out.println(date+" event type: DriverLookAround,channel");
            //save picture
            imageName="driverLookAround_"+driverLookAround.UTC.toStringTitle()+".jpg";
            savePath=imagePath+"/"+imageName;

            ToolKits.savePicture(pBuffer,dwBufSize,savePath);
            System.out.println("save picture to "+savePath);

        }else if(dwAlarmType==EVENT_IVS_TRAFFIC_DRIVERYAWN){
            //打哈欠事件
            DEV_EVENT_DRIVERYAWN_INFO driverYawn=new DEV_EVENT_DRIVERYAWN_INFO();
            ToolKits.GetPointerDataToStruct(pAlarmInfo,0,driverYawn);
            date=driverYawn.UTC.toStringTime();
            System.out.println(date+" event type: DriverYawn,channelID: "+driverYawn.nChannelID);
            //save picture
            imageName="driverYawn_"+driverYawn.UTC.toStringTitle()+".jpg";
            savePath=imagePath+"/"+imageName;

            ToolKits.savePicture(pBuffer,dwBufSize,savePath);
            System.out.println("save picture to "+savePath);

        }else{
            System.out.println("other event");
        }
        return 0;
    }
}
