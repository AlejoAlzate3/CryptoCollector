package com.cryptoCollector.microServices.crypto_collector_micro.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cryptoCollector.microServices.crypto_collector_micro.service.CryptoService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CryptoSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CryptoSyncScheduler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CryptoService cryptoService;
    private volatile boolean isRunning = false;

    public CryptoSyncScheduler(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void syncCryptocurrencies() {
        if (isRunning) {
            logger.warn("Sincronizacion anterior aun en progreso, saltando esta ejecucion");
            return;
        }

        try {
            isRunning = true;
            String startTime = LocalDateTime.now().format(formatter);
            logger.info("=== Iniciando sincronizacion automatica de criptomonedas a las {} ===", startTime);

            cryptoService.syncFromRemoteReactive()
                    .doOnSuccess(count -> {
                        String endTime = LocalDateTime.now().format(formatter);
                        logger.info("Sincronizacion completada exitosamente a las {}", endTime);
                        logger.info("  -> Total sincronizado: {} criptomonedas", count);
                    })
                    .doOnError(error -> {
                        String endTime = LocalDateTime.now().format(formatter);
                        logger.error("Error durante sincronizacion a las {}", endTime, error);

                        if (error.getMessage() != null && error.getMessage().contains("429")) {
                            logger.warn(
                                    "  -> CoinGecko API rate limit alcanzado. La proxima sincronizacion se intentara en 6 horas.");
                        }
                    })
                    .doFinally(signal -> {
                        isRunning = false;
                        logger.debug("Senal de finalizacion: {}", signal);
                    })
                    .subscribe();

        } catch (Exception e) {
            logger.error("Error inesperado al iniciar sincronizacion", e);
            isRunning = false;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
