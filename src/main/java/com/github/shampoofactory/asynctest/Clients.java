package com.github.shampoofactory.asynctest;

import javax.net.ssl.SSLEngine;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.function.Factory;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.apache.hc.core5.util.Timeout;

final class Clients {
    
    static CloseableHttpAsyncClient create(int nConn) {
        TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .useSystemProperties()
                // IMPORTANT uncomment the following method when running Java 9 or older
                // in order for ALPN support to work and avoid the illegal reflective
                // access operation warning
                // <method>
                .setTlsDetailsFactory(new Factory<SSLEngine, TlsDetails>() {
                    @Override
                    public TlsDetails create(final SSLEngine sslEngine) {
                        return new TlsDetails(sslEngine.getSession(),
                                sslEngine.getApplicationProtocol());
                    }
                })
                // </method>
                .build();
        PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(tlsStrategy)
                .setMaxConnPerRoute(nConn)
                .setMaxConnTotal(nConn * 2)
                .build();
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(30))
                .build();
        CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)
                .setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
                .setConnectionManager(cm)
                .useSystemProperties()
                .build();
        client.start();
        return client;
    }
    
    private Clients() {
    }
}
