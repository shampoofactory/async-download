package com.github.shampoofactory.asynctest;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Write a sequence of bytes from the given buffer. Thread safe.
 */
public interface WriteBytes extends Closeable {

    /**
     * Write all the specified bytes.
     * 
     * @param src
     * @param position
     * @throws IOException
     */
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

    /**
     * Write specified bytes returning the number of bytes written, which may
     * be zero.
     * 
     * @param src
     * @param position
     * @return
     * @throws IOException
     */
    int write(ByteBuffer src, long position) throws IOException;
}
