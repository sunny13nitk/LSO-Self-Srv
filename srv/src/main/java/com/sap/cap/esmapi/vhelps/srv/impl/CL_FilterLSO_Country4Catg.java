package com.sap.cap.esmapi.vhelps.srv.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.vhelps.pojos.TY_CatgCountryMapping;
import com.sap.cap.esmapi.vhelps.pojos.TY_CatgCountryMappingList;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_FilterDDLB4VHelp;

@Service("LSO_CountryByCatgFilter")
public class CL_FilterLSO_Country4Catg implements IF_FilterDDLB4VHelp
{

    @Autowired
    private TY_CatgCountryMappingList catgCountryList;

    @Override
    public List<TY_KeyValue> filterValueHelpbyCriteria(Object[] criteria, List<TY_KeyValue> ddlbVals)
    {
        if (criteria == null)
        {
            return ddlbVals;
        }
        else
        {
            List<TY_KeyValue> fltdVals = null;
            if (StringUtils.hasText(criteria[0].toString()) && catgCountryList != null
                    && CollectionUtils.isNotEmpty(ddlbVals))
            {
                // Grab the Current Category from Criteria
                String catgCurr = criteria[0].toString().toUpperCase();
                if (StringUtils.hasText(catgCurr)
                        && CollectionUtils.isNotEmpty(catgCountryList.getCatgCountryMappingsList()))
                {
                    // Get List of Countries for Current Category
                    List<TY_CatgCountryMapping> currCatgCountries = catgCountryList.getCatgCountryMappingsList()
                            .stream().filter(e -> e.getCategoryUC().equals(catgCurr)).collect(Collectors.toList());

                    // Check if each thus obtained exists in ddlbVals
                    if (CollectionUtils.isNotEmpty(currCatgCountries))
                    {
                        List<TY_CatgCountryMapping> validCountries4Catg = currCatgCountries.stream()
                                .filter(e -> ddlbVals.stream().anyMatch(ddlb -> ddlb.getKey().equals(e.getCountry())))
                                .collect(Collectors.toList());

                        if (CollectionUtils.isNotEmpty(validCountries4Catg))
                        {
                            fltdVals = new ArrayList<TY_KeyValue>();
                            for (TY_CatgCountryMapping coun : validCountries4Catg)
                            {
                                TY_KeyValue keyValue = new TY_KeyValue();
                                keyValue.setKey(coun.getCountry());
                                Optional<TY_KeyValue> keyValO = ddlbVals.stream()
                                        .filter(d -> d.getKey().equals(coun.getCountry())).findFirst();
                                if (keyValO.isPresent())
                                {
                                    keyValue.setValue(keyValO.get().getValue());
                                    // --- If So Append to fltdVals
                                    fltdVals.add(keyValue);
                                }
                            }
                        }

                    }

                }

            }

            return fltdVals;
        }
    }

}
