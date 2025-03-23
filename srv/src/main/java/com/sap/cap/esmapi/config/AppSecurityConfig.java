package com.sap.cap.esmapi.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive;

import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cloud.security.spring.config.IdentityServicesPropertySourceFactory;
import com.sap.cloud.security.spring.token.authentication.AuthenticationToken;
import com.sap.cloud.security.token.TokenClaims;

@Configuration
@Profile(GC_Constants.gc_BTPProfile)
@EnableWebSecurity()
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@PropertySource(factory = IdentityServicesPropertySourceFactory.class, ignoreResourceNotFound = true, value =
{ "" })
public class AppSecurityConfig
{

    @Autowired
    Converter<Jwt, AbstractAuthenticationToken> authConverter; // Required only when Xsuaa is used

    /*
     * WEB REsources Whitelisting for App
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

    @Bean
    @Profile(GC_Constants.gc_BTPProfile)
    public SecurityFilterChain appFilterChainforTest(HttpSecurity http) throws Exception
    {

        // /*
        // * ----------- CF Deployment --------------------
        // */

    // @formatter:off
    HeaderWriterLogoutHandler clearSiteData = new HeaderWriterLogoutHandler(
        new ClearSiteDataHeaderWriter(Directive.ALL));

            http
                .logout((logout) -> logout.logoutSuccessUrl("/logout/").permitAll())
                .logout((logout) -> logout.addLogoutHandler(clearSiteData))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz ->
                           authz
                                .requestMatchers("/login/**").permitAll()
                                .requestMatchers("/static/**").permitAll()
                                .requestMatchers("/err/**").permitAll()
                                .requestMatchers("/web-component.js/**").permitAll()
                                .requestMatchers("/lso/**").hasAnyAuthority(GC_Constants.gc_role_employee_lso, GC_Constants.gc_role_contractor_lso)
                                .requestMatchers("/post/**").hasAnyAuthority(GC_Constants.gc_role_employee_lso, GC_Constants.gc_role_contractor_lso)
                                .requestMatchers("/*").authenticated()
                                .anyRequest().denyAll())
                                .exceptionHandling(ex->ex.accessDeniedHandler(accessDeniedHandler()))
                                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new MyCustomHybridTokenAuthenticationConverter())));
   

        return http.build();

    }


    

    @Bean
    public AccessDeniedHandler accessDeniedHandler()
    {
        return new CustomAccessDeniedHandler();
    }

    // Configure the CSRF token repository 
    private CsrfTokenRepository csrfTokenRepository() 
    { 
        // Create a new HttpSessionCsrfTokenRepository 
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository(); 
        // Set the session attribute name for the CSRF token 
        repository.setSessionAttributeName("_csrf"); 
        // Return the repository 
        return repository; 
    } 

    /**
     * Workaround for hybrid use case until Cloud Authorization Service is globally
     * available.
     */
    class MyCustomHybridTokenAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken>
    {

        public AbstractAuthenticationToken convert(Jwt jwt)
        {
            if (jwt.hasClaim(TokenClaims.XSUAA.EXTERNAL_ATTRIBUTE))
            {
                return authConverter.convert(jwt);
            }
            return new AuthenticationToken(jwt, deriveAuthoritiesFromGroup(jwt));
        }

        private Collection<GrantedAuthority> deriveAuthoritiesFromGroup(Jwt jwt)
        {
            Collection<GrantedAuthority> groupAuthorities = new ArrayList<>();
            if (jwt.hasClaim(TokenClaims.GROUPS))
            {
                List<String> groups = jwt.getClaimAsStringList(TokenClaims.GROUPS);
                for (String group : groups)
                {
                    groupAuthorities.add(new SimpleGrantedAuthority(group.replace("IASAUTHZ_", "")));
                }
            }
            return groupAuthorities;
        }
    }

    /**
     * Workaround for IAS only use case until Cloud Authorization Service is
     * globally available.
     */
    static class MyCustomIasTokenAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken>
    {

        public AbstractAuthenticationToken convert(Jwt jwt)
        {
            final List<String> groups = jwt.getClaimAsStringList(TokenClaims.GROUPS);
            final List<GrantedAuthority> groupAuthorities = groups == null ? Collections.emptyList()
                    : groups.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
            return new AuthenticationToken(jwt, groupAuthorities);
        }
    }
}
