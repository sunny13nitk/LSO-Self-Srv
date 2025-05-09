package com.sap.cap.esmapi.catg.srv.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatalogItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatalogTree;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgDetails;
import com.sap.cap.esmapi.catg.pojos.TY_CatgRanks;
import com.sap.cap.esmapi.catg.pojos.TY_CatgRanksItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplates;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplatesCus;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseCatalogCustomizing;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

@Service
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CL_CatalogSrv implements IF_CatalogSrv
{

    private static final int maxCatgLevels = 4;

    private List<TY_CatalogTree> caseCatgContainer;

    @Autowired
    private TY_CatgCus catgCus;

    @Autowired
    private TY_CatgRanks catgRanks;

    @Autowired
    private TY_CatgTemplatesCus catgTmplCus;

    @Autowired
    private IF_SrvCloudAPI srvCloudApiSrv;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private IF_UserSessionSrv userSessionSrv;

    @Override
    public TY_CatalogTree getCaseCatgTree4LoB(EnumCaseTypes caseType) throws EX_ESMAPI
    {
        TY_CatalogTree caseCatgTree = null;
        if (caseType != null)
        {
            if (!CollectionUtils.isEmpty(caseCatgContainer))
            {
                // 1. Check from Session if Loaded already!
                Optional<TY_CatalogTree> caseCatgTreeO = caseCatgContainer.stream()
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

    @Override
    public String[] getCatgHierarchyforCatId(String catId, EnumCaseTypes caseType) throws EX_ESMAPI
    {
        String[] catTree = null;
        int idx = 0;

        if (StringUtils.hasText(catId) && caseType != null)
        {
            String catCurr = catId;

            // Get Complete Catalog Details
            TY_CatalogTree catalogTree = this.getCaseCatgTree4LoB(caseType);
            if (CollectionUtils.isNotEmpty(catalogTree.getCategories()))
            {
                catTree = new String[maxCatgLevels]; // Max upto 4 levels
                while (StringUtils.hasText(catCurr))
                {
                    String catScan = catCurr;
                    // Remove blank Categories from Catalog Tree Used for UI Presentation
                    catalogTree.getCategories().removeIf(x -> x.getId() == null);
                    // Scan for Category in Catalog Tree
                    Optional<TY_CatalogItem> itemSel = catalogTree.getCategories().stream()
                            .filter(t -> t.getId().equals(catScan)).findFirst();
                    if (itemSel.isPresent())
                    {
                        catTree[idx] = catCurr;

                        // Seek Parent
                        if (StringUtils.hasText(itemSel.get().getParentId()))
                        {
                            catCurr = itemSel.get().getParentId();
                        }
                        else
                        {
                            catCurr = null;
                        }

                        idx++;
                    }

                }
                // Refurbish Blank Category at Top for New Form - Session maintained
                catalogTree.getCategories().add(0, new TY_CatalogItem());

            }
        }

        return catTree;
    }

    @Override
    public TY_CatgTemplates getTemplates4Catg(String catId, EnumCaseTypes caseType) throws EX_ESMAPI
    {
        TY_CatgTemplates catgTmpl = null;
        if (StringUtils.hasText(catId) && CollectionUtils.isNotEmpty(catgTmplCus.getCatgTemplates())
                && caseType != null)
        {
            TY_CatalogTree catgTree = this.getCaseCatgTree4LoB(caseType);
            boolean isLvl1 = false;
            if (CollectionUtils.isNotEmpty(catgTree.getCategories()))
            {
                // Remove blank Categories from Catalog Tree Used for UI Presentation
                catgTree.getCategories().removeIf(x -> x.getId() == null);

                Optional<TY_CatalogItem> currCatgDetailsO = catgTree.getCategories().stream()
                        .filter(f -> f.getId().equals(catId)).findFirst();
                if (currCatgDetailsO.isPresent())
                {
                    // 1. Get Text from Catg Guid selected in form and Convert to Upper Case
                    String catgTxt = null;
                    if (StringUtils.hasText(currCatgDetailsO.get().getParentName()))
                    {
                        catgTxt = currCatgDetailsO.get().getParentName() + ">" + currCatgDetailsO.get().getName();
                        catgTxt = catgTxt.toUpperCase();
                    }
                    else
                    {
                        // No Level 1 Catg. is valid . Hence do not seek for level 1 catg.
                        catgTxt = currCatgDetailsO.get().getName().toUpperCase();
                        isLvl1 = true;

                    }

                    // 2. Get Template for Catg. Text using Starts with Pattern matching in Stream
                    // from Catg Tmpl Cus Autowired Bean
                    if (StringUtils.hasText(catgTxt))
                    {
                        String catTxtToSearch = catgTxt;
                        try
                        {
                            Optional<TY_CatgTemplates> catgTmplO = null;
                            if (!isLvl1)
                            {
                                catgTmplO = catgTmplCus.getCatgTemplates().stream()
                                        .filter(t -> t.getCatgU().startsWith(catTxtToSearch)).findFirst();

                            }
                            else
                            {
                                catgTmplO = catgTmplCus.getCatgTemplates().stream()
                                        .filter(t -> t.getCatgU().endsWith(catTxtToSearch)).findFirst();
                            }

                            if (catgTmplO.isPresent())
                            {
                                catgTmpl = catgTmplO.get();
                            }

                        }
                        catch (NullPointerException e)
                        {
                            // Do Nothing - No template Relevant Category selected
                        }

                    }
                }

                // Refurbish Blank Category at Top for New Form - Session maintained
                catgTree.getCategories().add(0, new TY_CatalogItem());
            }

        }

        return catgTmpl;
    }

    @Override
    public TY_CatgDetails getCategoryDetails4Catg(String catId, EnumCaseTypes caseType, boolean inUpperCase)
            throws EX_ESMAPI
    {
        TY_CatgDetails catgDetails = null;

        if (StringUtils.hasText(catId) && CollectionUtils.isNotEmpty(catgTmplCus.getCatgTemplates())

                && caseType != null)
        {
            TY_CatalogTree catgTree = this.getCaseCatgTree4LoB(caseType);
            boolean isLvl1 = false;
            if (CollectionUtils.isNotEmpty(catgTree.getCategories()))
            {
                // Remove blank Categories from Catalog Tree Used for UI Presentation
                catgTree.getCategories().removeIf(x -> x.getId() == null);

                Optional<TY_CatalogItem> currCatgDetailsO = catgTree.getCategories().stream()
                        .filter(f -> f.getId().equals(catId)).findFirst();
                if (currCatgDetailsO.isPresent())
                {
                    catgDetails = new TY_CatgDetails();
                    // 1. Get Text from Catg Guid selected in form and Convert to Upper Case
                    String catgTxt = null;
                    if (StringUtils.hasText(currCatgDetailsO.get().getParentName()))
                    {
                        catgTxt = currCatgDetailsO.get().getParentName() + ">" + currCatgDetailsO.get().getName();
                        if (inUpperCase)
                        {
                            catgTxt = catgTxt.toUpperCase();
                        }

                    }
                    else
                    {
                        // No Level 1 Catg. is valid . Hence do not seek for level 1 catg.
                        if (inUpperCase)
                        {
                            catgTxt = currCatgDetailsO.get().getName().toUpperCase();
                        }
                        else
                        {
                            catgTxt = currCatgDetailsO.get().getName();
                        }

                        isLvl1 = true;

                    }
                    catgDetails.setCatDesc(catgTxt);
                    catgDetails.setCatgId(catId);
                    catgDetails.setInUpperCase(inUpperCase);
                    catgDetails.setRoot(isLvl1);

                }
                // Refurbish Blank Category at Top for New Form - Session maintained
                catgTree.getCategories().add(0, new TY_CatalogItem());
            }
        }

        return catgDetails;
    }

    private TY_CatalogTree loadCatgTree4CaseType(EnumCaseTypes caseType)
    {

        TY_CatalogTree caseCatgTree = null;

        // Get the Config
        Optional<TY_CatgCusItem> caseCFgO = catgCus.getCustomizations().stream()
                .filter(g -> g.getCaseTypeEnum().toString().equals(caseType.toString())).findFirst();
        if (caseCFgO.isPresent() && srvCloudApiSrv != null && userSessionSrv != null)
        {
            // Read FRom Srv Cloud the Catg. Tree
            try
            {
                // Get config from Srv Cloud for Case type - Active Catalog ID
                TY_CaseCatalogCustomizing caseCus = srvCloudApiSrv.getActiveCaseTemplateConfig4CaseType(
                        caseCFgO.get().getCaseType(), userSessionSrv.getDestinationDetails4mUserSession());
                if (caseCus != null)
                {
                    if (StringUtils.hasText(caseCus.getCataglogId()))
                    {
                        // Get category Tree for Catalog ID
                        caseCatgTree = new TY_CatalogTree(caseType, srvCloudApiSrv.getActiveCaseCategoriesByCatalogId(
                                caseCus.getCataglogId(), userSessionSrv.getDestinationDetails4mUserSession()));
                        if (CollectionUtils.isNotEmpty(caseCatgTree.getCategories()))
                        {
                            // add to Container - for subsequent calls
                            if (caseCatgContainer == null)
                            {
                                caseCatgContainer = new ArrayList<TY_CatalogTree>();
                            }

                            if (caseCFgO.get().getToplvlCatgOnly())
                            {
                                List<TY_CatalogItem> toplvlCatgs = caseCatgTree.getCategories().stream()
                                        .filter(c -> c.getParentId() == null).collect(Collectors.toList());
                                if (CollectionUtils.isNotEmpty(toplvlCatgs))
                                {
                                    caseCatgTree.setCategories(toplvlCatgs);
                                }
                            }

                            // Categories Sort Enabled
                            if (caseCFgO.get().getCatgRankEnabled())
                            {
                                List<TY_CatalogItem> catgItems = prepareRankedCatgTree(caseCatgTree, caseType);
                                caseCatgTree.setCategories(catgItems);
                            }

                            this.caseCatgContainer.add(caseCatgTree);

                        }

                    }
                }

            }
            catch (Exception e)
            {
                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CATG_LOAD", new Object[]
                { caseCFgO.get().getCatgCsvPath(), caseType.toString() }, Locale.ENGLISH));
            }

        }

        else
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
            { caseType.toString() }, Locale.ENGLISH));
        }

        if (CollectionUtils.isNotEmpty(caseCatgTree.getCategories()))
        {
            caseCatgTree.getCategories().add(0, new TY_CatalogItem());
        }
        return caseCatgTree;
    }

    private List<TY_CatalogItem> prepareRankedCatgTree(TY_CatalogTree caseCatgTree, EnumCaseTypes caseType)
    {
        List<TY_CatalogItem> catgsSorted = new ArrayList<TY_CatalogItem>();

        if (catgRanks != null)
        {
            if (CollectionUtils.isNotEmpty(catgRanks.getCatgRankItems()))
            {
                // Get Categories for Current CaseType
                List<TY_CatgRanksItem> currLoBCatgRanks = catgRanks.getCatgRankItems().stream()
                        .filter(c -> c.getCaseTypeEnum().equals(caseType)).collect(Collectors.toList());

                if (CollectionUtils.isNotEmpty(currLoBCatgRanks))
                {
                    // Sort by Rank
                    currLoBCatgRanks.sort(Comparator.comparing(TY_CatgRanksItem::getRank));

                    // Get List of Categories from Catg tree Excluding the TopN
                    List<TY_CatalogItem> catgsExclTopN = new ArrayList<TY_CatalogItem>();
                    catgsExclTopN.addAll(caseCatgTree.getCategories());

                    catgsExclTopN.removeIf(topN -> currLoBCatgRanks.stream()
                            .anyMatch(cCatg -> cCatg.getCatg().equals(topN.getName())));

                    // Prepare the new List
                    for (TY_CatgRanksItem catgRank : currLoBCatgRanks)
                    {
                        Optional<TY_CatalogItem> catgItemO = caseCatgTree.getCategories().stream()
                                .filter(c -> c.getName().equals(catgRank.getCatg())).findFirst();
                        if (catgItemO.isPresent())
                        {
                            catgsSorted.add(catgItemO.get());
                        }

                    }

                    // Append TopN Excluded Categories to Sorted List
                    if (CollectionUtils.isNotEmpty(catgsExclTopN))
                    {
                        catgsSorted.addAll(catgsExclTopN);
                    }

                }
                else
                {
                    catgsSorted = caseCatgTree.getCategories();
                }

            }
        }

        return catgsSorted;
    }

}
