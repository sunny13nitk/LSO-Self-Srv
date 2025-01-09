package com.sap.cap.esmapi.status.pojos;

import com.opencsv.bean.CsvBindByPosition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_PortalStatusTransI
{
    @CsvBindByPosition(position = 0)
    private String caseType;
    @CsvBindByPosition(position = 1)
    private String fromStatus;
    @CsvBindByPosition(position = 2)
    private String toStatus;
    @CsvBindByPosition(position = 3)
    private Boolean editAllowed;
    @CsvBindByPosition(position = 4)
    private Boolean confirmAllowed;
}
