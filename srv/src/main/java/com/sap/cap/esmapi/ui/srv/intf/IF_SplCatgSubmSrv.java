package com.sap.cap.esmapi.ui.srv.intf;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_CaseFormSubmSpl;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;

public interface IF_SplCatgSubmSrv
{
    public TY_CaseFormSubmSpl validateAndSubmitCaseForm(TY_Case_Form caseForm) throws EX_ESMAPI;
}
