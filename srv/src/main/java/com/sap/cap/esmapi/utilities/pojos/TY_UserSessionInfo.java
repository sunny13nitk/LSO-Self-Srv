package com.sap.cap.esmapi.utilities.pojos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.cap.esmapi.ui.pojos.TY_CaseEditFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEdit_Form;
import com.sap.cap.esmapi.ui.pojos.TY_CaseFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_UserSessionInfo
{
    private Map<String, String> tokenDetails = new HashMap<>();
    private TY_UserDetails userDetails;
    private String csrfToken;
    private TY_FormSubmissions formSubmissions = new TY_FormSubmissions();
    private TY_CaseFormAsync currentForm4Submission;
    private List<String> messages; // Cases ESS List Messages only - New Cases Created
    private List<TY_CaseESS> cases; // Every Roundtrip Refresh - Event Case Form Submit/Status Change TBD
    private List<TY_Message> messagesStack = new ArrayList<TY_Message>(); // Session Messages Flow Stack
    private List<String> formErrorMsgs;
    private boolean rateLimitBreached;
    private List<String> allowedAttachmentTypes;
    private List<String> submissionIDs = new ArrayList<String>();
    private TY_CaseEditFormAsync currentCaseReply; // Case Reply in Session
    private TY_Case_Form caseFormB4Subm; // Temporary placeholder for CaseForm : toggle b/w POST/GET
    private TY_CaseEdit_Form caseReplyFormB4Subm; // Temporary placeholder for CaseEditForm : toggle b/w POST/GET
    private TY_DestinationProps destinationProps; // Destination Properties
    private boolean activeSubmission; // Handle successful Submission - UI toast message on Cases List
    private String qualtricsUrl; // Placeholder for Qualtrics Url
    private List<String> cnfCasesSess = new ArrayList<String>(); // Placeholder for Confirmed cases in session
    private String prevCatg; // Category selected Previous
}
