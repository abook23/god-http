package com.god.http;

import com.god.listener.UploadListener;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapperHC4;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by abook23 on 2015/8/6.
 */
public class HttpOutProgressEntity extends HttpEntityWrapperHC4 {
    private UploadListener listener;

    public HttpOutProgressEntity(HttpEntity wrapped, UploadListener listener) {
        super(wrapped);
        this.listener = listener;
    }

    @Override
    public void writeTo(final OutputStream outputStream) throws IOException {
        if (listener != null) {
            listener.contentLength(getContentLength());
            super.writeTo(new CountingOutputStream(outputStream, this.listener));
        } else {
            super.writeTo(outputStream);
        }

    }

    public static class CountingOutputStream extends FilterOutputStream {

        private final UploadListener listener;
        private long transferred;

        public CountingOutputStream(final OutputStream out, final UploadListener listener) {
            super(out);
            this.listener = listener;
            this.transferred = 0;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            this.transferred += len;
            this.listener.transferred(this.transferred);
        }

        public void write(int b) throws IOException {
            out.write(b);
            this.transferred++;
            this.listener.transferred(this.transferred);
        }
    }
}
