package com.god.http;


import com.god.listener.HttpListener;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGetHC4;
import org.apache.http.client.methods.HttpPostHC4;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStoreHC4;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;

/**
 * @author abook23
 */
public class HttpUtils {

    /**
     * 是否输出日志
     */
    public static boolean DEBUG = true;
    public static int LOG_NUMBER = 1000;

    private int HTTP_1002 = 1002;//服务器不响应
    private int HTTP_1001 = 1001;

    private static int CONNECTION_TIMEOUT = 5000;
    private static int SO_TIMEOUT = 10000;

    public static BasicCookieStoreHC4 cookieStore;
    private static HttpUtils httpUtils;
    private static PoolingHttpClientConnectionManager connectionManager;
    private static String caPath, caPassword;
    private int httpCode;

    private static HttpListener<HttpInfo> listener;

    private HttpUtils() {
    }

    public static HttpUtils getInstance() {
        if (httpUtils == null) {
            synchronized (HttpUtils.class) {
                if (httpUtils == null)
                    httpUtils = new HttpUtils();
            }
        }
        return httpUtils;
    }

    public static void setAsyncListener(HttpListener<HttpInfo> listener) {
        HttpUtils.listener = listener;
    }

    public static void clearSession() {
        cookieStore.clear();
        connectionManager = null;
    }

    /**
     * 设置自己的 CA 证书 ，如果 https 的证书是官方可信任的，就不用管了
     *
     * @param CAPath
     * @param CAPassword
     */
    public static void setHttpsCA(String CAPath, String CAPassword) {
        connectionManager = null;
        caPath = CAPath;
        caPassword = CAPassword;
    }

    /**
     * 设置超时
     *
     * @param connection_timeout 链接超时
     * @param so_timeout         读取超时
     *                           <p>default  connection_timeout = 5000,so_timeout=10000</p>
     */
    public static void setDefualConntTime(int connection_timeout, int so_timeout) {
        CONNECTION_TIMEOUT = connection_timeout;
        SO_TIMEOUT = so_timeout;
    }

    public static HttpInfo get(String url) {
        return getInstance().mHttpGet(url, null, CONNECTION_TIMEOUT, SO_TIMEOUT);
    }

    public static HttpInfo get(String url, Map<String, Object> param) {
        return getInstance().mHttpGet(url, param, CONNECTION_TIMEOUT, SO_TIMEOUT);
    }

    public static HttpInfo get(String url, Map<String, Object> param, int conn_time_out, int so_time_out) {
        return getInstance().mHttpGet(url, param, conn_time_out, so_time_out);
    }

    public static InputStream get2(String url, Map<String, Object> param, int conn_time_out, int so_time_out) throws IOException {
        return getInstance()._HttpGet(url, param, conn_time_out, so_time_out);
    }

    public static HttpInfo post(String url, Map<String, Object> params) {
        return getInstance().mHttpPost(url, params, CONNECTION_TIMEOUT, SO_TIMEOUT);
    }

    public static HttpInfo post(String url, Map<String, Object> params, int conn_time_out, int so_time_out) {
        return getInstance().mHttpPost(url, params, conn_time_out, so_time_out);
    }

    public static HttpInfo URLConnectionGet(String url) {
        return getInstance().HttpURLConnectionByGet(url);
    }

    public static HttpInfo URLConnectionPost(String url, Map<String, Object> params) {
        return getInstance().HttpURLConnectionByPost(url, params);
    }


    public CloseableHttpClient getDefaultHttpClient() {
        return getHttpClient();
    }


