package com.sap.cap.esmapi.catg.pojos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_CatgRanks
{

    private List<TY_CatgRanksItem> catgRankItems;
}
