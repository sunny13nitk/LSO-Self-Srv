package com.sap.cap.esmapi.catg.pojos;

import com.opencsv.bean.CsvBindByPosition;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_Catg2RanksItem
{

    @CsvBindByPosition(position = 0)
    private EnumCaseTypes caseTypeEnum;
    @CsvBindByPosition(position = 1)
    private String catg1;
    @CsvBindByPosition(position = 2)
    private String catg2;
    @CsvBindByPosition(position = 3)
    private Integer rank;

}
