package com.sap.cap.esmapi.utilities.pojos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_SessAttContainer
{
    private List<TY_SessionAttachment> attachments;
    private List<String> messages;

}
