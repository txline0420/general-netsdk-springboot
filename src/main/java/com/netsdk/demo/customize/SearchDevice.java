package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.demo.util.Testable;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;

import java.text.SimpleDateFormat;

/**
 * This demo only can be used in LAN.
 *
 * @author 29779
 */
public class SearchDevice implements Testable {

    static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;

    private static class SearchCallback implements NetSDKLib.fSearchDevicesCB {
        private static class SearchHolder {
            private static SearchCallback intance = new SearchCallback();
        }

        private SearchCallback() {
        }

        public static SearchCallback getInstance() {
            return SearchHolder.intance;
        }

        @Override
        public void invoke(Pointer pDevNetInfo, Pointer pUserData) {
            if (pDevNetInfo != null) {
                NetSDKLib.DEVICE_NET_INFO_EX deviceInfo = new NetSDKLib.DEVICE_NET_INFO_EX();
                ToolKits.GetPointerDataToStruct(pDevNetInfo, 0, deviceInfo);
                System.out.printf("%s:%d\r\n", new String(deviceInfo.szIP).trim(), deviceInfo.nPort);
            }
        }
    }

    private LLong searchHandle;

    /**
     * start search devices
     */
    public void startSearch() {
        searchHandle = netsdkApi.CLIENT_StartSearchDevices(SearchCallback.getInstance(), null, null);
    }

    /**
     * stop search devices
     */
    public void stopSearch() {
        if (searchHandle.longValue() != 0) {
            netsdkApi.CLIENT_StopSearchDevices(searchHandle);
            searchHandle.setValue(0);
        }
    }

    SearchDevicesCB cbSearchDevices = new SearchDevicesCB();

    private class SearchDevicesCB implements NetSDKLib.fSearchDevicesCB {
        @Override
        public void invoke(Pointer pDevNetInfo, Pointer pUserData) {
            NetSDKLib.DEVICE_NET_INFO_EX deviceInfo = new NetSDKLib.DEVICE_NET_INFO_EX();
            ToolKits.GetPointerData(pDevNetInfo, deviceInfo);

            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = simpleDate.format(new java.util.Date());
            String dateString = date.replaceAll("-", "").replaceAll(":", "").replaceAll(" ", "");

            if (deviceInfo.iIPVersion == 4) {  ///过滤IPV4;   如果等于6，输出IPV6;   不过滤IPV4/IPV6都输出
                System.out.println("dateTime:" + dateString + "\n" +
                        "deviceId:" + new String(deviceInfo.szSerialNo).trim() + "\n" +
                        "IP:" + new String(deviceInfo.szIP).trim() + "\n" +
                        "mask:" + new String(deviceInfo.szSubmask).trim() + "\n" +
                        "gateWay:" + new String(deviceInfo.szGateway).trim() + "\n" +
                        "deviceType:" + new String(deviceInfo.szDeviceType).trim() + "\n");
            }
        }
    }
    
    // 通过ip 搜索设备
    public void SearchDevicesByIPs() {
        new Thread(() -> {
            NetSDKLib.DEVICE_IP_SEARCH_INFO stuIn = new NetSDKLib.DEVICE_IP_SEARCH_INFO();
            stuIn.nIpNum = 255;
            for (int i = 1; i <= 255; i++) {
                byte[] ip = ("172.23.12." + i).getBytes();
                System.arraycopy(ip, 0, stuIn.szIPArr[i - 1].szIP, 0, ip.length);
            }
            stuIn.write();
            boolean ret = netsdkApi.CLIENT_SearchDevicesByIPs(stuIn.getPointer(), cbSearchDevices, null, null, 5000);
            if(!ret){
                System.err.println("CLIENT_SearchDevicesByIPs failed: "+ToolKits.getErrorCode());
                return;
            }
            System.out.println("CLIENT_SearchDevicesByIPs succeed.");
        }).start();
    }

    @Override
    public void initTest() {
        /**
         * initialization, only be called once.
         */
        netsdkApi.CLIENT_Init(null, null);
    }

    @Override
    public void runTest() {
        System.out.println("run test");

        CaseMenu menu = new CaseMenu();

        menu.addItem(new CaseMenu.Item(this, "Start Search Devices", "startSearch"));
        menu.addItem(new CaseMenu.Item(this, "Stop Search Devices", "stopSearch"));
        menu.addItem(new CaseMenu.Item(this, "SearchDevicesByIPs", "SearchDevicesByIPs"));

        menu.run();
    }

    @Override
    public void endTest() {
        /**
         * clean up resources in SDK
         */
        netsdkApi.CLIENT_Cleanup();
    }

    public static void main(String[] args) {
        SearchDevice demo = new SearchDevice();
        demo.initTest();
        demo.runTest();
        demo.endTest();
    }
}
