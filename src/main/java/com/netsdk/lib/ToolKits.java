package com.netsdk.lib;

import com.netsdk.lib.NetSDKLib.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import java.io.*;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

public class ToolKits {
    static NetSDKLib netsdkapi = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib configapi = NetSDKLib.CONFIG_INSTANCE;

    public ToolKits() {

    }

    /***************************************************************************************************
     *     CLIENT_GetNewDevConfig CLIENT_ParseData 和 CLIENT_PacketData  CLIENT_SetNewDevConfig 封装        *
     ***************************************************************************************************/
    /**
     * 获取多个配置
     *
     * @param hLoginHandle 登陆句柄
     * @param nChn         通道号，-1 表示全通道
     * @param strCmd       配置名称
     * @param cmdObjects   配置对应的结构体对象
     * @return 成功返回实际获取到的配置个数
     */
    public static int GetDevConfig(LLong hLoginHandle, int nChn, String strCmd, Structure[] cmdObjects) {
        IntByReference error = new IntByReference(0);
        int nBufferLen = 2 * 1024 * 1024;
        byte[] strBuffer = new byte[nBufferLen];

        if (!netsdkapi.CLIENT_GetNewDevConfig(hLoginHandle, strCmd, nChn, strBuffer, nBufferLen, error, 5000)) {
            System.err.printf("Get %s Config Failed!Last Error = %x\n", strCmd, netsdkapi.CLIENT_GetLastError());
            return -1;
        }

        IntByReference retLength = new IntByReference(0);
        int memorySize = cmdObjects.length * cmdObjects[0].size();
        Pointer objectsPointer = new Memory(memorySize);
        objectsPointer.clear(memorySize);

        SetStructArrToPointerData(cmdObjects, objectsPointer);

        if (!configapi.CLIENT_ParseData(strCmd, strBuffer, objectsPointer, memorySize, retLength.getPointer())) {
            System.err.println("Parse " + strCmd + " Config Failed!");
            return -1;
        }

        GetPointerDataToStructArr(objectsPointer, cmdObjects);

        return (retLength.getValue() / cmdObjects[0].size());
    }

    /**
     * 获取单个配置
     *
     * @param hLoginHandle 登陆句柄
     * @param nChn         通道号，-1 表示全通道
     * @param strCmd       配置名称
     * @param cmdObject    配置对应的结构体对象
     * @return 成功返回 true
     */
    public static boolean GetDevConfig(LLong hLoginHandle, int nChn, String strCmd, Structure cmdObject) {
        IntByReference error = new IntByReference(0);
        IntByReference retLen = new IntByReference(0);
        int nBufferLen = 2 * 1024 * 1024;
        byte[] strBuffer = new byte[nBufferLen];
        if (!netsdkapi.CLIENT_GetNewDevConfig(hLoginHandle, strCmd, nChn, strBuffer, nBufferLen, error, 5000)) {
            System.err.printf("Get %s Config Failed!Last Error = %x\n", strCmd, netsdkapi.CLIENT_GetLastError());
            return false;
        }
        cmdObject.write();
        if (!configapi.CLIENT_ParseData(strCmd, strBuffer, cmdObject.getPointer(), cmdObject.size(), retLen.getPointer())) {
            System.err.println("Parse " + strCmd + " Config Failed!" + ToolKits.getErrorCode());
            return false;
        }
        cmdObject.read();
        System.out.println("Get:" + new String(strBuffer).trim());
        return true;
    }

    /**
     * 设置多个配置
     *
     * @param hLoginHandle 登陆句柄
     * @param nChn         通道号，-1 表示全通道
     * @param strCmd       配置名称
     * @param cmdObjects   配置对应的结构体对象
     * @return 成功返回 true
     */
    public static boolean SetDevConfig(LLong hLoginHandle, int nChn, String strCmd, Structure[] cmdObjects) {
        int nBufferLen = 2 * 1024 * 1024;
        byte[] szBuffer = new byte[nBufferLen];
        for (int i = 0; i < nBufferLen; i++) szBuffer[i] = 0;
        IntByReference error = new IntByReference(0);
        IntByReference restart = new IntByReference(0);

        int memorySize = cmdObjects.length * cmdObjects[0].size();
        Pointer objectsPointer = new Memory(memorySize);
        objectsPointer.clear(memorySize);

        SetStructArrToPointerData(cmdObjects, objectsPointer);

        if (!configapi.CLIENT_PacketData(strCmd, objectsPointer, memorySize, szBuffer, nBufferLen)) {
            System.err.println("Packet " + strCmd + " Config Failed!");
            return false;
        }

        String strOut = new String(szBuffer).trim();
        System.out.println(strOut);

        if (!netsdkapi.CLIENT_SetNewDevConfig(hLoginHandle, strCmd, nChn, szBuffer, nBufferLen, error, restart, 5000)) {
            System.err.printf("Set %s Config Failed! Last Error = %x\n", strCmd, netsdkapi.CLIENT_GetLastError());
            return false;
        }
        return true;
    }

