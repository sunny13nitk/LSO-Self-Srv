package com.sap.cap.esmapi.utilities.srvCloudApi.srv.impl;

import java.io.IOException;
import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cap.esmapi.catg.pojos.TY_CatalogItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransI;
import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransitions;
import com.sap.cap.esmapi.status.pojos.TY_StatusCfgItem;
import com.sap.cap.esmapi.ui.pojos.TY_Attachment;
import com.sap.cap.esmapi.ui.pojos.TY_CaseConfirmPOJO;
import com.sap.cap.esmapi.utilities.StringsUtility;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.pojos.TY_AttachmentResponse;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseCatalogCustomizing;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseDetails;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseESS;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseGuidId;
import com.sap.cap.esmapi.utilities.pojos.TY_CasePatchInfo;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_CustomerAddUser_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_Customer_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_EmployeeAddUserIC_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_EmployeeAddUser_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_Employee_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_SrvCloud_Confirm;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_SrvCloud_Reply;
import com.sap.cap.esmapi.utilities.pojos.TY_CustomerCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_DefaultComm;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesDetails;
import com.sap.cap.esmapi.utilities.pojos.TY_PreviousAttachments;
import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;
import com.sap.cap.esmapi.utilities.pojos.TY_SrvCloudUrls;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cap.esmapi.utilities.srv.intf.IF_APISrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.URLUtility.CL_URLUtility;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Profile(GC_Constants.gc_LocalProfile)
public class CL_SrvCloudAPI implements IF_SrvCloudAPI
{

    @Autowired
    private TY_SrvCloudUrls srvCloudUrls;

    @Autowired
    private IF_APISrv apiSrv;

    @Autowired
    private TY_CatgCus caseTypeCus;

    @Autowired
    private TY_RLConfig rlConfig;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private TY_PortalStatusTransitions statusTransitions;

    @Override
    public JsonNode getAllCases(TY_DestinationProps desProps) throws IOException
    {
        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String url = null;

        try
        {
            if (StringUtils.hasLength(srvCloudUrls.getUserName()) && StringUtils.hasLength(srvCloudUrls.getPassword())
                    && StringUtils.hasLength(srvCloudUrls.getCasesUrl())
                    && StringUtils.hasText(srvCloudUrls.getToken()))
            {
                log.info("Url and Credentials Found!!");

                long numCases = apiSrv.getNumberofEntitiesByUrl(srvCloudUrls.getCasesUrl());
                if (numCases > 0)
                {
                    url = srvCloudUrls.getCasesUrl() + srvCloudUrls.getTopSuffix() + GC_Constants.equalsString
                            + numCases;

                    HttpGet httpGet = new HttpGet(url);
                    httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                    httpGet.addHeader("accept", "application/json");

                    try
                    {
                        // Fire the Url
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
                        // Log.info(apiOutput);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        jsonNode = mapper.readTree(apiOutput);

                    }
                    catch (IOException e)
                    {

                        e.printStackTrace();
                    }

                }

            }

        }
        finally
        {
            httpClient.close();
        }
        return jsonNode;

    }

