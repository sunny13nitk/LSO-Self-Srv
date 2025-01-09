package com.sap.cap.esmapi.utilities.srv.intf;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cloud.security.xsuaa.token.Token;

public interface IF_UserAPISrv
{

    public Ty_UserAccountEmployee getUserDetails(@AuthenticationPrincipal Token token) throws EX_ESMAPI;

    public TY_UserESS getESSDetails(@AuthenticationPrincipal Token token) throws EX_ESMAPI;

    public String createAccount() throws EX_ESMAPI;

    public Ty_UserAccountEmployee getUserDetails4mSession();

    public void addSessionMessage(String msg);

    public List<String> getSessionMessages();

    // Temporary Method
    public void setUserAccount(Ty_UserAccountEmployee userDetails);

}
