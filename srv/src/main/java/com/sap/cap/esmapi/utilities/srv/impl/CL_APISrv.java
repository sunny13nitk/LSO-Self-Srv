package com.sap.cap.esmapi.utilities.srv.impl;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.pojos.TY_SrvCloudUrls;
import com.sap.cap.esmapi.utilities.pojos.TY_TopSkipRelay;
import com.sap.cap.esmapi.utilities.srv.intf.IF_APISrv;

@Service
@Scope("prototype")
@Profile(GC_Constants.gc_TESTProfile)
public class CL_APISrv implements IF_APISrv
{

    @Autowired
    private TY_SrvCloudUrls srvCloudUrls;

    @Override
    public long getNumberofEntitiesByUrl(String url) throws RuntimeException, IOException
    {

        long numEmtities = 0;
        JsonNode jsonNode = null;

        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        try
        {
            System.out.println("Getting Entities for Url : " + url);
            String encoding = Base64.getEncoder()
                    .encodeToString((srvCloudUrls.getUserName() + ":" + srvCloudUrls.getPassword()).getBytes());
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
            httpGet.addHeader("accept", "application/json");
            // Fire the Url
            try
            {
                response = httpClient.execute(httpGet);
                // verify the valid error code first
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK)
                {
                    throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                }

                // Try and Get Entity from Response
                HttpEntity entity = response.getEntity();
                String apiOutput = EntityUtils.toString(entity);
                // Lets see what we got from API
                // System.out.println(apiOutput);

                // Conerting to JSON
                ObjectMapper mapper = new ObjectMapper();
                jsonNode = mapper.readTree(apiOutput);
                if (jsonNode != null)
                {
                    JsonNode countsNode = jsonNode.path("count");
                    if (countsNode != null)
                    {
                        System.out.println("Count node Bound!!");
                        numEmtities = countsNode.longValue();
                        System.out.println("# of entities : " + numEmtities);
                    }
                }

            }
            catch (IOException e)
            {

                e.printStackTrace();
            }

        }

        finally
        {
            httpClient.close();
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
