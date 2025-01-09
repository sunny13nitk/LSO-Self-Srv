package com.sap.cap.esmapi.vhelps.cus;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_VHelpsRoot
{
    @JsonProperty("VHelpFields")
    public List<TY_Cus_VHelpsLOB> vHelpsCus;
}
