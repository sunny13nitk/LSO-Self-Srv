package com.sap.cap.esmapi.casecreation.srv.intf;

import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;

public interface IF_CaseCreationService
{
    String createCase(EV_CaseFormSubmit event) throws EX_ESMAPI;
}
