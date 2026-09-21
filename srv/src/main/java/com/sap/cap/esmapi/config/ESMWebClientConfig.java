package com.sap.cap.esmapi.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ESMWebClientConfig
{

    @Bean("srvCloudWebClient")
    public WebClient srvCloudWebClient(WebClient.Builder webClientBuilder)
    {
        return webClientBuilder.build();
    }

    @Bean
    public WebClient.Builder webClientBuilder()
    {
        log.info("==== webclient builder c4c-pool ====");
        ConnectionProvider provider = ConnectionProvider.builder("c4c-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofHours(3))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(30))
                .compress(true)
                .wiretap(HttpClient.class.getName(), LogLevel.DEBUG, AdvancedByteBufFormat.SIMPLE);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

}
