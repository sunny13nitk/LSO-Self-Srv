package com.sap.cap.esmapi.vhelps.cus;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_FieldProperties
{
    @JsonProperty("fieldName")
    public String fieldName;
    @JsonProperty("catgSpecific")
    public boolean catgSpecific;
    @JsonProperty("catgListBean")
    public String catgListBean;
    @JsonProperty("fltListBean")
    public String fltListBean;

}
