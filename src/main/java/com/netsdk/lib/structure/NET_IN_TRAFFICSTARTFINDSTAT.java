package com.netsdk.lib.structure;


import com.netsdk.lib.NetSDKLib;

/** 
* @author 291189
* @description  接口(CLIENT_StartFindFluxStat)输入参数 
* @date 2022/05/07 09:59:47
*/
public class NET_IN_TRAFFICSTARTFINDSTAT extends NetSDKLib.SdkStructure {
/** 
此结构体大小
*/
public			int					dwSize;
/** 
开始时间 暂时精确到小时
*/
public NET_TIME stStartTime=new NET_TIME();
/** 
结束时间 暂时精确到小时
*/
public NET_TIME stEndTime=new NET_TIME();
/** 
等待接收数据的超时时间
*/
public			int					nWaittime;

public NET_IN_TRAFFICSTARTFINDSTAT(){
		this.dwSize=this.size();
}
}