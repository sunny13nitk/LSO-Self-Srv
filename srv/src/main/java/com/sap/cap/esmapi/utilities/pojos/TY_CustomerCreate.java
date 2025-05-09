package com.sap.cap.esmapi.utilities.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_CustomerCreate
{
    private String givenName;
    private String familyName;
    private String customerRole;
    private String lifeCycleStatus;
    private TY_DefaultComm defaultCommunication;

}
