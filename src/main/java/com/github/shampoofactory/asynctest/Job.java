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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.core5.concurrent.FutureCallback;

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
    private final ConcurrentHashMap<Integer, Future<Void>> map;
    private final AtomicReference<ExecutionException> error;
    private CountDownLatch latch;
    private Output<T> channel;

    private Job(
            Param param,
            String uri,
            HttpAsyncClient client,
            OutputSupplier<T> supplier) {
        this.param = param;
        this.uri = uri;
        this.client = client;
        this.supplier = supplier;
        this.map = new ConcurrentHashMap<>(param.maxConcurrent());
        this.error = new AtomicReference<>();
    }

    T execute() throws ExecutionException, InterruptedException, IOException {
        HttpResponse head = head();
        long contentLength = Headers.contentLength(head);
        log.info("CONTENT-LENGTH: {}", contentLength);
        boolean acceptRangeBytes = Headers.acceptRangeBytes(head);
        log.info("ACCEPT-RANGES bytes: {}", acceptRangeBytes);
        channel = supplier.output(contentLength);
        try {
            if (param.maxConcurrent() > 1 && acceptRangeBytes && contentLength >= param.minLen()) {
                queuePart(channel, contentLength);
            } else {
                queueGet(channel, contentLength);
            }
            latch.await();
        } finally {
            map.forEach((id, future) -> future.cancel(true));
            channel.close();
        }
        ExecutionException ex = error.get();
        if (ex != null) {
            throw ex;
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

    void queueGet(WriteBytes channel, long len) {
        latch = new CountDownLatch(1);
        if (len == 0) {
            latch.countDown();
            return;
        }
        Consumer consumer = Consumer.createGet(channel, 0, len);
        log.info("queue get: 0-{}", len);
        SimpleHttpRequest request = SimpleRequestBuilder.get(uri).build();
        SimpleRequestProducer producer = SimpleRequestProducer.create(request);
        HttpClientContext context = HttpClientContext.create();
        Future<Void> future = client.execute(producer, consumer, null, context,
                new FutureCallback<Void>() {
            @Override
            public void completed(Void result) {
                log.info("get: completed: {}", consumer.position());
                latch.countDown();
            }

            @Override
            public void failed(Exception ex) {
                log.warn("get: failed: {}", ex);
                abort(ex);
            }

            @Override
            public void cancelled() {
                log.debug("get: interrupted");
                latch.countDown();
            }
        });
        map.put(0, future);
    }

    void queuePart(WriteBytes channel, long len) {
        List<Split> list = param.split(len);
        log.debug("split list: {}", list);
        latch = new CountDownLatch(list.size());
        list.forEach(split -> queuePartFuture(split, param.retryCount()));
    }

    synchronized void queuePartFuture(Split split, int retry) {
        if (split.length() == 0) {
            latch.countDown();
            return;
        }
        final int id = split.id();
        final long head = split.head();
        final long tail;
        if (split.length() > param.maxLen()) {
            tail = head + param.maxLen();
        } else {
            tail = split.tail();
        }
        log.info("queue part: {} to: {}", split, tail);
        SimpleHttpRequest request = SimpleRequestBuilder.get(uri)
                .addHeader(HttpHeaders.RANGE, "bytes=" + head + "-" + (tail - 1))
                .build();
        SimpleRequestProducer producer = SimpleRequestProducer.create(request);
        Consumer consumer = Consumer.createPart(channel, head, tail);
        HttpClientContext context = HttpClientContext.create();
        Future<Void> future = client.execute(producer, consumer, null, context,
                new FutureCallback<Void>() {
            @Override
            public void completed(Void result) {
                if (tail == split.tail()) {
                    log.info("{}: completed: {}-{}", id, head, tail);
                    latch.countDown();
                    return;
                }
                log.debug("{}: continue: {}-{}", id, head, tail);
                queuePartFuture(split.cut(tail), retry - 1);
            }

            @Override
            public void failed(Exception ex) {
                if (!(ex instanceof IOException)) {
                    log.warn("{}: failed with error: {}", id, ex);
                    abort(ex);
                    return;
                }
                if (retry == 0) {
                    log.warn("{}: failed: {}", id, ex.toString());
                    abort(ex);
                    return;
                }
                try {
                    Thread.sleep(param.retryDelay());
                } catch (InterruptedException ex1) {
                    log.warn("{}: failed interruped: {}", id, ex.toString());
                    latch.countDown();
                    return;
                }
                log.warn("{}: failed will retry: {}", id, ex.toString());
                queuePartFuture(split.cut(consumer.position()), retry - 1);
            }

            @Override
            public void cancelled() {
                log.debug("{}: interrupted", id);
                latch.countDown();
            }
        });
        map.put(id, future);
    }

    void abort(Throwable cause) {
        error.set(new ExecutionException(cause));
        for (long i = 0, m = latch.getCount(); i < m; i++) {
            latch.countDown();
        }
    }

    @Override
    public String toString() {
        return "Job{"
                + "param=" + param
                + ", uri=" + uri
                + ", client=" + client
                + ", supplier=" + supplier
                + ", map=" + map
                + ", latch=" + latch
                + ", channel=" + channel
                + '}';
    }
}