    private KeyStore getKeyStore() throws KeyStoreException {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream inputStream = null;
        try {
            if (caPath == null || caPassword == null) {
                trustStore.load(null, null);
            } else {
                inputStream = new FileInputStream(new File(caPath));
                trustStore.load(inputStream, caPassword.toCharArray());//签名,和密码
            }
        } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
        }
        return trustStore;
    }

    /**
     * 获取https
     *
     * @return
     */
    private CloseableHttpClient getHttpClient() {
        try {
            if (connectionManager == null) {
                if (cookieStore == null)
                    cookieStore = new BasicCookieStoreHC4();
                KeyStore trustStore = getKeyStore();
                // 相信自己的CA和所有自签名的证书
                //SSLContext sslcontext = SSLContexts.createSystemDefault();
                SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore,
                        new TrustStrategy() {
                            @Override
                            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                                return true;
                            }
                        }).build();
                SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                        sslcontext, new String[]{"TLSv1", "SSLv3"}, null,
                        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                        .<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                        .register("https", sslConnectionSocketFactory)
                        .build();
                connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            }
            return HttpClients.custom().setDefaultCookieStore(cookieStore).setConnectionManager(connectionManager).build();
        } catch (Exception e) {
            e.printStackTrace();
            return HttpClients.createSystem();
        }
    }

    // Get方式请求
    private HttpInfo HttpURLConnectionByGet(String path) {
        HttpInfo httpInfo = new HttpInfo();
        // 新建一个URL对象
        try {
            URL url = new URL(path);
            // 打开一个HttpURLConnection连接
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            // 设置连接超时时间
            urlConn.setConnectTimeout(CONNECTION_TIMEOUT);
            try {
                urlConn.setRequestMethod("GET");
                // 开始连接
                urlConn.connect();
                // 判断请求是否成功
                if (urlConn.getResponseCode() == 200) {
                    // 获取返回的数据
                    byte[] data = readStream(urlConn.getInputStream());
                    String str = new String(data, "UTF-8");
                    httpInfo.setResult(str);
                }
                httpInfo.setHttpCode(urlConn.getResponseCode());
            } catch (IOException e) {
                e.printStackTrace();
                httpInfo.setHttpCode(HTTP_1001);
            }
            // 关闭连接
            urlConn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            httpInfo.setHttpCode(HTTP_1002);
        }
        println(httpInfo);
        return httpInfo;
    }

    // Post方式请求
    private HttpInfo HttpURLConnectionByPost(String strUrl, Map<String, Object> params) {
        HttpInfo httpInfo = new HttpInfo();
        StringBuilder param = new StringBuilder();
        int i = 0;
        for (String key : params.keySet()) {
            param.append(i == 0 ? "?" : "&");
            param.append(key).append("=").append(params.get(key));
            i++;
        }
        byte[] postData = param.toString().getBytes();
        // 新建一个URL对象
        try {
            URL url = new URL(strUrl);
            // 打开一个HttpURLConnection连接
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            // 设置连接超时时间
            urlConn.setConnectTimeout(CONNECTION_TIMEOUT);
            // Post请求必须设置允许输出
            urlConn.setDoOutput(true);
            // Post请求不能使用缓存
            urlConn.setUseCaches(false);
            // 设置为Post请求
            urlConn.setRequestMethod("POST");
            urlConn.setInstanceFollowRedirects(true);
            // 配置请求Content-Type
            urlConn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencode");
            // 开始连接
            try {
                urlConn.connect();
                // 发送请求参数
                DataOutputStream dos = new DataOutputStream(urlConn.getOutputStream());
                dos.write(postData);
                dos.flush();
                dos.close();
                // 判断请求是否成功
                int responseCode = urlConn.getResponseCode();
                httpInfo.setHttpCode(responseCode);
                if (responseCode == 200) {
                    // 获取返回的数据
                    byte[] data = readStream(urlConn.getInputStream());
                    String resultStr = new String(data, "UTF-8");
                    httpInfo.setResult(resultStr);
                }
            } catch (IOException e) {
                e.printStackTrace();
                httpInfo.setHttpCode(HTTP_1001);
            }
            // 关闭连接
            urlConn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            httpInfo.setHttpCode(HTTP_1002);
        }
        println(httpInfo);
        return httpInfo;
    }

    /**
     * HttpGet方式请求
     *
     * @param url           请求地址
     * @param conn_time_out 链接超时
     * @param so_time_out   读取超时
     */
    private HttpInfo mHttpGet(String url, Map<String, Object> param, int conn_time_out, int so_time_out) {
        HttpInfo httpInfo = new HttpInfo();
        try {
            InputStream inputStream = _HttpGet(url, param, conn_time_out, so_time_out);
            if (inputStream != null) {
                byte[] bytes = readStream(inputStream);
                String result = new String(bytes, "utf-8");
                httpInfo.setResult(result);
            }
            httpInfo.setHttpCode(httpCode);
        } catch (IOException e) {
            e.printStackTrace();
            httpInfo.setHttpCode(HTTP_1002);
        }
        println(httpInfo);
        return httpInfo;
    }

    /**
     * HttpGet方式请求
     *
     * @param url           请求地址
     * @param conn_time_out 链接超时
     * @param so_time_out   读取超时
     */
    private InputStream _HttpGet(String url, Map<String, Object> param, int conn_time_out, int so_time_out) throws IOException {
        if (url != null)
            println(url);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        if (param != null)
            for (String key : param.keySet()) {
                sb.append(i == 0 ? "?" : "&");
                sb.append(key).append("=").append(param.get(key));
                i++;
            }
        url += sb;
        HttpGetHC4 httpGet = new HttpGetHC4(url);
        CloseableHttpClient httpClient = getDefaultHttpClient();

        //httpclient4.x --4.3
//        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, conn_time_out);// 请求超时
//        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, so_time_out); // 读取超时

        //httpclient >4.3
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(conn_time_out).setConnectTimeout(so_time_out)
                .setSocketTimeout(so_time_out).build();
        httpGet.setConfig(requestConfig);

        // 获取HttpResponse实例
        CloseableHttpResponse httpResp = httpClient.execute(httpGet);
        httpCode = httpResp.getStatusLine().getStatusCode();
        InputStream result = null;
        // 判断是够请求成功
        if (httpCode == 200) {
            // 获取返回的数据
            result = httpResp.getEntity().getContent();
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private List<NameValuePair> mapToPair(Map<String, Object> params) {
        if (params == null)
            return null;
        List<NameValuePair> pairList = new ArrayList<>();
        for (Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            NameValuePair pair;
            if (value instanceof String) {
                pair = new BasicNameValuePair(entry.getKey(), (String) value);
            } else {
                pair = new BasicNameValuePair(entry.getKey(), String.valueOf(value));
            }
            //NameValuePair pair = new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue()));
            pairList.add(pair);
        }
        return pairList;
    }

    /**
     * HttpPost方式请求
     *
     * @param url
     * @param params
     */
    public static HttpInfo mHttpPost(String url, Map<String, Object> params) {
        return getInstance().mHttpPost(url, params, CONNECTION_TIMEOUT, SO_TIMEOUT);
    }

    @Deprecated
    public static HttpInfo mHttpPost(String url, HashMap<String, String> params) {
        HashMap<String, Object> hs = new HashMap<>();
        for (Entry<String, String> entry : params.entrySet()) {
            hs.put(entry.getKey(), entry.getValue());
        }
        return getInstance().mHttpPost(url, hs, CONNECTION_TIMEOUT, SO_TIMEOUT);
    }

    /**
     * HttpPost方式请求
     *
     * @param url
     * @param params
     */
    private HttpInfo mHttpPost(String url, Map<String, Object> params, int conn_time_out, int so_time_out) {
        return _httpPost(url, params, conn_time_out, so_time_out);
    }

    /**
     * HttpPost方式请求
     *
     * @param url    1
     * @param params 1
     */
    @SuppressWarnings("deprecation")
    private HttpInfo _httpPost(String url, Map<String, Object> params, int conn_time_out, int so_time_out) {
        HttpInfo httpInfo = new HttpInfo();
        if (url != null) {
            println(url + "\n" + params.toString());
        } else {
            httpInfo.setHttpCode(404);
            return httpInfo;
        }
        HttpPostHC4 httpPost = new HttpPostHC4(url);
        try {
            // 设置字符集
            HttpEntity entity = null;
            try {
                entity = new UrlEncodedFormEntity(mapToPair(params), HTTP.UTF_8);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            // 设置参数实体
            httpPost.setEntity(entity);
            CloseableHttpClient httpClient = getDefaultHttpClient();// 由于服务器使用了session
            //httpclient4.x --4.3
//            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, conn_time_out);// 请求超时
//            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, so_time_out); // 读取超时

            // httpclient >4.3
            RequestConfig.Builder builder = RequestConfig.custom();
            builder.setConnectionRequestTimeout(conn_time_out);// 请求超时
            builder.setConnectTimeout(so_time_out);// 链接超时
            builder.setSocketTimeout(so_time_out);// 读取超时
            httpPost.setConfig(builder.build());

            // 获取HttpResponse实例
            CloseableHttpResponse httpResp = httpClient.execute(httpPost);
            int httpCode = httpResp.getStatusLine().getStatusCode();
            // 判断是够请求成功
            if (httpCode == 200) {
                // 获取返回的数据
                try {
                    InputStream is = httpResp.getEntity().getContent();
                    String result = InputStreamToString(is);
                    httpInfo.setResult(result);
                } catch (IOException e) {
                    e.printStackTrace();
                    httpInfo.setHttpCode(HTTP_1001);
                }
            }
            httpInfo.setHttpCode(httpCode);
        } catch (IOException e) {
            e.printStackTrace();
            httpInfo.setHttpCode(HTTP_1002);
        }
        println(httpInfo);
        return httpInfo;
    }

    private void println(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }

    private void println(final HttpInfo httpInfo) {
        if (listener != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    listener.onAsyncSuccess(httpInfo, 0);
                }
            }).start();
            if (httpInfo.getHttpCode() != 200) {
                listener.onPostError(HttpCodeType.getHttpMsg(httpInfo.getHttpCode()), httpInfo.getHttpCode(), 0);
            }
        }
        if (DEBUG) {
            System.out.println("httpCode:" + httpInfo.getHttpCode() + ";msg:" + HttpCodeType.getHttpMsg(httpInfo.getHttpCode()));
            if (httpInfo.getResult() != null) {
                String s = httpInfo.getResult().replaceAll("\\s", "");
                if (s.length() > LOG_NUMBER) {
                    System.out.println(s.substring(0, LOG_NUMBER) + "...");
                } else {
                    System.out.println(s);
                }
            }
        }
    }

    // 获取连接返回的数据
    private byte[] readStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        byte[] data = baos.toByteArray();
        inputStream.close();
        baos.close();
        return data;
    }

    private String InputStreamToString(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static List<com.god.http.Cookie> getCookie() {
        List<com.god.http.Cookie> list = new ArrayList<>();
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie _cookie : cookies) {
            com.god.http.Cookie cookie = new com.god.http.Cookie();
            cookie.setName(_cookie.getName());
            cookie.setValue(_cookie.getValue());
            cookie.setComment(_cookie.getComment());
            cookie.setCommentURL(_cookie.getCommentURL());
            cookie.setDomain(_cookie.getDomain());
            cookie.setPath(_cookie.getPath());
            cookie.setSecure(_cookie.isSecure());
            cookie.setVersion(_cookie.getVersion());
            list.add(cookie);
        }
        return list;
    }

    public static void setCookie(List<com.god.http.Cookie> cookies) {
        if (cookieStore == null)
            cookieStore = new BasicCookieStoreHC4();
        for (com.god.http.Cookie cookie : cookies) {
            if (cookie.getName() == null)
                break;
            BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
            basicClientCookie.setVersion(cookie.getVersion());
            basicClientCookie.setPath(cookie.getPath());
            basicClientCookie.setSecure(cookie.isSecure());
            basicClientCookie.setDomain(cookie.getDomain());
            basicClientCookie.setComment(cookie.getComment());
            cookieStore.addCookie(basicClientCookie);
        }
    }

