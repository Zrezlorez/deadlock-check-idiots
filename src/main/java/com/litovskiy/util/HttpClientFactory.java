package com.litovskiy.util;

import okhttp3.OkHttpClient;

public final class HttpClientFactory {

    private HttpClientFactory() {
    }

    public static OkHttpClient create() {
        return BotConfig.isProxyEnabled() ? ProxyManager.getOkHttpClient() : new OkHttpClient();
    }
}
