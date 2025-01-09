package com.sap.cap.esmapi.vhelps.srv.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatgDetails;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.vhelps.cus.TY_Catg_MandatoryFlds;
import com.sap.cap.esmapi.vhelps.cus.TY_Cus_VHelpsLOB;
import com.sap.cap.esmapi.vhelps.cus.TY_FieldProperties;
import com.sap.cap.esmapi.vhelps.cus.TY_VHelpsRoot;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;
import com.sap.cap.esmapi.vhelps.pojos.TY_MandatoryFlds_CatgsList;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_FilterDDLB4VHelp;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_VHelpLOBUIModelSrv;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_VHelpSrv;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CL_VHelpLOBUIModelSrv implements IF_VHelpLOBUIModelSrv
{

    @Autowired
    private IF_CatalogSrv catalogSrv;

    @Autowired
    private IF_VHelpSrv vHelpSrv;

    @Autowired
    private TY_VHelpsRoot vHelpCusSrv;

    @Autowired
    private ApplicationContext appCtxt;

    @Override
    public Map<String, List<TY_KeyValue>> getVHelpUIModelMap4LobCatg(EnumCaseTypes lob, String catgId) throws EX_ESMAPI
    {
        Map<String, List<TY_KeyValue>> modelAttrs = new HashMap<String, List<TY_KeyValue>>();

        if (StringUtils.hasText(lob.name()) && StringUtils.hasText(catgId) && catalogSrv != null && vHelpSrv != null
                && vHelpCusSrv != null && appCtxt != null)
        {

            // Get all RElevant Value help Fields for LoB
            if (CollectionUtils.isNotEmpty(vHelpCusSrv.getVHelpsCus()))
            {
                // Get the Category Description for the Category ID from Case Form
                TY_CatgDetails catgDetails = catalogSrv.getCategoryDetails4Catg(catgId, lob, true);
                // Get Customizing for LOB
                Optional<TY_Cus_VHelpsLOB> lobVHelpCusO = vHelpCusSrv.getVHelpsCus().stream()
                        .filter(x -> x.getLOB().equals(lob.name())).findFirst();

                if (lobVHelpCusO.isPresent())
                {
                    TY_Cus_VHelpsLOB lobVHelpCus = lobVHelpCusO.get();
                    // Iterate over fieldNames for Value Helps
                    for (TY_FieldProperties fldLob : lobVHelpCus.getFields())
                    {
                        if (StringUtils.hasText(fldLob.getFieldName()) && StringUtils.hasText(fldLob.getCatgListBean()))
                        {
                            // If Category specific - SCan for Category to match and Then Populate in
                            // Attributes Map
                            if (fldLob.isCatgSpecific())
                            {
                                if (StringUtils.hasText(catgDetails.getCatDesc()))
                                {
                                    // Scan for the Current Category Text in Field Category Text bean

                                    // Get Bean Instance for Field Category Texts
                                    TY_MandatoryFlds_CatgsList catgsList = (TY_MandatoryFlds_CatgsList) appCtxt
                                            .getBean(fldLob.getCatgListBean());
                                    if (catgsList != null)
                                    {
                                        if (CollectionUtils.isNotEmpty(catgsList.getCatgsList()))
                                        {
                                            // check if Current Category is in List of Enabling Categories for current
                                            // iteration field
                                            Optional<TY_Catg_MandatoryFlds> catgMandFldsO = catgsList.getCatgsList()
                                                    .stream()
                                                    .filter(e -> e.getCatgString().equals(catgDetails.getCatDesc()))
                                                    .findFirst();
                                            if (catgMandFldsO.isPresent())
                                            {
                                                // Get DDLB for field --should be enabled
                                                List<TY_KeyValue> vHlpDDLB = vHelpSrv.getVHelpDDLB4Field(lob,
                                                        fldLob.getFieldName());

                                                // Check if Filter is specified on the Srv Cloud Obtained Value Helps
                                                if (StringUtils.hasText(fldLob.getFltListBean()))
                                                {
                                                    switch (fldLob.fieldName)
                                                    {
                                                    case GC_Constants.gc_LSO_COUNTRY: // For LSO_Country filter criteria
                                                                                      // is 'Category'
                                                        Object[] criteria = new Object[]
                                                        { catgDetails.getCatDesc() };
                                                        // Get Bean Instance for Filtering DDLB Vals
                                                        IF_FilterDDLB4VHelp ddlbFltBean = (IF_FilterDDLB4VHelp) appCtxt
                                                                .getBean(fldLob.getFltListBean());
                                                        if (ddlbFltBean != null)
                                                        {
                                                            // Filter the Srv Cloud DDLB as per criteria
                                                            vHlpDDLB = ddlbFltBean.filterValueHelpbyCriteria(criteria,
                                                                    vHlpDDLB);
                                                        }

                                                        break;

                                                    default:
                                                        break;
                                                    }
                                                }

                                                /*
                                                 * Only Add Blank Row if not present at top
                                                 */

                                                if (StringUtils.hasText(vHlpDDLB.get(0).getKey()))
                                                {
                                                    vHlpDDLB.add(0, new TY_KeyValue());
                                                }

                                                // Append to Model Map
                                                if (CollectionUtils.isNotEmpty(vHlpDDLB))
                                                {
                                                    modelAttrs.put(fldLob.getFieldName(), vHlpDDLB);
                                                }
                                            }
                                        }
                                    }

                                }
                            }

                            // Not Category specific - Get Vhelp and Populate in Attributes Map
                            else
                            {
                                // Get DDLB for field --should be enabled
                                List<TY_KeyValue> vHlpDDLB = vHelpSrv.getVHelpDDLB4Field(lob, fldLob.getFieldName());

                                // Append to Model Map
                                if (CollectionUtils.isNotEmpty(vHlpDDLB))
                                {
                                    modelAttrs.put(fldLob.getFieldName(), vHlpDDLB);
                                }
                            }
                        }
                    }

                }

            }

        }

        return modelAttrs;

    }

}
