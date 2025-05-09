package com.sap.cap.esmapi.ui.pojos;

import java.util.ArrayList;
import java.util.List;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_ESS_Stats 
{
    private TY_ESS_CaseSummary caseSummary = new TY_ESS_CaseSummary();
    private List<TY_NameValueLPair> lobSpread = new ArrayList<TY_NameValueLPair>();
    private List<TY_NameValueLPair> statusSpread = new ArrayList<TY_NameValueLPair>();    
}
