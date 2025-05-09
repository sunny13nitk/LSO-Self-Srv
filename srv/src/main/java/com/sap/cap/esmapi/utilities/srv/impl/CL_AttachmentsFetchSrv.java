package com.sap.cap.esmapi.utilities.srv.impl;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.pojos.TY_DestinationsSuffix;
import com.sap.cap.esmapi.utilities.pojos.TY_PreviousAttachments;
import com.sap.cap.esmapi.utilities.pojos.TY_SrvCloudUrls;
import com.sap.cap.esmapi.utilities.srv.intf.IF_AttachmentsFetchSrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CL_AttachmentsFetchSrv implements IF_AttachmentsFetchSrv
{

    private final TY_DestinationsSuffix dS;
    private final IF_SrvCloudAPI srvCloudAPI;
    private final IF_UserSessionSrv userSessionSrv;

    @Override
    public List<TY_PreviousAttachments> getAttachments4CaseByCaseGuid(String caseGuid) throws EX_ESMAPI, IOException
    {

        List<TY_PreviousAttachments> prevAtt = null;
        if (StringUtils.hasText(caseGuid) && StringUtils.hasText(dS.getDlAttPathString())
                && StringUtils.hasText(dS.getPrevAttPathString()) && srvCloudAPI != null)

        {
            prevAtt = srvCloudAPI.getAttachments4Case(caseGuid, userSessionSrv.getDestinationDetails4mUserSession());
        }

        return prevAtt;
    }

}
