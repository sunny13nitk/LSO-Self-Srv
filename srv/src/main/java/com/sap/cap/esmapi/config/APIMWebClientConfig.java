package com.sap.cap.esmapi.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.intf.IF_DestinationService;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class APIMWebClientConfig
{

    private final IF_DestinationService desSrv;

    @Bean
    public WebClient apimWebClient(WebClient.Builder builder)
    {
        if (desSrv != null)
        {
            HttpDestination destination = desSrv.getDestination(GC_Constants.gc_APIM_Destination);
            HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(10));
            return builder.baseUrl(destination.getUri().toString())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .clientConnector(new ReactorClientHttpConnector(httpClient)).build();

        }
        else
        {
            String msg = "Unable to Initialize web client for APIM with destination" + GC_Constants.gc_APIM_Destination;
            log.error(msg);
            throw new EX_ESMAPI(msg);

        }
    }
}
