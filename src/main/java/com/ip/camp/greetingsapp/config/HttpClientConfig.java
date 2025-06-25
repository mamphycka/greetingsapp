package com.ip.camp.greetingsapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpClientConfig {

    /**
     * Configures and provides a shared HttpClient instance.
     *
     * @return A pre-configured HttpClient.
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2) // Prefer HTTP/2
                .connectTimeout(Duration.ofSeconds(10)) // Set a connection timeout
                .build();
    }
}
