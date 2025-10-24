package com.cryptoCollector.microServices.crypto_collector_micro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información de una criptomoneda")
public class CryptoResponse {

    @Schema(description = "ID interno de la base de datos", example = "1")
    private Long id;

    @Schema(description = "ID único de CoinGecko", example = "bitcoin")
    private String coinId;

    @Schema(description = "Nombre de la criptomoneda", example = "Bitcoin")
    private String name;

    @Schema(description = "Símbolo ticker", example = "BTC")
    private String symbol;

    @Schema(description = "Ranking por capitalización de mercado", example = "1")
    private Integer marketCapRank;

    @Schema(description = "Precio actual en USD", example = "43250.50")
    private Double currentPrice;

    @Schema(description = "Capitalización de mercado en USD", example = "850000000000")
    private Double marketCap;

    @Schema(description = "Volumen de trading en 24h en USD", example = "25000000000")
    private Double totalVolume;

    @Schema(description = "Fecha de última actualización", example = "2024-01-15T10:30:00Z")
    private OffsetDateTime lastUpdated;
}
