package com.sap.cap.esmapi.casecreation.srv.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.casecreation.enums.EnumCaseScenario;
import com.sap.cap.esmapi.casecreation.enums.EnumMainPartnerType;
import com.sap.cap.esmapi.casecreation.enums.EnumReporterType;
import com.sap.cap.esmapi.casecreation.pojos.TY_CaseContext;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseContextBuilder;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CL_CaseContextBuilder implements IF_CaseContextBuilder
{
    @Override
    public TY_CaseContext build(EV_CaseFormSubmit event)
    {
        TY_CaseContext context = new TY_CaseContext();

        context.setCaseEvent(event);

        context.setDestinationProps(event.getPayload().getDesProps());

        EnumMainPartnerType mainPartner = resolveMainPartner(event);

        EnumReporterType reporter = resolveReporter(event);

        context.setCaseScenario(EnumCaseScenario.resolve(mainPartner, reporter));

        return context;
    }

    private EnumMainPartnerType resolveMainPartner(EV_CaseFormSubmit event)
    {
        TY_Case_Form form = event.getPayload().getCaseForm();

        if (StringUtils.hasText(form.getMdgAccount()))
        {
            return EnumMainPartnerType.ACCOUNT;
        }

        if (StringUtils.hasText(form.getAccId()))
        {
            return form.isEmployee() ? EnumMainPartnerType.EMPLOYEE : EnumMainPartnerType.INDIVIDUAL_CUSTOMER;
        }

        throw new IllegalArgumentException("Unable to determine Main Partner.");
    }

    private EnumReporterType resolveReporter(EV_CaseFormSubmit event)
    {
        TY_Case_Form form = event.getPayload().getCaseForm();

        if (!StringUtils.hasText(form.getReporter()))
        {
            return EnumReporterType.NONE;
        }

        if (form.isReporterAccount())
        {
            return EnumReporterType.ACCOUNT;
        }

        if (form.isReporterEmployee())
        {
            return EnumReporterType.EMPLOYEE;
        }

        return EnumReporterType.INDIVIDUAL_CUSTOMER;
    }
}