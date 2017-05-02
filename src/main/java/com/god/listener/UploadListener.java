package com.god.listener;

/**
 * Created by abook23 on 2015/12/21.
 */
public interface UploadListener {
    void transferred(long num);//上传量

    void contentLength(long length);//文件大小

    void uploadSuccess(Object data);//完成返回
}
