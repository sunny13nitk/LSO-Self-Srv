package com.sap.cap.esmapi.catg.pojos;

import java.util.List;

import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_CatalogTree
{
    private EnumCaseTypes caseTypeEnum;
    private List<TY_CatalogItem> categories;
    private List<TY_CatalogItem> categorieslvl2;

    
}
