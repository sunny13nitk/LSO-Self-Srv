package com.sap.cap.esmapi.casecreation.srv.impl;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.cap.esmapi.casecreation.enums.EnumReporterType;
import com.sap.cap.esmapi.casecreation.pojos.TY_CaseContext;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CasePayloadBuilder;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.pojos.TY_Account_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_AddUserCaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_CatgLvl1_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Employee_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Extensions_CaseCreate;

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

        addMainPartner(root, context);

        addReporter(root, context);

        addCommonFields(root, context);

        addDescription(root, context);

        addAttachments(root, context);

        return root;
    }

    private void addReporter(ObjectNode root, TY_CaseContext context)
    {
        // No reporter
        if (context.getCaseScenario().getReporterType() == EnumReporterType.NONE)
        {
            return;
        }

        final TY_Case_Form form = context.getCaseEvent().getPayload().getCaseForm();

        TY_AddUserCaseCreate reporter = new TY_AddUserCaseCreate();

        reporter.setPartyId(form.getReporter());

        switch (context.getCaseScenario().getReporterType())
        {
        case ACCOUNT:

            reporter.setPartyRole(GC_Constants.gc_PFCT_ACCOUNT);

            root.set("customAccounts", objectMapper.valueToTree(List.of(reporter)));
            break;

        case EMPLOYEE:

            reporter.setPartyRole(GC_Constants.gc_PFCT_ADDUSER);

            root.set("customEmployees", objectMapper.valueToTree(List.of(reporter)));
            break;

        case INDIVIDUAL_CUSTOMER:

            reporter.setPartyRole(GC_Constants.gc_PFCT_ADDUSER);

            root.set("customIndividualCustomers", objectMapper.valueToTree(List.of(reporter)));
            break;
        default:
            break;
        }
    }

    private void addMainPartner(ObjectNode root, TY_CaseContext context)
    {
        TY_Case_Form form = context.getCaseEvent().getPayload().getCaseForm();

        switch (context.getCaseScenario().getMainPartnerType())
        {
        case ACCOUNT:

            root.set("account", objectMapper.valueToTree(new TY_Account_CaseCreate(form.getMdgAccount())));
            break;

        case EMPLOYEE:

            root.set("employee", objectMapper.valueToTree(new TY_Employee_CaseCreate(form.getAccId())));
            break;

        case INDIVIDUAL_CUSTOMER:

            root.set("individualCustomer", objectMapper.valueToTree(new TY_Account_CaseCreate(form.getAccId())));
            break;
        }
    }

    private void addCommonFields(ObjectNode root, TY_CaseContext context)
    {
        final TY_Case_Form form = context.getCaseEvent().getPayload().getCaseForm();

        root.put("subject", form.getSubject());

        root.put("caseType", form.getCaseTxnType());

        root.put("origin", GC_Constants.gc_SelfServiceChannel);

        addCategories(root, context);

        addExtensions(root, context);
    }

    private void addExtensions(ObjectNode root, TY_CaseContext context)
    {
        TY_Case_Form form = context.getCaseEvent().getPayload().getCaseForm();

        TY_Extensions_CaseCreate extensions = new TY_Extensions_CaseCreate();

        extensions.setLSO_Country(form.getCountry());

        extensions.setLSO_Language(form.getLanguage());

        root.set("extensions", objectMapper.valueToTree(extensions));
    }

    private void addCategories(ObjectNode root, TY_CaseContext context)
    {
        String[] categories = context.getCaseEvent().getPayload().getCatTreeSelCatg();

        if (categories == null || categories.length == 0)
        {
            return;
        }

        int level = 1;

        for (int i = categories.length - 1; i >= 0; i--)
        {
            if (!StringUtils.hasText(categories[i]))
            {
                continue;
            }

            root.set("categoryLevel" + level++, objectMapper.valueToTree(new TY_CatgLvl1_CaseCreate(categories[i])));
        }
    }

    private void addDescription(ObjectNode root, TY_CaseContext context)
    {
        if (context.getDescription() != null)
        {
            root.set("description", objectMapper.valueToTree(context.getDescription()));
        }
    }

    private void addAttachments(ObjectNode root, TY_CaseContext context)
    {
        if (CollectionUtils.isEmpty(context.getAttachments()))
        {
            return;
        }

        root.set("attachments", objectMapper.valueToTree(context.getAttachments()));
    }

}