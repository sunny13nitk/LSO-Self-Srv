package com.sap.cap.esmapi.casecreation.srv.impl;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sap.cap.esmapi.casecreation.pojos.TY_CaseContext;
import com.sap.cap.esmapi.casecreation.srv.intf.IF_CaseContextValidator;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;

@Service
public class CL_CaseContextValidator implements IF_CaseContextValidator
{

    @Override
    public void validate(TY_CaseContext context) throws EX_ESMAPI
    {
        Objects.requireNonNull(context.getCaseScenario(), "Case Scenario cannot be null.");

        Objects.requireNonNull(context.getDestinationProps(), "Destination Properties cannot be null.");

        Objects.requireNonNull(context.getCaseEvent(), "Case Event cannot be null.");
    }

}
