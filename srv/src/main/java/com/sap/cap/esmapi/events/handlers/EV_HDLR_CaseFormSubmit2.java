package com.sap.cap.esmapi.events.handlers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseCreationService;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;
import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class EV_HDLR_CaseFormSubmit2
{
        private final IF_CaseCreationService caseCreationSrv;

        private final ApplicationEventPublisher publisher;

        private final TY_CatgCus catgCusSrv;

        private final MessageSource msgSrc;

        @Async
        @EventListener
        public void handleCaseFormSubmission(EV_CaseFormSubmit evCaseFormSubmit)
        {
                log.info("Received case form submission event: {}", evCaseFormSubmit);

                Optional<TY_CatgCusItem> cusItemO = catgCusSrv.getCustomizations().stream()
                                .filter(g -> g.getCaseType()
                                                .equals(evCaseFormSubmit.getPayload().getCaseForm().getCaseTxnType()))
                                .findFirst();

                try
                {
                        String caseId = caseCreationSrv.createCase(evCaseFormSubmit);
                        log.info("Case created successfully with ID: {}", caseId);
                        handleCaseSuccess(evCaseFormSubmit, cusItemO, caseId);
                }
                catch (EX_ESMAPI ex)
                {
                        log.error("Error occurred while creating case: {}", ex.getMessage(), ex);
                        // Handle the exception as needed, e.g., publish an error event or notify the
                        // user
                        handleCaseCreationError(evCaseFormSubmit, ex);
                }
        }

        private void handleCaseSuccess(EV_CaseFormSubmit evCaseFormSubmit, Optional<TY_CatgCusItem> cusItemO,
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
                publisher.publishEvent(logMsgEvent);
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
                publisher.publishEvent(logMsgEvent);

                // Should be handled Centrally via Aspect
                throw new EX_ESMAPI(msg);
        }

}