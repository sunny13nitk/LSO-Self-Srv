package com.sap.cap.esmapi.utilities.srv.intf;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;

public interface IF_APISrv
{
    public long getNumberofEntitiesByUrl(String url) throws EX_ESMAPI, IOException;

    public List<JsonNode> getJsonNodesforUrl(String countsUrlName, String pagedUrlName)
            throws RuntimeException, IOException;
}
