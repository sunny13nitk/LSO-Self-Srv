package com.sap.cap.esmapi.utilities.pojos;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_Case_EmployeeAddUserIC_SrvCloud
{

    @JsonIgnore
    private boolean external;
    private String subject;
    private String caseType;
    private TY_Employee_CaseCreate employee;
    private List<TY_AddUserCaseCreate> customIndividualCustomers = new ArrayList<TY_AddUserCaseCreate>();
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
