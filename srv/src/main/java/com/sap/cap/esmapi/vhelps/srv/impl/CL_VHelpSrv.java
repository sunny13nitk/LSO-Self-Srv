package com.sap.cap.esmapi.vhelps.srv.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.SessionScope;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;
import com.sap.cap.esmapi.vhelps.cus.TY_Cus_VHelpsLOB;
import com.sap.cap.esmapi.vhelps.cus.TY_FieldProperties;
import com.sap.cap.esmapi.vhelps.cus.TY_VHelpsRoot;
import com.sap.cap.esmapi.vhelps.pojos.TY_FldVals;
import com.sap.cap.esmapi.vhelps.pojos.TY_KeyValue;
import com.sap.cap.esmapi.vhelps.pojos.TY_LOBVHlpPool;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_VHelpSrv;

import lombok.extern.slf4j.Slf4j;

@Service
@SessionScope
@Slf4j
public class CL_VHelpSrv implements IF_VHelpSrv
{

    @Autowired
    private TY_VHelpsRoot vhlpCus;

    @Autowired
    private IF_SrvCloudAPI srvCloudApiSrv;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private IF_UserSessionSrv userSessionSrv;

    private List<TY_LOBVHlpPool> vhlpPool;

    @Override
    public List<TY_KeyValue> getVHelpDDLB4Field(EnumCaseTypes lob, String fieldName)
    {

        List<TY_KeyValue> vhlpDDLB = null;

        if (lob != null && StringUtils.hasText(fieldName) && vhlpCus != null)
        {

            // 1. Validate field for LOB
            // 1.a. Validate LOB
            Optional<TY_FieldProperties> fldValsO;
            Optional<TY_Cus_VHelpsLOB> lobVhlpO;

            // Customization Not maintained and Not Bound :LOB and Vhelp Fields
            if (CollectionUtils.isEmpty(vhlpCus.getVHelpsCus()))
            {
                String msg = msgSrc.getMessage("ERR_VHLP_NOCUS", null, Locale.ENGLISH);
                log.error(msg);
                throw new EX_ESMAPI(msg);
            }
            else // LOBVhelp customization Bound and LOB(s) configured
            {

                // Check for LOB First for which request is made
                lobVhlpO = vhlpCus.getVHelpsCus().stream().filter(f -> f.getLOB().equals(lob.toString())).findFirst();
                if (lobVhlpO.isPresent())
                {
                    // LOB Customizing Bound - Now check for Field REquested
                    fldValsO = lobVhlpO.get().getFields().stream().filter(d -> d.getFieldName().equals(fieldName))
                            .findFirst();
                    if (fldValsO.isPresent())
                    {
                        // Field Also Configured for Value help Determination
                        vhlpDDLB = fetchAndPopulateVhelp4Fld(lob, fieldName);
                    }
                    else
                    {
                        String msg = msgSrc.getMessage("ERR_VHLP_INVALID_FLD", new Object[]
                        { lob.toString(), fieldName }, Locale.ENGLISH);
                        log.error(msg);
                        throw new EX_ESMAPI(msg);
                    }
                }
                else
                {
                    String msg = msgSrc.getMessage("ERR_VHLP_INVALID_LOB", new Object[]
                    { lob.toString() }, Locale.ENGLISH);
                    log.error(msg);
                    throw new EX_ESMAPI(msg);

                }

            }

        }

        return vhlpDDLB;
    }

    private List<TY_KeyValue> fetchAndPopulateVhelp4Fld(EnumCaseTypes lob, String fieldName)
    {
        boolean fieldFound = false;
        List<TY_KeyValue> vhlpDDLB = null;

        if (vhlpPool == null)
        {
            try
            {
                vhlpDDLB = srvCloudApiSrv.getVHelpDDLB4Field(fieldName,
                        userSessionSrv.getDestinationDetails4mUserSession());
                if (CollectionUtils.isNotEmpty(vhlpDDLB))
                {
                    // sort Vhelps in Ascending order by default
                    Collections.sort(vhlpDDLB, Comparator.comparing(TY_KeyValue::getKey));
                    vhlpPool = new ArrayList<TY_LOBVHlpPool>();
                    TY_LOBVHlpPool lobVhlpPool = new TY_LOBVHlpPool(lob, new ArrayList<TY_FldVals>());
                    TY_FldVals fldVals = new TY_FldVals(fieldName, vhlpDDLB);
                    lobVhlpPool.getFldVals().add(fldVals);
                    vhlpPool.add(lobVhlpPool);
                }
            }
            catch (EX_ESMAPI | IOException e)
            {
                // DO Nothing - Don't Stop the Process - Just push the error in the log
                log.error(e.getLocalizedMessage());
            }

        }
        else
        {
            if (CollectionUtils.isNotEmpty(vhlpPool))
            {
                // SCan for LOB
                Optional<TY_LOBVHlpPool> lobVhlpsO;
                lobVhlpsO = vhlpPool.stream().filter(c -> c.getLob().name().equals(lob.name())).findFirst();
                if (lobVhlpsO.isPresent())
                {
                    // Scan for field
                    Optional<TY_FldVals> fldValsO = lobVhlpsO.get().getFldVals().stream()
                            .filter(x -> x.getFieldName().equals(fieldName)).findFirst();
                    if (fldValsO.isPresent())
                    {
                        fieldFound = true;
                        vhlpDDLB = fldValsO.get().getVals();
                    }

                }

                if (fieldFound != true)
                {
                    // Fetch from Srv Cloud and Maintain in session
                    try
                    {
                        vhlpDDLB = srvCloudApiSrv.getVHelpDDLB4Field(fieldName,
                                userSessionSrv.getDestinationDetails4mUserSession());
                        if (CollectionUtils.isNotEmpty(vhlpDDLB))
                        {
                            // sort Vhelps in Ascending order by default
                            Collections.sort(vhlpDDLB, Comparator.comparing(TY_KeyValue::getKey));
                            // Get the LOB Vhelp

                            Optional<TY_LOBVHlpPool> vHlpLobO = vhlpPool.stream()
                                    .filter(f -> f.getLob().name().equals(lob.name())).findFirst();
                            if (vHlpLobO.isPresent())
                            {

                                vHlpLobO.get().getFldVals().add(new TY_FldVals(fieldName, vhlpDDLB));
                            }
                            else
                            {

                                TY_LOBVHlpPool lobVhlpPool = new TY_LOBVHlpPool(lob, new ArrayList<TY_FldVals>());
                                TY_FldVals fldVals = new TY_FldVals(fieldName, vhlpDDLB);
                                lobVhlpPool.getFldVals().add(fldVals);
                                vhlpPool.add(lobVhlpPool);
                            }

                        }
                    }
                    catch (EX_ESMAPI | IOException e)
                    {
                        // DO Nothing - Don't Stop the Process - Just push the error in the log
                        log.error(e.getLocalizedMessage());
                    }

                }
            }
        }

        return vhlpDDLB;
    }

}
