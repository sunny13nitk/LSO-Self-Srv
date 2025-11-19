package com.sap.cap.esmapi.ui.srv.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgDetails;
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

    private final IF_CatalogSrv catalogSrv;

    private final String gc_desc = "SAP Certification Special Accomodation Request";

    @Override
    public ModelAndView getSplCatgModelAndView(TY_Case_Form caseForm, boolean... notiGgnoreErrors) throws EX_ESMAPI
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

            if (notiGgnoreErrors != null && notiGgnoreErrors.length > 0 && notiGgnoreErrors[0])
            {
                // Populate any Form Errors from Session
                if (CollectionUtils.isNotEmpty(userSessSrv.getFormErrors()))
                {
                    log.info("Form Errors present in session...");
                    List<String> errorsCaseForm = new ArrayList<String>();
                    errorsCaseForm.addAll(userSessSrv.getFormErrors());
                    modelVw.addObject("formErrors", errorsCaseForm);
                }
            }

            // clear Form errors on each refresh or a New Case form request
            if (CollectionUtils.isNotEmpty(userSessSrv.getFormErrors()))
            {
                userSessSrv.clearFormErrors();
            }

            // Also set the Category Description in Upper Case
            // Get the Category Description for the Category ID from Case Form
            TY_CatgDetails catgDetails = catalogSrv.getCategoryDetails4Catg(caseForm.getCatgDesc(),
                    EnumCaseTypes.Learning, true);
            if (catgDetails != null)
            {
                caseForm.setCatgText(catgDetails.getCatDesc());
                log.info(
                        "Catg. Text for Category ID : " + caseForm.getCatgDesc() + " is : " + catgDetails.getCatDesc());
            }

            // also Upload the Catg. Tree as per Case Type
            modelVw.addObject("catgsList", catalogTreeSrv.getCaseCatgTree4LoB(EnumCaseTypes.Learning).getCategories());

            caseForm.setTemplate(null);
            caseForm.setSubject(gc_desc);

            modelVw.addObject("caseForm", caseForm);

        }

        return modelVw;
    }

}
