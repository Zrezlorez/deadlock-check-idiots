package com.litovskiy.config;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class ProxyConfiguration {

    @Value("${proxy.isEnabled}")
    private boolean isProxyEnabled;

    @Value("${proxy.username}")
    private String username;

    @Value("${proxy.password}")
    private String password;

    @Value("${proxy.host}")
    private String host;

    @Value("${proxy.port}")
    private int port;

    @Bean
    public OkHttpClient getOkHttpClient() {
        OkHttpClient proxyClient = new OkHttpClient.Builder()
            .proxy(getProxy())
            .proxyAuthenticator((route, response) -> {
                String credential = Credentials.basic(username, password);
                return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
            })
            .build();
        return isProxyEnabled ? proxyClient : new OkHttpClient();
    }

    public Proxy getProxy() {
        InetSocketAddress address = new InetSocketAddress(host, port);
        return new Proxy(Proxy.Type.HTTP, address);
    }
}
