package com.netsdk.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.EM_DATA_SOURCE_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_MATCH_TWO_FACE_IN;
import com.netsdk.lib.structure.NET_MATCH_TWO_FACE_OUT;
import com.netsdk.module.entity.AddAnalyseTaskResult;
import com.netsdk.module.entity.ImageCompareInfo;
import com.netsdk.module.entity.PushAnalysePictureInfo;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author 47081
 * @version 1.0
 * @description 智能分析任务模块, 对智能分析任务相关接口进行二次封装, 方便调用
 * @date 2020/10/19
 */
public class AnalyseTaskModule extends BaseModule {

    public AnalyseTaskModule(NetSDKLib netSdkApi) {
        super(netSdkApi);
    }

    public AnalyseTaskModule() {
        super();
    }

    /**
     * 1:1图片比对
     *
     * @param originImage  原图片
     * @param compareImage 需要对比的图片
     * @param waitTime     超时时间,默认超时时间3000
     * @return 相似度 0-100
     */
    public int matchImage(long loginHandler, InputStream originImage, InputStream compareImage, int waitTime) throws IOException {
        BufferedImage image = ImageIO.read(originImage);
        NET_MATCH_TWO_FACE_IN inParam = new NET_MATCH_TWO_FACE_IN();
        ImageCompareInfo originInfo = readImage(image);
        //偏移量
        inParam.stuOriginalImage.dwoffset = 0;

        inParam.stuOriginalImage.dwLength = originInfo.getLength();
        inParam.stuOriginalImage.dwWidth = originInfo.getWidth();
        inParam.stuOriginalImage.dwHeight = originInfo.getHeight();
        image = ImageIO.read(compareImage);

        ImageCompareInfo compareInfo = readImage(image);
        //偏移量
        inParam.stuCompareImage.dwoffset = originInfo.getLength();
        inParam.stuCompareImage.dwLength = compareInfo.getLength();
        inParam.stuCompareImage.dwHeight = compareInfo.getHeight();
        inParam.stuCompareImage.dwWidth = compareInfo.getWidth();
        //图片写入
        inParam.pSendBuf = new Memory(originInfo.getLength() + compareInfo.getLength());
        inParam.pSendBuf.write(0, originInfo.getData(), 0, originInfo.getLength());
        inParam.pSendBuf.write(originInfo.getLength(), compareInfo.getData(), 0, compareInfo.getLength());
        //下发的数据大小
        inParam.dwSendBufLen = originInfo.getLength() + compareInfo.getLength();
        NET_MATCH_TWO_FACE_OUT outParam = new NET_MATCH_TWO_FACE_OUT();
        Pointer pInParam = new Memory(inParam.size());
        ToolKits.SetStructDataToPointer(inParam, pInParam, 0);
        Pointer pOutParam = new Memory(outParam.size());
        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);

        boolean result = getNetsdkApi().CLIENT_MatchTwoFaceImage(new NetSDKLib.LLong(loginHandler), pInParam, pOutParam, waitTime);

