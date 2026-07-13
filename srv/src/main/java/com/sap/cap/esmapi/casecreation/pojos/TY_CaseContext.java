package com.sap.cap.esmapi.casecreation.pojos;

import java.util.ArrayList;
import java.util.List;

import com.sap.cap.esmapi.casecreation.enums.EnumCaseScenario;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;
import com.sap.cap.esmapi.utilities.pojos.TY_Attachment_CaseCreate;
import com.sap.cap.esmapi.utilities.pojos.TY_Description_CaseCreate;
import com.sap.cap.esmapi.utilities.srvCloudApi.destination.pojos.TY_DestinationProps;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CaseContext
{
    private EV_CaseFormSubmit caseEvent;

    private EnumCaseScenario caseScenario;

    private boolean validScenario;

    private TY_DestinationProps destinationProps;

    // Runtime generated artifacts
    private TY_Description_CaseCreate description;

    private List<TY_Attachment_CaseCreate> attachments = new ArrayList<>();

}
