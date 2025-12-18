package com.sap.cap.esmapi.utilities.pojos;

import java.time.OffsetDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_CaseESS
{
    private String guid;
    private String id;
    private String caseType;
    private String caseTypeDescription;
    private String subject;
    private String statusDesc;
    private String accountId;
    private String employeeId;
    private String createdOn;
    private Date creationDate;
    private String formattedDate;
    private OffsetDateTime tsCreate;
    private String updatedOn;
    private Date updationDate;
    private String formattedUpdateDate;
    private OffsetDateTime tsUpdate;
    private String origin;
    private boolean confirmAllowed;
}
