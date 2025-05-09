package com.sap.cap.esmapi.utilities.srvCloudApi.destination.URLUtility;

import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;

public class CL_URLUtility
{

    private static final String cons_pathSlash = "\\/";
    private static final int cons_limit = 2;

    public static String getUrl4DestinationAPI(String destinationAPI, String destUrlPrefix) throws EX_ESMAPI
    {
        String url = null;
        if (StringUtils.hasText(destUrlPrefix) && StringUtils.hasText(destinationAPI))
        {
            url = new String();
            url += destUrlPrefix + destinationAPI;
            // String[] destAPIParts = destinationAPI.split(cons_pathSlash, cons_limit);
            // if (destAPIParts.length > 0)
            // {
            // url += destUrlPrefix + destAPIParts[destAPIParts.length - 1];
            // }
        }

        return url;
    }
}
