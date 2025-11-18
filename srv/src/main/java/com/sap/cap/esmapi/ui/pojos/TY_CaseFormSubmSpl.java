package com.sap.cap.esmapi.ui.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CaseFormSubmSpl
{
    private TY_CaseFormAsync caseFormAsync;
    private boolean isValid;
}
