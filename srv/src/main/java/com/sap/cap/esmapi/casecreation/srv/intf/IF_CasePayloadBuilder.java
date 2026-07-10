package com.sap.cap.esmapi.casecreation.srv.intf;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.cap.esmapi.casecreation.pojos.TY_CaseContext;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;

public interface IF_CasePayloadBuilder
{
    ObjectNode build(TY_CaseContext context) throws EX_ESMAPI;
}