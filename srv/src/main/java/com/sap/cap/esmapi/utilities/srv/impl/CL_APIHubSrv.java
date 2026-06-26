package com.sap.cap.esmapi.utilities.srv.impl;

import java.util.List;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.StringsUtility;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.pojos.TY_DestinationsSuffix;
import com.sap.cap.esmapi.utilities.pojos.TY_LKeyEMail;
import com.sap.cap.esmapi.utilities.pojos.TY_PFCTConfigResp;
import com.sap.cap.esmapi.utilities.srv.intf.IF_APIHubSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.intf.IF_DestinationService;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CL_APIHubSrv implements IF_APIHubSrv
{
    private final IF_DestinationService desSrv;

    private final WebClient apimClient;

    private final TY_DestinationsSuffix desSuffix;

    @Override
    public List<TY_PFCTConfigResp> getPartners4LKeyandEmail(TY_LKeyEMail lkeyEmail) throws EX_ESMAPI
    {
        List<TY_PFCTConfigResp> pfcts = null;

        if (lkeyEmail != null)
        {
            if (StringUtils.hasText(lkeyEmail.getHeader()) && StringUtils.hasText(lkeyEmail.getLkey())
                    && StringUtils.hasText(lkeyEmail.getEmail())
                    && StringUtils.hasText(GC_Constants.gc_APIM_Destination) && apimClient != null
                    && StringUtils.hasText(desSuffix.getPartnersAPIMUrl()))
            {
                log.info("Info validated pre APIM partners seek call...");

                HttpDestination destination = desSrv.getDestination(GC_Constants.gc_APIM_Destination);

                String username = desSrv.getUsername(destination);
                String password = desSrv.getPassword(destination);

                log.info("calling APIHUB process api");
                String url = desSuffix.getPartnersAPIMUrl();
                log.info("Before Transformation URL..... {}", url);

                // # TEst- Start
                log.info("Pass Params....");
                log.info(lkeyEmail.getLkey());
                log.info(lkeyEmail.getEmail());
                // # TEst- end

                url = StringsUtility.replaceURLwithParams(url, new String[]
                { lkeyEmail.getLkey(), lkeyEmail.getEmail() }, GC_Constants.gc_UrlReplParam);

                log.info("POST Transformation URL..... {}", url);

                // Step 3: Call API
                pfcts = apimClient.get().uri(url).headers(headers ->
                {
                    // headers.setBearerAuth(userBearerToken);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("userName", username);
                    headers.set("password", password);
                    headers.set("Authorization", "Bearer " + lkeyEmail.getHeader());
                }).retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono
                                        .error(new EX_ESMAPI("Client Error on APIM partners determination: " + error))))

                        .onStatus(HttpStatusCode::is5xxServerError, response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(
                                        new RuntimeException("Server Error on APIM partners determination: " + error))))

                        .bodyToFlux(TY_PFCTConfigResp.class).collectList().block();

            }
        }

        return pfcts;
    }

}
