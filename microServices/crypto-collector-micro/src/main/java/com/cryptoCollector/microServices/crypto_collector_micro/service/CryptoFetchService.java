package com.cryptoCollector.microServices.crypto_collector_micro.service;

import com.cryptoCollector.microServices.crypto_collector_micro.dto.CoinGeckoCoin;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class CryptoFetchService {

    private final WebClient webClient;

    public CryptoFetchService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.coingecko.com/api/v3")
                .build();
    }

    public Flux<CoinGeckoCoin> fetchExactly1000Reactive() {
        int perPage = 20;
        int totalPages = 50;

        return Flux.range(1, totalPages)
                .flatMap(page -> webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/coins/markets")
                                .queryParam("vs_currency", "usd")
                                .queryParam("order", "market_cap_desc")
                                .queryParam("per_page", perPage)
                                .queryParam("page", page)
                                .queryParam("price_change_percentage", "24h")
                                .build())
                        .retrieve()
                        .bodyToFlux(CoinGeckoCoin.class))
                .take(1000);
    }
}
