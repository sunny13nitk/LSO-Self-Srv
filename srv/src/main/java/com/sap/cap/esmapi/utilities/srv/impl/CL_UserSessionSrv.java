package com.sap.cap.esmapi.utilities.srv.impl;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.SessionScope;

import com.sap.cap.esmapi.catg.pojos.TY_CatalogItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatalogTree;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_CaseAlreadyConfirmed;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.exceptions.EX_SessionExpired;
import com.sap.cap.esmapi.hana.logging.srv.intf.IF_HANALoggingSrv;
import com.sap.cap.esmapi.status.srv.intf.IF_StatusSrv;
import com.sap.cap.esmapi.ui.pojos.TY_Attachment;
import com.sap.cap.esmapi.ui.pojos.TY_CaseConfirmPOJO;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEditFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEdit_Form;
import com.sap.cap.esmapi.ui.pojos.TY_CaseFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.ui.srv.intf.IF_ESS_UISrv;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseDetails;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseESS;
import com.sap.cap.esmapi.utilities.pojos.TY_DestinationsSuffix;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesDetails;
import com.sap.cap.esmapi.utilities.pojos.TY_PreviousAttachments;
import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;
import com.sap.cap.esmapi.utilities.pojos.TY_SessionAttachment;
import com.sap.cap.esmapi.utilities.pojos.TY_UserDetails;
import com.sap.cap.esmapi.utilities.pojos.TY_UserSessionInfo;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cap.esmapi.utilities.srv.intf.IF_AttachmentsFetchSrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_SessAttachmentsService;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.intf.IF_DestinationService;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_VHelpLOBUIModelSrv;
import com.sap.cds.services.request.UserInfo;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.exception.DestinationAccessException;
import com.sap.cloud.security.token.Token;
import com.sap.cloud.security.token.TokenClaims;

import cds.gen.db.esmlogs.Esmappmsglog;
import lombok.extern.slf4j.Slf4j;

@Service
@SessionScope
@Slf4j
public class CL_UserSessionSrv implements IF_UserSessionSrv
{

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private UserInfo userInfo;

    @Autowired
    private IF_SrvCloudAPI srvCloudApiSrv;

    @Autowired
    private IF_ESS_UISrv essSrv;

    @Autowired
    private TY_RLConfig rlConfig;

    @Autowired
    private TY_CatgCus catgCusSrv;

    @Autowired
    private IF_CatalogSrv catalogSrv;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IF_VHelpLOBUIModelSrv vHlpModelSrv;

    @Autowired
    private IF_HANALoggingSrv hanaLogSrv;

    @Autowired
    private IF_StatusSrv statusSrv;

    @Autowired
    private IF_SessAttachmentsService attSrv;

    @Autowired
    private IF_AttachmentsFetchSrv attFetchSrv;

    @Autowired
    private TY_DestinationsSuffix dS;

    @Autowired
    private IF_DestinationService destSrv;

    // Properties
    private TY_UserSessionInfo userSessInfo;

    @Override
    @PreAuthorize("isAuthenticated()")
    public TY_UserDetails getUserDetails(Token token) throws EX_ESMAPI
    {

        String newAccountID = null;
        // Token Blank
        if (token == null)
        {
            log.error(msgSrc.getMessage("NO_TOKEN", null, Locale.ENGLISH));
            throw new EX_ESMAPI(msgSrc.getMessage("NO_TOKEN", null, Locale.ENGLISH));

        }
        else
        {
            // Unauthenticated User
            if (!userInfo.isAuthenticated())
            {
                log.error(msgSrc.getMessage("UNAUTHENTICATED_ACCESS", new Object[]
                { token.getClaimAsString(TokenClaims.USER_NAME) }, Locale.ENGLISH));
                throw new EX_ESMAPI(msgSrc.getMessage("UNAUTHENTICATED_ACCESS", new Object[]
                { token.getClaimAsString(TokenClaims.USER_NAME) }, Locale.ENGLISH));
            }

            else
            {
                // #AUTH
                // Role Checks to be explicitly handled here
                if (CollectionUtils.isNotEmpty(userInfo.getRoles()))
                {
                    // Explicit Role Check for Interals and Externals and error in case of
                    // unassigned Role
                }

                if (userSessInfo == null)
                {
                    log.info("User Session Info. Instantiated!");
                    userSessInfo = new TY_UserSessionInfo();
                }

                // Return from Session if Populated else make some effort
                if (userSessInfo.getUserDetails() == null)
                {
                    // Fetch and Return
                    TY_UserDetails userDetails = new TY_UserDetails();
                    log.info("Fetching Logged in User Details!!");
                    userDetails.setAuthenticated(true);
                    userDetails.setRoles(userInfo.getRoles().stream().collect(Collectors.toList()));
                    Ty_UserAccountEmployee usAccConEmpl = new Ty_UserAccountEmployee();

                    if (StringUtils.hasText(token.getClaimAsString(TokenClaims.USER_NAME))
                            && StringUtils.hasText(token.getClaimAsString(TokenClaims.GIVEN_NAME))
                            && StringUtils.hasText(token.getClaimAsString(TokenClaims.FAMILY_NAME))
                            && StringUtils.hasText(token.getClaimAsString(TokenClaims.EMAIL)))
                    {
                        userSessInfo.getTokenDetails().put(GC_Constants.gc_TokenAttrib_ClientID, token.getClientId());
                        userSessInfo.getTokenDetails().put(GC_Constants.gc_TokenAttrib_UserName,
                                token.getClaimAsString(TokenClaims.USER_NAME));
                        userSessInfo.getTokenDetails().put(GC_Constants.gc_TokenAttrib_FirstName,
                                token.getClaimAsString(TokenClaims.GIVEN_NAME));

                        userSessInfo.getTokenDetails().put(GC_Constants.gc_TokenAttrib_LastName,
                                token.getClaimAsString(TokenClaims.FAMILY_NAME));

                        userSessInfo.getTokenDetails().put(GC_Constants.gc_TokenAttrib_Email,
                                token.getClaimAsString(TokenClaims.EMAIL));

                        log.info("Logged In User via Token : "
                                + userSessInfo.getTokenDetails().get(TokenClaims.USER_NAME));
                        log.info("User Name : " + userSessInfo.getTokenDetails().get(TokenClaims.GIVEN_NAME) + " "
                                + userSessInfo.getTokenDetails().get(TokenClaims.FAMILY_NAME));
                        log.info("User Email : " + userSessInfo.getTokenDetails().get(TokenClaims.EMAIL));

                    }
                    else
                    {
                        log.info("Token Does not contain Complete Information");
                        log.info(token.getClaimAsString(TokenClaims.USER_NAME) + " : "
                                + token.getClaimAsString(TokenClaims.FAMILY_NAME)
                                + token.getClaimAsString(TokenClaims.GIVEN_NAME) + "Email: "
                                + token.getClaimAsString(TokenClaims.EMAIL));
                    }

                    usAccConEmpl.setUserId(userSessInfo.getTokenDetails().get(GC_Constants.gc_TokenAttrib_UserName));
                    usAccConEmpl.setUserName(userSessInfo.getTokenDetails().get(GC_Constants.gc_TokenAttrib_FirstName)
                            + " " + userSessInfo.getTokenDetails().get(GC_Constants.gc_TokenAttrib_LastName));

                    // External/Internal User Classification
                    if (StringUtils.hasText(userSessInfo.getTokenDetails().get(GC_Constants.gc_TokenAttrib_UserName)))
                    {
                        if (!userSessInfo.getTokenDetails().get(GC_Constants.gc_TokenAttrib_UserName)
                                .matches(rlConfig.getInternalUsersRegex()))
                        {
                            log.info("User Marked as External User!");
                            usAccConEmpl.setExternal(true);
                            if (dS != null)
                            {
                                if (StringUtils.hasText(dS.getDestExternal()))
                                {
                                    usAccConEmpl.setDestination(dS.getDestExternal());
                                    log.info("External Destination set up for External User : " + dS.getDestExternal());
                                }
                            }

                        }
                        else
                        {
                            log.info("User Marked as Internal User!");
                            if (dS != null)
                            {
                                if (StringUtils.hasText(dS.getDestInternal()))
                                {
                                    usAccConEmpl.setDestination(dS.getDestInternal());
                                    log.info("Internal Destination set up for Internal User : " + dS.getDestInternal());
                                }
                            }

                        }

                        // Initialize Destination Service
                        if (rlConfig.isEnableDestinationCheck())
                        {
                            try
                            {
                                TY_DestinationProps desProps = destSrv
                                        .getDestinationDetails4User(usAccConEmpl.getDestination());
                                if (desProps != null)
                                {
                                    log.info("Destination connection established successfully for -  "
                                            + usAccConEmpl.getDestination());
                                    // #TEST
                                    log.info(desProps.toString());
                                    userSessInfo.setDestinationProps(desProps);
                                }
                            }
                            catch (EX_ESMAPI e)
                            {
                                handleDestinationLoadError(usAccConEmpl.getUserName(), usAccConEmpl.getDestination(),
                                        e.getLocalizedMessage());
                            }

                        }
                    }

                    usAccConEmpl.setUserEmail(userSessInfo.getTokenDetails().get(GC_Constants.gc_TokenAttrib_Email));
                    log.info("Scanning Account for Email Address : " + usAccConEmpl.getUserEmail());
                    usAccConEmpl.setAccountId(srvCloudApiSrv.getAccountIdByUserEmail(usAccConEmpl.getUserEmail(),
                            userSessInfo.getDestinationProps()));

                    // Only seek Employee If Account/Contact not Found
                    if (!StringUtils.hasText(usAccConEmpl.getAccountId()))
                    {
                        log.info("No Account Identified for Logged in User Email : " + usAccConEmpl.getUserEmail());
                        // If not an External Employee - Only then Seek Employee
                        if (!usAccConEmpl.isExternal())
                        {
                            // Seek Employee and populate
                            usAccConEmpl.setEmployeeId(srvCloudApiSrv.getEmployeeIdByUserId(usAccConEmpl.getUserId(),
                                    userSessInfo.getDestinationProps()));
                            if (StringUtils.hasText(usAccConEmpl.getEmployeeId()))
                            {
                                usAccConEmpl.setEmployee(true);
                            }
                            else
                            {
                                userDetails.setUsAccEmpl(usAccConEmpl);
                                userSessInfo.setUserDetails(userDetails); // Set in Session
                                // Go For Individual Customer Creation with the User Details
                                newAccountID = this.createAccount();
                            }
                        }
                        else // External User - Customer Not Found - Direct Customer Creation
                        {
                            userDetails.setUsAccEmpl(usAccConEmpl);
                            userSessInfo.setUserDetails(userDetails); // Set in Session
                            // Go For Individual Customer Creation with the User Details
                            newAccountID = this.createAccount();
                        }

                        userDetails.setUsAccEmpl(usAccConEmpl);
                        userSessInfo.setUserDetails(userDetails); // Set in Session

                        if (StringUtils.hasText(newAccountID))
                        {
                            userSessInfo.getUserDetails().getUsAccEmpl().setAccountId(newAccountID);
                        }

                    }
                    else
                    {
                        userDetails.setUsAccEmpl(usAccConEmpl);
                        userSessInfo.setUserDetails(userDetails); // Set in Session
                    }
                    log.info("User Details populated in Session : "
                            + userSessInfo.getUserDetails().getUsAccEmpl().toString());

                }
            }

        }

        return userSessInfo.getUserDetails();
    }

