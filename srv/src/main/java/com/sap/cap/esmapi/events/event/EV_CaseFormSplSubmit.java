package com.sap.cap.esmapi.events.event;

import org.springframework.context.ApplicationEvent;

import com.sap.cap.esmapi.ui.pojos.TY_CaseFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_SplCatg_Seek;

import lombok.Getter;

@Getter
public class EV_CaseFormSplSubmit extends ApplicationEvent
{
    private TY_CaseFormAsync payload;

    private TY_SplCatg_Seek splCatgSeek;

    public EV_CaseFormSplSubmit(Object source, TY_CaseFormAsync payload, TY_SplCatg_Seek splCatgSeek)
    {
        super(source);
        this.payload = payload;
        this.splCatgSeek = splCatgSeek;

    }

}
