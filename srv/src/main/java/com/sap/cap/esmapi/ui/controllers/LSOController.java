package com.sap.cap.esmapi.ui.controllers;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgDetails;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplates;
import com.sap.cap.esmapi.catg.pojos.TY_SplCatg;
import com.sap.cap.esmapi.catg.pojos.TY_SplCatgCus;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatgSrv;
import com.sap.cap.esmapi.events.event.EV_CaseConfirmSubmit;
import com.sap.cap.esmapi.exceptions.EX_CaseAlreadyConfirmed;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_CaseConfirmPOJO;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEdit_Form;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.ui.pojos.TY_SplCatg_Seek;
import com.sap.cap.esmapi.ui.srv.intf.IF_SplCatgMVSrv;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cap.esmapi.utilities.srv.intf.IF_SessAttachmentsService;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.uimodel.intf.IF_CountryLanguageVHelpAdj;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_VHelpLOBUIModelSrv;
import com.sap.cds.services.request.UserInfo;
import com.sap.cloud.security.token.Token;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/lso")
public class LSOController
{

    @Autowired
    private IF_UserSessionSrv userSessSrv;

    @Autowired
    private UserInfo userInfo;

    @Autowired
    private TY_CatgCus catgCusSrv;

    @Autowired
    private IF_CatgSrv catgTreeSrv;

    @Autowired
    private IF_CatalogSrv catalogTreeSrv;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private IF_VHelpLOBUIModelSrv vhlpUISrv;

    @Autowired
    private IF_SessAttachmentsService attSrv;

    @Autowired
    private TY_RLConfig rlConfig;

    @Autowired
    private IF_CountryLanguageVHelpAdj coLaDDLBSrv;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IF_CatalogSrv catalogSrv;

    @Autowired
    private IF_SplCatgMVSrv splCatgMVSrv;

    @Autowired
    private TY_SplCatgCus splCatgCus;

    @Autowired
    private ApplicationContext appCtxt;

    private final String caseListVWRedirect = "redirect:/lso/";
    private final String caseFormViewLXSS = "caseFormLSOLXSS";
    private final String caseFormReplyLXSS = "caseFormReplyLSOLXSS";
    private final String lsoCaseListViewLXSS = "lsoCasesListViewLXSS";
    private final String caseConfirmError = "alreadyConfirmed";
    private final String invalidToken = "invalid_token";

    @GetMapping("/")
    public String showCasesList(@AuthenticationPrincipal Token token, Model model)
    {
        String vw = null;
        if (token != null && userInfo != null && userSessSrv != null)
        {

            try
            {
                // Only Authenticated user via IDP
                if (userInfo.isAuthenticated())
                {

                    // #AUTH checks to be done later after role collection(s) are published in
                    // CL_UserSessionSrv
                    TY_UserESS userDetails = new TY_UserESS();

                    if (userSessSrv != null)
                    {
                        // Get User Info. from XSUAA TOken
                        if (userSessSrv.getESSDetails(token, true) != null)
                            // check User and Account Bound
                            if (userSessSrv.getUserDetails4mSession() != null)
                            {

                                log.info("User Details Bound from Session!");
                                if (StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                                        || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                                {
                                    vw = lsoCaseListViewLXSS;
                                    if (!CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
                                    {

                                        Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                                                .filter(g -> g.getCaseTypeEnum().toString()
                                                        .equals(EnumCaseTypes.Learning.toString()))
                                                .findFirst();
                                        if (cusItemO.isPresent() && catgTreeSrv != null)
                                        {
                                            userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                                            log.info("Fetching Cases for User From Session : "
                                                    + userSessSrv.getUserDetails4mSession().toString());
                                            userDetails.setCases(userSessSrv.getCases4User4mSession());
                                            model.addAttribute("userInfo", userDetails);
                                            model.addAttribute("caseTypeStr", EnumCaseTypes.Learning.toString());
                                            // Rate Limit Simulation
                                            model.addAttribute("rateLimitBreached",
                                                    userSessSrv.getCurrentRateLimitBreachedValue());

                                            // Even if No Cases - spl. for Newly Create Acc - to enable REfresh button
                                            model.addAttribute("sessMsgs", userSessSrv.getSessionMessages());

                                            // Session Active Toast
                                            model.addAttribute("submActive", userSessSrv.isCurrentSubmissionActive());

                                        }

                                        else
                                        {

                                            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                                            { EnumCaseTypes.Learning.toString() }, Locale.ENGLISH));
                                        }
                                    }

                                }
                            }

                    }
                }

            }
            catch (Exception e)
            {
                throw new EX_ESMAPI(e.getLocalizedMessage());
            }

        }
        else
        {
            vw = invalidToken;
        }

        return vw;

    }

