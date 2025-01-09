package com.sap.cap.esmapi.vhelps.pojos;

import java.util.List;

import com.sap.cap.esmapi.vhelps.cus.TY_Catg_MandatoryFlds;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_MandatoryFlds_CatgsList
{
    private List<TY_Catg_MandatoryFlds> catgsList;
}
