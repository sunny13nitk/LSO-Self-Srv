package com.sap.cap.esmapi.vhelps.pojos;

import com.opencsv.bean.CsvBindByPosition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CatgCountryMapping
{
    @CsvBindByPosition(position = 0)
    public String categoryUC;
    @CsvBindByPosition(position = 1)
    public String country;

}
