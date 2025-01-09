package com.sap.cap.esmapi.ui.pojos;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.sap.cap.esmapi.utilities.pojos.TY_AttachmentResponse;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CaseEditFormAsync
{
    private TY_CaseEdit_Form caseReply;
    private String submGuid;
    private String userId;
    private Timestamp timestamp;
    private boolean valid; // To be set post Payload Validation
    private List<TY_AttachmentResponse> attRespList = new ArrayList<TY_AttachmentResponse>();
    private TY_DestinationProps desProps;
}
