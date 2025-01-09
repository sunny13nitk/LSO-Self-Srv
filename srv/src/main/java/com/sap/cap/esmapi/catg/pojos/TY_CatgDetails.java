package com.sap.cap.esmapi.catg.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CatgDetails
{
    private String catgId;
    private String catDesc;
    private boolean root;
    private boolean inUpperCase;
}
