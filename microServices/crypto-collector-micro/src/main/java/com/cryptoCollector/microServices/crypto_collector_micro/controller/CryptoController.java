package com.cryptoCollector.microServices.crypto_collector_micro.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cryptoCollector.microServices.crypto_collector_micro.model.CryptoCurrency;
import com.cryptoCollector.microServices.crypto_collector_micro.service.CryptoService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final CryptoService service;

    public CryptoController(CryptoService service) {
        this.service = service;
    }

    /**
     * Sincroniza hasta 1000 criptomonedas desde CoinGecko.
     * ADVERTENCIA: Puede tomar varios minutos debido a rate limiting.
     */
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
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", e.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(error));
                });
    }

    /**
     * Endpoint de prueba: Sincroniza solo 100 criptomonedas (2 páginas de 50).
     * Más rápido y con menor probabilidad de rate limiting.
     */
    @PostMapping("/sync-test")
    public Mono<ResponseEntity<Map<String, Object>>> syncTest() {
        return service.syncTestReactive()
                .map(count -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("status", "OK");
                    body.put("message", "Test sync completed");
                    body.put("synced", count);
                    return ResponseEntity.ok(body);
                })
                .onErrorResume(e -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", e.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(error));
                });
    }

    /**
     * Endpoint para listar criptomonedas con paginación, filtros y ordenamiento.
     * 
     * @param query Búsqueda por nombre o símbolo (opcional)
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 20, max: 100)
     * @param sortBy Campo de ordenamiento (default: marketCapRank)
     * @param dir Dirección de ordenamiento: asc o desc (default: asc)
     * @return Página de criptomonedas
     */
    @GetMapping("/list")
    public Mono<ResponseEntity<Page<CryptoCurrency>>> list(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "marketCapRank") String sortBy,
            @RequestParam(defaultValue = "asc") String dir) {
        
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

    /**
     * Endpoint para obtener detalles de una criptomoneda específica.
     * 
     * @param coinId ID de la criptomoneda en CoinGecko (ej: bitcoin, ethereum)
     * @return Detalles de la criptomoneda o 404 si no existe
     */
    @GetMapping("/{coinId}")
    public Mono<ResponseEntity<CryptoCurrency>> getById(@PathVariable String coinId) {
        return service.findByCoinId(coinId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint para obtener estadísticas de la base de datos.
     * Muestra el total de criptomonedas y la última actualización.
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getStats() {
        return service.getStats()
                .map(ResponseEntity::ok);
    }
}