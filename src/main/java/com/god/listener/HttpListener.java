package com.god.listener;

/**
 * Created by abook23 on 2015/12/29.
 */
public interface HttpListener<T> {
    /**
     * 成功
     */
    void onAsyncSuccess(T t, int requestCode);

    /**
     * 失败
     */
    void onPostError(String msg, int resultCode, int requestCode);
}
