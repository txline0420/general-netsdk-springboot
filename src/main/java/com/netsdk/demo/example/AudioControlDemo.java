package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.Enum.EM_REASON_TYPE;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.SDK_REMOTE_FILE_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.structure.NET_CFG_CAP_SPEAK;
import com.netsdk.lib.structure.NET_IN_PRE_UPLOAD_REMOTE_FILE;
import com.netsdk.lib.structure.NET_LOOPPLAYBACK_AUDIOALARM_INFO;
import com.netsdk.lib.structure.NET_OUT_PRE_UPLOAD_REMOTE_FILE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.netsdk.lib.NetSDKLib.CtrlType.CTRLTYPE_CTRL_START_PLAYAUDIO;
import static com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_LOOPPLAYBACK_AUDIOALARM;

/**
 * @author 47081
 * @version 1.0
 * @description IPC声光警戒相机音频操作demo，
 *              包括设备音频能力集的获取，音频文件预上传，上传，获取音频文件列表，播放音频，删除音频
 *              2021/12/7 新增音频循环播放报警配置
 * @date 2020/6/5
 */
public class AudioControlDemo {
    private final String ip				= "172.29.2.145";
    private final int    port 			= 37777;
    private final String user 			= "admin";
    private final String password 		= "admin123";
    private final String srcFile        = "D:\\alarm1.wav"; //音频的本地路径

    private List<String> audioPaths;
    private List<String> destPaths;
    private List<String> audios;
    public AudioControlDemo(){
        audioPaths=new ArrayList<>();
        destPaths=new ArrayList<>();
        audios=new ArrayList<>();
    }
    public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    public static final NetSDKLib configSdk=NetSDKLib.CONFIG_INSTANCE;
    /**
     * 登录句柄
     */
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    /**
     *设备信息扩展
     */
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    public void initTest(){
        // 初始化SDK库
        netSdk.CLIENT_Init(DefaultDisconnectCallback.getINSTANCE(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(DefaultHaveReconnectCallBack.getINSTANCE(), null);

        //打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)){
            System.err.println("Open SDK Log Failed!!!");
        }
        login();
    }

    public void login(){
        // 登陆设备
        // TCP登入
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(ip, port, user,
                password ,nSpecCap, null, deviceInfo, nError);
        if(loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", ip);
        }else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", ip, ToolKits.getErrorCode());
            loginOut();
        }
    }

    public void loginOut(){
        System.out.println("End Test");
        if( loginHandle.longValue() != 0)
        {
            netSdk.CLIENT_Logout(loginHandle);
        }
        System.out.println("See You...");

        netSdk.CLIENT_Cleanup();
        System.exit(0);
    }

    /**
     * 获取设备音频能力:查看可上传路径，支持的音频格式
     */
    public void getDeviceAudioCap(){
        String audioCap=NetSDKLib.CFG_CAP_CMD_SPEAK;
        NET_CFG_CAP_SPEAK speak=new NET_CFG_CAP_SPEAK();
        if(getDevConfig(loginHandle,audioCap,speak,-1)){
            destPaths.clear();
            System.out.println("播放路径个数: "+speak.nAudioPlayPathNum);
            for (int i = 0; i < speak.nAudioPlayPathNum; i++) {
                System.out.println("is support upload: "+speak.stuAudioPlayPath[i].bSupportUpload+",path: "+new String(speak.stuAudioPlayPath[i].szPath)+",max file upload number:"+speak.stuAudioPlayPath[i].nMaxFileUploadNum+",max file upload size(kb): "+speak.stuAudioPlayPath[i].nMaxUploadFileSize/1024.0);
                if(speak.stuAudioPlayPath[i].bSupportUpload){
                    destPaths.add(new String(speak.stuAudioPlayPath[i].szPath,Charset.forName("GBK")).trim());
                }

                if(!"".equals(new String(speak.stuAudioPlayPath[i].szPath).trim())){
                    audioPaths.add(new String(speak.stuAudioPlayPath[i].szPath,Charset.forName("GBK")).trim());
                }
            }
        }
    }

    /**
     * 查询设备能力集
     * @param loginHandle 登录句柄
     * @param command 能力集命令
     * @param structure 能力集的结构体
     * @param channelID 通道号
     * @return
     */
    private boolean getDevConfig(NetSDKLib.LLong loginHandle, String command, Structure structure,int channelID){
        boolean result=false;
        int error[] = {0};
        int nBufferLen = 100*1024;
        byte[] strBuffer = new byte[nBufferLen];
        result=netSdk.CLIENT_QueryNewSystemInfo(loginHandle,command,channelID,strBuffer,nBufferLen,new IntByReference(0),3000);
        if(result){
            structure.write();
            if(configSdk.CLIENT_ParseData(command,strBuffer,structure.getPointer(),structure.size(),null)){
                structure.read();
            }else{
                System.out.println("Parse " + command + " Config Failed! Error = "+ToolKits.getErrorCode());
            }
        }else{
            System.out.printf("Get %s Config Failed!Last Error = %s\n" , command , ToolKits.getErrorCode());
        }
        return result;
    }

