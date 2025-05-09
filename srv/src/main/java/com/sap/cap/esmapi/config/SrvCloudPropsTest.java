package com.sap.cap.esmapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.pojos.TY_SrvCloudUrls;

@Configuration
@Profile(GC_Constants.gc_LocalProfile)
@PropertySources(
{ @PropertySource("classpath:srvcloudurls-testblank.properties") })
public class SrvCloudPropsTest
{
    @Bean
    @Autowired // For PropertySourcesPlaceholderConfigurer
    public TY_SrvCloudUrls SrvCloudUrls(@Value("${username}") final String userName,
            @Value("${password}") final String password, @Value("${usernameext}") final String userNameExt,
            @Value("${passwordext}") final String passwordExt, @Value("${casesurl}") final String casesUrl,
            @Value("${cpurl}") final String cpUrl, @Value("${accountsurl}") final String acUrl,
            @Value("${notesurl}") final String notesUrl, @Value("${notesreadurl}") final String notesreadUrl,
            @Value("${topN}") final String topN, @Value("${caseTemplateUrl}") final String caseTemplateUrl,
            @Value("${catgTreeUrl}") final String catgTreeUrl, @Value("${docSrvUrl}") final String docSrvUrl,
            @Value("${emplSrvUrl}") final String emplSrvUrl, @Value("${vhlpUrl}") final String vhlpUrl,
            @Value("${caseDetailsUrl}") final String caseDetailsUrl,
            @Value("${statusCfgUrl}") final String statusCfgUrl, @Value("${accByEmail}") final String accByEmailUrl,
            @Value("${conByEmail}") final String conByEmailUrl, @Value("${empById}") final String empByIdUrl,
            @Value("${casesByAcc}") final String casesByAccUrl, @Value("${casesByEmpl}") final String casesByEmplUrl,
            @Value("${customerurl}") final String customerUrl, @Value("${prevAtt}") final String prevAtt,
            @Value("${dlAtt}") final String dlAtt, @Value("${baseUrl}") final String baseUrl,
            @Value("${token}") final String token,
            @Value("${mimeTypesUrlPathString}") final String mimeTypesUrlPathString

    )

    {
        TY_SrvCloudUrls srvClUrls = new TY_SrvCloudUrls(userName, password, userNameExt, passwordExt, casesUrl, cpUrl,
                acUrl, notesUrl, notesreadUrl, topN, caseTemplateUrl, catgTreeUrl, docSrvUrl, emplSrvUrl, vhlpUrl,
                caseDetailsUrl, statusCfgUrl, accByEmailUrl, conByEmailUrl, empByIdUrl, casesByAccUrl, casesByEmplUrl,
                customerUrl, prevAtt, dlAtt, baseUrl, token, mimeTypesUrlPathString);

        return srvClUrls;
    }
}
