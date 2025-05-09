package com.sap.cap.esmapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ResourceConfig implements WebMvcConfigurer
{

    private static final String[] CLASSPATH_RESOURCE_LOCATIONS =
    { "classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/resources/static/",
            "classpath:/web-components.js" };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        registry.addResourceHandler("/**").addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);
    }

    // @Override
    // public void addResourceHandlers(ResourceHandlerRegistry registry)
    // {
    // registry.addResourceHandler("/images/**", "/css/**", "/js/**",
    // "/static/**").addResourceLocations(
    // "classpath:/static/images/", "classpath:/static/css/",
    // "classpath:/static/js/", "classpath:/static/");
    // }

}
