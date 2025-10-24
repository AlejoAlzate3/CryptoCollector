package com.cryptoCollector.microServices.crypto_collector_micro.mapper;

import com.cryptoCollector.microServices.crypto_collector_micro.dto.CoinGeckoCoin;
import com.cryptoCollector.microServices.crypto_collector_micro.dto.CryptoDto;
import com.cryptoCollector.microServices.crypto_collector_micro.dto.CryptoResponse;
import com.cryptoCollector.microServices.crypto_collector_micro.model.CryptoCurrency;

public final class CryptoMapper {
    private CryptoMapper() {
    }

    public static CryptoDto toDto(CryptoCurrency e) {
        if (e == null)
            return null;
        return CryptoDto.builder()
                .id(e.getId())
                .coinId(e.getCoinId())
                .name(e.getName())
                .symbol(e.getSymbol())
                .marketCapRank(e.getMarketCapRank())
                .currentPrice(e.getCurrentPrice())
                .marketCap(e.getMarketCap())
                .totalVolume(e.getTotalVolume())
                .lastUpdated(e.getLastUpdated())
                .build();
    }

    public static CryptoResponse toResponse(CryptoCurrency entity) {
        if (entity == null)
            return null;
        return CryptoResponse.builder()
                .id(entity.getId())
                .coinId(entity.getCoinId())
                .name(entity.getName())
                .symbol(entity.getSymbol())
                .marketCapRank(entity.getMarketCapRank())
                .currentPrice(entity.getCurrentPrice())
                .marketCap(entity.getMarketCap())
                .totalVolume(entity.getTotalVolume())
                .lastUpdated(entity.getLastUpdated())
                .build();
    }

    public static void updateEntityFromApi(CryptoCurrency entity, CoinGeckoCoin coin) {
        entity.setName(coin.getName());
        entity.setSymbol(coin.getSymbol());
        entity.setMarketCapRank(coin.getMarket_cap_rank());
        entity.setCurrentPrice(safeDouble(coin.getCurrent_price()));
        entity.setMarketCap(safeDouble(coin.getMarket_cap()));
        entity.setTotalVolume(safeDouble(coin.getTotal_volume()));
        entity.setLastUpdated(coin.getLast_updated());
    }

    private static Double safeDouble(Number n) {
        return n == null ? null : n.doubleValue();
    }
}