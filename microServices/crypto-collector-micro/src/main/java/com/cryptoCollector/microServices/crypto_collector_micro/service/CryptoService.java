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
     * Totalmente reactivo con delay entre requests.
     */
    @Transactional
    public Mono<Long> syncFromRemoteReactive() {
        return fetchService.fetchExactly1000Reactive()
                .flatMap(this::upsertReactive)
                .count();
    }

    /**
     * Versión de prueba: sincroniza solo 100 criptomonedas (2 páginas).
     * Más rápido para testing y menor riesgo de rate limiting.
     */
    @Transactional
    public Mono<Long> syncTestReactive() {
        return fetchService.fetchSinglePage(1, 50)
                .concatWith(fetchService.fetchSinglePage(2, 50))
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
     * @param query    Búsqueda por nombre o símbolo (case insensitive)
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

    /**
     * Obtiene estadísticas de la base de datos.
     * 
     * @return Mapa con total de criptomonedas y última actualización
     */
    public Mono<java.util.Map<String, Object>> getStats() {
        return Mono.fromCallable(() -> {
            long total = repository.count();
            java.util.Optional<CryptoCurrency> latest = repository.findAll(
                    org.springframework.data.domain.PageRequest.of(0, 1,
                            org.springframework.data.domain.Sort.by("lastUpdated").descending()))
                    .stream().findFirst();

            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("total", total);
            stats.put("lastUpdated", latest.map(CryptoCurrency::getLastUpdated).orElse(null));
            stats.put("hasSyncedData", total > 0);

            return stats;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Obtiene el estado del scheduler de sincronización automática.
     * 
     * @return Mapa con información del scheduler, última sincronización y próxima
     *         ejecución
     */
    public Mono<java.util.Map<String, Object>> getSchedulerStatus() {
        return Mono.fromCallable(() -> {
            java.util.Map<String, Object> status = new java.util.HashMap<>();

            // Configuración del scheduler
            status.put("enabled", true);
            status.put("frequency", "Every 6 hours");
            status.put("schedule", "00:00, 06:00, 12:00, 18:00 UTC");
            status.put("cronExpression", "0 0 */6 * * *");

            // Última sincronización
            long total = repository.count();
            java.util.Optional<CryptoCurrency> latest = repository.findAll(
                    org.springframework.data.domain.PageRequest.of(0, 1,
                            org.springframework.data.domain.Sort.by("lastUpdated").descending()))
                    .stream().findFirst();

            status.put("lastSync", latest.map(CryptoCurrency::getLastUpdated).orElse(null));
            status.put("totalCryptos", total);

            // Calcular próxima ejecución
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

            status.put("nextSync", nextSync);
            status.put("nextSyncDescription", String.format("%02d:00:00 UTC (%s)", nextHour, nextDay));

            // Calcular tiempo restante
            long minutesUntilNext = java.time.Duration.between(now, nextSync).toMinutes();
            status.put("minutesUntilNext", minutesUntilNext);

            return status;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
