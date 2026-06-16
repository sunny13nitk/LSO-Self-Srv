package com.sap.cap.esmapi.utilities.srv.intf;

import java.util.List;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.pojos.TY_LKeyEMail;
import com.sap.cap.esmapi.utilities.pojos.TY_PFCTConfigResp;

public interface IF_APIHubSrv
{
    public List<TY_PFCTConfigResp> getPartners4LKeyandEmail(TY_LKeyEMail lkeyEmail) throws EX_ESMAPI;
}
