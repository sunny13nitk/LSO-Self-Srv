package com.sap.cap.esmapi.vhelps.srv.intf;

import java.util.List;

import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;

public interface IF_VHelpSrv
{
    public List<TY_KeyValue> getVHelpDDLB4Field(EnumCaseTypes lob, String FieldName);
}
