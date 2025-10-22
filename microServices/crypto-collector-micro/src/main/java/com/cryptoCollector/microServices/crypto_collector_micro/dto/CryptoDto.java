package com.cryptoCollector.microServices.crypto_collector_micro.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CryptoDto {
    private Long id;
    private String coinId;
    private String name;
    private String symbol;
    private Integer marketCapRank;
    private Double currentPrice;
    private Double marketCap;
    private Double totalVolume;
    private OffsetDateTime lastUpdated;
}
