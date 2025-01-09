package com.sap.cap.esmapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class EX_SessionExpired extends RuntimeException
{
    public EX_SessionExpired(String message)
    {

        super(message);
        log.error(message);
    }
}
