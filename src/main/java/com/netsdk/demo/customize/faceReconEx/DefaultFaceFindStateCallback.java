package com.netsdk.demo.customize.faceReconEx;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author 47081
 * @version 1.0
 * @description 人脸搜索回调默认实现，回调函数请使用单例模式
 * @date 2021/4/28
 */
public class DefaultFaceFindStateCallback implements NetSDKLib.fFaceFindState {
  private static volatile DefaultFaceFindStateCallback instance;
  // 进度队列
  private static Map<FaceFindHandler, LinkedBlockingQueue<Integer>> progresses = new HashMap<>();

  private DefaultFaceFindStateCallback() {}

  public static DefaultFaceFindStateCallback getInstance() {
    if (instance == null) {
      synchronized (DefaultFaceFindStateCallback.class) {
        if (instance == null) {
          instance = new DefaultFaceFindStateCallback();
        }
      }
    }
    return instance;
  }

  @Override
  public void invoke(
      NetSDKLib.LLong lLoginID,
      NetSDKLib.LLong lAttachHandle,
      Pointer pstStates,
      int nStateNum,
      Pointer dwUser) {
    if (nStateNum < 1) {
      return;
    }
    FaceFindHandler faceFindHandler = new FaceFindHandler();
    LinkedBlockingQueue progress;
    NetSDKLib.NET_CB_FACE_FIND_STATE[] msg = new NetSDKLib.NET_CB_FACE_FIND_STATE[nStateNum];
    for (int i = 0; i < nStateNum; i++) {
      msg[i] = new NetSDKLib.NET_CB_FACE_FIND_STATE();
    }
    ToolKits.GetPointerDataToStructArr(pstStates, msg);
    for (int i = 0; i < nStateNum; i++) {
      faceFindHandler.setLoginHandler(lLoginID.longValue());
      faceFindHandler.setAttachHandler(lAttachHandle.longValue());
      faceFindHandler.setnToken(msg[i].nToken);
      progress = progresses.get(faceFindHandler);
      if (progress == null) {
        progress = new LinkedBlockingQueue();
      }
      progress.add(msg[i].nProgress);
      progresses.put(faceFindHandler, progress);
    }
  }

  private class FaceFindHandler {
    private long loginHandler;
    private long attachHandler;
    private long nToken;

    public FaceFindHandler() {}

    public FaceFindHandler(long loginHandler, long attachHandler, long nToken) {
      this.loginHandler = loginHandler;
      this.attachHandler = attachHandler;
      this.nToken = nToken;
    }

    public long getLoginHandler() {
      return loginHandler;
    }

    public void setLoginHandler(long loginHandler) {
      this.loginHandler = loginHandler;
    }

    public long getAttachHandler() {
      return attachHandler;
    }

    public void setAttachHandler(long attachHandler) {
      this.attachHandler = attachHandler;
    }

    public long getnToken() {
      return nToken;
    }

    public void setnToken(long nToken) {
      this.nToken = nToken;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;

      FaceFindHandler that = (FaceFindHandler) o;

      if (loginHandler != that.loginHandler) return false;
      if (attachHandler != that.attachHandler) return false;
      return nToken == that.nToken;
    }

    @Override
    public int hashCode() {
      int result = (int) (loginHandler ^ (loginHandler >>> 32));
      result = 31 * result + (int) (attachHandler ^ (attachHandler >>> 32));
      result = 31 * result + (int) (nToken ^ (nToken >>> 32));
      return result;
    }
  }

  public int getProgress(long loginHandler, long attachHandler, int nToken)
      throws InterruptedException {
    return progresses.get(new FaceFindHandler(loginHandler, attachHandler, nToken)).take();
  }
}
