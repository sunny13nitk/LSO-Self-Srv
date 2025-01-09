package com.sap.cap.esmapi.utilities.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Ty_UserAccountEmployee
{
    private String userId;
    private String userName;
    private String userEmail;
    private String accountId;
    private String employeeId;
    private boolean employee;
    private boolean external;
    private String destination;

    @Override
    public String toString()
    {
        return "Ty_UserAccountEmployee [userId=" + userId + ", userName=" + userName + ", userEmail=" + userEmail
                + ", accountId=" + accountId + ", employeeId=" + employeeId + ", employee=" + employee + ", external="
                + external + ", destination=" + destination + "]";
    }

}
