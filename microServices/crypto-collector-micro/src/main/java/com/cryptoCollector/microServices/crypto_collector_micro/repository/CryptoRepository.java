package com.cryptoCollector.microServices.crypto_collector_micro.repository;

import com.cryptoCollector.microServices.crypto_collector_micro.model.CryptoCurrency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CryptoRepository extends JpaRepository<CryptoCurrency, Long> {
    Optional<CryptoCurrency> findByCoinId(String coinId);

    Page<CryptoCurrency> findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(String name, String symbol, Pageable pageable);
    List<CryptoCurrency> findAllByCoinIdIn(Collection<String> coinIds);
}
