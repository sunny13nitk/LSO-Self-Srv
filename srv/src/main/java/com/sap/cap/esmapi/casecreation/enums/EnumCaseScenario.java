package com.sap.cap.esmapi.casecreation.enums;

import java.util.Objects;

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

    public static EnumCaseScenario resolve(EnumMainPartnerType mainPartnerType, EnumReporterType reporterType)
    {
        Objects.requireNonNull(mainPartnerType, "Main Partner Type cannot be null.");
        Objects.requireNonNull(reporterType, "Reporter Type cannot be null.");

        for (EnumCaseScenario scenario : values())
        {
            if (scenario.mainPartnerType == mainPartnerType && scenario.reporterType == reporterType)
            {
                return scenario;
            }
        }

        throw new IllegalArgumentException(String.format("Unsupported Case Scenario [Main Partner=%s, Reporter=%s]",
                mainPartnerType, reporterType));
    }
}