//    public static Map<String, String> cookieMap = new HashMap<>(64);
//
//    //从响应信息中获取cookie
//    public static String setCookie(CloseableHttpResponse httpResponse) {
//        System.out.println("----setCookieStore");
//        Header headers[] = httpResponse.getHeaders("Set-Cookie");
//        if (headers == null || headers.length == 0) {
//            System.out.println("----there are no cookies");
//            return null;
//        }
//        String cookie = "";
//        for (int i = 0; i < headers.length; i++) {
//            cookie += headers[i].getValue();
//            if (i != headers.length - 1) {
//                cookie += ";";
//            }
//        }
//        String cookies[] = cookie.split(";");
//        for (String c : cookies) {
//            c = c.trim();
//            if (cookieMap.containsKey(c.split("=")[0])) {
//                cookieMap.remove(c.split("=")[0]);
//            }
//            cookieMap.put(c.split("=")[0], c.split("=").length == 1 ? "" : (c.split("=").length == 2 ? c.split("=")[1] : c.split("=", 2)[1]));
//        }
//        System.out.println("----setCookieStore success");
//        String cookiesTmp = "";
//        for (String key : cookieMap.keySet()) {
//            cookiesTmp += key + "=" + cookieMap.get(key) + ";";
//        }
//        return cookiesTmp.substring(0, cookiesTmp.length() - 2);
//    }

}