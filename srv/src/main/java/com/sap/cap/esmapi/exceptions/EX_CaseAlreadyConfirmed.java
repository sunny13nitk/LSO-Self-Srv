package com.sap.cap.esmapi.exceptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EX_CaseAlreadyConfirmed extends RuntimeException
{

    public EX_CaseAlreadyConfirmed(String message)
    {

        super(message);
        log.error(message);
    }

}
