package com.sap.cap.esmapi.config;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBeanBuilder;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgRanks;
import com.sap.cap.esmapi.catg.pojos.TY_CatgRanksItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplates;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplatesCus;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransI;
import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransitions;
import com.sap.cap.esmapi.vhelps.cus.TY_Catg_MandatoryFlds;
import com.sap.cap.esmapi.vhelps.cus.TY_VHelpsRoot;
import com.sap.cap.esmapi.vhelps.pojos.TY_CatgCountryMapping;
import com.sap.cap.esmapi.vhelps.pojos.TY_CatgCountryMappingList;
import com.sap.cap.esmapi.vhelps.pojos.TY_CountryLangaugeMapping;
import com.sap.cap.esmapi.vhelps.pojos.TY_CountryLangaugeMappingsList;
import com.sap.cap.esmapi.vhelps.pojos.TY_MandatoryFlds_CatgsList;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AppInitializrConfig
{
    private final String configPath = "/configCatg/config.csv";
    private final String configCatgTemplates = "/configCatg/templates.csv";
    private final String configPathVHelpsJSON = "/vhelps/VHelps.json";
    private final String configCatgCountryMandatory = "/configCatg/Mandatory_Country_Catg.csv";
    private final String configCatgLanguageMandatory = "/configCatg/Mandatory_Language_Catg.csv";
    private final String configStatusTransition = "/configCatg/statusTransitions.csv";
    private final String countryLanguMappings = "/configCatg/CountryLanguageMappings.csv";
    private final String configCatgRanks = "/configCatg/catgRanks.csv";
    private final String countryByCatgs = "/configCatg/CountriesByCatg.csv";

    @Autowired
    private MessageSource msgSrc;

    @Bean
    public TY_CatgCus loadCaseTypes4mConfig()
    {
        TY_CatgCus caseCus = null;

        try
        {

            ClassPathResource classPathResource = new ClassPathResource(configPath);
            if (classPathResource != null)
            {
                Reader reader = new InputStreamReader(classPathResource.getInputStream());
                if (reader != null)
                {
                    log.info("Resource Bound... ");
                    List<TY_CatgCusItem> configs = new CsvToBeanBuilder(reader).withSkipLines(1)
                            .withType(TY_CatgCusItem.class).build().parse();

                    if (!CollectionUtils.isEmpty(configs))
                    {
                        log.info("Entries in Config Found for Case Types Config. : " + configs.size());
                        caseCus = new TY_CatgCus(configs);
                    }
                }
            }

        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASETYPE_CFG", new Object[]
            { configPath, e.getLocalizedMessage() }, Locale.ENGLISH));
        }

        return caseCus;
    }

    @Bean
    public TY_CatgRanks loadCatgRanks4mConfig()
    {
        TY_CatgRanks catgRanksCus = null;

        try
        {

            ClassPathResource classPathResource = new ClassPathResource(configCatgRanks);
            if (classPathResource != null)
            {
                Reader reader = new InputStreamReader(classPathResource.getInputStream());
                if (reader != null)
                {
                    log.info("Resource Bound... ");
                    List<TY_CatgRanksItem> configs = new CsvToBeanBuilder(reader).withSkipLines(1)
                            .withType(TY_CatgRanksItem.class).build().parse();

                    if (!CollectionUtils.isEmpty(configs))
                    {
                        log.info("Entries in Config. Found for Case Categories and Relative Ranks: " + configs.size());
                        catgRanksCus = new TY_CatgRanks(configs);
                    }
                }
            }

        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASETYPE_CFG", new Object[]
            { configPath, e.getLocalizedMessage() }, Locale.ENGLISH));
        }

        return catgRanksCus;
    }

    @Bean
    public TY_CatgTemplatesCus loadTemplatesCatg4mConfig()
    {
        TY_CatgTemplatesCus catgTempCus = null;

        try
        {

            ClassPathResource classPathResource = new ClassPathResource(configCatgTemplates);
            if (classPathResource != null)
            {
                Reader reader = new InputStreamReader(classPathResource.getInputStream());
                if (reader != null)
                {
                    log.info("Resource Bound... ");
                    List<TY_CatgTemplates> configs = new CsvToBeanBuilder(reader).withSkipLines(1)
                            .withType(TY_CatgTemplates.class).build().parse();

                    if (!CollectionUtils.isEmpty(configs))
                    {
                        log.info("Entries in Config. Found for Case Categories and Templates: " + configs.size());
                        catgTempCus = new TY_CatgTemplatesCus(configs);
                    }
                }
            }

        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASETYPE_CFG", new Object[]
            { configPath, e.getLocalizedMessage() }, Locale.ENGLISH));
        }

        return catgTempCus;
    }

    @Bean
    public TY_VHelpsRoot loadLOBVHelpsCus()
    {
        TY_VHelpsRoot vHelpCus = null;

        try
        {
            ObjectMapper om = new ObjectMapper();

            ClassPathResource classPathResource = new ClassPathResource(configPathVHelpsJSON);
            if (classPathResource != null && om != null)
            {
                Reader reader = new InputStreamReader(classPathResource.getInputStream());
                if (reader != null)
                {

                    vHelpCus = om.readValue(reader, TY_VHelpsRoot.class);
                    log.info("LOB Custom Fields Customization Loaded!");

                }

            }
        }
        catch (Exception e)
        {
            log.error("LOB Custom Fields Customization Could not be Loaded!");
        }
        return vHelpCus;
    }

    @Bean("Catgs_LSO_Country")
    public TY_MandatoryFlds_CatgsList loadCountryMandatoryCatgs()
    {
        TY_MandatoryFlds_CatgsList catgCountryMandList = null;

        try
        {

            ClassPathResource classPathResource = new ClassPathResource(configCatgCountryMandatory);
            if (classPathResource != null)
            {
                Reader reader = new InputStreamReader(classPathResource.getInputStream());
                if (reader != null)
                {
                    log.info("Resource Bound... ");
                    List<TY_Catg_MandatoryFlds> configs = new CsvToBeanBuilder(reader).withSkipLines(1)
                            .withType(TY_Catg_MandatoryFlds.class).build().parse();

                    if (!CollectionUtils.isEmpty(configs))
                    {
                        log.info("Entries in Config. Found for Case Categories that have a Mandatory Country Field: "
                                + configs.size());
                        catgCountryMandList = new TY_MandatoryFlds_CatgsList(configs);
                    }
                }
            }

        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASETYPE_CFG", new Object[]
            { configPath, e.getLocalizedMessage() }, Locale.ENGLISH));
        }

        return catgCountryMandList;
    }

    @Bean("Catgs_LSO_Language")
    public TY_MandatoryFlds_CatgsList loadLanguageMandatoryCatgs()
    {
        TY_MandatoryFlds_CatgsList catgLangMandList = null;

        try
        {

            ClassPathResource classPathResource = new ClassPathResource(configCatgLanguageMandatory);
            if (classPathResource != null)
            {
                Reader reader = new InputStreamReader(classPathResource.getInputStream());
                if (reader != null)
                {
                    log.info("Resource Bound... ");
                    List<TY_Catg_MandatoryFlds> configs = new CsvToBeanBuilder(reader).withSkipLines(1)
                            .withType(TY_Catg_MandatoryFlds.class).build().parse();

                    if (!CollectionUtils.isEmpty(configs))
                    {
                        log.info("Entries in Config. Found for Case Categories that have a Mandatory Language Field: "
                                + configs.size());
                        catgLangMandList = new TY_MandatoryFlds_CatgsList(configs);

                    }
                }
            }

        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASETYPE_CFG", new Object[]
            { configPath, e.getLocalizedMessage() }, Locale.ENGLISH));
        }

        return catgLangMandList;
    }

    @Bean
    public TY_CountryLangaugeMappingsList loadCountryanguageMappings()
    {
        TY_CountryLangaugeMappingsList countryLanguMappingsList = null;

        try
        {

            ClassPathResource classPathResource = new ClassPathResource(countryLanguMappings);
            if (classPathResource != null)
            {
                Reader reader = new InputStreamReader(classPathResource.getInputStream());
                if (reader != null)
                {
                    log.info("Resource Bound... ");
                    List<TY_CountryLangaugeMapping> configs = new CsvToBeanBuilder(reader).withSkipLines(1)
                            .withType(TY_CountryLangaugeMapping.class).build().parse();

                    if (!CollectionUtils.isEmpty(configs))
                    {
                        log.info("Entries in Config. Found for Country and Language Mappings: " + configs.size());
                        countryLanguMappingsList = new TY_CountryLangaugeMappingsList(configs);
                    }
                }
            }

        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASETYPE_CFG", new Object[]
            { configPath, e.getLocalizedMessage() }, Locale.ENGLISH));
        }

        return countryLanguMappingsList;
    }

    @Bean
    public TY_CatgCountryMappingList loadCountryCatgMappings()
    {
        TY_CatgCountryMappingList catgCountryMappingsList = null;

        try
        {

            ClassPathResource classPathResource = new ClassPathResource(countryByCatgs);
            if (classPathResource != null)
            {
                Reader reader = new InputStreamReader(classPathResource.getInputStream());
                if (reader != null)
                {
                    log.info("Resource Bound... ");
                    List<TY_CatgCountryMapping> configs = new CsvToBeanBuilder(reader).withSkipLines(1)
                            .withType(TY_CatgCountryMapping.class).build().parse();

                    if (!CollectionUtils.isEmpty(configs))
                    {
                        log.info("Entries in Config. Found for Categories and Country Mappings: " + configs.size());
                        catgCountryMappingsList = new TY_CatgCountryMappingList(configs);
                    }
                }
            }

        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASETYPE_CFG", new Object[]
            { configPath, e.getLocalizedMessage() }, Locale.ENGLISH));
        }

        return catgCountryMappingsList;
    }

    @Bean
    public TY_PortalStatusTransitions loadStatusTransitions4mConfig()
    {
        TY_PortalStatusTransitions statusTransCus = null;

        try
        {

            ClassPathResource classPathResource = new ClassPathResource(configStatusTransition);
            if (classPathResource != null)
            {
                Reader reader = new InputStreamReader(classPathResource.getInputStream());
                if (reader != null)
                {
                    log.info("Resource Bound... ");
                    List<TY_PortalStatusTransI> configs = new CsvToBeanBuilder(reader).withSkipLines(1)
                            .withType(TY_PortalStatusTransI.class).build().parse();

                    if (!CollectionUtils.isEmpty(configs))
                    {
                        log.info("Entries in Config Found for Case Status Transitions. : " + configs.size());
                        statusTransCus = new TY_PortalStatusTransitions(configs);
                    }
                }
            }

        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CFG_STATUSTRANS", new Object[]
            { e.getLocalizedMessage() }, Locale.ENGLISH));
        }

        return statusTransCus;
    }

}
