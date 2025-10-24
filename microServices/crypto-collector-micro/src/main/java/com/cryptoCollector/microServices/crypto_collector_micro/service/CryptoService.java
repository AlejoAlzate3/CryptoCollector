package com.cryptoCollector.microServices.crypto_collector_micro.service;

import com.cryptoCollector.microServices.crypto_collector_micro.dto.CoinGeckoCoin;
import com.cryptoCollector.microServices.crypto_collector_micro.model.CryptoCurrency;
import com.cryptoCollector.microServices.crypto_collector_micro.repository.CryptoRepository;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
public class CryptoService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoService.class);
    private final CryptoRepository repository;
    private final CryptoFetchService fetchService;

    public CryptoService(CryptoRepository repository,
            CryptoFetchService fetchService) {
        this.repository = repository;
        this.fetchService = fetchService;
    }

    @Transactional
    @CacheEvict(value = { "crypto-list", "crypto-details", "crypto-stats", "scheduler-status" }, allEntries = true)
    public Mono<Long> syncFromRemoteReactive() {
        logger.info("🗑️  Limpiando TODOS los caches antes de sincronizar datos...");
        return fetchService.fetchExactly1000Reactive()
                .flatMap(this::upsertReactive)
                .count()
                .doOnSuccess(count -> logger.info("✅ Sincronización completa. {} cryptos actualizadas. Cache limpio.",
                        count));
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

    public Mono<Page<CryptoCurrency>> listCryptos(String query, Pageable pageable) {
        logger.debug("� Consultando lista de cryptos: query={}, page={}",
                query, pageable.getPageNumber());
        return Mono.fromCallable(() -> {
            if (query != null && !query.trim().isEmpty()) {
                return repository.findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(
                        query.trim(), query.trim(), pageable);
            } else {
                return repository.findAll(pageable);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Cacheable(value = "crypto-details", key = "#coinId")
    public Mono<CryptoCurrency> findByCoinId(String coinId) {
        logger.info("💾 Cache MISS - Consultando BD para crypto: {}", coinId);
        return Mono.fromCallable(() -> repository.findByCoinId(coinId))
                .flatMap(opt -> opt.isPresent()
                        ? Mono.just(opt.get())
                        : Mono.empty())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Cacheable(value = "crypto-stats")
    public Mono<java.util.Map<String, Object>> getStats() {
        logger.info("💾 Cache MISS - Consultando estadísticas de BD");
        return Mono.fromCallable(() -> {
            long total = repository.count();
            java.util.Optional<CryptoCurrency> latest = repository.findAll(
                    org.springframework.data.domain.PageRequest.of(0, 1,
                            org.springframework.data.domain.Sort.by("lastUpdated").descending()))
                    .stream().findFirst();

            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("total", total);
            stats.put("lastUpdated",
                    latest.map(c -> c.getLastUpdated() != null ? c.getLastUpdated().toString() : null).orElse(null));
            stats.put("hasSyncedData", total > 0);

            return stats;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Cacheable(value = "scheduler-status")
    public Mono<java.util.Map<String, Object>> getSchedulerStatus() {
        logger.info("💾 Cache MISS - Consultando estado del scheduler");
        return Mono.fromCallable(() -> {
            java.util.Map<String, Object> status = new java.util.HashMap<>();

            status.put("enabled", true);
            status.put("frequency", "Every 6 hours");
            status.put("schedule", "00:00, 06:00, 12:00, 18:00 UTC");
            status.put("cronExpression", "0 0 */6 * * *");

            long total = repository.count();
            java.util.Optional<CryptoCurrency> latest = repository.findAll(
                    org.springframework.data.domain.PageRequest.of(0, 1,
                            org.springframework.data.domain.Sort.by("lastUpdated").descending()))
                    .stream().findFirst();

            // Convertir OffsetDateTime a String para Redis
            status.put("lastSync",
                    latest.map(c -> c.getLastUpdated() != null ? c.getLastUpdated().toString() : null).orElse(null));
            status.put("totalCryptos", total);

            OffsetDateTime now = OffsetDateTime.now();
            int currentHour = now.getHour();
            int nextHour;
            String nextDay = "today";

            if (currentHour < 6) {
                nextHour = 6;
            } else if (currentHour < 12) {
                nextHour = 12;
            } else if (currentHour < 18) {
                nextHour = 18;
            } else {
                nextHour = 0;
                nextDay = "tomorrow";
            }

            OffsetDateTime nextSync = now.withHour(nextHour).withMinute(0).withSecond(0).withNano(0);
            if (nextHour == 0 && currentHour >= 18) {
                nextSync = nextSync.plusDays(1);
            }

            // Convertir OffsetDateTime a String para Redis
            status.put("nextSync", nextSync.toString());
            status.put("nextSyncDescription", String.format("%02d:00:00 UTC (%s)", nextHour, nextDay));

            long minutesUntilNext = java.time.Duration.between(now, nextSync).toMinutes();
            status.put("minutesUntilNext", minutesUntilNext);

            return status;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
