package com.sap.cap.esmapi.utilities.srv.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;

import org.apache.http.HttpStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cap.esmapi.utilities.pojos.TY_TopSkipRelay;
import com.sap.cap.esmapi.utilities.srv.intf.IF_APISrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Scope("prototype")
// @Profile(GC_Constants.gc_BTPProfile)
// @Profile(GC_Constants.gc_LocalProfile)
public class CL_APISrvBTPDest implements IF_APISrv
{

    @Autowired
    private IF_UserSessionSrv userSessionSrv;

    @Autowired
    @Qualifier("srvCloudWebClient")
    private WebClient srvCloudWebClient;

    @Autowired
    private MessageSource msgSrc;

    @Override
    public long getNumberofEntitiesByUrl(String url) throws RuntimeException, IOException
    {

        long numEmtities = 0;
        JsonNode jsonNode = null;

        if (userSessionSrv != null)
        {
            TY_DestinationProps desProps = userSessionSrv.getDestinationDetails4mUserSession();
            if (desProps != null)
            {
                if (StringUtils.hasText(desProps.getAuthToken()))
                {

                    try
                    {
                        System.out.println("Getting Entities for Url : " + url);

                        // Fire the Url
                        ResponseEntity<String> response = srvCloudWebClient.get()
                                .uri(url)
                                .header(HttpHeaders.AUTHORIZATION, desProps.getAuthToken())
                                .header("accept", "application/json")
                                .exchangeToMono(r -> r.toEntity(String.class))
                                .block();
                        if (response == null)
                        {
                            throw new RuntimeException("No response received from API");
                        }
                            // verify the valid error code first
                        if (response.getStatusCode().value() != HttpStatus.SC_OK)
                        {
                            throw new RuntimeException(
                                    "Failed with HTTP error code : " + response.getStatusCode().value());
                        }

                            // Try and Get Entity from Response

                            String apiOutput = response.getBody();;
                            // Lets see what we got from API
                            // System.out.println(apiOutput);
                        if (StringUtils.hasText(apiOutput)) {
                            // Conerting to JSON
                            ObjectMapper mapper = new ObjectMapper();
                            jsonNode = mapper.readTree(apiOutput);
                            if (jsonNode != null) {
                                JsonNode countsNode = jsonNode.path("count");
                                if (countsNode != null) {
                                    System.out.println("Count node Bound!!");
                                    numEmtities = countsNode.longValue();
                                    System.out.println("# of entities : " + numEmtities);
                                }
                            }
                        }



                    }

                    catch (JsonProcessingException e)
                    {
                        throw new EX_ESMAPI(
                                msgSrc.getMessage("ERR_READ_ENTITIES_JSON",
                                        new Object[]{ url, e.getLocalizedMessage() }, Locale.ENGLISH));
                    }
                    catch (Exception e)
                    {
                        throw new EX_ESMAPI(
                                msgSrc.getMessage("ERR_ENTITIES_READ",
                                        new Object[]{ url, e.getLocalizedMessage() }, Locale.ENGLISH));
                    }

                }

            }
        }

        return numEmtities;

    }

    @Override
    public List<JsonNode> getJsonNodesforUrl(String countsUrlName, String pagedUrlName)
            throws RuntimeException, IOException
    {
        List<JsonNode> resultNodes = Collections.emptyList();
        if (StringUtils.hasText(countsUrlName) && StringUtils.hasText(pagedUrlName))
        {
            List<TY_TopSkipRelay> apiSlider = getAPISlider4Url(countsUrlName);
        }
        return resultNodes;
    }

    private List<TY_TopSkipRelay> getAPISlider4Url(String countsUrlName)
    {
        return null;
    }

}
