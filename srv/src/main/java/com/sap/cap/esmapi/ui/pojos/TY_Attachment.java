package com.sap.cap.esmapi.ui.pojos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_Attachment
{
    @JsonIgnore
    private boolean external;
    private String fileName;
    private String category;
    private boolean isSelected;
}
