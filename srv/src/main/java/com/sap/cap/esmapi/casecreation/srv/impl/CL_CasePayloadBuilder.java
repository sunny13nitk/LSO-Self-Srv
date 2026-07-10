package com.sap.cap.esmapi.casecreation.srv.impl;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.cap.esmapi.casecreation.pojos.TY_CaseContext;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CasePayloadBuilder;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.pojos.TY_Attachment_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Description_CaseCreate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CL_CasePayloadBuilder implements IF_CasePayloadBuilder
{

    private final ObjectMapper objectMapper;

    @Override
    public ObjectNode build(TY_CaseContext context) throws EX_ESMAPI
    {

        ObjectNode root = objectMapper.createObjectNode();

        addScenario(root, context);

        addCommonFields(root, context);

        addDescription(root, context);

        addAttachments(root, context);

        return root;
    }

    private void addScenario(ObjectNode root, TY_CaseContext context)
    {

    }

    private void addCommonFields(ObjectNode root, TY_CaseContext context)
    {

        TY_Case_Form form = context.getCaseEvent().getPayload().getCaseForm();

        root.put("subject", form.getSubject());

        root.put("caseType", form.getCaseTxnType());

        root.put("origin", GC_Constants.gc_SelfServiceChannel);

    }

    private void addDescription(ObjectNode root, TY_CaseContext context)
    {

        if (!StringUtils.hasText(context.getNoteId()))
        {
            return;
        }

        TY_Description_CaseCreate description = new TY_Description_CaseCreate(context.getNoteId());

        root.set("description", objectMapper.valueToTree(description));

    }

    private void addAttachments(ObjectNode root, TY_CaseContext context)
    {

        if (CollectionUtils.isEmpty(context.getAttachmentIds()))
        {
            return;
        }

        List<TY_Attachment_CaseCreate> attachments = context.getAttachmentIds().stream()
                .map(TY_Attachment_CaseCreate::new).toList();

        root.set("attachments", objectMapper.valueToTree(attachments));

    }

}