    /**
     * 设置单个配置
     *
     * @param hLoginHandle 登陆句柄
     * @param nChn         通道号，-1 表示全通道
     * @param strCmd       配置名称
     * @param cmdObject    配置对应的结构体对象
     * @return 成功返回 true
     */
    public static boolean SetDevConfig(LLong hLoginHandle, int nChn, String strCmd, Structure cmdObject) {
        boolean result = false;
        int nBufferLen = 2 * 1024 * 1024;
        byte szBuffer[] = new byte[nBufferLen];
        for (int i = 0; i < nBufferLen; i++) szBuffer[i] = 0;
        IntByReference error = new IntByReference(0);
        IntByReference restart = new IntByReference(0);

        cmdObject.write();
        if (configapi.CLIENT_PacketData(strCmd, cmdObject.getPointer(), cmdObject.size(),
                szBuffer, nBufferLen)) {
            cmdObject.read();
            String buffer = new String(szBuffer).trim();
            System.out.println(buffer);
            if (netsdkapi.CLIENT_SetNewDevConfig(hLoginHandle, strCmd, nChn, szBuffer, nBufferLen, error, restart, 5000)) {
                result = true;
            } else {
                System.err.printf("Set %s Config Failed! Last Error = %x\n", strCmd, netsdkapi.CLIENT_GetLastError());
                result = false;
            }
        } else {
            System.err.println("Packet " + strCmd + " Config Failed!");
            result = false;
        }

        return result;
    }

    public static void GetPointerData(Pointer pNativeData, Structure pJavaStu) {
        GetPointerDataToStruct(pNativeData, 0, pJavaStu);
    }

    public static void GetPointerDataToStruct(Pointer pNativeData, long OffsetOfpNativeData, Structure pJavaStu) {
        pJavaStu.write();
        Pointer pJavaMem = pJavaStu.getPointer();
        pJavaMem.write(0, pNativeData.getByteArray(OffsetOfpNativeData, pJavaStu.size()), 0,
                pJavaStu.size());
        pJavaStu.read();
    }

    public static void GetPointerDataToStructArr(Pointer pNativeData, Structure[] pJavaStuArr) {
        long offset = 0;
        for (int i = 0; i < pJavaStuArr.length; ++i) {
            GetPointerDataToStruct(pNativeData, offset, pJavaStuArr[i]);
            offset += pJavaStuArr[i].size();
        }
    }

    /**
     * 将结构体数组拷贝到内存
     *
     * @param pNativeData
     * @param pJavaStuArr
     */
    public static void SetStructArrToPointerData(Structure[] pJavaStuArr, Pointer pNativeData) {
        long offset = 0;
        for (int i = 0; i < pJavaStuArr.length; ++i) {
            SetStructDataToPointer(pJavaStuArr[i], pNativeData, offset);
            offset += pJavaStuArr[i].size();
        }
    }

    public static void SetStructDataToPointer(Structure pJavaStu, Pointer pNativeData, long OffsetOfpNativeData) {
        pJavaStu.write();
        Pointer pJavaMem = pJavaStu.getPointer();
        pNativeData.write(OffsetOfpNativeData, pJavaMem.getByteArray(0, pJavaStu.size()), 0, pJavaStu.size());
    }

    public static void ByteArrToStructure(byte[] pNativeData, Structure pJavaStu) {
        pJavaStu.write();
        Pointer pJavaMem = pJavaStu.getPointer();
        pJavaMem.write(0, pNativeData, 0, pJavaStu.size());
        pJavaStu.read();
    }

    public static void ByteArrZero(byte[] dst) {
        // 清零
        for (int i = 0; i < dst.length; ++i) {
            dst[i] = 0;
        }
    }

