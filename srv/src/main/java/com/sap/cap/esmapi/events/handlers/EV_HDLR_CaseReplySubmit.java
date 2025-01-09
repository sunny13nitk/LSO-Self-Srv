package com.sap.cap.esmapi.events.handlers;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.sap.cap.esmapi.events.event.EV_CaseReplySubmit;
import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;
import com.sap.cap.esmapi.utilities.pojos.TY_AttachmentResponse;
import com.sap.cap.esmapi.utilities.pojos.TY_Attachment_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseDetails;
import com.sap.cap.esmapi.utilities.pojos.TY_CasePatchInfo;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseReplyNote;
import com.sap.cap.esmapi.utilities.pojos.TY_Case_SrvCloud_Reply;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesCreate;
import com.sap.cap.esmapi.utilities.scrambling.CL_ScramblingUtils;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EV_HDLR_CaseReplySubmit
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
    public void handleCaseReplySubmission(EV_CaseReplySubmit evCaseReply)
    {
        if (evCaseReply != null && config != null)
        {
            if (evCaseReply.getPayload().isValid() && CollectionUtils.isNotEmpty(config.getCustomizations())
                    && evCaseReply.getPayload().getDesProps() != null)
            {

                // prepare the Payload for PATCH operation for the case

                // Case Reply Form Attached
                if (evCaseReply.getPayload().getCaseReply() != null)
                {
                    TY_DestinationProps desProps = evCaseReply.getPayload().getDesProps();
                    // Case GUID , Type and ETag Bound
                    if (StringUtils.hasText(evCaseReply.getPayload().getCaseReply().getCaseDetails().getCaseGuid())
                            && StringUtils.hasText(evCaseReply.getPayload().getCaseReply().getCaseDetails().getETag())
                            && StringUtils
                                    .hasText(evCaseReply.getPayload().getCaseReply().getCaseDetails().getCaseType()))
                    {

                        // Initialize PAyload
                        TY_Case_SrvCloud_Reply caseReplyPayload = new TY_Case_SrvCloud_Reply();

                        // Initialize Note(s) - At least one reply
                        caseReplyPayload.setNotes(new ArrayList<TY_CaseReplyNote>());

                        caseReplyPayload
                                .setCaseType(evCaseReply.getPayload().getCaseReply().getCaseDetails().getCaseType());

                        // Set Case Post Reply Status as fetched from Config in Case Reply Form
                        caseReplyPayload.setStatus(evCaseReply.getPayload().getCaseReply().getCaseDetails()
                                .getStatusTransitionCFG().getToStatusCode());

                        TY_CaseDetails caseDetails;
                        try
                        {
                            // Handle for Case Note(s) Existing
                            caseDetails = srvCloudApiSrv.getCaseDetails4Case(
                                    evCaseReply.getPayload().getCaseReply().getCaseDetails().getCaseGuid(), desProps);

                            if (caseDetails != null)
                            {
                                // Prepare Payload for Existing Note(s) in PATCH Operation
                                if (CollectionUtils.isNotEmpty(caseDetails.getNotes()))
                                {
                                    List<TY_CaseReplyNote> caseNotesExisting = caseDetails.getNotes().stream().map(n ->
                                    {
                                        TY_CaseReplyNote noteEx = new TY_CaseReplyNote(n.getContent(), n.getId(),
                                                n.getNoteId(), n.getNoteType());
                                        return noteEx;
                                    }).collect(Collectors.toList());
                                    caseReplyPayload.getNotes().addAll(caseNotesExisting);
                                }

                            }

                            // Handle for Case Reply Current
                            if (StringUtils.hasText(evCaseReply.getPayload().getCaseReply().getReply()))
                            {
                                // Reply Note Type Scan
                                Optional<TY_CatgCusItem> cfgO = config.getCustomizations().stream()
                                        .filter(c -> c.getCaseType().equals(
                                                evCaseReply.getPayload().getCaseReply().getCaseDetails().getCaseType()))
                                        .findFirst();
                                if (cfgO.isPresent())
                                {
                                    // Create REply in Configured Note Type
                                    if (StringUtils.hasText(cfgO.get().getReplyNoteType()))
                                    {

                                        // #JIRA - ESMLSO-516
                                        /*
                                         * Scramble Description for CC Information
                                         */
                                        String scrambledTxt = CL_ScramblingUtils
                                                .scrambleText(evCaseReply.getPayload().getCaseReply().getReply());
                                        if (StringUtils.hasText(scrambledTxt))
                                        {
                                            // Create Note and Get Guid back
                                            String noteId = srvCloudApiSrv.createNotes(new TY_NotesCreate(false,
                                                    scrambledTxt, cfgO.get().getReplyNoteType()), desProps);
                                            if (StringUtils.hasText(noteId))
                                            {
                                                caseReplyPayload.getNotes().add(new TY_CaseReplyNote(scrambledTxt, null,
                                                        noteId, cfgO.get().getReplyNoteType()));
                                            }
                                        }
                                    }
                                    else
                                    {
                                        // Create REply in Default Note Type
                                        // Create Note and Get Guid back
                                        // #JIRA - ESMLSO-516
                                        /*
                                         * Scramble Description for CC Information
                                         */
                                        String scrambledTxt = CL_ScramblingUtils
                                                .scrambleText(evCaseReply.getPayload().getCaseReply().getReply());
                                        if (StringUtils.hasText(scrambledTxt))
                                        {
                                            String noteId = srvCloudApiSrv.createNotes(
                                                    new TY_NotesCreate(false, scrambledTxt, null), desProps);
                                            if (StringUtils.hasText(noteId))
                                            {
                                                caseReplyPayload.getNotes()
                                                        .add(new TY_CaseReplyNote(scrambledTxt, null, noteId, null));
                                            }
                                        }

                                    }
                                }

                            }

                            // Check if Attachment needs to be Created
                            if (CollectionUtils.isNotEmpty(evCaseReply.getPayload().getAttRespList()))
                            {
                                // Prepare POJOdetails for TY_Case_SrvCloud newCaseEntity
                                List<TY_Attachment_CaseCreate> caseAttachmentsNew = new ArrayList<TY_Attachment_CaseCreate>();
                                for (TY_AttachmentResponse attR : evCaseReply.getPayload().getAttRespList())
                                {

                                    if (StringUtils.hasText(attR.getId()) && StringUtils.hasText(attR.getUploadUrl()))
                                    {
                                        log.info("Attachment with id : " + attR.getId()
                                                + " already Persisted in Document Container..");

                                        TY_Attachment_CaseCreate caseAttachment = new TY_Attachment_CaseCreate(
                                                attR.getId());
                                        caseAttachmentsNew.add(caseAttachment);

                                    }
                                }
                                caseReplyPayload.setAttachments(caseAttachmentsNew);

                            }

                            // Pass the External User Flag for External User Logon
                            caseReplyPayload.setExternal(evCaseReply.getPayload().getCaseReply().isExternal());

                            if (caseReplyPayload != null)
                            {
                                // Invoke Srv cloud API to Patch/Update the Case
                                if (srvCloudApiSrv
                                        .updateCasewithReply(
                                                new TY_CasePatchInfo(caseDetails.getCaseGuid(),
                                                        evCaseReply.getPayload().getCaseReply().getCaseDetails()
                                                                .getCaseId(),
                                                        caseDetails.getETag()),
                                                caseReplyPayload, desProps))
                                {
                                    handleCaseSuccUpdated(
                                            evCaseReply.getPayload().getCaseReply().getCaseDetails().getCaseId(),
                                            evCaseReply.getPayload().getSubmGuid(), evCaseReply);
                                }
                            }
                        }
                        catch (EX_ESMAPI | IOException e)
                        {
                            // Handle Case Details Fetch Error
                            handleCaseDetailsFetchError(evCaseReply, e);
                        }

                    }
                }

            }
        }
    }

    private void handleCaseSuccUpdated(String string, String submId, EV_CaseReplySubmit evCaseReply)
    {
        String msg;
        // Reply for Case with id - {0} updated successfully for Submission id - {1}.
        msg = msgSrc.getMessage("SUCC_CASE_REPLY_UPDATE", new Object[]
        { string, submId }, Locale.ENGLISH);

        log.info(msg);
        TY_Message logMsg = new TY_Message(evCaseReply.getPayload().getUserId(), Timestamp.from(Instant.now()),
                EnumStatus.Success, EnumMessageType.SUCC_CASE_REPL_SAVE, evCaseReply.getPayload().getSubmGuid(), msg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) evCaseReply.getPayload().getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);
    }

    private void handleCaseDetailsFetchError(EV_CaseReplySubmit evCaseReply, Exception e)
    {
        String msg;
        // Error Fetching Case Details for Case ID - {0} for Case Reply Submission id -
        // {1} for User - {2}! Details - {3} .
        msg = msgSrc.getMessage("ERR_CASE_DET_FETCH_REPL_SUBM", new Object[]
        { evCaseReply.getPayload().getCaseReply().getCaseDetails().getCaseId(), evCaseReply.getPayload().getSubmGuid(),
                evCaseReply.getPayload().getUserId(), e.getLocalizedMessage(), }, Locale.ENGLISH);

        log.error(msg);
        TY_Message logMsg = new TY_Message(evCaseReply.getPayload().getUserId(), Timestamp.from(Instant.now()),
                EnumStatus.Error, EnumMessageType.ERR_CASE_REPL_SAVE, evCaseReply.getPayload().getSubmGuid(), msg);

        // Instantiate and Fire the Event
        EV_LogMessage logMsgEvent = new EV_LogMessage((Object) evCaseReply.getPayload().getSubmGuid(), logMsg);
        applicationEventPublisher.publishEvent(logMsgEvent);

        // Should be handled Centrally via Aspect
        throw new EX_ESMAPI(msg);
    }
}
