package com.atbug.demo.client;

import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.io.InputStream;

@SpringBootApplication
public class DemoClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoClientApplication.class, args);
    }

    @RestController
    public static class HiController {
        private final HttpClient httpClient;
        private final String serverHost;

        public HiController(@Value("${demo.server.host:https://localhost:8080}") String serverHost,
                            @Value("${demo.client.wiretap:false}") boolean wiretap) throws SSLException {
            ConnectionProvider provider = ConnectionProvider.create("default", 10);
            try {
                HttpClient baseClient = HttpClient.create(provider);
                try (InputStream caCertStream = getClass().getClassLoader().getResourceAsStream("demo-root-ca.crt")) {
                    if (caCertStream != null) { // Load CA certificate if available
                        baseClient = baseClient.secure(spec -> {
                            try {
                                spec.sslContext(SslContextBuilder.forClient().trustManager(caCertStream).build());
                            } catch (SSLException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
                if (wiretap) { // Enable wiretap for debugging
                    baseClient = baseClient.wiretap(true);
                }
                String proxyEnv = System.getenv("HTTPS_PROXY");
                if (proxyEnv == null) proxyEnv = System.getenv("HTTP_PROXY");
                if (proxyEnv != null && proxyEnv.startsWith("http")) { // Check if proxy is set, then configure it
                    java.net.URI proxyUri = java.net.URI.create(proxyEnv);
                    String host = proxyUri.getHost();
                    int port = proxyUri.getPort() > 0 ? proxyUri.getPort() : 3128;
                    this.httpClient = baseClient.proxy(spec -> spec.type(reactor.netty.transport.ProxyProvider.Proxy.HTTP)
                            .host(host)
                            .port(port));
                } else {
                    this.httpClient = baseClient;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load CA certificate", e);
            }
            this.serverHost = serverHost;
        }

        @GetMapping("/hi")
        public Mono<String> hi() {
            return httpClient
                    .get()
                    .uri(serverHost + "/greeting")
                    .responseContent()
                    .aggregate()
                    .asString()
                    .map(response -> "demo-client received: " + response);
        }
    }
}
