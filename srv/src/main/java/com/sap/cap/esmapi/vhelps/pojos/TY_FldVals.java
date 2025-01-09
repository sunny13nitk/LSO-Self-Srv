package com.sap.cap.esmapi.vhelps.pojos;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_FldVals
{
    private String fieldName;
    private List<TY_KeyValue> vals = new ArrayList<TY_KeyValue>();
}
