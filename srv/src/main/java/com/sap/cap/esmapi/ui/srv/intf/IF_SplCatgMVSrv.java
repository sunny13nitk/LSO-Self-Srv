package com.sap.cap.esmapi.ui.srv.intf;

import org.springframework.web.servlet.ModelAndView;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;

public interface IF_SplCatgMVSrv
{
    public ModelAndView getSplCatgModelAndView(TY_Case_Form caseForm, boolean... notiGgnoreErrors) throws EX_ESMAPI;
}
