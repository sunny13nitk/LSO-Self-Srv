package com.sap.cap.esmapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cloud.security.spring.config.IdentityServicesPropertySourceFactory;

@Configuration
@Profile(GC_Constants.gc_LocalProfile)
@EnableWebSecurity()
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@PropertySource(factory = IdentityServicesPropertySourceFactory.class, ignoreResourceNotFound = true, value =
{ "" })
public class AppSecurityConfigLocal
{

    @Bean
    @Profile(GC_Constants.gc_LocalProfile)
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception
    {
        // @formatter:off
        /*
         * ----------- Local Testing --------------------
        */

    // @formatter:off
            http
                .logout(logout -> logout
                    .logoutSuccessUrl("/logout/")
                    .permitAll()
                )
                .csrf(csrf -> csrf.disable())   // don't insist on csrf tokens in put, post etc.
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/static/**").permitAll()
                    .requestMatchers("/api/**").permitAll()
                    .requestMatchers("/poclocal/**").permitAll()
                    .anyRequest().denyAll()
                );
// @formatter:on

        // @formatter:on

        // @formatter:on
        return http.build();

    }

    /*
     * WEB REsources Whitelisting
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() throws Exception
    {
        // @formatter:off
        return (web) -> web.ignoring()
                    .requestMatchers("/static/**")
                    .requestMatchers("/images/**")
                    .requestMatchers("/css/**")
                    .requestMatchers("/js/**")
                    .requestMatchers("/logout/**");
        // @formatter:on
    }

}
