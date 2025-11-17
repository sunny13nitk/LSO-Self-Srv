package com.sap.cap.esmapi.ui.pojos;

import com.sap.cap.esmapi.catg.pojos.TY_SplCatg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_SplCatg_Seek
{
    private TY_SplCatg splCatgCus;
    private boolean isFound;
}
