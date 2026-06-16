package com.sap.cap.esmapi.utilities.srv.impl;

import java.util.List;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cap.esmapi.ui.pojos.TY_TokenResponse;
import com.sap.cap.esmapi.ui.srv.impl.CL_TokenCache;
import com.sap.cap.esmapi.utilities.srv.intf.IF_APIHubTokenSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.intf.IF_DestinationService;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CL_APIHubTokenSrv implements IF_APIHubTokenSrv
{

    private final IF_DestinationService btpDestinationService;

    private final WebClient.Builder webClientBuilder;

    @Override
    public TY_TokenResponse getUserAccessToken(String destinationName)
    {

        TY_TokenResponse token = null;
        if (StringUtils.hasText(destinationName))
        {
            log.info("APIM Access token fetch BEGINS for configured destination " + destinationName);

            // Check cache first
            TY_TokenResponse cachedToken = CL_TokenCache.getToken(destinationName);
            if (cachedToken != null)
            {
                log.info("Token found in cache");
                return cachedToken;
            }

            // Load destination & credentials
            HttpDestination destination = btpDestinationService.getDestination(destinationName);

            String baseUrl = btpDestinationService.getUrl(destination);
            String apiKey = btpDestinationService.getApiKey(destination);

            WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

            try
            {
                log.info("Calling client token endpoint");

                TY_TokenResponse clientResponse = webClient.get()
                        // .uri("/authorize/bearerToken")
                        .uri("/esm-integration-bearer").headers(headers ->
                        {
                            headers.set("apiKey", apiKey);
                            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        }).retrieve().onStatus(HttpStatusCode::isError,
                                response -> response.bodyToMono(String.class).flatMap(errorBody ->
                                {
                                    log.error("APIHUB client token given the error response: {}", errorBody);
                                    return Mono
                                            .error(new RuntimeException("APIHUB client token Failed : " + errorBody));
                                }))
                        .bodyToMono(TY_TokenResponse.class).block();

                log.info("Client Token fetched successfully");

                CL_TokenCache.setToken(destinationName, clientResponse);
                log.info("Token cached and returned");

                return clientResponse;

            }
            catch (Exception e)
            {
                log.error("Exception while fetching user access token: {}", e.getMessage(), e);
                throw new RuntimeException("Unable to fetch user access token", e);
            }
        }

        return token;

    }

}
