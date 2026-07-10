package com.sap.cap.esmapi.casecreation.srv.impl;

import org.springframework.stereotype.Service;

import com.sap.cap.esmapi.casecreation.enums.EnumMainPartnerType;
import com.sap.cap.esmapi.casecreation.enums.EnumReporterType;
import com.sap.cap.esmapi.casecreation.pojos.TY_CaseContext;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseContextBuilder;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CL_CaseContextBuilder implements IF_CaseContextBuilder
{
    @Override
    public TY_CaseContext build(EV_CaseFormSubmit event)
    {
        return null;
    }

    private EnumMainPartnerType resolveMainPartner()
    {
        return null;
    }

    private EnumReporterType resolveReporter()
    {
        return null;
    }
}