package com.sap.cap.esmapi.events.handlers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.events.event.EV_CaseFormSplSubmit;
import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.srv.intf.IF_SplCatgCaseCreateSrv;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EV_HDLR_CaseFormSplSubmit
{
        @Autowired
        private TY_CatgCus catgCusSrv;

        @Autowired
        private MessageSource msgSrc;

        @Autowired
        private ApplicationEventPublisher applicationEventPublisher;

        @Autowired
        private ApplicationContext appCtxt;

        @Async
        @EventListener
        public void handleCaseFormSubmission(EV_CaseFormSplSubmit evCaseFormSubmit)
        {

                if (evCaseFormSubmit != null && catgCusSrv != null
                                && evCaseFormSubmit.getPayload().getDesProps() != null)
                {
                        log.info("Inside Case Form Asyncronous Submit Event Processing for Spl. Category ---- for Case Submission ID: "
                                        + evCaseFormSubmit.getPayload().getSubmGuid() + " for Special Category : "
                                        + evCaseFormSubmit.getSplCatgSeek().getSplCatgCus().getCatg());

                        Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                                        .filter(g -> g.getCaseType().equals(
                                                        evCaseFormSubmit.getPayload().getCaseForm().getCaseTxnType()))
                                        .findFirst();

                        if (StringUtils.hasText(evCaseFormSubmit.getSplCatgSeek().getSplCatgCus().getCasecreatesrv())
                                        && appCtxt != null)
                        {
                                log.info("Invoking Case Create Service : "
                                                + evCaseFormSubmit.getSplCatgSeek().getSplCatgCus().getCasecreatesrv());
                                IF_SplCatgCaseCreateSrv caseCreateSrv = (IF_SplCatgCaseCreateSrv) appCtxt.getBean(
                                                evCaseFormSubmit.getSplCatgSeek().getSplCatgCus().getCasecreatesrv(),
                                                IF_SplCatgCaseCreateSrv.class);
                                if (caseCreateSrv != null)
                                {
                                        try
                                        {
                                                String caseId = caseCreateSrv.createCase(evCaseFormSubmit);
                                                if (StringUtils.hasText(caseId))
                                                {
                                                        handleCaseSuccCreated(evCaseFormSubmit, cusItemO, caseId);
                                                }
                                        }
                                        catch (EX_ESMAPI ex)
                                        {
                                                handleCaseCreationError(evCaseFormSubmit, ex);
                                        }
                                }
                        }

                }

        }

        private void handleCaseCreationError(EV_CaseFormSplSubmit evCaseFormSubmit, Exception e)
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

        private void handleCaseSuccCreated(EV_CaseFormSplSubmit evCaseFormSubmit, Optional<TY_CatgCusItem> cusItemO,
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

}
