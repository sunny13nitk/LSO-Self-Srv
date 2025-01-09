package com.sap.cap.esmapi.utilities.pojos;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_DefaultComm 
{
    @JsonProperty("eMail")
    private String eMail;    
}
