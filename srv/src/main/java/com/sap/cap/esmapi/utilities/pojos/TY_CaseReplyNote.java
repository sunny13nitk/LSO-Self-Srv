package com.sap.cap.esmapi.utilities.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_CaseReplyNote
{
    private String content;
    private String id;
    private String noteId;
    private String noteType;
}