        if (!result) {
            System.out.println("match two image failed. error is " + ENUMERROR.getErrorMessage());
            return 0;
        }
        ToolKits.GetPointerData(pOutParam, outParam);
        return outParam.nSimilarity;
    }

    /**
     * 读取图片信息
     *
     * @param image 缓存的图片
     * @return
     * @throws IOException
     */
    private ImageCompareInfo readImage(BufferedImage image) {
        return readImage(image, "jpg");
    }

    private ImageCompareInfo readImage(BufferedImage image, String format) {
        ImageCompareInfo info = null;
        try {
            info = new ImageCompareInfo();
            info.setHeight(image.getHeight());
            info.setWidth(image.getWidth());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, format, outputStream);
            info.setData(outputStream.toByteArray());
            info.setLength(outputStream.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return info;
    }

    /**
     * 添加智能分析任务
     *
     * @param loginHandler 登录句柄
     * @param type         智能分析数据源类型{@link EM_DATA_SOURCE_TYPE}
     * @param inParam      输入参数,类型参考{@link EM_DATA_SOURCE_TYPE}所对应的结构体
     * @param waitTime     超时时间
     * @return 智能分析任务的结果
     */
    public AddAnalyseTaskResult addAnalyseTask(long loginHandler, EM_DATA_SOURCE_TYPE type, NetSDKLib.SdkStructure inParam, int waitTime) {
        Pointer pointer = new Memory(inParam.size());
        ToolKits.SetStructDataToPointer(inParam, pointer, 0);
        NetSDKLib.NET_OUT_ADD_ANALYSE_TASK outParam = new NetSDKLib.NET_OUT_ADD_ANALYSE_TASK();
        outParam.write();
        boolean result = getNetsdkApi().CLIENT_AddAnalyseTask(new NetSDKLib.LLong(loginHandler), type.getType(), pointer, outParam, waitTime);
        AddAnalyseTaskResult taskResult = new AddAnalyseTaskResult();
        taskResult.setResult(result);
        if (!result) {
            System.out.println("add analyseTask failed.error is " + ENUMERROR.getErrorMessage());
            return taskResult;
        }
        outParam.read();
        taskResult.setTaskId(outParam.nTaskID);
        taskResult.setVirtualChannel(outParam.nVirtualChannel);
        return taskResult;
    }

    /**
     * 启动智能分析任务
     *
     * @param loginHandler 登录句柄
     * @param taskId       任务id
     * @param waitTime     超时时间
     * @return 任务是否启动成功
     */
    public boolean startAnalyseTask(long loginHandler, int taskId, int waitTime) {
        NetSDKLib.NET_IN_START_ANALYSE_TASK inParam = new NetSDKLib.NET_IN_START_ANALYSE_TASK();
        inParam.nTaskID = taskId;
        NetSDKLib.NET_OUT_START_ANALYSE_TASK outParam = new NetSDKLib.NET_OUT_START_ANALYSE_TASK();
        boolean result = getNetsdkApi().CLIENT_StartAnalyseTask(new NetSDKLib.LLong(loginHandler), inParam, outParam, waitTime);
        if (!result) {
            System.out.println("start analyse task failed.error is " + ENUMERROR.getErrorMessage());
        }
        return result;
    }

    /**
     * 订阅智能分析任务
     *
     * @param loginHandler     登录句柄
     * @param taskIds          分析任务id数组
     * @param filterAlarmTypes 过滤的事件类型
     * @param isFilterImage    是否包含图片
     * @param waitTime         超时时间
     * @param callBack         智能分析的回调函数
     * @param dwUser           自定义数据,不需要可传入null
     * @return 订阅句柄, 句柄为0表示订阅失败, 句柄不为0订阅成功
     */
    public long attachAnalyseTaskResult(long loginHandler, int[] taskIds, int[] filterAlarmTypes, boolean isFilterImage, NetSDKLib.fAnalyseTaskResultCallBack callBack, int waitTime, Structure dwUser) {
        //对数组长度进行校验
        if (taskIds.length > NetSDKLib.MAX_ANALYSE_TASK_NUM) {
            System.out.println("taskIds's length is outOfBounds.please check");
            return 0;
        }
        if (filterAlarmTypes.length > NetSDKLib.MAX_ANALYSE_FILTER_EVENT_NUM) {
            System.out.println("dwAlarms's length is outOfBounds.Please check.");
            return 0;
        }
        NetSDKLib.NET_IN_ATTACH_ANALYSE_RESULT inParam = new NetSDKLib.NET_IN_ATTACH_ANALYSE_RESULT();
        //赋值
        inParam.nTaskIdNum = taskIds.length;
        System.arraycopy(taskIds, 0, inParam.nTaskIDs, 0, taskIds.length);
        inParam.stuFilter.nEventNum = filterAlarmTypes.length;
        System.arraycopy(filterAlarmTypes, 0, inParam.stuFilter.dwAlarmTypes, 0, filterAlarmTypes.length);
        inParam.stuFilter.nImageDataFlag = isFilterImage ? 1 : 0;
        inParam.cbAnalyseTaskResult = callBack;
        if (dwUser != null) {
            inParam.dwUser = dwUser.getPointer();
        }
        NetSDKLib.LLong attachHandler = getNetsdkApi().CLIENT_AttachAnalyseTaskResult(new NetSDKLib.LLong(loginHandler), inParam, waitTime);
        if (attachHandler.longValue() == 0) {
            System.out.println("attach analyseTask failed. error is " + ENUMERROR.getErrorMessage());
            return 0;
        }
        return attachHandler.longValue();
    }

    /**
     * 订阅智能分析任务
     *
     * @param loginHandler  登录句柄
     * @param taskIds       分析任务id
     * @param isFilterImage 是否包含图片
     * @param callBack      订阅回调函数,建议写成单例模式
     * @param waitTime      超时时间
     * @param dwUser        自定义数据
     * @return 订阅句柄, 句柄为0表示订阅失败, 不为0订阅成功
     */
    public long attachAnalyseTaskResult(long loginHandler, int[] taskIds, boolean isFilterImage, NetSDKLib.fAnalyseTaskResultCallBack callBack, int waitTime, Structure dwUser) {
        return attachAnalyseTaskResult(loginHandler, taskIds, new int[]{}, isFilterImage, callBack, waitTime, dwUser);
    }

    /**
     * 智能分析退订
     *
     * @param attachHandler 订阅句柄
     * @return 是否成功退订
     */
    public boolean detachAnalyseTaskResult(long attachHandler) {
        boolean result = getNetsdkApi().CLIENT_DetachAnalyseTaskResult(new NetSDKLib.LLong(attachHandler));
        if (!result) {
            System.out.println("detach analyseTask result failed. error is " + ENUMERROR.getErrorMessage());
        }
        return result;
    }

    ///////////////////////////推送图片有两种方式:文件的形式和远程url的方式,选择一种即可//////////////////////////////

    /**
     * @param type  推送图片还是url,1:图片文件,0:url
     * @param infos
     * @return
     */
    public boolean pushAnalysePicture(long loginHandler, int taskId, int type, List<PushAnalysePictureInfo> infos, int waitTime) {
        if (type != 1 && type != 0) {
            System.out.println("wrong type.please check.type 1: pictures,0:url");
            return false;
        }

        NetSDKLib.NET_IN_PUSH_ANALYSE_PICTURE_FILE inParam = new NetSDKLib.NET_IN_PUSH_ANALYSE_PICTURE_FILE();
        if (infos.size() > inParam.stuPushPicInfos.length) {
            System.out.println("infos's length is outOfBounds in stuPushInfos.");
        }
        inParam.nTaskID = taskId;
        inParam.nPicNum = infos.size();
        PushAnalysePictureInfo info = null;
        File file = null;
        int totalLength = 0;
        //推送图片
        if (type == 1) {
            for (int i = 0; i < inParam.nPicNum; i++) {
                info = infos.get(i);
                               
                byte[] fileId = info.getFileID().getBytes(Charset.forName(Utils.getPlatformEncode()));
                System.arraycopy(fileId, 0, inParam.stuPushPicInfos[i].szFileID, 0, fileId.length);
                                                
                //inParam.stuPushPicInfos[i].szFileID = info.getFileID().getBytes(Charset.forName(Utils.getPlatformEncode()));
                if (i == 0) {
                    inParam.stuPushPicInfos[i].nOffset = 0;
                } else {
                    inParam.stuPushPicInfos[i].nOffset = inParam.stuPushPicInfos[i - 1].nLength;
                }
                file = new File(info.getName());
                inParam.stuPushPicInfos[i].nLength = (int) file.length();
                totalLength += inParam.stuPushPicInfos[i].nLength;
                //stuXRayCustomInfo暂不赋值,自定义数据,xX光机定制专用
            }
            inParam.nBinBufLen = totalLength;
            //写入图片到Pointer
            inParam.pBinBuf = new Memory(totalLength);
            FileInputStream inputStream = null;
            byte[] data;
            int offset = 0;
            for (PushAnalysePictureInfo picture : infos) {

                try {
                    file = new File(picture.getName());
                    data = new byte[(int) file.length()];
                    inputStream = new FileInputStream(file);
                    inputStream.read(data);
                    inParam.pBinBuf.write(offset, data, 0, data.length);
                    offset += data.length;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        } else {
            //推送url
            for (int i = 0; i < infos.size(); i++) {
                info = infos.get(i);
               // inParam.stuPushPicInfos[i].szFileID = info.getFileID().getBytes(Charset.forName(Utils.getPlatformEncode()));                
                byte[] fileId = info.getFileID().getBytes(Charset.forName(Utils.getPlatformEncode()));
                System.arraycopy(fileId, 0, inParam.stuPushPicInfos[i].szFileID, 0, fileId.length);
               // inParam.stuPushPicInfos[i].szUrl = info.getName().getBytes(Charset.forName(Utils.getPlatformEncode()));                
                byte[] name = info.getName().getBytes(Charset.forName(Utils.getPlatformEncode()));
                System.arraycopy(name, 0, inParam.stuPushPicInfos[i].szUrl, 0, name.length);
            }
        }
        NetSDKLib.NET_OUT_PUSH_ANALYSE_PICTURE_FILE outParam = new NetSDKLib.NET_OUT_PUSH_ANALYSE_PICTURE_FILE();
        boolean result = getNetsdkApi().CLIENT_PushAnalysePictureFile(new NetSDKLib.LLong(loginHandler), inParam, outParam, waitTime);
        if (!result) {
            System.out.println("push picture failed.error is " + ENUMERROR.getErrorMessage());
        }
        return result;
    }

    /**
     * 删除(停止)智能分析任务
     *
     * @param loginHandler 登录句柄
     * @param taskId       智能任务id
     * @param waitTime     超时时间
     * @return 删除分析任务是否成功
     */
    public boolean removeAnalyseTask(long loginHandler, int taskId, int waitTime) {
        NetSDKLib.NET_IN_REMOVE_ANALYSE_TASK inParam = new NetSDKLib.NET_IN_REMOVE_ANALYSE_TASK();
        inParam.nTaskID = taskId;
        NetSDKLib.NET_OUT_REMOVE_ANALYSE_TASK outParam = new NetSDKLib.NET_OUT_REMOVE_ANALYSE_TASK();
        boolean result = getNetsdkApi().CLIENT_RemoveAnalyseTask(new NetSDKLib.LLong(loginHandler), inParam, outParam, waitTime);
        if (!result) {
            System.out.println("remove analyseTask failed.error is " + ENUMERROR.getErrorMessage());
        }
        return result;
    }

}
