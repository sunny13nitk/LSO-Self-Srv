package com.sap.cap.esmapi.utilities.srv.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseESS;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserAPISrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;
import com.sap.cloud.security.xsuaa.token.Token;

import lombok.extern.slf4j.Slf4j;

@Service
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Slf4j
public class CL_UserAPISrv implements IF_UserAPISrv
{
    private Ty_UserAccountEmployee userData;

    private List<String> sessionMessages;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private IF_SrvCloudAPI srvCloudApiSrv;

    @Autowired
    private IF_UserSessionSrv userSessionSrv;

    @Override
    public Ty_UserAccountEmployee getUserDetails(@AuthenticationPrincipal Token token) throws EX_ESMAPI
    {
        if (token == null)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("NO_TOKEN", null, Locale.ENGLISH));
        }

        else
        {
            // Return from Session if Populated else make some effort
            if (userData == null)
            {
                // Fetch and Return
                this.userData = new Ty_UserAccountEmployee();
                userData.setUserId(token.getLogonName());
                userData.setUserName(token.getGivenName() + " " + token.getFamilyName());
                userData.setUserEmail(token.getEmail());
                userData.setAccountId(srvCloudApiSrv.getAccountIdByUserEmail(userData.getUserEmail(),
                        userSessionSrv.getDestinationDetails4mUserSession()));
                log.info(userData.toString());

            }
        }
        return userData;

    }

    @Override
    public TY_UserESS getESSDetails(@AuthenticationPrincipal Token token) throws EX_ESMAPI
    {
        TY_UserESS userDetails = new TY_UserESS();

        // 1. Get User's Details
        userDetails.setUserDetails(this.getUserDetails(token));

        // 2.a. Account Identified - Show tickets
        if (userDetails.getUserDetails() != null)
        {
            if (StringUtils.hasText(userDetails.getUserDetails().getAccountId()))
            {
                // Get All Cases for the User
                // Account ID as Account
                // OR
                // Contact ID as reporter

                try
                {
                    // First Clear Any if in Session
                    userDetails.setCases(null);
                    // Fetch Afresh
                    userDetails.setCases(getCases4User());
                }
                catch (IOException e)
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASES_USER", new Object[]
                    { userData.getUserId(), e.getLocalizedMessage() }, Locale.ENGLISH));
                }

            }
            else
            {
                // 2.b. Account Not Identified - Create Account and Update Session
                // this.userData.setAccountId(createAccount());

                /*
                 * ---- Would be expliciltly handled while creating a Case via createAccount()
                 * call
                 */
            }
        }

        return userDetails;
    }

    @Override
    public String createAccount() throws EX_ESMAPI
    {
        String accountId = null;
        // User Email and UserName Bound
        if (StringUtils.hasText(userData.getUserEmail()) && StringUtils.hasText(userData.getUserName()))
        {

            accountId = srvCloudApiSrv.createAccount(userData.getUserEmail(), userData.getUserName(),
                    userSessionSrv.getDestinationDetails4mUserSession());
            // Also update in the session for newly created Account
            if (StringUtils.hasText(accountId))
            {
                userData.setAccountId(accountId);
                this.addSessionMessage(msgSrc.getMessage("NEW_AC", new Object[]
                { userData.getUserId() }, Locale.ENGLISH));
            }

        }
        return accountId;
    }

    @Override
    public Ty_UserAccountEmployee getUserDetails4mSession()
    {
        return this.userData;
    }

    @Override
    public void addSessionMessage(String msg)
    {
        if (CollectionUtils.isEmpty(this.sessionMessages))
        {
            this.sessionMessages = new ArrayList<String>();
        }

        if (StringUtils.hasText(msg))
        {
            sessionMessages.add(msg);
        }

    }

    @Override
    public List<String> getSessionMessages()
    {
        return this.sessionMessages;
    }

    // Temporary Method - To be deleted later
    @Override
    public void setUserAccount(Ty_UserAccountEmployee userDetails)
    {
        this.userData = userDetails;
    }

    private List<TY_CaseESS> getCases4User() throws IOException
    {
        return srvCloudApiSrv.getCases4User(userData.getAccountId(),
                userSessionSrv.getDestinationDetails4mUserSession());
    }

}
