package com.github.shampoofactory.asynctest;

import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.apache.hc.core5.http.nio.support.BasicResponseConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author vin
 * @param <T>
 */
public final class Job<T> {

    public static <T> T execute(
            Param param,
            String uri,
            HttpAsyncClient client,
            OutputSupplier<T> supplier) throws ExecutionException, InterruptedException, IOException {
        Job<T> job = new Job<>(param, uri, client, supplier);
        return job.execute();
    }

    public static <T> T execute(
            String uri,
            HttpAsyncClient client,
            OutputSupplier<T> supplier) throws ExecutionException, InterruptedException, IOException {
        return execute(Param.builder().build(), uri, client, supplier);
    }

    private static final Logger log = LoggerFactory.getLogger(Job.class);

    private final Param param;
    private final String uri;
    private final HttpAsyncClient client;
    private final OutputSupplier<T> supplier;

    private Job(
            Param param,
            String uri,
            HttpAsyncClient client,
            OutputSupplier<T> supplier) {
        this.param = param;
        this.uri = uri;
        this.client = client;
        this.supplier = supplier;
    }

    T execute() throws ExecutionException, InterruptedException, IOException {
        HttpResponse head = head();
        long contentLength = Headers.contentLength(head);
        log.info("CONTENT-LENGTH: {}", contentLength);
        boolean acceptRangeBytes = Headers.acceptRangeBytes(head);
        log.info("ACCEPT-RANGES bytes: {}", acceptRangeBytes);
        Output<T> channel = supplier.output(contentLength);
        try {
            final List<Future<Void>> futures;
            if (param.maxConcurrent() > 1 && acceptRangeBytes && contentLength >= param.minLen()) {
                futures = queuePart(channel, contentLength);
            } else {
                futures = queueGet(channel, contentLength);
            }
            try {
                for (Future<Void> future : futures) {
                    future.get();
                }
            } finally {
                for (Future<Void> future : futures) {
                    future.cancel(true);
                }
            }
        } finally {
            channel.close();
        }
        return channel.into();
    }

    HttpResponse head() throws ExecutionException, InterruptedException {
        SimpleHttpRequest request = SimpleRequestBuilder.head(uri).build();
        SimpleRequestProducer requestProducer = SimpleRequestProducer.create(request);
        BasicResponseConsumer<Void> responseConsumer = new BasicResponseConsumer<>(NoopEntityConsumer::new);
        HttpClientContext context = HttpClientContext.create();
        return client.execute(requestProducer, responseConsumer, null, context, null)
                .get()
                .getHead();
    }

    List<Future<Void>> queueGet(WriteBytes channel, long len)
            throws ExecutionException, InterruptedException, IOException {
        if (len == 0) {
            return Collections.emptyList();
        }
        ArrayList<Future<Void>> futures = new ArrayList<>(1);
        Consumer consumer = Consumer.createGet(channel, 0, len);
        log.info("queue get: 0-{}", len);
        SimpleHttpRequest request = SimpleRequestBuilder.get(uri).build();
        SimpleRequestProducer producer = SimpleRequestProducer.create(request);
        HttpClientContext context = HttpClientContext.create();
        Future<Void> future = client.execute(producer, consumer, null, context, null);
        futures.add(future);
        return futures;
    }

    List<Future<Void>> queuePart(WriteBytes channel, long len)
            throws ExecutionException, InterruptedException, IOException {
        if (len == 0) {
            return Collections.emptyList();
        }
        Consumer[] consumers = queuePartConsumers(channel, len);
        for (Consumer consumer : consumers) {
            if (consumer != null) {
                log.info("queue part: {}-{}", consumer.mark(), consumer.mark() + consumer.len());
            }
        }
        return queuePartFutures(consumers);
    }

    Consumer[] queuePartConsumers(WriteBytes channel, long len) throws IOException {
        int nThreads = param.maxConcurrent();
        int chunkLen = param.chunkLen();
        long nChunks = len / chunkLen;
        long trim = len - nChunks * chunkLen;
        long div = nChunks / nThreads;
        long mod = nChunks % nThreads;
        Consumer[] consumers = new Consumer[param.maxConcurrent()];
        long pos = 0;
        for (int i = 0; i < nThreads; i++) {
            long partLen = div * chunkLen;
            if (mod != 0) {
                partLen += chunkLen;
                mod -= 1;
            }
            if (i == nThreads - 1) {
                partLen += trim;
            }
            if (partLen == 0) {
                continue;
            }
            consumers[i] = Consumer.createPart(channel, pos, partLen);
            pos += partLen;
        }
        return consumers;
    }

    List<Future<Void>> queuePartFutures(Consumer[] consumers)
            throws ExecutionException, InterruptedException, IOException {
        ArrayList<Future<Void>> futures = new ArrayList<>(consumers.length);
        for (Consumer consumer : consumers) {
            long head = consumer.mark();
            long tail = head + consumer.len() - 1;
            SimpleHttpRequest request = SimpleRequestBuilder.get(uri)
                    .addHeader(HttpHeaders.RANGE, "bytes=" + head + "-" + tail)
                    .build();
            SimpleRequestProducer producer = SimpleRequestProducer.create(request);
            HttpClientContext context = HttpClientContext.create();
            Future<Void> future = client.execute(producer, consumer, null, context, null);
            futures.add(future);
        }
        return futures;
    }

    @Override
    public String toString() {
        return "Job{"
                + "param=" + param
                + ", uri=" + uri
                + ", client=" + client
                + ", supplier=" + supplier
                + '}';
    }
}
