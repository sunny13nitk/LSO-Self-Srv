package com.sap.cap.esmapi.ui.srv.impl;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sap.cap.esmapi.ui.pojos.TY_TokenResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CL_TokenCache
{
    private static final Map<String, CacheEntry> TOKEN_CACHE = new ConcurrentHashMap<>();

    public static TY_TokenResponse getToken(String destinationName)
    {
        CacheEntry cacheEntry = TOKEN_CACHE.get(destinationName);
        if (cacheEntry != null && Instant.now().isBefore(cacheEntry.expiryTime()))
        {
            return cacheEntry.token();
        }

        return null;
    }

    public static void setToken(String destinationName, TY_TokenResponse token)
    {
        if (token == null || token.getAccessToken() == null)
        {
            return;
        }

        Instant expiryTime = Instant.now().plusSeconds(token.getExpiresIn() - 60);
        TOKEN_CACHE.put(destinationName, new CacheEntry(token, expiryTime));

    }

    public static void clear(String destinationName)
    {
        TOKEN_CACHE.remove(destinationName);
    }

    public static void clearAll()
    {
        TOKEN_CACHE.clear();
    }

    private record CacheEntry(TY_TokenResponse token, Instant expiryTime)
    {
    }
}
