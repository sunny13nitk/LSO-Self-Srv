package com.sap.cap.esmapi.utilities.pojos;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_UserDetails
{
    private Ty_UserAccountEmployee usAccEmpl;
    private boolean authenticated;
    private List<String> roles = new ArrayList<String>();
}
