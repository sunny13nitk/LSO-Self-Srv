package com.sap.cap.esmapi.ui.pojos;

import org.springframework.web.multipart.MultipartFile;

import com.sap.cap.esmapi.utilities.pojos.TY_CaseDetails;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CaseEdit_Form
{
    private TY_CaseDetails caseDetails;
    private boolean external;
    private String reply;
    private MultipartFile attachment;
}
