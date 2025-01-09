package com.sap.cap.esmapi.rest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cap.esmapi.catg.pojos.TY_CaseCatgTree;
import com.sap.cap.esmapi.catg.pojos.TY_CatalogTree;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatgSrv;
import com.sap.cap.esmapi.utilities.StringsUtility;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.pojos.JSONAnotamy;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseCatalogCustomizing;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseESS;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseGuidId;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_SrvCloudUrls;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cap.esmapi.utilities.srv.intf.IF_APISrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserAPISrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;
import com.sap.cloud.security.xsuaa.token.Token;

@RestController
@Profile(GC_Constants.gc_LocalProfile)
@RequestMapping("/api")
public class APIRestController
{

    @Autowired
    private IF_UserAPISrv userSrv;

    @Autowired
    private TY_SrvCloudUrls srvCloudUrls;

    @Autowired
    private TY_CatgCus catgCus;

    @Autowired
    private IF_APISrv apiSrv;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private IF_SrvCloudAPI srvCloudApiSrv;

    @Autowired
    private IF_CatgSrv catgSrv;

    @Autowired
    private IF_CatalogSrv catalogSrv;

    @Autowired
    private IF_UserSessionSrv userSessionSrv;

    @GetMapping("/caseIds")
    public List<TY_CaseGuidId> getCaseGuidIdList()
    {
        return srvCloudApiSrv.getCaseGuidIdList(userSessionSrv.getDestinationDetails4mUserSession());
    }

    @GetMapping("/authInfo")
    public Map<String, String> sayHello(@AuthenticationPrincipal Token token)
    {

        Map<String, String> result = new HashMap<>();
        result.put("grant type", token.getGrantType());
        result.put("client id", token.getClientId());
        result.put("subaccount id", token.getSubaccountId());
        result.put("zone id", token.getZoneId());
        result.put("logon name", token.getLogonName());
        result.put("family name", token.getFamilyName());
        result.put("given name", token.getGivenName());
        result.put("email", token.getEmail());
        result.put("authorities", String.valueOf(token.getAuthorities()));
        result.put("scopes", String.valueOf(token.getScopes()));

        return result;
    }

    @GetMapping("/userInfo")
    public Ty_UserAccountEmployee getUserInfo(@AuthenticationPrincipal Token token)
    {
        return userSrv.getUserDetails(token);

    }

    @GetMapping("/casesCount")
    private String getNumberofCases() throws IOException
    {
        return String.valueOf(apiSrv.getNumberofEntitiesByUrl(srvCloudUrls.getCasesUrl()));
    }

    @GetMapping("/cases")
    private JsonNode getAllCases() throws IOException
    {
        return srvCloudApiSrv.getAllCases(userSessionSrv.getDestinationDetails4mUserSession());

    }

