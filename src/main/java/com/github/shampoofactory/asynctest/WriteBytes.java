package com.github.shampoofactory.asynctest;

import java.io.Closeable;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Write a sequence of bytes from the given buffer. Thread safe.
 */
public interface WriteBytes extends Closeable {

    default void writeAll(ByteBuffer src, long position) throws IOException {
        boolean retry = false;
        while (src.hasRemaining()) {
            int n = write(src, position);
            position += n;
            if (n == 0) {
                if (retry) {
                    throw new IOException("unable to write bytes");
                } else {
                    retry = true;
                }
            } else {
                retry = false;
            }
        }
    }

    int write(ByteBuffer src, long position) throws IOException;
}
