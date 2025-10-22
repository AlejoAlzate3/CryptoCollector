package com.cryptoCollector.microServices.crypto_collector_micro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CoinGeckoCoin {
    private String id;
    private String symbol;
    private String name;

    @JsonProperty("market_cap_rank")
    private Integer market_cap_rank;

    @JsonProperty("current_price")
    private Double current_price;

    @JsonProperty("market_cap")
    private Double market_cap;

    @JsonProperty("total_volume")
    private Double total_volume;

    @JsonProperty("last_updated")
    private OffsetDateTime last_updated;
}