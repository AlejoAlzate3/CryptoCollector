package com.cryptoCollector.microServices.crypto_collector_micro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.reactive.TransactionCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class ReactiveConfig {

    @Bean
    public TransactionalOperator transactionalOperator() {
        // Implementación simplificada para permitir que el servicio compile
        // ya que JPA no es nativo reactivo pero el servicio usa Mono/Flux
        return new TransactionalOperator() {
            @Override
            @NonNull
            public <T> Mono<T> transactional(@NonNull Mono<T> mono) {
                return mono;
            }

            @Override
            @NonNull
            public <T> Flux<T> transactional(@NonNull Flux<T> flux) {
                return flux;
            }

            @Override
            @NonNull
            public <T> Flux<T> execute(@NonNull TransactionCallback<T> action) {
                return Flux.empty();
            }
        };
    }
}
