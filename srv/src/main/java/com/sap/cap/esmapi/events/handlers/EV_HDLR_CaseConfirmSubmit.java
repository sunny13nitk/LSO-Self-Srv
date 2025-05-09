package com.sap.cap.esmapi.events.handlers;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.events.event.EV_CaseConfirmSubmit;
import com.sap.cap.esmapi.events.event.EV_CaseReplySubmit;
import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_CaseConfirmPOJO;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EV_HDLR_CaseConfirmSubmit
{
    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IF_SrvCloudAPI srvCloudApiSrv;

    @Autowired
    private TY_CatgCus config;

    @Async
    @EventListener
    public void handleCaseConfirmSubmission(EV_CaseConfirmSubmit evCaseCnfSubmit)
    {
        if (evCaseCnfSubmit != null && config != null)
        {
            TY_CaseConfirmPOJO caseDetails = evCaseCnfSubmit.getPayload();
            if (caseDetails != null)
            {

                if (StringUtils.hasText(caseDetails.getCaseGuid()) && StringUtils.hasText(caseDetails.getETag())
                        && caseDetails.getDesProps() != null)
                {

                    try
                    {

                        // Invoke Srv cloud API to Patch/Update the Case
                        if (srvCloudApiSrv.confirmCase(caseDetails))
                        {
                            handleCaseSuccUpdated(caseDetails);
                        }
                    }

                    catch (EX_ESMAPI | IOException e)
                    {
                        // Handle Case Details Fetch Error
                        handleCaseDetailsFetchError(caseDetails, e);
                    }

                }

            }

        }

    }

    private void handleCaseSuccUpdated(TY_CaseConfirmPOJO caseDetails)
    {
        String msg;
        // SUCC_CASE_CONFIRM_UPDATE= Case with id - {0} confirmed successfully for
        // Submission id - {1}.
        msg = msgSrc.getMessage("SUCC_CASE_CONFIRM_UPDATE", new Object[]
        { caseDetails.getCaseId(), caseDetails.getSubmGuid() }, Locale.ENGLISH);

        log.info(msg);
        TY_Message logMsg = new TY_Message(caseDetails.getUserId(), Timestamp.from(Instant.now()), EnumStatus.Success,
                EnumMessageType.SUCC_CASE_CONFIRM_SAVE, caseDetails.getSubmGuid(), msg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) caseDetails.getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);
    }

    private void handleCaseDetailsFetchError(TY_CaseConfirmPOJO caseDetails, Exception e)
    {
        String msg;
        // Error Fetching Case Details for Case ID - {0} for Case Reply Submission id -
        // {1} for User - {2}! Details - {3} .
        msg = msgSrc.getMessage("ERR_CASE_DET_FETCH_REPL_SUBM", new Object[]
        { caseDetails.getCaseId(), caseDetails.getSubmGuid(), caseDetails.getUserId(), e.getLocalizedMessage(), },
                Locale.ENGLISH);

        log.error(msg);
        TY_Message logMsg = new TY_Message(caseDetails.getUserId(), Timestamp.from(Instant.now()), EnumStatus.Error,
                EnumMessageType.ERR_CASE_CNF_SAV, caseDetails.getSubmGuid(), msg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) caseDetails.getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);

        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }
}
