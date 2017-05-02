package com.god.listener;

import java.io.File;

public interface DownloadListener {

    /**
     * 开始下载 总量
     *
     * @param fileByteSize 总量
     */
    void onStart(float fileByteSize);

    /**
     * 暂停
     */
    void onPause();

    /**
     * 恢复
     */
    void onResume();

    /**
     * 当前现在量
     *
     * @param size    当前量
     * @param maxSize 总大小
     */
    void onSize(float size, float maxSize);

    /**
     * 失败
     */
    void onFail();

    /**
     * 成功
     *
     * @param file
     */
    void onSuccess(File file);

    /**
     * 取消下载
     */
    void onCancel();
}
