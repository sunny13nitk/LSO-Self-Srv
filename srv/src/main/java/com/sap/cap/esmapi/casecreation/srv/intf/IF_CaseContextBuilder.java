package com.sap.cap.esmapi.casecreation.srv.intf;

import com.sap.cap.esmapi.casecreation.pojos.TY_CaseContext;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;

public interface IF_CaseContextBuilder
{
    TY_CaseContext build(EV_CaseFormSubmit event);
}
