package com.sap.cap.esmapi.ui.srv.intf;

import com.sap.cap.esmapi.events.event.EV_CaseFormSplSubmit;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;

public interface IF_SplCatgCaseCreateSrv
{
    public String createCase(EV_CaseFormSplSubmit ev_CaseFormSplSubmit) throws EX_ESMAPI;
}
