package com.god.listener;

/**
 * Created by abook23 on 2015/12/29.
 */
public interface HttpRequestListener<T> {
    /**
     * 成功
     */
    void onHttpSuccess(T t, int requestCode);

    /**
     * 失败
     */
    void onHttpError(String msg, int resultCode, int requestCode);
}
