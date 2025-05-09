package com.sap.cap.esmapi.events.event;

import org.springframework.context.ApplicationEvent;

import com.sap.cap.esmapi.ui.pojos.TY_CaseConfirmPOJO;

import lombok.Getter;

@Getter
public class EV_CaseConfirmSubmit extends ApplicationEvent
{

    private TY_CaseConfirmPOJO payload;

    public EV_CaseConfirmSubmit(Object source, TY_CaseConfirmPOJO payload)
    {

        super(source);
        this.payload = payload;
    }

}
