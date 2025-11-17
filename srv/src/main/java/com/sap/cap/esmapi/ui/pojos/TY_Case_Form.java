package com.sap.cap.esmapi.ui.pojos;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_Case_Form
{
    private String accId;
    private String caseTxnType;
    private String catgDesc;
    private String subject;
    private String description;
    private String template;
    private MultipartFile attachment;
    private String country;
    private String language;
    private String addEmail; // accomodate additional email address for reporter
    private String reporter;
    private boolean reporterEmployee;
    private boolean countryMandatory;
    private boolean langMandatory;
    private boolean employee;
    private boolean external;
    private boolean catgChange;
    private String catgText;
    private String appId; // Exam form Extensions
    private boolean extraTime;
    private boolean exambreak;
    private boolean extramonitor;
    private boolean extraperson;

}
