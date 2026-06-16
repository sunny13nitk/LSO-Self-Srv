package com.sap.cap.esmapi.utilities.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_PFCTConfigResp
{
    private String pfct;
    private String id;
    private String displayId;
    private String formattedName;
    private String jsonPath;
    private Boolean stopseek;
}