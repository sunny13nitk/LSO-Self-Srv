package com.sap.cap.esmapi.utilities.pojos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_UserESS
{
    private Ty_UserAccountEmployee userDetails;
    private List<TY_CaseESS>  cases;  
}
