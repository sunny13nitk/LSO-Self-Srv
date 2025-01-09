package com.sap.cap.esmapi.vhelps.srv.intf;

import java.util.List;

import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;

public interface IF_FilterDDLB4VHelp
{
    public List<TY_KeyValue> filterValueHelpbyCriteria(Object[] criteria, List<TY_KeyValue> ddlbVals);
}
