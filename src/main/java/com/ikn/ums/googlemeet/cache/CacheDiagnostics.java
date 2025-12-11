package com.ikn.ums.googlemeet.cache;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheDiagnostics {

    @Autowired
    private CacheManager cacheManager;

    @PostConstruct
    public void checkCacheManager() {
        log.info("CACHE MANAGER IN USE = {}", cacheManager.getClass());
    }
}
