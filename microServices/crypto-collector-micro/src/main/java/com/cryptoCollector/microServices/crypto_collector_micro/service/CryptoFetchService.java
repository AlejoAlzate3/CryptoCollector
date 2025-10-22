package com.cryptoCollector.microServices.crypto_collector_micro.service;

import com.cryptoCollector.microServices.crypto_collector_micro.dto.CoinGeckoCoin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
public class CryptoFetchService {

        private static final Logger logger = LoggerFactory.getLogger(CryptoFetchService.class);
        private final WebClient webClient;
        private final String apiKey;

        public CryptoFetchService(WebClient.Builder webClientBuilder,
                        @Value("${coingecko.api.key:}") String apiKey) {
                this.apiKey = apiKey;
                this.webClient = webClientBuilder
                                .baseUrl("https://api.coingecko.com/api/v3")
                                .defaultHeader("x-cg-demo-api-key", apiKey)
                                .build();

                if (apiKey != null && !apiKey.isEmpty()) {
                        logger.info("CoinGecko API Key configurada - usando límites premium");
                } else {
                        logger.warn("CoinGecko API Key NO configurada - usando límites públicos");
                }
        }

        /**
         * Obtiene hasta 1000 criptomonedas de CoinGecko.
         * Con API Key: usa 250 items por página (4 páginas total) con delay mínimo.
         * Sin API Key: usa 50 items por página (20 páginas) con delay mayor.
         */
        public Flux<CoinGeckoCoin> fetchExactly1000Reactive() {
                boolean hasApiKey = apiKey != null && !apiKey.isEmpty();

                // Con API Key: 4 páginas de 250 items con delay corto
                // Sin API Key: 20 páginas de 50 items con delay largo
                int perPage = hasApiKey ? 250 : 50;
                int totalPages = hasApiKey ? 4 : 20;
                long delayMillis = hasApiKey ? 300 : 1200; // 0.3s vs 1.2s

                logger.info("Iniciando fetch de {} paginas con {} items cada una (API Key: {})",
                                totalPages, perPage, hasApiKey ? "SI" : "NO");

                return Flux.range(1, totalPages)
                                .delayElements(Duration.ofMillis(delayMillis))
                                .concatMap(page -> {
                                        logger.debug("Fetching page {}/{}", page, totalPages);
                                        return fetchPage(page, perPage);
                                })
                                .doOnComplete(() -> logger.info("Fetch completado exitosamente"))
                                .doOnError(error -> logger.error("Error durante fetch: {}", error.getMessage()));
        }

        /**
         * Obtiene una sola página de criptomonedas.
         * Útil para testing con menos datos.
         */
        public Flux<CoinGeckoCoin> fetchSinglePage(int page, int perPage) {
                logger.info("Fetching single page: {} with {} items", page, perPage);
                return fetchPage(page, perPage);
        }

        private Flux<CoinGeckoCoin> fetchPage(int page, int perPage) {
                return webClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/coins/markets")
                                                .queryParam("vs_currency", "usd")
                                                .queryParam("order", "market_cap_desc")
                                                .queryParam("per_page", perPage)
                                                .queryParam("page", page)
                                                .queryParam("price_change_percentage", "24h")
                                                .build())
                                .retrieve()
                                .bodyToFlux(CoinGeckoCoin.class)
                                .onErrorResume(error -> {
                                        logger.error("Error fetching page {}: {}", page, error.getMessage());
                                        return Flux.empty();
                                });
        }
}
