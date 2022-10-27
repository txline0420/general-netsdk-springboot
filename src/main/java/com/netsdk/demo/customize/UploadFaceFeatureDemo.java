package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_IN_DELIVER_USER_PICTURE;
import com.netsdk.lib.structure.NET_IN_GETFACEEIGEN_INFO;
import com.netsdk.lib.structure.NET_OUT_DELIVER_USER_PICTURE;
import com.netsdk.lib.structure.NET_OUT_GETFACEEIGEN_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 251589
 * @version V1.0
 * @Description:
 * @date 2020/12/9 19:27
 */
public class UploadFaceFeatureDemo {
    static NetSDKLib netSDK = NetSDKLib.NETSDK_INSTANCE;
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);     //登陆句柄
    private NetSDKLib.LLong m_hAttachHandle = new NetSDKLib.LLong(0); // 订阅句柄
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();


    public UploadFaceFeatureDemo(NetSDKLib.LLong loginHandle) {
        this.loginHandle = loginHandle;
    }

    public UploadFaceFeatureDemo() {

    }

    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    public class fDisConnectCB implements NetSDKLib.fDisConnect {
        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
        }
    }

    // 网络连接恢复，设备重连成功回调
    // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
    public class HaveReConnect implements NetSDKLib.fHaveReConnect {
        public void invoke(NetSDKLib.LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
        }
    }

    private fDisConnectCB m_DisConnectCB = new fDisConnectCB();
    private HaveReConnect haveReConnect = new HaveReConnect();

    public void EndTest() {
        System.out.println("End Test");
        if (loginHandle.longValue() != 0) {
            netSDK.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netSDK.CLIENT_Cleanup();
        System.exit(0);
    }

    public void InitTest() {
        //初始化SDK库
        netSDK.CLIENT_Init(m_DisConnectCB, null);

        // 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
        // 此操作为可选操作，但建议用户进行设置
        netSDK.CLIENT_SetAutoReconnect(haveReConnect, null);

        //设置登录超时时间和尝试次数，可选
        int waitTime = 3000; //登录请求响应超时时间设置为3S
        int tryTimes = 3;    //登录时尝试建立链接3次
        netSDK.CLIENT_SetConnectTime(waitTime, tryTimes);

        // 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
        netSDK.CLIENT_SetNetworkParam(netParam);

        // 打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();

        File path = new File(".");
        String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\" + System.currentTimeMillis() + ".log";

        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        boolean logOpen = netSDK.CLIENT_LogOpen(setLog);
        if (!logOpen) {
            System.err.println("Failed to open NetSDK log !!!");
        }

        // 设备登入
        int nSpecCap = 0;
        Pointer pCapParam = null;
        IntByReference nError = new IntByReference(0);
        loginHandle = netSDK.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
                m_strPassword, nSpecCap, pCapParam, deviceInfo, nError);

        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Port[%d]Success!\n", m_strIp, m_nPort);
        } else {
            System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%s]\n", m_strIp, m_nPort, ToolKits.getErrorCode() + "errorMsg: " + ENUMERROR.getErrorMessage());
            EndTest();
        }
    }

    public void getFaceInfo() {
        // 获取
        String userId = "123";  // 用户ID
        int emType = NetSDKLib.EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_GET;
        NetSDKLib.NET_IN_GET_FACE_INFO inGet = new NetSDKLib.NET_IN_GET_FACE_INFO();
        System.arraycopy(userId.getBytes(), 0, inGet.szUserID, 0, userId.getBytes().length);   // 用户ID

        NetSDKLib.NET_OUT_GET_FACE_INFO outGet = new NetSDKLib.NET_OUT_GET_FACE_INFO();
        outGet.nPhotoData = 1;    // 白光人脸照片数据个数 1
        outGet.nInPhotoDataLen[0] = 200 * 1024; // 用户申请的每张白光人脸照片大小 200KB
        int size = outGet.nInPhotoDataLen[0] * outGet.nPhotoData;
        outGet.pPhotoData[0] = new Memory(size);
        outGet.pPhotoData[0].clear(size);
        inGet.write();
        outGet.write();
        boolean bRet = netSDK.CLIENT_FaceInfoOpreate(loginHandle, emType, inGet.getPointer(), outGet.getPointer(), 5000);
        inGet.read();
        outGet.read();
        if (bRet) {
            System.out.println("FaceInfoOperate Get Succeed!");
            System.out.println("图片个数 : " + outGet.nPhotoData);
            for (int i = 0; i < outGet.nPhotoData; i++) {
                ToolKits.savePicture(outGet.pPhotoData[i], outGet.nOutPhotoDataLen[i], "photo_data_" + i + ".jpg");
            }
        } else {
            System.err.println("FaceInfoOperate Get Failed!" + netSDK.CLIENT_GetLastError() + "MSG:" + ENUMERROR.getErrorMessage());
        }
    }

    /**
     * 获取人脸特征值
     */
    public void getFaceFeature() {
        // 获取人脸特征值
        int emType = NetSDKLib.EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_GETFACEEIGEN;

        // 入参
        NET_IN_GETFACEEIGEN_INFO inParam = new NET_IN_GETFACEEIGEN_INFO();
        byte[] bytes = getImageData();


        Pointer bP=new Memory(bytes.length);
        bP.read(0, bytes, 0, bytes.length);


        inParam.pszPhotoData = bP;
        inParam.nPhotoDataLen = bytes.length;

        // 出参
        NET_OUT_GETFACEEIGEN_INFO outParam = new NET_OUT_GETFACEEIGEN_INFO();
        inParam.write();
        outParam.write();

        boolean bRet = netSDK.CLIENT_FaceInfoOpreate(loginHandle, emType, inParam.getPointer(), outParam.getPointer(), 3000);
        inParam.read();
        outParam.read();

        if (bRet) {
            ToolKits.GetPointerDataToStruct(outParam.getPointer(), 0, outParam);
            System.out.println("FaceInfoOpreate Succeed!" + outParam);
        } else {
            System.err.println("FaceInfoOpreate Failed!" + ToolKits.getErrorCode() + "  " + ENUMERROR.getErrorMessage());
        }
    }


    // 下发人脸数据
    public void setFaceFeature() {
        // 入参
        NET_IN_DELIVER_USER_PICTURE inParam = new NET_IN_DELIVER_USER_PICTURE();
        String userId = "251500";
        String citizenId = "123";
        inParam.szUserID = userId.getBytes();
        inParam.szCitizenID = citizenId.getBytes();

        // 出参
        NET_OUT_DELIVER_USER_PICTURE outParam = new NET_OUT_DELIVER_USER_PICTURE();

        boolean ret = netSDK.CLIENT_DeliverUserFacePicture(loginHandle, inParam, outParam, 3000);
        if (ret) {
            System.out.println("下发人脸数据成功！");
            ToolKits.GetPointerData(outParam.getPointer(), outParam);
            System.out.println(outParam);
        } else {
            System.err.println("下发人脸数据失败！" + netSDK.CLIENT_GetLastError() + "  msg: " + ENUMERROR.getErrorMessage());
        }
    }


    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    private byte[] getImageData() {
        File f = new File("d://0520_1.jpg");
        try {
            BufferedImage bi = ImageIO.read(f);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpg", baos);
            byte[] bytes = baos.toByteArray();

            return bytes;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "获取人脸特征值", "getFaceFeature"));
        menu.addItem(new CaseMenu.Item(this, "下发人脸特征值", "setFaceFeature"));
        menu.run();
    }

    ////////////////////////////////////////////////////////////////
    String m_strIp = "172.12.5.69";
    int m_nPort = 37777;
    String m_strUser = "admin";
    String m_strPassword = "admin123";

    ////////////////////////////////////////////////////////////////
    public static void main(String[] args) {
        UploadFaceFeatureDemo demo = new UploadFaceFeatureDemo();
        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }
}
