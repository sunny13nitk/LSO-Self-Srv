package com.sap.cap.esmapi.catg.srv.intf;

import com.sap.cap.esmapi.catg.pojos.TY_CaseCatgTree;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;

public interface IF_CatgSrv
{
    public TY_CaseCatgTree getCaseCatgTree4LoB(EnumCaseTypes caseType)  throws EX_ESMAPI;
      
}
