package com.sap.cap.esmapi.status.pojos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_PortalStatusTransitions
{
    private List<TY_PortalStatusTransI> statusTransitions;
}
