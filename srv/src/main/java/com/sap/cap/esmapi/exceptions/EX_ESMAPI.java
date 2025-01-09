package com.sap.cap.esmapi.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class EX_ESMAPI extends RuntimeException
{

    public EX_ESMAPI(String message)
    {
        
        super(message);
        log.error(message);
    }

   

   
}

