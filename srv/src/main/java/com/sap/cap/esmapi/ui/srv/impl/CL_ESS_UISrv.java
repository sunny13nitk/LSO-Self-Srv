package com.sap.cap.esmapi.ui.srv.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_ESS_Stats;
import com.sap.cap.esmapi.ui.pojos.TY_NameValueLPair;
import com.sap.cap.esmapi.ui.srv.intf.IF_ESS_UISrv;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseESS;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.apache.commons.math3.util.Precision;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CL_ESS_UISrv implements IF_ESS_UISrv
{
    @Autowired
    private IF_SrvCloudAPI srvCloudApiSrv;

    @Autowired
    private IF_UserSessionSrv userSessionSrv;

    @Override
    public TY_ESS_Stats getStatsForUserCases(List<TY_CaseESS> cases4User) throws EX_ESMAPI
    {
        TY_ESS_Stats stats = null;
        if (!CollectionUtils.isEmpty(cases4User))
        {
            stats = new TY_ESS_Stats();

            /*
             * -----Prepare Case Summary
             */

            stats.getCaseSummary().setTotalCases(cases4User.size());
            // Get Completed Cases
            stats.getCaseSummary().setCompletedCases(cases4User.stream().filter(c ->
            {
                if (c.getStatusDesc().equals(GC_Constants.gc_statusCompleted)
                        || c.getStatusDesc().equals(GC_Constants.gc_statusSolnProvided))
                {
                    return true;
                }
                else
                {
                    return false;
                }

            }).collect(Collectors.toList()).size());
            // Set Percentage Completed
            stats.getCaseSummary().setPerCompleted(Precision.round(
                    ((stats.getCaseSummary().getCompletedCases() * 100) / stats.getCaseSummary().getTotalCases()), 0));

            System.out.println("Total Cases : " + stats.getCaseSummary().getTotalCases() + " Completed : "
                    + stats.getCaseSummary().getCompletedCases() + " % Compl.: "
                    + stats.getCaseSummary().getPerCompleted());

            /*
             * -----Prepare LOB Spread
             */

            // Grouping and Showing Group Key and Correspoding Entities in each Group
            Map<String, List<TY_CaseESS>> allocsPerCaseTypeList = cases4User.stream()
                    .collect(Collectors.groupingBy(TY_CaseESS::getCaseTypeDescription));

            if (!CollectionUtils.isEmpty(allocsPerCaseTypeList))
            {
                for (Map.Entry<String, List<TY_CaseESS>> group : allocsPerCaseTypeList.entrySet())
                {
                    stats.getLobSpread().add(new TY_NameValueLPair(group.getKey(), group.getValue().size()));
                    System.out.println(group.getKey() + " has " + group.getValue().size() + " cases..");
                }
            }

            /*
             * -----Prepare Status Spread
             */

            // Grouping and Showing Group Key and Correspoding Entities in each Group
            Map<String, List<TY_CaseESS>> allocsPerStatusList = cases4User.stream()
                    .collect(Collectors.groupingBy(TY_CaseESS::getStatusDesc));

            if (!CollectionUtils.isEmpty(allocsPerStatusList))
            {
                for (Map.Entry<String, List<TY_CaseESS>> group : allocsPerStatusList.entrySet())
                {
                    stats.getStatusSpread().add(new TY_NameValueLPair(group.getKey(), group.getValue().size()));
                    System.out.println(group.getKey() + " has " + group.getValue().size() + " cases..");
                }
            }

        }
        return stats;
    }

    @Override
    public List<TY_CaseESS> getCases4User(Ty_UserAccountEmployee userDetails) throws IOException
    {
        return srvCloudApiSrv.getCases4User(userDetails, userSessionSrv.getDestinationDetails4mUserSession());
    }

    @Override
    public List<TY_CaseESS> getCases4User(Ty_UserAccountEmployee userDetails, EnumCaseTypes caseType) throws IOException
    {
        return srvCloudApiSrv.getCases4User(userDetails, caseType, userSessionSrv.getDestinationDetails4mUserSession());
    }

}
