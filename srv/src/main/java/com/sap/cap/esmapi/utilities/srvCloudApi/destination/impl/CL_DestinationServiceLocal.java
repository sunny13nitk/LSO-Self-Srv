package com.sap.cap.esmapi.utilities.srvCloudApi.destination.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.pojos.TY_SrvCloudUrls;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.intf.IF_DestinationService;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile(GC_Constants.gc_LocalProfile)
public class CL_DestinationServiceLocal implements IF_DestinationService
{

    private final TY_SrvCloudUrls srvCloudUrls;

    private TY_DestinationProps destinationProps;

    private static final String prop_URL = "URL";
    private static final String prop_Token = "authTokens";
    private static final String cons_value = ", value=";
    private static final String cons_bracketClose = "\\)";

    @Override
    public TY_DestinationProps getDestinationDetails4User(String DestinationName) throws EX_ESMAPI
    {
        log.info("Destination loaded for Local Testing");
        this.destinationProps = new TY_DestinationProps(srvCloudUrls.getBaseUrl(), srvCloudUrls.getToken());
        return this.destinationProps;
    }

    @Override
    public HttpDestination getDestination(String destinationName) throws EX_ESMAPI
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDestination'");
    }

    @Override
    public String getUrl(HttpDestination destination) throws EX_ESMAPI
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUrl'");
    }

    @Override
    public String getUsername(HttpDestination destination) throws EX_ESMAPI
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUsername'");
    }

    @Override
    public String getPassword(HttpDestination destination) throws EX_ESMAPI
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPassword'");
    }

    @Override
    public String getApiKey(HttpDestination destination) throws EX_ESMAPI
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getApiKey'");
    }

}
