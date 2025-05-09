package com.sap.cap.esmapi.ui.pojos;

import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CaseConfirmPOJO
{
    private String caseId;
    private String caseGuid;
    private String caseType;
    private String origin;
    private String status;
    private String submGuid;
    private String eTag;
    private String cnfStatusCode;
    private String userId;
    private TY_DestinationProps desProps;
}
