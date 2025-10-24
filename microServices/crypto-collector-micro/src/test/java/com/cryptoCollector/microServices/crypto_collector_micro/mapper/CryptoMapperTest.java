package com.cryptoCollector.microServices.crypto_collector_micro.mapper;

import com.cryptoCollector.microServices.crypto_collector_micro.dto.CoinGeckoCoin;
import com.cryptoCollector.microServices.crypto_collector_micro.dto.CryptoDto;
import com.cryptoCollector.microServices.crypto_collector_micro.dto.CryptoResponse;
import com.cryptoCollector.microServices.crypto_collector_micro.model.CryptoCurrency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CryptoMapper - Tests Unitarios")
class CryptoMapperTest {

    @Test
    @DisplayName("Debe convertir CryptoCurrency a CryptoDto correctamente")
    void testToDto_Success() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        CryptoCurrency entity = CryptoCurrency.builder()
                .id(1L)
                .coinId("bitcoin")
                .name("Bitcoin")
                .symbol("BTC")
                .marketCapRank(1)
                .currentPrice(45000.0)
                .marketCap(900000000000.0)
                .totalVolume(50000000000.0)
                .lastUpdated(now)
                .build();

        // When
        CryptoDto dto = CryptoMapper.toDto(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCoinId()).isEqualTo("bitcoin");
        assertThat(dto.getName()).isEqualTo("Bitcoin");
        assertThat(dto.getSymbol()).isEqualTo("BTC");
        assertThat(dto.getMarketCapRank()).isEqualTo(1);
        assertThat(dto.getCurrentPrice()).isEqualTo(45000.0);
        assertThat(dto.getMarketCap()).isEqualTo(900000000000.0);
        assertThat(dto.getTotalVolume()).isEqualTo(50000000000.0);
        assertThat(dto.getLastUpdated()).isEqualTo(now);
    }

    @Test
    @DisplayName("Debe manejar CryptoCurrency null en toDto")
    void testToDto_NullEntity() {
        CryptoDto dto = CryptoMapper.toDto(null);

        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Debe manejar campos null en CryptoCurrency al convertir a CryptoDto")
    void testToDto_NullFields() {
        CryptoCurrency entity = CryptoCurrency.builder()
                .id(1L)
                .coinId("bitcoin")
                .name(null) // Campo null
                .symbol(null)
                .marketCapRank(null)
                .currentPrice(null)
                .marketCap(null)
                .totalVolume(null)
                .lastUpdated(null)
                .build();

        CryptoDto dto = CryptoMapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCoinId()).isEqualTo("bitcoin");
        assertThat(dto.getName()).isNull();
        assertThat(dto.getSymbol()).isNull();
        assertThat(dto.getMarketCapRank()).isNull();
        assertThat(dto.getCurrentPrice()).isNull();
        assertThat(dto.getMarketCap()).isNull();
        assertThat(dto.getTotalVolume()).isNull();
        assertThat(dto.getLastUpdated()).isNull();
    }

    @Test
    @DisplayName("Debe convertir CryptoCurrency a CryptoResponse correctamente")
    void testToResponse_Success() {
        OffsetDateTime now = OffsetDateTime.now();
        CryptoCurrency entity = CryptoCurrency.builder()
                .id(2L)
                .coinId("ethereum")
                .name("Ethereum")
                .symbol("ETH")
                .marketCapRank(2)
                .currentPrice(3000.0)
                .marketCap(350000000000.0)
                .totalVolume(20000000000.0)
                .lastUpdated(now)
                .build();

        CryptoResponse response = CryptoMapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getCoinId()).isEqualTo("ethereum");
        assertThat(response.getName()).isEqualTo("Ethereum");
        assertThat(response.getSymbol()).isEqualTo("ETH");
        assertThat(response.getMarketCapRank()).isEqualTo(2);
        assertThat(response.getCurrentPrice()).isEqualTo(3000.0);
        assertThat(response.getMarketCap()).isEqualTo(350000000000.0);
        assertThat(response.getTotalVolume()).isEqualTo(20000000000.0);
        assertThat(response.getLastUpdated()).isEqualTo(now);
    }

    @Test
    @DisplayName("Debe manejar CryptoCurrency null en toResponse")
    void testToResponse_NullEntity() {
        CryptoResponse response = CryptoMapper.toResponse(null);

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("Debe manejar campos null en CryptoCurrency al convertir a CryptoResponse")
    void testToResponse_NullFields() {
        CryptoCurrency entity = CryptoCurrency.builder()
                .id(3L)
                .coinId("cardano")
                .name(null)
                .symbol(null)
                .marketCapRank(null)
                .currentPrice(null)
                .marketCap(null)
                .totalVolume(null)
                .lastUpdated(null)
                .build();

        CryptoResponse response = CryptoMapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getCoinId()).isEqualTo("cardano");
        assertThat(response.getName()).isNull();
        assertThat(response.getSymbol()).isNull();
        assertThat(response.getMarketCapRank()).isNull();
        assertThat(response.getCurrentPrice()).isNull();
        assertThat(response.getMarketCap()).isNull();
        assertThat(response.getTotalVolume()).isNull();
        assertThat(response.getLastUpdated()).isNull();
    }

    @Test
    @DisplayName("Debe actualizar entidad desde CoinGeckoCoin correctamente")
    void testUpdateEntityFromApi_Success() {
        OffsetDateTime oldDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime newDate = OffsetDateTime.now();

        CryptoCurrency entity = CryptoCurrency.builder()
                .id(1L)
                .coinId("bitcoin")
                .name("Bitcoin")
                .symbol("BTC")
                .marketCapRank(1)
                .currentPrice(40000.0)
                .marketCap(800000000000.0)
                .totalVolume(45000000000.0)
                .lastUpdated(oldDate)
                .build();

        CoinGeckoCoin apiCoin = CoinGeckoCoin.builder()
                .id("bitcoin")
                .name("Bitcoin Updated")
                .symbol("btc")
                .market_cap_rank(1)
                .current_price(45000.0)
                .market_cap(900000000000.0)
                .total_volume(50000000000.0)
                .last_updated(newDate)
                .build();

        CryptoMapper.updateEntityFromApi(entity, apiCoin);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCoinId()).isEqualTo("bitcoin");
        assertThat(entity.getName()).isEqualTo("Bitcoin Updated");
        assertThat(entity.getSymbol()).isEqualTo("btc");
        assertThat(entity.getMarketCapRank()).isEqualTo(1);
        assertThat(entity.getCurrentPrice()).isEqualTo(45000.0);
        assertThat(entity.getMarketCap()).isEqualTo(900000000000.0);
        assertThat(entity.getTotalVolume()).isEqualTo(50000000000.0);
        assertThat(entity.getLastUpdated()).isEqualTo(newDate);
    }

    @Test
    @DisplayName("Debe manejar valores null en CoinGeckoCoin al actualizar")
    void testUpdateEntityFromApi_NullValues() {
        CryptoCurrency entity = CryptoCurrency.builder()
                .id(1L)
                .coinId("bitcoin")
                .name("Bitcoin")
                .symbol("BTC")
                .marketCapRank(1)
                .currentPrice(45000.0)
                .marketCap(900000000000.0)
                .totalVolume(50000000000.0)
                .lastUpdated(OffsetDateTime.now())
                .build();

        CoinGeckoCoin apiCoin = CoinGeckoCoin.builder()
                .id("bitcoin")
                .name(null)
                .symbol(null)
                .market_cap_rank(null)
                .current_price(null)
                .market_cap(null)
                .total_volume(null)
                .last_updated(null)
                .build();

        CryptoMapper.updateEntityFromApi(entity, apiCoin);

        assertThat(entity.getName()).isNull();
        assertThat(entity.getSymbol()).isNull();
        assertThat(entity.getMarketCapRank()).isNull();
        assertThat(entity.getCurrentPrice()).isNull();
        assertThat(entity.getMarketCap()).isNull();
        assertThat(entity.getTotalVolume()).isNull();
        assertThat(entity.getLastUpdated()).isNull();
    }

    @Test
    @DisplayName("Debe preservar ID y coinId al actualizar desde API")
    void testUpdateEntityFromApi_PreservesIdAndCoinId() {
        CryptoCurrency entity = CryptoCurrency.builder()
                .id(999L)
                .coinId("unique-coin-id")
                .name("Old Name")
                .symbol("OLD")
                .build();

        CoinGeckoCoin apiCoin = CoinGeckoCoin.builder()
                .id("different-id")
                .name("New Name")
                .symbol("NEW")
                .build();

        CryptoMapper.updateEntityFromApi(entity, apiCoin);

        assertThat(entity.getId()).isEqualTo(999L);
        assertThat(entity.getCoinId()).isEqualTo("unique-coin-id");
        // Pero otros campos SÍ cambian
        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getSymbol()).isEqualTo("NEW");
    }

    @Test
    @DisplayName("Debe manejar conversión completa de ida y vuelta")
    void testRoundTrip_EntityToResponseToEntity() {
        OffsetDateTime now = OffsetDateTime.now();
        CryptoCurrency original = CryptoCurrency.builder()
                .id(10L)
                .coinId("test-coin")
                .name("Test Coin")
                .symbol("TST")
                .marketCapRank(100)
                .currentPrice(1.5)
                .marketCap(1000000.0)
                .totalVolume(50000.0)
                .lastUpdated(now)
                .build();

        CryptoResponse response = CryptoMapper.toResponse(original);

        assertThat(response.getId()).isEqualTo(original.getId());
        assertThat(response.getCoinId()).isEqualTo(original.getCoinId());
        assertThat(response.getName()).isEqualTo(original.getName());
        assertThat(response.getSymbol()).isEqualTo(original.getSymbol());
        assertThat(response.getMarketCapRank()).isEqualTo(original.getMarketCapRank());
        assertThat(response.getCurrentPrice()).isEqualTo(original.getCurrentPrice());
        assertThat(response.getMarketCap()).isEqualTo(original.getMarketCap());
        assertThat(response.getTotalVolume()).isEqualTo(original.getTotalVolume());
        assertThat(response.getLastUpdated()).isEqualTo(original.getLastUpdated());
    }
}