    @GetMapping("/casesESS")
    private List<TY_CaseESS> getAllCasesESS() throws IOException
    {
        List<TY_CaseESS> casesESSList = null;

        try
        {

            JsonNode jsonNode = getAllCases();

            if (jsonNode != null)
            {

                JsonNode rootNode = jsonNode.path("value");
                if (rootNode != null)
                {
                    System.out.println("Cases Bound!!");
                    casesESSList = new ArrayList<TY_CaseESS>();

                    Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                    while (payloadItr.hasNext())
                    {
                        System.out.println("Payload Iterator Bound");
                        Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                        String payloadFieldName = payloadEnt.getKey();
                        System.out.println("Payload Field Scanned:  " + payloadFieldName);

                        if (payloadFieldName.equals("value"))
                        {
                            Iterator<JsonNode> casesItr = payloadEnt.getValue().elements();
                            System.out.println("Cases Iterator Bound");
                            while (casesItr.hasNext())
                            {

                                JsonNode caseEnt = casesItr.next();
                                if (caseEnt != null)
                                {
                                    String caseid = null, caseguid = null, caseType = null, caseTypeDescription = null,
                                            subject = null, status = null, createdOn = null, accountId = null,
                                            contactId = null, origin = null;

                                    System.out.println("Cases Entity Bound - Reading Case...");
                                    Iterator<String> fieldNames = caseEnt.fieldNames();
                                    while (fieldNames.hasNext())
                                    {
                                        String caseFieldName = fieldNames.next();
                                        System.out.println("Case Entity Field Scanned:  " + caseFieldName);
                                        if (caseFieldName.equals("id"))
                                        {
                                            System.out.println(
                                                    "Case GUID Added : " + caseEnt.get(caseFieldName).asText());
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

                                        if (caseFieldName.equals("caseType"))
                                        {
                                            System.out.println(
                                                    "Case Type Added : " + caseEnt.get(caseFieldName).asText());
                                            if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                            {
                                                caseType = caseEnt.get(caseFieldName).asText();
                                            }
                                        }

                                        if (caseFieldName.equals("caseTypeDescription"))
                                        {
                                            System.out.println("Case Type Description Added : "
                                                    + caseEnt.get(caseFieldName).asText());
                                            if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                            {
                                                caseTypeDescription = caseEnt.get(caseFieldName).asText();
                                            }
                                        }

                                        if (caseFieldName.equals("origin"))
                                        {
                                            System.out.println("Origin Added : " + caseEnt.get(caseFieldName).asText());
                                            if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                            {
                                                origin = caseEnt.get(caseFieldName).asText();
                                            }
                                        }

                                        if (caseFieldName.equals("subject"))
                                        {
                                            System.out.println(
                                                    "Case Subject Added : " + caseEnt.get(caseFieldName).asText());
                                            if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                            {
                                                subject = caseEnt.get(caseFieldName).asText();
                                            }
                                        }

                                        if (caseFieldName.equals("statusDescription"))
                                        {
                                            System.out.println(
                                                    "Case Status Added : " + caseEnt.get(caseFieldName).asText());
                                            if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                            {
                                                status = caseEnt.get(caseFieldName).asText();
                                            }
                                        }

                                        if (caseFieldName.equals("statusDescription"))
                                        {
                                            System.out.println(
                                                    "Case Status Added : " + caseEnt.get(caseFieldName).asText());
                                            if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                            {
                                                status = caseEnt.get(caseFieldName).asText();
                                            }
                                        }

                                        if (caseFieldName.equals("adminData"))
                                        {
                                            System.out.println("Inside Admin Data:  ");

                                            JsonNode admEnt = caseEnt.path("adminData");
                                            if (admEnt != null)
                                            {
                                                System.out.println("AdminData Node Bound");

                                                Iterator<String> fieldNamesAdm = admEnt.fieldNames();
                                                while (fieldNamesAdm.hasNext())
                                                {
                                                    String admFieldName = fieldNamesAdm.next();
                                                    if (admFieldName.equals("createdOn"))
                                                    {
                                                        System.out.println(
                                                                "Created On : " + admEnt.get(admFieldName).asText());
                                                        createdOn = admEnt.get(admFieldName).asText();
                                                    }
                                                }

                                            }
                                        }

                                        if (caseFieldName.equals("account"))
                                        {
                                            System.out.println("Inside Account:  ");

                                            JsonNode accEnt = caseEnt.path("account");
                                            if (accEnt != null)
                                            {
                                                System.out.println("Account Node Bound");

                                                Iterator<String> fieldNamesAcc = accEnt.fieldNames();
                                                while (fieldNamesAcc.hasNext())
                                                {
                                                    String accFieldName = fieldNamesAcc.next();
                                                    if (accFieldName.equals("id"))
                                                    {
                                                        System.out.println(
                                                                "Account ID : " + accEnt.get(accFieldName).asText());
                                                        accountId = accEnt.get(accFieldName).asText();
                                                    }
                                                }

                                            }
                                        }

                                        if (caseFieldName.equals("reporter"))
                                        {
                                            System.out.println("Inside Reporter:  ");

                                            JsonNode repEnt = caseEnt.path("reporter");
                                            if (repEnt != null)
                                            {
                                                System.out.println("Reporter Node Bound");

                                                Iterator<String> fieldNamesRep = repEnt.fieldNames();
                                                while (fieldNamesRep.hasNext())
                                                {
                                                    String repFieldName = fieldNamesRep.next();
                                                    if (repFieldName.equals("id"))
                                                    {
                                                        System.out.println(
                                                                "Reporter ID : " + repEnt.get(repFieldName).asText());
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

                                            casesESSList.add(new TY_CaseESS(caseguid, caseid, caseType,
                                                    caseTypeDescription, subject, status, accountId, contactId,
                                                    createdOn, date, dateFormatted, odt, origin,false));

                                        }
                                        else
                                        {
                                            casesESSList.add(new TY_CaseESS(caseguid, caseid, caseType,
                                                    caseTypeDescription, subject, status, accountId, contactId,
                                                    createdOn, null, null, null, origin,false));
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

        return casesESSList;
    }

    @GetMapping("/ESS")
    private TY_UserESS getESSPortal(@AuthenticationPrincipal Token token)
    {
        TY_UserESS userDetails = userSrv.getESSDetails(token);

        return userDetails;
    }

    @GetMapping("/casesAnatomy")
    private JSONAnotamy getAllCasesAnotamy() throws IOException
    {
        JSONAnotamy metaData = null;

        List<String> caseIds = new ArrayList<String>();

        JsonNode jsonNode = getAllCases();

        if (jsonNode != null)
        {
            JsonNode rootNode = jsonNode.path("value");
            if (rootNode != null)
            {
                System.out.println("Cases Bound!!");

                Iterator<Map.Entry<String, JsonNode>> payloadItr = jsonNode.fields();
                while (payloadItr.hasNext())
                {
                    System.out.println("Payload Iterator Bound");
                    Map.Entry<String, JsonNode> payloadEnt = payloadItr.next();
                    String payloadFieldName = payloadEnt.getKey();
                    System.out.println("Payload Field Scanned:  " + payloadFieldName);

                    if (payloadFieldName.equals("value"))
                    {
                        Iterator<JsonNode> casesItr = payloadEnt.getValue().elements();
                        System.out.println("Cases Iterator Bound");
                        while (casesItr.hasNext())
                        {

                            JsonNode caseEnt = casesItr.next();
                            if (caseEnt != null)
                            {
                                System.out.println("Cases Entity Bound - Reading Case...");
                                Iterator<String> fieldNames = caseEnt.fieldNames();
                                while (fieldNames.hasNext())
                                {
                                    String caseFieldName = fieldNames.next();
                                    System.out.println("Case Entity Field Scanned:  " + caseFieldName);
                                    if (caseFieldName.equals("id"))
                                    {
                                        System.out.println("Case Id Added : " + caseEnt.get(caseFieldName).asText());
                                        if (StringUtils.hasText(caseEnt.get(caseFieldName).asText()))
                                        {
                                            caseIds.add(caseEnt.get(caseFieldName).asText());
                                        }
                                    }

                                }

                            }

                        }

                    }

                }
            }

            if (CollectionUtils.isNotEmpty(caseIds))
            {

                // List<String> distinctCaseIDs =
                // caseIds.stream().distinct().collect(Collectors.toList());
                metaData = new JSONAnotamy();
                metaData.setCaseIDS(caseIds);
            }
        }

        return metaData;
    }

    @GetMapping("/accByEmail")
    private String getAccountIdByEmail(@RequestParam(name = "email", required = true) String email) throws IOException
    {
        return srvCloudApiSrv.getAccountIdByUserEmail(email, userSessionSrv.getDestinationDetails4mUserSession());
    }

    @GetMapping("/cpByEmail")
    private String getContactIdByEmail(@RequestParam(name = "email", required = true) String email) throws IOException
    {
        return srvCloudApiSrv.getContactPersonIdByUserEmail(email, userSessionSrv.getDestinationDetails4mUserSession());

    }

    @GetMapping("/cfg")
    private TY_CatgCus checkCaseCus()
    {
        return this.catgCus;
    }

    @GetMapping("/cfgCatg/{caseType}")
    private TY_CaseCatgTree checkCaseCusCatg(@PathVariable("caseType") EnumCaseTypes caseType)
    {
        return catgSrv.getCaseCatgTree4LoB(caseType);
    }

    @GetMapping("/accURL")
    private String getACCURL(@RequestParam(name = "userName", required = true) String userName,
            @RequestParam(name = "email", required = true) String email)
    {
        return srvCloudApiSrv.createAccount(email, userName, userSessionSrv.getDestinationDetails4mUserSession());
    }

    @GetMapping("/caseConfig")
    private TY_CaseCatalogCustomizing getCaseConfig(@RequestParam(name = "caseType", required = true) String caseType)
            throws IOException
    {

        return srvCloudApiSrv.getActiveCaseTemplateConfig4CaseType(caseType,
                userSessionSrv.getDestinationDetails4mUserSession());

    }

    @GetMapping("/notesURL")
    private String createNotes()
    {
        TY_NotesCreate newNote = new TY_NotesCreate(false, "This is a new Note dropped in via /n a Service call", null);

        return srvCloudApiSrv.createNotes(newNote, userSessionSrv.getDestinationDetails4mUserSession());
    }

    @GetMapping("/parseURL")
    private String parseUrl()
    {
        System.out.println(StringsUtility.replaceURLwithParams(srvCloudUrls.getCatgTreeUrl(), new String[]
        { "24d8e296-403d-4551-b29b-bbdfbb5e5c9c" }, GC_Constants.gc_UrlReplParam));
        return StringsUtility.replaceURLwithParams(srvCloudUrls.getCatgTreeUrl(), new String[]
        { "24d8e296-403d-4551-b29b-bbdfbb5e5c9c" }, GC_Constants.gc_UrlReplParam);
    }

    @GetMapping("/catalogDetails")
    private TY_CatalogTree getCatgTreeCaseType(@RequestParam(name = "caseType", required = true) String caseType)
    {
        return catalogSrv.getCaseCatgTree4LoB(EnumCaseTypes.valueOf(caseType));
    }

}
