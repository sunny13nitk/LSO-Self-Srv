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
public class TY_Catg2Templates
{
    @CsvBindByPosition(position = 0)
    private String lvl1;
    @CsvBindByPosition(position = 1)
    private String lvl2;
    @CsvBindByPosition(position = 2)
    private String questionnaire;
}
