package com.sap.cap.esmapi.catg.srv.impl;

import java.util.ArrayList;
import java.util.Collections;
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
import com.sap.cap.esmapi.catg.pojos.TY_Catg2Templates;
import com.sap.cap.esmapi.catg.pojos.TY_Catg2TemplatesCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgDetails;
import com.sap.cap.esmapi.catg.pojos.TY_CatgRanks;
import com.sap.cap.esmapi.catg.pojos.TY_CatgRanksItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplates;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplatesCus;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.StringsUtility;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.pojos.TY_CaseCatalogCustomizing;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;

import lombok.extern.slf4j.Slf4j;

@Service
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Slf4j
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
    private TY_Catg2TemplatesCus catg2TmplCus;

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

    // Get Complete Category Hierarchy passing most granular Category Id and Case
    // Type
    @Override
    public String[] getCatgHierarchyforCatId(String catId, EnumCaseTypes caseType, boolean addBlank) throws EX_ESMAPI
    {
        String[] catTree = null;
        int idx = 0;
        List<TY_CatalogItem> catgTreeAll = null;

        if (StringUtils.hasText(catId) && caseType != null)
        {
            String catCurr = catId;
            catgTreeAll = new ArrayList<TY_CatalogItem>();
            // Get Complete Catalog Details
            TY_CatalogTree catalogTree = this.getCaseCatgTree4LoB(caseType);
            if (CollectionUtils.isNotEmpty(catalogTree.getCategories()))
            {
                catTree = new String[maxCatgLevels]; // Max upto 4 levels
                while (StringUtils.hasText(catCurr))
                {
                    String catScan = catCurr;
                    // Remove blank Categories from Catalog Tree Used for UI Presentation
                    // Add level 1 and level 2 categories to single list for scanning the category
                    // hierarchy for selected category id
                    catalogTree.getCategories().removeIf(x -> x.getId() == null);
                    catgTreeAll.addAll(catalogTree.getCategories());
                    catgTreeAll.addAll(catalogTree.getCategorieslvl2());
                    // Scan for Category in Catalog Tree
                    Optional<TY_CatalogItem> itemSel = catgTreeAll.stream().filter(t -> t.getId().equals(catScan))
                            .findFirst();
                    if (itemSel.isPresent())
                    {
                        log.info("Category found in Catalog Tree for category id : " + catScan
                                + " with category name : " + itemSel.get().getName());
                        catTree[idx] = catCurr;
                        log.info("Category added to category hierarchy array at index : " + idx + " with category id : "
                                + catCurr);
                        // Seek Parent
                        if (StringUtils.hasText(itemSel.get().getParentId()))
                        {
                            log.info("Seeking parent category for category id : " + catCurr
                                    + " with parent category id : " + itemSel.get().getParentId());
                            catCurr = itemSel.get().getParentId();
                        }
                        else
                        {
                            catCurr = null;
                        }

                        idx++;
                    }

                }
                StringsUtility.reverseArray(catTree);
                for (int i = 0; i < catTree.length; i++)
                {
                    log.info("Category Hierarchy Array at index : " + i + " has category id : " + catTree[i]);
                }
                if (addBlank)
                {
                    // Refurbish Blank Category at Top for New Form - Session maintained
                    catalogTree.getCategories().add(0, new TY_CatalogItem());
                }

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
    public TY_CatgDetails getCategoryDetails4Catg(String catId, EnumCaseTypes caseType, boolean inUpperCase,
            boolean isCatg2) throws EX_ESMAPI
    {
        TY_CatgDetails catgDetails = null;

        if (StringUtils.hasText(catId) && CollectionUtils.isNotEmpty(catgTmplCus.getCatgTemplates())

                && caseType != null)
        {
            TY_CatalogTree catgTree = this.getCaseCatgTree4LoB(caseType);
            boolean isLvl1 = false;
            if (CollectionUtils.isNotEmpty(catgTree.getCategories()))
            {
                if (!isCatg2)
                {

                    log.info("Scanning Category details for level 1 category ..  " + catId);
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

                }
                else // Category 2 Details
                {
                    log.info("Scanning Category details for level 2 category ..  " + catId);
                    // Remove blank Categories from Catalog Tree Used for UI Presentation
                    catgTree.getCategorieslvl2().removeIf(x -> x.getId() == null);

                    Optional<TY_CatalogItem> currCatgDetailsO = catgTree.getCategorieslvl2().stream()
                            .filter(f -> f.getId().equals(catId)).findFirst();
                    if (currCatgDetailsO.isPresent())
                    {
                        catgDetails = new TY_CatgDetails();
                        // 1. Get Text from Catg Guid selected in form and Convert to Upper Case
                        String catgTxt = null;

                        // No Level 1 Catg. is valid . Hence do not seek for level 1 catg.
                        if (inUpperCase)
                        {
                            catgTxt = currCatgDetailsO.get().getName().toUpperCase();
                        }
                        else
                        {
                            catgTxt = currCatgDetailsO.get().getName();
                        }

                        isLvl1 = false;

                        catgDetails.setCatDesc(catgTxt);
                        catgDetails.setCatgId(catId);
                        catgDetails.setInUpperCase(inUpperCase);
                        catgDetails.setRoot(isLvl1);

                    }

                    // Refurbish Blank Category at Top for New Form - Session maintained
                    catgTree.getCategories().add(0, new TY_CatalogItem());
                }
            }
        }

        return catgDetails;

    }

    @Override
    public List<TY_CatalogItem> getCategoryLvl2ByRootCatgId(String... rootCatgId) throws EX_ESMAPI
    {
        List<TY_CatalogItem> lvl2Catgs = null;
        Optional<TY_CatgCusItem> caseCFgO = catgCus.getCustomizations().stream()
                .filter(g -> g.getCaseTypeEnum().toString().equals(EnumCaseTypes.Learning.toString())).findFirst();
        if (caseCFgO.isPresent())
        {

            if (rootCatgId == null || rootCatgId.length == 0)
            {
                if (caseCatgContainer == null)
                    return Collections.emptyList();
                else
                {
                    log.info("Seeking Level 2 Categories for All Root Categories as Root Category Id is Blank/Null");
                    return caseCatgContainer.stream()
                            .filter(c -> c.getCaseTypeEnum().equals(caseCFgO.get().getCaseTypeEnum()))
                            .flatMap(c -> c.getCategorieslvl2().stream()).collect(Collectors.toList());
                }
            }
            else
            {
                log.info("Seeking Level 2 Categories for Root Category Id :" + rootCatgId[0]);
                lvl2Catgs = caseCatgContainer.stream()
                        .filter(c -> c.getCaseTypeEnum().equals(caseCFgO.get().getCaseTypeEnum()))
                        .flatMap(c -> c.getCategorieslvl2().stream()).filter(c -> StringUtils.hasText(c.getParentId())
                                && StringUtils.hasText(c.getId()) && (c.getParentId().equals(rootCatgId[0])))
                        .collect(Collectors.toList());
            }

        }
        log.info("Number of Level 2 Categories found for Root Category Id :"
                + (rootCatgId != null && rootCatgId.length > 0 ? rootCatgId[0] : "All Root Categories") + " are : "
                + (lvl2Catgs != null ? lvl2Catgs.size() : 0));
        return lvl2Catgs;

    }

    @Override
    public List<TY_CatalogItem> getCategoryLvl2ByRootCatgDesc(String Catg1Desc) throws EX_ESMAPI
    {
        if (StringUtils.hasText(Catg1Desc) && caseCatgContainer != null)
        {
            log.info("Seeking Level 2 Categories for Root Category Description :" + Catg1Desc);
            return caseCatgContainer.stream().filter(c -> c.getCaseTypeEnum().equals(EnumCaseTypes.Learning))
                    .flatMap(c -> c.getCategorieslvl2().stream()).filter(c -> StringUtils.hasText(c.getParentName())
                            && StringUtils.hasText(c.getId()) && (c.getParentName().equals(Catg1Desc)))
                    .collect(Collectors.toList());
        }
        else
        {
            log.info(
                    "Root Category Description is Blank/Null. Hence Seeking Level 2 Categories for All Root Categories");
            return caseCatgContainer.stream().filter(c -> c.getCaseTypeEnum().equals(EnumCaseTypes.Learning))
                    .flatMap(c -> c.getCategorieslvl2().stream()).collect(Collectors.toList());
        }
    }

    @Override
    public TY_Catg2Templates getTemplates4Catg2(String cat1Id, String cat2Id) throws EX_ESMAPI
    {
        TY_Catg2Templates catg2Tmpl = null;
        if (StringUtils.hasText(cat1Id) && StringUtils.hasText(cat2Id)
                && CollectionUtils.isNotEmpty(catg2TmplCus.getCatgTemplates()))
        {
            String catg1TxtToSearch = cat1Id.toUpperCase();
            String catg2TxtToSearch = cat2Id.toUpperCase();
            try
            {
                log.info("Scanning Category 2 Template for category 1 : " + catg1TxtToSearch + " and category 2 : "
                        + catg2TxtToSearch);
                Optional<TY_Catg2Templates> catgTmplO = catg2TmplCus.getCatgTemplates().stream()
                        .filter(t -> t.getLvl1().equals(catg1TxtToSearch) && t.getLvl2().equals(catg2TxtToSearch))
                        .findFirst();

                if (catgTmplO.isPresent())
                {
                    catg2Tmpl = catgTmplO.get();
                    log.info("Category 2 Template found for category 1 : " + catg1TxtToSearch + " and category 2 : "
                            + catg2TxtToSearch);
                }

            }
            catch (NullPointerException e)
            {
                // Do Nothing - No template Relevant Category selected
            }

        }

        return catg2Tmpl;
    }

    private TY_CatalogTree loadCatgTree4CaseType(EnumCaseTypes caseType)
    {

        TY_CatalogTree caseCatgTree = null;
        TY_CatalogTree caseCatgTreeCopy = null;

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
                        caseCatgTree = new TY_CatalogTree(caseType,
                                srvCloudApiSrv.getActiveCaseCategoriesByCatalogId(caseCus.getCataglogId(),
                                        userSessionSrv.getDestinationDetails4mUserSession()),
                                new ArrayList<TY_CatalogItem>());
                        if (caseCatgTree != null) // Category Tree Deep Copy
                        {
                            caseCatgTreeCopy = new TY_CatalogTree();
                            List<TY_CatalogItem> copy = new ArrayList<>(caseCatgTree.getCategories().size());
                            for (TY_CatalogItem item : caseCatgTree.getCategories())
                            {
                                copy.add(new TY_CatalogItem(item.getId(), item.getName(), item.getParentId(),
                                        item.getParentName())); // deep copy
                            }

                            caseCatgTreeCopy.setCaseTypeEnum(caseCatgTree.getCaseTypeEnum());
                            caseCatgTreeCopy.setCategories(copy);
                        }

                        if (CollectionUtils.isNotEmpty(caseCatgTree.getCategories()))
                        {
                            // add to Container - for subsequent calls
                            if (caseCatgContainer == null)
                            {
                                caseCatgContainer = new ArrayList<TY_CatalogTree>();
                            }

                            // Retain only Top Level Categories as Step 1
                            List<TY_CatalogItem> toplvlCatgs = caseCatgTree.getCategories().stream()
                                    .filter(c -> c.getParentId() == null).collect(Collectors.toList());
                            if (CollectionUtils.isNotEmpty(toplvlCatgs))
                            {
                                caseCatgTree.setCategories(toplvlCatgs);
                            }

                            // Categories Sort Enabled
                            if (caseCFgO.get().getCatgRankEnabled())
                            {
                                List<TY_CatalogItem> catgItems = prepareRankedCatgTree(caseCatgTree, caseType);
                                caseCatgTree.setCategories(catgItems);
                            }

                            // Now load the level 2 Categories for the Catalog - As part of loading the
                            // Catalog Tree for Case Type to avoid multiple calls to Srv Cloud while
                            // navigating in Case form and seeking for category details

                            if (CollectionUtils.isNotEmpty(caseCatgTree.getCategories()))
                            {
                                for (TY_CatalogItem lvl1Catg : caseCatgTree.getCategories())
                                {
                                    List<TY_CatalogItem> catgLvl2 = caseCatgTreeCopy.getCategories().stream().filter(
                                            c -> c.getParentId() != null && c.getParentId().equals(lvl1Catg.getId()))
                                            .collect(Collectors.toList());
                                    if (CollectionUtils.isNotEmpty(catgLvl2))
                                    {
                                        log.info("Loading Level 2 Categories for Category :" + lvl1Catg.getName()
                                                + " and Case Type :" + caseType.toString() + " with Count :"
                                                + catgLvl2.size());
                                        caseCatgTree.getCategorieslvl2().addAll(catgLvl2);
                                    }
                                }
                            }

                            // Finally add the Catalog Tree to Container for Session Maintenance
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
            caseCatgTree.getCategorieslvl2().add(0, new TY_CatalogItem());
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
