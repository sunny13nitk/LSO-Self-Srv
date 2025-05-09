package com.sap.cap.esmapi.status.srv.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.SessionScope;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransI;
import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransICode;
import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransitions;
import com.sap.cap.esmapi.status.pojos.TY_StatusCfg;
import com.sap.cap.esmapi.status.pojos.TY_StatusCfgItem;
import com.sap.cap.esmapi.status.srv.intf.IF_StatusSrv;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@SessionScope
@RequiredArgsConstructor
@Slf4j
public class CL_StatusSrv implements IF_StatusSrv
{

    private final TY_CatgCus catgCus; // Autowired

    private final TY_PortalStatusTransitions statusTransitions; // Autowired

    private final MessageSource msgSrc; // Autowired

    private final IF_SrvCloudAPI srvCloudApi; // Autowired

    private final IF_UserSessionSrv userSessionSrv; // Autowired

    private List<TY_StatusCfg> lobStatusCfgList;

    @Override
    public TY_StatusCfg getStatusCfg4CaseType(EnumCaseTypes caseType) throws EX_ESMAPI, IOException
    {
        TY_StatusCfg statusCFG = null;
        if (caseType != null && catgCus != null)
        {
            if (CollectionUtils.isNotEmpty(lobStatusCfgList))
            {
                Optional<TY_StatusCfg> statusCfgO = lobStatusCfgList.stream()
                        .filter(s -> s.getCaseType().equals(caseType)).findFirst();
                if (statusCfgO.isPresent())
                {
                    statusCFG = statusCfgO.get();
                }
                else
                {
                    statusCFG = fetchStatusCfg4CaseType(caseType);
                }
            }
            else
            {
                statusCFG = fetchStatusCfg4CaseType(caseType);
            }
        }

        return statusCFG;
    }

    @Override
    public TY_PortalStatusTransICode getPortalStatusTransition4CaseTypeandCaseStatus(String caseType, String caseStatus)
            throws EX_ESMAPI, IOException
    {
        TY_PortalStatusTransICode statTransCus = null;

        if (StringUtils.hasText(caseStatus) && StringUtils.hasText(caseType) && msgSrc != null && catgCus != null)
        {
            if (CollectionUtils.isNotEmpty(statusTransitions.getStatusTransitions()))
            {
                Optional<TY_PortalStatusTransI> transO = statusTransitions.getStatusTransitions().stream().filter(s ->
                {
                    if (s.getCaseType().equals(caseType) && s.getFromStatus().equalsIgnoreCase(caseStatus))
                    {
                        return true;
                    }
                    return false;
                }).findFirst();
                if (transO.isPresent())
                {
                    statTransCus = new TY_PortalStatusTransICode(transO.get(), null);

                    Optional<TY_CatgCusItem> cusItemO = catgCus.getCustomizations().stream()
                            .filter(f -> f.getCaseType().equals(caseType)).findFirst();
                    if (cusItemO.isPresent())
                    {
                        // Get All Status Definitions for Case Type
                        TY_StatusCfg statusCFG = this.getStatusCfg4CaseType(cusItemO.get().getCaseTypeEnum());
                        if (statusCFG != null)
                        {
                            if (CollectionUtils.isNotEmpty(statusCFG.getUserStatusAssignments()))
                            {
                                // Filter for To Status Description
                                Optional<TY_StatusCfgItem> toStatusO = statusCFG.getUserStatusAssignments().stream()
                                        .filter(s -> s.getUserStatusDescription()
                                                .equalsIgnoreCase(transO.get().getToStatus()))
                                        .findFirst();

                                if (toStatusO.isPresent())
                                {
                                    statTransCus.setToStatusCode(toStatusO.get().getUserStatus());
                                }
                            }
                        }
                    }

                }
                else
                {
                    String msg = msgSrc.getMessage("ERR_CFG_STATTRAN_NOTFOUND", new Object[]
                    { caseType, caseStatus }, Locale.ENGLISH);
                    log.error(msg);

                    throw new EX_ESMAPI(msg);
                }
            }
        }

        return statTransCus;
    }

    @Override
    public String getConfirmedStatusCode4CaseType(String caseType) throws EX_ESMAPI, IOException
    {
        String cnfStatusCode = null;

        if (StringUtils.hasText(caseType) && catgCus != null)
        {
            // Load Customizing for Case Type
            Optional<TY_CatgCusItem> cusO = catgCus.getCustomizations().stream()
                    .filter(c -> c.getCaseType().equalsIgnoreCase(caseType)).findFirst();
            if (cusO.isPresent())
            {
                TY_CatgCusItem cus = cusO.get();
                if (StringUtils.hasText(cus.getStatusSchema()) && StringUtils.hasText(cus.getConfirmStatus()))
                {
                    TY_StatusCfg statusCfg = this.getStatusCfg4CaseType(cus.getCaseTypeEnum());
                    if (statusCfg != null)
                    {
                        if (CollectionUtils.isNotEmpty(statusCfg.getUserStatusAssignments()))
                        {
                            Optional<TY_StatusCfgItem> cfStatusO = statusCfg.getUserStatusAssignments().stream()
                                    .filter(s -> s.getUserStatusDescription().equals(cus.getConfirmStatus()))
                                    .findFirst();
                            if (cfStatusO.isPresent())
                            {
                                cnfStatusCode = cfStatusO.get().getUserStatus();
                            }
                        }
                    }
                }
                else
                {
                    // ERR_NO_SCHEMA_SPECIFIED= No Status Schema or Confirmed Status configured in
                    // App Customization for Case Type - {0}.
                    String msg = msgSrc.getMessage("ERR_NO_SCHEMA_SPECIFIED", new Object[]
                    { caseType }, Locale.ENGLISH);
                    log.error(msg);
                    throw new EX_ESMAPI(msg);
                }
            }

        }
        return cnfStatusCode;

    }

    private TY_StatusCfg fetchStatusCfg4CaseType(EnumCaseTypes caseType) throws EX_ESMAPI, IOException
    {
        TY_StatusCfg statusCfg = null;
        if (CollectionUtils.isNotEmpty(catgCus.getCustomizations()))
        {
            Optional<TY_CatgCusItem> cusO = catgCus.getCustomizations().stream()
                    .filter(c -> c.getCaseTypeEnum().name().equalsIgnoreCase(caseType.name())).findFirst();
            if (cusO.isPresent())
            {
                TY_CatgCusItem cus = cusO.get();
                if (StringUtils.hasText(cus.getStatusSchema()))
                {
                    // Fetch Status Description snd Codes from Service cloud

                    List<TY_StatusCfgItem> statusCfgs = srvCloudApi.getStatusCfg4StatusSchema(cus.getStatusSchema(),
                            userSessionSrv.getDestinationDetails4mUserSession());
                    if (CollectionUtils.isNotEmpty(statusCfgs))
                    {
                        if (CollectionUtils.isEmpty(lobStatusCfgList))
                        {
                            lobStatusCfgList = new ArrayList<TY_StatusCfg>();
                        }

                        statusCfg = new TY_StatusCfg(caseType, statusCfgs);

                        lobStatusCfgList.add(statusCfg);
                    }

                }
                else
                {
                    // ERR_NO_SCHEMA_SPECIFIED= No Status Schema configured in App Customization for
                    // Case Type - {0}.
                    String msg = msgSrc.getMessage("ERR_NO_SCHEMA_SPECIFIED", new Object[]
                    { caseType }, Locale.ENGLISH);
                    log.error(msg);
                    throw new EX_ESMAPI(msg);
                }
            }
        }

        return statusCfg;
    }

}
