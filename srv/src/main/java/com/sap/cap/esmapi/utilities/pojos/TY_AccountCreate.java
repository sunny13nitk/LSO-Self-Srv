package com.sap.cap.esmapi.utilities.pojos;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_AccountCreate 
{
    private String firstLineName;
    private String customerRole;
    private String lifeCycleStatus;
    private boolean isNaturalPerson; 
    private TY_DefaultComm defaultCommunication;
     
}
