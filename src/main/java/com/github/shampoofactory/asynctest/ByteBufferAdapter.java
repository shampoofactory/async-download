package com.github.shampoofactory.asynctest;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ByteBufferAdapter implements Output<ByteBuffer> {

    private final ByteBuffer buffer;
    private final Object lock;
    private boolean isClosed;

    public ByteBufferAdapter(int allocate) {
        this.buffer = ByteBuffer.allocate(allocate);
        this.lock = new Object();
        this.isClosed = false;
    }

    @Override
    public ByteBuffer into() {
        synchronized (lock) {
            if (!isClosed) {
                throw new IllegalStateException("ByteBuffer not closed");
            }
            buffer.rewind();
            return buffer;
        }
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        synchronized (lock) {
            if (isClosed) {
                throw new IllegalStateException("is closed");
            }
            if (position > Integer.MAX_VALUE) {
                throw new BufferOverflowException();
            }
            int len = src.remaining();
            buffer.position((int) position);
            buffer.put(src);
            return len;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            isClosed = true;
        }
    }

    @Override
    public String toString() {
        return "ByteBufferAdapter{" + "buffer=" + buffer + ", isClosed=" + isClosed + '}';
    }
}