    @Override
    public List<TY_CaseESS> getCases4User(String accountIdUser, TY_DestinationProps desProps) throws IOException
    {
        List<TY_CaseESS> casesESSList = null;

        List<TY_CaseESS> casesESSList4User = null;

        try
        {
            if (accountIdUser == null)
            {
                return null;
            }
            else
            {
                JsonNode jsonNode = getAllCases(desProps);

                if (jsonNode != null && CollectionUtils.isNotEmpty(statusTransitions.getStatusTransitions()))
                {
                    List<TY_PortalStatusTransI> statusTransitionsList = statusTransitions.getStatusTransitions();
                    JsonNode rootNode = jsonNode.path("value");
                    if (rootNode != null)
                    {
                        log.info("Cases Bound!!");
                        casesESSList = new ArrayList<TY_CaseESS>();

                        Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                        while (payloadItr.hasNext())
                        {
                            // log.info("Payload Iterator Bound");
                            Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                            String payloadFieldName = payloadEnt.getKey();
                            // log.info("Payload Field Scanned: " + payloadFieldName);

                            if (payloadFieldName.equals("value"))
                            {
                                Iterator<JsonNode> casesItr = payloadEnt.getValue().elements();
                                // log.info("Cases Iterator Bound");
                                while (casesItr.hasNext())
                                {

                                    JsonNode caseEnt = casesItr.next();
                                    if (caseEnt != null)
                                    {
                                        String caseid = null, caseguid = null, caseType = null,
                                                caseTypeDescription = null, subject = null, status = null,
                                                createdOn = null, accountId = null, contactId = null, origin = null,
                                                updatedOn = null;

                                        boolean canConfirm = false;

                                        // log.info("Cases Entity Bound - Reading Case...");
                                        Iterator<String> fieldNames = caseEnt.fieldNames();
                                        while (fieldNames.hasNext())
                                        {
                                            String caseFieldName = fieldNames.next();
                                            // log.info("Case Entity Field Scanned: " + caseFieldName);
                                            if (caseFieldName.equals("id"))
                                            {
                                                // log.info("Case GUID Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    caseguid = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("displayId"))
                                            {
                                                // log.info("Case Id Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    caseid = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("caseType"))
                                            {
                                                // log.info("Case Type Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    caseType = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("caseTypeDescription"))
                                            {
                                                // log.info("Case Type Description Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    caseTypeDescription = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("subject"))
                                            {
                                                // log.info("Case Subject Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    subject = caseEnt.get(caseFieldName).asText();
                                                }
                                            }
                                            if (caseFieldName.equals("statusDescription"))
                                            {
                                                // log.info("Case Status Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    status = caseEnt.get(caseFieldName).asText();
                                                    if (StringUtils.hasText(status))
                                                    {
                                                        String locStatus = status;
                                                        Optional<TY_PortalStatusTransI> transO = statusTransitionsList
                                                                .stream()
                                                                .filter(l -> l.getFromStatus().equals(locStatus))
                                                                .findFirst();
                                                        if (transO.isPresent())
                                                        {
                                                            canConfirm = transO.get().getConfirmAllowed();
                                                        }
                                                    }

                                                }
                                            }

                                            if (caseFieldName.equals("origin"))
                                            {
                                                // log.info("Case Status Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    origin = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("statusDescription"))
                                            {
                                                // log.info("Case Status Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    status = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("adminData"))
                                            {
                                                // log.info("Inside Admin Data: " );

                                                JsonNode admEnt = caseEnt.path("adminData");
                                                if (admEnt != null)
                                                {
                                                    // log.info("AdminData Node Bound");

                                                    Iterator<String> fieldNamesAdm = admEnt.fieldNames();
                                                    while (fieldNamesAdm.hasNext())
                                                    {
                                                        String admFieldName = fieldNamesAdm.next();
                                                        if (admFieldName.equals("createdOn"))
                                                        {

                                                            createdOn = admEnt.get(admFieldName).asText();
                                                        }

                                                        if (admFieldName.equals("updatedOn"))
                                                        {

                                                            updatedOn = admEnt.get(admFieldName).asText();
                                                        }
                                                    }

                                                }
                                            }

                                            if (caseFieldName.equals("account"))
                                            {
                                                // log.info("Inside Account: " );

                                                JsonNode accEnt = caseEnt.path("account");
                                                if (accEnt != null)
                                                {
                                                    // log.info("Account Node Bound");

                                                    Iterator<String> fieldNamesAcc = accEnt.fieldNames();
                                                    while (fieldNamesAcc.hasNext())
                                                    {
                                                        String accFieldName = fieldNamesAcc.next();
                                                        if (accFieldName.equals("id"))
                                                        {
                                                            // log.info(
                                                            // "Account ID : " + accEnt.get(accFieldName).asText());
                                                            accountId = accEnt.get(accFieldName).asText();
                                                        }
                                                    }

                                                }
                                            }

                                            if (caseFieldName.equals("individualCustomer")
                                                    && (!StringUtils.hasText(accountId)))
                                            {
                                                // log.info("Inside Account: " );

                                                JsonNode accEnt = caseEnt.path("individualCustomer");
                                                if (accEnt != null)
                                                {
                                                    // log.info("Account Node Bound");

                                                    Iterator<String> fieldNamesAcc = accEnt.fieldNames();
                                                    while (fieldNamesAcc.hasNext())
                                                    {
                                                        String accFieldName = fieldNamesAcc.next();
                                                        if (accFieldName.equals("id"))
                                                        {
                                                            // log.info(
                                                            // "Account ID : " + accEnt.get(accFieldName).asText());
                                                            accountId = accEnt.get(accFieldName).asText();
                                                        }
                                                    }

                                                }
                                            }

                                            if (caseFieldName.equals("reporter"))
                                            {
                                                // log.info("Inside Reporter: " );

                                                JsonNode repEnt = caseEnt.path("reporter");
                                                if (repEnt != null)
                                                {
                                                    // log.info("Reporter Node Bound");

                                                    Iterator<String> fieldNamesRep = repEnt.fieldNames();
                                                    while (fieldNamesRep.hasNext())
                                                    {
                                                        String repFieldName = fieldNamesRep.next();
                                                        if (repFieldName.equals("id"))
                                                        {
                                                            // log.info(
                                                            // "Reporter ID : " + repEnt.get(repFieldName).asText());
                                                            contactId = repEnt.get(repFieldName).asText();
                                                        }
                                                    }

                                                }
                                            }

                                        }

                                        if (StringUtils.hasText(caseid) && StringUtils.hasText(caseguid))
                                        {
                                            if (StringUtils.hasText(createdOn))
                                            {
                                                // Parse the date-time string into OffsetDateTime
                                                OffsetDateTime odt = OffsetDateTime.parse(createdOn);
                                                // Convert OffsetDateTime into Instant
                                                Instant instant = odt.toInstant();
                                                // If at all, you need java.util.Date
                                                Date date = Date.from(instant);

                                                SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy");
                                                String dateFormatted = sdf.format(date);

                                                if (StringUtils.hasText(updatedOn))
                                                {
                                                    // Parse the date-time string into OffsetDateTime
                                                    OffsetDateTime odtUpd = OffsetDateTime.parse(updatedOn);
                                                    // Convert OffsetDateTime into Instant
                                                    Instant instantUpd = odtUpd.toInstant();
                                                    // If at all, you need java.util.Date
                                                    Date dateUpd = Date.from(instantUpd);

                                                    SimpleDateFormat sdfUpd = new SimpleDateFormat("dd/M/yyyy");
                                                    String dateFormattedUpd = sdfUpd.format(dateUpd);

                                                    casesESSList.add(new TY_CaseESS(caseguid, caseid, caseType,
                                                            caseTypeDescription, subject, status, accountId, contactId,
                                                            createdOn, date, dateFormatted, odt, updatedOn, dateUpd,
                                                            dateFormattedUpd, odtUpd, origin, canConfirm));
                                                }
                                                else
                                                {

                                                    casesESSList.add(new TY_CaseESS(caseguid, caseid, caseType,
                                                            caseTypeDescription, subject, status, accountId, contactId,
                                                            createdOn, date, dateFormatted, odt, null, null, null, null,
                                                            origin, canConfirm));
                                                }

                                            }
                                            else
                                            {
                                                casesESSList.add(
                                                        new TY_CaseESS(caseguid, caseid, caseType, caseTypeDescription,
                                                                subject, status, accountId, contactId, null, null, null,
                                                                null, null, null, null, null, origin, canConfirm));
                                            }

                                        }

                                    }

                                }

                            }

                        }
                    }

                }

            }

        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        /*
         * ------- FILTER FOR USER ACCOUNT or REPORTED BY CONTACT PERSON
         */

        if (!CollectionUtils.isEmpty(casesESSList))
        {
            casesESSList4User = casesESSList.stream().filter(e ->
            {
                // #ESMModule
                // If no Account Itself in Present in Case - Ignore Such Cases --Add Employee
                // with an and condition once ESM module is enabled
                if (!StringUtils.hasText(e.getAccountId()))
                {
                    return false;
                }

                else
                {
                    if (e.getAccountId().equals(accountIdUser))
                    {
                        return true;
                    }

                }
                return false;

            }).collect(Collectors.toList());

        }

        if (!CollectionUtils.isEmpty(casesESSList4User))
        {
            log.info("# Cases returned in call : " + casesESSList4User.size());
        }
        return casesESSList4User;
    }

    @Override
    public List<TY_CaseGuidId> getCaseGuidIdList(TY_DestinationProps desProps)
    {
        List<TY_CaseGuidId> casesGuidIdsList = null;

        try
        {

            JsonNode jsonNode = getAllCases(desProps);

            if (jsonNode != null)
            {

                JsonNode rootNode = jsonNode.path("value");
                if (rootNode != null)
                {
                    log.info("Cases Bound!!");
                    casesGuidIdsList = new ArrayList<TY_CaseGuidId>();

                    Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                    while (payloadItr.hasNext())
                    {
                        log.info("Payload Iterator Bound");
                        Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                        String payloadFieldName = payloadEnt.getKey();
                        log.info("Payload Field Scanned:  " + payloadFieldName);

                        if (payloadFieldName.equals("value"))
                        {
                            Iterator<JsonNode> casesItr = payloadEnt.getValue().elements();
                            log.info("Cases Iterator Bound");
                            while (casesItr.hasNext())
                            {

                                JsonNode caseEnt = casesItr.next();
                                if (caseEnt != null)
                                {
                                    String caseid = null, caseguid = null;
                                    log.info("Cases Entity Bound - Reading Case...");
                                    Iterator<String> fieldNames = caseEnt.fieldNames();
                                    while (fieldNames.hasNext())
                                    {
                                        String caseFieldName = fieldNames.next();
                                        log.info("Case Entity Field Scanned:  " + caseFieldName);
                                        if (caseFieldName.equals("id"))
                                        {
                                            log.info("Case GUID Added : " + caseEnt.get(caseFieldName).asText());
                                            if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                            {
                                                caseguid = caseEnt.get(caseFieldName).asText();
                                            }
                                        }

                                        if (caseFieldName.equals("displayId"))
                                        {
                                            System.out
                                                    .println("Case Id Added : " + caseEnt.get(caseFieldName).asText());
                                            if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                            {
                                                caseid = caseEnt.get(caseFieldName).asText();
                                            }
                                        }

                                    }

                                    if (StringUtils.hasText(caseid) && StringUtils.hasText(caseguid))
                                    {
                                        casesGuidIdsList.add(new TY_CaseGuidId(caseguid, caseid));
                                    }

                                }

                            }

                        }

                    }
                }

            }

        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        return casesGuidIdsList;
    }

    @Override
    public Long getNumberofCases(TY_DestinationProps desProps) throws IOException
    {
        return apiSrv.getNumberofEntitiesByUrl(srvCloudUrls.getCasesUrl());
    }

    @Override
    public String getAccountIdByUserEmail(String userEmail, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String accountID = null;
        if (StringUtils.hasText(userEmail) && srvCloudUrls != null)
        {
            userEmail = '\'' + userEmail + '\''; // In Parmeter Form
            if (StringUtils.hasText(srvCloudUrls.getAccByEmail()))
            {

                try
                {
                    String urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getAccByEmail(), new String[]
                    { userEmail, userEmail }, GC_Constants.gc_UrlReplParam);

                    if (StringUtils.hasText(urlLink) && StringUtils.hasText(srvCloudUrls.getToken()))
                    {

                        try
                        {

                            URL url = new URL(urlLink);
                            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()),
                                    url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                            String correctEncodedURL = uri.toASCIIString();

                            HttpGet httpGet = new HttpGet(correctEncodedURL);
                            httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                            httpGet.addHeader("accept", "application/json");
                            // Fire the Url
                            response = httpClient.execute(httpGet);

                            // verify the valid error code first
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode != HttpStatus.SC_OK)
                            {
                                throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                            }

                            // Try and Get Entity from Response
                            org.apache.http.HttpEntity entity = response.getEntity();
                            String apiOutput = EntityUtils.toString(entity);

                            // Conerting to JSON
                            ObjectMapper mapper = new ObjectMapper();
                            jsonNode = mapper.readTree(apiOutput);
                            if (jsonNode != null)
                            {
                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    if (payloadFieldName.equals("value"))
                                    {
                                        Iterator<JsonNode> accItr = payloadEnt.getValue().elements();
                                        while (accItr.hasNext())
                                        {
                                            JsonNode accEnt = accItr.next();
                                            if (accEnt != null)
                                            {

                                                Iterator<String> fieldNames = accEnt.fieldNames();
                                                while (fieldNames.hasNext())
                                                {
                                                    String accFieldName = fieldNames.next();
                                                    if (accFieldName.equals("id"))
                                                    {
                                                        log.info("Account Id Added : "
                                                                + accEnt.get(accFieldName).asText());
                                                        accountID = accEnt.get(accFieldName).asText();
                                                    }

                                                }

                                            }
                                        }

                                    }

                                }
                            }

                        }

                        catch (Exception e)
                        {
                            if (e != null)
                            {
                                log.error(e.getLocalizedMessage());
                            }
                        }

                    }
                }

                finally
                {

                    try
                    {
                        httpClient.close();
                    }
                    catch (IOException e)
                    {

                        log.error(e.getLocalizedMessage());
                    }

                }

            }

        }
        return accountID;
    }

    @Override
    public String getEmployeeIdByUserEmail(String userEmail, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String accountID = null;
        if (StringUtils.hasText(userEmail) && srvCloudUrls != null)
        {
            userEmail = '\'' + userEmail + '\''; // In Parmeter Form
            if (StringUtils.hasText(srvCloudUrls.getEmpById()))
            {

                try
                {
                    String urlLink = srvCloudUrls.getEmpById() + userEmail;

                    if (StringUtils.hasText(urlLink) && StringUtils.hasText(srvCloudUrls.getToken()))
                    {

                        try
                        {

                            URL url = new URL(urlLink);
                            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()),
                                    url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                            String correctEncodedURL = uri.toASCIIString();

                            HttpGet httpGet = new HttpGet(correctEncodedURL);
                            httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                            httpGet.addHeader("accept", "application/json");
                            // Fire the Url
                            response = httpClient.execute(httpGet);

                            // verify the valid error code first
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode != HttpStatus.SC_OK)
                            {
                                throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                            }

                            // Try and Get Entity from Response
                            org.apache.http.HttpEntity entity = response.getEntity();
                            String apiOutput = EntityUtils.toString(entity);

                            // Conerting to JSON
                            ObjectMapper mapper = new ObjectMapper();
                            jsonNode = mapper.readTree(apiOutput);
                            if (jsonNode != null)
                            {
                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    if (payloadFieldName.equals("value"))
                                    {
                                        Iterator<JsonNode> accItr = payloadEnt.getValue().elements();
                                        while (accItr.hasNext())
                                        {
                                            JsonNode accEnt = accItr.next();
                                            if (accEnt != null)
                                            {

                                                Iterator<String> fieldNames = accEnt.fieldNames();
                                                while (fieldNames.hasNext())
                                                {
                                                    String accFieldName = fieldNames.next();
                                                    if (accFieldName.equals("id"))
                                                    {
                                                        log.info("Employee Id Added : "
                                                                + accEnt.get(accFieldName).asText());
                                                        accountID = accEnt.get(accFieldName).asText();
                                                    }

                                                }

                                            }
                                        }

                                    }

                                }
                            }

                        }

                        catch (Exception e)
                        {
                            if (e != null)
                            {
                                log.error(e.getLocalizedMessage());
                            }
                        }

                    }
                }

                finally
                {

                    try
                    {
                        httpClient.close();
                    }
                    catch (IOException e)
                    {

                        log.error(e.getLocalizedMessage());
                    }

                }

            }

        }
        return accountID;
    }

    @Override
    public JsonNode getAllAccounts(TY_DestinationProps desProps) throws IOException
    {
        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String url = null;

        try
        {
            if (StringUtils.hasLength(srvCloudUrls.getUserName()) && StringUtils.hasLength(srvCloudUrls.getPassword())
                    && StringUtils.hasLength(srvCloudUrls.getAccountsUrl()))
            {
                log.info("Url and Credentials Found!!");

                long numAccounts = apiSrv.getNumberofEntitiesByUrl(srvCloudUrls.getAccountsUrl());
                if (numAccounts > 0)
                {
                    url = srvCloudUrls.getAccountsUrl() + srvCloudUrls.getTopSuffix() + GC_Constants.equalsString
                            + numAccounts;
                    String encoding = Base64.getEncoder()
                            .encodeToString((srvCloudUrls.getUserName() + ":" + srvCloudUrls.getPassword()).getBytes());

                    HttpGet httpGet = new HttpGet(url);
                    httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
                    httpGet.addHeader("accept", "application/json");

                    try
                    {
                        // Fire the Url
                        response = httpClient.execute(httpGet);

                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_OK)
                        {
                            throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                        }

                        // Try and Get Entity from Response
                        org.apache.http.HttpEntity entity = response.getEntity();
                        String apiOutput = EntityUtils.toString(entity);
                        // Lets see what we got from API
                        log.info(apiOutput);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        jsonNode = mapper.readTree(apiOutput);

                    }
                    catch (IOException e)
                    {

                        e.printStackTrace();
                    }
                }

            }

        }
        finally
        {
            httpClient.close();
        }
        return jsonNode;
    }

    @Override
    public JsonNode getAllEmployees(TY_DestinationProps desProps) throws IOException
    {
        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String url = null;

        try
        {
            if (StringUtils.hasLength(srvCloudUrls.getUserName()) && StringUtils.hasLength(srvCloudUrls.getPassword())
                    && StringUtils.hasLength(srvCloudUrls.getEmplUrl()))
            {
                log.info("Url and Credentials Found!!");

                long numEmpl = apiSrv.getNumberofEntitiesByUrl(srvCloudUrls.getEmplUrl());
                if (numEmpl > 0)
                {
                    url = srvCloudUrls.getEmplUrl() + srvCloudUrls.getTopSuffix() + GC_Constants.equalsString + numEmpl;
                    String encoding = Base64.getEncoder()
                            .encodeToString((srvCloudUrls.getUserName() + ":" + srvCloudUrls.getPassword()).getBytes());

                    HttpGet httpGet = new HttpGet(url);
                    httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
                    httpGet.addHeader("accept", "application/json");

                    try
                    {
                        // Fire the Url
                        response = httpClient.execute(httpGet);

                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_OK)
                        {
                            throw new RuntimeException(
                                    "Failed with HTTP error code : " + statusCode + "on Employees Read API");
                        }

                        // Try and Get Entity from Response
                        org.apache.http.HttpEntity entity = response.getEntity();
                        String apiOutput = EntityUtils.toString(entity);
                        // Lets see what we got from API
                        log.info(apiOutput);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        jsonNode = mapper.readTree(apiOutput);

                    }
                    catch (IOException e)
                    {

                        e.printStackTrace();
                    }
                }

            }

        }
        finally
        {
            httpClient.close();
        }
        return jsonNode;
    }

    @Override
    public JsonNode getAllContacts(TY_DestinationProps desProps) throws IOException
    {
        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String url = null;

        try
        {
            if (StringUtils.hasLength(srvCloudUrls.getUserName()) && StringUtils.hasLength(srvCloudUrls.getPassword())
                    && StringUtils.hasLength(srvCloudUrls.getCpUrl()))
            {
                log.info("Url and Credentials Found!!");

                long numAccounts = apiSrv.getNumberofEntitiesByUrl(srvCloudUrls.getCpUrl());
                if (numAccounts > 0)
                {
                    url = srvCloudUrls.getCpUrl() + srvCloudUrls.getTopSuffix() + GC_Constants.equalsString
                            + numAccounts;
                    String encoding = Base64.getEncoder()
                            .encodeToString((srvCloudUrls.getUserName() + ":" + srvCloudUrls.getPassword()).getBytes());

                    HttpGet httpGet = new HttpGet(url);
                    httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
                    httpGet.addHeader("accept", "application/json");

                    try
                    {
                        // Fire the Url
                        response = httpClient.execute(httpGet);

                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_OK)
                        {
                            throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                        }

                        // Try and Get Entity from Response
                        org.apache.http.HttpEntity entity = response.getEntity();
                        String apiOutput = EntityUtils.toString(entity);
                        // Lets see what we got from API
                        log.info(apiOutput);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        jsonNode = mapper.readTree(apiOutput);

                    }
                    catch (IOException e)
                    {

                        e.printStackTrace();
                    }
                }

            }

        }
        finally
        {
            httpClient.close();
        }
        return jsonNode;
    }

    @Override
    public String getContactPersonIdByUserEmail(String userEmail, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String contactID = null;
        if (StringUtils.hasText(userEmail) && srvCloudUrls != null)
        {
            userEmail = '\'' + userEmail + '\''; // In Parmeter Form
            if (StringUtils.hasText(srvCloudUrls.getConByEmail()))
            {

                try
                {
                    String urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getConByEmail(), new String[]
                    { userEmail }, GC_Constants.gc_UrlReplParam);

                    if (StringUtils.hasText(urlLink))
                    {

                        String encoding = Base64.getEncoder().encodeToString(
                                (srvCloudUrls.getUserName() + ":" + srvCloudUrls.getPassword()).getBytes());

                        try
                        {

                            URL url = new URL(urlLink);
                            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()),
                                    url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                            String correctEncodedURL = uri.toASCIIString();

                            HttpGet httpGet = new HttpGet(correctEncodedURL);
                            httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
                            httpGet.addHeader("accept", "application/json");
                            // Fire the Url
                            response = httpClient.execute(httpGet);

                            // verify the valid error code first
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode != HttpStatus.SC_OK)
                            {
                                throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                            }

                            // Try and Get Entity from Response
                            org.apache.http.HttpEntity entity = response.getEntity();
                            String apiOutput = EntityUtils.toString(entity);

                            // Conerting to JSON
                            ObjectMapper mapper = new ObjectMapper();
                            jsonNode = mapper.readTree(apiOutput);
                            if (jsonNode != null)
                            {
                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    if (payloadFieldName.equals("value"))
                                    {
                                        Iterator<JsonNode> accItr = payloadEnt.getValue().elements();
                                        while (accItr.hasNext())
                                        {
                                            JsonNode accEnt = accItr.next();
                                            if (accEnt != null)
                                            {

                                                Iterator<String> fieldNames = accEnt.fieldNames();
                                                while (fieldNames.hasNext())
                                                {
                                                    String accFieldName = fieldNames.next();
                                                    if (accFieldName.equals("id"))
                                                    {
                                                        log.info("Contact Id Added : "
                                                                + accEnt.get(accFieldName).asText());
                                                        contactID = accEnt.get(accFieldName).asText();
                                                    }

                                                }

                                            }
                                        }

                                    }

                                }
                            }

                        }

                        catch (Exception e)
                        {
                            if (e != null)
                            {
                                log.error(e.getLocalizedMessage());
                            }
                        }

                    }
                }

                finally
                {

                    try
                    {
                        httpClient.close();
                    }
                    catch (IOException e)
                    {

                        log.error(e.getLocalizedMessage());
                    }

                }

            }

        }
        return contactID;

    }

    @Override
    public String createAccount(String userEmail, String userName, TY_DestinationProps desProps) throws EX_ESMAPI
    {
        String accountId = null;
        TY_CustomerCreate newAccount;
        // User Email and UserName Bound
        if (StringUtils.hasText(userEmail) && StringUtils.hasText(userName))
        {
            log.info("Creating Account for UserName : " + userName + "with Email : " + userEmail);
            String[] names = userName.split("\\s+");
            if (names.length > 1)
            {
                newAccount = new TY_CustomerCreate(names[0], names[1], GC_Constants.gc_roleCustomer,
                        GC_Constants.gc_statusACTIVE, new TY_DefaultComm(userEmail));
            }
            else
            {
                newAccount = new TY_CustomerCreate(names[0], names[0], GC_Constants.gc_roleCustomer,
                        GC_Constants.gc_statusACTIVE, new TY_DefaultComm(userEmail));
            }

            if (newAccount != null)
            {

                HttpClient httpclient = HttpClients.createDefault();
                String accPOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCustomerUrl());
                if (StringUtils.hasText(accPOSTURL))
                {
                    String encoding = Base64.getEncoder().encodeToString(
                            (srvCloudUrls.getUserNameExt() + ":" + srvCloudUrls.getPasswordExt()).getBytes());
                    HttpPost httpPost = new HttpPost(accPOSTURL);
                    httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
                    httpPost.addHeader("Content-Type", "application/json");

                    ObjectMapper objMapper = new ObjectMapper();
                    try
                    {
                        String requestBody = objMapper.writeValueAsString(newAccount);
                        log.info(requestBody);

                        StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                        httpPost.setEntity(entity);

                        // POST Account in Service Cloud
                        try
                        {
                            // Fire the Url
                            HttpResponse response = httpclient.execute(httpPost);
                            // verify the valid error code first
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode != HttpStatus.SC_CREATED)
                            {
                                throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                            }

                            // Try and Get Entity from Response
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);

                            // Conerting to JSON
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode jsonNode = mapper.readTree(apiOutput);

                            if (jsonNode != null)
                            {

                                JsonNode rootNode = jsonNode.path("value");
                                if (rootNode != null)
                                {

                                    log.info("Account Bound!!");

                                    Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                    while (payloadItr.hasNext())
                                    {
                                        log.info("Payload Iterator Bound");
                                        Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                        String payloadFieldName = payloadEnt.getKey();
                                        log.info("Payload Field Scanned:  " + payloadFieldName);

                                        if (payloadFieldName.equals("value"))
                                        {
                                            JsonNode accEnt = payloadEnt.getValue();
                                            log.info("New Account Entity Bound");
                                            if (accEnt != null)
                                            {

                                                log.info("Accounts Entity Bound - Reading Account...");
                                                Iterator<String> fieldNames = accEnt.fieldNames();
                                                while (fieldNames.hasNext())
                                                {
                                                    String accFieldName = fieldNames.next();

                                                    if (accFieldName.equals("id"))
                                                    {
                                                        log.info("Account GUID Added : "
                                                                + accEnt.get(accFieldName).asText());
                                                        if (StringUtils.hasText(accEnt.get(accFieldName).asText()))
                                                        {
                                                            accountId = accEnt.get(accFieldName).asText();

                                                        }
                                                        break;
                                                    }

                                                }

                                            }

                                        }

                                    }
                                }
                            }

                        }
                        catch (IOException e)
                        {
                            throw new EX_ESMAPI(msgSrc.getMessage("ERR_ACC_POST", new Object[]
                            { e.getLocalizedMessage() }, Locale.ENGLISH));
                        }
                    }
                    catch (JsonProcessingException e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_AC_JSON", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }

                }

            }
        }
        return accountId;
    }

    @Override
    public TY_CaseCatalogCustomizing getActiveCaseTemplateConfig4CaseType(String caseType, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {
        TY_CaseCatalogCustomizing caseCus = null;
        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String url = null;

        try
        {
            if (StringUtils.hasLength(srvCloudUrls.getCaseTemplateUrl())
                    && StringUtils.hasText(srvCloudUrls.getToken()))
            {
                log.info("Url and Credentials Found!!");

                url = srvCloudUrls.getCaseTemplateUrl() + caseType;

                HttpGet httpGet = new HttpGet(url);
                httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpGet.addHeader("accept", "application/json");

                // Fire the Url
                response = httpClient.execute(httpGet);

                // verify the valid error code first
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK)
                {

                    if (statusCode == HttpStatus.SC_NOT_FOUND)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                        { caseType }, Locale.ENGLISH));
                    }
                    else
                    {
                        throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                    }

                }

                // Try and Get Entity from Response
                HttpEntity entity = response.getEntity();
                String apiOutput = EntityUtils.toString(entity);
                // Lets see what we got from API
                // log.info(apiOutput);

                // Conerting to JSON
                ObjectMapper mapper = new ObjectMapper();
                jsonNode = mapper.readTree(apiOutput);

                if (jsonNode != null)
                {
                    JsonNode rootNode = jsonNode.path("value");
                    if (rootNode != null)
                    {
                        log.info("Customizing Bound!!");
                        List<TY_CaseCatalogCustomizing> caseCusList = new ArrayList<TY_CaseCatalogCustomizing>();

                        Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                        while (payloadItr.hasNext())
                        {
                            // log.info("Payload Iterator Bound");
                            Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                            String payloadFieldName = payloadEnt.getKey();
                            // log.info("Payload Field Scanned: " + payloadFieldName);

                            if (payloadFieldName.equals("value"))
                            {
                                Iterator<JsonNode> cusItr = payloadEnt.getValue().elements();
                                // log.info("Cases Iterator Bound");
                                while (cusItr.hasNext())
                                {

                                    JsonNode cusEnt = cusItr.next();
                                    if (cusEnt != null)
                                    {
                                        String caseTypePL = null, statusSchema = null, status = null,
                                                partyScheme = null, cataglogId = null;

                                        Iterator<String> fieldNames = cusEnt.fieldNames();
                                        while (fieldNames.hasNext())
                                        {
                                            String cusFieldName = fieldNames.next();
                                            // log.info("Case Entity Field Scanned: " + caseFieldName);
                                            if (cusFieldName.equals("caseType"))
                                            {
                                                // log.info("Case GUID Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(cusEnt.get(cusFieldName).asText()))
                                                {
                                                    caseTypePL = cusEnt.get(cusFieldName).asText();
                                                }
                                            }

                                            if (cusFieldName.equals("statusSchema"))
                                            {
                                                // log.info("Case Id Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(cusEnt.get(cusFieldName).asText()))
                                                {
                                                    statusSchema = cusEnt.get(cusFieldName).asText();
                                                }
                                            }

                                            if (cusFieldName.equals("status"))
                                            {
                                                // log.info("Case Id Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(cusEnt.get(cusFieldName).asText()))
                                                {
                                                    status = cusEnt.get(cusFieldName).asText();
                                                }
                                            }

                                            if (cusFieldName.equals("partyScheme"))
                                            {
                                                // log.info("Case Id Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(cusEnt.get(cusFieldName).asText()))
                                                {
                                                    partyScheme = cusEnt.get(cusFieldName).asText();
                                                }
                                            }

                                            if (cusFieldName.equals("catalog"))
                                            {
                                                // log.info("Inside Admin Data: " );

                                                JsonNode catEnt = cusEnt.path("catalog");
                                                if (catEnt != null)
                                                {
                                                    // log.info("AdminData Node Bound");

                                                    Iterator<String> fieldNamesCat = catEnt.fieldNames();
                                                    while (fieldNamesCat.hasNext())
                                                    {
                                                        String catFieldName = fieldNamesCat.next();
                                                        if (catFieldName.equals("id"))
                                                        {
                                                            // log.info( "Created On : " +
                                                            // admEnt.get(admFieldName).asText());
                                                            cataglogId = catEnt.get(catFieldName).asText();
                                                        }
                                                    }

                                                }
                                            }

                                        }

                                        if (StringUtils.hasText(cataglogId) && StringUtils.hasText(caseTypePL))
                                        {

                                            caseCusList.add(new TY_CaseCatalogCustomizing(caseTypePL, statusSchema,
                                                    status, partyScheme, cataglogId));

                                        }

                                    }

                                }

                            }

                        }

                        // Get the Active Catalog Assignment
                        if (CollectionUtils.isNotEmpty(caseCusList))
                        {
                            Optional<TY_CaseCatalogCustomizing> caseCusO = caseCusList.stream()
                                    .filter(r -> r.getStatus().equals(GC_Constants.gc_statusACTIVE)).findFirst();
                            if (caseCusO.isPresent())
                            {
                                caseCus = caseCusO.get();
                            }
                        }
                    }

                }

            }
        }

        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CATG_LOAD_CASETYP", new Object[]
            { caseType, e.getMessage() }, Locale.ENGLISH));

        }
        finally
        {
            httpClient.close();
        }

        return caseCus;
    }

    @Override
    public List<TY_CatalogItem> getActiveCaseCategoriesByCatalogId(String catalogID, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {
        List<TY_CatalogItem> catgTree = null;
        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String urlLink = null;

        try
        {
            if (StringUtils.hasLength(srvCloudUrls.getCatgTreeUrl()) && StringUtils.hasText(srvCloudUrls.getToken()))
            {
                log.info("Url and Credentials Found!!");

                urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getCatgTreeUrl(), new String[]
                { catalogID }, GC_Constants.gc_UrlReplParam);

                // Query URL Encoding to avoid Illegal character error in Query
                URL url = new URL(urlLink);
                URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(),
                        url.getPath(), url.getQuery(), url.getRef());
                String correctEncodedURL = uri.toASCIIString();

                HttpGet httpGet = new HttpGet(correctEncodedURL);

                httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpGet.addHeader("accept", "application/json");

                // Fire the Url
                response = httpClient.execute(httpGet);

                // verify the valid error code first
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK)
                {

                    if (statusCode == HttpStatus.SC_NOT_FOUND)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_CATALOG_READ", new Object[]
                        { catalogID }, Locale.ENGLISH));
                    }
                    else
                    {
                        throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                    }

                }

                // Try and Get Entity from Response
                HttpEntity entity = response.getEntity();
                String apiOutput = EntityUtils.toString(entity);
                // Lets see what we got from API
                // log.info(apiOutput);

                // Conerting to JSON
                ObjectMapper mapper = new ObjectMapper();
                jsonNode = mapper.readTree(apiOutput);

                if (jsonNode != null)
                {

                    JsonNode rootNode = jsonNode.path("value");
                    if (rootNode != null)
                    {
                        log.info("Customizing Bound!!");
                        catgTree = new ArrayList<TY_CatalogItem>();

                        Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                        while (payloadItr.hasNext())
                        {
                            // log.info("Payload Iterator Bound");
                            Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                            String payloadFieldName = payloadEnt.getKey();
                            // log.info("Payload Field Scanned: " + payloadFieldName);

                            if (payloadFieldName.equals("value"))
                            {
                                Iterator<JsonNode> cusItr = payloadEnt.getValue().elements();
                                // log.info("Cases Iterator Bound");
                                while (cusItr.hasNext())
                                {

                                    JsonNode cusEnt = cusItr.next();
                                    if (cusEnt != null)
                                    {
                                        String id = null, parentId = null, name = null, parentName = null;

                                        Iterator<String> fieldNames = cusEnt.fieldNames();
                                        while (fieldNames.hasNext())
                                        {
                                            String cusFieldName = fieldNames.next();
                                            // log.info("Case Entity Field Scanned: " + caseFieldName);
                                            if (cusFieldName.equals("id"))
                                            {
                                                // log.info("Case GUID Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(cusEnt.get(cusFieldName).asText()))
                                                {
                                                    id = cusEnt.get(cusFieldName).asText();
                                                }
                                            }

                                            if (cusFieldName.equals("parentId"))
                                            {
                                                // log.info("Case Id Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(cusEnt.get(cusFieldName).asText()))
                                                {
                                                    parentId = cusEnt.get(cusFieldName).asText();
                                                }
                                            }

                                            if (cusFieldName.equals("name"))
                                            {
                                                // log.info("Case Id Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(cusEnt.get(cusFieldName).asText()))
                                                {
                                                    name = cusEnt.get(cusFieldName).asText();
                                                }
                                            }

                                            if (cusFieldName.equals("parentName"))
                                            {
                                                // log.info("Case Id Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(cusEnt.get(cusFieldName).asText()))
                                                {
                                                    parentName = cusEnt.get(cusFieldName).asText();
                                                }
                                            }

                                        }

                                        if (StringUtils.hasText(id))
                                        {

                                            catgTree.add(new TY_CatalogItem(id, name, parentId, parentName));

                                        }

                                    }

                                }

                            }

                        }

                    }

                }

            }
        }

        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CATALOG_READ", new Object[]
            { catalogID, e.getMessage() }, Locale.ENGLISH));

        }
        finally
        {
            httpClient.close();
        }

        return catgTree;
    }

    @Override
    public String createNotes(TY_NotesCreate notes, TY_DestinationProps desProps) throws EX_ESMAPI
    {
        String noteId = null;

        if (StringUtils.hasText(notes.getHtmlContent()))
        {
            HttpClient httpclient = HttpClients.createDefault();
            String notesPOSTURL = srvCloudUrls.getNotesUrl();
            if (StringUtils.hasText(notesPOSTURL))
            {

                HttpPost httpPost = new HttpPost(notesPOSTURL);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpPost.addHeader("Content-Type", "application/json");

                ObjectMapper objMapper = new ObjectMapper();
                try
                {
                    String requestBody = objMapper.writeValueAsString(notes);
                    log.info(requestBody);

                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(entity);

                    // POST Notes in Service Cloud
                    try
                    {
                        // Fire the Url
                        HttpResponse response = httpclient.execute(httpPost);
                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_CREATED)
                        {
                            throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                        }

                        // Try and Get Entity from Response
                        HttpEntity entityResp = response.getEntity();
                        String apiOutput = EntityUtils.toString(entityResp);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(apiOutput);

                        if (jsonNode != null)
                        {

                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null)
                            {

                                log.info("Notes Bound!!");

                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    log.info("Payload Iterator Bound");
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    log.info("Payload Field Scanned:  " + payloadFieldName);

                                    if (payloadFieldName.equals("value"))
                                    {
                                        JsonNode notesEnt = payloadEnt.getValue();
                                        log.info("New Notes Entity Bound");
                                        if (notesEnt != null)
                                        {

                                            log.info("Notes Entity Bound - Reading Notes...");
                                            Iterator<String> fieldNames = notesEnt.fieldNames();
                                            while (fieldNames.hasNext())
                                            {
                                                String notesFieldName = fieldNames.next();
                                                log.info("Notes Entity Field Scanned:  " + notesFieldName);
                                                if (notesFieldName.equals("id"))
                                                {
                                                    log.info("Notes GUID Added : "
                                                            + notesEnt.get(notesFieldName).asText());
                                                    if (StringUtils.hasText(notesEnt.get(notesFieldName).asText()))
                                                    {
                                                        noteId = notesEnt.get(notesFieldName).asText();

                                                    }
                                                }

                                            }

                                        }

                                    }

                                }
                            }
                        }

                    }
                    catch (IOException e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }
                }
                catch (JsonProcessingException e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_NOTES_JSON", new Object[]
                    { e.getLocalizedMessage() }, Locale.ENGLISH));
                }

            }

        }

        return noteId;
    }

    @Override
    public String createCase(TY_Case_Customer_SrvCloud caseEntity, TY_DestinationProps desProps) throws EX_ESMAPI
    {
        String caseId = null;

        if (StringUtils.hasText(caseEntity.getAccount().getId()))
        {
            HttpClient httpclient = HttpClients.createDefault();
            String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCasesUrl());
            if (StringUtils.hasText(casePOSTURL))
            {

                HttpPost httpPost = new HttpPost(casePOSTURL);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpPost.addHeader("Content-Type", "application/json");

                ObjectMapper objMapper = new ObjectMapper();
                try
                {
                    String requestBody = objMapper.writeValueAsString(caseEntity);
                    log.info(requestBody);

                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(entity);

                    // POST Case in Service Cloud
                    try
                    {
                        // Fire the Url
                        HttpResponse response = httpclient.execute(httpPost);
                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_CREATED)
                        {
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);
                            log.info(apiOutput);
                            throw new RuntimeException(
                                    "Failed with HTTP error code : " + statusCode + " Details: " + apiOutput);

                        }

                        // Try and Get Entity from Response
                        HttpEntity entityResp = response.getEntity();
                        String apiOutput = EntityUtils.toString(entityResp);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(apiOutput);

                        if (jsonNode != null)
                        {

                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null)
                            {

                                log.info("Notes Bound!!");

                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    log.info("Payload Iterator Bound");
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    log.info("Payload Field Scanned:  " + payloadFieldName);

                                    if (payloadFieldName.equals("value"))
                                    {
                                        JsonNode caseEnt = payloadEnt.getValue();
                                        log.info("New Case Entity Bound");
                                        if (caseEnt != null)
                                        {

                                            log.info("Case Entity Bound - Reading Case...");
                                            Iterator<String> fieldNames = caseEnt.fieldNames();
                                            while (fieldNames.hasNext())
                                            {
                                                String caseFieldName = fieldNames.next();
                                                log.info("Case Entity Field Scanned:  " + caseFieldName);
                                                if (caseFieldName.equals("displayId"))
                                                {
                                                    log.info("Case ID Added : " + caseEnt.get(caseFieldName).asText());
                                                    if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                    {
                                                        caseId = caseEnt.get(caseFieldName).asText();

                                                    }
                                                    break;
                                                }

                                            }

                                        }

                                    }

                                }
                            }
                        }

                    }
                    catch (IOException e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }
                }
                catch (JsonProcessingException e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_NOTES_JSON", new Object[]
                    { e.getLocalizedMessage() }, Locale.ENGLISH));
                }

            }

        }

        return caseId;
    }

    @Override
    public TY_AttachmentResponse createAttachment(TY_Attachment attachment, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {
        TY_AttachmentResponse attR = null;

        if (attachment != null)
        {
            // Populate the Attachment POJO for getting the POST Url for Saving the
            // attachment
            if (StringUtils.hasText(attachment.getFileName()))
            {
                HttpClient httpclient = HttpClients.createDefault();
                String docPOSTURL = srvCloudUrls.getDocSrvUrl();

                // Call Attachment POST to generate the Document Store Url

                HttpPost httpPost = new HttpPost(docPOSTURL);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpPost.addHeader("Content-Type", "application/json");

                ObjectMapper objMapper = new ObjectMapper();
                String requestBody;
                try
                {
                    requestBody = objMapper.writeValueAsString(attachment);
                    log.info(requestBody);

                    if (requestBody != null)
                    {
                        StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                        httpPost.setEntity(entity);
                        // POST Notes in Service Cloud
                        try
                        {
                            // Fire the Url
                            HttpResponse response = httpclient.execute(httpPost);

                            // verify the valid error code first
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_OK)
                            {
                                throw new RuntimeException("Failed with HTTP error code : " + statusCode + " Message - "
                                        + response.getStatusLine().toString());
                            }

                            // Try and Get Entity from Response
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);

                            // Conerting to JSON
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode jsonNode = mapper.readTree(apiOutput);

                            if (jsonNode != null)
                            {
                                JsonNode rootNode = jsonNode.path("value");
                                if (rootNode != null)
                                {

                                    log.info("Attachments Bound!!");

                                    Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                    while (payloadItr.hasNext())
                                    {
                                        log.info("Payload Iterator Bound");
                                        Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                        String payloadFieldName = payloadEnt.getKey();
                                        log.info("Payload Field Scanned:  " + payloadFieldName);

                                        if (payloadFieldName.equals("value"))
                                        {
                                            JsonNode attEnt = payloadEnt.getValue();
                                            log.info("New Attachment Entity Bound");
                                            if (attEnt != null)
                                            {
                                                // Initailize Response Entity
                                                attR = new TY_AttachmentResponse();

                                                log.info("Attachments Entity Bound - Reading Attachments Response...");
                                                Iterator<String> fieldNames = attEnt.fieldNames();
                                                while (fieldNames.hasNext())
                                                {
                                                    String attFieldName = fieldNames.next();
                                                    log.info("Notes Entity Field Scanned:  " + attFieldName);

                                                    // attachment ID
                                                    if (attFieldName.equals("id"))
                                                    {
                                                        log.info("Attachment GUID Added : "
                                                                + attEnt.get(attFieldName).asText());
                                                        if (StringUtils.hasText(attEnt.get(attFieldName).asText()))
                                                        {
                                                            attR.setId(attEnt.get(attFieldName).asText());

                                                        }
                                                    }

                                                    // attachment Upload URL
                                                    if (attFieldName.equals("uploadUrl"))
                                                    {
                                                        log.info("Attachment Upload Url Added : "
                                                                + attEnt.get(attFieldName).asText());
                                                        if (StringUtils.hasText(attEnt.get(attFieldName).asText()))
                                                        {
                                                            attR.setUploadUrl(attEnt.get(attFieldName).asText());

                                                        }
                                                    }

                                                }

                                            }

                                        }

                                    }
                                }
                            }

                        }
                        catch (IOException e)
                        {
                            throw new EX_ESMAPI(msgSrc.getMessage("ERR_DOCS_POST", new Object[]
                            { e.getLocalizedMessage() }, Locale.ENGLISH));
                        }

                    }
                }
                catch (JsonProcessingException e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_DOCS_JSON", new Object[]
                    { e.getLocalizedMessage(), attachment.toString() }, Locale.ENGLISH));
                }

            }
        }

        return attR;

    }

    @Override
    public boolean persistAttachment(String url, MultipartFile file, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {
        boolean isPersisted = false;
        if (StringUtils.hasText(url))
        {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPut httpPut = new HttpPut(url);
            if (httpPut != null)
            {
                ByteArrayEntity requestEntity = new ByteArrayEntity(file.getBytes());
                if (requestEntity != null)
                {
                    httpPut.setEntity(requestEntity);

                    // Fire the Url
                    HttpResponse response = httpclient.execute(httpPut);
                    // verify the valid error code first
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK)
                    {
                        isPersisted = true;
                    }
                    else
                    {
                        HttpEntity entityResp = response.getEntity();
                        String apiOutput = EntityUtils.toString(entityResp);
                        log.error(apiOutput);
                        throw new EX_ESMAPI("Error peristing Attachment for filename : " + file.getOriginalFilename()
                                + "HTTPSTATUS Code" + statusCode + "Details :" + apiOutput);
                    }

                }
            }

        }

        return isPersisted;
    }

    @Override
    public boolean persistAttachment(String url, String fileName, byte[] blob, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {
        boolean isPersisted = false;
        if (StringUtils.hasText(url))
        {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPut httpPut = new HttpPut(url);
            if (httpPut != null)
            {
                ByteArrayEntity requestEntity = new ByteArrayEntity(blob);
                if (requestEntity != null)
                {
                    httpPut.setEntity(requestEntity);

                    // Fire the Url
                    HttpResponse response = httpclient.execute(httpPut);
                    // verify the valid error code first
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK)
                    {
                        isPersisted = true;
                    }
                    else
                    {
                        HttpEntity entityResp = response.getEntity();
                        String apiOutput = EntityUtils.toString(entityResp);
                        log.error(apiOutput);
                        throw new EX_ESMAPI("Error peristing Attachment for filename : " + fileName + "HTTPSTATUS Code"
                                + statusCode + "Details :" + apiOutput);
                    }

                }
            }

        }

        return isPersisted;
    }

    @Override
    public String getEmployeeIdByUserId(String userId, TY_DestinationProps desProps) throws EX_ESMAPI
    {
        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String empID = null;
        // Only Internal User(s) Allowed Login can Execute Employee Search
        if (StringUtils.hasText(userId) && srvCloudUrls != null && userId.matches(rlConfig.getInternalUsersRegex()))
        {
            userId = '\'' + userId + '\''; // In Parmeter Form
            if (StringUtils.hasText(srvCloudUrls.getEmpById()))
            {

                try
                {
                    String urlLink = srvCloudUrls.getEmpById() + userId;

                    if (StringUtils.hasText(urlLink) && StringUtils.hasText(srvCloudUrls.getToken()))
                    {

                        try
                        {

                            URL url = new URL(urlLink);
                            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()),
                                    url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                            String correctEncodedURL = uri.toASCIIString();

                            HttpGet httpGet = new HttpGet(correctEncodedURL);
                            httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                            httpGet.addHeader("accept", "application/json");
                            // Fire the Url
                            response = httpClient.execute(httpGet);

                            // verify the valid error code first
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode != HttpStatus.SC_OK)
                            {
                                throw new RuntimeException("Failed with HTTP error code : " + statusCode);
                            }

                            // Try and Get Entity from Response
                            org.apache.http.HttpEntity entity = response.getEntity();
                            String apiOutput = EntityUtils.toString(entity);

                            // Conerting to JSON
                            ObjectMapper mapper = new ObjectMapper();
                            jsonNode = mapper.readTree(apiOutput);
                            if (jsonNode != null)
                            {
                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    if (payloadFieldName.equals("value"))
                                    {
                                        Iterator<JsonNode> accItr = payloadEnt.getValue().elements();
                                        while (accItr.hasNext())
                                        {
                                            JsonNode accEnt = accItr.next();
                                            if (accEnt != null)
                                            {

                                                Iterator<String> fieldNames = accEnt.fieldNames();
                                                while (fieldNames.hasNext())
                                                {
                                                    String accFieldName = fieldNames.next();
                                                    if (accFieldName.equals("id"))
                                                    {
                                                        log.info("Employee Id Added : "
                                                                + accEnt.get(accFieldName).asText());
                                                        empID = accEnt.get(accFieldName).asText();
                                                    }

                                                }

                                            }
                                        }

                                    }

                                }
                            }

                        }

                        catch (Exception e)
                        {
                            if (e != null)
                            {
                                log.error(e.getLocalizedMessage());
                            }
                        }

                    }
                }

                finally
                {

                    try
                    {
                        httpClient.close();
                    }
                    catch (IOException e)
                    {

                        log.error(e.getLocalizedMessage());
                    }

                }

            }

        }
        return empID;

    }

    @Override
    public List<TY_CaseESS> getCases4User(Ty_UserAccountEmployee userDetails, TY_DestinationProps desProps)
            throws IOException
    {
        List<TY_CaseESS> casesESSList = null;

        List<TY_CaseESS> casesESSList4User = null;

        try
        {
            if (StringUtils.hasText(userDetails.getAccountId()) || StringUtils.hasText(userDetails.getEmployeeId()))
            {

                JsonNode jsonNode = getAllCases(desProps);

                if (jsonNode != null && CollectionUtils.isNotEmpty(statusTransitions.getStatusTransitions()))
                {
                    List<TY_PortalStatusTransI> statusTransitionsList = statusTransitions.getStatusTransitions();
                    JsonNode rootNode = jsonNode.path("value");
                    if (rootNode != null)
                    {
                        log.info("Cases Bound!!");
                        casesESSList = new ArrayList<TY_CaseESS>();

                        Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                        while (payloadItr.hasNext())
                        {
                            // log.info("Payload Iterator Bound");
                            Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                            String payloadFieldName = payloadEnt.getKey();
                            // log.info("Payload Field Scanned: " + payloadFieldName);

                            if (payloadFieldName.equals("value"))
                            {
                                Iterator<JsonNode> casesItr = payloadEnt.getValue().elements();
                                // log.info("Cases Iterator Bound");
                                while (casesItr.hasNext())
                                {

                                    JsonNode caseEnt = casesItr.next();
                                    if (caseEnt != null)
                                    {
                                        String caseid = null, caseguid = null, caseType = null,
                                                caseTypeDescription = null, subject = null, status = null,
                                                createdOn = null, accountId = null, contactId = null, origin = null,
                                                updatedOn = null;
                                        boolean canConfirm = false;

                                        // log.info("Cases Entity Bound - Reading Case...");
                                        Iterator<String> fieldNames = caseEnt.fieldNames();
                                        while (fieldNames.hasNext())
                                        {
                                            String caseFieldName = fieldNames.next();
                                            // log.info("Case Entity Field Scanned: " + caseFieldName);
                                            if (caseFieldName.equals("id"))
                                            {
                                                // log.info("Case GUID Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    caseguid = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("displayId"))
                                            {
                                                // log.info("Case Id Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    caseid = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("caseType"))
                                            {
                                                // log.info("Case Type Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    caseType = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("caseTypeDescription"))
                                            {
                                                // log.info("Case Type Description Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    caseTypeDescription = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("subject"))
                                            {
                                                // log.info("Case Subject Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    subject = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("origin"))
                                            {
                                                // log.info("Case Subject Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    origin = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("statusDescription"))
                                            {
                                                // log.info("Case Status Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    status = caseEnt.get(caseFieldName).asText();
                                                    if (StringUtils.hasText(status))
                                                    {
                                                        String locStatus = status;
                                                        Optional<TY_PortalStatusTransI> transO = statusTransitionsList
                                                                .stream()
                                                                .filter(l -> l.getFromStatus().equals(locStatus))
                                                                .findFirst();
                                                        if (transO.isPresent())
                                                        {
                                                            canConfirm = transO.get().getConfirmAllowed();
                                                        }
                                                    }

                                                }
                                            }

                                            if (caseFieldName.equals("statusDescription"))
                                            {
                                                // log.info("Case Status Added : " +
                                                // caseEnt.get(caseFieldName).asText());
                                                if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                {
                                                    status = caseEnt.get(caseFieldName).asText();
                                                }
                                            }

                                            if (caseFieldName.equals("adminData"))
                                            {
                                                // log.info("Inside Admin Data: " );

                                                JsonNode admEnt = caseEnt.path("adminData");
                                                if (admEnt != null)
                                                {
                                                    // log.info("AdminData Node Bound");

                                                    Iterator<String> fieldNamesAdm = admEnt.fieldNames();
                                                    while (fieldNamesAdm.hasNext())
                                                    {
                                                        String admFieldName = fieldNamesAdm.next();
                                                        if (admFieldName.equals("createdOn"))
                                                        {
                                                            // log.info( "Created On : " +
                                                            // admEnt.get(admFieldName).asText());
                                                            createdOn = admEnt.get(admFieldName).asText();
                                                        }

                                                        if (admFieldName.equals("updatedOn"))
                                                        {
                                                            updatedOn = admEnt.get(admFieldName).asText();
                                                        }

                                                    }
                                                }

                                                if (caseFieldName.equals("account"))
                                                {
                                                    // log.info("Inside Account: " );

                                                    JsonNode accEnt = caseEnt.path("account");
                                                    if (accEnt != null)
                                                    {
                                                        // log.info("Account Node Bound");

                                                        Iterator<String> fieldNamesAcc = accEnt.fieldNames();
                                                        while (fieldNamesAcc.hasNext())
                                                        {
                                                            String accFieldName = fieldNamesAcc.next();
                                                            if (accFieldName.equals("id"))
                                                            {
                                                                // log.info(
                                                                // "Account ID : " + accEnt.get(accFieldName).asText());
                                                                accountId = accEnt.get(accFieldName).asText();
                                                            }
                                                        }

                                                    }
                                                }

                                                if (caseFieldName.equals("individualCustomer")
                                                        && (!StringUtils.hasText(accountId)))
                                                {
                                                    // log.info("Inside Account: " );

                                                    JsonNode accEnt = caseEnt.path("individualCustomer");
                                                    if (accEnt != null)
                                                    {
                                                        // log.info("Account Node Bound");

                                                        Iterator<String> fieldNamesAcc = accEnt.fieldNames();
                                                        while (fieldNamesAcc.hasNext())
                                                        {
                                                            String accFieldName = fieldNamesAcc.next();
                                                            if (accFieldName.equals("id"))
                                                            {
                                                                // log.info(
                                                                // "Account ID : " + accEnt.get(accFieldName).asText());
                                                                accountId = accEnt.get(accFieldName).asText();
                                                            }
                                                        }

                                                    }
                                                }

                                                if (caseFieldName.equals("reporter"))
                                                {
                                                    // log.info("Inside Reporter: " );

                                                    JsonNode repEnt = caseEnt.path("reporter");
                                                    if (repEnt != null)
                                                    {
                                                        // log.info("Reporter Node Bound");

                                                        Iterator<String> fieldNamesRep = repEnt.fieldNames();
                                                        while (fieldNamesRep.hasNext())
                                                        {
                                                            String repFieldName = fieldNamesRep.next();
                                                            if (repFieldName.equals("id"))
                                                            {
                                                                // log.info(
                                                                // "Reporter ID : " +
                                                                // repEnt.get(repFieldName).asText());
                                                                contactId = repEnt.get(repFieldName).asText();
                                                            }
                                                        }

                                                    }
                                                }

                                            }

                                            if (StringUtils.hasText(caseid) && StringUtils.hasText(caseguid))
                                            {
                                                if (StringUtils.hasText(createdOn))
                                                {
                                                    // Parse the date-time string into OffsetDateTime
                                                    OffsetDateTime odt = OffsetDateTime.parse(createdOn);
                                                    // Convert OffsetDateTime into Instant
                                                    Instant instant = odt.toInstant();
                                                    // If at all, you need java.util.Date
                                                    Date date = Date.from(instant);

                                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy");
                                                    String dateFormatted = sdf.format(date);

                                                    if (StringUtils.hasText(updatedOn))
                                                    {
                                                        // Parse the date-time string into OffsetDateTime
                                                        OffsetDateTime odtUpd = OffsetDateTime.parse(updatedOn);
                                                        // Convert OffsetDateTime into Instant
                                                        Instant instantUpd = odtUpd.toInstant();
                                                        // If at all, you need java.util.Date
                                                        Date dateUpd = Date.from(instantUpd);

                                                        SimpleDateFormat sdfUpd = new SimpleDateFormat("dd/M/yyyy");
                                                        String dateFormattedUpd = sdfUpd.format(dateUpd);

                                                        casesESSList.add(new TY_CaseESS(caseguid, caseid, caseType,
                                                                caseTypeDescription, subject, status, accountId,
                                                                contactId, createdOn, date, dateFormatted, odt,
                                                                updatedOn, dateUpd, dateFormattedUpd, odtUpd, origin,
                                                                canConfirm));
                                                    }
                                                    else
                                                    {

                                                        casesESSList.add(new TY_CaseESS(caseguid, caseid, caseType,
                                                                caseTypeDescription, subject, status, accountId,
                                                                contactId, createdOn, date, dateFormatted, odt, null,
                                                                null, null, null, origin, canConfirm));
                                                    }

                                                }
                                                else
                                                {
                                                    casesESSList.add(new TY_CaseESS(caseguid, caseid, caseType,
                                                            caseTypeDescription, subject, status, accountId, contactId,
                                                            null, null, null, null, null, null, null, null, origin,
                                                            canConfirm));
                                                }

                                            }
                                        }

                                    }

                                }

                            }

                        }
                    }

                }

            }
            else
            {
                return null;
            }

        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        /*
         * ------- FILTER FOR USER ACCOUNT or REPORTED BY CONTACT PERSON
         */

        if (!CollectionUtils.isEmpty(casesESSList))
        {
            casesESSList4User = casesESSList.stream().filter(e ->
            {
                // #ESMModule
                // If no Account Itself in Present in Case - Ignore Such Cases --Add Employee
                // with an and condition once ESM module is enabled
                if (!StringUtils.hasText(e.getAccountId()))
                {
                    return false;
                }

                if (StringUtils.hasText(e.getEmployeeId()))
                {

                    if (e.getAccountId().equals(userDetails.getAccountId()))
                    {
                        return true;
                    }

                }
                else
                {
                    if (e.getAccountId().equals(userDetails.getAccountId()))
                    {
                        return true;
                    }

                }
                return false;

            }).collect(Collectors.toList());

        }

        if (!CollectionUtils.isEmpty(casesESSList4User))
        {
            log.info("# Cases returned in call : " + casesESSList4User.size());
        }
        return casesESSList4User;
    }

    @Override
    public List<TY_KeyValue> getVHelpDDLB4Field(String fieldName, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {
        List<TY_KeyValue> vhlbDDLB = null;

        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String urlLink = null;
        try
        {
            if (StringUtils.hasText(fieldName) && StringUtils.hasText(srvCloudUrls.getVhlpUrl())
                    && StringUtils.hasText(srvCloudUrls.getToken()))

            {
                log.info("Invoking Value help for FieldName : " + fieldName);

                urlLink = srvCloudUrls.getVhlpUrl() + fieldName;

                HttpGet httpGet = new HttpGet(urlLink);

                httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpGet.addHeader("accept", "application/json");

                // Fire the Url
                response = httpClient.execute(httpGet);

                // verify the valid error code first
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK)
                {

                    if (statusCode == HttpStatus.SC_NOT_FOUND)
                    {
                        String msg = msgSrc.getMessage("ERR_VHLP_FLD_SRVCLOUD_NOTFOUND", new Object[]
                        { fieldName }, Locale.ENGLISH);
                        log.error(msg);
                        throw new EX_ESMAPI(msg);
                    }
                    else
                    {
                        String msg = msgSrc.getMessage("ERR_VHLP_FLD_SRVCLOUD_GEN", new Object[]
                        { fieldName, statusCode }, Locale.ENGLISH);
                        log.error(msg);
                        throw new EX_ESMAPI(msg);

                    }

                }

                // Try and Get Entity from Response
                HttpEntity entity = response.getEntity();
                String apiOutput = EntityUtils.toString(entity);
                // Lets see what we got from API
                // log.info(apiOutput);

                // Conerting to JSON
                ObjectMapper mapper = new ObjectMapper();
                jsonNode = mapper.readTree(apiOutput);

                if (jsonNode != null)
                {

                    JsonNode rootNode = jsonNode.path("value");
                    if (rootNode != null)
                    {
                        JsonNode contentNode = rootNode.at("/content");
                        if (contentNode != null && contentNode.isArray() && contentNode.size() > 0)
                        {
                            log.info("Values Bound for Value Help for Field -  " + fieldName);
                            vhlbDDLB = new ArrayList<TY_KeyValue>();
                            for (JsonNode arrayItem : contentNode)
                            {
                                String code = null, desc = null;
                                Boolean isActive = true;
                                Iterator<Entry<String, JsonNode>> fields = arrayItem.fields();
                                while (fields.hasNext())
                                {
                                    Entry<String, JsonNode> jsonField = fields.next();
                                    if (jsonField.getKey().equals("code"))
                                    {
                                        code = jsonField.getValue().asText();
                                    }

                                    if (jsonField.getKey().equals("description"))
                                    {
                                        desc = jsonField.getValue().asText();
                                    }

                                    if (jsonField.getKey().equals("active"))
                                    {
                                        isActive = jsonField.getValue().asBoolean();
                                    }

                                }

                                if (StringUtils.hasText(code) && StringUtils.hasText(desc) && isActive)
                                {
                                    TY_KeyValue keyVal = new TY_KeyValue(code, desc);
                                    vhlbDDLB.add(keyVal);
                                }
                            }
                        }

                    }

                }
            }
        }

        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_VHLP_FLD_SRVCLOUD_NOTFOUND", new Object[]
            { fieldName, e.getMessage() }, Locale.ENGLISH));

        }
        finally
        {
            httpClient.close();
        }

        return vhlbDDLB;
    }

    @Override
    public List<TY_CaseESS> getCases4User(Ty_UserAccountEmployee userDetails, EnumCaseTypes caseType,
            TY_DestinationProps desProps) throws IOException
    {

        List<TY_CaseESS> casesByCaseType = null;

        JsonNode jsonNode = null;
        HttpResponse response = null;
        String id = null, correctEncodedURL = null, urlLink = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        if (caseType != null && userDetails != null && caseTypeCus != null)
        {

            Optional<TY_CatgCusItem> cusItemO = caseTypeCus.getCustomizations().stream()
                    .filter(g -> g.getCaseTypeEnum().toString().equals(EnumCaseTypes.Learning.toString())).findFirst();
            if (cusItemO.isPresent())
            {

                try
                {

                    if (userDetails.isEmployee())
                    {
                        // Seek Cases for Employee Logged in
                        if (StringUtils.hasText(userDetails.getEmployeeId()))
                        {
                            id = userDetails.getEmployeeId();
                            urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getCasesByEmpl(), new String[]
                            { id, cusItemO.get().getCaseType() }, GC_Constants.gc_UrlReplParam);
                        }
                    }
                    else
                    {
                        // Seek Cases for Individual Customer Logged In
                        if (StringUtils.hasText(userDetails.getAccountId()))
                        {
                            id = userDetails.getAccountId();
                            urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getCasesByAcc(), new String[]
                            { id, cusItemO.get().getCaseType() }, GC_Constants.gc_UrlReplParam);

                        }
                    }

                    if (StringUtils.hasText(urlLink))
                    {
                        String encoding = null;

                        if (userDetails.isExternal())
                        {
                            encoding = Base64.getEncoder().encodeToString(
                                    (srvCloudUrls.getUserNameExt() + ":" + srvCloudUrls.getPasswordExt()).getBytes());
                        }
                        else
                        {
                            encoding = Base64.getEncoder().encodeToString(
                                    (srvCloudUrls.getUserName() + ":" + srvCloudUrls.getPassword()).getBytes());

                        }

                        try
                        {

                            URL url = new URL(urlLink);
                            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()),
                                    url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                            correctEncodedURL = uri.toASCIIString();

                            HttpGet httpGet = new HttpGet(correctEncodedURL);
                            httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                            httpGet.addHeader("accept", "application/json");
                            // Fire the Url
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
                            // Log.info(apiOutput);

                            // Conerting to JSON
                            ObjectMapper mapper = new ObjectMapper();
                            jsonNode = mapper.readTree(apiOutput);

                            if (jsonNode != null
                                    && CollectionUtils.isNotEmpty(statusTransitions.getStatusTransitions()))
                            {
                                List<TY_PortalStatusTransI> statusTransitionsList = statusTransitions
                                        .getStatusTransitions();
                                JsonNode rootNode = jsonNode.path("value");
                                if (rootNode != null)
                                {
                                    log.info("Cases Bound!!");
                                    casesByCaseType = new ArrayList<TY_CaseESS>();

                                    Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                    while (payloadItr.hasNext())
                                    {
                                        // log.info("Payload Iterator Bound");
                                        Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                        String payloadFieldName = payloadEnt.getKey();
                                        // log.info("Payload Field Scanned: " + payloadFieldName);

                                        if (payloadFieldName.equals("value"))
                                        {
                                            Iterator<JsonNode> casesItr = payloadEnt.getValue().elements();
                                            // log.info("Cases Iterator Bound");
                                            while (casesItr.hasNext())
                                            {

                                                JsonNode caseEnt = casesItr.next();
                                                if (caseEnt != null)
                                                {
                                                    String caseid = null, caseguid = null, caseTypeVar = null,
                                                            caseTypeDescription = null, subject = null, status = null,
                                                            createdOn = null, accountId = null, employeeId = null,
                                                            origin = null, updatedOn = null;

                                                    boolean canConfirm = false;

                                                    // log.info("Cases Entity Bound - Reading Case...");
                                                    Iterator<String> fieldNames = caseEnt.fieldNames();
                                                    while (fieldNames.hasNext())
                                                    {
                                                        String caseFieldName = fieldNames.next();
                                                        // log.info("Case Entity Field Scanned: " + caseFieldName);
                                                        if (caseFieldName.equals("id"))
                                                        {
                                                            // log.info("Case GUID Added : " +
                                                            // caseEnt.get(caseFieldName).asText());
                                                            if (StringUtils
                                                                    .hasText(caseEnt.get(caseFieldName).asText()))
                                                            {
                                                                caseguid = caseEnt.get(caseFieldName).asText();
                                                            }
                                                        }

                                                        if (caseFieldName.equals("displayId"))
                                                        {
                                                            // log.info("Case Id Added : " +
                                                            // caseEnt.get(caseFieldName).asText());
                                                            if (StringUtils
                                                                    .hasText(caseEnt.get(caseFieldName).asText()))
                                                            {
                                                                caseid = caseEnt.get(caseFieldName).asText();
                                                            }
                                                        }

                                                        if (caseFieldName.equals("caseType"))
                                                        {
                                                            // log.info("Case Type Added : " +
                                                            // caseEnt.get(caseFieldName).asText());
                                                            if (StringUtils
                                                                    .hasText(caseEnt.get(caseFieldName).asText()))
                                                            {
                                                                caseTypeVar = caseEnt.get(caseFieldName).asText();
                                                            }
                                                        }

                                                        if (caseFieldName.equals("caseTypeDescription"))
                                                        {
                                                            // log.info("Case Type Description Added : " +
                                                            // caseEnt.get(caseFieldName).asText());
                                                            if (StringUtils
                                                                    .hasText(caseEnt.get(caseFieldName).asText()))
                                                            {
                                                                caseTypeDescription = caseEnt.get(caseFieldName)
                                                                        .asText();
                                                            }
                                                        }

                                                        if (caseFieldName.equals("subject"))
                                                        {
                                                            // log.info("Case Subject Added : " +
                                                            // caseEnt.get(caseFieldName).asText());
                                                            if (StringUtils
                                                                    .hasText(caseEnt.get(caseFieldName).asText()))
                                                            {
                                                                subject = caseEnt.get(caseFieldName).asText();
                                                            }
                                                        }

                                                        if (caseFieldName.equals("origin"))
                                                        {
                                                            // log.info("Case Subject Added : " +
                                                            // caseEnt.get(caseFieldName).asText());
                                                            if (StringUtils
                                                                    .hasText(caseEnt.get(caseFieldName).asText()))
                                                            {
                                                                origin = caseEnt.get(caseFieldName).asText();
                                                            }
                                                        }

                                                        if (caseFieldName.equals("statusDescription"))
                                                        {
                                                            // log.info("Case Status Added : " +
                                                            // caseEnt.get(caseFieldName).asText());
                                                            if (StringUtils
                                                                    .hasText(caseEnt.get(caseFieldName).asText()))
                                                            {
                                                                status = caseEnt.get(caseFieldName).asText();
                                                                if (StringUtils.hasText(status))
                                                                {
                                                                    String locStatus = status;
                                                                    Optional<TY_PortalStatusTransI> transO = statusTransitionsList
                                                                            .stream().filter(l -> l.getFromStatus()
                                                                                    .equals(locStatus))
                                                                            .findFirst();
                                                                    if (transO.isPresent())
                                                                    {
                                                                        canConfirm = transO.get().getConfirmAllowed();
                                                                    }
                                                                }

                                                            }
                                                        }

                                                        if (caseFieldName.equals("statusDescription"))
                                                        {
                                                            // log.info("Case Status Added : " +
                                                            // caseEnt.get(caseFieldName).asText());
                                                            if (StringUtils
                                                                    .hasText(caseEnt.get(caseFieldName).asText()))
                                                            {
                                                                status = caseEnt.get(caseFieldName).asText();
                                                            }
                                                        }

                                                        if (caseFieldName.equals("adminData"))
                                                        {
                                                            // log.info("Inside Admin Data: " );

                                                            JsonNode admEnt = caseEnt.path("adminData");
                                                            if (admEnt != null)
                                                            {
                                                                // log.info("AdminData Node Bound");

                                                                Iterator<String> fieldNamesAdm = admEnt.fieldNames();
                                                                while (fieldNamesAdm.hasNext())
                                                                {
                                                                    String admFieldName = fieldNamesAdm.next();
                                                                    if (admFieldName.equals("createdOn"))
                                                                    {
                                                                        // log.info( "Created On : " +
                                                                        // admEnt.get(admFieldName).asText());
                                                                        createdOn = admEnt.get(admFieldName).asText();
                                                                    }

                                                                    if (admFieldName.equals("updatedOn"))
                                                                    {

                                                                        updatedOn = admEnt.get(admFieldName).asText();
                                                                    }
                                                                }

                                                            }
                                                        }

                                                        if (caseFieldName.equals("individualCustomer")
                                                                && (!StringUtils.hasText(accountId)))
                                                        {
                                                            // log.info("Inside Account: " );

                                                            JsonNode accEnt = caseEnt.path("individualCustomer");
                                                            if (accEnt != null)
                                                            {
                                                                // log.info("Account Node Bound");

                                                                Iterator<String> fieldNamesAcc = accEnt.fieldNames();
                                                                while (fieldNamesAcc.hasNext())
                                                                {
                                                                    String accFieldName = fieldNamesAcc.next();
                                                                    if (accFieldName.equals("id"))
                                                                    {
                                                                        // log.info(
                                                                        // "Account ID : " +
                                                                        // accEnt.get(accFieldName).asText());
                                                                        accountId = accEnt.get(accFieldName).asText();
                                                                    }
                                                                }

                                                            }
                                                        }

                                                        if (caseFieldName.equals("employee"))
                                                        {
                                                            // log.info("Inside Reporter: " );

                                                            JsonNode empEnt = caseEnt.path("employee");
                                                            if (empEnt != null)
                                                            {
                                                                // log.info("Reporter Node Bound");

                                                                Iterator<String> fieldNamesRep = empEnt.fieldNames();
                                                                while (fieldNamesRep.hasNext())
                                                                {
                                                                    String repFieldName = fieldNamesRep.next();
                                                                    if (repFieldName.equals("id"))
                                                                    {
                                                                        // log.info(
                                                                        // "Reporter ID : " +
                                                                        // repEnt.get(repFieldName).asText());
                                                                        employeeId = empEnt.get(repFieldName).asText();
                                                                    }
                                                                }

                                                            }
                                                        }

                                                    }

                                                    if (StringUtils.hasText(caseid) && StringUtils.hasText(caseguid))
                                                    {
                                                        if (StringUtils.hasText(createdOn))
                                                        {
                                                            // Parse the date-time string into OffsetDateTime
                                                            OffsetDateTime odt = OffsetDateTime.parse(createdOn);
                                                            // Convert OffsetDateTime into Instant
                                                            Instant instant = odt.toInstant();
                                                            // If at all, you need java.util.Date
                                                            Date date = Date.from(instant);

                                                            SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy");
                                                            String dateFormatted = sdf.format(date);

                                                            if (StringUtils.hasText(updatedOn))
                                                            {
                                                                // Parse the date-time string into OffsetDateTime
                                                                OffsetDateTime odtUpd = OffsetDateTime.parse(updatedOn);
                                                                // Convert OffsetDateTime into Instant
                                                                Instant instantUpd = odtUpd.toInstant();
                                                                // If at all, you need java.util.Date
                                                                Date dateUpd = Date.from(instantUpd);

                                                                SimpleDateFormat sdfUpd = new SimpleDateFormat(
                                                                        "dd/M/yyyy");
                                                                String dateFormattedUpd = sdfUpd.format(dateUpd);

                                                                casesByCaseType.add(new TY_CaseESS(caseguid, caseid,
                                                                        caseTypeVar, caseTypeDescription, subject,
                                                                        status, accountId, employeeId, createdOn, date,
                                                                        dateFormatted, odt, updatedOn, dateUpd,
                                                                        dateFormattedUpd, odtUpd, origin, canConfirm));
                                                            }
                                                            else
                                                            {

                                                                casesByCaseType.add(new TY_CaseESS(caseguid, caseid,
                                                                        caseTypeVar, caseTypeDescription, subject,
                                                                        status, accountId, employeeId, createdOn, date,
                                                                        dateFormatted, odt, null, null, null, null,
                                                                        origin, canConfirm));
                                                            }

                                                        }
                                                        else
                                                        {
                                                            casesByCaseType.add(new TY_CaseESS(caseguid, caseid,
                                                                    caseTypeVar, caseTypeDescription, subject, status,
                                                                    accountId, employeeId, null, null, null, null, null,
                                                                    null, null, null, origin, canConfirm));
                                                        }

                                                    }

                                                }

                                            }

                                        }

                                    }
                                }

                            }

                        }
                        catch (Exception e)
                        {
                            if (e != null)
                            {
                                log.error(e.getLocalizedMessage());
                            }
                        }
                    }

                }
                finally
                {

                    try
                    {
                        httpClient.close();
                    }
                    catch (IOException e)
                    {

                        log.error(e.getLocalizedMessage());
                    }

                }
            }
        }

        return casesByCaseType;
    }

    @Override
    public TY_CaseDetails getCaseDetails4Case(String caseId, TY_DestinationProps desProps) throws EX_ESMAPI, IOException
    {
        TY_CaseDetails caseDetails = null;
        if (StringUtils.hasText(caseId))
        {
            JsonNode jsonNode = null;
            HttpResponse response = null;
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            String urlLink = null;
            try
            {
                if (StringUtils.hasText(caseId) && StringUtils.hasText(srvCloudUrls.getCaseDetailsUrl())
                        && StringUtils.hasText(srvCloudUrls.getToken()))

                {
                    log.info("Fetching Details for Case ID : " + caseId);

                    urlLink = srvCloudUrls.getCaseDetailsUrl() + caseId;

                    HttpGet httpGet = new HttpGet(urlLink);

                    httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                    httpGet.addHeader("accept", "application/json");

                    // Fire the Url
                    response = httpClient.execute(httpGet);

                    // verify the valid error code first
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK)
                    {
                        String msg = msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                        { caseId }, Locale.ENGLISH);
                        log.error(msg);
                        throw new EX_ESMAPI(msg);
                    }

                    // Try and Get Entity from Response
                    HttpEntity entity = response.getEntity();
                    String apiOutput = EntityUtils.toString(entity);
                    // Lets see what we got from API
                    // log.info(apiOutput);

                    // Get Response Header(s) from API REsponse
                    Header[] headers = response.getAllHeaders();
                    String eTag = null;
                    if (headers.length > 0)
                    {
                        // Get the Etag
                        Optional<Header> etagO = Arrays.asList(headers).stream()
                                .filter(e -> e.getName().equals(GC_Constants.gc_ETag)).findFirst();
                        if (etagO.isPresent())
                        {
                            eTag = etagO.get().getValue();
                        }
                    }

                    // Conerting to JSON
                    ObjectMapper mapper = new ObjectMapper();
                    jsonNode = mapper.readTree(apiOutput);

                    if (jsonNode != null)
                    {

                        JsonNode rootNode = jsonNode.path("value");
                        if (rootNode != null)
                        {
                            caseDetails = new TY_CaseDetails();
                            caseDetails.setCaseGuid(caseId);
                            caseDetails.setETag(eTag);
                            caseDetails.setNotes(new ArrayList<TY_NotesDetails>());

                            // Add Description
                            JsonNode descNode = rootNode.at("/description");
                            if (descNode != null && descNode.size() > 0)
                            {
                                log.info("Desc for Case ID : " + caseId + " bound..");

                                Iterator<String> fieldNamesDesc = descNode.fieldNames();
                                String content = null, noteType = null, userCreate = null, timestamp = null, id = null,
                                        noteId = null;
                                OffsetDateTime odt = null;
                                boolean agentNote = false;
                                while (fieldNamesDesc.hasNext())
                                {
                                    String descFieldName = fieldNamesDesc.next();
                                    if (descFieldName.equals("id"))
                                    {
                                        id = descNode.get(descFieldName).asText();
                                    }

                                    if (descFieldName.equals("noteId"))
                                    {
                                        noteId = descNode.get(descFieldName).asText();
                                    }

                                    if (descFieldName.equals("content"))
                                    {
                                        content = descNode.get(descFieldName).asText();
                                    }

                                    if (descFieldName.equals("noteType"))
                                    {
                                        noteType = descNode.get(descFieldName).asText();
                                    }

                                    if (descFieldName.equals("adminData"))
                                    {
                                        // log.info("Inside Reporter: " );

                                        JsonNode admEnt = descNode.path("adminData");
                                        if (admEnt != null)
                                        {
                                            // log.info("Reporter Node Bound");

                                            Iterator<String> fieldNamesAdm = admEnt.fieldNames();
                                            while (fieldNamesAdm.hasNext())
                                            {
                                                String admFieldName = fieldNamesAdm.next();
                                                if (admFieldName.equals("createdOn"))
                                                {
                                                    if (StringUtils.hasText(admEnt.get(admFieldName).asText()))
                                                    {

                                                        timestamp = admEnt.get(admFieldName).asText();
                                                        // Parse the date-time string into OffsetDateTime
                                                        odt = OffsetDateTime.parse(timestamp);
                                                    }
                                                }

                                                if (admFieldName.equals("createdByName"))
                                                {

                                                    if (StringUtils.hasText(admEnt.get(admFieldName).asText()))
                                                    {
                                                        userCreate = admEnt.get(admFieldName).asText();
                                                        if (!userCreate.startsWith(rlConfig.getTechUserRegex()))
                                                        {
                                                            agentNote = true;
                                                        }
                                                    }
                                                }

                                            }

                                        }
                                    }

                                }
                                TY_NotesDetails newNote = new TY_NotesDetails(noteType, id, noteId, odt, userCreate,
                                        content, agentNote);
                                caseDetails.getNotes().add(newNote);

                            }

                        }
                    }

                }
            }
            catch (Exception e)
            {
                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                { caseId, e.getMessage() }, Locale.ENGLISH));

            }
            finally
            {
                httpClient.close();
            }

        }
        return caseDetails;
    }

    @Override
    public List<TY_StatusCfgItem> getStatusCfg4StatusSchema(String StatusSchema, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {
        List<TY_StatusCfgItem> userStatusAssignments = null;
        if (StringUtils.hasText(StatusSchema) && StringUtils.hasText(srvCloudUrls.getStatusSchemaUrl())
                && StringUtils.hasText(srvCloudUrls.getToken()))
        {

            JsonNode jsonNode = null;
            HttpResponse response = null;
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            String urlLink = null;
            try
            {

                log.info("Fetching Details for Status Schema: " + StatusSchema);

                urlLink = srvCloudUrls.getStatusSchemaUrl() + StatusSchema;

                HttpGet httpGet = new HttpGet(urlLink);

                httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpGet.addHeader("accept", "application/json");

                // Fire the Url
                response = httpClient.execute(httpGet);

                // verify the valid error code first
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK)
                {
                    String msg = msgSrc.getMessage("ERR_INVALID_SCHEMA", new Object[]
                    { StatusSchema }, Locale.ENGLISH);
                    log.error(msg);
                    throw new EX_ESMAPI(msg);
                }

                // Try and Get Entity from Response
                HttpEntity entity = response.getEntity();
                String apiOutput = EntityUtils.toString(entity);
                // Lets see what we got from API
                // log.info(apiOutput);

                // Conerting to JSON
                ObjectMapper mapper = new ObjectMapper();
                jsonNode = mapper.readTree(apiOutput);

                if (jsonNode != null)
                {

                    JsonNode rootNode = jsonNode.path("value");
                    if (rootNode != null)
                    {
                        userStatusAssignments = new ArrayList<TY_StatusCfgItem>();

                        JsonNode contentNode = rootNode.at("/userStatusAssignments");
                        if (contentNode != null && contentNode.isArray() && contentNode.size() > 0)
                        {
                            log.info("Status for Schema : " + StatusSchema + " bound..");
                            for (JsonNode arrayItem : contentNode)
                            {
                                String userStatus = null, userStatusDescription = null;

                                Iterator<Entry<String, JsonNode>> fields = arrayItem.fields();
                                while (fields.hasNext())
                                {
                                    Entry<String, JsonNode> jsonField = fields.next();
                                    if (jsonField.getKey().equals("userStatus"))
                                    {
                                        userStatus = jsonField.getValue().asText();
                                    }

                                    if (jsonField.getKey().equals("userStatusDescription"))
                                    {
                                        userStatusDescription = jsonField.getValue().asText();
                                    }

                                }
                                userStatusAssignments.add(new TY_StatusCfgItem(userStatus, userStatusDescription));

                            }

                        }

                    }

                }
            }
            catch (Exception e)
            {
                throw new EX_ESMAPI(msgSrc.getMessage("ERR_INVALID_SCHEMA", new Object[]
                { StatusSchema, e.getMessage() }, Locale.ENGLISH));

            }
            finally
            {
                httpClient.close();
            }

        }

        return userStatusAssignments;
    }

    @Override
    public boolean updateCasewithReply(TY_CasePatchInfo patchInfo, TY_Case_SrvCloud_Reply caseReply,
            TY_DestinationProps desProps) throws EX_ESMAPI, IOException
    {
        boolean caseUpdated = false;
        if (caseReply != null && patchInfo != null)
        {
            if (StringUtils.hasText(patchInfo.getCaseGuid()) && StringUtils.hasText(patchInfo.getETag()))
            {
                HttpClient httpclient = HttpClients.createDefault();
                String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCaseDetailsUrl());
                if (StringUtils.hasText(casePOSTURL))
                {
                    String encoding = null;
                    casePOSTURL = casePOSTURL + patchInfo.getCaseGuid();

                    if (caseReply.isExternal())
                    {
                        encoding = Base64.getEncoder().encodeToString(
                                (srvCloudUrls.getUserNameExt() + ":" + srvCloudUrls.getPasswordExt()).getBytes());
                    }
                    else
                    {
                        encoding = Base64.getEncoder().encodeToString(
                                (srvCloudUrls.getUserName() + ":" + srvCloudUrls.getPassword()).getBytes());

                    }

                    HttpPatch httpPatch = new HttpPatch(casePOSTURL);
                    httpPatch.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
                    httpPatch.addHeader("Content-Type", "application/json");
                    httpPatch.addHeader(GC_Constants.gc_IFMatch, patchInfo.getETag());

                    // Remove Description Note Type from Payload before Persisting
                    // Important as the Description or Default text Type Should not be persisted
                    // alongwith Note(s)
                    if (CollectionUtils.isNotEmpty(caseReply.getNotes()))
                    {
                        // Remove null Note Type Note(s)
                        caseReply.getNotes().removeIf(n -> n.getNoteType() == null);
                        caseReply.getNotes()
                                .removeIf(n -> n.getNoteType().equalsIgnoreCase(GC_Constants.gc_NoteTypeDescription));
                        caseReply.getNotes()
                                .removeIf(n -> n.getNoteType().equalsIgnoreCase(GC_Constants.gc_DescNoteType));

                    }

                    ObjectMapper objMapper = new ObjectMapper();

                    String requestBody = objMapper.writeValueAsString(caseReply);
                    log.info(requestBody);

                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPatch.setEntity(entity);

                    // PATCH Case in Service Cloud
                    try
                    {
                        // Fire the Url
                        HttpResponse response = httpclient.execute(httpPatch);
                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_OK)
                        {
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);
                            log.error(apiOutput);
                            // Error Updating Case id - {0}. HTTP Status - {1}. Details : {2}.
                            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_REPLY_UPDATE", new Object[]
                            { patchInfo.getCaseId(), statusCode, apiOutput }, Locale.ENGLISH));

                        }
                        else
                        {
                            caseUpdated = true;
                        }

                    }
                    catch (IOException e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }
                }

            }

        }
        return caseUpdated;
    }

    @Override
    public String createCase4Employee(TY_Case_Employee_SrvCloud caseEntity, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {
        String caseId = null;

        if (StringUtils.hasText(caseEntity.getEmployee().getId()))
        {
            HttpClient httpclient = HttpClients.createDefault();
            String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCasesUrl());
            if (StringUtils.hasText(casePOSTURL))
            {

                HttpPost httpPost = new HttpPost(casePOSTURL);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpPost.addHeader("Content-Type", "application/json");

                ObjectMapper objMapper = new ObjectMapper();
                try
                {
                    String requestBody = objMapper.writeValueAsString(caseEntity);
                    log.info(requestBody);

                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(entity);

                    // POST Case in Service Cloud
                    try
                    {
                        // Fire the Url
                        HttpResponse response = httpclient.execute(httpPost);
                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_CREATED)
                        {
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);
                            log.info(apiOutput);
                            throw new RuntimeException(
                                    "Failed with HTTP error code : " + statusCode + " Details: " + apiOutput);

                        }

                        // Try and Get Entity from Response
                        HttpEntity entityResp = response.getEntity();
                        String apiOutput = EntityUtils.toString(entityResp);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(apiOutput);

                        if (jsonNode != null)
                        {

                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null)
                            {

                                log.info("Notes Bound!!");

                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    log.info("Payload Iterator Bound");
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    log.info("Payload Field Scanned:  " + payloadFieldName);

                                    if (payloadFieldName.equals("value"))
                                    {
                                        JsonNode caseEnt = payloadEnt.getValue();
                                        log.info("New Case Entity Bound");
                                        if (caseEnt != null)
                                        {

                                            log.info("Case Entity Bound - Reading Case...");
                                            Iterator<String> fieldNames = caseEnt.fieldNames();
                                            while (fieldNames.hasNext())
                                            {
                                                String caseFieldName = fieldNames.next();
                                                log.info("Case Entity Field Scanned:  " + caseFieldName);
                                                if (caseFieldName.equals("displayId"))
                                                {
                                                    log.info("Case ID Added : " + caseEnt.get(caseFieldName).asText());
                                                    if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                    {
                                                        caseId = caseEnt.get(caseFieldName).asText();

                                                    }
                                                    break;
                                                }

                                            }

                                        }

                                    }

                                }
                            }
                        }

                    }
                    catch (IOException e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }
                }
                catch (JsonProcessingException e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_NOTES_JSON", new Object[]
                    { e.getLocalizedMessage() }, Locale.ENGLISH));
                }

            }

        }

        return caseId;
    }

    @Override
    public String createCase4Customer(TY_Case_Customer_SrvCloud caseEntity, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {
        String caseId = null;

        if (StringUtils.hasText(caseEntity.getAccount().getId()))
        {
            HttpClient httpclient = HttpClients.createDefault();
            String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCasesUrl());
            if (StringUtils.hasText(casePOSTURL))
            {

                HttpPost httpPost = new HttpPost(casePOSTURL);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpPost.addHeader("Content-Type", "application/json");

                ObjectMapper objMapper = new ObjectMapper();
                try
                {
                    String requestBody = objMapper.writeValueAsString(caseEntity);
                    log.info(requestBody);

                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(entity);

                    // POST Case in Service Cloud
                    try
                    {
                        // Fire the Url
                        HttpResponse response = httpclient.execute(httpPost);
                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_CREATED)
                        {
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);
                            log.info(apiOutput);
                            throw new RuntimeException(
                                    "Failed with HTTP error code : " + statusCode + " Details: " + apiOutput);

                        }

                        // Try and Get Entity from Response
                        HttpEntity entityResp = response.getEntity();
                        String apiOutput = EntityUtils.toString(entityResp);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(apiOutput);

                        if (jsonNode != null)
                        {

                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null)
                            {

                                log.info("Notes Bound!!");

                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    log.info("Payload Iterator Bound");
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    log.info("Payload Field Scanned:  " + payloadFieldName);

                                    if (payloadFieldName.equals("value"))
                                    {
                                        JsonNode caseEnt = payloadEnt.getValue();
                                        log.info("New Case Entity Bound");
                                        if (caseEnt != null)
                                        {

                                            log.info("Case Entity Bound - Reading Case...");
                                            Iterator<String> fieldNames = caseEnt.fieldNames();
                                            while (fieldNames.hasNext())
                                            {
                                                String caseFieldName = fieldNames.next();
                                                log.info("Case Entity Field Scanned:  " + caseFieldName);
                                                if (caseFieldName.equals("displayId"))
                                                {
                                                    log.info("Case ID Added : " + caseEnt.get(caseFieldName).asText());
                                                    if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                    {
                                                        caseId = caseEnt.get(caseFieldName).asText();

                                                    }
                                                    break;
                                                }

                                            }

                                        }

                                    }

                                }
                            }
                        }

                    }
                    catch (IOException e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }
                }
                catch (JsonProcessingException e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_NOTES_JSON", new Object[]
                    { e.getLocalizedMessage() }, Locale.ENGLISH));
                }

            }

        }

        return caseId;
    }

    @Override
    public List<TY_PreviousAttachments> getAttachments4Case(String caseGuid, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {
        List<TY_PreviousAttachments> prevAtt = null;

        JsonNode jsonNode = null;
        HttpResponse response = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        final String urlAttrib = "url";

        if (StringUtils.hasText(caseGuid) && StringUtils.hasText(srvCloudUrls.getPrevAtt())
                && StringUtils.hasText(srvCloudUrls.getDlAtt()) && StringUtils.hasText(srvCloudUrls.getToken()))

        {
            log.info("Fetching Attachments for Case GUID : " + caseGuid);

            String urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getPrevAtt(), new String[]
            { caseGuid }, GC_Constants.gc_UrlReplParam);

            if (StringUtils.hasText(urlLink))
            {

                try
                {

                    URL url = new URL(urlLink);
                    URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(),
                            url.getPath(), url.getQuery(), url.getRef());
                    String correctEncodedURL = uri.toASCIIString();

                    HttpGet httpGet = new HttpGet(correctEncodedURL);
                    httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                    httpGet.addHeader("accept", "application/json");
                    // Fire the Url
                    response = httpClient.execute(httpGet);

                    // verify the valid error code first
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK)
                    {

                        HttpEntity entity = response.getEntity();
                        String apiOutput = EntityUtils.toString(entity);

                        ObjectMapper mapper = new ObjectMapper();
                        jsonNode = mapper.readTree(apiOutput);
                        if (jsonNode != null)
                        {

                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null && rootNode.size() > 0)
                            {
                                log.info("Attachments Bound for Case Guid - " + caseGuid + " : " + rootNode.size());
                                prevAtt = new ArrayList<TY_PreviousAttachments>();

                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    // log.info("Payload Iterator Bound");
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    // log.info("Payload Field Scanned: " + payloadFieldName);

                                    if (payloadFieldName.equals("value"))
                                    {
                                        Iterator<JsonNode> attItr = payloadEnt.getValue().elements();
                                        // log.info("Cases Iterator Bound");
                                        while (attItr.hasNext())
                                        {

                                            JsonNode attEnt = attItr.next();
                                            if (attEnt != null)
                                            {
                                                boolean byTechnicalUser = false;
                                                long fileSize = 0;
                                                String id = null, title = null, createdByName = null, createdOn = null,
                                                        dateFormatted = null, urlAtt = null, type = null;

                                                // log.info("Cases Entity Bound - Reading Case...");
                                                Iterator<String> fieldNames = attEnt.fieldNames();
                                                while (fieldNames.hasNext())
                                                {
                                                    String attFieldName = fieldNames.next();
                                                    // log.info("Case Entity Field Scanned: " + caseFieldName);
                                                    if (attFieldName.equals("id"))
                                                    {

                                                        if (StringUtils.hasText(attEnt.get(attFieldName).asText()))
                                                        {
                                                            id = attEnt.get(attFieldName).asText();
                                                        }
                                                    }

                                                    if (attFieldName.equals("title"))
                                                    {
                                                        // log.info("Case Id Added : " +
                                                        // caseEnt.get(caseFieldName).asText());
                                                        if (StringUtils.hasText(attEnt.get(attFieldName).asText()))
                                                        {
                                                            title = attEnt.get(attFieldName).asText();
                                                        }
                                                    }

                                                    if (attFieldName.equals("type"))
                                                    {
                                                        // log.info("Case Type Added : " +
                                                        // caseEnt.get(caseFieldName).asText());
                                                        if (StringUtils.hasText(attEnt.get(attFieldName).asText()))
                                                        {
                                                            type = attEnt.get(attFieldName).asText();
                                                        }
                                                    }

                                                    if (attFieldName.equals("fileSize"))
                                                    {
                                                        // log.info("Case Type Added : " +
                                                        // caseEnt.get(caseFieldName).asText());
                                                        if (StringUtils.hasText(attEnt.get(attFieldName).asText()))
                                                        {
                                                            fileSize = attEnt.get(attFieldName).asLong() / (1024);
                                                        }
                                                    }

                                                    if (attFieldName.equals("adminData"))
                                                    {
                                                        // log.info("Inside Admin Data: " );

                                                        JsonNode admEnt = attEnt.path("adminData");
                                                        if (admEnt != null)
                                                        {
                                                            // log.info("AdminData Node Bound");

                                                            Iterator<String> fieldNamesAdm = admEnt.fieldNames();
                                                            while (fieldNamesAdm.hasNext())
                                                            {
                                                                String admFieldName = fieldNamesAdm.next();
                                                                if (admFieldName.equals("createdOn"))
                                                                {

                                                                    createdOn = admEnt.get(admFieldName).asText();

                                                                    // Parse the date-time string into OffsetDateTime
                                                                    OffsetDateTime odt = OffsetDateTime
                                                                            .parse(createdOn);
                                                                    // Convert OffsetDateTime into Instant
                                                                    Instant instant = odt.toInstant();
                                                                    // If at all, you need java.util.Date
                                                                    Date date = Date.from(instant);

                                                                    SimpleDateFormat sdf = new SimpleDateFormat(
                                                                            "dd/M/yyyy");
                                                                    dateFormatted = sdf.format(date);
                                                                }

                                                                if (admFieldName.equals("createdByName"))
                                                                {
                                                                    createdByName = admEnt.get(admFieldName).asText();
                                                                    if (StringUtils
                                                                            .hasText(rlConfig.getTechUserRegex()))
                                                                    {
                                                                        if (createdByName.startsWith(
                                                                                rlConfig.getTechUserRegex()))
                                                                        {
                                                                            byTechnicalUser = true;
                                                                        }

                                                                    }
                                                                }
                                                            }

                                                        }
                                                    }

                                                }

                                                if (StringUtils.hasText(id) && StringUtils.hasText(title)
                                                        && fileSize > 0)
                                                {
                                                    if (type == null)
                                                    {
                                                        prevAtt.add(new TY_PreviousAttachments(id, title, fileSize,
                                                                createdByName, dateFormatted, byTechnicalUser, null));
                                                    }
                                                    else if (StringUtils.hasText(type))
                                                    {
                                                        if (!type.equals(GC_Constants.gc_AttachmentTypeInternal))
                                                        {
                                                            prevAtt.add(new TY_PreviousAttachments(id, title, fileSize,
                                                                    createdByName, dateFormatted, byTechnicalUser,
                                                                    null));
                                                        }
                                                    }

                                                }

                                            }

                                        }

                                    }

                                }
                            }

                        }

                    }

                }
                catch (Exception e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                    { caseGuid, e.getMessage() }, Locale.ENGLISH));

                }
                finally
                {
                    httpClient.close();
                }

                if (CollectionUtils.isNotEmpty(prevAtt))
                {
                    for (TY_PreviousAttachments attDet : prevAtt)
                    {
                        // Get Attachment GUID and Generate S3 Link for D/l
                        httpClient = HttpClientBuilder.create().build();
                        urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getDlAtt(), new String[]
                        { attDet.getId() }, GC_Constants.gc_UrlReplParam);

                        if (StringUtils.hasText(urlLink) && StringUtils.hasText(srvCloudUrls.getToken()))
                        {

                            try
                            {

                                URL url = new URL(urlLink);
                                URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()),
                                        url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                                String correctEncodedURL = uri.toASCIIString();

                                HttpGet httpGet = new HttpGet(correctEncodedURL);
                                httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                                httpGet.addHeader("accept", "application/json");
                                // Fire the Url
                                response = httpClient.execute(httpGet);

                                // verify the valid error code first
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == HttpStatus.SC_OK)
                                {
                                    // update attDet
                                    HttpEntity entity = response.getEntity();
                                    String apiOutput = EntityUtils.toString(entity);

                                    ObjectMapper mapper = new ObjectMapper();
                                    jsonNode = mapper.readTree(apiOutput);
                                    if (jsonNode != null)
                                    {

                                        Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                        while (payloadItr.hasNext())
                                        {
                                            // log.info("Payload Iterator Bound");
                                            Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                            String payloadFieldName = payloadEnt.getKey();
                                            // log.info("Payload Field Scanned: " + payloadFieldName);

                                            if (payloadFieldName.equals("value"))
                                            {
                                                JsonNode contentNode = payloadEnt.getValue();
                                                if (contentNode != null)
                                                {
                                                    String attDLUrl = contentNode.get(urlAttrib).asText();
                                                    if (StringUtils.hasText(attDLUrl))
                                                    {
                                                        attDet.setUrl(attDLUrl);
                                                    }

                                                }
                                            }
                                        }

                                    }
                                }
                            }

                            catch (Exception e)
                            {
                                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                                { caseGuid, e.getMessage() }, Locale.ENGLISH));

                            }
                            finally
                            {
                                httpClient.close();
                            }
                        }
                    }
                }
            }
        }

        return prevAtt;
    }

    @Override
    public List<TY_NotesDetails> getFormattedNotes4Case(String caseId, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {
        List<TY_NotesDetails> formattedExternalNotes = new ArrayList<TY_NotesDetails>();
        if (StringUtils.hasText(caseId))
        {
            JsonNode jsonNode = null;
            HttpResponse response = null;
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            String urlLink = null;

            if (StringUtils.hasText(caseId) && StringUtils.hasText(srvCloudUrls.getNotesReadUrl()))

            {
                log.info("Fetching External Notes for Case ID : " + caseId);
                try
                {
                    urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getNotesReadUrl(), new String[]
                    { caseId }, GC_Constants.gc_UrlReplParam);

                    if (StringUtils.hasText(urlLink) && StringUtils.hasText(srvCloudUrls.getToken()))
                    {

                        HttpGet httpGet = new HttpGet(urlLink);

                        httpGet.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                        httpGet.addHeader("accept", "application/json");

                        // Fire the Url
                        response = httpClient.execute(httpGet);

                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_OK)
                        {
                            String msg = msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                            { caseId }, Locale.ENGLISH);
                            log.error(msg);
                            throw new EX_ESMAPI(msg);
                        }

                        // Try and Get Entity from Response
                        HttpEntity entity = response.getEntity();
                        String apiOutput = EntityUtils.toString(entity);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        jsonNode = mapper.readTree(apiOutput);

                        if (jsonNode != null)
                        {
                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null && rootNode.isArray() && rootNode.size() > 0)
                            {
                                log.info("Notes for Case ID : " + caseId + " bound..");
                            }
                        }

                    }

                }
                catch (Exception e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_NOTESF_FETCH", new Object[]
                    { caseId, e.getMessage() }, Locale.ENGLISH));

                }
                finally
                {
                    httpClient.close();
                }

            }

        }

        return formattedExternalNotes;
    }

    @Override
    public ResponseEntity<List<String>> getAllowedAttachmentTypes(TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {

        List<String> allowedAttachments = null;
        if (desProps != null && srvCloudUrls != null)
        {

            if (StringUtils.hasText(desProps.getAuthToken()))
            {
                JsonNode jsonNode = null;
                HttpResponse response = null;
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                String urlLink = null;

                try
                {

                    if (StringUtils.hasLength(srvCloudUrls.getMimeTypesUrlPathString()))
                    {
                        log.info("Url and Credentials Found!!");

                        urlLink = CL_URLUtility.getUrl4DestinationAPI(srvCloudUrls.getMimeTypesUrlPathString(),
                                desProps.getBaseUrl());
                        // Query URL Encoding to avoid Illegal character error in Query
                        URL url = new URL(urlLink);
                        URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()),
                                url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                        String correctEncodedURL = uri.toASCIIString();

                        HttpGet httpGet = new HttpGet(correctEncodedURL);

                        httpGet.setHeader(HttpHeaders.AUTHORIZATION, desProps.getAuthToken());
                        httpGet.addHeader("accept", "application/json");

                        // Fire the Url
                        response = httpClient.execute(httpGet);

                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != org.apache.http.HttpStatus.SC_OK)
                        {

                            if (statusCode == org.apache.http.HttpStatus.SC_NOT_FOUND)
                            {

                                // ERR_MIME_TYPES_API=Error Reading Allowed Mime Type Value(s) from SDocument
                                // Service API. Details - {0}.
                                throw new EX_ESMAPI(msgSrc.getMessage("ERR_MIME_TYPES_API", new Object[]
                                { "Not FOUND any Status Values" }, Locale.ENGLISH));
                            }
                            else
                            {
                                throw new EX_ESMAPI(msgSrc.getMessage("ERR_MIME_TYPES_APII", new Object[]
                                { statusCode }, Locale.ENGLISH));
                            }

                        }

                        // Try and Get Entity from Response
                        HttpEntity entity = response.getEntity();
                        String apiOutput = EntityUtils.toString(entity);
                        // Lets see what we got from API
                        // log.info(apiOutput);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        jsonNode = mapper.readTree(apiOutput);

                        if (jsonNode != null)
                        {
                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null)
                            {
                                log.info("Customizing Bound!!");

                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {

                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();

                                    if (payloadFieldName.equals("value"))
                                    {
                                        allowedAttachments = new ArrayList<String>();
                                        Iterator<JsonNode> cusItr = payloadEnt.getValue().elements();
                                        // log.info("Cases Iterator Bound");
                                        while (cusItr.hasNext())
                                        {

                                            JsonNode cusEnt = cusItr.next();
                                            if (cusEnt != null)
                                            {
                                                boolean isAllowed = false;

                                                Iterator<String> fieldNames = cusEnt.fieldNames();
                                                while (fieldNames.hasNext())
                                                {
                                                    String cusFieldName = fieldNames.next();
                                                    if (cusFieldName.equals("isAllowed"))
                                                    {
                                                        if (cusEnt.get(cusFieldName).asBoolean())
                                                        {
                                                            isAllowed = cusEnt.get(cusFieldName).asBoolean();
                                                        }

                                                    }

                                                    if (isAllowed)
                                                    {

                                                        JsonNode contentNode = cusEnt.at("/fileExtensions");
                                                        if (contentNode != null && contentNode.isArray()
                                                                && contentNode.size() > 0)
                                                        {
                                                            log.info("File Extensions Bound");

                                                            for (JsonNode arrayItem : contentNode)
                                                            {
                                                                String attType = null;

                                                                Iterator<Entry<String, JsonNode>> fields = arrayItem
                                                                        .fields();
                                                                while (fields.hasNext())
                                                                {
                                                                    Entry<String, JsonNode> jsonField = fields.next();
                                                                    if (jsonField.getKey().equals("name"))
                                                                    {
                                                                        attType = jsonField.getValue().asText();
                                                                        allowedAttachments.add(attType);
                                                                    }

                                                                }

                                                            }

                                                        }

                                                    }

                                                }

                                            }
                                        }
                                    }
                                }

                            }

                        }
                    }
                }

                catch (Exception e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_STATUS_API", new Object[]
                    { e.getMessage() }, Locale.ENGLISH));

                }
                finally
                {
                    httpClient.close();
                }
            }

        }

        return new ResponseEntity<>(allowedAttachments, org.springframework.http.HttpStatus.OK);
    }

    @Override
    public boolean confirmCase(TY_CaseConfirmPOJO caseDetails) throws EX_ESMAPI, IOException
    {
        boolean caseConfirmed = false;
        if (caseDetails != null)
        {
            if (StringUtils.hasText(caseDetails.getETag()) && StringUtils.hasText(caseDetails.getCaseGuid())
                    && StringUtils.hasText(caseDetails.getCnfStatusCode()))
            {

                HttpClient httpclient = HttpClients.createDefault();
                String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCaseDetailsUrl()) + caseDetails.getCaseGuid();
                if (StringUtils.hasText(casePOSTURL))
                {
                    HttpPatch httpPatch = new HttpPatch(casePOSTURL);
                    httpPatch.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                    httpPatch.addHeader("Content-Type", "application/json");
                    httpPatch.addHeader(GC_Constants.gc_IFMatch, caseDetails.getETag());
                    ObjectMapper objMapper = new ObjectMapper();

                    TY_Case_SrvCloud_Confirm caseConfirmPayload = new TY_Case_SrvCloud_Confirm(
                            caseDetails.getCnfStatusCode());

                    String requestBody = objMapper.writeValueAsString(caseConfirmPayload);
                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPatch.setEntity(entity);

                    // PATCH Case in Service Cloud
                    try
                    {
                        // Fire the Url
                        HttpResponse response = httpclient.execute(httpPatch);
                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_OK)
                        {
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);
                            log.error(apiOutput);
                            // ERR_CASE_CONFIRM= Error Confirming Case id - {0}. HTTP Status - {1}. Details
                            // : {2}.
                            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_CONFIRM", new Object[]
                            { caseDetails.getCaseId(), statusCode, apiOutput }, Locale.ENGLISH));

                        }
                        else
                        {
                            caseConfirmed = true;
                        }

                    }
                    catch (Exception e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_CONFIRM", new Object[]
                        { caseDetails.getCaseId(), HttpStatus.SC_EXPECTATION_FAILED, e.getLocalizedMessage() },
                                Locale.ENGLISH));
                    }

                }

            }
        }

        return caseConfirmed;

    }

    @Override
    public String createCase4EmployeeAddUser(TY_Case_EmployeeAddUser_SrvCloud caseEntity, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {
        String caseId = null;

        if (StringUtils.hasText(caseEntity.getEmployee().getId()))
        {
            HttpClient httpclient = HttpClients.createDefault();
            String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCasesUrl());
            if (StringUtils.hasText(casePOSTURL))
            {

                HttpPost httpPost = new HttpPost(casePOSTURL);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpPost.addHeader("Content-Type", "application/json");

                ObjectMapper objMapper = new ObjectMapper();
                try
                {
                    String requestBody = objMapper.writeValueAsString(caseEntity);
                    log.info(requestBody);

                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(entity);

                    // POST Case in Service Cloud
                    try
                    {
                        // Fire the Url
                        HttpResponse response = httpclient.execute(httpPost);
                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_CREATED)
                        {
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);
                            log.info(apiOutput);
                            throw new RuntimeException(
                                    "Failed with HTTP error code : " + statusCode + " Details: " + apiOutput);

                        }

                        // Try and Get Entity from Response
                        HttpEntity entityResp = response.getEntity();
                        String apiOutput = EntityUtils.toString(entityResp);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(apiOutput);

                        if (jsonNode != null)
                        {

                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null)
                            {

                                log.info("Notes Bound!!");

                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    log.info("Payload Iterator Bound");
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    log.info("Payload Field Scanned:  " + payloadFieldName);

                                    if (payloadFieldName.equals("value"))
                                    {
                                        JsonNode caseEnt = payloadEnt.getValue();
                                        log.info("New Case Entity Bound");
                                        if (caseEnt != null)
                                        {

                                            log.info("Case Entity Bound - Reading Case...");
                                            Iterator<String> fieldNames = caseEnt.fieldNames();
                                            while (fieldNames.hasNext())
                                            {
                                                String caseFieldName = fieldNames.next();
                                                log.info("Case Entity Field Scanned:  " + caseFieldName);
                                                if (caseFieldName.equals("displayId"))
                                                {
                                                    log.info("Case ID Added : " + caseEnt.get(caseFieldName).asText());
                                                    if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                    {
                                                        caseId = caseEnt.get(caseFieldName).asText();

                                                    }
                                                    break;
                                                }

                                            }

                                        }

                                    }

                                }
                            }
                        }

                    }
                    catch (IOException e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }
                }
                catch (JsonProcessingException e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_NOTES_JSON", new Object[]
                    { e.getLocalizedMessage() }, Locale.ENGLISH));
                }

            }

        }

        return caseId;
    }

    @Override
    public String createCase4CustomerAddUser(TY_Case_CustomerAddUser_SrvCloud caseEntity, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {
        String caseId = null;

        if (StringUtils.hasText(caseEntity.getIndividualCustomer().getId()))
        {
            HttpClient httpclient = HttpClients.createDefault();
            String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCasesUrl());
            if (StringUtils.hasText(casePOSTURL))
            {

                HttpPost httpPost = new HttpPost(casePOSTURL);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpPost.addHeader("Content-Type", "application/json");

                ObjectMapper objMapper = new ObjectMapper();
                try
                {
                    String requestBody = objMapper.writeValueAsString(caseEntity);
                    log.info(requestBody);

                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(entity);

                    // POST Case in Service Cloud
                    try
                    {
                        // Fire the Url
                        HttpResponse response = httpclient.execute(httpPost);
                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_CREATED)
                        {
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);
                            log.info(apiOutput);
                            throw new RuntimeException(
                                    "Failed with HTTP error code : " + statusCode + " Details: " + apiOutput);

                        }

                        // Try and Get Entity from Response
                        HttpEntity entityResp = response.getEntity();
                        String apiOutput = EntityUtils.toString(entityResp);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(apiOutput);

                        if (jsonNode != null)
                        {

                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null)
                            {

                                log.info("Notes Bound!!");

                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    log.info("Payload Iterator Bound");
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    log.info("Payload Field Scanned:  " + payloadFieldName);

                                    if (payloadFieldName.equals("value"))
                                    {
                                        JsonNode caseEnt = payloadEnt.getValue();
                                        log.info("New Case Entity Bound");
                                        if (caseEnt != null)
                                        {

                                            log.info("Case Entity Bound - Reading Case...");
                                            Iterator<String> fieldNames = caseEnt.fieldNames();
                                            while (fieldNames.hasNext())
                                            {
                                                String caseFieldName = fieldNames.next();
                                                log.info("Case Entity Field Scanned:  " + caseFieldName);
                                                if (caseFieldName.equals("displayId"))
                                                {
                                                    log.info("Case ID Added : " + caseEnt.get(caseFieldName).asText());
                                                    if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                    {
                                                        caseId = caseEnt.get(caseFieldName).asText();

                                                    }
                                                    break;
                                                }

                                            }

                                        }

                                    }

                                }
                            }
                        }

                    }
                    catch (IOException e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }
                }
                catch (JsonProcessingException e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_NOTES_JSON", new Object[]
                    { e.getLocalizedMessage() }, Locale.ENGLISH));
                }

            }

        }

        return caseId;
    }

    private String getPOSTURL4BaseUrl(String urlBase)
    {
        String url = null;
        if (StringUtils.hasText(urlBase))
        {
            String[] urlParts = urlBase.split("\\?");
            if (urlParts.length > 0)
            {
                url = urlParts[0];
            }
        }
        return url;
    }

    @Override
    public String createCase4EmployeeAddUserIC(TY_Case_EmployeeAddUserIC_SrvCloud caseEntity,
            TY_DestinationProps desProps) throws EX_ESMAPI
    {
        String caseId = null;

        if (StringUtils.hasText(caseEntity.getEmployee().getId()))
        {
            HttpClient httpclient = HttpClients.createDefault();
            String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCasesUrl());
            if (StringUtils.hasText(casePOSTURL))
            {

                HttpPost httpPost = new HttpPost(casePOSTURL);
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken());
                httpPost.addHeader("Content-Type", "application/json");

                ObjectMapper objMapper = new ObjectMapper();
                try
                {
                    String requestBody = objMapper.writeValueAsString(caseEntity);
                    log.info(requestBody);

                    StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(entity);

                    // POST Case in Service Cloud
                    try
                    {
                        // Fire the Url
                        HttpResponse response = httpclient.execute(httpPost);
                        // verify the valid error code first
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != HttpStatus.SC_CREATED)
                        {
                            HttpEntity entityResp = response.getEntity();
                            String apiOutput = EntityUtils.toString(entityResp);
                            log.info(apiOutput);
                            throw new RuntimeException(
                                    "Failed with HTTP error code : " + statusCode + " Details: " + apiOutput);

                        }

                        // Try and Get Entity from Response
                        HttpEntity entityResp = response.getEntity();
                        String apiOutput = EntityUtils.toString(entityResp);

                        // Conerting to JSON
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(apiOutput);

                        if (jsonNode != null)
                        {

                            JsonNode rootNode = jsonNode.path("value");
                            if (rootNode != null)
                            {

                                log.info("Notes Bound!!");

                                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                                while (payloadItr.hasNext())
                                {
                                    log.info("Payload Iterator Bound");
                                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                                    String payloadFieldName = payloadEnt.getKey();
                                    log.info("Payload Field Scanned:  " + payloadFieldName);

                                    if (payloadFieldName.equals("value"))
                                    {
                                        JsonNode caseEnt = payloadEnt.getValue();
                                        log.info("New Case Entity Bound");
                                        if (caseEnt != null)
                                        {

                                            log.info("Case Entity Bound - Reading Case...");
                                            Iterator<String> fieldNames = caseEnt.fieldNames();
                                            while (fieldNames.hasNext())
                                            {
                                                String caseFieldName = fieldNames.next();
                                                log.info("Case Entity Field Scanned:  " + caseFieldName);
                                                if (caseFieldName.equals("displayId"))
                                                {
                                                    log.info("Case ID Added : " + caseEnt.get(caseFieldName).asText());
                                                    if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                                    {
                                                        caseId = caseEnt.get(caseFieldName).asText();

                                                    }
                                                    break;
                                                }

                                            }

                                        }

                                    }

                                }
                            }
                        }

                    }
                    catch (IOException e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }
                }
                catch (JsonProcessingException e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_NOTES_JSON", new Object[]
                    { e.getLocalizedMessage() }, Locale.ENGLISH));
                }

            }

        }

        return caseId;
    }

}
