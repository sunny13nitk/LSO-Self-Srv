package com.sap.cap.esmapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;

@Configuration
@PropertySources(
{ @PropertySource("classpath:messages.properties"), @PropertySource("classpath:appconfig.properties") })
public class PropertyConfig
{

	@Bean
	public static PropertySourcesPlaceholderConfigurer properties()
	{
		PropertySourcesPlaceholderConfigurer pSConf = new PropertySourcesPlaceholderConfigurer();
		return pSConf;
	}

	@Bean
	public ResourceBundleMessageSource messageSource()
	{

		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.addBasenames("messages");
		source.setUseCodeAsDefaultMessage(true);

		return source;
	}

	@Bean
	@Autowired // For PropertySourcesPlaceholderConfigurer
	public TY_RLConfig RatelimitConfigLoad(@Value("${numFormSubms}") final int numFormSubms,
			@Value("${intvSecs}") final long intvSecs, @Value("${allowedAttachments}") final String allowedAttachments,
			@Value("${allowedSizeAttachmentMB}") final long allowedSizeAttachmentMB,
			@Value("${internalUsers}") final String internalUsersRegex,
			@Value("${techUserRegex}") final String techUserRegex,
			@Value("${allowPrevAttDL}") final boolean allowPrevAttDL,
			@Value("${enableDestinationCheck}") final boolean enableDestinationCheck)
	{
		TY_RLConfig rlConfig = new TY_RLConfig(numFormSubms, intvSecs, allowedAttachments, allowedSizeAttachmentMB,
				internalUsersRegex, techUserRegex, allowPrevAttDL, enableDestinationCheck);
		return rlConfig;
	}

}