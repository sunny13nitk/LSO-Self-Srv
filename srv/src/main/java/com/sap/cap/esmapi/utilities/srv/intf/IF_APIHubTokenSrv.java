package com.sap.cap.esmapi.utilities.srv.intf;

import com.sap.cap.esmapi.ui.pojos.TY_TokenResponse;

public interface IF_APIHubTokenSrv
{
    public TY_TokenResponse getUserAccessToken(String destinationName);
}
