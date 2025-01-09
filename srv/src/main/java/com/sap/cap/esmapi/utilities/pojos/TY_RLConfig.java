package com.sap.cap.esmapi.utilities.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_RLConfig
{
    private int numFormSubms;
    private long intvSecs;
    private String allowedAttachments;
    private long allowedSizeAttachmentMB;
    private String internalUsersRegex;
    private String techUserRegex;
    private boolean allowPrevAttDL;
    private boolean enableDestinationCheck;
}
