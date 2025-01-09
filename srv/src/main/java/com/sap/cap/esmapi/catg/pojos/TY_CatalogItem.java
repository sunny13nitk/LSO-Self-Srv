package com.sap.cap.esmapi.catg.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_CatalogItem 
{
    private String id;
    private String name;
    private String parentId;
    private String parentName;
}
