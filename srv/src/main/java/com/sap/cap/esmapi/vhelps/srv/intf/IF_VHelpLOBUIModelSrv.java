package com.sap.cap.esmapi.vhelps.srv.intf;

import java.util.List;
import java.util.Map;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;

public interface IF_VHelpLOBUIModelSrv
{
    public Map<String, List<TY_KeyValue>> getVHelpUIModelMap4LobCatg(EnumCaseTypes lob, String catgId) throws EX_ESMAPI;
}
