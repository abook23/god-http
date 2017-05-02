package com.god.http;

import com.god.listener.UploadListener;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPostHC4;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by abook23 on 2015/8/6.
 * 图片上传带进度条
 */
public class UploadByMultipart {


    private static final String TAG = "UploadByMultipart";
    private UploadListener listener;

    public static UploadByMultipart newInstance() {
        return new UploadByMultipart();
    }

    public void setUploadListener(UploadListener listener) {
        this.listener = listener;
    }
    /**
     * HttpPost方式请求
     *
     * @param url
     * @param mapParams
     * @return
     */
    public String uploadFile(String url, Map<String, String> mapParams, Map<String, File> files) throws Exception {
        if (url == null) {
            throw new Exception("url is null");
        }

        List<NameValuePair> params = new ArrayList<>();
        for (Map.Entry<String,String> entry:mapParams.entrySet()){
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return uploadFile(url,params,files);
    }

    public String uploadFile(String url,File... files) throws Exception {
        return uploadFile(url,null,files);
    }

    public String uploadFile(String url, Map<String, String> mapParams, File... files) throws Exception {
        if (url == null) {
            throw new Exception("url is null");
        }
        List<NameValuePair> params = new ArrayList<>();
        if (mapParams!=null){
            Iterator<Map.Entry<String, String>> iter = mapParams.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                params.add(new BasicNameValuePair(entry.getKey(),entry.getValue()));
            }
        }
        Map<String, File> fileMap = new HashMap<>();
        for (File file:files){
            fileMap.put(file.getName(),file);
        }
        return uploadFile(url,params,fileMap);
    }

    /**
     * @param url
     * @param files
     * @return
     * @throws IOException
     */
    public String uploadFile(String url, List<NameValuePair> params, Map<String, File> files) throws Exception {
        if (url == null) {
            throw new Exception("url is null");
        }
        CloseableHttpClient httpClient = HttpUtils.getInstance().getDefaultHttpClient();
        HttpPostHC4 post = new HttpPostHC4(url);
        HttpEntity entity = makeMultipartEntity(params, files);
        if (listener != null)
            new HttpOutProgressEntity(entity, listener);
        post.setEntity(entity);
        CloseableHttpResponse response = httpClient.execute(post);
        if (response.getStatusLine().getStatusCode() == 200) {
            String serverResponse = EntityUtils.toString(response.getEntity());
            if (listener != null)
                listener.uploadSuccess(serverResponse);
            return serverResponse;
        }
        return response.getStatusLine().getStatusCode() + "";
    }

    private HttpEntity makeMultipartEntity(List<NameValuePair> params, Map<String, File> files) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);  //如果有SocketTimeoutException等情况，可修改这个枚举
        //builder.setCharset(Charset.forName("UTF-8")); //不要用这个，会导致服务端接收不到参数
        if (params != null && params.size() > 0) {
            for (NameValuePair p : params) {
                builder.addTextBody(p.getName(), p.getValue(), ContentType.TEXT_PLAIN.withCharset("UTF-8"));
            }
        }
        if (files != null && files.size() > 0) {
            for (Map.Entry<String, File> entry : files.entrySet()) {
                builder.addPart(entry.getKey(), new FileBody(entry.getValue()));
            }
        }
        return builder.build();
    }
}
