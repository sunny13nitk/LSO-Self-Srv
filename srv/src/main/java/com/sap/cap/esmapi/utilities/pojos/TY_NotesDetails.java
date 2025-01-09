package com.sap.cap.esmapi.utilities.pojos;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_NotesDetails
{

    private String noteType;
    private String id;
    private String noteId;
    private OffsetDateTime timestamp;
    private String createdByName;
    private String content;
    private boolean agentNote;

}
