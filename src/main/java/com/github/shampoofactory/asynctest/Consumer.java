package com.github.shampoofactory.asynctest;

import org.apache.hc.client5.http.async.methods.AbstractBinResponseConsumer;
import org.apache.hc.core5.http.*;

import java.io.IOException;
import java.nio.ByteBuffer;

class Consumer extends AbstractBinResponseConsumer<Void> {

    public static Consumer createGet(WriteBytes channel, long pos, long len) throws IOException {
        if (pos < 0) {
            throw new IllegalArgumentException();
        }
        final long cap;
        if (len < -1) {
            throw new IllegalArgumentException();
        } else if (len == -1) {
            cap = Long.MAX_VALUE;
        } else {
            cap = len;
        }
        return new Consumer(channel, 200, pos, len, pos, cap);
    }

    public static Consumer createPart(WriteBytes channel, long pos, long len) throws IOException {
        if (pos < 0) {
            throw new IllegalArgumentException();
        }
        if (len < 0) {
            throw new IllegalArgumentException();
        }
        return new Consumer(channel, 206, pos, len, pos, len);
    }

    private final WriteBytes channel;
    private final int statusCode;
    private final long mark;
    private final long len;
    private long pos;
    private long cap;

    private Consumer(WriteBytes channel, int statusCode, long mark, long len, long pos, long cap) {
        this.statusCode = statusCode;
        this.channel = channel;
        this.mark = mark;
        this.len = len;
        this.pos = pos;
        this.cap = cap;
    }

    public long mark() {
        return mark;
    }

    public long len() {
        return len;
    }

    @Override
    protected void start(HttpResponse response, ContentType contentType) throws HttpException, IOException {
        int code = response.getCode();
        if (code != statusCode) {
            throw new HttpException("server returned unexpected status code: " + code);
        }
        if (contentType == null) {
            throw new HttpException("server returned no data");
        }
    }

    @Override
    protected Void buildResult() {
        return null;
    }

    @Override
    protected int capacityIncrement() {
        if (cap > Integer.MAX_VALUE) {
            cap -= Integer.MAX_VALUE;
            return Integer.MAX_VALUE;
        } else {
            int oldCap = (int) cap;
            cap = 0;
            return oldCap;
        }
    }

    @Override
    protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
        long newPos = pos + src.remaining();
        if (len != -1 && newPos - mark > len) {
            throw new IOException("server returned too much data");
        }
        if (len != - 1 && endOfStream && newPos - mark != len) {
            throw new IOException("server returned insufficient data");
        }
        channel.writeAll(src, pos);
        pos = newPos;

    }

    @Override
    public void releaseResources() {
    }

    @Override
    public String toString() {
        return "Consumer{"
                + "channel=" + channel
                + ", statusCode=" + statusCode
                + ", mark=" + mark
                + ", len=" + len
                + ", pos=" + pos
                + ", cap=" + cap
                + '}';
    }
}
