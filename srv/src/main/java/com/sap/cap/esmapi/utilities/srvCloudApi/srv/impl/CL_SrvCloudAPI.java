package com.sap.cap.esmapi.utilities.srvCloudApi.srv.impl;

import java.io.IOException;
import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cap.esmapi.catg.pojos.TY_CatalogItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
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
import com.sap.cap.esmapi.utilities.pojos.TY_CasePatchInfo;
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
    private WebClient webClient;

    @Autowired
    private TY_PortalStatusTransitions statusTransitions;

    @Override
    public String getAccountIdByUserEmail(String userEmail, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        String accountID = null;

        if (!StringUtils.hasText(userEmail) || srvCloudUrls == null)
        {
            return null;
        }

        if (!StringUtils.hasText(srvCloudUrls.getAccByEmail()))
        {
            return null;
        }

        try
        {

            userEmail = "'" + userEmail + "'";

            String urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getAccByEmail(), new String[]
            { userEmail, userEmail }, GC_Constants.gc_UrlReplParam);

            if (!StringUtils.hasText(urlLink) || !StringUtils.hasText(srvCloudUrls.getToken()))
            {
                return null;
            }

            // Proper URL encoding (same as before)
            URL url = new URL(urlLink);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());

            String correctEncodedURL = uri.toASCIIString();

            ResponseEntity<String> response = webClient.get().uri(correctEncodedURL)
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class)).block();

            if (response == null)
            {
                throw new RuntimeException("No response received from API");
            }

            // Spring 6 compliant status handling
            if (!response.getStatusCode().is2xxSuccessful())
            {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusCode().value());
            }

            String apiOutput = response.getBody();

            if (!StringUtils.hasText(apiOutput))
            {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(apiOutput);

            JsonNode valueArray = jsonNode.path("value");

            if (valueArray.isArray())
            {
                for (JsonNode accEnt : valueArray)
                {

                    String id = accEnt.path("id").asText(null);

                    if (StringUtils.hasText(id))
                    {
                        log.info("Account Id Added : {}", id);
                        accountID = id;
                        break; // match original behavior (first match)
                    }
                }
            }

        }
        catch (Exception e)
        {
            log.error("Error while fetching account by email", e);
            throw new EX_ESMAPI(e.getMessage());
        }

        return accountID;
    }

    @Override
    public String getEmployeeIdByUserEmail(String userEmail, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        String employeeId = null;

        if (!StringUtils.hasText(userEmail) || srvCloudUrls == null)
        {
            return null;
        }

        if (!StringUtils.hasText(srvCloudUrls.getEmpById()))
        {
            return null;
        }

        try
        {

            userEmail = "'" + userEmail + "'";

            String urlLink = srvCloudUrls.getEmpById() + userEmail;

            if (!StringUtils.hasText(urlLink) || !StringUtils.hasText(srvCloudUrls.getToken()))
            {
                return null;
            }

            // Proper URL encoding (same logic as before)
            URL url = new URL(urlLink);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());

            String correctEncodedURL = uri.toASCIIString();

            ResponseEntity<String> response = webClient.get().uri(correctEncodedURL)
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class)).block();

            if (response == null)
            {
                throw new RuntimeException("No response received from API");
            }

            // Spring 6 compliant status validation
            if (!response.getStatusCode().is2xxSuccessful())
            {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusCode().value());
            }

            String apiOutput = response.getBody();

            if (!StringUtils.hasText(apiOutput))
            {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(apiOutput);

            JsonNode valueArray = jsonNode.path("value");

            if (valueArray.isArray())
            {
                for (JsonNode empEnt : valueArray)
                {

                    String id = empEnt.path("id").asText(null);

                    if (StringUtils.hasText(id))
                    {
                        log.info("Employee Id Added : {}", id);
                        employeeId = id;
                        break; // match original behavior (first match)
                    }
                }
            }

        }
        catch (Exception e)
        {
            log.error("Error while fetching employee by email", e);
            throw new EX_ESMAPI(e.getMessage());
        }

        return employeeId;
    }

    @Override
    public String createAccount(String userEmail, String userName, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        String accountId = null;

        if (!StringUtils.hasText(userEmail) || !StringUtils.hasText(userName))
        {
            return null;
        }

        log.info("Creating Account for UserName : {} with Email : {}", userName, userEmail);

        String[] names = userName.split("\\s+");

        TY_CustomerCreate newAccount;

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

        try
        {

            String accPOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCustomerUrl());

            if (!StringUtils.hasText(accPOSTURL))
            {
                return null;
            }

            String encoding = Base64.getEncoder()
                    .encodeToString((srvCloudUrls.getUserNameExt() + ":" + srvCloudUrls.getPasswordExt()).getBytes());

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(newAccount);

            log.info(requestBody);

            ResponseEntity<String> response = webClient.post().uri(accPOSTURL)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + encoding)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).bodyValue(requestBody)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new RuntimeException("No response received from API");
            }

            // Apache equivalent: SC_CREATED (201)
            if (!response.getStatusCode().equals(HttpStatus.SC_CREATED))
            {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusCode().value());
            }

            String apiOutput = response.getBody();

            if (!StringUtils.hasText(apiOutput))
            {
                return null;
            }

            JsonNode jsonNode = mapper.readTree(apiOutput);

            JsonNode valueNode = jsonNode.path("value");

            if (!valueNode.isMissingNode())
            {

                String id = valueNode.path("id").asText(null);

                if (StringUtils.hasText(id))
                {
                    log.info("Account GUID Added : {}", id);
                    accountId = id;
                }
            }

        }
        catch (JsonProcessingException e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_AC_JSON", new Object[]
            { e.getLocalizedMessage() }, Locale.ENGLISH));

        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_ACC_POST", new Object[]
            { e.getLocalizedMessage() }, Locale.ENGLISH));
        }

        return accountId;
    }

    @Override
    public TY_CaseCatalogCustomizing getActiveCaseTemplateConfig4CaseType(String caseType, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {

        String url = srvCloudUrls.getCaseTemplateUrl() + caseType;

        JsonNode jsonNode = executeGet(url, srvCloudUrls.getToken(), "ERR_CASE_TYPE_NOCFG", new Object[]
        { caseType });

        JsonNode valueArray = jsonNode.path("value");

        if (!valueArray.isArray())
            return null;

        List<TY_CaseCatalogCustomizing> list = new ArrayList<>();

        for (JsonNode node : valueArray)
        {

            String caseTypePL = node.path("caseType").asText(null);
            String statusSchema = node.path("statusSchema").asText(null);
            String status = node.path("status").asText(null);
            String partyScheme = node.path("partyScheme").asText(null);
            String catalogId = node.path("catalog").path("id").asText(null);

            if (StringUtils.hasText(catalogId) && StringUtils.hasText(caseTypePL))
            {

                list.add(new TY_CaseCatalogCustomizing(caseTypePL, statusSchema, status, partyScheme, catalogId));
            }
        }

        return list.stream().filter(r -> GC_Constants.gc_statusACTIVE.equals(r.getStatus())).findFirst().orElse(null);
    }

    @Override
    public List<TY_CatalogItem> getActiveCaseCategoriesByCatalogId(String catalogID, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {

        if (!StringUtils.hasLength(srvCloudUrls.getCatgTreeUrl()) || !StringUtils.hasText(srvCloudUrls.getToken()))
        {
            return null;
        }

        try
        {

            log.info("Url and Credentials Found!!");

            String urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getCatgTreeUrl(), new String[]
            { catalogID }, GC_Constants.gc_UrlReplParam);

            // Proper URL encoding
            URL url = new URL(urlLink);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());

            String correctEncodedURL = uri.toASCIIString();

            // 🔥 Use generic GET executor
            JsonNode jsonNode = executeGet(correctEncodedURL, srvCloudUrls.getToken(), "ERR_CATALOG_READ", new Object[]
            { catalogID });

            if (jsonNode == null)
            {
                return null;
            }

            JsonNode valueArray = jsonNode.path("value");

            if (!valueArray.isArray())
            {
                return null;
            }

            log.info("Customizing Bound!!");

            List<TY_CatalogItem> catgTree = new ArrayList<>();

            for (JsonNode node : valueArray)
            {

                String id = node.path("id").asText(null);
                String parentId = node.path("parentId").asText(null);
                String name = node.path("name").asText(null);
                String parentName = node.path("parentName").asText(null);

                if (StringUtils.hasText(id))
                {
                    catgTree.add(new TY_CatalogItem(id, name, parentId, parentName));
                }
            }

            return catgTree;

        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CATALOG_READ", new Object[]
            { catalogID, e.getMessage() }, Locale.ENGLISH));
        }
    }

    @Override
    public String createNotes(TY_NotesCreate notes, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        if (notes == null || !StringUtils.hasText(notes.getHtmlContent()))
        {
            return null;
        }

        String notesPOSTURL = srvCloudUrls.getNotesUrl();

        if (!StringUtils.hasText(notesPOSTURL))
        {
            return null;
        }

        try
        {

            log.info("Creating Notes...");
            log.info(new ObjectMapper().writeValueAsString(notes));

            // 🔥 Use centralized POST executor
            JsonNode jsonNode = executePost(notesPOSTURL, srvCloudUrls.getToken(), notes, 201, // SC_CREATED equivalent
                    "ERR_NOTES_POST");

            if (jsonNode == null)
            {
                return null;
            }

            JsonNode valueNode = jsonNode.path("value");

            if (!valueNode.isMissingNode())
            {

                String noteId = valueNode.path("id").asText(null);

                if (StringUtils.hasText(noteId))
                {
                    log.info("Notes GUID Added : {}", noteId);
                    return noteId;
                }
            }

            return null;

        }
        catch (JsonProcessingException e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_NOTES_JSON", new Object[]
            { e.getLocalizedMessage() }, Locale.ENGLISH));

        }
        catch (EX_ESMAPI e)
        {
            throw e;
        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
            { e.getLocalizedMessage() }, Locale.ENGLISH));
        }
    }

    @Override
    public TY_AttachmentResponse createAttachment(TY_Attachment attachment, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {

        if (attachment == null || !StringUtils.hasText(attachment.getFileName()))
        {
            return null;
        }

        String docPOSTURL = srvCloudUrls.getDocSrvUrl();

        if (!StringUtils.hasText(docPOSTURL))
        {
            return null;
        }

        try
        {

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(attachment);

            log.info(requestBody);

            ResponseEntity<String> response = webClient.post().uri(docPOSTURL)
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).bodyValue(requestBody)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from API");
            }

            int status = response.getStatusCode().value();

            // Accept both 201 and 200 (same as original logic)
            if (status != 201 && status != 200)
            {
                throw new RuntimeException(
                        "Failed with HTTP error code : " + status + " Message - " + response.getStatusCode());
            }

            String apiOutput = response.getBody();

            if (!StringUtils.hasText(apiOutput))
            {
                return null;
            }

            JsonNode jsonNode = mapper.readTree(apiOutput);
            JsonNode valueNode = jsonNode.path("value");

            if (valueNode.isMissingNode())
            {
                return null;
            }

            log.info("Attachments Bound!!");

            TY_AttachmentResponse attR = new TY_AttachmentResponse();

            String id = valueNode.path("id").asText(null);
            String uploadUrl = valueNode.path("uploadUrl").asText(null);

            if (StringUtils.hasText(id))
            {
                log.info("Attachment GUID Added : {}", id);
                attR.setId(id);
            }

            if (StringUtils.hasText(uploadUrl))
            {
                log.info("Attachment Upload Url Added : {}", uploadUrl);
                attR.setUploadUrl(uploadUrl);
            }

            return attR;

        }
        catch (JsonProcessingException e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_NEW_DOCS_JSON", new Object[]
            { e.getLocalizedMessage(), attachment.toString() }, Locale.ENGLISH));

        }
        catch (EX_ESMAPI e)
        {
            throw e;
        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_DOCS_POST", new Object[]
            { e.getLocalizedMessage() }, Locale.ENGLISH));
        }
    }

    @Override
    public boolean persistAttachment(String url, MultipartFile file, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {

        if (!StringUtils.hasText(url) || file == null)
        {
            return false;
        }

        try
        {

            byte[] fileBytes = file.getBytes();

            ResponseEntity<String> response = webClient.put().uri(url).bodyValue(fileBytes)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received while persisting attachment");
            }

            int status = response.getStatusCode().value();

            // Only HTTP 200 is valid (same as SC_OK)
            if (status == 200)
            {
                return true;
            }

            String errorBody = response.getBody();
            log.error(errorBody);

            throw new EX_ESMAPI("Error persisting Attachment for filename : " + file.getOriginalFilename()
                    + " HTTPSTATUS Code " + status + " Details : " + errorBody);

        }
        catch (EX_ESMAPI e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(e.getMessage());
        }
    }

    @Override
    public boolean persistAttachment(String url, String fileName, byte[] blob, TY_DestinationProps desProps)
            throws EX_ESMAPI, IOException
    {

        if (!StringUtils.hasText(url) || blob == null)
        {
            return false;
        }

        try
        {

            ResponseEntity<String> response = webClient.put().uri(url).bodyValue(blob)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received while persisting attachment");
            }

            int status = response.getStatusCode().value();

            // Only HTTP 200 is valid (same as SC_OK)
            if (status == 200)
            {
                return true;
            }

            String errorBody = response.getBody();
            log.error(errorBody);

            throw new EX_ESMAPI("Error persisting Attachment for filename : " + fileName + " HTTPSTATUS Code " + status
                    + " Details : " + errorBody);

        }
        catch (EX_ESMAPI e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(e.getMessage());
        }
    }

    @Override
    public String getEmployeeIdByUserId(String userId, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        // Only Internal User(s) Allowed Login can Execute Employee Search
        if (!StringUtils.hasText(userId) || srvCloudUrls == null || !userId.matches(rlConfig.getInternalUsersRegex()))
        {
            return null;
        }

        if (!StringUtils.hasText(srvCloudUrls.getEmpById()) || !StringUtils.hasText(srvCloudUrls.getToken()))
        {
            return null;
        }

        try
        {

            userId = "'" + userId + "'";

            String urlLink = srvCloudUrls.getEmpById() + userId;

            if (!StringUtils.hasText(urlLink))
            {
                return null;
            }

            // Proper URL encoding
            URL url = new URL(urlLink);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());

            String correctEncodedURL = uri.toASCIIString();

            ResponseEntity<String> response = webClient.get().uri(correctEncodedURL)
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken()).accept(MediaType.APPLICATION_JSON)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from API");
            }

            int status = response.getStatusCode().value();

            // Must be HTTP 200 (same as SC_OK)
            if (status != 200)
            {
                throw new RuntimeException("Failed with HTTP error code : " + status);
            }

            String apiOutput = response.getBody();

            if (!StringUtils.hasText(apiOutput))
            {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(apiOutput);

            JsonNode valueArray = jsonNode.path("value");

            if (valueArray.isArray())
            {
                for (JsonNode node : valueArray)
                {

                    String id = node.path("id").asText(null);

                    if (StringUtils.hasText(id))
                    {
                        log.info("Employee Id Added : {}", id);
                        return id;
                    }
                }
            }

            return null;

        }
        catch (EX_ESMAPI e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error(e.getLocalizedMessage());
            throw new EX_ESMAPI(e.getMessage());
        }
    }

    @Override
    public List<TY_KeyValue> getVHelpDDLB4Field(String fieldName, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        if (!StringUtils.hasText(fieldName) || !StringUtils.hasText(srvCloudUrls.getVhlpUrl())
                || !StringUtils.hasText(srvCloudUrls.getToken()))
        {
            return null;
        }

        try
        {

            log.info("Invoking Value help for FieldName : {}", fieldName);

            String urlLink = srvCloudUrls.getVhlpUrl() + fieldName;

            ResponseEntity<String> response = webClient.get().uri(urlLink)
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken()).accept(MediaType.APPLICATION_JSON)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from API");
            }

            int status = response.getStatusCode().value();

            if (status == 404)
            {

                String msg = msgSrc.getMessage("ERR_VHLP_FLD_SRVCLOUD_NOTFOUND", new Object[]
                { fieldName }, Locale.ENGLISH);

                log.error(msg);
                throw new EX_ESMAPI(msg);
            }

            if (status != 200)
            {

                String msg = msgSrc.getMessage("ERR_VHLP_FLD_SRVCLOUD_GEN", new Object[]
                { fieldName, status }, Locale.ENGLISH);

                log.error(msg);
                throw new EX_ESMAPI(msg);
            }

            String apiOutput = response.getBody();

            if (!StringUtils.hasText(apiOutput))
            {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(apiOutput);

            JsonNode contentNode = jsonNode.path("value").path("content");

            if (!contentNode.isArray() || contentNode.isEmpty())
            {
                return null;
            }

            log.info("Values Bound for Value Help for Field - {}", fieldName);

            List<TY_KeyValue> vhlbDDLB = new ArrayList<>();

            for (JsonNode item : contentNode)
            {

                String code = item.path("code").asText(null);
                String desc = item.path("description").asText(null);
                boolean isActive = item.path("active").asBoolean(true);

                if (StringUtils.hasText(code) && StringUtils.hasText(desc) && isActive)
                {

                    vhlbDDLB.add(new TY_KeyValue(code, desc));
                }
            }

            return vhlbDDLB;

        }
        catch (EX_ESMAPI e)
        {
            throw e;
        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_VHLP_FLD_SRVCLOUD_NOTFOUND", new Object[]
            { fieldName, e.getMessage() }, Locale.ENGLISH));
        }
    }

    @Override
    public TY_CaseDetails getCaseDetails4Case(String caseId, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        if (!StringUtils.hasText(caseId) || !StringUtils.hasText(srvCloudUrls.getCaseDetailsUrl())
                || !StringUtils.hasText(srvCloudUrls.getToken()))
        {
            return null;
        }

        try
        {

            log.info("Fetching Details for Case ID : {}", caseId);

            String urlLink = srvCloudUrls.getCaseDetailsUrl() + caseId;

            ResponseEntity<String> response = webClient.get().uri(urlLink)
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken()).accept(MediaType.APPLICATION_JSON)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from API");
            }

            int status = response.getStatusCode().value();

            if (status != 200)
            {

                String msg = msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                { caseId }, Locale.ENGLISH);

                log.error(msg);
                throw new EX_ESMAPI(msg);
            }

            // 🔥 Extract ETag from response headers
            String eTag = response.getHeaders().getFirst(GC_Constants.gc_ETag);

            String apiOutput = response.getBody();

            if (!StringUtils.hasText(apiOutput))
            {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(apiOutput);

            JsonNode rootNode = jsonNode.path("value");

            if (rootNode.isMissingNode())
            {
                return null;
            }

            TY_CaseDetails caseDetails = new TY_CaseDetails();
            caseDetails.setCaseGuid(caseId);
            caseDetails.setETag(eTag);
            caseDetails.setNotes(new ArrayList<>());

            // 🔥 Extract Description (note)
            JsonNode descNode = rootNode.path("description");

            if (!descNode.isMissingNode() && descNode.size() > 0)
            {

                log.info("Desc for Case ID : {} bound..", caseId);

                String id = descNode.path("id").asText(null);
                String noteId = descNode.path("noteId").asText(null);
                String content = descNode.path("content").asText(null);
                String noteType = descNode.path("noteType").asText(null);

                JsonNode adminData = descNode.path("adminData");

                String userCreate = null;
                OffsetDateTime odt = null;
                boolean agentNote = false;

                if (!adminData.isMissingNode())
                {

                    String timestamp = adminData.path("createdOn").asText(null);

                    if (StringUtils.hasText(timestamp))
                    {
                        odt = OffsetDateTime.parse(timestamp);
                    }

                    userCreate = adminData.path("createdByName").asText(null);

                    if (StringUtils.hasText(userCreate) && !userCreate.startsWith(rlConfig.getTechUserRegex()))
                    {
                        agentNote = true;
                    }
                }

                TY_NotesDetails newNote = new TY_NotesDetails(noteType, id, noteId, odt, userCreate, content,
                        agentNote);

                caseDetails.getNotes().add(newNote);
            }

            return caseDetails;

        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
            { caseId, e.getMessage() }, Locale.ENGLISH));
        }
    }

    @Override
    public List<TY_StatusCfgItem> getStatusCfg4StatusSchema(String statusSchema, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {

        if (!StringUtils.hasText(statusSchema) || !StringUtils.hasText(srvCloudUrls.getStatusSchemaUrl())
                || !StringUtils.hasText(srvCloudUrls.getToken()))
        {
            return null;
        }

        try
        {

            log.info("Fetching Details for Status Schema: {}", statusSchema);

            String urlLink = srvCloudUrls.getStatusSchemaUrl() + statusSchema;

            ResponseEntity<String> response = webClient.get().uri(urlLink)
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken()).accept(MediaType.APPLICATION_JSON)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from API");
            }

            int status = response.getStatusCode().value();

            if (status != 200)
            {

                String msg = msgSrc.getMessage("ERR_INVALID_SCHEMA", new Object[]
                { statusSchema }, Locale.ENGLISH);

                log.error(msg);
                throw new EX_ESMAPI(msg);
            }

            String apiOutput = response.getBody();

            if (!StringUtils.hasText(apiOutput))
            {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(apiOutput);

            JsonNode assignmentsNode = jsonNode.path("value").path("userStatusAssignments");

            if (!assignmentsNode.isArray() || assignmentsNode.isEmpty())
            {
                return null;
            }

            log.info("Status for Schema : {} bound..", statusSchema);

            List<TY_StatusCfgItem> userStatusAssignments = new ArrayList<>();

            for (JsonNode item : assignmentsNode)
            {

                String userStatus = item.path("userStatus").asText(null);
                String userStatusDescription = item.path("userStatusDescription").asText(null);

                userStatusAssignments.add(new TY_StatusCfgItem(userStatus, userStatusDescription));
            }

            return userStatusAssignments;

        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_INVALID_SCHEMA", new Object[]
            { statusSchema, e.getMessage() }, Locale.ENGLISH));
        }
    }

    @Override
    public boolean updateCasewithReply(TY_CasePatchInfo patchInfo, TY_Case_SrvCloud_Reply caseReply,
            TY_DestinationProps desProps) throws EX_ESMAPI
    {

        if (caseReply == null || patchInfo == null || !StringUtils.hasText(patchInfo.getCaseGuid())
                || !StringUtils.hasText(patchInfo.getETag()))
        {
            return false;
        }

        String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCaseDetailsUrl());

        if (!StringUtils.hasText(casePOSTURL))
        {
            return false;
        }

        try
        {

            casePOSTURL = casePOSTURL + patchInfo.getCaseGuid();

            // 🔥 Determine Basic Auth
            String encoding;
            if (caseReply.isExternal())
            {
                encoding = Base64.getEncoder().encodeToString(
                        (srvCloudUrls.getUserNameExt() + ":" + srvCloudUrls.getPasswordExt()).getBytes());
            }
            else
            {
                encoding = Base64.getEncoder()
                        .encodeToString((srvCloudUrls.getUserName() + ":" + srvCloudUrls.getPassword()).getBytes());
            }

            // 🔥 Remove Description Note Types
            if (CollectionUtils.isNotEmpty(caseReply.getNotes()))
            {
                caseReply.getNotes().removeIf(n -> n.getNoteType() == null);
                caseReply.getNotes()
                        .removeIf(n -> n.getNoteType().equalsIgnoreCase(GC_Constants.gc_NoteTypeDescription));
                caseReply.getNotes().removeIf(n -> n.getNoteType().equalsIgnoreCase(GC_Constants.gc_DescNoteType));
            }

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(caseReply);

            log.info(requestBody);

            ResponseEntity<String> response = webClient.patch().uri(casePOSTURL)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + encoding)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(GC_Constants.gc_IFMatch, patchInfo.getETag()).bodyValue(requestBody)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from API");
            }

            int status = response.getStatusCode().value();

            // Must be HTTP 200
            if (status != 200)
            {

                String errorBody = response.getBody();
                log.error(errorBody);

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_REPLY_UPDATE", new Object[]
                { patchInfo.getCaseId(), status, errorBody }, Locale.ENGLISH));
            }

            return true;

        }
        catch (EX_ESMAPI e)
        {
            throw e;
        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_NOTES_POST", new Object[]
            { e.getLocalizedMessage() }, Locale.ENGLISH));
        }
    }

    @Override
    public List<TY_PreviousAttachments> getAttachments4Case(String caseGuid, TY_DestinationProps desProps)
            throws EX_ESMAPI
    {

        List<TY_PreviousAttachments> prevAtt = null;
        final String urlAttrib = "url";

        if (!StringUtils.hasText(caseGuid) || !StringUtils.hasText(srvCloudUrls.getPrevAtt())
                || !StringUtils.hasText(srvCloudUrls.getDlAtt()) || !StringUtils.hasText(srvCloudUrls.getToken()))
        {

            return null;
        }

        log.info("Fetching Attachments for Case GUID : " + caseGuid);

        try
        {

            // -------------------------------
            // FIRST CALL → GET ATTACHMENTS
            // -------------------------------

            String urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getPrevAtt(), new String[]
            { caseGuid }, GC_Constants.gc_UrlReplParam);

            URL url = new URL(urlLink);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());

            ResponseEntity<String> response = webClient.get().uri(uri.toASCIIString())
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null || response.getStatusCode().value() != 200)
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                { caseGuid }, Locale.ENGLISH));
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.getBody());

            JsonNode rootNode = jsonNode.path("value");

            if (!rootNode.isArray() || rootNode.isEmpty())
            {
                return null;
            }

            log.info("Attachments Bound for Case Guid - " + caseGuid + " : " + rootNode.size());

            prevAtt = new ArrayList<>();

            for (JsonNode attEnt : rootNode)
            {

                boolean byTechnicalUser = false;
                long fileSize = 0;

                String id = attEnt.path("id").asText(null);
                String title = attEnt.path("title").asText(null);
                String type = attEnt.path("type").asText(null);
                fileSize = attEnt.path("fileSize").asLong(0) / 1024;

                String createdByName = null;
                String dateFormatted = null;

                JsonNode admin = attEnt.path("adminData");

                if (!admin.isMissingNode())
                {

                    String createdOn = admin.path("createdOn").asText(null);

                    if (StringUtils.hasText(createdOn))
                    {

                        OffsetDateTime odt = OffsetDateTime.parse(createdOn);
                        Date date = Date.from(odt.toInstant());

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy");

                        dateFormatted = sdf.format(date);
                    }

                    createdByName = admin.path("createdByName").asText(null);

                    if (StringUtils.hasText(createdByName) && StringUtils.hasText(rlConfig.getTechUserRegex())
                            && createdByName.startsWith(rlConfig.getTechUserRegex()))
                    {

                        byTechnicalUser = true;
                    }
                }

                if (StringUtils.hasText(id) && StringUtils.hasText(title) && fileSize > 0
                        && (type == null || !type.equals(GC_Constants.gc_AttachmentTypeInternal)))
                {

                    prevAtt.add(new TY_PreviousAttachments(id, title, fileSize, createdByName, dateFormatted,
                            byTechnicalUser, null));
                }
            }

            // ----------------------------------------
            // SECOND CALL → GET DOWNLOAD URL PER FILE
            // ----------------------------------------

            for (TY_PreviousAttachments attDet : prevAtt)
            {

                String dlUrl = StringsUtility.replaceURLwithParams(srvCloudUrls.getDlAtt(), new String[]
                { attDet.getId() }, GC_Constants.gc_UrlReplParam);

                URL dUrl = new URL(dlUrl);
                URI dUri = new URI(dUrl.getProtocol(), dUrl.getUserInfo(), IDN.toASCII(dUrl.getHost()), dUrl.getPort(),
                        dUrl.getPath(), dUrl.getQuery(), dUrl.getRef());

                ResponseEntity<String> dlResponse = webClient.get().uri(dUri.toASCIIString())
                        .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken())
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .exchangeToMono(r -> r.toEntity(String.class)).block();

                if (dlResponse != null && dlResponse.getStatusCode().value() == 200)
                {

                    JsonNode dlJson = mapper.readTree(dlResponse.getBody());
                    JsonNode valueNode = dlJson.path("value");

                    if (!valueNode.isMissingNode())
                    {

                        String attDLUrl = valueNode.path(urlAttrib).asText(null);

                        if (StringUtils.hasText(attDLUrl))
                        {
                            attDet.setUrl(attDLUrl);
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

        return prevAtt;
    }

    @Override
    public List<TY_NotesDetails> getFormattedNotes4Case(String caseId, TY_DestinationProps desProps) throws EX_ESMAPI
    {

        List<TY_NotesDetails> formattedExternalNotes = new ArrayList<>();

        if (!StringUtils.hasText(caseId) || !StringUtils.hasText(srvCloudUrls.getNotesReadUrl())
                || !StringUtils.hasText(srvCloudUrls.getToken()))
        {

            return formattedExternalNotes;
        }

        log.info("Fetching External Notes for Case ID : " + caseId);

        try
        {

            String urlLink = StringsUtility.replaceURLwithParams(srvCloudUrls.getNotesReadUrl(), new String[]
            { caseId }, GC_Constants.gc_UrlReplParam);

            ResponseEntity<String> response = webClient.get().uri(urlLink)
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null || response.getStatusCode().value() != 200)
            {

                String msg = msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                { caseId }, Locale.ENGLISH);

                log.error(msg);
                throw new EX_ESMAPI(msg);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.getBody());

            JsonNode rootNode = jsonNode.path("value");

            if (rootNode.isArray() && !rootNode.isEmpty())
            {

                log.info("Notes for Case ID : " + caseId + " bound..");

                for (JsonNode noteNode : rootNode)
                {

                    String id = noteNode.path("id").asText(null);
                    String noteId = noteNode.path("noteId").asText(null);
                    String content = noteNode.path("content").asText(null);
                    String noteType = noteNode.path("noteType").asText(null);

                    String createdBy = null;
                    OffsetDateTime odt = null;
                    boolean agentNote = false;

                    JsonNode admin = noteNode.path("adminData");

                    if (!admin.isMissingNode())
                    {

                        String createdOn = admin.path("createdOn").asText(null);

                        if (StringUtils.hasText(createdOn))
                        {
                            odt = OffsetDateTime.parse(createdOn);
                        }

                        createdBy = admin.path("createdByName").asText(null);

                        if (StringUtils.hasText(createdBy) && !createdBy.startsWith(rlConfig.getTechUserRegex()))
                        {

                            agentNote = true;
                        }
                    }

                    formattedExternalNotes
                            .add(new TY_NotesDetails(noteType, id, noteId, odt, createdBy, content, agentNote));
                }
            }

        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_NOTESF_FETCH", new Object[]
            { caseId, e.getMessage() }, Locale.ENGLISH));
        }

        return formattedExternalNotes;
    }

    @Override
    public ResponseEntity<List<String>> getAllowedAttachmentTypes(TY_DestinationProps desProps) throws EX_ESMAPI
    {

        List<String> allowedAttachments = null;

        if (desProps == null || srvCloudUrls == null || !StringUtils.hasText(desProps.getAuthToken())
                || !StringUtils.hasLength(srvCloudUrls.getMimeTypesUrlPathString()))
        {

            return ResponseEntity.ok(null);
        }

        try
        {

            log.info("Url and Credentials Found!!");

            String urlLink = CL_URLUtility.getUrl4DestinationAPI(srvCloudUrls.getMimeTypesUrlPathString(),
                    desProps.getBaseUrl());

            // Proper URL encoding
            URL url = new URL(urlLink);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());

            ResponseEntity<String> response = webClient.get().uri(uri.toASCIIString())
                    .header(HttpHeaders.AUTHORIZATION, desProps.getAuthToken())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from MimeTypes API");
            }

            int statusCode = response.getStatusCode().value();

            if (statusCode != 200)
            {

                if (statusCode == 404)
                {

                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_MIME_TYPES_API", new Object[]
                    { "Not FOUND any Status Values" }, Locale.ENGLISH));
                }

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_MIME_TYPES_APII", new Object[]
                { statusCode }, Locale.ENGLISH));
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.getBody());

            JsonNode rootNode = jsonNode.path("value");

            if (rootNode.isArray() && !rootNode.isEmpty())
            {

                log.info("Customizing Bound!!");
                allowedAttachments = new ArrayList<>();

                for (JsonNode cusEnt : rootNode)
                {

                    boolean isAllowed = cusEnt.path("isAllowed").asBoolean(false);

                    if (isAllowed)
                    {

                        JsonNode fileExtNode = cusEnt.path("fileExtensions");

                        if (fileExtNode.isArray() && !fileExtNode.isEmpty())
                        {

                            log.info("File Extensions Bound");

                            for (JsonNode ext : fileExtNode)
                            {

                                String attType = ext.path("name").asText(null);

                                if (StringUtils.hasText(attType))
                                {
                                    allowedAttachments.add(attType);
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

        return ResponseEntity.ok(allowedAttachments);
    }

    @Override
    public boolean confirmCase(TY_CaseConfirmPOJO caseDetails) throws EX_ESMAPI
    {

        if (caseDetails == null || !StringUtils.hasText(caseDetails.getETag())
                || !StringUtils.hasText(caseDetails.getCaseGuid())
                || !StringUtils.hasText(caseDetails.getCnfStatusCode()))
        {

            return false;
        }

        try
        {

            String casePOSTURL = getPOSTURL4BaseUrl(srvCloudUrls.getCaseDetailsUrl()) + caseDetails.getCaseGuid();

            if (!StringUtils.hasText(casePOSTURL))
            {
                return false;
            }

            TY_Case_SrvCloud_Confirm payload = new TY_Case_SrvCloud_Confirm(caseDetails.getCnfStatusCode());

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(payload);

            ResponseEntity<String> response = webClient.patch().uri(casePOSTURL)
                    .header(HttpHeaders.AUTHORIZATION, srvCloudUrls.getToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(GC_Constants.gc_IFMatch, caseDetails.getETag()).bodyValue(requestBody)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_CONFIRM", new Object[]
                { caseDetails.getCaseId(), 417, "No response received" }, Locale.ENGLISH));
            }

            int statusCode = response.getStatusCode().value();

            if (statusCode != 200)
            {

                String apiOutput = response.getBody();
                log.error(apiOutput);

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_CONFIRM", new Object[]
                { caseDetails.getCaseId(), statusCode, apiOutput }, Locale.ENGLISH));
            }

            return true;

        }
        catch (EX_ESMAPI e)
        {
            throw e;
        }
        catch (Exception e)
        {

            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_CONFIRM", new Object[]
            { caseDetails.getCaseId(), 417, e.getLocalizedMessage() }, Locale.ENGLISH));
        }
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

    private JsonNode executeGet(String url, String authHeader, String notFoundMsgKey, Object[] msgArgs) throws EX_ESMAPI
    {

        try
        {

            ResponseEntity<String> response = webClient.get().uri(url).header(HttpHeaders.AUTHORIZATION, authHeader)
                    .accept(MediaType.APPLICATION_JSON).exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from API");
            }

            int status = response.getStatusCode().value();

            // Handle 404 separately
            if (status == 404 && notFoundMsgKey != null)
            {
                throw new EX_ESMAPI(msgSrc.getMessage(notFoundMsgKey, msgArgs, Locale.ENGLISH));
            }

            // Handle non-2xx
            if (status < 200 || status >= 300)
            {
                throw new RuntimeException("Failed with HTTP error code : " + status);
            }

            String body = response.getBody();

            if (!StringUtils.hasText(body))
            {
                return null;
            }

            return new ObjectMapper().readTree(body);

        }
        catch (EX_ESMAPI e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(e.getMessage());
        }
    }

    private JsonNode executePost(String url, String authHeader, Object body, int expectedStatus, String errorMsgKey)
            throws EX_ESMAPI
    {

        try
        {

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(body);

            ResponseEntity<String> response = webClient.post().uri(url).header(HttpHeaders.AUTHORIZATION, authHeader)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).bodyValue(requestBody)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from API");
            }

            int status = response.getStatusCode().value();

            if (status != expectedStatus)
            {
                throw new EX_ESMAPI(msgSrc.getMessage(errorMsgKey, new Object[]
                { status }, Locale.ENGLISH));
            }

            String bodyResp = response.getBody();

            if (!StringUtils.hasText(bodyResp))
            {
                return null;
            }

            return mapper.readTree(bodyResp);

        }
        catch (JsonProcessingException e)
        {
            throw new EX_ESMAPI(e.getMessage());
        }
    }

    private void executePut(String url, byte[] data, String errorMessage) throws EX_ESMAPI
    {

        try
        {

            ResponseEntity<String> response = webClient.put().uri(url).bodyValue(data)
                    .exchangeToMono(r -> r.toEntity(String.class)).block();

            if (response == null)
            {
                throw new EX_ESMAPI("No response received from API");
            }

            int status = response.getStatusCode().value();

            if (status < 200 || status >= 300)
            {
                throw new EX_ESMAPI(errorMessage + " HTTPSTATUS Code: " + status);
            }

        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(e.getMessage());
        }
    }

    @Override
    public List<TY_CaseESS> getCases4Userv2(Ty_UserAccountEmployee userDetails, EnumCaseTypes caseType,
            TY_DestinationProps desProps) throws IOException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCases4Userv2'");
    }

    @Override
    public String createCase(Object payload, TY_DestinationProps destinationProps) throws EX_ESMAPI
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createCase'");
    }

}
