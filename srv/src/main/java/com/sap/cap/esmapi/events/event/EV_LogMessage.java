package com.sap.cap.esmapi.events.event;

import org.springframework.context.ApplicationEvent;

import com.sap.cap.esmapi.utilities.pojos.TY_Message;

import lombok.Getter;

/*
 * Event Raised whenever a Message Needs to be Persisted to HANADB Logger
 * To be Used in Admin Console
 */
@Getter
public class EV_LogMessage extends ApplicationEvent
{

    private TY_Message messageToLog;

    public EV_LogMessage(Object source, TY_Message message)
    {
        super(source);
        this.messageToLog = message;
    }

}
