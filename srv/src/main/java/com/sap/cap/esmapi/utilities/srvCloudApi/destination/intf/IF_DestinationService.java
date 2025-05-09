package com.sap.cap.esmapi.utilities.srvCloudApi.destination.intf;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;

public interface IF_DestinationService
{
    public TY_DestinationProps getDestinationDetails4User(String DestinationName) throws EX_ESMAPI;
}
