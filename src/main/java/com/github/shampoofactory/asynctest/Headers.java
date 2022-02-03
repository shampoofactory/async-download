package com.github.shampoofactory.asynctest;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.MessageHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Headers {

    private static final Logger log = LoggerFactory.getLogger(Headers.class);

    public static boolean acceptRangeBytes(MessageHeaders headers) {
        Header header = headers.getFirstHeader(HttpHeaders.ACCEPT_RANGES);
        if (header == null) {
            return false;
        }
        String value = header.getValue();
        if (value.equalsIgnoreCase("bytes")) {
            return true;
        }
        log.info("ACCEPT-RANGES: unknown value: {}", value);
        return false;
    }

    public static long contentLength(MessageHeaders headers) {
        Header header = headers.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
        if (header == null) {
            return -1;
        }
        String value = header.getValue();
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            log.warn("CONTENT-LENGTH: cannot parse value: {}", value);
            return -1;
        }
    }

    private Headers() {
    }
}
