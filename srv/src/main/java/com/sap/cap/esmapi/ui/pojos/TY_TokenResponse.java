package com.sap.cap.esmapi.ui.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TY_TokenResponse
{

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String scope;

}
