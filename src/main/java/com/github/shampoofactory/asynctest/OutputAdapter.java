package com.github.shampoofactory.asynctest;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 
 * @author vin
 * @param <T>
 */
public final class OutputAdapter<T> implements Output<T> {

    private final T t;
    private final WriteBytes writeBytes;

    public OutputAdapter(T t, WriteBytes writeBytes) {
        this.t = t;
        this.writeBytes = writeBytes;
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        return writeBytes.write(src, position);
    }

    @Override
    public void close() throws IOException {
        writeBytes.close();
    }

    @Override
    public T into() {
        return t;
    }

    @Override
    public String toString() {
        return "OutputAdapter{" + "t=" + t + ", writeBytes=" + writeBytes + '}';
    }
}
