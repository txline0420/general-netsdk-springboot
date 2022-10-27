package com.netsdk.demo.customize.faceReconEx;

import com.netsdk.demo.util.EventTaskHandler;
import com.sun.jna.Pointer;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

/**
 * @author ： 47040
 * @since ： Created in 2021/2/5 17:54
 */
public class SavePicHandler implements EventTaskHandler {

    private final byte[] imgBuffer;
    private final int length;
    private final String savePath;

    public SavePicHandler(Pointer pBuf, int dwBufOffset, int dwBufSize, String sDstFile) {

        this.imgBuffer = pBuf.getByteArray(dwBufOffset, dwBufSize);
        this.length = dwBufSize;
        this.savePath = sDstFile;
    }

    @Override
    public void eventCallBackProcess() {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(savePath)));
            out.write(imgBuffer, 0, length);
            out.close();
            System.out.println("保存图片成功: " + savePath);
        } catch (Exception e) {
            System.err.println("保存图片失败: " + e.getLocalizedMessage());
        }
    }
}
