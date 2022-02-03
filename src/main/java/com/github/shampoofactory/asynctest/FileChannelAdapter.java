package com.github.shampoofactory.asynctest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelAdapter implements Output<FileChannel> {

    private final FileChannel channel;

    public FileChannelAdapter(FileChannel channel) {
        this.channel = channel;
    }

    @Override
    public FileChannel into() {
        return channel;
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        return channel.write(src, position);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public String toString() {
        return "FileChannelAdapter{" + "channel=" + channel + '}';
    }
}
