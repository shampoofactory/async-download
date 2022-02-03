package com.github.shampoofactory.asynctest;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hc.client5.http.async.HttpAsyncClient;

/**
 *
 * @author vin
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        log.info("args: {}", (Object) args);
        if (args.length != 2) {
            System.err.println("invalid arguments supplied");
            System.err.println("usage: AsyncDownload URL FILE");
            System.exit(1);
        }
        String uri = args[0];
        String fileName = args[1];
        if (Files.exists(Paths.get(fileName))) {
            System.err.println("file already exists: " + fileName);
            System.exit(1);
        }
        Param param = Param.builder().build();
        try (CloseableHttpAsyncClient client = Clients.create(param.maxConcurrent())) {
            download(client, param, uri, fileName);
        }
    }

    static void download(HttpAsyncClient client, Param param, String uri, String fileName)
            throws ExecutionException, InterruptedException, IOException {
        try (RandomAccessFile file = new RandomAccessFile(fileName, "rw")) {
            OutputSupplier<FileChannel> channelSupplier = OutputSupplier.withFileChannel(file.getChannel());
            Job.execute(param, uri, client, channelSupplier);
        }
    }
}
