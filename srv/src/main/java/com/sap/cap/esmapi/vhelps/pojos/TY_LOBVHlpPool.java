package com.sap.cap.esmapi.vhelps.pojos;

import java.util.List;

import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_LOBVHlpPool
{
    private EnumCaseTypes lob;
    private List<TY_FldVals> fldVals;

}
