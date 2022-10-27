package com.netsdk.demo.customize.accessMeasure;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_CFG_ACCESSCONTROL_MEASURE_TEMP_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 260611
 * @since ： Created in 2021/10/11 11:18
 */
public class AccessMeasure {

    static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
    // 登陆句柄
    private static NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);
    // 智能订阅句柄
    private NetSDKLib.LLong attachHandle = new NetSDKLib.LLong(0);

    private static class DisconnectCallback implements NetSDKLib.fDisConnect {
        private static DisconnectCallback instance = new DisconnectCallback();

        private DisconnectCallback() {
        }

        public static DisconnectCallback getInstance() {
            return instance;
        }

        @Override
        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s:%d] Disconnect!\n", pchDVRIP, nDVRPort);
        }
    }

    private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
        private static HaveReconnectCallback instance = new HaveReconnectCallback();

        private HaveReconnectCallback() {
        }

        public static HaveReconnectCallback getInstance() {
            return instance;
        }

        @Override
        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s:%d] HaveReconnected!\n", pchDVRIP, nDVRPort);
        }
    }

    public void EndTest() {
        System.out.println("End Test");
        if (loginHandle.longValue() != 0) {
            netsdkApi.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netsdkApi.CLIENT_Cleanup();
        System.exit(0);
    }

    public void InitTest() {
        // 初始化SDK库
        netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);

        // 设置断线自动重练功能
        netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);

        // 向设备登入
        int nSpecCap = 0;
        IntByReference nError = new IntByReference(0);
        loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, username, password, nSpecCap, null, deviceInfo, nError);

        if (loginHandle.longValue() == 0) {
            System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port,
                    netsdkApi.CLIENT_GetLastError());
            EndTest();
            return;
        }

        System.out.printf("Login Device [%s:%d] Success. \n", address, port);
    }

    // 编码格式
    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    /******************************** 测试接口功能 ***************************************/

    /**
     * 获取下发门禁测温配置
     */
    public void GetandSetAccessControlMeasureTempInfo() {
        NET_CFG_ACCESSCONTROL_MEASURE_TEMP_INFO config = new NET_CFG_ACCESSCONTROL_MEASURE_TEMP_INFO();
        Pointer pointer = new Memory(config.size());
        ToolKits.SetStructDataToPointer(config, pointer, 0);
        /**配置获取**/
        boolean result = netsdkApi.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ACCESSCONTROL_MEASURE_TEMP, -1,
                pointer, config.size(), 5000, null);
        if (!result) {
            System.out.println("获取门禁测温配置失败:" + ENUMERROR.getErrorMessage());
        } else {
            System.out.println("获取门禁测温配置成功:" + ENUMERROR.getErrorMessage());
            ToolKits.GetPointerData(pointer, config);
            System.out.println("bEnable:" + config.bEnable);
            System.out.println("bOnlyTempMode:" + config.bOnlyTempMode);
            System.out.println("bDisplayTemp:" + config.bDisplayTemp);
            System.out.println("emMaskDetectMode:" + config.emMaskDetectMode);
            System.out.println("emMeasureType:" + config.emMeasureType);
            System.out.println("stuInfraredTempParam:\n" + config.stuInfraredTempParam.toString());
            System.out.println("stuThermalImageTempParam:\n" + config.stuThermalImageTempParam.toString());
            System.out.println("stuGuideModuleTempParam:\n" + config.stuGuideModuleTempParam.toString());
            System.out.println("stuWristTempParam:\n" + config.stuWristTempParam.toString());
//            if(config.emMeasureType == 1){
//                System.out.println("Mode:Infrared");
//                System.out.println("stuInfraredTempParam:\n" + config.stuInfraredTempParam.toString());
//            }else if(config.emMeasureType == 2){
//                System.out.println("Mode:Thermal image");
//                System.out.println("stuThermalImageTempParam:\n" + config.stuThermalImageTempParam.toString());
//            }else if(config.emMeasureType == 3){
//                System.out.println("Mode:Guide module");
//                System.out.println("stuGuideModuleTempParam:\n" + config.stuGuideModuleTempParam.toString());
//            }else if(config.emMeasureType == 4){
//                System.out.println("Mode:Wrist");
//                System.out.println("stuWristTempParam:\n" + config.stuWristTempParam.toString());
//            }else{
//                System.out.println("Mode:unknow or error");
//            }

            /**修改相关参数**/
            if(config.bEnable != 0) {
                config.bEnable = 0;
            }else{
                config.bEnable = 1;
            }
            if(config.bDisplayTemp != 0) {
                config.bDisplayTemp = 0;
            }else{
                config.bDisplayTemp = 1;
            }
            if(config.bOnlyTempMode != 0) {
                config.bOnlyTempMode = 0;
            }else{
                config.bOnlyTempMode = 1;
            }
            if(++config.emMaskDetectMode > 3){
                config.emMaskDetectMode = 0;
            }

            /**如需修改测温类型，将此处取消注释**/
//            if(++config.emMeasureType > 4){
//                config.emMeasureType = 1;
//            }

            if(config.emMeasureType == 1){
                config.stuInfraredTempParam.nMaxDistance++;
                config.stuInfraredTempParam.nRetentionTime++;
                config.stuInfraredTempParam.dbCorrectTemp += 0.1;
                config.stuInfraredTempParam.dbTempThreshold +=0.1;
                config.stuInfraredTempParam.dbValidTempLowerLimit +=0.1;
                if(config.stuInfraredTempParam.bDebugModelEnable != 0) {
                    config.stuInfraredTempParam.bDebugModelEnable = 0;
                }else{
                    config.stuInfraredTempParam.bDebugModelEnable = 1;
                }
                if(config.stuInfraredTempParam.bRectEnable != 0) {
                    config.stuInfraredTempParam.bRectEnable = 0;
                }else{
                    config.stuInfraredTempParam.bRectEnable = 1;
                }
            }else if(config.emMeasureType == 2){
                config.stuThermalImageTempParam.nFaceCompareThreshold++;
                config.stuThermalImageTempParam.nOverTempMaxDistance++;
                config.stuThermalImageTempParam.nRetentionTime++;
            }else if(config.emMeasureType == 3){
                if(config.stuGuideModuleTempParam.bRectEnable != 0) {
                    config.stuGuideModuleTempParam.bRectEnable = 0;
                }else{
                    config.stuGuideModuleTempParam.bRectEnable = 1;
                }
                config.stuGuideModuleTempParam.nMaxDistance++;
                config.stuGuideModuleTempParam.dbCorrectTemp+=0.1;
                config.stuGuideModuleTempParam.dbTempRandReplaceThreshold+=0.1;
                config.stuGuideModuleTempParam.dbTempThreshold+=0.1;
                config.stuGuideModuleTempParam.dbValidTempLowerLimit+=0.1;
                if(config.stuGuideModuleTempParam.bDebugModelEnable != 0) {
                    config.stuGuideModuleTempParam.bDebugModelEnable = 0;
                }else{
                    config.stuGuideModuleTempParam.bDebugModelEnable = 1;
                }
                if(++config.stuGuideModuleTempParam.emCalibrationMode > 4){
                    config.stuGuideModuleTempParam.emCalibrationMode = 0;
                }
                if(config.stuGuideModuleTempParam.bHeatDisplayEnbale != 0) {
                    config.stuGuideModuleTempParam.bHeatDisplayEnbale = 0;
                }else{
                    config.stuGuideModuleTempParam.bHeatDisplayEnbale = 1;
                }
            }else if(config.emMeasureType == 4){
                config.stuWristTempParam.dbCorrectTemp+=0.1;
                config.stuWristTempParam.dbTempThreshold+=0.1;
                config.stuWristTempParam.dbValidTempLowerLimit+=0.1;
                config.stuWristTempParam.nInvalidMeasureDistance++;
                config.stuWristTempParam.nMeasureTimeout++;
                config.stuWristTempParam.nValidMeasureDistance++;
            }
            config.write();
            /**配置下发**/
            boolean bRet = netsdkApi.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ACCESSCONTROL_MEASURE_TEMP, -1,
                    config.getPointer(), config.size(), 5000, new IntByReference(0), null);
            if(!bRet){
                System.out.println("下发门禁测温配置失败:" + ENUMERROR.getErrorMessage());
            }else{
                System.out.println("下发门禁测温配置成功");
            }
        }
    }
    /******************************** 测试控制台 ***************************************/

    // 配置登陆地址，端口，用户名，密码
    String address = "171.2.100.146"; // 172.24.1.229 172.24.31.180 //172.12.66.45
    int port = 37777;
    String username = "admin";
    String password = "admin123";

    public static void main(String[] args) {
        AccessMeasure demo = new AccessMeasure();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();

    }

    /**
     * 加载测试内容
     */
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "获取下发门禁测温配置", "GetandSetAccessControlMeasureTempInfo"));
        menu.run();
    }

    /******************************** 结束 ***************************************/
}
