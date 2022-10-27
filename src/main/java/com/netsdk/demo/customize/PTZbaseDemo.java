package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

/**
 * @author 291189
 * @version 1.0
 * @description  ERR220415122 创新谷/嵊泗海关项目/旗舰哈勃/DH-PSDW82449M-A270-D840L/SDK/接口定制
 * @date 2022/5/18 9:51
 */
public class PTZbaseDemo extends Initialization {
    private static String ipAddr = "172.32.0.58";
    private static int port = 37777;
    private static String user = "admin";
    private static String password = "admin123";

    int channel=0;

//获取镜头当前倍率下水平视场角参数
    public void QueryDevInfoHFOV(){

        int nQueryType = NetSDKLib.NET_QUERY_PTZBASE_GET_HFOV_VALUE;

        // 入参
        NET_IN_PTZBASE_GET_HFOV_VALUE pIn = new NET_IN_PTZBASE_GET_HFOV_VALUE();

        pIn.fZoom=0.5f;
        pIn.nChannel=channel;
        Pointer pInBuf = new Memory(pIn.size());
        ToolKits.SetStructDataToPointer(pIn, pInBuf, 0);
        // 出参
        NET_OUT_PTZBASE_GET_HFOV_VALUE pOut = new NET_OUT_PTZBASE_GET_HFOV_VALUE();

        Pointer pOutBuf = new Memory(pOut.size());
        ToolKits.SetStructDataToPointer(pOut, pOutBuf, 0);
        boolean ret = netSdk.CLIENT_QueryDevInfo(loginHandle, nQueryType, pInBuf, pOutBuf, null, 3000);

        if (!ret) {
            System.err.printf("QueryDevInfoHFOV Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }

        ToolKits.GetPointerDataToStruct(pOutBuf, 0, pOut);

        System.out.println("nMaxValue:"+pOut.nMaxValue);
        System.out.println("nMinValue:"+pOut.nMinValue);
        System.out.println("nValue:"+pOut.nValue);
    }

    //获取镜头当前倍率下垂直视场角参数
    public void QueryDevInfoVFOV(){

        int nQueryType = NetSDKLib.NET_QUERY_PTZBASE_GET_VFOV_VALUE;

        // 入参
        NET_IN_PTZBASE_GET_VFOV_VALUE pIn = new NET_IN_PTZBASE_GET_VFOV_VALUE();

        pIn.fZoom=0.5f;
        pIn.nChannel=channel;
        Pointer pInBuf = new Memory(pIn.size());
        ToolKits.SetStructDataToPointer(pIn, pInBuf, 0);
        // 出参
        NET_OUT_PTZBASE_GET_VFOV_VALUE pOut = new NET_OUT_PTZBASE_GET_VFOV_VALUE();

        Pointer pOutBuf = new Memory(pOut.size());
        ToolKits.SetStructDataToPointer(pOut, pOutBuf, 0);
        boolean ret = netSdk.CLIENT_QueryDevInfo(loginHandle, nQueryType, pInBuf, pOutBuf, null, 3000);

        if (!ret) {
            System.err.printf("QueryDevInfoVFOV Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }
        ToolKits.GetPointerDataToStruct(pOutBuf, 0, pOut);

        System.out.println("nMaxValue:"+pOut.nMaxValue);
        System.out.println("nMinValue:"+pOut.nMinValue);
        System.out.println("nValue:"+pOut.nValue);
    }


    //  public static final int NET_QUERY_PTZBASE_GET_CENTER_GPS			= 0x3a;	 // 获取中心位置GPS信息，pInBuf = NET_IN_PTZBASE_GET_CENTER_GPS*,pOutBuf = NET_OUT_PTZBASE_GET_CENTER_GPS*
    //获取中心位置GPS信息
    public void QueryDevInfoCenterGPS(){

        int nQueryType = NetSDKLib.NET_QUERY_PTZBASE_GET_CENTER_GPS;

        // 入参
        NET_IN_PTZBASE_GET_CENTER_GPS pIn = new NET_IN_PTZBASE_GET_CENTER_GPS();
//计算GPS信息标志位，为1 时使用dPosition中的位置信息来进行计算，为0 时使用当前云台PT位置信息计算GPS
        pIn.bPosEnable=1;
        pIn.nChannel=channel;
        double[] dPosition = pIn.dPosition;

        /**
         云台方向信息，第一个元素为水平角度，第二个元素为垂直角度
         */
            dPosition[0]=600;
            dPosition[1]=319;

        Pointer pInBuf = new Memory(pIn.size());
        ToolKits.SetStructDataToPointer(pIn, pInBuf, 0);
        // 出参
        NET_OUT_PTZBASE_GET_CENTER_GPS pOut = new NET_OUT_PTZBASE_GET_CENTER_GPS();

        Pointer pOutBuf = new Memory(pOut.size());
        ToolKits.SetStructDataToPointer(pOut, pOutBuf, 0);
        boolean ret = netSdk.CLIENT_QueryDevInfo(loginHandle, nQueryType, pInBuf, pOutBuf, null, 3000);

        if (!ret) {
            System.err.printf("QueryDevInfoCenterGPS Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }
        ToolKits.GetPointerDataToStruct(pOutBuf, 0, pOut);
        System.out.println("dLongitude:"+pOut.dLongitude);
        System.out.println("dLatitude:"+pOut.dLatitude);

    }



    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this , "QueryDevInfoHFOV" , "QueryDevInfoHFOV")));
        menu.addItem((new CaseMenu.Item(this , "QueryDevInfoVFOV" , "QueryDevInfoVFOV")));
        menu.addItem((new CaseMenu.Item(this , "QueryDevInfoCenterGPS" , "QueryDevInfoCenterGPS")));
        menu.run();
    }

    public static void main(String[] args){
        InitTest(ipAddr, port, user, password);
        PTZbaseDemo scd=new PTZbaseDemo();
        scd.RunTest();
        LoginOut();
    }

}
