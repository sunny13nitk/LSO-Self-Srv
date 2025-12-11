package com.sap.cap.esmapi.ui.srv.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.events.event.EV_CaseFormSplSubmit;
import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_CaseFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.ui.srv.intf.IF_SplCatgCaseCreateSrv;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_Account_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_AttachmentResponse;
import com.sap.cap.esmapi.utilities.pojos.TY_Attachment_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_Customer_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_Employee_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_CatgLvl1_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Description_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Employee_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Extensions_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesCreate;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("SRV_CASE_CREATE_EXAM_REQUEST")
@RequiredArgsConstructor
@Slf4j
public class CL_SplCatgCaseCreateSrv implements IF_SplCatgCaseCreateSrv
{

    private final MessageSource msgSrc;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final IF_SrvCloudAPI srvCloudApiSrv;
    private final TY_CatgCus catgCusSrv;

    private final String gc_tag_appid = " Certification Title: ";
    private final String gc_tag_addtime = " Additonal time requested : ";
    private final String gc_comma = " ,";

    @Override
    public String createCase(EV_CaseFormSplSubmit ev_CaseFormSplSubmit) throws EX_ESMAPI
    {
        String caseId = null;
        if (ev_CaseFormSplSubmit.getPayload() != null)
        {
            if (validateCaseFormAsync(ev_CaseFormSplSubmit.getPayload()))
            {
                log.info("Creating Case for Special Category Exam Request with Submission Id : "
                        + ev_CaseFormSplSubmit.getPayload().getSubmGuid());

                TY_DestinationProps desProps = ev_CaseFormSplSubmit.getPayload().getDesProps();

                Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream().filter(
                        g -> g.getCaseType().equals(ev_CaseFormSplSubmit.getPayload().getCaseForm().getCaseTxnType()))
                        .findFirst();
                // Prepare the description
                String desc = prepareDescription(ev_CaseFormSplSubmit.getPayload().getCaseForm());

                ev_CaseFormSplSubmit.getPayload().getCaseForm().setDescription(desc);

                if (!ev_CaseFormSplSubmit.getPayload().getCaseForm().isEmployee())
                {
                    log.info("External User -- Ind. Customer Scenario");
                    caseId = createCase4IndCustomer(ev_CaseFormSplSubmit, desProps, cusItemO);
                }
                else // Case Create for an Employee
                {
                    log.info("Internal User -- Employee Scenario");
                    caseId = createCase4Employee(ev_CaseFormSplSubmit, desProps, cusItemO);
                }
            }
            else
            {
                handleInvalidCaseForm(ev_CaseFormSplSubmit.getPayload());
            }
        }

        return caseId;
    }

    private String prepareDescription(TY_Case_Form caseFormAsync)
    {
        String desc = " ";
        if (caseFormAsync != null)
        {
            desc = desc + gc_tag_appid + caseFormAsync.getAppId();
            desc = desc + gc_comma + gc_tag_addtime + (caseFormAsync.isExtraTime() ? "Yes" : "No");
        }
        return desc;
    }

