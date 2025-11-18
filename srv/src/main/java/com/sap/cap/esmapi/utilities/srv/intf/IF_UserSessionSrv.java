package com.sap.cap.esmapi.utilities.srv.intf;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_CaseConfirmPOJO;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEditFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEdit_Form;
import com.sap.cap.esmapi.ui.pojos.TY_CaseFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.ui.pojos.TY_SplCatg_Seek;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseESS;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_UserDetails;
import com.sap.cap.esmapi.utilities.pojos.TY_UserSessionInfo;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cloud.security.token.Token;

public interface IF_UserSessionSrv
{

    // For Test Purpose Only to TEst the Controller - #Test
    public void loadUser4Test();

    public void setPreviousCategory(String catg);

    public String getPreviousCategory();

    public TY_SplCatg_Seek isSplCatg(String caseFormCatgDesc);

    // For Test Purpose Only to Test the Controller - #Test
    public TY_UserSessionInfo getSessionInfo4Test();

    // Set Case Form in Session to Toggle b/w GET & POST w/o lossing form Data
    public void setCaseFormB4Submission(TY_Case_Form caseForm);

    // Get Case Form in Session to Toggle b/w GET & POST w/o lossing form Data
    public TY_Case_Form getCaseFormB4Submission();

    // Set Case Edit Form in Session to Toggle b/w GET & POST w/o lossing form Data
    public void setCaseEditFormB4Submission(TY_CaseEdit_Form caseEditForm);

    // Get Case Form in Session to Toggle b/w GET & POST w/o lossing form Data
    public TY_CaseEdit_Form getCaseEditFormB4Submission();

    public List<TY_CaseESS> getCases4User4mSession();

    /*
     * Get User Credentials via Token - Get and persist in Session if Not Bound -
     * Get only if Already bound for a session
     */
    public TY_UserDetails getUserDetails(@AuthenticationPrincipal Token token) throws EX_ESMAPI;

    /*
     * Get Complete User Session Info Details via Token - Get and Persist if not
     * bound - Get only if bound for session - if refresh true -- reload Cases and
     * Stats for Current User and refurbish in Session
     */
    public TY_UserSessionInfo getESSDetails(@AuthenticationPrincipal Token token, boolean refresh) throws EX_ESMAPI;

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
    // ------- session :currentForm4Submission to be picked up by Event Handler
    // @formatter:on

    public boolean SubmitCaseForm(TY_Case_Form caseForm);

    // @formatter:off -- Submit Case Reply Form
    // : Form Data Saved in session :currentCaseReply
    // --Validate Case Reply Form - Implicit Call
    // ---- Fail
    // ------- Message Logging Event
    //
    // ------- Message Stack in Session Populated and REturn false
    // ---- Succ
    // ------- Create and Publish Case Reply Submit Event
    // ------- session :currentCaseReply to be picked up by Event Handler
    // @formatter:on
    public boolean SubmitCaseReply(TY_CaseEdit_Form caseReplyForm) throws EX_ESMAPI, IOException;

    public boolean validateProcessRequestorEmail();

    public void clearFormErrors();

    public List<String> getFormErrors();

    public void addFormErrors(String errorMsg);

    public TY_CaseFormAsync getCurrentForm4Submission();

    public TY_CaseEditFormAsync getCurrentReplyForm4Submission();

    public String createAccount() throws EX_ESMAPI;

    public Ty_UserAccountEmployee getUserDetails4mSession();

    public void addSessionMessage(String msg);

    public void addMessagetoStack(TY_Message msg);

    public List<TY_Message> getMessageStack();

    public List<String> getSessionMessages();

    // Check, Increment and Clear Session Count for Rate Limit. Server RoundTrip for
    // Create Process
    public boolean isWithinRateLimit();

    // Only Check Session for Rate Limit and Clear If Time Elapsed. No Session
    // Increment - View Operations
    public boolean checkRateLimit();

    public boolean isCaseFormValid();

    public boolean isCaseReplyValid();

    /*
     * Just to get the Flag rateLimitBreached from Session
     */
    public boolean getCurrentRateLimitBreachedValue();

    /*
     * Clear Previos Form Error Messages and Message Stack for Form Attachment Error
     * if any for provisioning new Session Error Messages and hosting new
     * attachments
     */
    public void clearPreviousSubmission4mSessionBuffer();

    /*
     * Update Session Messages and Log(s) for a submission event result
     */
    public void updateCases4SubmissionIds() throws EX_ESMAPI;

    /*
     * Get Model for Case Edit for a particular Case GUID -- Includes Case
     * Navigation Authority Check if the Case really belongs to User in Current
     * Session
     */
    public TY_CaseEdit_Form getCaseDetails4Edit(String caseID) throws EX_ESMAPI;

    /**
     * Get Case Details for Case Confirmation
     * 
     * @param caseID - Case ID :The ID of the case --Includes check for the case
     *               belonging to the User
     * @return TY_CASEConfirmPOJO
     * @throws EX_ESMAPI
     */
    public TY_CaseConfirmPOJO getCaseDetails4Confirmation(String caseID) throws EX_ESMAPI;

    public TY_DestinationProps getDestinationDetails4mUserSession();

    public void setSubmissionActive();

    public void clearActiveSubmission();

    public boolean isCurrentSubmissionActive();

    // @formatter:off
    /**
     * Returns the Survey Url for the given Case Id
     * - Ensures that the Case belongs to the Current User only who is triggering invocation
     * - Utilizes session memory to get base Url 
     * -- Only goes to Destination Service in case the Survey Base url is not loaded in session memory
     * @param caseId
     * @return
     * @throws EX_ESMAPI
     */
     // @formatter:on
    public String getSurveyUrl4CaseId(String caseId) throws Exception;

    /**
     * Add the case Id to Session Confirmed Cases
     */
    public void addCaseToSessionConfirmedCases(String caseId);

    /**
     * Check that the case if it is already confirmed in the Current Session by the
     * User
     * 
     * @param caseId - Case to be chacked for Confirmation
     * @return - true in case the case is already confirmed in current Session
     */
    public boolean isCaseAlreadyConfirmed(String caseId);

}
