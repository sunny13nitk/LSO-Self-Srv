package com.sap.cap.esmapi.utilities.srv.intf;

import java.io.IOException;
import java.util.List;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.pojos.TY_PreviousAttachments;

public interface IF_AttachmentsFetchSrv
{
    public List<TY_PreviousAttachments> getAttachments4CaseByCaseGuid(String caseGuid) throws EX_ESMAPI, IOException;
}
