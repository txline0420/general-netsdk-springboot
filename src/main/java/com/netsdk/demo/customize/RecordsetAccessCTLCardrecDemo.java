package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/8/20 14:19
 */
public class RecordsetAccessCTLCardrecDemo {

    public static void main(String[] args) {
        NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO mes=new NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO();
        System.out.println("dwSize:"+mes.dwSize);//3792
    }
}
