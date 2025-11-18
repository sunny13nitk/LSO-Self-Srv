package com.sap.cap.esmapi.ui.srv.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_CaseFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_CaseFormSubmSpl;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.ui.srv.intf.IF_SplCatgSubmSrv;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("SRV_CASESUBM_EXAM_REQUEST")
@RequiredArgsConstructor
@Slf4j
public class CL_SplCatgSubmSrv implements IF_SplCatgSubmSrv
{

    private final IF_UserSessionSrv userSessionSrv;
    private final TY_CatgCus catgCusSrv;
    private final MessageSource msgSrc;
    private final IF_CatalogSrv catalogSrv;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final String regex = "^\\d{10}$";

    @Override
    public TY_CaseFormSubmSpl validateAndSubmitCaseForm(TY_Case_Form caseForm) throws EX_ESMAPI
    {

        TY_CaseFormSubmSpl caseFormAsyncSpl = null;

        if (caseForm != null && userSessionSrv != null)
        {

            log.info("Inside Case Form Submission for Specal Category with form details as : " + caseForm.toString());
            TY_CaseFormAsync caseFormAsync = new TY_CaseFormAsync();
            caseFormAsync.setCaseForm(caseForm);
            caseFormAsync.setSubmGuid(UUID.randomUUID().toString());
            // Latest time Stamp from Form Submissions
            caseFormAsync.setTimestamp(Timestamp.from(Instant.now()));
            caseFormAsync.setUserId(userSessionSrv.getUserDetails4mSession().getUserId());

            // Get Case Type Enum from Case Transaction Type
            Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                    .filter(f -> f.getCaseType().equals(caseForm.getCaseTxnType())).findFirst();
            if (cusItemO.isPresent())
            {
                if (!CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
                {
                    if (cusItemO.isPresent() && catalogSrv != null)
                    {
                        String[] catTreeSelCatg = catalogSrv.getCatgHierarchyforCatId(caseForm.getCatgDesc(),
                                cusItemO.get().getCaseTypeEnum());
                        caseFormAsync.setCatTreeSelCatg(catTreeSelCatg);

                        caseFormAsyncSpl = new TY_CaseFormSubmSpl();
                        caseFormAsyncSpl.setCaseFormAsync(caseFormAsync);
                    }
                }
            }

            // validate the form fields as per Special Category requirements
            if (caseFormAsyncSpl != null)
            {
                if (isCaseFormDataValid(caseForm))
                {
                    caseFormAsyncSpl.setValid(true);
                }
                else
                {
                    caseFormAsyncSpl.setValid(false);
                }
            }
        }
        return caseFormAsyncSpl;

    }

    private boolean isCaseFormDataValid(TY_Case_Form caseForm)
    {

        boolean isValid = false;
        /*
         * for Exam Extensions Validation rules : 1. Appointment Id is mandatory with 10
         * digits only 2. Either of extension options is selected
         * 
         */
        if (StringUtils.hasText(caseForm.getAppId()))
        {
            if (caseForm.getAppId().matches(regex))
            {
                log.info("Appointment Id is valid with 10 digits : " + caseForm.getAppId());

                // Check for atleast one extension option selected
                if (caseForm.isExtraTime() || caseForm.isExambreak() || caseForm.isExtramonitor()
                        || caseForm.isExtraperson())
                {
                    log.info("Atleast one extension option is selected for the Exam Extensions request");
                    isValid = true;
                }
                else
                {
                    handleNoExtensionOption();
                }
            }
            else
            {
                handleInvalidAppointmentID(caseForm.getAppId());
            }
        }
        else
        {
            handleInvalidAppointmentID(caseForm.getAppId());
        }

        return isValid;
    }

    private void handleInvalidAppointmentID(String appid)
    {
        String msg = msgSrc.getMessage("ERR_APPOINTMNT_ID", new Object[]
        { appid }, null);
        log.error(msg);
        userSessionSrv.addFormErrors(msg);
        // Logging Framework
        TY_Message logMsg = new TY_Message(userSessionSrv.getUserDetails4mSession().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_PAYLOAD, appid, msg);
        userSessionSrv.getMessageStack().add(logMsg);
        // Instantiate and Fire the Event : Syncronous processing
        EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);
    }

    private void handleNoExtensionOption()
    {
        String msg = msgSrc.getMessage("ERR_EXAM_EXTN_NOOPTION", null, null);
        log.error(msg);
        userSessionSrv.addFormErrors(msg);
        // Logging Framework
        TY_Message logMsg = new TY_Message(userSessionSrv.getUserDetails4mSession().getUserId(),
                Timestamp.from(Instant.now()), EnumStatus.Error, EnumMessageType.ERR_PAYLOAD, null, msg);
        userSessionSrv.getMessageStack().add(logMsg);
        // Instantiate and Fire the Event : Syncronous processing
        EV_LogMessage logMsgEvent = new EV_LogMessage(this, logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);
    }

}
