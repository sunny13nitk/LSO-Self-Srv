package com.sap.cap.esmapi.ui.controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplates;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatgSrv;
import com.sap.cap.esmapi.events.event.EV_CaseFormSplSubmit;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;
import com.sap.cap.esmapi.events.event.EV_CaseReplySubmit;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEditFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEdit_Form;
import com.sap.cap.esmapi.ui.pojos.TY_CaseFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.ui.pojos.TY_SplCatg_Seek;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.srv.intf.IF_SessAttachmentsService;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.uimodel.intf.IF_CountryLanguageVHelpAdj;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_VHelpLOBUIModelSrv;
import com.sap.cds.services.request.UserInfo;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/post")
@Slf4j
public class LSOPostController
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
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IF_SessAttachmentsService attSrv;

    @Autowired
    private TY_RLConfig rlConfig;

    @Autowired
    private IF_CountryLanguageVHelpAdj coLaDDLBSrv;

    private final String caseListVWRedirect = "redirect:/lso/";
    private final String caseFormErrorRedirect = "redirect:/lso/errForm/";
    private final String caseFormSplErrorRedirect = "redirect:/lso/errFormSpl/";
    private final String caseFormViewLXSS = "caseFormLSOLXSS";
    private final String caseFormReplyLXSS = "caseFormReplyLSOLXSS";
    private final String caseFormReplyErrorRedirect = "redirect:/lso/errCaseReply/";

    @PostMapping(value = "/saveCase", params = "action=saveCase")
    public String saveCase(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {

        String viewName = caseListVWRedirect;
        userSessSrv.clearActiveSubmission();
        if (caseForm != null && userSessSrv != null && userInfo.isAuthenticated())
        {
            if (userSessSrv.getUserDetails4mSession().isEmployee())
            {
                caseForm.setEmployee(true);
            }

            log.info("Processing of Case Form - UI layer :Begins....");
            TY_SplCatg_Seek splCatgSeek = userSessSrv.isSplCatg(caseForm.getCatgDesc());

            // Any Validation Error(s) on the Form or Submission not possible
            if (!userSessSrv.SubmitCaseForm(caseForm))
            {
                log.info("Error in Case Form!");

                if (splCatgSeek.isFound())
                {
                    log.info("Special Category Case Form redirection post Case Form Submission..");

                    // REplace
                    viewName = caseFormSplErrorRedirect;
                }
                else
                {
                    log.info("Not a Special Category Case Form!");
                    // Redirect to Error Processing of Form
                    viewName = caseFormErrorRedirect;
                }

            }
            else
            {
                // Fire Case Submission Event - To be processed Asyncronously

                if (userSessSrv != null)
                {
                    log.info("User Bound in Session.. Ready to fire event for Case Submission..");
                }

                if (splCatgSeek.isFound())
                {
                    log.info("Special Category Case Form Submission - Firing Special Case Submission Category Event..");
                    TY_CaseFormAsync caseFormAsync = userSessSrv.getCurrentForm4Submission();
                    // External/Internal User Pass to Asynch Thread as Session Scoped Service would
                    // not be accessible in Asynch thread
                    caseFormAsync.getCaseForm().setExternal(userSessSrv.getUserDetails4mSession().isExternal());
                    caseFormAsync.setDesProps(userSessSrv.getDestinationDetails4mUserSession());
                    EV_CaseFormSplSubmit eventCaseSplSubmit = new EV_CaseFormSplSubmit(this, caseFormAsync,
                            splCatgSeek);
                    applicationEventPublisher.publishEvent(eventCaseSplSubmit);
                    userSessSrv.setSubmissionActive();
                }
                else
                {
                    log.info("Normal Case Form Submission - Firing Generic Case Submission Event..");
                    TY_CaseFormAsync caseFormAsync = userSessSrv.getCurrentForm4Submission();
                    // External/Internal User Pass to Asynch Thread as Session Scoped Service would
                    // not be accessible in Asynch thread
                    caseFormAsync.getCaseForm().setExternal(userSessSrv.getUserDetails4mSession().isExternal());
                    caseFormAsync.setDesProps(userSessSrv.getDestinationDetails4mUserSession());
                    EV_CaseFormSubmit eventCaseSubmit = new EV_CaseFormSubmit(this, caseFormAsync);
                    applicationEventPublisher.publishEvent(eventCaseSubmit);
                    userSessSrv.setSubmissionActive();
                }

            }

            log.info("Processing of Case Form - UI layer :Ends....");
        }

        return viewName;

    }

    @PostMapping(value = "/saveCase", params = "action=upload")
    public String uploadAttachments(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {
        String viewName = caseFormViewLXSS;

        List<String> attMsgs = Collections.emptyList();

        if (caseForm != null && attSrv != null && userSessSrv != null)
        {

            log.info("Processing of Case Attachment Upload Form - UI layer :Begins....");
            if (caseForm.getAttachment() != null)
            {
                if (StringUtils.hasText(caseForm.getAttachment().getOriginalFilename()))
                {
                    // Clear Attachment Service Session Messages for subsequent roundtip
                    attSrv.clearSessionMessages();
                    if (!attSrv.addAttachment(caseForm.getAttachment()))
                    {
                        // Attachment to Local Storage Persistence Error
                        attMsgs = attSrv.getSessionMessages();

                    }

                }

            }

            // Clear form for New Attachment as Current Attachment already in Container
            caseForm.setAttachment(null);
            caseForm.setCatgChange(false);
            caseForm.setCatgText(null);

            Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                    .filter(g -> g.getCaseTypeEnum().toString().equals(EnumCaseTypes.Learning.toString())).findFirst();
            if (cusItemO.isPresent() && catgTreeSrv != null)
            {

                model.addAttribute("caseTypeStr", EnumCaseTypes.Learning.toString());

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                if (userSessSrv != null)
                {
                    log.info("User Bound in Session..");
                }
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                if (userSessSrv.getUserDetails4mSession().isEmployee())
                {
                    caseForm.setEmployee(true);
                }

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
                // also Place the form in Session
                userSessSrv.setCaseFormB4Submission(caseForm);

                model.addAttribute("formErrors", attMsgs);

                // also Upload the Catg. Tree as per Case Type
                model.addAttribute("catgsList",
                        catalogTreeSrv.getCaseCatgTree4LoB(EnumCaseTypes.Learning).getCategories());

                model.addAttribute("attachments", attSrv.getAttachmentNames());

                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
            }

            log.info("Processing of Case Attachment Upload Form - UI layer :Ends....");
        }

        return viewName;

    }

    @PostMapping(value = "/uploadAJAX")
    public boolean uploadFileAttachments(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {
        Boolean uploadSuccess = false;
        if (caseForm != null && attSrv != null && userSessSrv != null)
        {

            log.info("Processing of Case Attachment Upload Form - UI layer :Begins....");
            if (caseForm.getAttachment() != null)
            {
                if (StringUtils.hasText(caseForm.getAttachment().getOriginalFilename()))
                {
                    // Clear Attachment Service Session Messages for subsequent roundtip
                    attSrv.clearSessionMessages();
                    if (!attSrv.addAttachment(caseForm.getAttachment()))
                    {
                        // Attachment to Local Storage Persistence Error
                        uploadSuccess = false;
                        userSessSrv.addFormErrors(caseFormErrorRedirect);

                    }
                    else
                    {
                        uploadSuccess = true;
                    }

                }

            }

            // Clear form for New Attachment as Current Attachment already in Container
            caseForm.setAttachment(null);
            caseForm.setCatgChange(false);
            caseForm.setCatgText(null);
            if (userSessSrv.getUserDetails4mSession().isEmployee())
            {
                caseForm.setEmployee(true);
            }
            // Scan for Template Load
            TY_CatgTemplates catgTemplate = catalogTreeSrv.getTemplates4Catg(caseForm.getCatgDesc(),
                    EnumCaseTypes.Learning);
            if (catgTemplate != null)
            {

                // Set Questionnaire for Category
                caseForm.setTemplate(catgTemplate.getQuestionnaire());

            }
            userSessSrv.setCaseFormB4Submission(caseForm);

            log.info("Processing of Case Attachment Upload Form - UI layer :Ends....");
        }

        return uploadSuccess;
    }

    @PostMapping(value = "/saveCase", params = "action=catgChange")
    public String refreshCaseForm4Catg(@RequestParam(name = "_csrf") String csrfToken,
            @ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {

        String viewCaseForm = caseFormViewLXSS;
        if (caseForm != null && userSessSrv != null)
        {

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
                                vhlpUISrv.getVHelpUIModelMap4LobCatg(EnumCaseTypes.Learning, caseForm.getCatgDesc())));
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

        return viewCaseForm;

    }

    @PostMapping(value = "/saveCaseReply", params = "action=saveCaseEdit")
    public String saveCaseReply(@ModelAttribute("caseEditForm") TY_CaseEdit_Form caseReplyForm, Model model)
            throws EX_ESMAPI, IOException
    {

        String viewName = caseListVWRedirect;
        if (caseReplyForm != null && userSessSrv != null)
        {
            userSessSrv.clearActiveSubmission();
            log.info("Processing of Case Reply Form - UI layer :Begins....");

            // Any Validation Error(s) on the Form or Submission not possible
            if (!userSessSrv.SubmitCaseReply(caseReplyForm))
            {
                // Redirect to Error Processing of Form
                viewName = caseFormReplyErrorRedirect;
            }
            else
            {
                // Fire Case Submission Event - To be processed Asyncronously
                TY_CaseEditFormAsync caseEditFormAsync = userSessSrv.getCurrentReplyForm4Submission();

                // External/Internal User Pass to Asynch Thread as Session Scoped Service would
                // not be accessible in Asynch thread
                caseEditFormAsync.getCaseReply().setExternal(userSessSrv.getUserDetails4mSession().isExternal());
                caseEditFormAsync.setDesProps(userSessSrv.getDestinationDetails4mUserSession());
                EV_CaseReplySubmit eventCaseReplySubmit = new EV_CaseReplySubmit(this, caseEditFormAsync);
                applicationEventPublisher.publishEvent(eventCaseReplySubmit);
                userSessSrv.setSubmissionActive();
            }

            log.info("Processing of Case Form - UI layer :Ends....");
        }
        return viewName;
    }

    @PostMapping(value = "/saveCaseReply", params = "action=upload")
    public String uploadCaseReplyAttachment(@ModelAttribute("caseEditForm") TY_CaseEdit_Form caseReplyForm, Model model)
    {

        List<String> attMsgs = Collections.emptyList();
        if (caseReplyForm != null && userSessSrv != null)
        {
            if (StringUtils.hasText(caseReplyForm.getCaseDetails().getCaseGuid()))
            {

                // Get Case Details
                TY_CaseEdit_Form caseEditForm = userSessSrv
                        .getCaseDetails4Edit(caseReplyForm.getCaseDetails().getCaseGuid());

                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(caseReplyForm.getReply());

                // Clear form for New Attachment as Current Attachment already in Container
                caseEditForm.setAttachment(null);

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                if (caseReplyForm.getAttachment() != null)
                {
                    if (StringUtils.hasText(caseReplyForm.getAttachment().getOriginalFilename()))
                    {
                        // Clear Attachment Service Session Messages for subsequent roundtip
                        attSrv.clearSessionMessages();
                        if (!attSrv.addAttachment(caseReplyForm.getAttachment()))
                        {
                            // Attachment to Local Storage Persistence Error
                            attMsgs = attSrv.getSessionMessages();

                        }

                    }

                }

                userSessSrv.setCaseEditFormB4Submission(caseEditForm);

                model.addAttribute("caseEditForm", caseEditForm);

                model.addAttribute("formErrors", attMsgs);

                model.addAttribute("attachments", attSrv.getAttachmentNames());
                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

            }
        }

        return caseFormReplyLXSS;
    }

    @PostMapping(value = "/saveCase", params = "action=languAdj")
    public String adjustLanguage4Country(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {

        String viewCaseForm = caseFormViewLXSS;
        if (caseForm != null && userSessSrv != null)
        {

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
                        caseForm.setCatgChange(false);
                        caseForm.setCatgText(null);

                    }

                    if (vhlpUISrv != null && coLaDDLBSrv != null)
                    {
                        model.addAllAttributes(coLaDDLBSrv.adjustCountryLanguageDDLB(caseForm.getCountry(),
                                vhlpUISrv.getVHelpUIModelMap4LobCatg(EnumCaseTypes.Learning, caseForm.getCatgDesc())));
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

        return viewCaseForm;

    }

    @PostMapping("/selCatg")
    @ResponseBody
    public Boolean refreshCaseForm4CatgSel(@RequestParam(value = "catgDesc", required = false) String newCatg)
    {
        log.info("Received request to check category change for category 1 : " + newCatg);
        TY_Case_Form sessionForm = userSessSrv.getCaseFormB4Submission();

        if (sessionForm == null)
        {
            log.warn("Session form is null during category check. Treating as changed.");
            return true;
        }

        String currentCatg = sessionForm.getCatgDesc();

        // Normalize null to empty
        String normalizedCurrent = StringUtils.hasText(currentCatg) ? currentCatg : "";

        String normalizedNew = StringUtils.hasText(newCatg) ? newCatg : "";

        // Compare normalized values
        return !normalizedCurrent.equals(normalizedNew);
    }

    @PostMapping("/selCatg2")
    @ResponseBody
    public Boolean refreshCaseForm4Catg2Sel(@RequestParam(value = "catg2Desc", required = false) String newCatg2)
    {
        log.info("Received request to check category change for category 2 : " + newCatg2);
        TY_Case_Form sessionForm = userSessSrv.getCaseFormB4Submission();

        if (sessionForm == null)
        {
            log.warn("Session form is null during category check. Treating as changed.");
            return true;
        }

        String normalizedCurrent = StringUtils.hasText(sessionForm.getCatg2Desc()) ? sessionForm.getCatg2Desc() : "";

        String normalizedNew = StringUtils.hasText(newCatg2) ? newCatg2 : "";

        return !normalizedCurrent.equals(normalizedNew);
    }

    @PostMapping(value = "/uploadAJAXReply")
    public boolean uploadFileAttachmentsReply(@ModelAttribute("caseEditForm") TY_CaseEdit_Form caseReplyForm)
    {
        boolean uploadSuccess = false;

        if (caseReplyForm != null && userSessSrv != null)
        {
            log.info("Processing of Case Attachment Upload Form - UI layer :Begins....");
            if (StringUtils.hasText(caseReplyForm.getCaseDetails().getCaseGuid()))
            {
                // Get Case Details
                TY_CaseEdit_Form caseEditForm = userSessSrv
                        .getCaseDetails4Edit(caseReplyForm.getCaseDetails().getCaseGuid());

                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(caseReplyForm.getReply());

                // Clear form for New Attachment as Current Attachment already in Container
                caseEditForm.setAttachment(null);

                if (caseReplyForm.getAttachment() != null)
                {
                    if (StringUtils.hasText(caseReplyForm.getAttachment().getOriginalFilename()))
                    {
                        // Clear Attachment Service Session Messages for subsequent roundtip
                        attSrv.clearSessionMessages();
                        if (!attSrv.addAttachment(caseReplyForm.getAttachment()))
                        {
                            // Attachment to Local Storage Persistence Error
                            // attMsgs = attSrv.getSessionMessages();
                            uploadSuccess = false;
                            userSessSrv.addFormErrors(caseFormReplyErrorRedirect);
                            log.error("Attachment to Local Storage Persistence Error");

                        }
                        else
                        {
                            uploadSuccess = true;
                        }

                    }

                }
                userSessSrv.setCaseEditFormB4Submission(caseEditForm);

            }

        }

        return uploadSuccess;

    }

}
