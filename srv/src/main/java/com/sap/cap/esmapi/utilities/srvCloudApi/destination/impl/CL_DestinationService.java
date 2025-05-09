package com.sap.cap.esmapi.utilities.srvCloudApi.destination.impl;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.intf.IF_DestinationService;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.exception.DestinationAccessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile(GC_Constants.gc_BTPProfile)
public class CL_DestinationService implements IF_DestinationService
{

    private final MessageSource msgSrc;

    private static final String prop_URL = "URL";
    private static final String prop_Token = "cloudsdk.authTokens";
    private static final String cons_value = ", value=";
    private static final String cons_bracketClose = "\\)";

    @Override
    public TY_DestinationProps getDestinationDetails4User(String DestinationName) throws EX_ESMAPI
    {

        return getDestinationDetails(DestinationName);

    }

    private TY_DestinationProps getDestinationDetails(String destinationName)
    {
        TY_DestinationProps desProps = null;
        try
        {

            log.info("Scanning for Destination : " + destinationName);
            Destination dest = DestinationAccessor.getDestination(destinationName);
            if (dest != null)
            {

                log.info("Destination Bound via Destination Accessor. Details....");

                desProps = new TY_DestinationProps();

                for (String prop : dest.getPropertyNames())
                {

                    if (prop.equals(prop_URL))
                    {
                        desProps.setBaseUrl(dest.get(prop).get().toString());
                    }

                    if (prop.equals(prop_Token))
                    {
                        desProps.setAuthToken(parseToken(dest.get(prop).get().toString()));
                    }

                }

            }
        }
        catch (DestinationAccessException e)
        {
            log.error("Error Accessing Destination : " + e.getLocalizedMessage());
            String msg = msgSrc.getMessage("ERR_DESTINATION_ACCESS", new Object[]
            { destinationName, e.getLocalizedMessage() }, Locale.ENGLISH);
            throw new EX_ESMAPI(msg);

        }

        return desProps;
    }

    private String parseToken(String authToken)
    {
        String token = null;

        if (StringUtils.hasText(authToken))
        {
            String[] tokens = authToken.split(cons_value);
            if (tokens.length > 0)
            {
                String tokenval = tokens[tokens.length - 1];
                if (StringUtils.hasText(tokenval))
                {
                    String[] tokenAuth = tokenval.split(cons_bracketClose);
                    if (tokenAuth.length > 0)
                    {
                        token = tokenAuth[0];
                    }
                }
            }
        }

        return token;
    }

}
