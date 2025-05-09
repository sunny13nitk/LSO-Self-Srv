package com.sap.cap.esmapi.utilities.pojos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_NotesCreate
{
    @JsonIgnore
    private boolean external;
    private String htmlContent;
    private String noteTypeCode;
}
