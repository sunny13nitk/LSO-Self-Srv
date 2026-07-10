package com.sap.cap.esmapi.casecreation.srv.impl;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.cap.esmapi.casecreation.pojos.TY_CaseContext;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseContextBuilder;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseCreationService;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CasePayloadBuilder;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseScenarioValidator;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CL_CaseCreationService implements IF_CaseCreationService
{

    private final IF_CaseContextBuilder caseContextBuilder;

    private final IF_CaseScenarioValidator scenarioValidator;

    private final IF_CasePayloadBuilder payloadBuilder;

    private final IF_SrvCloudAPI srvCloudApi;

    @Override
    public String createCase(EV_CaseFormSubmit event) throws EX_ESMAPI
    {

        TY_CaseContext context = caseContextBuilder.build(event);

        scenarioValidator.validate(context);

        createCaseNote(context);
        collectAttachmentReferences(context);

        ObjectNode payload = payloadBuilder.build(context);

        return srvCloudApi.createCase(payload, context.getDestinationProps());

    }

    private void createCaseNote(TY_CaseContext context)
    {
        // TODO Auto-generated method stub
    }

    private void collectAttachmentReferences(TY_CaseContext context)
    {
        // TODO Auto-generated method stub
    }

}
