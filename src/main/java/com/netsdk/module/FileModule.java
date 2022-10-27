package com.netsdk.module;

import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_IN_PRE_UPLOAD_REMOTE_FILE;
import com.netsdk.lib.structure.NET_OUT_PRE_UPLOAD_REMOTE_FILE;
import com.netsdk.module.entity.DeliveryFileInfo;
import com.netsdk.module.entity.FilePreUploadResult;

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.netsdk.lib.NetSDKLib.MAX_DELIVERY_FILE_NUM;


/**
 * @author 47081
 * @version 1.0
 * @description 操作文件的二次封装类
 * @date 2020/9/14
 */
public class FileModule extends BaseModule {

    public FileModule(NetSDKLib netSdkApi) {
        super(netSdkApi);
    }

    private FileModule() {
        this(NetSDKLib.NETSDK_INSTANCE);
    }

    /**
     * 检测文件是否可以上传
     *
     * @param loginHandler 登录句柄
     * @param srcPath      源文件路径
     * @param destPath     上传到设备的路径
     * @return null:预上传接口调用失败,FilePreUploadResult: canUpload:true，可以上传,false:不可上传,emType:不可上传的原因
     */
    public FilePreUploadResult canUpload(long loginHandler, String srcPath, String destPath) {
        NET_IN_PRE_UPLOAD_REMOTE_FILE inParam = new NET_IN_PRE_UPLOAD_REMOTE_FILE();
        inParam.pszFileSrc = srcPath;
        inParam.pszFileDst = destPath;
        NET_OUT_PRE_UPLOAD_REMOTE_FILE outParam = new NET_OUT_PRE_UPLOAD_REMOTE_FILE();
        boolean result = getNetsdkApi().CLIENT_PreUploadRemoteFile(new NetSDKLib.LLong(loginHandler), inParam, outParam, 5000);
        if (!result) {
            System.out.println("pre upload failed." + ENUMERROR.getErrorMessage());
            return null;
        }
        return new FilePreUploadResult(outParam.bContinue2Upload, outParam.emType);
    }

    /**
     * 上传文件
     *
     * @param srcPath  源文件路径,绝对路径
     * @param destPath 上传到设备的目录
     * @param fileName 上传后的文件的文件名
     * @return
     */
    public boolean uploadFile(long loginHandle, String srcPath, String destPath, String fileName) {
        NetSDKLib.NET_IN_UPLOAD_REMOTE_FILE uploadParam = new NetSDKLib.NET_IN_UPLOAD_REMOTE_FILE();
        uploadParam.pszFileSrc = new NativeString(srcPath).getPointer();
        if (destPath != null && !destPath.trim().equals("")) {
            uploadParam.pszFolderDst = new NativeString(destPath).getPointer();
        }
        uploadParam.pszFileDst = new NativeString(fileName).getPointer();
        //文件分包大小(字节): 0表示不分包
        //大文件要分包,特别是dav视频,建议512kb
        uploadParam.nPacketLen = 1024 * 512;
        NetSDKLib.NET_OUT_UPLOAD_REMOTE_FILE uploadOutParam = new NetSDKLib.NET_OUT_UPLOAD_REMOTE_FILE();
        uploadParam.write();
        boolean isUpload = getNetsdkApi().CLIENT_UploadRemoteFile(new NetSDKLib.LLong(loginHandle), uploadParam, uploadOutParam, 3000);
        uploadParam.read();
        if (!isUpload) {
            /**
             * 如果上传失败,请检查上传的文件大小，上传路径,上传格式
             */
            System.out.println(ENUMERROR.getErrorMessage());
            return false;
        }
        return true;
    }

    /**
     * 向视频输出口投放视频和图片文件
     *
     * @param loginHandle 登录句柄
     * @param port        要投放文件的端口
     * @param emPlayMode  播放类型 0:未知,1:播放一次,2:循环播放
     * @param startTime   开始时间
     * @param endTime     结束时间,当emPlayMode为2时有效
     * @param infos       文件信息列表
     * @return 是否投放成功
     */
    public boolean deliveryFileToDevice(long loginHandle, int port, int emPlayMode, Date startTime, Date endTime, List<DeliveryFileInfo> infos) {
        NetSDKLib.NET_CTRL_DELIVERY_FILE stuInfo = new NetSDKLib.NET_CTRL_DELIVERY_FILE();
        stuInfo.nPort = port;
        stuInfo.emPlayMode = emPlayMode;
        if (startTime == null) {
            System.out.println("startTime is null");
            return false;
        }
        if (infos.size() > MAX_DELIVERY_FILE_NUM) {
            System.out.println("out of delivery file num");
            return false;
        }
        //时间赋值
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        stuInfo.stuStartPlayTime.dwYear = calendar.get(Calendar.YEAR);
        stuInfo.stuStartPlayTime.dwMonth = calendar.get(Calendar.MONTH);
        stuInfo.stuStartPlayTime.dwDay = calendar.get(Calendar.DATE);
        //24小时制
        stuInfo.stuStartPlayTime.dwHour = calendar.get(Calendar.HOUR_OF_DAY);
        stuInfo.stuStartPlayTime.dwMinute = calendar.get(Calendar.MINUTE);
        stuInfo.stuStartPlayTime.dwSecond = calendar.get(Calendar.SECOND);
        if (emPlayMode == 2) {
            calendar.setTime(endTime);
            stuInfo.stuStopPlayTime.dwYear = calendar.get(Calendar.YEAR);
            stuInfo.stuStopPlayTime.dwMonth = calendar.get(Calendar.MONTH);
            stuInfo.stuStopPlayTime.dwDay = calendar.get(Calendar.DATE);
            //24小时制
            stuInfo.stuStopPlayTime.dwHour = calendar.get(Calendar.HOUR_OF_DAY);
            stuInfo.stuStopPlayTime.dwMinute = calendar.get(Calendar.MINUTE);
            stuInfo.stuStopPlayTime.dwSecond = calendar.get(Calendar.SECOND);
        }

        //文件赋值
        stuInfo.nFileCount = infos.size();
        DeliveryFileInfo info = null;
        for (int i = 0; i < infos.size(); i++) {
            info = infos.get(i);
            stuInfo.stuFileInfo[i].emFileType = info.getEmFileType().getType();
            stuInfo.stuFileInfo[i].nImageSustain = info.getnImageSustain();
            System.arraycopy(info.getSzFileUrl().getBytes(Charset.forName(Utils.getPlatformEncode())), 0, stuInfo.stuFileInfo[i].szFileURL, 0,
                    info.getSzFileUrl().getBytes(Charset.forName(Utils.getPlatformEncode())).length);
        }
        stuInfo.write();

        int emType = NetSDKLib.CtrlType.CTRLTYPE_CTRL_DELIVERY_FILE;
        boolean bRet = getNetsdkApi().CLIENT_ControlDevice(new NetSDKLib.LLong(loginHandle), emType,
                stuInfo.getPointer(), 3000);
        stuInfo.read();
        if (!bRet) {
            System.out.println("deliveryFile failed." + ENUMERROR.getErrorMessage());
        }
        System.out.println("deliveryFile success");
        return bRet;
    }

}
