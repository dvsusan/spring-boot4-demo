package com.susan.training.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;

/**
 * 把 RestClient 适配成声明式 HTTP Interface 代理。
 */
@Component
public class OrderHttpClientFactory {

    /**
     * 每次使用独立的 RestClient 配置，避免修改容器中的共享 Builder。
     */
    public OrderApiClient create(
            String baseUrl,
            RestClient.Builder builder,
            OrderClientProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        var requestFactory = new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        RestClient restClient = builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return proxyFactory.createClient(OrderApiClient.class);
    }
}
