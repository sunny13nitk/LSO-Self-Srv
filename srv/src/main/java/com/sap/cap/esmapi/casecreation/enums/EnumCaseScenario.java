package com.sap.cap.esmapi.casecreation.enums;

public enum EnumCaseScenario
{
    IC(EnumMainPartnerType.INDIVIDUAL_CUSTOMER, EnumReporterType.NONE),

    IC_ADD_IC(EnumMainPartnerType.INDIVIDUAL_CUSTOMER, EnumReporterType.INDIVIDUAL_CUSTOMER),

    IC_ADD_ACCOUNT(EnumMainPartnerType.INDIVIDUAL_CUSTOMER, EnumReporterType.ACCOUNT),

    ACCOUNT(EnumMainPartnerType.ACCOUNT, EnumReporterType.NONE),

    ACCOUNT_ADD_IC(EnumMainPartnerType.ACCOUNT, EnumReporterType.INDIVIDUAL_CUSTOMER),

    ACCOUNT_ADD_ACCOUNT(EnumMainPartnerType.ACCOUNT, EnumReporterType.ACCOUNT),

    EMPLOYEE(EnumMainPartnerType.EMPLOYEE, EnumReporterType.NONE),

    EMPLOYEE_ADD_IC(EnumMainPartnerType.EMPLOYEE, EnumReporterType.INDIVIDUAL_CUSTOMER),

    EMPLOYEE_ADD_ACCOUNT(EnumMainPartnerType.EMPLOYEE, EnumReporterType.ACCOUNT),

    EMPLOYEE_ADD_EMPLOYEE(EnumMainPartnerType.EMPLOYEE, EnumReporterType.EMPLOYEE);

    private final EnumMainPartnerType mainPartnerType;
    private final EnumReporterType reporterType;

    EnumCaseScenario(EnumMainPartnerType mainPartnerType, EnumReporterType reporterType)
    {
        this.mainPartnerType = mainPartnerType;
        this.reporterType = reporterType;
    }

    public EnumMainPartnerType getMainPartnerType()
    {
        return mainPartnerType;
    }

    public EnumReporterType getReporterType()
    {
        return reporterType;
    }
}