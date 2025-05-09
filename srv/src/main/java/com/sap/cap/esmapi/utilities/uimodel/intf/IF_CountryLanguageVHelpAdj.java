package com.sap.cap.esmapi.utilities.uimodel.intf;

import java.util.List;
import java.util.Map;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;

public interface IF_CountryLanguageVHelpAdj
{
    public Map<String, List<TY_KeyValue>> adjustCountryLanguageDDLB(String countryValue,
            Map<String, List<TY_KeyValue>> ddlbs) throws EX_ESMAPI;
}
