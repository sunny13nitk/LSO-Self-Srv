package com.sap.cap.esmapi.vhelps.pojos;

import com.opencsv.bean.CsvBindByPosition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CountryLangaugeMapping
{
    @CsvBindByPosition(position = 0)
    private String country;
    @CsvBindByPosition(position = 1)
    private String langu1;
    @CsvBindByPosition(position = 2)
    private String langu2;
    @CsvBindByPosition(position = 3)
    private String langu3;
}
