package com.sap.cap.esmapi.utilities.pojos;

import java.sql.Timestamp;

import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TY_Message
{
    private String userName;
    private Timestamp timestamp;
    private EnumStatus status;
    private EnumMessageType msgType;
    private String objectId;
    private String message;
    private String country;
    private String category1;
    private String category2;

    public TY_Message(String userName, Timestamp timestamp, EnumStatus status, EnumMessageType msgType,
            String objectId, String message)
    {
        this(userName, timestamp, status, msgType, objectId, message, null, null, null);
    }
}
