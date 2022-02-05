package com.github.shampoofactory.asynctest;

import org.apache.hc.client5.http.async.methods.AbstractBinResponseConsumer;
import org.apache.hc.core5.http.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Consumer extends AbstractBinResponseConsumer<Void> {

    public static Consumer createGet(WriteBytes channel, long head, long tail) {
        if (head < 0) {
            throw new IllegalArgumentException();
        }
        if (tail == -1) {
            tail = Long.MAX_VALUE;
        } else if (tail <= head || tail == Long.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        log.debug("createGet: head: {} tail: {}", head, tail);
        return new Consumer(channel, 200, head, tail);
    }

    public static Consumer createPart(WriteBytes channel, long head, long tail) {
        if (head < 0) {
            throw new IllegalArgumentException();
        }
        if (tail <= head) {
            throw new IllegalArgumentException();
        }
        log.debug("createPart: head: {} tail: {}", head, tail);
        return new Consumer(channel, 206, head, tail);
    }

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);

    private final WriteBytes channel;
    private final int statusCode;
    private final long head;
    private final long tail; // -1 if tail unknown
    private long position;

    private Consumer(WriteBytes channel, int statusCode, long head, long tail) {
        this.channel = channel;
        this.statusCode = statusCode;
        this.head = head;
        this.tail = tail;
        this.position = head;
    }

    public long position() {
        return position;
    }

    @Override
    protected void start(HttpResponse response, ContentType contentType) throws HttpException, IOException {
        log.debug("start: {} {}", response, contentType);
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
        log.debug("buildResult");
        return null;
    }

    @Override
    protected int capacityIncrement() {
        log.debug("capacityIncrement");
        return Integer.MAX_VALUE;
    }

    @Override
    protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
        log.debug("data: length: {} eos: {} position: {}", src.remaining(), endOfStream, position);
        long newPosition = position + src.remaining();
        if (newPosition > tail) {
            throw new IOException("server returned too much data");
        }
        if (endOfStream && tail != Long.MAX_VALUE && newPosition < tail) {
            throw new IOException("server returned insufficient data");
        }
        channel.writeAll(src, position);
        position = newPosition;

    }

    @Override
    public void releaseResources() {
    }

    @Override
    public String toString() {
        return "Consumer{"
                + "channel=" + channel
                + ", statusCode=" + statusCode
                + ", head=" + head
                + ", tail=" + tail
                + ", position=" + position
                + '}';
    }
}
