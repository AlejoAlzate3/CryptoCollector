package com.cryptoCollector.microServices.crypto_collector_micro.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cryptoCollector.microServices.crypto_collector_micro.exception.ExternalApiException;
import com.cryptoCollector.microServices.crypto_collector_micro.exception.ResourceNotFoundException;
import com.cryptoCollector.microServices.crypto_collector_micro.model.CryptoCurrency;
import com.cryptoCollector.microServices.crypto_collector_micro.service.CryptoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/crypto")
@Tag(name = "Criptomonedas", description = "Endpoints para consultar información de criptomonedas")
@SecurityRequirement(name = "bearerAuth")
public class CryptoController {

    private final CryptoService service;

    public CryptoController(CryptoService service) {
        this.service = service;
    }

    @Operation(summary = "Sincronizar criptomonedas desde CoinGecko",
               description = "Sincroniza hasta 1000 criptomonedas desde la API de CoinGecko. ADVERTENCIA: Puede tomar varios minutos debido a rate limiting.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sincronización exitosa"),
        @ApiResponse(responseCode = "502", description = "Error al comunicarse con CoinGecko API")
    })
    @PostMapping("/sync")
    public Mono<ResponseEntity<Map<String, Object>>> sync() {
        return service.syncFromRemoteReactive()
                .map(count -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("status", "OK");
                    body.put("synced", count);
                    return ResponseEntity.ok(body);
                })
                .onErrorResume(e -> {
                    // Lanzar excepción para que sea manejada por GlobalExceptionHandler
                    return Mono.error(new ExternalApiException(
                            "Error al sincronizar con CoinGecko: " + e.getMessage(), e));
                });
    }

    @Operation(summary = "Listar criptomonedas con paginación",
               description = "Obtiene una lista paginada de criptomonedas con filtros opcionales")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    @GetMapping("/list")
    public Mono<ResponseEntity<Page<CryptoCurrency>>> list(
            @Parameter(description = "Búsqueda por nombre o símbolo") @RequestParam(required = false) String query,
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (máximo 100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "marketCapRank") String sortBy,
            @Parameter(description = "Dirección de ordenamiento (asc/desc)") @RequestParam(defaultValue = "asc") String dir) {
        
        // Limitar tamaño máximo
        size = Math.min(size, 100);
        
        // Crear Sort
        Sort sort = dir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        // Crear PageRequest
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        
        return service.listCryptos(query, pageRequest)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Obtener criptomoneda por ID",
               description = "Obtiene los detalles completos de una criptomoneda específica por su coinId (ej: bitcoin, ethereum)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Criptomoneda encontrada"),
        @ApiResponse(responseCode = "404", description = "Criptomoneda no encontrada"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    @GetMapping("/{coinId}")
    public Mono<ResponseEntity<CryptoCurrency>> getById(
            @Parameter(description = "ID de la criptomoneda (ej: bitcoin, ethereum)") @PathVariable String coinId) {
        return service.findByCoinId(coinId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Cryptocurrency", "coinId", coinId)));
    }

    @Operation(summary = "Obtener estadísticas de la base de datos",
               description = "Muestra el total de criptomonedas sincronizadas y la última actualización")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    @GetMapping("/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getStats() {
        return service.getStats()
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtener estado del scheduler",
               description = "Muestra la configuración del scheduler de sincronización automática, última ejecución y próxima ejecución programada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado obtenido"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    @GetMapping("/scheduler/status")
    public Mono<ResponseEntity<Map<String, Object>>> getSchedulerStatus() {
        return service.getSchedulerStatus()
                .map(ResponseEntity::ok);
    }
}