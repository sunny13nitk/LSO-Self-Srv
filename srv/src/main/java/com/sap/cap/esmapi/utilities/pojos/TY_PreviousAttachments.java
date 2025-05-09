package com.sap.cap.esmapi.utilities.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_PreviousAttachments
{
    private String id;
    private String title;
    private long fileSizeinKB;
    private String createdByName;
    private String createdOn;
    private boolean byTechnicalUser;
    private String url;

}