    private void handleInvalidCaseForm(TY_CaseFormAsync caseFormAsync)
    {
        String msg;
        // ERR_CASE_POST_VAL= Error Creating Case via Case Service. Form Validation
        // Errors for Submission Id - {1} !
        msg = msgSrc.getMessage("ERR_CASE_POST_VAL", new Object[]
        { caseFormAsync.getSubmGuid() }, Locale.ENGLISH);

        log.error(msg);
        TY_Message logMsg = new TY_Message(caseFormAsync.getUserId(), Timestamp.from(Instant.now()), EnumStatus.Error,
                EnumMessageType.ERR_CASE_CREATE, caseFormAsync.getSubmGuid(), msg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) caseFormAsync.getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);

        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }

    private boolean validateCaseFormAsync(TY_CaseFormAsync caseFormAsync)
    {
        boolean isValid = false;
        if (caseFormAsync.getCaseForm() != null && caseFormAsync.getSubmGuid() != null)
        {
            if (StringUtils.hasText(caseFormAsync.getCaseForm().getCaseTxnType())
                    && StringUtils.hasText(caseFormAsync.getCaseForm().getCatgDesc())
                    && StringUtils.hasText(caseFormAsync.getCaseForm().getAppId())
                    && StringUtils.hasText(caseFormAsync.getCaseForm().getSubject()))
            {
                if (caseFormAsync.getCaseForm().isExtraTime())
                {
                    isValid = true;
                }
            }
        }

        return isValid;
    }

    private String createCase4Employee(EV_CaseFormSplSubmit evCaseFormSubmit, TY_DestinationProps desProps,
            Optional<TY_CatgCusItem> cusItemO)
    {
        log.info("Case creation for Employee....");
        String caseID = null;
        TY_Case_Employee_SrvCloud newCaseEntity4Employee;
        newCaseEntity4Employee = new TY_Case_Employee_SrvCloud();
        // Account must be present
        if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getAccId()))
        {
            newCaseEntity4Employee
                    .setEmployee(new TY_Employee_CaseCreate(evCaseFormSubmit.getPayload().getCaseForm().getAccId())); // Account
                                                                                                                      // ID

            // Case Txn. Type
            newCaseEntity4Employee.setCaseType(evCaseFormSubmit.getPayload().getCaseForm().getCaseTxnType());
            // Cae Subject
            newCaseEntity4Employee.setSubject(evCaseFormSubmit.getPayload().getCaseForm().getSubject());

            // Fetch CatgGuid by description from Customizing - Set
            // Categories
            if (evCaseFormSubmit.getPayload().getCatTreeSelCatg().length > 0)
            {
                String[] catTreeSelCatg = evCaseFormSubmit.getPayload().getCatTreeSelCatg();
                if (Arrays.stream(catTreeSelCatg).filter(e -> e != null).count() > 0)
                {
                    switch ((int) Arrays.stream(catTreeSelCatg).filter(e -> e != null).count())
                    {
                    case 4:
                        newCaseEntity4Employee.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[3]));
                        newCaseEntity4Employee.setCategoryLevel2(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                        newCaseEntity4Employee.setCategoryLevel3(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                        newCaseEntity4Employee.setCategoryLevel4(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                        break;
                    case 3:
                        newCaseEntity4Employee.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                        newCaseEntity4Employee.setCategoryLevel2(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                        newCaseEntity4Employee.setCategoryLevel3(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                        break;
                    case 2:
                        newCaseEntity4Employee.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                        newCaseEntity4Employee.setCategoryLevel2(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                        break;
                    case 1:
                        newCaseEntity4Employee.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                        break;
                    default:

                        handleCatgError(evCaseFormSubmit, cusItemO);
                        break;

                    }
                }
                else
                {

                    handleCatgError(evCaseFormSubmit, cusItemO);

                }

                // Create Notes if There is a description
                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getDescription()))
                {

                    // Create Note and Get Guid back
                    String noteId = srvCloudApiSrv
                            .createNotes(new TY_NotesCreate(evCaseFormSubmit.getPayload().getCaseForm().isExternal(),
                                    evCaseFormSubmit.getPayload().getCaseForm().getDescription(),
                                    GC_Constants.gc_NoteTypeDescription), desProps);
                    if (StringUtils.hasText(noteId))
                    {
                        newCaseEntity4Employee.setDescription(new TY_Description_CaseCreate(noteId));
                    }

                }

                // Set the Channel
                newCaseEntity4Employee.setOrigin(GC_Constants.gc_SelfServiceChannel);

                try
                {
                    caseID = srvCloudApiSrv.createCase4Employee(newCaseEntity4Employee, desProps);
                    if (StringUtils.hasText(caseID))
                    {
                        log.info("Case created with ID : " + caseID);
                    }
                }
                catch (Exception e)
                {

                    handleCaseCreationError(evCaseFormSubmit, e);

                }
            }

        }
        return caseID;
    }

    private String createCase4IndCustomer(EV_CaseFormSplSubmit evCaseFormSubmit, TY_DestinationProps desProps,
            Optional<TY_CatgCusItem> cusItemO)
    {
        String caseID = null;
        log.info("Case creation for Individual Customer....");
        TY_Case_Customer_SrvCloud newCaseEntity4Customer;
        newCaseEntity4Customer = new TY_Case_Customer_SrvCloud();
        // Account must be present
        if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getAccId()))
        {
            newCaseEntity4Customer
                    .setAccount(new TY_Account_CaseCreate(evCaseFormSubmit.getPayload().getCaseForm().getAccId())); // Account
                                                                                                                    // ID

            // Case Txn. Type
            newCaseEntity4Customer.setCaseType(evCaseFormSubmit.getPayload().getCaseForm().getCaseTxnType());
            // Cae Subject
            newCaseEntity4Customer.setSubject(evCaseFormSubmit.getPayload().getCaseForm().getSubject());

            // Fetch CatgGuid by description from Customizing - Set
            // Categories
            if (evCaseFormSubmit.getPayload().getCatTreeSelCatg().length > 0)
            {
                String[] catTreeSelCatg = evCaseFormSubmit.getPayload().getCatTreeSelCatg();
                if (Arrays.stream(catTreeSelCatg).filter(e -> e != null).count() > 0)
                {
                    switch ((int) Arrays.stream(catTreeSelCatg).filter(e -> e != null).count())
                    {
                    case 4:
                        newCaseEntity4Customer.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[3]));
                        newCaseEntity4Customer.setCategoryLevel2(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                        newCaseEntity4Customer.setCategoryLevel3(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                        newCaseEntity4Customer.setCategoryLevel4(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                        break;
                    case 3:
                        newCaseEntity4Customer.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                        newCaseEntity4Customer.setCategoryLevel2(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                        newCaseEntity4Customer.setCategoryLevel3(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                        break;
                    case 2:
                        newCaseEntity4Customer.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                        newCaseEntity4Customer.setCategoryLevel2(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                        break;
                    case 1:
                        newCaseEntity4Customer.setCategoryLevel1(new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                        break;
                    default:

                        handleCatgError(evCaseFormSubmit, cusItemO);
                        break;

                    }
                }
                else
                {

                    handleCatgError(evCaseFormSubmit, cusItemO);

                }

                // Create Notes if There is a description
                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getDescription()))
                {

                    // Create Note and Get Guid back
                    String noteId = srvCloudApiSrv
                            .createNotes(new TY_NotesCreate(evCaseFormSubmit.getPayload().getCaseForm().isExternal(),
                                    evCaseFormSubmit.getPayload().getCaseForm().getDescription(),
                                    GC_Constants.gc_NoteTypeDescription), desProps);
                    if (StringUtils.hasText(noteId))
                    {
                        newCaseEntity4Customer.setDescription(new TY_Description_CaseCreate(noteId));
                    }

                }

                // Check if Attachment needs to be Created
                if (CollectionUtils.isNotEmpty(evCaseFormSubmit.getPayload().getAttRespList()))
                {
                    // Prepare POJOdetails for
                    // TY_Case_SrvCloud
                    // newCaseEntity4Customer
                    List<TY_Attachment_CaseCreate> caseAttachmentsNew = new ArrayList<TY_Attachment_CaseCreate>();
                    for (TY_AttachmentResponse attR : evCaseFormSubmit.getPayload().getAttRespList())
                    {

                        if (StringUtils.hasText(attR.getId()) && StringUtils.hasText(attR.getUploadUrl()))
                        {
                            log.info("Attachment with id : " + attR.getId()
                                    + " already Persisted in Document Container..");

                            TY_Attachment_CaseCreate caseAttachment = new TY_Attachment_CaseCreate(attR.getId());
                            caseAttachmentsNew.add(caseAttachment);

                        }
                    }
                    newCaseEntity4Customer.setAttachments(caseAttachmentsNew);

                }

                // For Extensions
                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getCountry())
                        || StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getLanguage()))
                {
                    TY_Extensions_CaseCreate extn = new TY_Extensions_CaseCreate();
                    if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getCountry()))
                    {
                        extn.setLSO_Country(evCaseFormSubmit.getPayload().getCaseForm().getCountry());
                    }

                    if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getLanguage()))
                    {
                        extn.setLSO_Language(evCaseFormSubmit.getPayload().getCaseForm().getLanguage());
                    }

                    newCaseEntity4Customer.setExtensions(extn);

                }

                // Set the Channel
                newCaseEntity4Customer.setOrigin(GC_Constants.gc_SelfServiceChannel);

                // Set the External User Flag - Pick Right
                // Technical User in D/S API Call
                newCaseEntity4Customer.setExternal(evCaseFormSubmit.getPayload().getCaseForm().isExternal());

                try
                {
                    caseID = srvCloudApiSrv.createCase4Customer(newCaseEntity4Customer, desProps);
                    if (StringUtils.hasText(caseID))
                    {
                        log.info("Case created with ID : " + caseID);

                    }
                }
                catch (Exception e)
                {

                    handleCaseCreationError(evCaseFormSubmit, e);

                }
            }

        }
        return caseID;
    }

    private void handleCaseCreationError(EV_CaseFormSplSubmit evCaseFormSubmit, Exception e)
    {
        String msg;
        msg = msgSrc.getMessage("ERR_CASE_POST", new Object[]
        { e.getLocalizedMessage(), evCaseFormSubmit.getPayload().getSubmGuid() }, Locale.ENGLISH);

        log.error(msg);

        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }

    private void handleCatgError(EV_CaseFormSplSubmit evCaseFormSubmit, Optional<TY_CatgCusItem> cusItemO)
    {
        String msg = msgSrc.getMessage("ERR_INVALID_CATG", new Object[]
        { cusItemO.get().getCaseTypeEnum().toString(), evCaseFormSubmit.getPayload().getCaseForm().getCatgDesc() },
                Locale.ENGLISH);

        log.error(msg);
        TY_Message logMsg = new TY_Message(evCaseFormSubmit.getPayload().getUserId(), Timestamp.from(Instant.now()),
                EnumStatus.Error, EnumMessageType.ERR_CASE_CATG, evCaseFormSubmit.getPayload().getSubmGuid(), msg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) evCaseFormSubmit.getPayload().getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);
    }

}
