package com.sap.cap.esmapi.ui.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatgSrv;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_Attachment;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.ui.pojos.TY_ESS_Stats;
import com.sap.cap.esmapi.ui.srv.intf.IF_ESS_UISrv;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.pojos.TY_Account_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_AttachmentResponse;
import com.sap.cap.esmapi.utilities.pojos.TY_Attachment_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_Customer_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_CatgLvl1_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Description_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserAPISrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;
import com.sap.cds.services.request.UserInfo;
import com.sap.cloud.security.xsuaa.token.Token;

import lombok.extern.slf4j.Slf4j;

/*
 * Main Controller Class
 */

@Slf4j
@Controller
@RequestMapping("/ess")
public class ESSController
{

    @Autowired
    private IF_UserAPISrv userSrv;

    @Autowired
    private UserInfo userInfo;

    @Autowired
    private IF_ESS_UISrv uiSrv;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private TY_CatgCus catgCusSrv;

    @Autowired
    private IF_CatgSrv catgTreeSrv;

    @Autowired
    private IF_CatalogSrv catalogTreeSrv;

    @Autowired
    private IF_SrvCloudAPI srvCloudApiSrv;

    @Autowired
    private IF_UserSessionSrv userSessionSrv;

    private static final String VW_ESSListView = "essListView";
    private static final String VW_Error = "error";
    private static final String VW_CaseForm = "caseForm";
    private static final String VW_ESSListViewRedirect = "redirect:/ess/";

    @GetMapping("/")
    public String showCasesList4User(@AuthenticationPrincipal Token token, Model model)
    {
        if (token != null && userInfo != null && userSrv != null)
        {
            // Only Authenticated user via IDP
            if (userInfo.isAuthenticated())
            {

                /*
                 * //1. Get the Cases and Information for the user
                 */
                try
                {

                    TY_UserESS userDetails = userSrv.getESSDetails(token);
                    if (userDetails != null && uiSrv != null)
                    {
                        // Populate User Details in Model
                        model.addAttribute("userInfo", userDetails);

                        // Provision to add Session Messages: Even if No Cases - spl. for Newly Create
                        // Account - to enable REfresh button
                        model.addAttribute("sessMsgs", userSrv.getSessionMessages());

                        // Only Populate Stats. if there are cases Bound
                        if (!CollectionUtils.isEmpty(userDetails.getCases()))
                        {
                            TY_ESS_Stats stats = uiSrv.getStatsForUserCases(userDetails.getCases());
                            model.addAttribute("stats", stats);
                        }

                    }
                }

                catch (Exception e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASES_LIST", new Object[]
                    { e.getLocalizedMessage() }, Locale.ENGLISH));
                }
            }

