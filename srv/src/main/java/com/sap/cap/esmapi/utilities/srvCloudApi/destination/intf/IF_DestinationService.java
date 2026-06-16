package com.sap.cap.esmapi.utilities.srvCloudApi.destination.intf;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;

public interface IF_DestinationService
{
    public TY_DestinationProps getDestinationDetails4User(String DestinationName) throws EX_ESMAPI;

    public HttpDestination getDestination(String destinationName) throws EX_ESMAPI;

    public String getUrl(HttpDestination destination) throws EX_ESMAPI;

    public String getUsername(HttpDestination destination) throws EX_ESMAPI;

    public String getPassword(HttpDestination destination) throws EX_ESMAPI;

    public String getApiKey(HttpDestination destination) throws EX_ESMAPI;

}
