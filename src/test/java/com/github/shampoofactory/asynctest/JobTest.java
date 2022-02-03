package com.github.shampoofactory.asynctest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobTest {

    private static final Logger log = LoggerFactory.getLogger(JobTest.class);

    static CloseableHttpAsyncClient client;

    public JobTest() {
    }

    @BeforeAll
    public static void setUpClass() {
        client = Clients.create();
    }

    @AfterAll
    public static void tearDownClass() {
        try {
            client.close();
        } catch (IOException ex) {
            log.error("close error: {}", ex);
        }
    }

    @Test
    public void testExecute10B() throws Exception {
        String string = "0123456789";
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        String encoded = Base64.getEncoder().encodeToString(bytes);
        String uri = "https://httpbin.org/base64/" + encoded;
        Param param = Param.builder().build();
        OutputSupplier<ByteBuffer> channelSupplier = OutputSupplier.withByteBuffer();
        ByteBuffer buffer = Job.execute(param, uri, client, channelSupplier);
        ByteBuffer expected = ByteBuffer.wrap(bytes);
        assertEquals(expected, buffer);
    }

    @Test
    public void testExecute10MB() throws Exception {
        // N.B. Website only supplies MD5.
        String uri = "http://ipv4.download.thinkbroadband.com/10MB.zip";
        String md5 = "3aa55f03c298b83cd7708e90d289afbd";
        Param param = Param.builder().build();
        OutputSupplier<ByteBuffer> channelSupplier = OutputSupplier.withByteBuffer();
        ByteBuffer buffer = Job.execute(param, uri, client, channelSupplier);
        byte[] hash = Hash.get(buffer, "MD5");
        byte[] expected = Hex.decodeHex(md5);
        assertArrayEquals(expected, hash);
    }
}