    @GetMapping("/createCase/")
    public String showCaseAsyncForm(Model model)
    {
        userSessSrv.clearActiveSubmission();
        String viewCaseForm = caseFormViewLXSS;

        if ((StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()) && userInfo.isAuthenticated())
        {

            Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                    .filter(g -> g.getCaseTypeEnum().toString().equals(EnumCaseTypes.Learning.toString())).findFirst();
            if (cusItemO.isPresent() && catgTreeSrv != null)
            {

                model.addAttribute("caseTypeStr", EnumCaseTypes.Learning.toString());

                // Before case form Inititation we must check the Rate Limit for the Current
                // User Session --current Form Submission added for Rate Limit Evaulation
                if (userSessSrv.isWithinRateLimit())
                {
                    // Populate User Details
                    TY_UserESS userDetails = new TY_UserESS();
                    userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                    model.addAttribute("userInfo", userDetails);

                    // Initialize Attachments Session Service
                    if (attSrv != null)
                    {
                        attSrv.initialize();
                    }
                    // Populate Case Form Details
                    TY_Case_Form caseForm = new TY_Case_Form();
                    if (userSessSrv.getUserDetails4mSession().isEmployee())
                    {
                        caseForm.setAccId(userSessSrv.getUserDetails4mSession().getEmployeeId()); // hidden
                    }
                    else
                    {
                        caseForm.setAccId(userSessSrv.getUserDetails4mSession().getAccountId()); // hidden
                    }

                    caseForm.setCaseTxnType(cusItemO.get().getCaseType()); // hidden
                    model.addAttribute("caseForm", caseForm);

                    model.addAttribute("formErrors", null);

                    // clear Form errors on each refresh or a New Case form request
                    if (CollectionUtils.isNotEmpty(userSessSrv.getFormErrors()))
                    {
                        userSessSrv.clearFormErrors();

                    }

                    // also Upload the Catg. Tree as per Case Type
                    model.addAttribute("catgsList",
                            catalogTreeSrv.getCaseCatgTree4LoB(EnumCaseTypes.Learning).getCategories());

                    // Attachment file Size
                    model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

                }
                else
                {
                    // Not Within Rate Limit - REdirect to List View
                    viewCaseForm = caseListVWRedirect;

                }

            }
            else
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                { EnumCaseTypes.Learning.toString() }, Locale.ENGLISH));
            }

        }

        return viewCaseForm;
    }

    @GetMapping("/errForm/")
    public String showErrorCaseForm(Model model)
    {

        if ((StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations())
                && userSessSrv.getCurrentForm4Submission() != null)
        {
            userSessSrv.clearActiveSubmission();

            Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                    .filter(g -> g.getCaseTypeEnum().toString().equals(EnumCaseTypes.Learning.toString())).findFirst();
            if (cusItemO.isPresent() && catgTreeSrv != null)
            {

                model.addAttribute("caseTypeStr", EnumCaseTypes.Learning.toString());

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                // Populate Case Form Details
                TY_Case_Form caseForm = new TY_Case_Form();
                if (userSessSrv.getUserDetails4mSession().isEmployee())
                {
                    caseForm.setAccId(userSessSrv.getUserDetails4mSession().getEmployeeId()); // hidden
                }
                else
                {
                    caseForm.setAccId(userSessSrv.getUserDetails4mSession().getAccountId()); // hidden
                }

                caseForm.setCaseTxnType(cusItemO.get().getCaseType()); // hidden
                caseForm.setCatgDesc(userSessSrv.getCurrentForm4Submission().getCaseForm().getCatgDesc()); // Curr Catg
                caseForm.setDescription(userSessSrv.getCurrentForm4Submission().getCaseForm().getDescription()); // Curr
                                                                                                                 // Notes
                caseForm.setSubject(userSessSrv.getCurrentForm4Submission().getCaseForm().getSubject()); // Curr Subject

                if (StringUtils.hasText(userSessSrv.getCurrentForm4Submission().getCaseForm().getAddEmail())) // Affected
                                                                                                              // User
                                                                                                              // email
                {
                    caseForm.setAddEmail(userSessSrv.getCurrentForm4Submission().getCaseForm().getAddEmail());
                }

                if (StringUtils.hasText(userSessSrv.getCurrentForm4Submission().getCaseForm().getCountry()))
                {
                    caseForm.setCountry(userSessSrv.getCurrentForm4Submission().getCaseForm().getCountry());
                }

                if (StringUtils.hasText(userSessSrv.getCurrentForm4Submission().getCaseForm().getLanguage()))
                {
                    caseForm.setLanguage(userSessSrv.getCurrentForm4Submission().getCaseForm().getLanguage());
                }

                if (StringUtils.hasText(userSessSrv.getCurrentForm4Submission().getCaseForm().getLanguage()))
                {
                    caseForm.setLanguage(userSessSrv.getCurrentForm4Submission().getCaseForm().getLanguage());
                }

                model.addAttribute("formErrors", userSessSrv.getFormErrors());

                // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                // But just to handle theoratically in case there is an Error in Attachment
                // Persistence only- Remove the attachment otherwise let it persist
                if (CollectionUtils.isNotEmpty(userSessSrv.getMessageStack()))
                {
                    Optional<TY_Message> attErrO = userSessSrv.getMessageStack().stream()
                            .filter(e -> e.getMsgType().equals(EnumMessageType.ERR_ATTACHMENT)).findFirst();
                    if (!attErrO.isPresent())
                    {
                        // Attachment able to presist do not remove it from Current Payload
                        caseForm.setAttachment(userSessSrv.getCurrentForm4Submission().getCaseForm().getAttachment());

                    }
                }

                // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                // But just to handle theoratically in case there is an Error in Attachment
                // Persistence only- Remove the attachment otherwise let it persist
                if (CollectionUtils.isNotEmpty(userSessSrv.getMessageStack()))
                {
                    Optional<TY_Message> attErrO = userSessSrv.getMessageStack().stream()
                            .filter(e -> e.getMsgType().equals(EnumMessageType.ERR_ATTACHMENT)).findFirst();
                    if (!attErrO.isPresent())
                    {
                        // Attachment able to presist do not remove it from Current Payload
                        caseForm.setAttachment(userSessSrv.getCurrentForm4Submission().getCaseForm().getAttachment());

                    }
                }

                if (vhlpUISrv != null)
                {
                    model.addAllAttributes(coLaDDLBSrv.adjustCountryLanguageDDLB(caseForm.getCountry(),
                            vhlpUISrv.getVHelpUIModelMap4LobCatg(EnumCaseTypes.Learning, caseForm.getCatgDesc())));
                }

                model.addAttribute("caseForm", caseForm);

                // also Upload the Catg. Tree as per Case Type
                model.addAttribute("catgsList",
                        catalogTreeSrv.getCaseCatgTree4LoB(EnumCaseTypes.Learning).getCategories());

                if (attSrv != null)
                {
                    if (CollectionUtils.isNotEmpty(attSrv.getAttachmentNames()))
                    {
                        model.addAttribute("attachments", attSrv.getAttachmentNames());
                    }
                }

                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

            }
            else
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                { EnumCaseTypes.Learning.toString() }, Locale.ENGLISH));
            }

        }

        return caseFormViewLXSS;
    }

    @GetMapping("/errFormSpl/")
    public String showErrorCaseSplForm(Model model)
    {
        String viewName = null;

        if ((StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations())
                && userSessSrv.getCurrentForm4Submission() != null)
        {
            log.info("Inside form Error Redirect - spl case catg... ");
            userSessSrv.clearActiveSubmission();
            TY_Case_Form caseForm = userSessSrv.getCurrentForm4Submission().getCaseForm();
            log.info("Seeking Spl. Catg customizing for category: " + caseForm.getCatgDesc());
            TY_SplCatg_Seek splCatgSeek = userSessSrv.isSplCatg(caseForm.getCatgDesc());
            if (splCatgSeek != null)
            {
                ModelAndView mv = splCatgMVSrv.getSplCatgModelAndView(caseForm, true);
                viewName = mv.getViewName();
                model.addAllAttributes(mv.getModel());
            }
        }

        return viewName;

    }

    @GetMapping("/removeAttachment/{fileName}")
    public String removeAttachmentCaseCreate(@PathVariable String fileName, Model model)
    {
        if (StringUtils.hasText(fileName) && attSrv != null && userSessSrv != null)
        {
            userSessSrv.clearActiveSubmission();
            if (attSrv.checkIFExists(fileName))
            {
                attSrv.removeAttachmentByName(fileName);
            }

            // Populate the view

            Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                    .filter(g -> g.getCaseTypeEnum().toString().equals(EnumCaseTypes.Learning.toString())).findFirst();
            if (cusItemO.isPresent() && catgTreeSrv != null)
            {

                model.addAttribute("caseTypeStr", EnumCaseTypes.Learning.toString());

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                // Get Case from Session Service

                TY_Case_Form caseForm = userSessSrv.getCaseFormB4Submission();

                if (caseForm != null)
                {

                    if (userSessSrv.getUserDetails4mSession().isEmployee())
                    {
                        caseForm.setEmployee(true);
                    }

                    // Clear form for New Attachment as Current Attachment already in Container
                    caseForm.setAttachment(null);

                    // Scan for Template Load
                    TY_CatgTemplates catgTemplate = catalogTreeSrv.getTemplates4Catg(caseForm.getCatgDesc(),
                            EnumCaseTypes.Learning);
                    if (catgTemplate != null)
                    {

                        // Set Questionnaire for Category
                        caseForm.setTemplate(catgTemplate.getQuestionnaire());

                    }

                    if (vhlpUISrv != null)
                    {
                        model.addAllAttributes(coLaDDLBSrv.adjustCountryLanguageDDLB(caseForm.getCountry(),
                                vhlpUISrv.getVHelpUIModelMap4LobCatg(EnumCaseTypes.Learning, caseForm.getCatgDesc())));
                    }

                    model.addAttribute("caseForm", caseForm);

                    // also Upload the Catg. Tree as per Case Type
                    model.addAttribute("catgsList",
                            catalogTreeSrv.getCaseCatgTree4LoB(EnumCaseTypes.Learning).getCategories());

                    model.addAttribute("attachments", attSrv.getAttachmentNames());

                    // Attachment file Size
                    model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
                }

            }
        }

        return caseFormViewLXSS;
    }

    @GetMapping("/caseReply/removeAttachment/{fileName}")
    public String removeAttachmentCaseReply(@PathVariable String fileName, Model model)
    {
        if (StringUtils.hasText(fileName) && attSrv != null && userSessSrv != null)
        {
            userSessSrv.clearActiveSubmission();
            if (attSrv.checkIFExists(fileName))
            {
                attSrv.removeAttachmentByName(fileName);
            }

            // Populate User Details
            TY_UserESS userDetails = new TY_UserESS();
            userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
            model.addAttribute("userInfo", userDetails);

            model.addAttribute("formErrors", userSessSrv.getFormErrors());

            // Get Case Details
            TY_CaseEdit_Form caseEditForm = null;

            // Try to Get Case Edit Form from Upload Form from User Submit
            if (userSessSrv.getCurrentReplyForm4Submission() != null)
            {
                caseEditForm = userSessSrv.getCaseDetails4Edit(
                        userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid());

                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getReply());
                // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                // But just to handle theoratically in case there is an Error in Attachment
                // Persistence only- Remove the attachment otherwise let it persist
                if (CollectionUtils.isNotEmpty(userSessSrv.getMessageStack()))
                {
                    Optional<TY_Message> attErrO = userSessSrv.getMessageStack().stream()
                            .filter(e -> e.getMsgType().equals(EnumMessageType.ERR_ATTACHMENT)).findFirst();
                    if (attErrO.isPresent())
                    {
                        // Attachment able to presist do not remove it from Current Payload
                        caseEditForm.setAttachment(null);

                    }
                }
            }
            else
            {
                caseEditForm = userSessSrv
                        .getCaseDetails4Edit(userSessSrv.getCaseEditFormB4Submission().getCaseDetails().getCaseGuid());
                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(userSessSrv.getCaseEditFormB4Submission().getReply());
            }

            if (caseEditForm != null)
            {

                model.addAttribute("caseEditForm", caseEditForm);

                model.addAttribute("attachments", attSrv.getAttachmentNames());

                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
            }
        }

        return caseFormReplyLXSS;

    }

    @GetMapping("/errCaseReply/")
    public String showErrorCaseReplyForm(Model model)
    {
        if (userSessSrv != null)
        {
            userSessSrv.clearActiveSubmission();

            if (userSessSrv.getCurrentReplyForm4Submission() != null && StringUtils.hasText(
                    userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid()))
            {

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                model.addAttribute("formErrors", userSessSrv.getFormErrors());

                // Get Case Details
                TY_CaseEdit_Form caseEditForm = userSessSrv.getCaseDetails4Edit(
                        userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid());

                if (caseEditForm != null)
                {
                    // Super Impose Reply from User Form 4m Session
                    caseEditForm.setReply(userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getReply());
                    // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                    // But just to handle theoratically in case there is an Error in Attachment
                    // Persistence only- Remove the attachment otherwise let it persist
                    if (CollectionUtils.isNotEmpty(userSessSrv.getMessageStack()))
                    {
                        Optional<TY_Message> attErrO = userSessSrv.getMessageStack().stream()
                                .filter(e -> e.getMsgType().equals(EnumMessageType.ERR_ATTACHMENT)).findFirst();
                        if (attErrO.isPresent())
                        {
                            // Attachment able to presist do not remove it from Current Payload
                            caseEditForm.setAttachment(null);

                        }
                    }

                    model.addAttribute("caseEditForm", caseEditForm);

                    model.addAttribute("attachments", attSrv.getAttachmentNames());

                    // Attachment file Size
                    model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
                }

            }
            else
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                { userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid() },
                        Locale.ENGLISH));
            }
        }

        return caseFormReplyLXSS;

    }

    @GetMapping("/caseDetails/{caseID}")
    public String getCaseDetails(@PathVariable String caseID, Model model)
    {
        String viewName = caseFormReplyLXSS;
        userSessSrv.clearActiveSubmission();
        log.info("Navigating to case with UUID : " + caseID);
        if (StringUtils.hasText(caseID))
        {
            if (userSessSrv != null)
            {
                // Before case form Inititation we must check the Rate Limit for the Current
                // User Session --current Form Submission added for Rate Limit Evaulation
                if (userSessSrv.checkRateLimit())
                {
                    try
                    {

                        // Populate User Details
                        TY_UserESS userDetails = new TY_UserESS();
                        userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                        model.addAttribute("userInfo", userDetails);

                        // Get Case Details
                        TY_CaseEdit_Form caseEditForm = userSessSrv.getCaseDetails4Edit(caseID);
                        if (caseEditForm != null)
                        {
                            model.addAttribute("caseEditForm", caseEditForm);
                            if (CollectionUtils.isNotEmpty(caseEditForm.getCaseDetails().getNotes()))
                            {
                                log.info("# External Notes Bound for Case ID - "
                                        + caseEditForm.getCaseDetails().getNotes().size());

                            }

                            // Initialize Attachments Session Service
                            if (attSrv != null)
                            {
                                attSrv.initialize();
                            }

                            // Attachment file Size
                            model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
                        }
                    }
                    catch (Exception e)
                    {
                        return "error";
                    }
                }
                else
                {
                    // Not Within Rate Limit - REdirect to List View
                    viewName = caseListVWRedirect;
                }

            }
        }

        return viewName;
    }

    @GetMapping("/confirmCase/{caseID}")
    public ModelAndView confirmCase(@PathVariable String caseID, RedirectAttributes attributes)
    {
        String svyUrl = null;

        TY_CaseConfirmPOJO caseDetails;

        if (StringUtils.hasText(caseID) && userSessSrv != null)
        {
            log.info("Triggered Confirmation for case ID : " + caseID);

            try
            {

                Ty_UserAccountEmployee usAccConEmpl = userSessSrv.getUserDetails4mSession();

                if (!usAccConEmpl.isExternal())
                { // Internal User
                    log.info("Internal employee detected. Skipping survey redirect for case ID: " + caseID);

                    caseDetails = userSessSrv.getCaseDetails4Confirmation(caseID);
                    if (caseDetails != null && StringUtils.hasText(caseDetails.getETag()))
                    {
                        EV_CaseConfirmSubmit caseConfirmEvent = new EV_CaseConfirmSubmit(this, caseDetails);
                        applicationEventPublisher.publishEvent(caseConfirmEvent);
                    }
                    return new ModelAndView(new RedirectView(GC_Constants.gc_basePath)); // Redirect for INTERNAL user -
                                                                                         // Base Path
                }
                else
                { // External User
                    svyUrl = userSessSrv.getSurveyUrl4CaseId(caseID);

                    if (StringUtils.hasText(svyUrl))
                    {
                        caseDetails = userSessSrv.getCaseDetails4Confirmation(caseID);
                        if (caseDetails != null && StringUtils.hasText(caseDetails.getETag()))
                        {
                            EV_CaseConfirmSubmit caseConfirmEvent = new EV_CaseConfirmSubmit(this, caseDetails);
                            applicationEventPublisher.publishEvent(caseConfirmEvent);
                        }
                        return new ModelAndView(new RedirectView(svyUrl)); // Redirect for EXTERNAL user with survey URL
                    }
                }
            }
            catch (Exception e)
            {
                if (e instanceof EX_ESMAPI)
                {
                    throw new EX_ESMAPI("Exception Triggered while Confirming the Case Id : " + caseID + "Details : "
                            + e.getLocalizedMessage());
                }

                if (e instanceof EX_CaseAlreadyConfirmed)
                {
                    userSessSrv.addSessionMessage(e.getMessage());
                    attributes.addFlashAttribute("message", e.getMessage());
                    return new ModelAndView(new RedirectView("/lso/errorConfirm"));

                }
            }

        }
        return new ModelAndView(new RedirectView(GC_Constants.gc_basePath)); // Redirect for INTERNAL user -
                                                                             // Base Path
    }

    @GetMapping("/errorConfirm")
    public String redirectWithUsingRedirectView(Model model, @ModelAttribute("message") String message)
    {
        model.addAttribute("message", message);
        return caseConfirmError;

    }

    @GetMapping("/refreshForm4SelCatg")
    public String refreshFormCxtx4SelCatg(HttpServletRequest request, Model model)
    {
        if (userSessSrv != null)
        {
            TY_Case_Form caseForm = userSessSrv.getCaseFormB4Submission();

            if (caseForm != null)
            {
                // Check for Special Category Customizations
                if (splCatgCus != null)
                {
                    log.info("Checking Special Category Customizations for Category ID : " + caseForm.getCatgDesc());
                    log.info("Getting Text for the Category IDfrom customizing...");
                    // Also set the Category Description in Upper Case
                    // Get the Category Description for the Category ID from Case Form
                    TY_CatgDetails catgDetails = catalogSrv.getCategoryDetails4Catg(caseForm.getCatgDesc(),
                            EnumCaseTypes.Learning, true);
                    if (catgDetails != null)
                    {
                        caseForm.setCatgText(catgDetails.getCatDesc());
                        log.info("Catg. Text for Category ID : " + caseForm.getCatgDesc() + " is : "
                                + catgDetails.getCatDesc());
                    }

                    if (CollectionUtils.isNotEmpty(splCatgCus.getSplCatgCus()))
                    {
                        Optional<TY_SplCatg> splCatgCusO = splCatgCus.getSplCatgCus().stream()
                                .filter(e -> e.getCatg().equals(caseForm.getCatgText().toUpperCase())).findFirst();

                        if (splCatgCusO.isPresent())
                        {
                            log.info("Special Category Customization Found for Category : "
                                    + caseForm.getCatgText().toUpperCase());
                            // Adjust the Case Form Context as per Special Category Customizations
                            String splCatgSrv = splCatgCusO.get().getMvsrv();
                            if (StringUtils.hasText(splCatgSrv))
                            {
                                log.info("Checking Special Category Service : " + splCatgSrv);
                                if (appCtxt != null)
                                {
                                    try
                                    {
                                        IF_SplCatgMVSrv splCatgService = (IF_SplCatgMVSrv) appCtxt.getBean(splCatgSrv);
                                        if (splCatgService != null)
                                        {
                                            log.info("Invoking Special Category Service : " + splCatgSrv);
                                            ModelAndView splCatgMV = splCatgService.getSplCatgModelAndView(caseForm);
                                            if (splCatgMV != null)
                                            {
                                                log.info("Special Category Service ModelAndView Retrieved : "
                                                        + splCatgMV.getViewName());
                                                // Merge Model Attributes
                                                model.addAllAttributes(splCatgMV.getModel());
                                                // Override View Name
                                                return splCatgMV.getViewName();
                                            }
                                        }
                                    }
                                    catch (BeansException be)
                                    {
                                        log.error("Special Category Service Bean Not Found: " + splCatgSrv, be);
                                    }
                                }
                            }

                        }
                    }
                }

                userSessSrv.setCaseFormB4Submission(null);

                // Normal Scenario - Catg. chosen Not relevant for Notes Template and/or
                // additional fields

                if ((StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                        || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                        && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
                {

                    Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                            .filter(g -> g.getCaseTypeEnum().toString().equals(EnumCaseTypes.Learning.toString()))
                            .findFirst();
                    if (cusItemO.isPresent() && catgTreeSrv != null)
                    {

                        model.addAttribute("caseTypeStr", EnumCaseTypes.Learning.toString());

                        // Populate User Details
                        TY_UserESS userDetails = new TY_UserESS();
                        userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                        model.addAttribute("userInfo", userDetails);

                        // clear Form errors on each refresh or a New Case form request
                        if (CollectionUtils.isNotEmpty(userSessSrv.getFormErrors()))
                        {
                            userSessSrv.clearFormErrors();
                        }

                        // also Upload the Catg. Tree as per Case Type
                        model.addAttribute("catgsList",
                                catalogTreeSrv.getCaseCatgTree4LoB(EnumCaseTypes.Learning).getCategories());

                        // Scan Current Catg for Templ. Load and or Additional Fields

                        // Scan for Template Load
                        TY_CatgTemplates catgTemplate = catalogTreeSrv.getTemplates4Catg(caseForm.getCatgDesc(),
                                EnumCaseTypes.Learning);
                        if (catgTemplate != null)
                        {

                            // Set Questionnaire for Category
                            caseForm.setTemplate(catgTemplate.getQuestionnaire());

                        }

                        if (vhlpUISrv != null)
                        {
                            model.addAllAttributes(coLaDDLBSrv.adjustCountryLanguageDDLB(caseForm.getCountry(),
                                    vhlpUISrv.getVHelpUIModelMap4LobCatg(EnumCaseTypes.Learning,
                                            caseForm.getCatgDesc())));
                        }

                        // Case Form Model Set at last
                        model.addAttribute("caseForm", caseForm);

                        if (attSrv != null)
                        {
                            if (CollectionUtils.isNotEmpty(attSrv.getAttachmentNames()))
                            {
                                model.addAttribute("attachments", attSrv.getAttachmentNames());
                            }
                        }

                        // Attachment file Size
                        model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
                    }
                    else
                    {

                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                        { EnumCaseTypes.Learning.toString() }, Locale.ENGLISH));
                    }

                }
            }

        }

        return caseFormViewLXSS;
    }

    @GetMapping("/refreshForm4AttUpload")
    public String refreshCaseFormPostAttachmentUpload(Model model)
    {
        String viewName = caseFormViewLXSS;

        if (attSrv != null && userSessSrv != null)
        {
            Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                    .filter(g -> g.getCaseTypeEnum().toString().equals(EnumCaseTypes.Learning.toString())).findFirst();
            if (cusItemO.isPresent() && catgTreeSrv != null)
            {

                TY_Case_Form caseForm = userSessSrv.getCaseFormB4Submission();

                model.addAttribute("caseTypeStr", EnumCaseTypes.Learning.toString());

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                if (userSessSrv != null)
                {
                    log.info("User Bound in Session..");
                }
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                if (vhlpUISrv != null)
                {
                    model.addAllAttributes(coLaDDLBSrv.adjustCountryLanguageDDLB(caseForm.getCountry(),
                            vhlpUISrv.getVHelpUIModelMap4LobCatg(EnumCaseTypes.Learning, caseForm.getCatgDesc())));
                }
                model.addAttribute("caseForm", caseForm);
                // also Place the form in Session
                userSessSrv.setCaseFormB4Submission(caseForm);

                model.addAttribute("formErrors", attSrv.getSessionMessages());

                // also Upload the Catg. Tree as per Case Type
                model.addAttribute("catgsList",
                        catalogTreeSrv.getCaseCatgTree4LoB(EnumCaseTypes.Learning).getCategories());

                model.addAttribute("attachments", attSrv.getAttachmentNames());

                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

            }

        }
        return viewName;

    }

    @GetMapping("/refreshFormReply4AttUpload")
    public String refreshCaseReplyFormPostAttachmentUpload(Model model)
    {
        String viewName = caseFormReplyLXSS;
        List<String> attMsgs = Collections.emptyList();
        TY_CaseEdit_Form caseEditForm = null;
        if (userSessSrv != null)
        {
            caseEditForm = userSessSrv.getCaseEditFormB4Submission();
            if (caseEditForm != null)
            {
                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                // Attachment to Local Storage Persistence Error
                attMsgs = attSrv.getSessionMessages();

                model.addAttribute("caseEditForm", caseEditForm);

                model.addAttribute("formErrors", attMsgs);

                model.addAttribute("attachments", attSrv.getAttachmentNames());
                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
            }
        }

        return viewName;

    }

}
