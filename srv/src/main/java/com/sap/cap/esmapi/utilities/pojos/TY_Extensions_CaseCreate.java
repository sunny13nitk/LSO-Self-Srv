package com.sap.cap.esmapi.utilities.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_Extensions_CaseCreate
{
    @JsonProperty("LSO_Country")
    private String LSO_Country;
    @JsonProperty("LSO_Language")
    private String LSO_Language;
}
