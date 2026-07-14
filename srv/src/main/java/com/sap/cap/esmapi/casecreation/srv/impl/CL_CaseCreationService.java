package com.sap.cap.esmapi.casecreation.srv.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.cap.esmapi.casecreation.pojos.TY_CaseContext;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseContextBuilder;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseContextValidator;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseCreationService;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CasePayloadBuilder;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.pojos.TY_AttachmentResponse;
import com.sap.cap.esmapi.utilities.pojos.TY_Attachment_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Description_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_NotesCreate;
import com.sap.cap.esmapi.utilities.scrambling.CL_ScramblingUtils;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CL_CaseCreationService implements IF_CaseCreationService
{

    private final IF_CaseContextBuilder caseContextBuilder;

    private final IF_CaseContextValidator scenarioValidator;

    private final IF_CasePayloadBuilder payloadBuilder;

    private final IF_SrvCloudAPI srvCloudApi;
    private final ObjectMapper objectMapper;

    @Override
    public String createCase(EV_CaseFormSubmit event) throws EX_ESMAPI
    {
        try
        {
            log.info("========== Case Creation Started ==========");

            TY_CaseContext context = caseContextBuilder.build(event);

            logContext(context);

            scenarioValidator.validate(context);

            createCaseNote(context);

            collectAttachmentReferences(context);

            ObjectNode payload = payloadBuilder.build(context);

            try
            {
                log.info("Generated Payload:\n{}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            }
            catch (JsonProcessingException e)
            {
                log.warn("Failed to serialize payload for logging.", e);
            }

            String caseId = srvCloudApi.createCase(payload, context.getDestinationProps());

            log.info("Service Cloud Case Created Successfully. Case ID : {}", caseId);

            log.info("========== Case Creation Completed ==========");
            return caseId;

        }

        catch (EX_ESMAPI e)
        {
            log.error("Case Creation Failed for Submission ID : {}", event.getPayload().getSubmGuid(), e);
            throw e;
        }

    }

    private void createCaseNote(TY_CaseContext context) throws EX_ESMAPI
    {
        TY_Case_Form form = context.getCaseEvent().getPayload().getCaseForm();

        if (!StringUtils.hasText(form.getDescription()))
        {
            return;
        }

        String scrambledTxt = CL_ScramblingUtils.scrambleText(form.getDescription());

        if (!StringUtils.hasText(scrambledTxt))
        {
            return;
        }

        String noteId = srvCloudApi.createNotes(
                new TY_NotesCreate(form.isExternal(), scrambledTxt, GC_Constants.gc_NoteTypeDescription),
                context.getDestinationProps());

        context.setDescription(new TY_Description_CaseCreate(noteId));
    }

    private void collectAttachmentReferences(TY_CaseContext context)
    {
        List<TY_Attachment_CaseCreate> attachments = new ArrayList<>();

        for (TY_AttachmentResponse attR : context.getCaseEvent().getPayload().getAttRespList())
        {
            if (StringUtils.hasText(attR.getId()) && StringUtils.hasText(attR.getUploadUrl()))
            {
                log.info("Attachment with id : {} already persisted in Document Container.", attR.getId());

                attachments.add(new TY_Attachment_CaseCreate(attR.getId()));
            }
        }

        context.setAttachments(attachments);
    }

    private void logContext(TY_CaseContext context)
    {
        log.info("========== Case Context ==========");

        log.info("Scenario      : {}", context.getCaseScenario());

        if (context.getCaseScenario() != null)
        {
            log.info("Main Partner  : {}", context.getCaseScenario().getMainPartnerType());
            log.info("Reporter      : {}", context.getCaseScenario().getReporterType());
        }

        log.info("Note Present  : {}",
                StringUtils.hasText(context.getCaseEvent().getPayload().getCaseForm().getDescription()));

        List<TY_AttachmentResponse> attachments = context.getCaseEvent().getPayload().getAttRespList();

        log.info("Attachments   : {}", attachments == null ? 0 : attachments.size());

        log.info("Destination   : {}", context.getDestinationProps().getBaseUrl());

        log.info("==================================");
    }
}