    //byte转换为byte[2]数组
    public static byte[] getByteArray(byte b) {
        byte[] array = new byte[8];
        for (int i = 0; i < 8; i++) {
            array[i] = (byte) ((b & (1 << i)) > 0 ? 1 : 0);
        }

        return array;
    }

    public static byte[] getByteArrayEx(byte b) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }

    public static void StringToByteArr(String src, byte[] dst) {
        try {
            byte[] GBKBytes = src.getBytes("GBK");
            for (int i = 0; i < GBKBytes.length; i++) {
                dst[i] = (byte) GBKBytes[i];
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public static long GetFileSize(String filePath) {
        File f = new File(filePath);
        if (f.exists() && f.isFile()) {
            return f.length();
        } else {
            return 0;
        }
    }

    public static boolean ReadAllFileToMemory(String file, Memory mem) {
        if (mem != Memory.NULL) {
            long fileLen = GetFileSize(file);
            if (fileLen <= 0) {
                return false;
            }

            try {
                File infile = new File(file);
                if (infile.canRead()) {
                    FileInputStream in = new FileInputStream(infile);
                    int buffLen = 1024;
                    byte[] buffer = new byte[buffLen];
                    long currFileLen = 0;
                    int readLen = 0;
                    while (currFileLen < fileLen) {
                        readLen = in.read(buffer);
                        mem.write(currFileLen, buffer, 0, readLen);
                        currFileLen += readLen;
                    }

                    in.close();
                    return true;
                } else {
                    System.err.println("Failed to open file %s for read!!!\n");
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Failed to open file %s for read!!!\n");
                e.printStackTrace();
            }
        }

        return false;
    }

    public static void savePicture(byte[] pBuf, String sDstFile) {
        try {
            FileOutputStream fos = new FileOutputStream(sDstFile);
            fos.write(pBuf);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void savePicture(Pointer pBuf, int dwBufSize, String sDstFile) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(sDstFile)));
            out.write(pBuf.getByteArray(0, dwBufSize), 0, dwBufSize);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void savePicture(Pointer pBuf, int dwBufOffset, int dwBufSize, String sDstFile) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(sDstFile)));
            out.write(pBuf.getByteArray(dwBufOffset, dwBufSize), 0, dwBufSize);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 读取本地图片到byte[]
    public static byte[] readPictureToByteArray(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            System.err.println("picture is not exist!");
            return null;
        }

        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream((int) file.length());
        BufferedInputStream byteInStream = null;
        try {
            byteInStream = new BufferedInputStream(new FileInputStream(file));
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = byteInStream.read(buf)) != -1) {
                byteOutStream.write(buf, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                byteInStream.close();
                byteOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return byteOutStream.toByteArray();
    }

    // 将一位数组转为二维数组
    public static byte[][] ByteArrToByteArrArr(byte[] byteArr, int count, int length) {
        if (count * length != byteArr.length) {
            System.err.println(count * length + " != " + byteArr.length);
            return null;
        }
        byte[][] byteArrArr = new byte[count][length];

        for (int i = 0; i < count; i++) {
            System.arraycopy(byteArr, i * length, byteArrArr[i], 0, length);
        }

        return byteArrArr;
    }

    /**
     * 获取接口错误码
     *
     * @return
     */
    public static String getErrorCode() {
        return " { error code: ( 0x80000000|" + (netsdkapi.CLIENT_GetLastError() & 0x7fffffff) + " ). 参考  LastError.java }";
    }

    /**
     * 显示目录中文件和子目录
     *
     * @param lLoginID 登陆句柄
     * @param szPath   查询的文件路径
     * @param stOut    出参
     * @return 返回文件/目录信息数组
     */
    public static SDK_REMOTE_FILE_INFO[] ListRemoteFile(LLong lLoginID, String szPath, NET_OUT_LIST_REMOTE_FILE stOut) {
        NET_IN_LIST_REMOTE_FILE stIn = new NET_IN_LIST_REMOTE_FILE();
        stIn.pszPath = "/mnt/sdcard/" + szPath;
        stIn.bFileNameOnly = 1;  // 只获取文件名称, 不返回文件夹信息, 文件信息中只有文件名有效
        stIn.emCondition = NET_REMOTE_FILE_COND.NET_REMOTE_FILE_COND_NONE;

        int maxFileCount = 50;  // 每次查询的文件个数
        SDK_REMOTE_FILE_INFO[] remoteFileArr;
        while (true) {
            remoteFileArr = new SDK_REMOTE_FILE_INFO[maxFileCount];
            for (int i = 0; i < maxFileCount; i++) {
                remoteFileArr[i] = new SDK_REMOTE_FILE_INFO();
            }

            stOut.nMaxFileCount = maxFileCount;
            stOut.pstuFiles = new Memory(remoteFileArr[0].size() * maxFileCount);   // Pointer初始化
            stOut.pstuFiles.clear(remoteFileArr[0].size() * maxFileCount);

            ToolKits.SetStructArrToPointerData(remoteFileArr, stOut.pstuFiles);    // 将数组内存拷贝给Pointer

            if (netsdkapi.CLIENT_ListRemoteFile(lLoginID, stIn, stOut, 3000)) {
                if (maxFileCount > stOut.nRetFileCount) {
                    ToolKits.GetPointerDataToStructArr(stOut.pstuFiles, remoteFileArr);    // 将Pointer的信息输出到结构体
                    break;
                } else {
                    maxFileCount += 50;
                }
            } else {
                return null;
            }
        }
        return remoteFileArr;
    }

    /**
     * 显示目录中文件和子目录
     *
     * @param lLoginID 登陆句柄
     * @param szPath   查询的文件路径
     * @param stOut    出参
     * @return 返回文件/目录信息数组
     */
    public static SDK_REMOTE_FILE_INFO[] ListAudioFile(LLong lLoginID, String szPath, NET_OUT_LIST_REMOTE_FILE stOut) {
        NET_IN_LIST_REMOTE_FILE stIn = new NET_IN_LIST_REMOTE_FILE();
        stIn.pszPath = szPath;
        stIn.bFileNameOnly = 1;  // 只获取文件名称, 不返回文件夹信息, 文件信息中只有文件名有效
        stIn.emCondition = NET_REMOTE_FILE_COND.NET_REMOTE_FILE_COND_NONE;

        int maxFileCount = 50;  // 每次查询的文件个数
        SDK_REMOTE_FILE_INFO[] remoteFileArr;
        while (true) {
            remoteFileArr = new SDK_REMOTE_FILE_INFO[maxFileCount];
            for (int i = 0; i < maxFileCount; i++) {
                remoteFileArr[i] = new SDK_REMOTE_FILE_INFO();
            }

            stOut.nMaxFileCount = maxFileCount;
            stOut.pstuFiles = new Memory(remoteFileArr[0].size() * maxFileCount);   // Pointer初始化
            stOut.pstuFiles.clear(remoteFileArr[0].size() * maxFileCount);

            ToolKits.SetStructArrToPointerData(remoteFileArr, stOut.pstuFiles);    // 将数组内存拷贝给Pointer

            if (netsdkapi.CLIENT_ListRemoteFile(lLoginID, stIn, stOut, 3000)) {
                if (maxFileCount > stOut.nRetFileCount) {
                    ToolKits.GetPointerDataToStructArr(stOut.pstuFiles, remoteFileArr);    // 将Pointer的信息输出到结构体
                    break;
                } else {
                    maxFileCount += 50;
                }
            } else {
                return null;
            }
        }
        return remoteFileArr;
    }

    /**
     * 删除文件或目录,删除具体的文件或者一类文件
     *
     * @param lLoginID 登陆句柄
     * @param szPath   删除的文件名称
     */
    public static boolean RemoveRemoteFilesEx(LLong lLoginID, String szPath) {
        boolean bRet = false;
        if (szPath.indexOf("*") != -1) {    // 删除一类文件
            String[] szPathStr = szPath.split("[*]");

            String szFindPath = szPathStr[0];                // 查询文件路径
            String szFileType = szPathStr[1].substring(1);  // 文件类型

            // 查询当前路径下的文件
            NET_OUT_LIST_REMOTE_FILE stOut = new NET_OUT_LIST_REMOTE_FILE();

            SDK_REMOTE_FILE_INFO[] remoteFile = ToolKits.ListRemoteFile(lLoginID, szFindPath, stOut);
            if (remoteFile != null) {
                for (int i = 0; i < stOut.nRetFileCount; i++) {
                    // 过滤并删除
                    if (new String(remoteFile[i].szPath).trim().indexOf(szFileType) != -1) {
                        bRet = RemoveRemoteFiles(lLoginID, szFindPath + new String(remoteFile[i].szPath).trim());
                    }
                }
            }
        } else { // 删除具体的文件
            bRet = RemoveRemoteFiles(lLoginID, szPath);
        }
        return bRet;
    }

    /**
     * 删除多个文件或目录
     *
     * @param lLoginID 登陆句柄
     * @param szPath   删除的文件名称
     * @return true 成功; false 失败
     */
    public static boolean RemoveRemoteFilesArr(LLong lLoginID, String[] szPath) {
        FILE_PATH[] filePath = new FILE_PATH[szPath.length];
        for (int i = 0; i < szPath.length; i++) {
            filePath[i] = new FILE_PATH();
        }

        for (int i = 0; i < szPath.length; i++) {
            filePath[i].pszPath = "/mnt/sdcard/" + szPath[i];
            System.out.println("/mnt/sdcard/" + szPath[i]);
        }

        // 入参
        NET_IN_REMOVE_REMOTE_FILES stIn = new NET_IN_REMOVE_REMOTE_FILES();
        stIn.nFileCount = szPath.length;
        stIn.pszPathPointer = new Memory(filePath[0].size() * szPath.length);
        stIn.pszPathPointer.clear(filePath[0].size() * szPath.length);

        ToolKits.SetStructArrToPointerData(filePath, stIn.pszPathPointer);

        // 出参
        NET_OUT_REMOVE_REMOTE_FILES stOut = new NET_OUT_REMOVE_REMOTE_FILES();

        boolean bRet = netsdkapi.CLIENT_RemoveRemoteFiles(lLoginID, stIn, stOut, 3000);

        return bRet;
    }

    /**
     * 删除单个文件或目录
     *
     * @param lLoginID 登陆句柄
     * @param szPath   删除的文件名称
     * @return true 成功; false 失败
     */
    public static boolean RemoveAudioFiles(LLong lLoginID, String szPath) {
        FILE_PATH filePath = new FILE_PATH();
        filePath.pszPath = szPath;

        // 入参
        NET_IN_REMOVE_REMOTE_FILES stIn = new NET_IN_REMOVE_REMOTE_FILES();
        stIn.nFileCount = 1;
        stIn.pszPathPointer = filePath.getPointer();

        // 出参
        NET_OUT_REMOVE_REMOTE_FILES stOut = new NET_OUT_REMOVE_REMOTE_FILES();

        filePath.write();
        boolean bRet = netsdkapi.CLIENT_RemoveRemoteFiles(lLoginID, stIn, stOut, 3000);
        filePath.read();

        return bRet;
    }

    /**
     * 删除单个文件或目录
     *
     * @param lLoginID 登陆句柄
     * @param szPath   删除的文件名称
     * @return true 成功; false 失败
     */
    public static boolean RemoveRemoteFiles(LLong lLoginID, String szPath) {
        FILE_PATH filePath = new FILE_PATH();
        filePath.pszPath = "/mnt/sdcard/" + szPath;

        // 入参
        NET_IN_REMOVE_REMOTE_FILES stIn = new NET_IN_REMOVE_REMOTE_FILES();
        stIn.nFileCount = 1;
        stIn.pszPathPointer = filePath.getPointer();

        // 出参
        NET_OUT_REMOVE_REMOTE_FILES stOut = new NET_OUT_REMOVE_REMOTE_FILES();

        filePath.write();
        boolean bRet = netsdkapi.CLIENT_RemoveRemoteFiles(lLoginID, stIn, stOut, 3000);
        filePath.read();

        return bRet;
    }

    /**
     * 获取播放盒上全部节目信息
     *
     * @param lLoginID d登陆句柄
     * @param stOut    出参
     * @return 返回播放盒节目信息数组
     */
    public static NET_PROGRAM_ON_PLAYBOX[] GetAllProgramOnPlayBox(LLong lLoginID, NET_OUT_GET_ALL_PLAYBOX_PROGRAM stOut) {
        // 入参
        NET_IN_GET_ALL_PLAYBOX_PROGRAM stIn = new NET_IN_GET_ALL_PLAYBOX_PROGRAM();

        NET_PROGRAM_ON_PLAYBOX[] playboxArr;
        int maxProgramCount = 10; // 每次查询的节目信息个数
        while (true) {
            playboxArr = new NET_PROGRAM_ON_PLAYBOX[maxProgramCount];
            for (int i = 0; i < maxProgramCount; i++) {
                playboxArr[i] = new NET_PROGRAM_ON_PLAYBOX();
                for (int j = 0; j < NetSDKLib.MAX_WINDOWS_COUNT; j++) {
                    // 申请一块内存，自己设置，设置大点
                    playboxArr[i].stuOrdinaryInfo.stuWindowsInfo[j].pstElementsBuf = new Memory(100 * 1024);
                    playboxArr[i].stuOrdinaryInfo.stuWindowsInfo[j].pstElementsBuf.clear(100 * 1024);
                    playboxArr[i].stuOrdinaryInfo.stuWindowsInfo[j].nBufLen = 100 * 1024;
                }
            }

            // 出参
            stOut.nMaxProgramCount = maxProgramCount;
            stOut.pstProgramInfo = new Memory(playboxArr[0].size() * maxProgramCount);
            stOut.pstProgramInfo.clear(playboxArr[0].size() * maxProgramCount);

            ToolKits.SetStructArrToPointerData(playboxArr, stOut.pstProgramInfo);   // 将数组内存拷贝给Pointer

            if (netsdkapi.CLIENT_GetAllProgramOnPlayBox(lLoginID, stIn, stOut, 3000)) {
                if (maxProgramCount > stOut.nRetProgramCount) {
                    ToolKits.GetPointerDataToStructArr(stOut.pstProgramInfo, playboxArr);    // 将Pointer的信息输出到结构体
                    break;
                } else {
                    maxProgramCount += 10;
                }
            } else {
                return null;
            }
        }

        return playboxArr;
    }

    /**
     * 批量删除节目信息
     *
     * @param lLoginID          登陆句柄
     * @param szProGrammeIdList 需要删除的节目ID
     * @return true 成功; false 失败
     */
    public static boolean DelMultiProgrammesById(LLong lLoginID, String[] szProGrammeIdList) {
        // 入参
        NET_IN_DEL_PROGRAMMES stIn = new NET_IN_DEL_PROGRAMMES();
        stIn.nProgrammeID = szProGrammeIdList.length;  // 需要删除的节目ID个数

        for (int i = 0; i < szProGrammeIdList.length; i++) {
            System.arraycopy(szProGrammeIdList[i].getBytes(), 0, stIn.szProGrammeIdListArr[i].szProGrammeIdList, 0, szProGrammeIdList[i].getBytes().length);
        }

        // 出参
        NET_OUT_DEL_PROGRAMMES stOut = new NET_OUT_DEL_PROGRAMMES();

        return netsdkapi.CLIENT_DelMultiProgrammesById(lLoginID, stIn, stOut, 5000);
    }

    /**
     * 删除多个节目计划
     *
     * @param lLoginID 登陆句柄
     * @param szPlanID 需要删除的计划ID
     * @return true 成功; false 失败
     */
    public static boolean DelMultiProgrammePlans(LLong lLoginID, String[] szPlanID) {
        // 入参
        NET_IN_DEL_PROGRAMMEPLANS stIn = new NET_IN_DEL_PROGRAMMEPLANS();
        stIn.nPlanID = szPlanID.length;  // 节目计划ID个数

        for (int i = 0; i < szPlanID.length; i++) {
            System.arraycopy(szPlanID[i].getBytes(), 0, stIn.szPlanIDArr[i].szPlanID, 0, szPlanID[i].getBytes().length);
        }

        // 出参
        NET_OUT_DEL_PROGRAMMEPLANS stOut = new NET_OUT_DEL_PROGRAMMEPLANS();

        return netsdkapi.CLIENT_DelMultiProgrammePlans(lLoginID, stIn, stOut, 5000);
    }

    /**
     * 获取所有节目计划信息
     *
     * @param lLoginID 登陆句柄
     * @param stOut    出参
     * @return NET_PROGRAMME_PLANS_INFO 结构体
     */
    public static NET_PROGRAMME_PLANS_INFO GetAllProgrammePlans(LLong lLoginID, NET_OUT_GET_ALL_PROGRAMMEPLANS stOut) {
        // 入参
        NET_IN_GET_ALL_PROGRAMMEPLANS stIn = new NET_IN_GET_ALL_PROGRAMMEPLANS();

        NET_PROGRAMME_PLANS_INFO planInfo;
        int maxPlanCnt = 10; // 每次查询的计划个数
        while (true) {
            planInfo = new NET_PROGRAMME_PLANS_INFO(maxPlanCnt);

            // 出参
            stOut.nMaxPlanCnt = maxPlanCnt;
            stOut.pstImmePlan = new Memory(stOut.nMaxPlanCnt * planInfo.szImmePlan[0].size());
            stOut.pstImmePlan.clear(stOut.nMaxPlanCnt * planInfo.szImmePlan[0].size());

            stOut.pstTimerPlan = new Memory(stOut.nMaxPlanCnt * planInfo.szTimerPlan[0].size());
            stOut.pstTimerPlan.clear(stOut.nMaxPlanCnt * planInfo.szTimerPlan[0].size());

            ToolKits.SetStructArrToPointerData(planInfo.szImmePlan, stOut.pstImmePlan);       // 将数组内存拷贝给Pointer
            ToolKits.SetStructArrToPointerData(planInfo.szTimerPlan, stOut.pstTimerPlan);     // 将数组内存拷贝给Pointer

            if (netsdkapi.CLIENT_GetAllProgrammePlans(lLoginID, stIn, stOut, 3000)) {
                if (maxPlanCnt > stOut.nRetImmCnt && maxPlanCnt > stOut.nRetTimerCnt) {
                    ToolKits.GetPointerDataToStructArr(stOut.pstImmePlan, planInfo.szImmePlan);  // 将Pointer的值输出到数组
                    ToolKits.GetPointerDataToStructArr(stOut.pstTimerPlan, planInfo.szTimerPlan);  // 将Pointer的值输出到数组
                    break;
                } else {
                    maxPlanCnt += 10;
                }
            } else {
                return null;
            }
        }
        return planInfo;
    }

    // Win下，将GBK String类型的转为Pointer
    public static Pointer GetGBKStringToPointer(String src) {
        Pointer pointer = null;
        try {
            byte[] b = src.getBytes("GBK");

            pointer = new Memory(b.length + 1);
            pointer.clear(b.length + 1);

            pointer.write(0, b, 0, b.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return pointer;
    }

    // win下，将Pointer值转为 GBK String
    public static String GetPointerDataToGBKString(Pointer pointer) {
        String str = "";
        if (pointer == null) {
            return str;
        }

        int length = 0;
        byte[] bufferPlace = new byte[1];

        for (int i = 0; i < 2048; i++) {
            pointer.read(i, bufferPlace, 0, 1);
            if (bufferPlace[0] == '\0') {
                length = i;
                break;
            }
        }

        if (length > 0) {
            byte[] buffer = new byte[length];
            pointer.read(0, buffer, 0, length);
            try {
                str = new String(buffer, "GBK").trim();
            } catch (UnsupportedEncodingException e) {
                return str;
            }
        }

        return str;
    }

    // win下，将Pointer值转为 GBK String
    public static String GetPointerDataToGBKString(Pointer pointer, int length) {
        String str = "";
        if (pointer == null) {
            return str;
        }

        if (length > 0) {
            byte[] buffer = new byte[length];
            pointer.read(0, buffer, 0, length);
            try {
                str = new String(buffer, "GBK").trim();
            } catch (UnsupportedEncodingException e) {
                return str;
            }
        }

        return str;
    }

    public static void StringToByteArray(String src, byte[] dst) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] = 0;
        }

        System.arraycopy(src.getBytes(), 0, dst, 0, src.getBytes().length);
    }

    /**
     * 生成MD5
     *
     * @param path 图片路径
     * @return MD5
     * @throws FileNotFoundException
     */
    public static String GetStringMD5(String path) {
        File file = new File(path);

        String value = "";
        FileInputStream inputStream = null;

        try {
            inputStream = new FileInputStream(file);
            MappedByteBuffer byteBuffer = inputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

    /**
     * 写入流数据
     *
     * @param stream
     * @return
     */
    public static Pointer setStreamToPointer(InputStream stream) {
        Pointer pointer = null;
        try {
            int size = stream.available();
            pointer = new Memory(size);
            byte[] data = new byte[size];
            stream.read(data);
            pointer.write(0, data, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return pointer;
        }

    }
}
