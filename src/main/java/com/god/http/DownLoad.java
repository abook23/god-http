package com.god.http;

import com.god.listener.DownloadListener;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGetHC4;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by abook23 on 2016/8/11.
 * E-mail abook23@163.com
 */
public class DownLoad {
    private boolean pause;
    private boolean cancel;

    private int POOL_SIZE = 2;
    private int cpuNum = Runtime.getRuntime().availableProcessors();// 获取当前系统的CPU 数目
    private int nThreads = POOL_SIZE * cpuNum + 1;//ExecutorService
    private ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
    private DownloadListener downloadListener;

    private void setPause(boolean pause) {
        this.pause = pause;
    }

    private boolean isCancel() {
        return cancel;
    }

    private void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    /**
     * 获取线程池的方法，因为涉及到并发问题
     *
     * @return
     */
    private ExecutorService getThreadPool() {
        if (executorService == null) {
            synchronized (ExecutorService.class) {
                if (executorService == null) {
                    executorService = Executors.newFixedThreadPool(nThreads);
                }
            }
        }
        return executorService;
    }

    /**
     * 取消 队列中的任务真正现在的线程 无法终止
     */
    public synchronized void cancelTask() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
            executorService = getThreadPool();

        }
    }

    public boolean isPause() {
        return pause;
    }

    public void pause() {
        setPause(true);
    }

    public void cancel() {
        setCancel(true);
    }

    public void resume() {
        setPause(false);
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    public void down(final String url, final String savePath) {
        getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                downFile(url, savePath, false);
            }
        });
    }

    public void down(final String url, final String savePath, final boolean overlap) {
        getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                downFile(url, savePath, overlap);
            }
        });
    }

    public void down(final String url, final String savePath, final boolean overlap, DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
        getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                downFile(url, savePath, overlap);
            }
        });
    }

    /**
     * fileUtils.getRootPath() + "/Download"
     *
     * @param url
     */
    private void downFile(String url, String savePath, boolean overlap) {
        try {
            HttpGetHC4 httpGet = new HttpGetHC4(url);
            CloseableHttpClient httpClient = HttpUtils.getInstance().getDefaultHttpClient();
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {

                long length = httpResponse.getEntity().getContentLength();
                int index = savePath.lastIndexOf("/");
                createFile(savePath.substring(0, index), savePath.substring(index + 1));
                File file = new File(savePath);
                if (!overlap) {//文件以及存在
                    if (length == file.length()) {
                        if (!httpGet.isAborted()) {
                            httpGet.abort();//终止链接，很重要 不然 is.close 很难断开，浪费流量
                        }
                        if (downloadListener != null) {
                            downloadListener.onSuccess(file);
                        }
                        return;
                    } else {
                        file.delete();
                    }
                }
                if (downloadListener != null)
                    downloadListener.onStart(length);//开始下载
                InputStream is = httpResponse.getEntity().getContent();
                OutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                    byte buffer[] = new byte[1024];
                    int ln = -1;
                    int readSize = 0;
                    label:
                    while (!isCancel()) {
                        if (isCancel()) {
                            setCancel(true);
                            if (downloadListener != null) {
                                downloadListener.onCancel();
                            }
                        }
                        if (isPause()) {
                            if (downloadListener != null) {
                                downloadListener.onPause();
                            }
                            try {
                                Thread.sleep(1000);//再停休眠1s
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        while (!isPause() && (ln = is.read(buffer)) != -1) {
                            os.write(buffer, 0, ln);
                            readSize += ln;
                            if (downloadListener != null) {
                                downloadListener.onSize(readSize, length);
                                if (readSize == length) {//下载完成
                                    downloadListener.onSuccess(file);
                                    break label;
                                }
                            }
                            if (isCancel()) {//取消
                                if (downloadListener != null) {
                                    downloadListener.onCancel();
                                }
                                break label;
                            }
                        }
                        if (ln == -1) {
                            downloadListener.onSuccess(file);
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace(); // To change body of catch statement use File |
                    if (downloadListener != null)
                        downloadListener.onFail();
                } finally {
                    httpClient.close();
//                    if (!httpGet.isAborted()) {//普通httpClient
//                        httpGet.abort();//终止链接，很重要 不然 is.close 很难断开，浪费流量
//                    }
                    if (os != null) {
                        os.flush();
                        os.close();
                    }
                    if (is != null) {//建议先终止 http 链接在 断开
                        try {
                            is.close();
                        } catch (IOException e) {
                            //e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 原始的 http 链接  很难以终止
     * 推荐用 httpClient
     *
     * @param url      下载地址
     * @param savePath 保存路径
     */
    @Deprecated
    public void downFileForURLConnection(String url, String savePath) {
        try {
            HttpURLConnection urlCon = (HttpURLConnection) new URL(url).openConnection();
            urlCon.setDoInput(true);
            urlCon.setUseCaches(false);
            urlCon.setRequestMethod("GET");
            urlCon.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=utf-8");
            urlCon.connect();// 建立连接
            long length = urlCon.getContentLength();
            if (urlCon.getResponseCode() == 200) {
                downloadListener.onStart(length);//开始下载
                InputStream is = null;
                OutputStream os = null;
                try {
                    is = urlCon.getInputStream();
                    int index = savePath.lastIndexOf("/");
                    createFile(savePath.substring(0, index), savePath.substring(index + 1));
                    File file = new File(savePath);
                    os = new FileOutputStream(file);
                    byte buffer[] = new byte[1024];
                    int ln;
                    int readSize = 0;
                    label:
                    while (!isCancel()) {
                        if (isCancel()) {
                            setCancel(true);
                            if (downloadListener != null) {
                                downloadListener.onCancel();
                            }
                        }
                        if (isPause()) {
                            if (downloadListener != null) {
                                downloadListener.onPause();
                                try {
                                    Thread.sleep(1000);//再停休眠500ms
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        while (!isPause() && (ln = is.read(buffer)) != -1) {
                            os.write(buffer, 0, ln);
                            readSize += ln;
                            if (downloadListener != null) {
                                downloadListener.onSize(readSize, length);
                                if (readSize == length) {//下载完成
                                    downloadListener.onSuccess(file);
                                }
                            }
                            if (isCancel()) {//取消
                                file.delete();
                                if (downloadListener != null) {
                                    downloadListener.onCancel();
                                }
                                break label;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace(); // To change body of catch statement use File |
                } finally {
                    if (os != null) {
                        os.flush();
                        os.close();
                    }
                    if (!isCancel()) {
                        if (is != null) {
                            //下载到一半，取消下载难以断开，对手机用户来说，浪费流量
                            is.close();
                            urlCon.disconnect();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 创建目录
     */
    private File createDir(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return file;
            } else {
                throw new IOException("创建文件夹失败");
            }
        }
        return file;
    }

    /**
     * 在ＳＤ卡上 项目根目录 创建文件
     *
     * @param fileName 文件名
     * @return
     * @throws IOException
     */
    public File createFile(String dirsName, String fileName) throws IOException {
        File dir = createDir(dirsName);//创建文件夹
        if (dir.exists()) {
            File file = new File(dir.getPath() + File.separator + fileName);
            if (!file.exists()) {
                if (file.createNewFile()) {//创建文件
                    return file;
                } else {
                    throw new IOException("创建文件失败");
                }
            } else {
                return file;
            }
        }
        return null;
    }
}

