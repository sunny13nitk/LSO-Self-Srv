package com.sap.cap.esmapi.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomFilterClass extends OncePerRequestFilter
{

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        try
        {
            filterChain.doFilter(request, response);
        }
        catch (Exception e)
        {
            log.error(
                    "Invalid Token! Access to app not possible. Try clearing browser history and cookies and reaccessing the app. You can also try logging in via a private/Incognito window. "
                            + e.getLocalizedMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid Token! Access to app not possible. Try clearing browser history and cookies and reaccessing the app. You can also try logging in via a private/Incognito window.");
        }

    }

}
