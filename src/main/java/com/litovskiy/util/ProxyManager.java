package com.litovskiy.util;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;

public final class ProxyManager {

    private ProxyManager() {
    }

    public static OkHttpClient getOkHttpClient() {
        Properties props = PropsManager.getProps();
        String username = props.getProperty("proxy.username");
        String password = props.getProperty("proxy.password");
        return new OkHttpClient.Builder()
            .proxy(getProxy())
            .proxyAuthenticator((route, response) -> {
                String credential = Credentials.basic(username, password);
                return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
            })
            .build();
    }

    public static Proxy getProxy() {
        Properties props = PropsManager.getProps();
        String host = props.getProperty("proxy.host");
        int port = Integer.parseInt(props.getProperty("proxy.port"));

        InetSocketAddress address = new InetSocketAddress(host, port);
        return new Proxy(Proxy.Type.HTTP, address);
    }
}
