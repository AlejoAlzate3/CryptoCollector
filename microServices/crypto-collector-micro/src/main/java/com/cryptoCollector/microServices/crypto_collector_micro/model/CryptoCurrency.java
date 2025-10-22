package com.cryptoCollector.microServices.crypto_collector_micro.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cryptocurrency", uniqueConstraints = {
        @UniqueConstraint(columnNames = "coin_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CryptoCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin_id", nullable = false, length = 128)
    private String coinId; // id from CoinGecko, unique

    private String name;
    private String symbol;
    private Integer marketCapRank;
    private Double currentPrice;
    private Double marketCap;
    private Double totalVolume;

    private OffsetDateTime lastUpdated;
}