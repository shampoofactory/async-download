package com.github.shampoofactory.asynctest;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Output supplier.
 * 
 * @author vin
 * @param <T>
 */
@FunctionalInterface
public interface OutputSupplier<T> {

    static OutputSupplier<FileChannel> withFileChannel(FileChannel channel) {
        return len -> new FileChannelAdapter(channel);
    }

    static OutputSupplier<ByteBuffer> withByteBuffer() {
        return len -> {
            if (len == -1 || len > (long) Integer.MAX_VALUE) {
                throw new BufferOverflowException();
            }
            return new ByteBufferAdapter((int) len);
        };
    }

    Output<T> output(long len) throws IOException;
}
