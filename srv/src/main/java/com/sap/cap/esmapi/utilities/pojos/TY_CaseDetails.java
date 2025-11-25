package com.sap.cap.esmapi.utilities.pojos;

import java.util.List;

import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransICode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CaseDetails
{
    private String caseId;
    private String caseGuid;
    private String caseType;
    private String origin;
    private String status;
    private String description;
    private String eTag;
    private String catgLvl1;

    private List<TY_NotesDetails> notes;

    private List<TY_PreviousAttachments> prevAttachments;

    private TY_PortalStatusTransICode statusTransitionCFG;

    private boolean editableSplCatg;

}
