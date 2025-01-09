package com.sap.cap.esmapi.hana.logging.srv.impl;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.hana.logging.srv.intf.IF_HANALoggingSrv;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.db.esmlogs.Esmappmsglog;
import cds.gen.db.esmlogs.Esmappmsglog_;
import lombok.extern.slf4j.Slf4j;

@Service
@Scope("prototype")
@Slf4j
public class CL_HANALoggingSrv implements IF_HANALoggingSrv
{

    @Autowired
    private PersistenceService ps;

    @Autowired
    private Environment env;

    @Autowired
    private MessageSource msgSrc;

    private final String msgLogsTablePath = "db.esmlogs.esmappmsglog"; // Table Path - HANA
    private final String objectID = "objectid";

    @Override
    public Result createLog(TY_Message logMsg) throws EX_ESMAPI
    {
        Result response = null;
        if (logMsg != null && msgSrc != null && ps != null)
        {
            String msg = msgSrc.getMessage("PERS_LOG", new Object[]
            { logMsg.getUserName(), logMsg.getMsgType().toString() }, Locale.ENGLISH);
            if (StringUtils.hasText(msg))
            {
                log.info(msg);
            }
            // Only if Application is Running in BTP with one of the active profile(s) as
            // btp - check in application.properties
            if (Arrays.asList(env.getActiveProfiles()).stream().filter(e -> e.equals(GC_Constants.gc_BTPProfile))
                    .findFirst().isPresent())
            {
                Map<String, Object> logEntity = new HashMap<>();
                logEntity.put("ID", UUID.randomUUID()); // ID
                logEntity.put("username", logMsg.getUserName()); // User Name
                logEntity.put("timestamp", new Timestamp(System.currentTimeMillis())); // TimeStamp
                logEntity.put("status", logMsg.getStatus().toString()); // Status
                logEntity.put("msgtype", logMsg.getMsgType().toString()); // Message Type
                logEntity.put("objectid", logMsg.getObjectId()); // Object ID
                logEntity.put("message", logMsg.getMessage()); // Message Text

                if (logEntity != null)
                {
                    CqnInsert qLogInsert = Insert.into(this.msgLogsTablePath).entry(logEntity);
                    if (qLogInsert != null)
                    {
                        log.info("LOG Insert Query Bound!");
                        Result result = ps.run(qLogInsert);
                        if (result.list().size() > 0)
                        {
                            log.info("# Log Successfully Inserted - " + result.rowCount());
                            response = result;

                        }
                    }
                }
            }
        }

        return response;
    }

    @Override
    public List<Esmappmsglog> getLogsByObjectIDs(List<String> objIDs) throws EX_ESMAPI
    {
        List<Esmappmsglog> logs = null;

        // Only if Application is Running in BTP with one of the active profile(s) as
        // btp - check in application.properties
        if (Arrays.asList(env.getActiveProfiles()).stream().filter(e -> e.equals(GC_Constants.gc_BTPProfile))
                .findFirst().isPresent())
        {
            if (CollectionUtils.isNotEmpty(objIDs) && ps != null)
            {
                CqnSelect qLogsByObjectId = Select.from(Esmappmsglog_.class).where(l -> l.get(objectID).in(objIDs));
                if (qLogsByObjectId != null)
                {
                    logs = ps.run(qLogsByObjectId).listOf(Esmappmsglog.class);
                }

            }
        }

        return logs;
    }

}
