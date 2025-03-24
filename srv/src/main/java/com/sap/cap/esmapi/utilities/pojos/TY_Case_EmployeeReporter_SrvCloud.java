package com.sap.cap.esmapi.utilities.pojos;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_Case_EmployeeReporter_SrvCloud
{

    private String subject;
    private String caseType;
    private TY_Employee_CaseCreate employee;
    private TY_Reporter_CaseCreate reporter;
    private String origin;
    private TY_CatgLvl1_CaseCreate categoryLevel1;
    private TY_CatgLvl1_CaseCreate categoryLevel2;
    private TY_CatgLvl1_CaseCreate categoryLevel3;
    private TY_CatgLvl1_CaseCreate categoryLevel4;
    private TY_Description_CaseCreate description;
    private TY_Extensions_CaseCreate extensions;
    private List<TY_Attachment_CaseCreate> attachments = new ArrayList<TY_Attachment_CaseCreate>();

    @Override
    public String toString()
    {
        return "TY_Case_SrvCloud [subject=" + subject + ", caseType=" + caseType + ", account=" + employee
                + ", categoryLevel1=" + categoryLevel1 + ", categoryLevel2=" + categoryLevel2 + ", categoryLevel3="
                + categoryLevel3 + ", categoryLevel4=" + categoryLevel4 + ", description=" + description + "]";
    }

}
