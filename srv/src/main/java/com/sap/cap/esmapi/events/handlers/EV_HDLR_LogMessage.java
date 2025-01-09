package com.sap.cap.esmapi.events.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sap.cap.esmapi.events.event.EV_LogMessage;
import com.sap.cap.esmapi.hana.logging.srv.intf.IF_HANALoggingSrv;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EV_HDLR_LogMessage
{

    @Autowired
    private IF_HANALoggingSrv dbLogSrv;

    @EventListener
    public void handleLogMessage(EV_LogMessage evLogMessage)
    {
        if (evLogMessage != null && dbLogSrv != null)
        {
            log.info("Inside Message Logging Event Handler...");
            if (evLogMessage.getMessageToLog() != null)
            {
                if (dbLogSrv.createLog(evLogMessage.getMessageToLog()) != null)
                {
                    log.info("Message Logged in DB!");
                }
            }
        }

    }
}
