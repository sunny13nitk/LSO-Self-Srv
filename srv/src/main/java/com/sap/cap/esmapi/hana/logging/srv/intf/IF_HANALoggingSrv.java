package com.sap.cap.esmapi.hana.logging.srv.intf;

import java.util.List;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cds.Result;

import cds.gen.db.esmlogs.Esmappmsglog;

public interface IF_HANALoggingSrv
{
    // Create Log from Message POJO
    public Result createLog(TY_Message logMsg) throws EX_ESMAPI;

    public List<Esmappmsglog> getLogsByObjectIDs(List<String> objIDs) throws EX_ESMAPI;
}
