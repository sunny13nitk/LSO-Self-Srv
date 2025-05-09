package com.sap.cap.esmapi.catg.srv.impl;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.opencsv.bean.CsvToBeanBuilder;
import com.sap.cap.esmapi.catg.pojos.TY_CaseCatgTree;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgGuidsDesc;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatgSrv;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CL_CatgSrv implements IF_CatgSrv 
{

    private List<TY_CaseCatgTree> caseCatgContainer;

    @Autowired
    private TY_CatgCus catgCus;

    @Autowired
    private MessageSource msgSrc;

    @Override
    public TY_CaseCatgTree getCaseCatgTree4LoB(EnumCaseTypes caseType) throws EX_ESMAPI 
    {
        TY_CaseCatgTree caseCatgTree= null;
        if (caseType != null)
         {
            if (!CollectionUtils.isEmpty(caseCatgContainer)) 
            {
                // 1. Check from Session if Loaded already!
                Optional<TY_CaseCatgTree> caseCatgTreeO = caseCatgContainer.stream()
                        .filter(f -> f.getCaseTypeEnum().toString().equals(caseType.toString())).findFirst();
                if (caseCatgTreeO.isPresent()) 
                {
                    System.out.println("REading Catg. Tree from Session for :" + caseType);
                    return caseCatgTreeO.get();
                }
                else
                {
                    caseCatgTree = loadCatgTree4CaseType(caseType);
                }
            } 
            else
            {
                caseCatgTree = loadCatgTree4CaseType(caseType);
            }

        }

        return caseCatgTree;
    }

    private TY_CaseCatgTree loadCatgTree4CaseType(EnumCaseTypes caseType)
    {

        TY_CaseCatgTree caseCatgTree= null;

        // Load from Config

        // Get the Config
        Optional<TY_CatgCusItem> caseCFgO = catgCus.getCustomizations().stream()
                .filter(g -> g.getCaseTypeEnum().toString().equals(caseType.toString())).findFirst();
        if (caseCFgO.isPresent()) 
        {
            // Read From Case Type csv Catg Configuration

            try
            {

                ClassPathResource classPathResource = new ClassPathResource(caseCFgO.get().getCatgCsvPath());
                if (classPathResource != null) 
                {
                    Reader reader = new InputStreamReader(classPathResource.getInputStream());
                    if (reader != null) 
                    {
                        System.out.println("Catg. Resource Bound... at path : " +caseCFgO.get().getCatgCsvPath() );
                        
                        List<TY_CatgGuidsDesc> catgs = new CsvToBeanBuilder(reader).withSkipLines(1)
                                .withType(TY_CatgGuidsDesc.class).build().parse();

                        if (!CollectionUtils.isEmpty(catgs))
                        {
                            caseCatgTree = new TY_CaseCatgTree(caseType, catgs);
                            if(caseCatgContainer == null)
                            {
                                caseCatgContainer = new ArrayList<TY_CaseCatgTree>();
                            }
                            else
                            {
                                caseCatgContainer.add(caseCatgTree);
                            }
                            System.out.println("Category Entries in Config Found " + catgs.size() + " for Case Type: " + caseType.toString());
                           
                        }
                    }
                }

            } catch (Exception e)
            {
                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CATG_LOAD",
                        new Object[] { caseCFgO.get().getCatgCsvPath(), caseType.toString() }, Locale.ENGLISH));
            }

        } 
        else 
        {
            throw new EX_ESMAPI(
                    msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[] { caseType.toString() }, Locale.ENGLISH));
        }

        return caseCatgTree;
    }

}