    /**
     * 音频文件预上传，校验该文件是否可以上传
     */
    public void preAudioUpload(){
        NET_IN_PRE_UPLOAD_REMOTE_FILE inparam=new NET_IN_PRE_UPLOAD_REMOTE_FILE();
        inparam.pszFileSrc=new File(srcFile).getAbsolutePath();
        Scanner scanner=new Scanner(System.in);
        for (int i = 0; i < destPaths.size(); i++) {
            System.out.println(i+" : "+destPaths.get(i));
        }
        System.out.println("please input the num to select the path to preupload: ");

        String destPath=destPaths.get(scanner.nextInt());
        //要上传的路径,上传文件夹+文件名
        inparam.pszFileDst=destPath+new File(srcFile).getName();
        NET_OUT_PRE_UPLOAD_REMOTE_FILE outparam=new NET_OUT_PRE_UPLOAD_REMOTE_FILE();
        boolean preUpload=netSdk.CLIENT_PreUploadRemoteFile(loginHandle,inparam,outparam,3000);
        if(preUpload){
            System.out.println("can upload: "+outparam.bContinue2Upload);
        }else{
            System.out.println("pre upload failed: "+ToolKits.getErrorCode()+",reason:"+EM_REASON_TYPE.getReason(outparam.emType));
        }
    }
    /**
     * 上传音频文件
     */
    public void audioUpload(){
        Scanner scanner=new Scanner(System.in);
        for (int i = 0; i < destPaths.size(); i++) {
            System.out.println(i+" : "+destPaths.get(i));
        }
        System.out.println("please input the num to select the path to upload: ");
        String destPath=destPaths.get(scanner.nextInt());
        //入参
        NetSDKLib.NET_IN_UPLOAD_REMOTE_FILE uploadParam=new NetSDKLib.NET_IN_UPLOAD_REMOTE_FILE();
        //要上传的文件
        File file=new File(srcFile);
        String filePath=file.getAbsolutePath();
        String fileName=file.getName();
        //源文件的路径
        uploadParam.pszFileSrc=new NativeString(filePath).getPointer();
        //目标文件名称
        uploadParam.pszFileDst=new NativeString(fileName).getPointer();
        // 接口参数上的注释:目标文件夹路径：可为NULL, NULL时设备使用默认路径
        //注意:如果为null,会使用默认路径，但使用默认路径上传会失败,
        // 需要先获取设备音频能力集，得到可上传的路径,并设置为目标文件夹路径
        uploadParam.pszFolderDst=new NativeString(destPath).getPointer();
        //文件分包大小(字节): 0表示不分包
        uploadParam.nPacketLen=1024*2;
        NetSDKLib.NET_OUT_UPLOAD_REMOTE_FILE uploadOutParam=new NetSDKLib.NET_OUT_UPLOAD_REMOTE_FILE();
        uploadParam.write();
        boolean isUpload=netSdk.CLIENT_UploadRemoteFile(loginHandle,uploadParam,uploadOutParam,3000);
        uploadParam.read();
        if(isUpload){
            System.out.println("upload audio success!");
        }else{
            /**
             * 如果上传失败,请检查上传的文件大小，上传路径,上传格式
             */
            System.out.println("failed to upload audio.the error is "+ToolKits.getErrorCode());
        }
    }

    /**
     * 获取音频文件列表
     */
    public void audioList(){
        // 出参
        NetSDKLib.NET_OUT_LIST_REMOTE_FILE stOut = new NetSDKLib.NET_OUT_LIST_REMOTE_FILE();
        audios.clear();
        for (String path:audioPaths) {
            SDK_REMOTE_FILE_INFO[] remoteFile = ToolKits.ListAudioFile(loginHandle, path, stOut);
            if(remoteFile != null) {
                System.out.println("nRetFileCount : " + stOut.nRetFileCount);
                for(int j = 0; j < stOut.nRetFileCount; j++) {
                    System.out.println("szPath : " + new String(remoteFile[j].szPath, Charset.forName("GBK")).trim());
                    audios.add(new String(remoteFile[j].szPath,Charset.forName("GBK")));
                }
            } else {
                System.err.println("ListRemoteFile Failed!" + ToolKits.getErrorCode());
            }
        }

    }

