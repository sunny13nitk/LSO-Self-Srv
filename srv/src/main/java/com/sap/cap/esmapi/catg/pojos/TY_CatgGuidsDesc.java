package com.sap.cap.esmapi.catg.pojos;


import com.opencsv.bean.CsvBindByPosition;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_CatgGuidsDesc
{
    @CsvBindByPosition(position = 0)
    private String lvl1guid;
    @CsvBindByPosition(position = 1)
    private String lvl2guid;
    @CsvBindByPosition(position = 2)
    private String description;    
}
