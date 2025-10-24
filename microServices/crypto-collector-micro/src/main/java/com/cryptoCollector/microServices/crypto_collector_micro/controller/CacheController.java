package com.cryptoCollector.microServices.crypto_collector_micro.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/api/cache")
@Tag(name = "Gestión de Caché", description = "Endpoints para administrar la caché de Redis")
@SecurityRequirement(name = "bearerAuth")
public class CacheController {

    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);
    private final CacheManager cacheManager;

    public CacheController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/info")
    @Operation(summary = "Obtener información de cachés", description = "Muestra todos los cachés configurados y sus nombres")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Información obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    public Mono<ResponseEntity<Map<String, Object>>> getCacheInfo() {
        return Mono.fromCallable(() -> {
            Map<String, Object> info = new HashMap<>();
            Collection<String> cacheNames = cacheManager.getCacheNames();

            info.put("totalCaches", cacheNames.size());
            info.put("cacheNames", cacheNames);
            info.put("cacheDescriptions", Map.of(
                    "crypto-list", "Lista paginada de criptomonedas (TTL: 5 min)",
                    "crypto-details", "Detalles de criptomoneda individual (TTL: 2 min)",
                    "crypto-stats", "Estadísticas generales (TTL: 1 min)",
                    "scheduler-status", "Estado del scheduler (TTL: 1 min)",
                    "coingecko-api", "Respuestas de CoinGecko API (TTL: 30 seg)"));

            logger.info("📊 Información de cachés solicitada");
            return ResponseEntity.ok(info);
        });
    }

    @DeleteMapping("/clear-all")
    @Operation(summary = "Limpiar todos los cachés", description = "Invalida y limpia TODOS los cachés de Redis. Las próximas consultas irán a la base de datos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todos los cachés limpiados exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    public Mono<ResponseEntity<Map<String, Object>>> clearAllCaches() {
        return Mono.fromCallable(() -> {
            List<String> clearedCaches = new ArrayList<>();

            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    clearedCaches.add(cacheName);
                }
            });

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Todos los cachés limpiados exitosamente");
            response.put("clearedCaches", clearedCaches);
            response.put("totalCleared", clearedCaches.size());

            logger.warn("🗑️  TODOS los cachés limpiados manualmente: {}", clearedCaches);
            return ResponseEntity.ok(response);
        });
    }

    @DeleteMapping("/clear/{cacheName}")
    @Operation(summary = "Limpiar un caché específico", description = "Invalida y limpia un caché específico por su nombre")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caché limpiado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Caché no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    public Mono<ResponseEntity<Map<String, Object>>> clearSpecificCache(@PathVariable String cacheName) {
        return Mono.fromCallable(() -> {
            var cache = cacheManager.getCache(cacheName);

            if (cache == null) {
                logger.warn("⚠️  Intento de limpiar caché inexistente: {}", cacheName);
                return ResponseEntity.notFound().build();
            }

            cache.clear();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Caché limpiado exitosamente");
            response.put("cacheName", cacheName);

            logger.info("🗑️  Caché '{}' limpiado manualmente", cacheName);
            return ResponseEntity.ok(response);
        });
    }

    @DeleteMapping("/clear-lists")
    @Operation(summary = "Limpiar caché de listas", description = "Invalida solo el caché de listas de criptomonedas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caché de listas limpiado"),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    public Mono<ResponseEntity<Map<String, Object>>> clearListsCache() {
        return clearSpecificCache("crypto-list");
    }

    @DeleteMapping("/clear-details")
    @Operation(summary = "Limpiar caché de detalles", description = "Invalida solo el caché de detalles de criptomonedas individuales")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caché de detalles limpiado"),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    public Mono<ResponseEntity<Map<String, Object>>> clearDetailsCache() {
        return clearSpecificCache("crypto-details");
    }

    @PostMapping("/warmup")
    @Operation(summary = "Precalentar caché", description = "Carga datos comúnmente consultados en caché para mejorar rendimiento inicial")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caché precalentado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    public Mono<ResponseEntity<Map<String, Object>>> warmupCache() {
        return Mono.fromCallable(() -> {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Warmup de caché iniciado");
            response.put("note", "Los datos se cargarán en caché cuando se consulten por primera vez");
            response.put("recommendation", "Ejecuta GET /api/crypto/list, /api/crypto/stats para precargar datos");

            logger.info("🔥 Warmup de caché solicitado");
            return ResponseEntity.ok(response);
        });
    }
}
