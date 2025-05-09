package com.sap.cap.esmapi.utilities.pojos;

import java.sql.Timestamp;

import org.apache.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TY_FormResponse
{
    private String submGuid;
    private String userId;
    private Timestamp timestamp;
    private boolean valid; // To be set post Payload Validation
    private TY_Message message;
    private HttpStatus apiStatus;
    private String caseId;

}