            return VW_ESSListView;
        }
        else
        {
            return VW_Error;
        }

    }

    @GetMapping("/createCase/{caseType}")
    public String showTxnDetails4Scrip(@PathVariable("caseType") EnumCaseTypes caseType, Model model) throws Exception
    {

        String accountId;

        // Only Authenticated user via IDP
        if (userInfo.isAuthenticated())
        {

            try
            {

                if (StringUtils.hasText(caseType.toString()) && userSrv != null)
                {
                    System.out.println("Case Type Selected for Creation: " + caseType);

                    TY_UserESS userDetails = new TY_UserESS();

                    // 1. Check if Account Exists for the logged in User as A/C is mandatory to
                    // create a case

                    if (StringUtils.hasText(userSrv.getUserDetails4mSession().getAccountId()))
                    {

                        accountId = userSrv.getUserDetails4mSession().getAccountId();
                    }

                    else // Create the Account with logged in User credentials
                    {
                        accountId = userSrv.createAccount(); // Implictly refreshed in buffer
                    }

                    // Prepare Case Model - Form
                    if (StringUtils.hasText(accountId) && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
                    {
                        userDetails.setUserDetails(userSrv.getUserDetails4mSession());
                        model.addAttribute("userInfo", userDetails);

                        Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                                .filter(g -> g.getCaseTypeEnum().toString().equals(caseType.toString())).findFirst();
                        if (cusItemO.isPresent() && catgTreeSrv != null)
                        {

                            model.addAttribute("caseTypeStr", caseType.toString());

                            TY_Case_Form caseForm = new TY_Case_Form();
                            caseForm.setAccId(accountId); // hidden
                            caseForm.setCaseTxnType(cusItemO.get().getCaseType()); // hidden
                            model.addAttribute("caseForm", caseForm);

                            model.addAttribute("formError", null);

                            // also Upload the Catg. Tree as per Case Type
                            model.addAttribute("catgsList",
                                    catalogTreeSrv.getCaseCatgTree4LoB(caseType).getCategories());

                        }
                        else
                        {
                            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                            { caseType.toString() }, Locale.ENGLISH));
                        }

                    }

                }

            }
            catch (Exception e)
            {
                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASES_FORM", new Object[]
                { e.getLocalizedMessage() }, Locale.ENGLISH));
            }
        }

        return VW_CaseForm;
    }

    @PostMapping("/saveCase")
    public String saveCase(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {
        log.info("Inside SAve Case Method!!");
        System.out.println("Inside SAve Case Method!!");
        TY_UserESS userDetails = new TY_UserESS();
        TY_Case_Customer_SrvCloud newCaseEntity = new TY_Case_Customer_SrvCloud();
        Optional<TY_CatgCusItem> cusItemO = null;
        TY_AttachmentResponse attR = null;

        if (caseForm != null && userInfo.isAuthenticated())
        {
            System.out.println("Case Payload: " + caseForm.toString());

            // Validate Case Form
            List<String> msgs = validateCaseForm(caseForm);
            cusItemO = catgCusSrv.getCustomizations().stream()
                    .filter(g -> g.getCaseType().equals(caseForm.getCaseTxnType())).findFirst();
            if (!CollectionUtils.isEmpty(msgs))
            {
                model.addAttribute("formError", msgs);
                /*
                 * ------ Prepare Model for REload
                 */
                if (StringUtils.hasText(userSrv.getUserDetails4mSession().getAccountId()))
                {
                    userDetails.setUserDetails(userSrv.getUserDetails4mSession());
                    model.addAttribute("userInfo", userDetails);
                }

                if (cusItemO.isPresent() && catalogTreeSrv != null)
                {
                    model.addAttribute("caseTypeStr", cusItemO.get().getCaseTypeEnum().toString());
                    model.addAttribute("caseForm", caseForm);
                    // also Upload the Catg. Tree as per Case Type
                    model.addAttribute("catgsList",
                            catalogTreeSrv.getCaseCatgTree4LoB(cusItemO.get().getCaseTypeEnum()).getCategories());

                    return "caseForm";
                }
            } // CaseForm has errors

            else // Commit the Case
            {

                newCaseEntity.setAccount(new TY_Account_CaseCreate(caseForm.getAccId())); // Account ID
                newCaseEntity.setCaseType(caseForm.getCaseTxnType()); // Case Txn. Type
                newCaseEntity.setSubject(caseForm.getSubject()); // Subject

                // Fetch CatgGuid by description from Customizing - Set Categories
                if (cusItemO.isPresent() && catalogTreeSrv != null)
                {
                    String[] catTreeSelCatg = catalogTreeSrv.getCatgHierarchyforCatId(caseForm.getCatgDesc(),
                            cusItemO.get().getCaseTypeEnum());
                    if (Arrays.stream(catTreeSelCatg).filter(e -> e != null).count() > 0)
                    {
                        switch ((int) Arrays.stream(catTreeSelCatg).filter(e -> e != null).count())
                        {
                        case 4:
                            newCaseEntity.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[3]));
                            newCaseEntity.setCategoryLevel2(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                            newCaseEntity.setCategoryLevel3(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                            newCaseEntity.setCategoryLevel4(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                            break;
                        case 3:
                            newCaseEntity.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                            newCaseEntity.setCategoryLevel2(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                            newCaseEntity.setCategoryLevel3(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                            break;
                        case 2:
                            newCaseEntity.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                            newCaseEntity.setCategoryLevel2(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                            break;
                        case 1:
                            newCaseEntity.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                        default:
                            throw new EX_ESMAPI(msgSrc.getMessage("ERR_INVALID_CATG", new Object[]
                            { cusItemO.get().getCaseTypeEnum().toString(), caseForm.getCatgDesc() }, Locale.ENGLISH));
                        }
                    }
                    else
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_INVALID_CATG", new Object[]
                        { cusItemO.get().getCaseTypeEnum().toString(), caseForm.getCatgDesc() }, Locale.ENGLISH));
                    }

                    // Create Notes if There is a description
                    if (StringUtils.hasText(caseForm.getDescription()))
                    {
                        // Create Note and Get Guid back
                        String noteId = srvCloudApiSrv.createNotes(
                                new TY_NotesCreate(false, caseForm.getDescription(), null),
                                userSessionSrv.getDestinationDetails4mUserSession());
                        if (StringUtils.hasText(noteId))
                        {
                            newCaseEntity.setDescription(new TY_Description_CaseCreate(noteId));
                        }
                    }

                    // Check if Attachment needs to be Created
                    if (caseForm.getAttachment() != null)
                    {
                        try
                        {
                            if (caseForm.getAttachment().getBytes() != null)
                            {
                                // Create Attachment
                                TY_Attachment newAttachment = new TY_Attachment(false,
                                        FilenameUtils.getName(caseForm.getAttachment().getOriginalFilename()),
                                        GC_Constants.gc_Attachment_Category, false);
                                attR = srvCloudApiSrv.createAttachment(newAttachment,
                                        userSessionSrv.getDestinationDetails4mUserSession());
                                if (attR != null)
                                {
                                    if (StringUtils.hasText(attR.getId()) && StringUtils.hasText(attR.getUploadUrl()))
                                    {
                                        log.info("Attachment id :" + attR.getId() + " generated for Upload Url : "
                                                + attR.getUploadUrl());

                                        if (srvCloudApiSrv.persistAttachment(attR.getUploadUrl(),
                                                caseForm.getAttachment(),
                                                userSessionSrv.getDestinationDetails4mUserSession()))
                                        {
                                            log.info("Attachment with id : " + attR.getId()
                                                    + " Persisted in Document Container..");

                                            // Prepare POJOdetails for TY_Case_SrvCloud newCaseEntity
                                            List<TY_Attachment_CaseCreate> caseAttachmentsNew = new ArrayList<TY_Attachment_CaseCreate>();
                                            TY_Attachment_CaseCreate caseAttachment = new TY_Attachment_CaseCreate(
                                                    attR.getId());
                                            caseAttachmentsNew.add(caseAttachment);
                                            newCaseEntity.setAttachments(caseAttachmentsNew);

                                        }

                                    }

                                }
                            }
                        }
                        catch (EX_ESMAPI | IOException e)
                        {
                            if (e instanceof IOException)
                            {
                                throw new EX_ESMAPI(msgSrc.getMessage("FILE_NO_DATA", new Object[]
                                { caseForm.getAttachment().getOriginalFilename() }, Locale.ENGLISH));
                            }
                            else if (e instanceof EX_ESMAPI)
                            {
                                throw new EX_ESMAPI(msgSrc.getMessage("ERROR_DOCS_PERSIST", new Object[]
                                { attR.getUploadUrl(), caseForm.getAttachment().getOriginalFilename(),
                                        e.getLocalizedMessage() }, Locale.ENGLISH));
                            }
                        }
                    }

                    // Case Payload is now Ready: Post and get the Case ID back
                    try
                    {
                        String caseID = srvCloudApiSrv.createCase(newCaseEntity,
                                userSessionSrv.getDestinationDetails4mUserSession());
                        if (StringUtils.hasText(caseID))
                        {
                            System.out.println("Case ID : " + caseID + " created..");
                            // Populate Success message in session
                            userSrv.addSessionMessage(msgSrc.getMessage("SUCC_CASE", new Object[]
                            { caseID, cusItemO.get().getCaseTypeEnum().toString() }, Locale.ENGLISH));
                        }
                    }
                    catch (Exception e)
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_POST", new Object[]
                        { e.getLocalizedMessage() }, Locale.ENGLISH));
                    }

                }
            }

        }
        return VW_ESSListViewRedirect;
    }

    private List<String> validateCaseForm(TY_Case_Form caseForm)
    {
        List<String> msgs = new ArrayList<String>();

        if (!StringUtils.hasLength(caseForm.getAccId()))
        {
            msgs.add(msgSrc.getMessage("ERR_NO_AC", null, Locale.ENGLISH));
        }

        if (!StringUtils.hasLength(caseForm.getCaseTxnType()))
        {
            msgs.add(msgSrc.getMessage("ERR_NO_CASETYPE", null, Locale.ENGLISH));
        }

        if (!StringUtils.hasLength(caseForm.getCatgDesc()))
        {
            msgs.add(msgSrc.getMessage("ERR_NO_CATG", null, Locale.ENGLISH));
        }

        if (!StringUtils.hasLength(caseForm.getSubject()))
        {
            msgs.add(msgSrc.getMessage("ERR_NO_SUBJECT", null, Locale.ENGLISH));
        }

        return msgs;
    }

}
