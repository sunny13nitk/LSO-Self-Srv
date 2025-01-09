package com.sap.cap.esmapi.vhelps.cus;

import com.opencsv.bean.CsvBindByPosition;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_Catg_MandatoryFlds
{
    @CsvBindByPosition(position = 0)
    private String catgString;
}
