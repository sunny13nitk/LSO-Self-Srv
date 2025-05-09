package com.sap.cap.esmapi.events.event;

import org.springframework.context.ApplicationEvent;

import com.sap.cap.esmapi.ui.pojos.TY_CaseEditFormAsync;

import lombok.Getter;

@Getter
public class EV_CaseReplySubmit extends ApplicationEvent
{
    private TY_CaseEditFormAsync payload;

    public EV_CaseReplySubmit(Object source, TY_CaseEditFormAsync payload)
    {
        super(source);
        this.payload = payload;

    }

}
