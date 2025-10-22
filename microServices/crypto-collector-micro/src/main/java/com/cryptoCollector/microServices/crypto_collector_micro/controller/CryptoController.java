package com.cryptoCollector.microServices.crypto_collector_micro.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cryptoCollector.microServices.crypto_collector_micro.service.CryptoService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final CryptoService service;

    public CryptoController(CryptoService service) {
        this.service = service;
    }

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
}