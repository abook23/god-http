package com.god.http;

/**
 * Created by abook23 on 2016/4/17.
 */
public enum HttpCodeType {
    http200(200, "链接成功"), http400(400, "请求无效"), http401(401, "未授权：登录失败"), http403(403, "禁止访问"), http404(404, "服务器错误"),
    http405(405, "资源被禁止"), http500(500, "内部服务器错误"), http1001(1001, "客户端异常"), http1002(1002, "服务器请求超时");
    int code;
    String msg;

    HttpCodeType(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static String getHttpMsg(int code) {
        for (HttpCodeType h : HttpCodeType.values()) {
            if (h.code == code) {
                return h.msg;
            }
        }
        return "未知类型";
    }
}
