package com.sap.cap.esmapi.utilities.emailValidator;

/*
 * Utility Class that employs Apache Commons Email Validator and 
 * general regex to suit SAP specific email addreses
 */
public class CL_EmailValidationUtils
{
    private static final String REGEX = "^[\\p{L}0-9!#$%&'*+\\/=?^_`{|}~-][\\p{L}0-9.!#$%&'*+\\/=?^_`{|}~-]{0,63}@[\\p{L}0-9-]+(?:\\.[\\p{L}0-9-]{2,7})*$";
}
