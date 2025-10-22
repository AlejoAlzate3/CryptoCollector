package com.cryptoCollector.microServices.crypto_collector_micro.service;

import com.cryptoCollector.microServices.crypto_collector_micro.dto.CoinGeckoCoin;
import com.cryptoCollector.microServices.crypto_collector_micro.model.CryptoCurrency;
import com.cryptoCollector.microServices.crypto_collector_micro.repository.CryptoRepository;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
public class CryptoService {

    private final CryptoRepository repository;
    private final CryptoFetchService fetchService;

    public CryptoService(CryptoRepository repository,
                        CryptoFetchService fetchService) {
        this.repository = repository;
        this.fetchService = fetchService;
    }

    /**
     * Sincroniza exactamente 1000 criptos desde CoinGecko.
     * Totalmente reactivo.
     */
    @Transactional
    public Mono<Long> syncFromRemoteReactive() {
        return fetchService.fetchExactly1000Reactive()
                .flatMap(this::upsertReactive)
                .count();
    }

    private Mono<CryptoCurrency> upsertReactive(CoinGeckoCoin coin) {
        return Mono.fromCallable(() -> {
                    return repository.findByCoinId(coin.getId())
                            .map(existing -> updateEntity(existing, coin))
                            .orElseGet(() -> createEntity(coin));
                })
                .map(repository::save)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private CryptoCurrency updateEntity(CryptoCurrency existing, CoinGeckoCoin coin) {
        existing.setName(coin.getName());
        existing.setSymbol(coin.getSymbol());
        existing.setMarketCapRank(coin.getMarket_cap_rank());
        existing.setCurrentPrice(coin.getCurrent_price());
        existing.setMarketCap(coin.getMarket_cap());
        existing.setTotalVolume(coin.getTotal_volume());
        existing.setLastUpdated(coin.getLast_updated());
        return existing;
    }

    private CryptoCurrency createEntity(CoinGeckoCoin coin) {
        return CryptoCurrency.builder()
                .coinId(coin.getId())
                .name(coin.getName())
                .symbol(coin.getSymbol())
                .marketCapRank(coin.getMarket_cap_rank())
                .currentPrice(coin.getCurrent_price())
                .marketCap(coin.getMarket_cap())
                .totalVolume(coin.getTotal_volume())
                .lastUpdated(coin.getLast_updated() != null
                        ? coin.getLast_updated()
                        : OffsetDateTime.now())
                .build();
    }

    /**
     * Lista criptomonedas con paginación y búsqueda opcional.
     * 
     * @param query Búsqueda por nombre o símbolo (case insensitive)
     * @param pageable Configuración de paginación y ordenamiento
     * @return Página de criptomonedas
     */
    public Mono<Page<CryptoCurrency>> listCryptos(String query, Pageable pageable) {
        return Mono.fromCallable(() -> {
            if (query != null && !query.trim().isEmpty()) {
                return repository.findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(
                    query.trim(), query.trim(), pageable);
            } else {
                return repository.findAll(pageable);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Busca una criptomoneda por su coinId.
     * 
     * @param coinId ID de la criptomoneda en CoinGecko
     * @return Mono con la criptomoneda o Mono vacío si no existe
     */
    public Mono<CryptoCurrency> findByCoinId(String coinId) {
        return Mono.fromCallable(() -> repository.findByCoinId(coinId))
                .flatMap(opt -> opt.isPresent() 
                    ? Mono.just(opt.get()) 
                    : Mono.empty())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
