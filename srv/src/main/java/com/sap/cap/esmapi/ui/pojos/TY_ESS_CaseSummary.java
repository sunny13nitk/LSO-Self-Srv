package com.sap.cap.esmapi.ui.pojos;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_ESS_CaseSummary
{
    private long totalCases;
    private long completedCases;
    private double perCompleted;    
}
