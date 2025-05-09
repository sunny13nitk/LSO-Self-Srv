package com.sap.cap.esmapi.utilities.pojos;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TY_FormSubmissions
{
    private List<Timestamp> formSubmissions = new ArrayList<Timestamp>();

}
