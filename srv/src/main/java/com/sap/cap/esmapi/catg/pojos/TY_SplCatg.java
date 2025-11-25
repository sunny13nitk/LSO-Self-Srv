package com.sap.cap.esmapi.catg.pojos;

import com.opencsv.bean.CsvBindByPosition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_SplCatg
{
    @CsvBindByPosition(position = 0)
    private String catg;
    @CsvBindByPosition(position = 1)
    private String mvsrv;
    @CsvBindByPosition(position = 2)
    private String formsubmsrv;
    @CsvBindByPosition(position = 3)
    private String casecreatesrv;
    @CsvBindByPosition(position = 4)
    private Boolean editAllowed;

}