    /**
     * 播放音频文件
     */
    public void audioPlay(){

        for(int i=0;i<audios.size();i++){
            System.out.println(i+" : "+audios.get(i));
        }
        System.out.println("Please choose the audio to play,input the num: ");
        Scanner scanner=new Scanner(System.in);
        String path=audios.get(scanner.nextInt());
        System.out.println("path:"+path);
        NetSDKLib.NET_CTRL_START_PLAYAUDIO playParam=new NetSDKLib.NET_CTRL_START_PLAYAUDIO();
        //字节数组赋值请使用System.arraycopy
        byte[] pathArray=null;
        try {
            pathArray = path.getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.arraycopy(pathArray,0,playParam.szAudioPath,0,pathArray.length);
        Pointer pointer=new Memory(playParam.size());
        ToolKits.SetStructDataToPointer(playParam,pointer,0);
        boolean isPlay=netSdk.CLIENT_ControlDevice(loginHandle,CTRLTYPE_CTRL_START_PLAYAUDIO,pointer,3000);
        if(isPlay){
            System.out.println("play audio success.");
        }else{
            System.out.println("failed to play audio.the error is "+ToolKits.getErrorCode());
        }
    }

    /**
     * 删除音频文件
     */
    public void audioRemove(){
        for(int i=0;i<audios.size();i++){
            System.out.println(i+" : "+audios.get(i));
        }
        System.out.println("Please choose the audio to remove,input the num: ");
        Scanner scanner=new Scanner(System.in);
        int     index=scanner.nextInt();
        String path=audios.get(index);
        if(ToolKits.RemoveAudioFiles(loginHandle, path)) {
            System.out.println("RemoveRemoteFiles Succeed!");
            audios.remove(index);
        } else {
            System.err.println("RemoveRemoteFiles Failed!" + ToolKits.getErrorCode());
        }

    }
    //   CLIENT_GetConfig/CLIENT_SetConfig
    //           NET_EM_CFG_LOOPPLAYBACK_AUDIOALARM,		//
    /**(2021-12-7)
     * 音频循环播放报警配置，对应结构体 NET_LOOPPLAYBACK_AUDIOALARM_INFO,与通道不相关,通道号需要填成-1
     */
    public void loopPlayBackAudioAlarm() throws UnsupportedEncodingException {

        NET_LOOPPLAYBACK_AUDIOALARM_INFO msg=new NET_LOOPPLAYBACK_AUDIOALARM_INFO();

        int dwOutBufferSize=msg.size();
        Pointer szOutBuffer =new Memory(dwOutBufferSize);
        ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
        boolean ret=netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_LOOPPLAYBACK_AUDIOALARM, -1, szOutBuffer, dwOutBufferSize, 3000, null);
        if(!ret) {
            System.err.printf("getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
            return;
        }
        ToolKits.GetPointerData(szOutBuffer, msg);
        int bEnable = msg.bEnable;
        System.out.println("使能:"+bEnable);
        byte[] szAudioFilePath = msg.szAudioFilePath;

        System.out.println("语音播报文件路径gbk:"+new String(szAudioFilePath,"GBK"));
        System.out.println("语音播报文件路径utf:"+new String(szAudioFilePath,"UTF-8"));

        byte[] szStartTime = msg.szStartTime;
        System.out.println("开始时间:"+new String(szStartTime));
        byte[] szEndTime = msg.szEndTime;
        System.out.println("结束时间:"+new String(szEndTime));

        NetSDKLib.NET_TSECT[] stuTimeSection = msg.stuTimeSection;
        for (NetSDKLib.NET_TSECT stuTime:stuTimeSection
        ) {
            System.out.println("start:"+stuTime.startTime());
            System.out.println("end:"+stuTime.endTime());

        }


        msg.bEnable=1;
        IntByReference restart = new IntByReference(0);
        int dwInBufferSize=msg.size();
        Pointer szInBuffer =new Memory(dwInBufferSize);
        ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
        boolean result=netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_LOOPPLAYBACK_AUDIOALARM, -1, szInBuffer, dwInBufferSize, 3000, restart, null);
        if(!result) {
            System.err.printf("setconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
        }else {
            System.out.println("setconfig success");
        }


    }

    public void run(){
        CaseMenu menu=new CaseMenu();
        menu.addItem(new CaseMenu.Item(this,"获取设备音频能力","getDeviceAudioCap"));
        menu.addItem(new CaseMenu.Item(this,"音频文件预上传(判断文件是否可以上传)","preAudioUpload"));
        menu.addItem(new CaseMenu.Item(this,"上传音频文件","audioUpload"));
        menu.addItem(new CaseMenu.Item(this,"获取音频列表","audioList"));
        menu.addItem(new CaseMenu.Item(this,"播放音频","audioPlay"));
        menu.addItem(new CaseMenu.Item(this,"删除音频文件","audioRemove"));
        menu.addItem(new CaseMenu.Item(this,"音频循环播放报警配置","loopPlayBackAudioAlarm"));
        menu.run();
    }

    public static void main(String[] args) {
        AudioControlDemo demo=new AudioControlDemo();
        demo.initTest();
        demo.run();
        demo.loginOut();
    }
}
