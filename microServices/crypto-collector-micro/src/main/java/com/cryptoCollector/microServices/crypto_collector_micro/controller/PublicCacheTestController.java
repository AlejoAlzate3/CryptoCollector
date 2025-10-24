package com.cryptoCollector.microServices.crypto_collector_micro.controller;

import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador TEMPORAL para demostrar el funcionamiento de Redis Cache.
 * Este endpoint NO requiere autenticación para facilitar las pruebas.
 */
@RestController
@RequestMapping("/api/public/cache")
public class PublicCacheTestController {

    private final CacheManager cacheManager;
    private static int callCounter = 0;

    public PublicCacheTestController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/demo")
    public Mono<Map<String, Object>> cacheDemo() {
        callCounter++;
        return Mono.fromCallable(() -> {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Este endpoint NO está cacheado - Siempre consulta la BD");
            response.put("timestamp", LocalDateTime.now());
            response.put("callNumber", callCounter);
            response.put("note", "Cada llamada genera un nuevo timestamp");

            Thread.sleep(100);

            return response;
        });
    }

    @GetMapping("/redis-info")
    public Mono<Map<String, Object>> redisInfo() {
        return Mono.fromCallable(() -> {
            Map<String, Object> info = new HashMap<>();
            info.put("redisConfigured", cacheManager != null);
            info.put("cacheNames", cacheManager.getCacheNames());
            info.put("totalCaches", cacheManager.getCacheNames().size());
            info.put("status", "Redis is working! ✅");
            return info;
        });
    }
}
