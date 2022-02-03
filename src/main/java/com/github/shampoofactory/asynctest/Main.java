package com.github.shampoofactory.asynctest;

import java.io.IOException;
import java.io.RandomAccessFile;
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
        if (args.length != 2) {
            System.err.println("invalid arguments supplied");
            System.err.println("usage: AsyncDownload URL FILE");
            System.exit(1);
        }
        try (CloseableHttpAsyncClient client = Clients.create()) {
            download(client, args[0], args[1]);
        }
    }

    static void download(HttpAsyncClient client, String uri, String fileName)
            throws ExecutionException, InterruptedException, IOException {
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        OutputSupplier channelSupplier = OutputSupplier.withFileChannel(file.getChannel());
        Job.execute(uri, client, channelSupplier);
    }
}