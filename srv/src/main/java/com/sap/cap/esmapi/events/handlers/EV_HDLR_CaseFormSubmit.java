package com.sap.cap.esmapi.events.handlers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;
import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_Account_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_AddUserCaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_AttachmentResponse;
import com.sap.cap.esmapi.utilities.pojos.TY_Attachment_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_CustomerAddUser_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_Customer_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_EmployeeAddUserIC_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_EmployeeAddUser_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_Employee_SrvCloud;
import com.sap.cap.esmapi.utilities.pojos.TY_CatgLvl1_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Description_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Employee_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Extensions_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesCreate;
import com.sap.cap.esmapi.utilities.scrambling.CL_ScramblingUtils;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EV_HDLR_CaseFormSubmit
{
        @Autowired
        private TY_CatgCus catgCusSrv;

        @Autowired
        private MessageSource msgSrc;

        @Autowired
        private ApplicationEventPublisher applicationEventPublisher;

        @Autowired
        private IF_SrvCloudAPI srvCloudApiSrv;

        @Async
        @EventListener
        public void handleCaseFormSubmission(EV_CaseFormSubmit evCaseFormSubmit)
        {

                if (evCaseFormSubmit != null && catgCusSrv != null
                                && evCaseFormSubmit.getPayload().getDesProps() != null)
                {
                        TY_DestinationProps desProps = evCaseFormSubmit.getPayload().getDesProps();
                        log.info("Inside Case Form Asyncronous Submit Event Processing---- for Case Submission ID: "
                                        + evCaseFormSubmit.getPayload().getSubmGuid());

                        if (evCaseFormSubmit.getPayload() != null
                                        && evCaseFormSubmit.getPayload().getCatTreeSelCatg() != null)
                        {
                                log.info("Case Payload Bound...");
                                if (evCaseFormSubmit.getPayload().isValid())
                                {
                                        log.info("Case Payload is found to be successfully validated .......");
                                        Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                                                        .filter(g -> g.getCaseType().equals(evCaseFormSubmit
                                                                        .getPayload().getCaseForm().getCaseTxnType()))
                                                        .findFirst();

                                        // Submission Id found on Case
                                        if (StringUtils.hasText(evCaseFormSubmit.getPayload().getSubmGuid())
                                                        && cusItemO.isPresent())
                                        {

                                                if (!evCaseFormSubmit.getPayload().getCaseForm().isEmployee())
                                                {
                                                        if (StringUtils.hasText(evCaseFormSubmit.getPayload()
                                                                        .getCaseForm().getReporter()))
                                                        {
                                                                log.info("External User -- Reporter as Ind. Customer Scenario");
                                                                // Only Individual Customer(s) as Reporters for External
                                                                // Scenario
                                                                createCase4IndCustomerAddUser(evCaseFormSubmit,
                                                                                desProps, cusItemO);
                                                        }
                                                        else
                                                        {
                                                                log.info("External User -- Ind. Customer Scenario");
                                                                createCase4IndCustomer(evCaseFormSubmit, desProps,
                                                                                cusItemO);
                                                        }

                                                }
                                                else // Case Create for an Employee
                                                {
                                                        if (StringUtils.hasText(evCaseFormSubmit.getPayload()
                                                                        .getCaseForm().getReporter()))
                                                        {
                                                                if (evCaseFormSubmit.getPayload().getCaseForm()
                                                                                .isReporterEmployee())
                                                                {
                                                                        log.info("Internal User -- Reporter as Employee Scenario");
                                                                        createCase4EmployeeAddUser(evCaseFormSubmit,
                                                                                        desProps, cusItemO);
                                                                }
                                                                else
                                                                {
                                                                        log.info("Internal User -- Reporter Exists Scenario");
                                                                        createCase4EmployeeAddUserIndCustomer(
                                                                                        evCaseFormSubmit, desProps,
                                                                                        cusItemO);
                                                                }

                                                        }
                                                        else
                                                        {
                                                                createCase4Employee(evCaseFormSubmit, desProps,
                                                                                cusItemO);
                                                        }

                                                }
                                        }

                                }
                        }
                }

        }

        private void createCase4EmployeeAddUserIndCustomer(EV_CaseFormSubmit evCaseFormSubmit,
                        TY_DestinationProps desProps, Optional<TY_CatgCusItem> cusItemO)
        {
                TY_Case_EmployeeAddUserIC_SrvCloud newCaseEntity4Employee;
                newCaseEntity4Employee = new TY_Case_EmployeeAddUserIC_SrvCloud();
                // Account must be present
                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getAccId()))
                {
                        newCaseEntity4Employee.setEmployee(new TY_Employee_CaseCreate(
                                        evCaseFormSubmit.getPayload().getCaseForm().getAccId())); // Account
                                                                                                  // ID

                        // Case Txn. Type
                        newCaseEntity4Employee
                                        .setCaseType(evCaseFormSubmit.getPayload().getCaseForm().getCaseTxnType());
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
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[3]));
                                                newCaseEntity4Employee.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Employee.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Employee.setCategoryLevel4(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 3:
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Employee.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Employee.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 2:
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Employee.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 1:
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
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

                                        // #JIRA - ESMLSO-516
                                        /*
                                         * Scramble Description for CC Information
                                         */
                                        String scrambledTxt = CL_ScramblingUtils.scrambleText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getDescription());
                                        if (StringUtils.hasText(scrambledTxt))
                                        {
                                                // Create Note and Get Guid back
                                                String noteId = srvCloudApiSrv.createNotes(new TY_NotesCreate(
                                                                evCaseFormSubmit.getPayload().getCaseForm()
                                                                                .isExternal(),
                                                                scrambledTxt, GC_Constants.gc_NoteTypeDescription),
                                                                desProps);
                                                if (StringUtils.hasText(noteId))
                                                {
                                                        newCaseEntity4Employee.setDescription(
                                                                        new TY_Description_CaseCreate(noteId));
                                                }
                                        }
                                }

                                // Check if Attachment needs to be Created
                                if (CollectionUtils.isNotEmpty(evCaseFormSubmit.getPayload().getAttRespList()))
                                {
                                        // Prepare POJOdetails for
                                        // TY_Case_SrvCloud
                                        // newCaseEntity4Employee
                                        List<TY_Attachment_CaseCreate> caseAttachmentsNew = new ArrayList<TY_Attachment_CaseCreate>();
                                        for (TY_AttachmentResponse attR : evCaseFormSubmit.getPayload()
                                                        .getAttRespList())
                                        {

                                                if (StringUtils.hasText(attR.getId())
                                                                && StringUtils.hasText(attR.getUploadUrl()))
                                                {
                                                        log.info("Attachment with id : " + attR.getId()
                                                                        + " already Persisted in Document Container..");

                                                        TY_Attachment_CaseCreate caseAttachment = new TY_Attachment_CaseCreate(
                                                                        attR.getId());
                                                        caseAttachmentsNew.add(caseAttachment);

                                                }
                                        }
                                        newCaseEntity4Employee.setAttachments(caseAttachmentsNew);

                                }

                                // For Extensions
                                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getCountry())
                                                || StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage()))
                                {
                                        TY_Extensions_CaseCreate extn = new TY_Extensions_CaseCreate();
                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getCountry()))
                                        {
                                                extn.setLSO_Country(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getCountry());
                                        }

                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getLanguage()))
                                        {
                                                extn.setLSO_Language(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage());
                                        }

                                        newCaseEntity4Employee.setExtensions(extn);

                                }

                                // Set the Channel
                                newCaseEntity4Employee.setOrigin(GC_Constants.gc_SelfServiceChannel);

                                // Set the Reporter PFCT
                                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getReporter()))
                                {
                                        TY_AddUserCaseCreate addUser = new TY_AddUserCaseCreate(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getReporter(),
                                                        GC_Constants.gc_PFCT_ADDUSER);
                                        newCaseEntity4Employee.getCustomIndividualCustomers().add(addUser);

                                }

                                try
                                {
                                        String caseID = srvCloudApiSrv
                                                        .createCase4EmployeeAddUserIC(newCaseEntity4Employee, desProps);
                                        if (StringUtils.hasText(caseID))
                                        {
                                                handleCaseSuccCreated(evCaseFormSubmit, cusItemO, caseID);

                                        }
                                }
                                catch (Exception e)
                                {

                                        handleCaseCreationError(evCaseFormSubmit, e);

                                }
                        }

                }
        }

        private void createCase4Employee(EV_CaseFormSubmit evCaseFormSubmit, TY_DestinationProps desProps,
                        Optional<TY_CatgCusItem> cusItemO)
        {
                TY_Case_Employee_SrvCloud newCaseEntity4Employee;
                newCaseEntity4Employee = new TY_Case_Employee_SrvCloud();
                // Account must be present
                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getAccId()))
                {
                        newCaseEntity4Employee.setEmployee(new TY_Employee_CaseCreate(
                                        evCaseFormSubmit.getPayload().getCaseForm().getAccId())); // Account
                                                                                                  // ID

                        // Case Txn. Type
                        newCaseEntity4Employee
                                        .setCaseType(evCaseFormSubmit.getPayload().getCaseForm().getCaseTxnType());
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
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[3]));
                                                newCaseEntity4Employee.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Employee.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Employee.setCategoryLevel4(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 3:
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Employee.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Employee.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 2:
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Employee.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 1:
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
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

                                        // #JIRA - ESMLSO-516
                                        /*
                                         * Scramble Description for CC Information
                                         */
                                        String scrambledTxt = CL_ScramblingUtils.scrambleText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getDescription());
                                        if (StringUtils.hasText(scrambledTxt))
                                        {
                                                // Create Note and Get Guid back
                                                String noteId = srvCloudApiSrv.createNotes(new TY_NotesCreate(
                                                                evCaseFormSubmit.getPayload().getCaseForm()
                                                                                .isExternal(),
                                                                scrambledTxt, GC_Constants.gc_NoteTypeDescription),
                                                                desProps);
                                                if (StringUtils.hasText(noteId))
                                                {
                                                        newCaseEntity4Employee.setDescription(
                                                                        new TY_Description_CaseCreate(noteId));
                                                }
                                        }
                                }

                                // Check if Attachment needs to be Created
                                if (CollectionUtils.isNotEmpty(evCaseFormSubmit.getPayload().getAttRespList()))
                                {
                                        // Prepare POJOdetails for
                                        // TY_Case_SrvCloud
                                        // newCaseEntity4Employee
                                        List<TY_Attachment_CaseCreate> caseAttachmentsNew = new ArrayList<TY_Attachment_CaseCreate>();
                                        for (TY_AttachmentResponse attR : evCaseFormSubmit.getPayload()
                                                        .getAttRespList())
                                        {

                                                if (StringUtils.hasText(attR.getId())
                                                                && StringUtils.hasText(attR.getUploadUrl()))
                                                {
                                                        log.info("Attachment with id : " + attR.getId()
                                                                        + " already Persisted in Document Container..");

                                                        TY_Attachment_CaseCreate caseAttachment = new TY_Attachment_CaseCreate(
                                                                        attR.getId());
                                                        caseAttachmentsNew.add(caseAttachment);

                                                }
                                        }
                                        newCaseEntity4Employee.setAttachments(caseAttachmentsNew);

                                }

                                // For Extensions
                                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getCountry())
                                                || StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage()))
                                {
                                        TY_Extensions_CaseCreate extn = new TY_Extensions_CaseCreate();
                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getCountry()))
                                        {
                                                extn.setLSO_Country(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getCountry());
                                        }

                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getLanguage()))
                                        {
                                                extn.setLSO_Language(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage());
                                        }

                                        newCaseEntity4Employee.setExtensions(extn);

                                }

                                // Set the Channel
                                newCaseEntity4Employee.setOrigin(GC_Constants.gc_SelfServiceChannel);

                                try
                                {
                                        String caseID = srvCloudApiSrv.createCase4Employee(newCaseEntity4Employee,
                                                        desProps);
                                        if (StringUtils.hasText(caseID))
                                        {
                                                handleCaseSuccCreated(evCaseFormSubmit, cusItemO, caseID);

                                        }
                                }
                                catch (Exception e)
                                {

                                        handleCaseCreationError(evCaseFormSubmit, e);

                                }
                        }

                }
        }

        private void createCase4EmployeeAddUser(EV_CaseFormSubmit evCaseFormSubmit, TY_DestinationProps desProps,
                        Optional<TY_CatgCusItem> cusItemO)
        {
                TY_Case_EmployeeAddUser_SrvCloud newCaseEntity4Employee = new TY_Case_EmployeeAddUser_SrvCloud();
                // Account must be present
                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getAccId()))
                {
                        newCaseEntity4Employee.setEmployee(new TY_Employee_CaseCreate(
                                        evCaseFormSubmit.getPayload().getCaseForm().getAccId())); // Account
                                                                                                  // ID

                        // Case Txn. Type
                        newCaseEntity4Employee
                                        .setCaseType(evCaseFormSubmit.getPayload().getCaseForm().getCaseTxnType());
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
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[3]));
                                                newCaseEntity4Employee.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Employee.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Employee.setCategoryLevel4(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 3:
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Employee.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Employee.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 2:
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Employee.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 1:
                                                newCaseEntity4Employee.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
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

                                        // #JIRA - ESMLSO-516
                                        /*
                                         * Scramble Description for CC Information
                                         */
                                        String scrambledTxt = CL_ScramblingUtils.scrambleText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getDescription());
                                        if (StringUtils.hasText(scrambledTxt))
                                        {
                                                // Create Note and Get Guid back
                                                String noteId = srvCloudApiSrv.createNotes(new TY_NotesCreate(
                                                                evCaseFormSubmit.getPayload().getCaseForm()
                                                                                .isExternal(),
                                                                scrambledTxt, GC_Constants.gc_NoteTypeDescription),
                                                                desProps);
                                                if (StringUtils.hasText(noteId))
                                                {
                                                        newCaseEntity4Employee.setDescription(
                                                                        new TY_Description_CaseCreate(noteId));
                                                }
                                        }
                                }

                                // Check if Attachment needs to be Created
                                if (CollectionUtils.isNotEmpty(evCaseFormSubmit.getPayload().getAttRespList()))
                                {
                                        // Prepare POJOdetails for
                                        // TY_Case_SrvCloud
                                        // newCaseEntity4Employee
                                        List<TY_Attachment_CaseCreate> caseAttachmentsNew = new ArrayList<TY_Attachment_CaseCreate>();
                                        for (TY_AttachmentResponse attR : evCaseFormSubmit.getPayload()
                                                        .getAttRespList())
                                        {

                                                if (StringUtils.hasText(attR.getId())
                                                                && StringUtils.hasText(attR.getUploadUrl()))
                                                {
                                                        log.info("Attachment with id : " + attR.getId()
                                                                        + " already Persisted in Document Container..");

                                                        TY_Attachment_CaseCreate caseAttachment = new TY_Attachment_CaseCreate(
                                                                        attR.getId());
                                                        caseAttachmentsNew.add(caseAttachment);

                                                }
                                        }
                                        newCaseEntity4Employee.setAttachments(caseAttachmentsNew);

                                }

                                // For Extensions
                                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getCountry())
                                                || StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage()))
                                {
                                        TY_Extensions_CaseCreate extn = new TY_Extensions_CaseCreate();
                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getCountry()))
                                        {
                                                extn.setLSO_Country(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getCountry());
                                        }

                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getLanguage()))
                                        {
                                                extn.setLSO_Language(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage());
                                        }

                                        newCaseEntity4Employee.setExtensions(extn);

                                }

                                // Set the Channel
                                newCaseEntity4Employee.setOrigin(GC_Constants.gc_SelfServiceChannel);
                                // Set the Reporter PFCT
                                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getReporter()))
                                {
                                        TY_AddUserCaseCreate addUser = new TY_AddUserCaseCreate(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getReporter(),
                                                        GC_Constants.gc_PFCT_ADDUSER);
                                        newCaseEntity4Employee.getCustomEmployees().add(addUser);

                                }

                                try
                                {
                                        String caseID = srvCloudApiSrv
                                                        .createCase4EmployeeAddUser(newCaseEntity4Employee, desProps);
                                        if (StringUtils.hasText(caseID))
                                        {
                                                handleCaseSuccCreated(evCaseFormSubmit, cusItemO, caseID);

                                        }
                                }
                                catch (Exception e)
                                {

                                        handleCaseCreationError(evCaseFormSubmit, e);

                                }
                        }

                }
        }

        private void createCase4IndCustomer(EV_CaseFormSubmit evCaseFormSubmit, TY_DestinationProps desProps,
                        Optional<TY_CatgCusItem> cusItemO)
        {
                TY_Case_Customer_SrvCloud newCaseEntity4Customer;
                newCaseEntity4Customer = new TY_Case_Customer_SrvCloud();
                // Account must be present
                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getAccId()))
                {
                        newCaseEntity4Customer.setAccount(new TY_Account_CaseCreate(
                                        evCaseFormSubmit.getPayload().getCaseForm().getAccId())); // Account
                                                                                                  // ID

                        // Case Txn. Type
                        newCaseEntity4Customer
                                        .setCaseType(evCaseFormSubmit.getPayload().getCaseForm().getCaseTxnType());
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
                                                newCaseEntity4Customer.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[3]));
                                                newCaseEntity4Customer.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Customer.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Customer.setCategoryLevel4(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 3:
                                                newCaseEntity4Customer.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Customer.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Customer.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 2:
                                                newCaseEntity4Customer.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Customer.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 1:
                                                newCaseEntity4Customer.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
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

                                        // #JIRA - ESMLSO-516
                                        /*
                                         * Scramble Description for CC Information
                                         */
                                        String scrambledTxt = CL_ScramblingUtils.scrambleText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getDescription());
                                        if (StringUtils.hasText(scrambledTxt))
                                        {

                                                // Create Note and Get Guid back
                                                String noteId = srvCloudApiSrv.createNotes(new TY_NotesCreate(
                                                                evCaseFormSubmit.getPayload().getCaseForm()
                                                                                .isExternal(),
                                                                scrambledTxt, GC_Constants.gc_NoteTypeDescription),
                                                                desProps);
                                                if (StringUtils.hasText(noteId))
                                                {
                                                        newCaseEntity4Customer.setDescription(
                                                                        new TY_Description_CaseCreate(noteId));
                                                }
                                        }
                                }

                                // Check if Attachment needs to be Created
                                if (CollectionUtils.isNotEmpty(evCaseFormSubmit.getPayload().getAttRespList()))
                                {
                                        // Prepare POJOdetails for
                                        // TY_Case_SrvCloud
                                        // newCaseEntity4Customer
                                        List<TY_Attachment_CaseCreate> caseAttachmentsNew = new ArrayList<TY_Attachment_CaseCreate>();
                                        for (TY_AttachmentResponse attR : evCaseFormSubmit.getPayload()
                                                        .getAttRespList())
                                        {

                                                if (StringUtils.hasText(attR.getId())
                                                                && StringUtils.hasText(attR.getUploadUrl()))
                                                {
                                                        log.info("Attachment with id : " + attR.getId()
                                                                        + " already Persisted in Document Container..");

                                                        TY_Attachment_CaseCreate caseAttachment = new TY_Attachment_CaseCreate(
                                                                        attR.getId());
                                                        caseAttachmentsNew.add(caseAttachment);

                                                }
                                        }
                                        newCaseEntity4Customer.setAttachments(caseAttachmentsNew);

                                }

                                // For Extensions
                                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getCountry())
                                                || StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage()))
                                {
                                        TY_Extensions_CaseCreate extn = new TY_Extensions_CaseCreate();
                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getCountry()))
                                        {
                                                extn.setLSO_Country(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getCountry());
                                        }

                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getLanguage()))
                                        {
                                                extn.setLSO_Language(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage());
                                        }

                                        newCaseEntity4Customer.setExtensions(extn);

                                }

                                // Set the Channel
                                newCaseEntity4Customer.setOrigin(GC_Constants.gc_SelfServiceChannel);

                                // Set the External User Flag - Pick Right
                                // Technical User in D/S API Call
                                newCaseEntity4Customer
                                                .setExternal(evCaseFormSubmit.getPayload().getCaseForm().isExternal());

                                try
                                {
                                        String caseID = srvCloudApiSrv.createCase4Customer(newCaseEntity4Customer,
                                                        desProps);
                                        if (StringUtils.hasText(caseID))
                                        {
                                                handleCaseSuccCreated(evCaseFormSubmit, cusItemO, caseID);

                                        }
                                }
                                catch (Exception e)
                                {

                                        handleCaseCreationError(evCaseFormSubmit, e);

                                }
                        }

                }
        }

        private void createCase4IndCustomerAddUser(EV_CaseFormSubmit evCaseFormSubmit, TY_DestinationProps desProps,
                        Optional<TY_CatgCusItem> cusItemO)
        {
                TY_Case_CustomerAddUser_SrvCloud newCaseEntity4Customer;
                newCaseEntity4Customer = new TY_Case_CustomerAddUser_SrvCloud();
                // Account must be present
                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getAccId()))
                {
                        newCaseEntity4Customer.setIndividualCustomer(new TY_Account_CaseCreate(
                                        evCaseFormSubmit.getPayload().getCaseForm().getAccId())); // Account
                                                                                                  // ID

                        // Case Txn. Type
                        newCaseEntity4Customer
                                        .setCaseType(evCaseFormSubmit.getPayload().getCaseForm().getCaseTxnType());
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
                                                newCaseEntity4Customer.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[3]));
                                                newCaseEntity4Customer.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Customer.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Customer.setCategoryLevel4(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 3:
                                                newCaseEntity4Customer.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[2]));
                                                newCaseEntity4Customer.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Customer.setCategoryLevel3(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 2:
                                                newCaseEntity4Customer.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[1]));
                                                newCaseEntity4Customer.setCategoryLevel2(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
                                                break;
                                        case 1:
                                                newCaseEntity4Customer.setCategoryLevel1(
                                                                new TY_CatgLvl1_CaseCreate(catTreeSelCatg[0]));
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

                                        // #JIRA - ESMLSO-516
                                        /*
                                         * Scramble Description for CC Information
                                         */
                                        String scrambledTxt = CL_ScramblingUtils.scrambleText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getDescription());
                                        if (StringUtils.hasText(scrambledTxt))
                                        {

                                                // Create Note and Get Guid back
                                                String noteId = srvCloudApiSrv.createNotes(new TY_NotesCreate(
                                                                evCaseFormSubmit.getPayload().getCaseForm()
                                                                                .isExternal(),
                                                                scrambledTxt, GC_Constants.gc_NoteTypeDescription),
                                                                desProps);
                                                if (StringUtils.hasText(noteId))
                                                {
                                                        newCaseEntity4Customer.setDescription(
                                                                        new TY_Description_CaseCreate(noteId));
                                                }
                                        }
                                }

                                // Check if Attachment needs to be Created
                                if (CollectionUtils.isNotEmpty(evCaseFormSubmit.getPayload().getAttRespList()))
                                {
                                        // Prepare POJOdetails for
                                        // TY_Case_SrvCloud
                                        // newCaseEntity4Customer
                                        List<TY_Attachment_CaseCreate> caseAttachmentsNew = new ArrayList<TY_Attachment_CaseCreate>();
                                        for (TY_AttachmentResponse attR : evCaseFormSubmit.getPayload()
                                                        .getAttRespList())
                                        {

                                                if (StringUtils.hasText(attR.getId())
                                                                && StringUtils.hasText(attR.getUploadUrl()))
                                                {
                                                        log.info("Attachment with id : " + attR.getId()
                                                                        + " already Persisted in Document Container..");

                                                        TY_Attachment_CaseCreate caseAttachment = new TY_Attachment_CaseCreate(
                                                                        attR.getId());
                                                        caseAttachmentsNew.add(caseAttachment);

                                                }
                                        }
                                        newCaseEntity4Customer.setAttachments(caseAttachmentsNew);

                                }

                                // For Extensions
                                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getCountry())
                                                || StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage()))
                                {
                                        TY_Extensions_CaseCreate extn = new TY_Extensions_CaseCreate();
                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getCountry()))
                                        {
                                                extn.setLSO_Country(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getCountry());
                                        }

                                        if (StringUtils.hasText(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getLanguage()))
                                        {
                                                extn.setLSO_Language(evCaseFormSubmit.getPayload().getCaseForm()
                                                                .getLanguage());
                                        }

                                        newCaseEntity4Customer.setExtensions(extn);

                                }

                                // Set the Channel
                                newCaseEntity4Customer.setOrigin(GC_Constants.gc_SelfServiceChannel);

                                // Set the External User Flag - Pick Right
                                // Technical User in D/S API Call
                                newCaseEntity4Customer
                                                .setExternal(evCaseFormSubmit.getPayload().getCaseForm().isExternal());

                                // Set the Reporter PFCT
                                if (StringUtils.hasText(evCaseFormSubmit.getPayload().getCaseForm().getReporter()))
                                {
                                        TY_AddUserCaseCreate addUser = new TY_AddUserCaseCreate(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getReporter(),
                                                        GC_Constants.gc_PFCT_ADDUSER);
                                        newCaseEntity4Customer.getCustomIndividualCustomers().add(addUser);
                                }

                                try
                                {
                                        String caseID = srvCloudApiSrv
                                                        .createCase4CustomerAddUser(newCaseEntity4Customer, desProps);
                                        if (StringUtils.hasText(caseID))
                                        {
                                                handleCaseSuccCreated(evCaseFormSubmit, cusItemO, caseID);

                                        }
                                }
                                catch (Exception e)
                                {

                                        handleCaseCreationError(evCaseFormSubmit, e);

                                }
                        }

                }
        }

        private void handleCaseCreationError(EV_CaseFormSubmit evCaseFormSubmit, Exception e)
        {
                String msg;
                msg = msgSrc.getMessage("ERR_CASE_POST", new Object[]
                { e.getLocalizedMessage(), evCaseFormSubmit.getPayload().getSubmGuid() }, Locale.ENGLISH);

                log.error(msg);
                TY_Message logMsg = new TY_Message(evCaseFormSubmit.getPayload().getUserId(),
                                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_CASE_CREATE,
                                evCaseFormSubmit.getPayload().getSubmGuid(), msg);

                // Instantiate and Fire the Event
                EV_LogMessage logMsgEvent = new EV_LogMessage((Object) evCaseFormSubmit.getPayload().getSubmGuid(),
                                logMsg);
                applicationEventPublisher.publishEvent(logMsgEvent);

                // Should be handled Centrally via Aspect
                throw new EX_ESMAPI(msg);
        }

        private void handleCaseSuccCreated(EV_CaseFormSubmit evCaseFormSubmit, Optional<TY_CatgCusItem> cusItemO,
                        String caseID)
        {
                String msg = "Case ID : " + caseID + " created..";
                log.info(msg);
                msg = msgSrc.getMessage("SUCC_CASE", new Object[]
                { caseID, cusItemO.get().getCaseTypeEnum().toString(), evCaseFormSubmit.getPayload().getSubmGuid() },
                                Locale.ENGLISH);
                log.info(msg);
                // Populate Success message in session

                TY_Message logMsg = new TY_Message(evCaseFormSubmit.getPayload().getUserId(),
                                Timestamp.from(Instant.now()), EnumStatus.Success, EnumMessageType.SUCC_CASE_CREATE,
                                evCaseFormSubmit.getPayload().getSubmGuid(), msg);

                // Instantiate and Fire the Event
                EV_LogMessage logMsgEvent = new EV_LogMessage((Object) evCaseFormSubmit.getPayload().getSubmGuid(),
                                logMsg);
                applicationEventPublisher.publishEvent(logMsgEvent);
        }

        private void handleCatgError(EV_CaseFormSubmit evCaseFormSubmit, Optional<TY_CatgCusItem> cusItemO)
        {
                String msg = msgSrc.getMessage("ERR_INVALID_CATG", new Object[]
                { cusItemO.get().getCaseTypeEnum().toString(),
                                evCaseFormSubmit.getPayload().getCaseForm().getCatgDesc() }, Locale.ENGLISH);

                log.error(msg);
                TY_Message logMsg = new TY_Message(evCaseFormSubmit.getPayload().getUserId(),
                                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_CASE_CATG,
                                evCaseFormSubmit.getPayload().getSubmGuid(), msg);

                // Instantiate and Fire the Event
                EV_LogMessage logMsgEvent = new EV_LogMessage((Object) evCaseFormSubmit.getPayload().getSubmGuid(),
                                logMsg);
                applicationEventPublisher.publishEvent(logMsgEvent);
        }

}
