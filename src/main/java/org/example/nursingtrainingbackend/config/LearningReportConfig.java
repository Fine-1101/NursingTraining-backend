package org.example.nursingtrainingbackend.config;

import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * AI学习报告配置。
 */
@Configuration
@EnableConfigurationProperties(LearningReportProperties.class)
public class LearningReportConfig {

    /**
     * 创建AI接口专用HTTP客户端。
     */
    @Bean
    @Qualifier("aiReportRestClient")
    public RestClient aiReportRestClient(
            LearningReportProperties properties
    ) {
        LearningReportProperties.Ai ai =
                properties.ai();

        /*
         * JDK原生HTTP客户端。
         * connectTimeout控制建立连接的最长等待时间。
         */
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(ai.connectTimeout())
                .build();

        /*
         * Spring请求工厂。
         * readTimeout控制等待AI返回内容的最长时间。
         */
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                ai.readTimeout()
        );

        return  RestClient.builder()
                .baseUrl(ai.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + ai.apiKey()
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }
}