    @Override
    public TY_UserSessionInfo getESSDetails(Token token, boolean refresh) throws EX_ESMAPI
    {
        // Token must be present
        if (token != null)
        {
            // get User Details with Token
            getUserDetails(token);

            // Reload Cases if Refresh Requested or Cases List Blank
            if (refresh || CollectionUtils.isEmpty(userSessInfo.getCases()))
            {
                if (userSessInfo.getUserDetails() != null)
                {
                    try
                    {

                        // Get ONLY Learning Cases for User
                        userSessInfo.setCases(essSrv.getCases4User(userSessInfo.getUserDetails().getUsAccEmpl(),
                                EnumCaseTypes.Learning));

                        if (CollectionUtils.isNotEmpty(userSessInfo.getSubmissionIDs()))
                        {
                            // Seek Case IDs for Submissions
                            updateCases4SubmissionIds();
                        }

                    }
                    catch (Exception e)
                    {
                        // Log error
                        log.error(msgSrc.getMessage("ERR_CASES_USER", new Object[]
                        { userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), e.getLocalizedMessage() },
                                Locale.ENGLISH));

                        // Raise Exception to be handled at UI via Central Aspect
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASES_USER", new Object[]
                        { userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), e.getLocalizedMessage() },
                                Locale.ENGLISH));
                    }
                }
            }

        }

        return this.userSessInfo;
    }

    // @formatter:off -- Submit Case Form
    // : After comsumer Call to Rate Limit is Successful - Caller Resp.
    // : Form Data Saved in session :currentForm4Submission
    // --Validate Case Form - Implicit Call
    // ---- Fail
    // ------- Message Logging Event
    //
    // ------- Message Stack in Session Populated and REturn false
    // ---- Succ
    // ------- Create and Publish Case Submit Event
    // ------- session :currentForm4Submission : update valid flag to be picked up
    // ------- by Event Handler
    // @formatter:on
    @Override
    public boolean SubmitCaseForm(TY_Case_Form caseForm)
    {
        boolean isSubmitted = true;

        // Clear Buffer for Previous Form Submission
        clearPreviousSubmission4mSessionBuffer();

        if (caseForm != null && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()) && vHlpModelSrv != null)
        {
            // Push Form data to Session
            TY_CaseFormAsync caseFormAsync = new TY_CaseFormAsync();

            // Check if Country/Language are Mandatory for chosen Category in Form at time
            // of Submission

            // Make the Case Enum Scan Generic
            // Get Case Type Enum from Case Transaction Type
            Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                    .filter(f -> f.getCaseType().equals(caseForm.getCaseTxnType())).findFirst();
            if (cusItemO.isPresent())
            {

                Map<String, List<TY_KeyValue>> vHlpsMap = vHlpModelSrv
                        .getVHelpUIModelMap4LobCatg(cusItemO.get().getCaseTypeEnum(), caseForm.getCatgDesc());
                // Some Attributes Relevant for Current Category
                if (vHlpsMap.size() > 0)
                { // Country Field Relevant
                    if (CollectionUtils.isNotEmpty(vHlpsMap.get(GC_Constants.gc_LSO_COUNTRY)))
                    {
                        caseForm.setCountryMandatory(true);
                    }
                    else // Remove if Country field is not relevant for Current Category and passed on //
                         // from Form Buffer
                    {
                        caseForm.setCountry(null);
                    }

                    // Language Field Relevant
                    if (CollectionUtils.isNotEmpty(vHlpsMap.get(GC_Constants.gc_LSO_LANGUAGE)))
                    {
                        caseForm.setLangMandatory(true);
                    }
                    else // Remove if Country field is not relevant for Current Category and passed on
                         // from Form Buffer
                    {
                        caseForm.setLanguage(null);
                    }

                }

            }

            // Format the text input for New Lines
            String formattedReply = caseForm.getDescription().replaceAll("\r\n", "<br/>");
            if (StringUtils.hasText(formattedReply))
            {
                caseForm.setDescription(formattedReply);
            }

            caseFormAsync.setCaseForm(caseForm);
            caseFormAsync.setSubmGuid(UUID.randomUUID().toString());
            // Latest time Stamp from Form Submissions
            caseFormAsync.setTimestamp(Timestamp.from(Instant.now()));
            caseFormAsync.setUserId(userSessInfo.getUserDetails().getUsAccEmpl().getUserId());

            if (!CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))

            {

                if (cusItemO.isPresent() && catalogSrv != null)
                {
                    String[] catTreeSelCatg = catalogSrv.getCatgHierarchyforCatId(caseForm.getCatgDesc(),
                            cusItemO.get().getCaseTypeEnum());
                    caseFormAsync.setCatTreeSelCatg(catTreeSelCatg);
                }

                userSessInfo.setCurrentForm4Submission(caseFormAsync);

                // Validate Case Form : Implicit Call
                if (this.isCaseFormValid())
                {

                    // Attachments to be handled Here in Session service Only once the Form is
                    // Validated Successfully
                    try
                    {
                        if (attSrv != null)
                        {
                            // Attachments Bound
                            if (CollectionUtils.isNotEmpty(attSrv.getAttachments()))
                            {
                                int i = 0;
                                for (TY_SessionAttachment attachment : attSrv.getAttachments())
                                {
                                    // Create Attachment
                                    TY_Attachment newAttachment = new TY_Attachment(
                                            userSessInfo.getUserDetails().getUsAccEmpl().isExternal(),
                                            FilenameUtils.getName(attachment.getName()),
                                            GC_Constants.gc_Attachment_Category, false);
                                    caseFormAsync.getAttRespList().add(srvCloudApiSrv.createAttachment(newAttachment,
                                            userSessInfo.getDestinationProps()));
                                    if (caseFormAsync.getAttRespList().get(i) != null)
                                    {
                                        if (srvCloudApiSrv.persistAttachment(
                                                caseFormAsync.getAttRespList().get(i).getUploadUrl(),
                                                attachment.getName(), attachment.getBlob(),
                                                userSessInfo.getDestinationProps()))
                                        {
                                            log.info("Attachment with id : "
                                                    + caseFormAsync.getAttRespList().get(i).getId()
                                                    + " Persisted in Document Container.. for Submission ID: "
                                                    + caseFormAsync.getSubmGuid());
                                        }
                                    }

                                    i++;
                                }
                            }
                        }

                    }
                    catch (EX_ESMAPI | IOException e)
                    {

                        if (e instanceof IOException)
                        {
                            handleNoFileDataAttachment(caseFormAsync);
                        }
                        else if (e instanceof EX_ESMAPI)
                        {
                            handleAttachmentPersistError(caseFormAsync, e);
                        }

                        isSubmitted = false;
                    }
                    userSessInfo.getCurrentForm4Submission().setValid(true);

                    // SUCC_CASE_SUBM=Case with submission id - {0} of Type - {1} submitted
                    // Successfully for User - {2}!
                    String msg = msgSrc.getMessage("SUCC_CASE_SUBM", new Object[]
                    { caseFormAsync.getSubmGuid(), caseFormAsync.getCaseForm().getCaseTxnType(),
                            caseFormAsync.getUserId() }, Locale.ENGLISH);
                    log.info(msg); // System Log

                    // Logging Framework
                    TY_Message logMsg = new TY_Message(caseFormAsync.getUserId(), caseFormAsync.getTimestamp(),
                            EnumStatus.Success, EnumMessageType.SUCC_CASE_SUBM, caseFormAsync.getSubmGuid(), msg);
                    userSessInfo.getMessagesStack().add(logMsg);
                    // Instantiate and Fire the Event : Syncronous processing
                    EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
                    applicationEventPublisher.publishEvent(logMsgEvent);

                    // Add to Display Messages : to be shown to User or Successful Submission
                    this.addSessionMessage(msg);

                    // Add Submission Guids to Session Context for REconcillation after Case
                    // Creation Process
                    this.userSessInfo.getSubmissionIDs().add(caseFormAsync.getSubmGuid());

                    isSubmitted = true;

                }
                else // Error Handling :Payload Error
                {
                    // Message Handling Implicitly done via call to Form Validity Check
                    isSubmitted = false;
                }

            }

        }

        return isSubmitted;
    }

    @Override
    public String createAccount() throws EX_ESMAPI
    {
        log.info("Inside Account Creation Routine...");
        String accountId = null;
        // Only if no Account or Employee Identified in Current Session
        // No Account Determined
        if (!StringUtils.hasText(userSessInfo.getUserDetails().getUsAccEmpl().getAccountId()))
        {
            log.info("No Account Identified....");
            // No Employee determined
            if (!StringUtils.hasText(userSessInfo.getUserDetails().getUsAccEmpl().getEmployeeId()))
            {
                log.info("No Employee Identified....");
                // Create new Individual Customer Account with User Credentials
                // User Email and UserName Bound
                if (StringUtils.hasText(userSessInfo.getUserDetails().getUsAccEmpl().getUserEmail())
                        && StringUtils.hasText(userSessInfo.getUserDetails().getUsAccEmpl().getUserName()))
                {

                    try
                    {
                        accountId = srvCloudApiSrv.createAccount(
                                userSessInfo.getUserDetails().getUsAccEmpl().getUserEmail(),
                                userSessInfo.getUserDetails().getUsAccEmpl().getUserName(),
                                userSessInfo.getDestinationProps());
                        // Also update in the session for newly created Account
                        if (StringUtils.hasText(accountId))
                        {
                            userSessInfo.getUserDetails().getUsAccEmpl().setAccountId(accountId);
                            // Session Display Message
                            this.addSessionMessage(msgSrc.getMessage("NEW_AC", new Object[]
                            { userSessInfo.getUserDetails().getUsAccEmpl().getUserId() }, Locale.ENGLISH));
                            // Add to Log
                            log.info(msgSrc.getMessage("NEW_AC", new Object[]
                            { userSessInfo.getUserDetails().getUsAccEmpl().getUserId() }, Locale.ENGLISH));

                            TY_Message message = new TY_Message(
                                    userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                    Timestamp.from(Instant.now()), EnumStatus.Success, EnumMessageType.SUCC_ACC_CREATE,
                                    accountId, msgSrc.getMessage("NEW_AC", new Object[]
                                    { userSessInfo.getUserDetails().getUsAccEmpl().getUserId() }, Locale.ENGLISH));
                            // For Logging Framework
                            userSessInfo.getMessagesStack().add(message);
                            // Instantiate and Fire the Event
                            EV_LogMessage logMsgEvent = new EV_LogMessage(this, message);
                            applicationEventPublisher.publishEvent(logMsgEvent);
                        }
                    }
                    catch (EX_ESMAPI ex) // Any Error During Individual Customer Creation for the User
                    {
                        log.error(msgSrc.getMessage("ERR_API_AC", new Object[]
                        { userSessInfo.getUserDetails().getUsAccEmpl().getUserId() }, Locale.ENGLISH));

                        TY_Message message = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_ACC_CREATE,
                                userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                msgSrc.getMessage("ERR_API_AC", new Object[]
                                { userSessInfo.getUserDetails().getUsAccEmpl().getUserId() }, Locale.ENGLISH));
                        // For Logging Framework
                        userSessInfo.getMessagesStack().add(message);
                        // Instantiate and Fire the Event
                        EV_LogMessage logMsgEvent = new EV_LogMessage(this, message);
                        applicationEventPublisher.publishEvent(logMsgEvent);

                    }

                }

            }
        }

        return accountId;
    }

    @Override
    public Ty_UserAccountEmployee getUserDetails4mSession()
    {
        if (this.userSessInfo.getUserDetails() != null)
        {
            return userSessInfo.getUserDetails().getUsAccEmpl();
        }

        return null;
    }

    @Override
    public void addSessionMessage(String msg)
    {
        if (StringUtils.hasText(msg))
        {
            if (userSessInfo.getMessages() == null)
            {
                userSessInfo.setMessages(new ArrayList<String>());
            }
            userSessInfo.getMessages().add(msg);
        }
    }

    @Override
    public List<String> getSessionMessages()
    {
        return userSessInfo.getMessages();
    }

    @Override
    public boolean isWithinRateLimit()
    {

        boolean withinRateLimit = true;

        if (userSessInfo == null)
        {
            throw new EX_SessionExpired("User Session expired due to Inactivity. Please Sign In to Proceed!");
        }

        // Rate Config Specified
        if (rlConfig != null)
        {
            if (CollectionUtils.isNotEmpty(userSessInfo.getFormSubmissions().getFormSubmissions()))
            {
                // Current # Submissions more than or equals to # configurable - check
                if (userSessInfo.getFormSubmissions().getFormSubmissions().size() >= rlConfig.getNumFormSubms())
                {
                    // Get Current Time Stamp
                    Timestamp currTS = Timestamp.from(Instant.now());
                    // Get Top N :latest Submissions since submissions are always appended
                    // chronologically
                    List<Timestamp> topNSubmList = new ArrayList<Timestamp>();
                    topNSubmList = userSessInfo.getFormSubmissions().getFormSubmissions();
                    Collections.sort(topNSubmList, Collections.reverseOrder());

                    // Compare the Time difference from the latest one
                    long secsElapsedLastSubmit = (currTS.getTime() - topNSubmList.get(0).getTime()) / 1000;
                    // Last Submission elapsed time less than
                    if (secsElapsedLastSubmit < rlConfig.getIntvSecs())
                    {
                        withinRateLimit = false;
                        userSessInfo.setRateLimitBreached(true);
                        log.error(msgSrc.getMessage("ERR_RATE_LIMIT", new Object[]
                        { userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(currTS) }, Locale.ENGLISH));

                        TY_Message logMsg = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                currTS, EnumStatus.Error, EnumMessageType.ERR_RATELIMIT,
                                userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                msgSrc.getMessage("ERR_RATE_LIMIT", new Object[]
                                { userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                        new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(currTS) }, Locale.ENGLISH));
                        // For Logging Framework
                        userSessInfo.getMessagesStack().add(logMsg);
                        // Instantiate and Fire the Event
                        EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
                        applicationEventPublisher.publishEvent(logMsgEvent);

                    }
                    else
                    {
                        // Clear the older Submissions Details - Since now the clock is refreshed for
                        // Current Session after Waiting
                        userSessInfo.getFormSubmissions().getFormSubmissions().clear();
                        withinRateLimit = true;
                        userSessInfo.setRateLimitBreached(false);
                        userSessInfo.getFormSubmissions().getFormSubmissions().add(currTS);
                    }

                }
                else // Just add the Form Submission time Stamp to Session
                {
                    userSessInfo.getFormSubmissions().getFormSubmissions().add(Timestamp.from(Instant.now()));
                    userSessInfo.setRateLimitBreached(false);
                    withinRateLimit = true;
                }

            }

            else // Just add the Form Submission time Stamp to Session
            {
                userSessInfo.getFormSubmissions().getFormSubmissions().add(Timestamp.from(Instant.now()));
                userSessInfo.setRateLimitBreached(false);
                withinRateLimit = true;
            }

        }

        return withinRateLimit;
    }

    @Override
    public boolean checkRateLimit()
    {

        boolean withinRateLimit = true;

        if (userSessInfo == null)
        {
            throw new EX_SessionExpired("User Session expired due to Inactivity. Please Sign In to Proceed!");
        }

        // Rate Config Specified
        if (rlConfig != null)
        {
            if (CollectionUtils.isNotEmpty(userSessInfo.getFormSubmissions().getFormSubmissions()))
            {
                // Current # Submissions more than or equals to # configurable - check
                if (userSessInfo.getFormSubmissions().getFormSubmissions().size() >= rlConfig.getNumFormSubms())
                {

                    int numSubm, numSubmsWithinSlot = 0;

                    numSubm = userSessInfo.getFormSubmissions().getFormSubmissions().size();
                    // Get Current Time Stamp
                    Timestamp currTS = Timestamp.from(Instant.now());
                    // Get Top N :latest Submissions since submissions are always appended
                    // chronologically
                    List<Timestamp> topNSubmList = new ArrayList<Timestamp>();
                    topNSubmList = userSessInfo.getFormSubmissions().getFormSubmissions();

                    for (Timestamp submTimeStamp : topNSubmList)
                    {
                        // Compare the Time difference from the latest one
                        long secsElapsedLastSubmit = (currTS.getTime() - submTimeStamp.getTime()) / 1000;
                        if (secsElapsedLastSubmit < rlConfig.getIntvSecs())
                        {
                            numSubmsWithinSlot++;
                        }
                    }

                    // Total Session Form Submissions
                    // more than config allowed && all created within the same allowed configured
                    // time slot
                    if ((numSubmsWithinSlot >= numSubm) && (numSubm >= rlConfig.getNumFormSubms()))
                    {
                        withinRateLimit = false;
                        userSessInfo.setRateLimitBreached(true);
                        log.error(msgSrc.getMessage("ERR_RATE_LIMIT", new Object[]
                        { userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(currTS) }, Locale.ENGLISH));

                        TY_Message logMsg = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                currTS, EnumStatus.Error, EnumMessageType.ERR_RATELIMIT,
                                userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                msgSrc.getMessage("ERR_RATE_LIMIT", new Object[]
                                { userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                        new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(currTS) }, Locale.ENGLISH));
                        // For Logging Framework
                        userSessInfo.getMessagesStack().add(logMsg);
                        // Instantiate and Fire the Event
                        EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
                        applicationEventPublisher.publishEvent(logMsgEvent);
                    }
                    else
                    {
                        // Clear the older Submissions Details - Since now the clock is refreshed for
                        // Current Session after Waiting
                        userSessInfo.getFormSubmissions().getFormSubmissions().clear();
                        withinRateLimit = true;
                        userSessInfo.setRateLimitBreached(false);
                    }

                }

            }

        }

        return withinRateLimit;
    }

    @Override
    public boolean isCaseFormValid()
    {
        boolean isValid = true;
        if (userSessInfo == null)
        {
            throw new EX_SessionExpired("User Session expired due to Inactivity. Please Sign In to Proceed!");
        }

        // Get the Latest Form Submission from Session and Validate
        if (userSessInfo.getCurrentForm4Submission() != null && catalogSrv != null
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
        {
            if (userSessInfo.getCurrentForm4Submission().getCaseForm() != null)
            {
                // Subject and Category are Mandatory fields
                if (StringUtils.hasText(userSessInfo.getCurrentForm4Submission().getCaseForm().getSubject())
                        && StringUtils.hasText(userSessInfo.getCurrentForm4Submission().getCaseForm().getCatgDesc()))
                {

                    // Include Country and Language mandatory check for certain category as
                    // requested by business.
                    if (userSessInfo.getCurrentForm4Submission().getCaseForm().isCountryMandatory())
                    {
                        if (!StringUtils.hasText(userSessInfo.getCurrentForm4Submission().getCaseForm().getCountry()))
                        {
                            // Payload Error as Category level shuld be atleast 2
                            handleMandatoryFieldMissingError(GC_Constants.gc_LSO_COUNTRY_DESC);
                            return false;
                        }
                    }

                    if (userSessInfo.getCurrentForm4Submission().getCaseForm().isLangMandatory())
                    {
                        if (!StringUtils.hasText(userSessInfo.getCurrentForm4Submission().getCaseForm().getLanguage()))
                        {
                            // Payload Error as Category level shuld be atleast 2
                            handleMandatoryFieldMissingError(GC_Constants.gc_LSO_LANGUAGE_DESC);
                            return false;
                        }
                    }

                    Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                            .filter(g -> g.getCaseType()
                                    .equals(userSessInfo.getCurrentForm4Submission().getCaseForm().getCaseTxnType()))
                            .findFirst();

                    if (cusItemO.isPresent())
                    {
                        // Only Validate for Category Lower than level 1 if Set in Customization for
                        // Multi level Categories in Case Type
                        if (!cusItemO.get().getToplvlCatgOnly())
                        {

                            long catLen = Arrays.stream(catalogSrv.getCatgHierarchyforCatId(
                                    userSessInfo.getCurrentForm4Submission().getCaseForm().getCatgDesc(),
                                    cusItemO.get().getCaseTypeEnum())).filter(Objects::nonNull).count();
                            // Check that Category is not a level 1 - Base Category
                            if (catLen <= 1)
                            {
                                // Extract Category Description
                                TY_CatalogTree catgTree = catalogSrv
                                        .getCaseCatgTree4LoB(cusItemO.get().getCaseTypeEnum());
                                if (catgTree != null)
                                {
                                    if (CollectionUtils.isNotEmpty(catgTree.getCategories()))
                                    {

                                        // Remove blank Categories from Catalog Tree Used for UI Presentation
                                        catgTree.getCategories().removeIf(x -> x.getId() == null);
                                        Optional<TY_CatalogItem> currCatgDetailsO = catgTree
                                                .getCategories().stream().filter(f -> f.getId().equals(userSessInfo
                                                        .getCurrentForm4Submission().getCaseForm().getCatgDesc()))
                                                .findFirst();
                                        if (currCatgDetailsO.isPresent())
                                        {
                                            // Payload Error as Category level shuld be atleast 2
                                            String msg = msgSrc.getMessage("ERR_CATG_LVL", new Object[]
                                            { currCatgDetailsO.get().getName() }, Locale.ENGLISH);
                                            log.error(msg); // System Log

                                            // Logging Framework
                                            TY_Message logMsg = new TY_Message(
                                                    userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                                                    Timestamp.from(Instant.now()), EnumStatus.Error,
                                                    EnumMessageType.ERR_PAYLOAD,
                                                    userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), msg);
                                            userSessInfo.getMessagesStack().add(logMsg);

                                            // Instantiate and Fire the Event : Syncronous processing
                                            EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
                                            applicationEventPublisher.publishEvent(logMsgEvent);

                                            this.addFormErrors(msg);// For Form Display

                                        }
                                        // Refurbish Blank Category at Top for New Form - Session maintained
                                        catgTree.getCategories().add(0, new TY_CatalogItem());
                                    }
                                }

                                // Add to Display Messages : to be shown to User or Successful Submission
                                isValid = false;

                            }

                        }
                    }

                }
                else
                {

                    // Payload error

                    handleCaseReplyError();

                    isValid = false;

                }

            }
        }

        return isValid;

    }

    @Override
    public void addMessagetoStack(TY_Message msg)
    {

        userSessInfo.getMessagesStack().add(msg);
    }

    @Override
    public List<TY_Message> getMessageStack()
    {
        return userSessInfo.getMessagesStack();
    }

    @Override
    public boolean getCurrentRateLimitBreachedValue()
    {
        boolean rateLimitBreachedVal = false;
        if (userSessInfo != null)
        {
            rateLimitBreachedVal = userSessInfo.isRateLimitBreached();
        }

        return rateLimitBreachedVal;
    }

    // #Test
    @Override
    public void loadUser4Test()
    {

        if (userSessInfo == null)
        {
            userSessInfo = new TY_UserSessionInfo();
        }
        if (userSessInfo.getUserDetails() == null)
        {

            TY_UserDetails userDetails = new TY_UserDetails();

            /*
             * Test with Existing Employee
             */
            String userEmail = "sunny.bhardwaj@sap.com";
            String userId = "I057386";
            String userName = "Sunny Bhardwaj";

            userDetails.setAuthenticated(true);
            //
            userDetails.setRoles(userInfo.getRoles().stream().collect(Collectors.toList()));

            /*
             * Test with Existing Employee
             */

            /*
             * Test with Existing Customer
             */
            // String userEmail = "rsharma@gmail.com";
            // String userId = "P565GJJH";
            // String userName = "Rohit Sharma";

            // userDetails.setAuthenticated(true);
            // //
            // userDetails.setRoles(userInfo.getRoles().stream().collect(Collectors.toList()));

            // Ty_UserAccountEmployee usAccConEmpl = new Ty_UserAccountEmployee(userId,
            // userName, userEmail,
            // srvCloudApiSrv.getAccountIdByUserEmail(userEmail),
            // srvCloudApiSrv.getEmployeeIdByUserId(userId),
            // false, false);

            /*
             * Test with Existing Customer
             */

            /*
             * Test with New Customer //
             */
            // String userEmail = "narendramodi@gmail.com";
            // String userId = "SJH86775";
            // String userName = "Narendra Modi";

            // userDetails.setAuthenticated(true);
            // //
            // userDetails.setRoles(userInfo.getRoles().stream().collect(Collectors.toList()));

            /*
             * Test with New Customer
             */

            Ty_UserAccountEmployee usAccConEmpl = new Ty_UserAccountEmployee();
            usAccConEmpl.setUserId(userId);
            usAccConEmpl.setUserName(userName);
            usAccConEmpl.setUserEmail(userEmail);

            if (StringUtils.hasText(userId))
            {
                // If External User
                if (!userId.matches(rlConfig.getInternalUsersRegex()))
                {
                    usAccConEmpl.setExternal(true);

                    TY_DestinationProps desProps = destSrv.getDestinationDetails4User(dS.getDestExternal());
                    if (desProps != null)
                    {
                        userSessInfo.setDestinationProps(desProps);
                    }

                    // Seek the Account for External
                    String accountID = srvCloudApiSrv.getAccountIdByUserEmail(usAccConEmpl.getUserEmail(),
                            userSessInfo.getDestinationProps());
                    if (StringUtils.hasText(accountID))
                    {
                        // If Account Found - Set It
                        usAccConEmpl.setAccountId(accountID);
                    }
                }
                else // For Internal Users
                {
                    TY_DestinationProps desProps = destSrv.getDestinationDetails4User(dS.getDestInternal());
                    if (desProps != null)
                    {
                        userSessInfo.setDestinationProps(desProps);
                    }
                    // Seek an Employee
                    String empID = srvCloudApiSrv.getEmployeeIdByUserId(usAccConEmpl.getUserId(),
                            userSessInfo.getDestinationProps());
                    if (StringUtils.hasText(empID))
                    {
                        usAccConEmpl.setEmployee(true);
                        usAccConEmpl.setEmployeeId(empID);
                    }
                }

            }

            userSessInfo.setUserDetails(userDetails); // Set in Session
            userSessInfo.getUserDetails().setUsAccEmpl(usAccConEmpl); // Set in Session

            if (userSessInfo.getUserDetails().getUsAccEmpl() != null)
            {
                try
                {
                    // Create Customer for New User
                    if ((!StringUtils.hasText(usAccConEmpl.getAccountId()))
                            && (!StringUtils.hasText(usAccConEmpl.getEmployeeId())))
                    {
                        // Go For Individual Customer Creation with the User Details
                        String newAccountID = this.createAccount();
                        if (StringUtils.hasText(newAccountID))
                        {
                            userSessInfo.getUserDetails().getUsAccEmpl().setAccountId(newAccountID);
                        }
                    }

                    // Get the cases for User
                    // Clear from Buffer
                    if (CollectionUtils.isNotEmpty(this.getCases4User4mSession()))
                    {
                        userSessInfo.getCases().clear();
                    }

                    // Fetch Afresh and Reset
                    userSessInfo.setCases(
                            essSrv.getCases4User(userSessInfo.getUserDetails().getUsAccEmpl(), EnumCaseTypes.Learning));
                    if (CollectionUtils.isNotEmpty(userSessInfo.getSubmissionIDs()))
                    {
                        // Seek Case IDs for Submissions
                        updateCases4SubmissionIds();
                    }

                }
                catch (Exception e)
                {
                    // Log error
                    log.error(msgSrc.getMessage("ERR_CASES_USER", new Object[]
                    { userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), e.getLocalizedMessage() },
                            Locale.ENGLISH));

                    // Raise Exception to be handled at UI via Central Aspect
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASES_USER", new Object[]
                    { userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), e.getLocalizedMessage() },
                            Locale.ENGLISH));
                }

            }
        }
        else

        {
            // Get the cases for User
            // Clear from Buffer
            if (CollectionUtils.isNotEmpty(this.getCases4User4mSession()))
            {
                userSessInfo.getCases().clear();
            }

            // Fetch Afresh and Reset
            try
            {
                userSessInfo.setCases(
                        essSrv.getCases4User(userSessInfo.getUserDetails().getUsAccEmpl(), EnumCaseTypes.Learning));
                if (CollectionUtils.isNotEmpty(userSessInfo.getSubmissionIDs()))
                {
                    // Seek Case IDs for Submissions
                    updateCases4SubmissionIds();
                }
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    // #Test
    @Override
    public TY_UserSessionInfo getSessionInfo4Test()
    {
        return this.userSessInfo;
    }

    @Override
    public TY_CaseFormAsync getCurrentForm4Submission()
    {
        TY_CaseFormAsync currCaseForm = null;
        if (userSessInfo != null)
        {
            currCaseForm = userSessInfo.getCurrentForm4Submission();
        }

        return currCaseForm;
    }

    @Override
    public void clearFormErrors()
    {
        this.userSessInfo.getFormErrorMsgs().clear();
    }

    @Override
    public List<String> getFormErrors()
    {
        return this.userSessInfo.getFormErrorMsgs();
    }

    @Override
    public void addFormErrors(String errorMsg)
    {
        if (StringUtils.hasText(errorMsg))
        {
            if (userSessInfo.getFormErrorMsgs() != null)
            {
                userSessInfo.getFormErrorMsgs().add(errorMsg);
            }
            else
            {
                userSessInfo.setFormErrorMsgs(new ArrayList<String>());
                userSessInfo.getFormErrorMsgs().add(errorMsg);
            }
        }
    }

    @Override
    public void clearPreviousSubmission4mSessionBuffer()
    {
        // clear Previous Run Form Error Messages
        if (CollectionUtils.isNotEmpty(userSessInfo.getFormErrorMsgs()))
        {
            this.clearFormErrors();
        }

        // Clear previous run attachment error(s) from Messages Stack
        if (CollectionUtils.isNotEmpty(userSessInfo.getMessagesStack()))
        {
            Optional<TY_Message> attErrO = userSessInfo.getMessagesStack().stream()
                    .filter(e -> e.getMsgType().equals(EnumMessageType.ERR_ATTACHMENT)).findFirst();
            if (attErrO.isPresent())
            {
                userSessInfo.getMessagesStack().remove(attErrO.get());

            }
        }
    }

    @Override
    public List<TY_CaseESS> getCases4User4mSession()
    {

        if (userSessInfo == null)
        {
            throw new EX_SessionExpired("User Session expired due to Inactivity. Please Sign In to Proceed!");
        }

        if (CollectionUtils.isNotEmpty(userSessInfo.getCases()))
        {
            log.info("# Cases returned for User From Session : " + userSessInfo.getCases().size());
            return userSessInfo.getCases();
        }

        return null;
    }

    @Override
    public void updateCases4SubmissionIds() throws EX_ESMAPI
    {
        if (userSessInfo == null)
        {
            throw new EX_SessionExpired("User Session expired due to Inactivity. Please Sign In to Proceed!");
        }

        if (hanaLogSrv != null && CollectionUtils.isNotEmpty(userSessInfo.getSubmissionIDs()))
        {
            // Get the Logs for the Object IDS - Sumbission GUIDS
            List<Esmappmsglog> logs = hanaLogSrv.getLogsByObjectIDs(userSessInfo.getSubmissionIDs());
            if (CollectionUtils.isNotEmpty(logs))
            {
                log.info("# Of Log entries for Submission(s) in Current Session - " + logs.size());
                // Get Logs Excluding Successful Submission
                List<Esmappmsglog> logsExclSubm = logs.stream()
                        .filter(l -> !(l.getMsgtype().equalsIgnoreCase(EnumMessageType.SUCC_CASE_SUBM.toString())
                                || l.getMsgtype().equalsIgnoreCase(EnumMessageType.SUCC_CASE_REPL_SUBM.toString())
                                || l.getMsgtype().equalsIgnoreCase(EnumMessageType.SUCC_CASE_CONFIRM_SAVE.toString())

                        )).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(logsExclSubm))
                {
                    for (Esmappmsglog esmappmsglog : logsExclSubm)
                    {
                        log.info("# Of Log Entries after Filtering Case Submissions  - " + logsExclSubm.size());
                        // further Check due to HANA String Comparison Issue - Not picked up in Filter

                        // Append Messages to Session

                        this.addSessionMessage(esmappmsglog.getMessage());
                        // # Performance - REmove the Submission Guids that are reconcilled for
                        // respective Case IDs
                        userSessInfo.getSubmissionIDs().remove(esmappmsglog.getObjectid());

                    }
                }

            }

        }
    }

    @Override
    public TY_CaseEdit_Form getCaseDetails4Edit(String caseID) throws EX_ESMAPI
    {
        TY_CaseEdit_Form caseEditForm = null;
        if (StringUtils.hasText(caseID))
        {
            // Check if REquested Case belongs to the User
            if (CollectionUtils.isNotEmpty(getCases4User4mSession()))
            {
                Optional<TY_CaseESS> caseESSO = getCases4User4mSession().stream()
                        .filter(c -> c.getGuid().equals(caseID)).findFirst();
                if (caseESSO.isPresent())
                {
                    TY_CaseESS caseESS = caseESSO.get();

                    // If The Channel is SELF_SERVICE, then only seek Notes and Use Other Case
                    // details from Session Var
                    // if (caseESS.getOrigin().equals(GC_Constants.gc_SelfServiceChannel))
                    // {
                    try
                    {
                        log.info("Getting Case details from Service Cloud...");
                        TY_CaseDetails caseDetails = srvCloudApiSrv.getCaseDetails4Case(caseESS.getGuid(),
                                userSessInfo.getDestinationProps());
                        // --- Filter by External Note Type(s) only
                        if (caseDetails != null)
                        {
                            caseEditForm = new TY_CaseEdit_Form();
                            caseDetails.setCaseType(caseESS.getCaseType());
                            caseDetails.setCaseId(caseESS.getId());
                            caseDetails.setStatus(caseESS.getStatusDesc());
                            caseDetails.setDescription(caseESS.getSubject());
                            caseDetails.setOrigin(caseESS.getOrigin());

                            // Get External Note & Default Note Type(s) for Current Case Type
                            if (CollectionUtils.isNotEmpty(catgCusSrv.getCustomizations()))
                            {
                                // Get Customization for Current Case Type
                                Optional<TY_CatgCusItem> cusIO = catgCusSrv.getCustomizations().stream()
                                        .filter(c -> c.getCaseType().equals(caseDetails.getCaseType())).findFirst();
                                if (cusIO.isPresent())
                                {
                                    // Check if Note Type configured for External Note(s)
                                    if (StringUtils.hasText(cusIO.get().getAppNoteTypes()))
                                    {
                                        List<String> appNoteTypes = Arrays
                                                .asList(cusIO.get().getAppNoteTypes().split("\\|"));
                                        if (CollectionUtils.isNotEmpty(appNoteTypes))
                                        {
                                            // Get all Note(s) Formatted

                                            // Getting Formatted External Notes Here

                                            List<TY_NotesDetails> fmExtNotes = srvCloudApiSrv.getFormattedNotes4Case(
                                                    caseDetails.getCaseGuid(), userSessInfo.getDestinationProps());
                                            if (CollectionUtils.isNotEmpty(fmExtNotes))
                                            {
                                                caseDetails.getNotes().addAll(fmExtNotes);
                                            }

                                            // Filter by External Note Type(s) only
                                            List<TY_NotesDetails> notesExternal = caseDetails.getNotes().stream()
                                                    .filter(n -> appNoteTypes.contains(n.getNoteType()))
                                                    .collect(Collectors.toList());

                                            if (CollectionUtils.isNotEmpty(notesExternal))
                                            {

                                                Collections.sort(notesExternal,
                                                        Comparator.comparing(TY_NotesDetails::getTimestamp).reversed());
                                                caseDetails.setNotes(notesExternal);
                                            }

                                        }
                                    }
                                }
                            }

                            if (attFetchSrv != null && rlConfig.isAllowPrevAttDL())
                            {
                                List<TY_PreviousAttachments> prevAtt = attFetchSrv
                                        .getAttachments4CaseByCaseGuid(caseID);
                                if (CollectionUtils.isNotEmpty(prevAtt))
                                {
                                    caseDetails.setPrevAttachments(prevAtt);
                                }
                            }
                            caseEditForm.setCaseDetails(caseDetails);

                            caseEditForm.getCaseDetails().setStatusTransitionCFG(
                                    statusSrv.getPortalStatusTransition4CaseTypeandCaseStatus(caseDetails.getCaseType(),
                                            caseDetails.getStatus()));

                        }
                    }
                    catch (Exception e)
                    {
                        handleErrorCaseFetch(caseID, e);
                    }

                    // }

                    // Else Only Use Case Details from Sess Var and prepare the Case Edit Form Model
                }
                else // Unauthorized Access - Case Is not in User's List
                {
                    handleUnauthorizedCaseAccess(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), caseID);
                }
            }

            else
            {
                log.error("Session Expired : No Cases bound for User in Session");
            }

        }

        return caseEditForm;
    }

    @Override
    public boolean SubmitCaseReply(TY_CaseEdit_Form caseReplyForm) throws EX_ESMAPI, IOException
    {
        boolean isSubmitted = false;

        if (caseReplyForm != null)
        {

            // Clear Buffer for Previous Form Submission
            clearPreviousSubmission4mSessionBuffer();
            // Push Form data to Session
            TY_CaseEditFormAsync caseReplyAsync = new TY_CaseEditFormAsync();

            // Format the text input for New Lines
            String formattedReply = caseReplyForm.getReply().replaceAll("\r\n", "<br/>");
            if (StringUtils.hasText(formattedReply))
            {
                caseReplyForm.setReply(formattedReply);
            }
            caseReplyAsync.setCaseReply(caseReplyForm);
            caseReplyAsync.setSubmGuid(UUID.randomUUID().toString());
            // Latest time Stamp from Form Submissions
            caseReplyAsync.setTimestamp(Timestamp.from(Instant.now()));
            caseReplyAsync.setUserId(userSessInfo.getUserDetails().getUsAccEmpl().getUserId());
            userSessInfo.setCurrentCaseReply(caseReplyAsync);

            if (isCaseReplyValid())
            {
                caseReplyAsync.setValid(true);
                // Attchments to be handled Here in Session service Only once the Form is
                // Validated Successfully
                try
                {
                    if (attSrv != null)
                    {
                        // Attachments Bound
                        if (CollectionUtils.isNotEmpty(attSrv.getAttachments()))
                        {
                            int i = 0;
                            for (TY_SessionAttachment attachment : attSrv.getAttachments())
                            {
                                // Create Attachment
                                TY_Attachment newAttachment = new TY_Attachment(
                                        userSessInfo.getUserDetails().getUsAccEmpl().isExternal(),
                                        FilenameUtils.getName(attachment.getName()),
                                        GC_Constants.gc_Attachment_Category, false);
                                caseReplyAsync.getAttRespList().add(srvCloudApiSrv.createAttachment(newAttachment,
                                        userSessInfo.getDestinationProps()));
                                if (caseReplyAsync.getAttRespList().get(i) != null)
                                {
                                    if (srvCloudApiSrv.persistAttachment(
                                            caseReplyAsync.getAttRespList().get(i).getUploadUrl(), attachment.getName(),
                                            attachment.getBlob(), userSessInfo.getDestinationProps()))
                                    {
                                        log.info(
                                                "Attachment with id : " + caseReplyAsync.getAttRespList().get(i).getId()
                                                        + " Persisted in Document Container.. for Submission ID: "
                                                        + caseReplyAsync.getSubmGuid());
                                    }
                                }

                                i++;
                            }
                        }
                    }

                }
                catch (EX_ESMAPI | IOException e)
                {

                    if (e instanceof IOException)
                    {
                        handleNoFileDataAttachment(caseReplyAsync);
                    }
                    else if (e instanceof EX_ESMAPI)
                    {
                        handleAttachmentPersistError(caseReplyAsync, e);
                    }

                    isSubmitted = false;
                }

                userSessInfo.getCurrentCaseReply().setValid(true);

                userSessInfo.getCurrentCaseReply().getCaseReply().getCaseDetails()
                        .setStatusTransitionCFG(statusSrv.getPortalStatusTransition4CaseTypeandCaseStatus(
                                userSessInfo.getCurrentCaseReply().getCaseReply().getCaseDetails().getCaseType(),
                                userSessInfo.getCurrentCaseReply().getCaseReply().getCaseDetails().getStatus()));

                // SUCC_CASE_REPL_SUBM=Reply for Case with id - {0} of Type - {1} submitted
                // Successfully with Submission id - {2} for User - {3}!
                String msg = msgSrc.getMessage("SUCC_CASE_REPL_SUBM", new Object[]
                { caseReplyAsync.getCaseReply().getCaseDetails().getCaseId(),
                        caseReplyAsync.getCaseReply().getCaseDetails().getCaseType(), caseReplyAsync.getSubmGuid(),
                        caseReplyAsync.getUserId() }, Locale.ENGLISH);
                log.info(msg); // System Log

                // Logging Framework
                TY_Message logMsg = new TY_Message(caseReplyAsync.getUserId(), caseReplyAsync.getTimestamp(),
                        EnumStatus.Success, EnumMessageType.SUCC_CASE_REPL_SUBM, caseReplyAsync.getSubmGuid(), msg);
                userSessInfo.getMessagesStack().add(logMsg);
                // Instantiate and Fire the Event : Syncronous processing
                EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
                applicationEventPublisher.publishEvent(logMsgEvent);

                // Add to Display Messages : to be shown to User or Successful Submission
                this.addSessionMessage(msg);

                // Add Submission Guids to Session Context for REconcillation after Case
                // Creation Process
                this.userSessInfo.getSubmissionIDs().add(caseReplyAsync.getSubmGuid());

                isSubmitted = true;
            }

        }

        return isSubmitted;
    }

    @Override
    public boolean isCaseReplyValid()
    {
        boolean isValid = true;

        // Get the Latest Form Submission from Session and Validate
        if (userSessInfo.getCurrentCaseReply() != null && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
        {
            if (userSessInfo.getCurrentCaseReply().getCaseReply() != null)
            {
                // REply Text is Mandatory
                if (StringUtils.hasText(userSessInfo.getCurrentCaseReply().getCaseReply().getReply()))
                {
                    isValid = true;
                }
                else
                {
                    isValid = false;
                    handleCaseReplyBlankError();
                }

            }
            else
            {

                // Payload error

                handleCaseReplyError();

                isValid = false;

            }

        }
        return isValid;
    }

    @Override
    public TY_CaseEditFormAsync getCurrentReplyForm4Submission()
    {
        TY_CaseEditFormAsync currCaseReplyForm = null;
        if (userSessInfo != null)
        {
            currCaseReplyForm = userSessInfo.getCurrentCaseReply();
        }

        return currCaseReplyForm;
    }

    @Override
    public void setCaseFormB4Submission(TY_Case_Form caseForm)
    {
        this.userSessInfo.setCaseFormB4Subm(caseForm);
    }

    @Override
    public TY_Case_Form getCaseFormB4Submission()
    {
        return userSessInfo.getCaseFormB4Subm();
    }

    @Override
    public void setCaseEditFormB4Submission(TY_CaseEdit_Form caseEditForm)
    {
        this.userSessInfo.setCaseReplyFormB4Subm(caseEditForm);
    }

    @Override
    public TY_CaseEdit_Form getCaseEditFormB4Submission()
    {
        return userSessInfo.getCaseReplyFormB4Subm();
    }

    @Override
    public TY_DestinationProps getDestinationDetails4mUserSession()
    {
        return userSessInfo.getDestinationProps();
    }

    @Override
    public void setSubmissionActive()
    {
        if (userSessInfo != null)
        {
            this.userSessInfo.setActiveSubmission(true);
        }

    }

    @Override
    public void clearActiveSubmission()
    {
        if (userSessInfo != null)
        {
            this.userSessInfo.setActiveSubmission(false);
        }
    }

    @Override
    public boolean isCurrentSubmissionActive()
    {
        boolean isActive = false;
        if (userSessInfo != null)
        {
            isActive = this.userSessInfo.isActiveSubmission();
        }

        return isActive;
    }

    @Override
    public String getSurveyUrl4CaseId(String caseId) throws EX_ESMAPI
    {
        String svyUrl = null;
        String cons_pattn = "\\~";
        final String prop_URL = "URL";

        if (StringUtils.hasText(caseId) && StringUtils.hasText(dS.getDestQualtrics()))
        {

            // Validate that the case belongs to User Only
            if (CollectionUtils.isNotEmpty(getCases4User4mSession()))
            {
                Optional<TY_CaseESS> caseESSO = getCases4User4mSession().stream().filter(c -> c.getId().equals(caseId))
                        .findFirst();
                if (!caseESSO.isPresent())
                {
                    handleUnauthorizedCaseAccess(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), caseId);
                }
                else
                {
                    // check if the Case has been already confirmed by the User in Current Session
                    if (this.isCaseAlreadyConfirmed(caseId))
                    {
                        String msg = msgSrc.getMessage("ERR_CASE_ALREADY_CONFIRMED", new Object[]
                        { caseId }, Locale.ENGLISH);
                        throw new EX_CaseAlreadyConfirmed(msg);
                    }

                    else
                    {
                        // Add Current CasId to Confirmed cases List already
                        this.addCaseToSessionConfirmedCases(caseId);

                        // First check for base Url in Session if loaded
                        if (StringUtils.hasText(userSessInfo.getQualtricsUrl()))
                        {
                            log.info("Survey base Url loaded from session.. ");
                            svyUrl = userSessInfo.getQualtricsUrl();
                            svyUrl = svyUrl.replaceAll(cons_pattn, caseId);
                        }
                        else // Fetch BaseUrl from Destination accessor
                        {

                            try
                            {

                                log.info("Scanning for Destination : " + dS.getDestQualtrics());
                                Destination dest = DestinationAccessor.getDestination(dS.getDestQualtrics());
                                if (dest != null)
                                {

                                    log.info("Qualtrics Destination Bound via Destination Accessor.");

                                    for (String prop : dest.getPropertyNames())
                                    {

                                        if (prop.equals(prop_URL))
                                        {
                                            svyUrl = dest.get(prop).get().toString();
                                            userSessInfo.setQualtricsUrl(svyUrl); // Load in Session Memory for later
                                                                                  // Use
                                            svyUrl = svyUrl.replaceAll(cons_pattn, caseId);
                                        }

                                    }

                                }
                            }
                            catch (DestinationAccessException e)
                            {
                                log.error("Error Accessing Destination : " + e.getLocalizedMessage());
                                String msg = msgSrc.getMessage("ERR_DESTINATION_ACCESS", new Object[]
                                { dS.getDestQualtrics(), e.getLocalizedMessage() }, Locale.ENGLISH);
                                throw new EX_ESMAPI(msg);

                            }

                        }
                    }

                }

            }

        }
        return svyUrl;
    }

    @Override
    public void addCaseToSessionConfirmedCases(String caseId)
    {
        if (StringUtils.hasText(caseId))
        {
            if (this.userSessInfo.getCnfCasesSess() != null)
            {
                this.userSessInfo.getCnfCasesSess().add(caseId);
            }
        }
    }

    @Override
    public boolean isCaseAlreadyConfirmed(String caseId)
    {
        boolean isCaseConfirmed = false;
        if (CollectionUtils.isNotEmpty(this.userSessInfo.getCnfCasesSess()))
        {
            Optional<String> caseFoundO = this.userSessInfo.getCnfCasesSess().stream().filter(c -> c.equals(caseId))
                    .findFirst();
            if (caseFoundO.isPresent())
            {
                isCaseConfirmed = true;
            }
        }

        return isCaseConfirmed;
    }

    @Override
    public TY_CaseConfirmPOJO getCaseDetails4Confirmation(String caseID) throws EX_ESMAPI
    {
        TY_CaseConfirmPOJO caseDetails = null;
        if (StringUtils.hasText(caseID))
        {
            // Check if REquested Case belongs to the User
            if (CollectionUtils.isNotEmpty(getCases4User4mSession()) && statusSrv != null)
            {
                Optional<TY_CaseESS> caseESSO = getCases4User4mSession().stream().filter(c -> c.getId().equals(caseID))
                        .findFirst();
                if (caseESSO.isPresent())
                {
                    TY_CaseESS caseESS = caseESSO.get();

                    try
                    {
                        log.info("Getting Case details from Service Cloud...");
                        TY_CaseDetails caseDetails4mAPI = srvCloudApiSrv.getCaseDetails4Case(caseESS.getGuid(),
                                userSessInfo.getDestinationProps());
                        // --- Filter by External Note Type(s) only
                        if (caseDetails4mAPI != null)
                        {
                            caseDetails = new TY_CaseConfirmPOJO();
                            caseDetails.setCaseGuid(caseESS.getGuid());
                            caseDetails.setSubmGuid(UUID.randomUUID().toString());
                            caseDetails.setCaseType(caseESS.getCaseType());
                            caseDetails.setCaseId(caseESS.getId());
                            caseDetails.setStatus(caseESS.getStatusDesc());
                            caseDetails.setOrigin(caseESS.getOrigin());
                            caseDetails.setETag(caseDetails4mAPI.getETag());
                            caseDetails.setUserId(userSessInfo.getUserDetails().getUsAccEmpl().getUserId());
                            caseDetails
                                    .setCnfStatusCode(statusSrv.getConfirmedStatusCode4CaseType(caseESS.getCaseType()));
                            caseDetails.setDesProps(userSessInfo.getDestinationProps());

                            String msg = msgSrc.getMessage("SUCC_CASE_CONFIRM_SUBM", new Object[]
                            { caseESS.getId(), caseESS.getCaseType(), caseDetails.getSubmGuid(),
                                    caseDetails.getUserId() }, Locale.ENGLISH);
                            log.info(msg); // System Log

                            // Add to Display Messages : to be shown to User or Successful Submission
                            this.addSessionMessage(msg);

                            // Add Submission Guids to Session Context for REconcillation after Case
                            // Creation Process
                            this.userSessInfo.getSubmissionIDs().add(caseDetails.getSubmGuid());
                        }

                    }
                    catch (Exception e)
                    {
                        handleErrorCaseFetch(caseID, e);
                    }

                }
            }
        }

        return caseDetails;
    }

    @Override
    public void setPreviousCategory(String catg)
    {
        this.userSessInfo.setPrevCatg(catg);
    }

    @Override
    public String getPreviousCategory()
    {
        return this.userSessInfo.getPrevCatg();
    }

    private void handleCaseReplyError()
    {
        String msg = msgSrc.getMessage("ERR_CASE_PAYLOAD", new Object[]
        { userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Timestamp.from(Instant.now())) }, Locale.ENGLISH);
        log.error(msg);

        TY_Message message = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_PAYLOAD,
                userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), msg);

        // For Logging Framework
        userSessInfo.getMessagesStack().add(message);
        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage(this, message);
        applicationEventPublisher.publishEvent(logMsgEvent);

        this.addFormErrors(msg);// For Form Display
    }

    private void handleCaseReplyBlankError()
    {
        String msg = msgSrc.getMessage("ERR_BLANK_CASE_REPLY", null, Locale.ENGLISH);
        log.error(msg);

        TY_Message message = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_PAYLOAD,
                userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), msg);

        // For Logging Framework
        userSessInfo.getMessagesStack().add(message);
        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage(this, message);
        applicationEventPublisher.publishEvent(logMsgEvent);

        this.addFormErrors(msg);// For Form Display
    }

    private void handleErrorCaseFetch(String caseID, Exception e)
    {
        String msg;
        msg = msgSrc.getMessage("EXC_CASE_GET", new Object[]
        { caseID, e.getLocalizedMessage() }, Locale.ENGLISH);

        log.error(msg);
        TY_Message logMsg = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_CASE_DET_FETCH, caseID, msg);
        this.addMessagetoStack(logMsg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) caseID, logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);

        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }

    private void handleUnauthorizedCaseAccess(String userId, String caseID)
    {
        String msg;
        msg = msgSrc.getMessage("ERR_UNAUTH_CASE_ACCESS", new Object[]
        { userId, caseID }, Locale.ENGLISH);

        log.error(msg);
        TY_Message logMsg = new TY_Message(userId, Timestamp.from(Instant.now()), EnumStatus.Error,
                EnumMessageType.ERR_UNAUTH_CASE_ACCESS, caseID, msg);
        this.addMessagetoStack(logMsg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) caseID, logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);

        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }

    private void handleMandatoryFieldMissingError(String fldName)
    {
        String msg = msgSrc.getMessage("ERR_MAND_FLDS", new Object[]
        { fldName }, Locale.ENGLISH);
        log.error(msg); // System Log

        // Logging Framework
        TY_Message logMsg = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_PAYLOAD,
                userSessInfo.getUserDetails().getUsAccEmpl().getUserId(), msg);
        userSessInfo.getMessagesStack().add(logMsg);

        // Instantiate and Fire the Event : Syncronous processing
        EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);

        this.addFormErrors(msg);// For Form Display
    }

    private void handleAttachmentPersistError(TY_CaseFormAsync caseFormAsync, Exception e)
    {
        String msg;
        msg = msgSrc.getMessage("ERROR_DOCS_PERSIST", new Object[]
        { caseFormAsync.getCaseForm().getAttachment().getOriginalFilename(), e.getLocalizedMessage() }, Locale.ENGLISH);

        log.error(msg);

        this.addFormErrors(msg);// For Form Display

        TY_Message logMsg = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_ATTACHMENT,
                caseFormAsync.getSubmGuid(), msg);
        this.addMessagetoStack(logMsg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) caseFormAsync.getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);
        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }

    private void handleNoFileDataAttachment(TY_CaseFormAsync caseFormAsync)
    {
        String msg;
        msg = msgSrc.getMessage("FILE_NO_DATA", new Object[]
        { caseFormAsync.getCaseForm().getAttachment().getOriginalFilename() }, Locale.ENGLISH);

        log.error(msg);
        TY_Message logMsg = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_ATTACHMENT,
                caseFormAsync.getSubmGuid(), msg);
        this.addMessagetoStack(logMsg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) caseFormAsync.getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);

        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }

    private void handleAttachmentPersistError(TY_CaseEditFormAsync caseFormAsync, Exception e)
    {
        String msg;
        msg = msgSrc.getMessage("ERROR_DOCS_PERSIST", new Object[]
        { caseFormAsync.getCaseReply().getAttachment().getOriginalFilename(), e.getLocalizedMessage() },
                Locale.ENGLISH);

        log.error(msg);

        this.addFormErrors(msg);// For Form Display

        TY_Message logMsg = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_ATTACHMENT,
                caseFormAsync.getSubmGuid(), msg);
        this.addMessagetoStack(logMsg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) caseFormAsync.getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);
        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }

    private void handleNoFileDataAttachment(TY_CaseEditFormAsync caseFormAsync)
    {
        String msg;
        msg = msgSrc.getMessage("FILE_NO_DATA", new Object[]
        { caseFormAsync.getCaseReply().getAttachment().getOriginalFilename() }, Locale.ENGLISH);

        log.error(msg);
        TY_Message logMsg = new TY_Message(userSessInfo.getUserDetails().getUsAccEmpl().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_ATTACHMENT,
                caseFormAsync.getSubmGuid(), msg);
        this.addMessagetoStack(logMsg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) caseFormAsync.getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);

        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }

    private void handleDestinationLoadError(String userName, String destName, String msg)
    {
        if (hanaLogSrv != null)
        {
            hanaLogSrv.createLog(new TY_Message(userName, Timestamp.from(Instant.now()), EnumStatus.Error,
                    EnumMessageType.ERR_SRVCLOUDAPI, destName, msg));

        }
        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }

}
