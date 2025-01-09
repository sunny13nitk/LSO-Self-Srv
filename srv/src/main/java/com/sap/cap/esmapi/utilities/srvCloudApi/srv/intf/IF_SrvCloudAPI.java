package com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cap.esmapi.catg.pojos.TY_CatalogItem;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.status.pojos.TY_StatusCfgItem;
import com.sap.cap.esmapi.ui.pojos.TY_Attachment;
import com.sap.cap.esmapi.ui.pojos.TY_CaseConfirmPOJO;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.pojos.TY_AttachmentResponse;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseCatalogCustomizing;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseDetails;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseESS;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseGuidId;
import com.sap.cap.esmapi.utilities.pojos.TY_CasePatchInfo;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_Customer_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_Employee_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_SrvCloud_Reply;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesDetails;
import com.sap.cap.esmapi.utilities.pojos.TY_PreviousAttachments;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;

/*
 * Impl with comments
 */
public interface IF_SrvCloudAPI
{
        public JsonNode getAllCases(TY_DestinationProps desProps) throws IOException;

        public List<TY_CaseESS> getCases4User(String accountIdUser, TY_DestinationProps desProps) throws IOException;

        public List<TY_CaseESS> getCases4User(Ty_UserAccountEmployee userDetails, TY_DestinationProps desProps)
                        throws IOException;

        public List<TY_CaseESS> getCases4User(Ty_UserAccountEmployee userDetails, EnumCaseTypes caseType,
                        TY_DestinationProps desProps) throws IOException;

        public List<TY_CaseGuidId> getCaseGuidIdList(TY_DestinationProps desProps);

        public Long getNumberofCases(TY_DestinationProps desProps) throws IOException;

        public JsonNode getAllAccounts(TY_DestinationProps desProps) throws IOException;

        public JsonNode getAllEmployees(TY_DestinationProps desProps) throws IOException;

        public JsonNode getAllContacts(TY_DestinationProps desProps) throws IOException;

        public String getAccountIdByUserEmail(String userEmail, TY_DestinationProps desProps) throws EX_ESMAPI;

        public String getEmployeeIdByUserId(String userId, TY_DestinationProps desProps) throws EX_ESMAPI;

        public String createCase(TY_Case_Customer_SrvCloud caseEntity, TY_DestinationProps desProps) throws EX_ESMAPI;

        public String createCase4Employee(TY_Case_Employee_SrvCloud caseEntity, TY_DestinationProps desProps)
                        throws EX_ESMAPI;

        public String createCase4Customer(TY_Case_Customer_SrvCloud caseEntity, TY_DestinationProps desProps)
                        throws EX_ESMAPI;

        public String getContactPersonIdByUserEmail(String userEmail, TY_DestinationProps desProps) throws EX_ESMAPI;

        public String createAccount(String userEmail, String userName, TY_DestinationProps desProps) throws EX_ESMAPI;

        public String createNotes(TY_NotesCreate notes, TY_DestinationProps desProps) throws EX_ESMAPI;

        public TY_AttachmentResponse createAttachment(TY_Attachment attachment, TY_DestinationProps desProps)
                        throws EX_ESMAPI;

        public boolean persistAttachment(String url, MultipartFile file, TY_DestinationProps desProps)
                        throws EX_ESMAPI, IOException;

        public boolean persistAttachment(String url, String fileName, byte[] blob, TY_DestinationProps desProps)
                        throws EX_ESMAPI, IOException;

        public TY_CaseCatalogCustomizing getActiveCaseTemplateConfig4CaseType(String caseType,
                        TY_DestinationProps desProps) throws EX_ESMAPI, IOException;

        public List<TY_CatalogItem> getActiveCaseCategoriesByCatalogId(String catalogID, TY_DestinationProps desProps)
                        throws EX_ESMAPI, IOException;

        public List<TY_KeyValue> getVHelpDDLB4Field(String fieldName, TY_DestinationProps desProps)
                        throws EX_ESMAPI, IOException;

        public TY_CaseDetails getCaseDetails4Case(String caseId, TY_DestinationProps desProps)
                        throws EX_ESMAPI, IOException;

        public List<TY_StatusCfgItem> getStatusCfg4StatusSchema(String StatusSchema, TY_DestinationProps desProps)
                        throws EX_ESMAPI, IOException;

        public boolean updateCasewithReply(TY_CasePatchInfo patchInfo, TY_Case_SrvCloud_Reply caseReply,
                        TY_DestinationProps desProps) throws EX_ESMAPI, IOException;

        public boolean confirmCase(TY_CaseConfirmPOJO caseDetails) throws EX_ESMAPI, IOException;

        public List<TY_PreviousAttachments> getAttachments4Case(String caseGuid, TY_DestinationProps desProps)
                        throws EX_ESMAPI, IOException;

        public List<TY_NotesDetails> getFormattedNotes4Case(String caseGuid, TY_DestinationProps desProps)
                        throws EX_ESMAPI, IOException;

        public ResponseEntity<List<String>> getAllowedAttachmentTypes(TY_DestinationProps desProps)
                        throws EX_ESMAPI, IOException;

}
