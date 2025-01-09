package com.sap.cap.esmapi.utilities.uimodel.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.uimodel.intf.IF_CountryLanguageVHelpAdj;
import com.sap.cap.esmapi.vhelps.pojos.TY_CountryLangaugeMapping;
import com.sap.cap.esmapi.vhelps.pojos.TY_CountryLangaugeMappingsList;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CL_CountryLanguageVHelpAdj implements IF_CountryLanguageVHelpAdj
{
    @Autowired
    private TY_CountryLangaugeMappingsList countryLanguMappings;

    @Autowired
    private MessageSource msgSrc;

    @Override
    public Map<String, List<TY_KeyValue>> adjustCountryLanguageDDLB(String countryValue,
            Map<String, List<TY_KeyValue>> ddlbs) throws EX_ESMAPI
    {
        Map<String, List<TY_KeyValue>> adjDDLBs = ddlbs;
        List<String> languKeys = null;
        if (StringUtils.hasText(countryValue) && ddlbs.size() >= 2)
        {
            // Both Country and Language DDLB bound
            if ((ddlbs.get(GC_Constants.gc_LSO_COUNTRY) != null) && (ddlbs.get(GC_Constants.gc_LSO_LANGUAGE) != null))
            {
                log.info("Adjusting Dropdown Values for Language for Country : " + countryValue);
                if (CollectionUtils.isNotEmpty(countryLanguMappings.getCountryLanguageMappingsList()))
                {
                    // Get Language Keys for Country in Params
                    Optional<TY_CountryLangaugeMapping> currCountryLanguKeysO = countryLanguMappings
                            .getCountryLanguageMappingsList().stream().filter(c -> c.getCountry().equals(countryValue))
                            .findFirst();
                    if (currCountryLanguKeysO.isPresent())
                    {
                        languKeys = new ArrayList<String>();
                        if (StringUtils.hasText(currCountryLanguKeysO.get().getLangu1()))
                        {
                            languKeys.add(currCountryLanguKeysO.get().getLangu1());
                        }

                        if (StringUtils.hasText(currCountryLanguKeysO.get().getLangu2()))
                        {
                            languKeys.add(currCountryLanguKeysO.get().getLangu2());
                        }

                        if (StringUtils.hasText(currCountryLanguKeysO.get().getLangu3()))
                        {
                            languKeys.add(currCountryLanguKeysO.get().getLangu3());
                        }

                        // Copy Langu DDLBfrom Params
                        List<TY_KeyValue> languDDLBCP = ddlbs.get(GC_Constants.gc_LSO_LANGUAGE);
                        // REmove balank Entity from Top if Any
                        if (languDDLBCP.get(0).getKey() == null)
                        {
                            languDDLBCP.remove(0);
                        }

                        List<TY_KeyValue> languDDLBExp = new ArrayList<TY_KeyValue>();
                        for (String languKey : languKeys)
                        {
                            Optional<TY_KeyValue> languKeyValO = languDDLBCP.stream()
                                    .filter(e -> e.getKey().equals(languKey)).findFirst();
                            if (languKeyValO.isPresent())
                            {
                                languDDLBExp.add(new TY_KeyValue(languKey, languKeyValO.get().getValue()));
                            }
                            else
                            {

                                // Default to English Language and supress the exception if no mapping for
                                // Country for language found
                                languDDLBExp.add(new TY_KeyValue(GC_Constants.gc_LANGU_Default,
                                        GC_Constants.gc_LANGU_Default_DESC));
                                // throw new EX_ESMAPI(msgSrc.getMessage("ERR_INVALID_LANGU", new Object[]
                                // { languKey, countryValue }, Locale.ENGLISH));

                            }
                        }

                        if (CollectionUtils.isNotEmpty(languDDLBExp))
                        {
                            Map<String, List<TY_KeyValue>> expDDLB = new HashMap<>();
                            expDDLB.put(GC_Constants.gc_LSO_COUNTRY, ddlbs.get(GC_Constants.gc_LSO_COUNTRY));
                            languDDLBExp.add(new TY_KeyValue(null, null)); // Blank Value at Top
                            expDDLB.put(GC_Constants.gc_LSO_LANGUAGE, languDDLBExp);
                            return expDDLB;
                        }

                    }
                    else
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_INVALID_COUNTRY", new Object[]
                        { countryValue }, Locale.ENGLISH));
                    }
                }
            }

        }

        return adjDDLBs;
    }
}
