package com.sap.cap.esmapi.ui.srv.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.ui.srv.intf.IF_SplCatgMVSrv;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("SRV_EXAM_REQUEST")
@RequiredArgsConstructor
@Slf4j
public class CL_SplCatgMVSrv implements IF_SplCatgMVSrv
{

    private final IF_UserSessionSrv userSessSrv;

    private final TY_CatgCus catgCusSrv;

    private final IF_CatalogSrv catalogTreeSrv;

    private final String gc_desc = "SAP Certification Special Accomodation Request";

    @Override
    public ModelAndView getSplCatgModelAndView(TY_Case_Form caseForm) throws EX_ESMAPI
    {
        log.info("Inside CL_SplCatgMVSrv for special category SAP Certification special accomodation request");
        ModelAndView modelVw = new ModelAndView("caseFormExamExtensions");
        // Prepare the model for Case form
        if ((StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
        {
            userSessSrv.setCaseFormB4Submission(null);

            modelVw.addObject("caseTypeStr", EnumCaseTypes.Learning.toString());

            // Populate User Details
            TY_UserESS userDetails = new TY_UserESS();
            userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
            modelVw.addObject("userInfo", userDetails);

            // clear Form errors on each refresh or a New Case form request
            if (CollectionUtils.isNotEmpty(userSessSrv.getFormErrors()))
            {
                userSessSrv.clearFormErrors();
            }

            // also Upload the Catg. Tree as per Case Type
            modelVw.addObject("catgsList", catalogTreeSrv.getCaseCatgTree4LoB(EnumCaseTypes.Learning).getCategories());

            caseForm.setTemplate(null);
            caseForm.setDescription(gc_desc);

            modelVw.addObject("caseForm", caseForm);

        }

        return modelVw;
    }

}
