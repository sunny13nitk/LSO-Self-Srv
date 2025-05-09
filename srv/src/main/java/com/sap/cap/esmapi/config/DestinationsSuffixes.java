package com.sap.cap.esmapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import com.sap.cap.esmapi.utilities.pojos.TY_DestinationsSuffix;

//Dedstinations Suffixes
@Configuration
@PropertySources(
{ @PropertySource("classpath:DestinationsSuffixes.properties") })
public class DestinationsSuffixes
{

    @Bean
    @Autowired // For PropertySourcesPlaceholderConfigurer
    public TY_DestinationsSuffix Destinations(@Value("${casesurlPathString}") final String casesUrlPathString,
            @Value("${cpurlPathString}") final String cpUrlPathString,
            @Value("${accountsurlPathString}") final String acUrlPathString,
            @Value("${notesurlPathString}") final String notesUrlPathString,
            @Value("${notesReadPathString}") final String notesReadPathString,
            @Value("${topNPathString}") final String topNPathString,
            @Value("${caseTemplateUrlPathString}") final String caseTemplateUrlPathString,
            @Value("${catgTreeUrlPathString}") final String catgTreeUrlPathString,
            @Value("${docSrvUrlPathString}") final String docSrvUrlPathString,
            @Value("${emplSrvUrlPathString}") final String emplSrvUrlPathString,
            @Value("${vhlpUrlPathString}") final String vhlpUrlPathString,
            @Value("${caseDetailsUrlPathString}") final String caseDetailsUrlPathString,
            @Value("${statusCfgUrlPathString}") final String statusCfgUrlPathString,
            @Value("${accByEmailPathString}") final String accByEmailUrlPathString,
            @Value("${conByEmailPathString}") final String conByEmailUrlPathString,
            @Value("${empByIdPathString}") final String empByIdUrlPathString,
            @Value("${casesByAccPathString}") final String casesByAccUrlPathString,
            @Value("${casesByEmplPathString}") final String casesByEmplUrlPathString,
            @Value("${customerurlPathString}") final String customerUrlPathString,
            @Value("${prevAttPathString}") final String prevAttPathString,
            @Value("${dlAttPathString}") final String dlAttPathString,
            @Value("${destInternal}") final String destInternal, @Value("${destExternal}") final String destExternal,
            @Value("${destQualtrics}") final String destQualtrics,
            @Value("${mimeTypesUrlPathString}") final String mimeTypesUrlPathString

    )

    {
        TY_DestinationsSuffix destinationsSuffixes = new TY_DestinationsSuffix(casesUrlPathString, cpUrlPathString,
                acUrlPathString, notesUrlPathString, notesReadPathString, topNPathString, caseTemplateUrlPathString,
                catgTreeUrlPathString, docSrvUrlPathString, emplSrvUrlPathString, vhlpUrlPathString,
                caseDetailsUrlPathString, statusCfgUrlPathString, accByEmailUrlPathString, conByEmailUrlPathString,
                empByIdUrlPathString, casesByAccUrlPathString, casesByEmplUrlPathString, customerUrlPathString,
                prevAttPathString, dlAttPathString, destInternal, destExternal, destQualtrics, mimeTypesUrlPathString);

        return destinationsSuffixes;
    }

}
