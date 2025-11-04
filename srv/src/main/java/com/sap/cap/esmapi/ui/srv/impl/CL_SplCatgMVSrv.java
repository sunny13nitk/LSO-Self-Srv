package com.sap.cap.esmapi.ui.srv.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.ui.srv.intf.IF_SplCatgMVSrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("SRV_EXAM_REQUEST")
@RequiredArgsConstructor
@Slf4j
public class CL_SplCatgMVSrv implements IF_SplCatgMVSrv
{

    private final IF_UserSessionSrv userSessionSrv;

    @Override
    public ModelAndView getSplCatgModelAndView(TY_Case_Form caseForm) throws EX_ESMAPI
    {
        log.info("Inside CL_SplCatgMVSrv for special category SAP Certification special accomodation request");
        return null;
    }

}
