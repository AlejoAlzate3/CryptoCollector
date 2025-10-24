package com.cryptoCollector.microServices.crypto_collector_micro.service;

import com.cryptoCollector.microServices.crypto_collector_micro.dto.CoinGeckoCoin;
import com.cryptoCollector.microServices.crypto_collector_micro.model.CryptoCurrency;
import com.cryptoCollector.microServices.crypto_collector_micro.repository.CryptoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CryptoService - Tests Unitarios")
class CryptoServiceTest {

    @Mock
    private CryptoRepository repository;

    @Mock
    private CryptoFetchService fetchService;

    @InjectMocks
    private CryptoService cryptoService;

    private CoinGeckoCoin mockCoin;
    private CryptoCurrency mockCrypto;

    @BeforeEach
    void setUp() {
        mockCoin = CoinGeckoCoin.builder()
                .id("bitcoin")
                .name("Bitcoin")
                .symbol("btc")
                .market_cap_rank(1)
                .current_price(45000.0)
                .market_cap(900000000000.0)
                .total_volume(50000000000.0)
                .last_updated(OffsetDateTime.now())
                .build();

        mockCrypto = CryptoCurrency.builder()
                .id(1L)
                .coinId("bitcoin")
                .name("Bitcoin")
                .symbol("btc")
                .marketCapRank(1)
                .currentPrice(45000.0)
                .marketCap(900000000000.0)
                .totalVolume(50000000000.0)
                .lastUpdated(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debe sincronizar correctamente cuando la crypto es nueva")
    void testSyncFromRemote_NewCrypto() {
        when(fetchService.fetchExactly1000Reactive())
                .thenReturn(Flux.just(mockCoin));
        when(repository.findByCoinId("bitcoin"))
                .thenReturn(Optional.empty());
        when(repository.save(any(CryptoCurrency.class)))
                .thenReturn(mockCrypto);

        StepVerifier.create(cryptoService.syncFromRemoteReactive())
                .assertNext(count -> assertThat(count).isEqualTo(1L))
                .verifyComplete();

        verify(fetchService).fetchExactly1000Reactive();
        verify(repository).findByCoinId("bitcoin");
        verify(repository).save(any(CryptoCurrency.class));
    }

    @Test
    @DisplayName("Debe actualizar correctamente cuando la crypto ya existe")
    void testSyncFromRemote_ExistingCrypto() {
        CryptoCurrency existingCrypto = CryptoCurrency.builder()
                .id(1L)
                .coinId("bitcoin")
                .name("Bitcoin")
                .symbol("btc")
                .marketCapRank(1)
                .currentPrice(40000.0)
                .marketCap(800000000000.0)
                .totalVolume(45000000000.0)
                .lastUpdated(OffsetDateTime.now().minusDays(1))
                .build();

        when(fetchService.fetchExactly1000Reactive())
                .thenReturn(Flux.just(mockCoin));
        when(repository.findByCoinId("bitcoin"))
                .thenReturn(Optional.of(existingCrypto));
        when(repository.save(any(CryptoCurrency.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(cryptoService.syncFromRemoteReactive())
                .assertNext(count -> assertThat(count).isEqualTo(1L))
                .verifyComplete();

        verify(fetchService).fetchExactly1000Reactive();
        verify(repository).findByCoinId("bitcoin");
        verify(repository).save(argThat(crypto -> {
            assertThat(crypto.getCurrentPrice()).isEqualTo(45000.0);
            assertThat(crypto.getName()).isEqualTo("Bitcoin");
            return true;
        }));
    }

    @Test
    @DisplayName("Debe sincronizar múltiples cryptos correctamente")
    void testSyncFromRemote_MultipleCryptos() {
        CoinGeckoCoin ethereum = CoinGeckoCoin.builder()
                .id("ethereum")
                .name("Ethereum")
                .symbol("eth")
                .market_cap_rank(2)
                .current_price(3000.0)
                .market_cap(350000000000.0)
                .total_volume(20000000000.0)
                .last_updated(OffsetDateTime.now())
                .build();

        when(fetchService.fetchExactly1000Reactive())
                .thenReturn(Flux.just(mockCoin, ethereum));
        when(repository.findByCoinId(anyString()))
                .thenReturn(Optional.empty());
        when(repository.save(any(CryptoCurrency.class)))
                .thenReturn(mockCrypto);

        StepVerifier.create(cryptoService.syncFromRemoteReactive())
                .assertNext(count -> assertThat(count).isEqualTo(2L))
                .verifyComplete();

        verify(fetchService).fetchExactly1000Reactive();
        verify(repository, times(2)).findByCoinId(anyString());
        verify(repository, times(2)).save(any(CryptoCurrency.class));
    }

    @Test
    @DisplayName("Debe manejar error al sincronizar desde API externa")
    void testSyncFromRemote_ApiError() {
        when(fetchService.fetchExactly1000Reactive())
                .thenReturn(Flux.error(new RuntimeException("API Error")));

        StepVerifier.create(cryptoService.syncFromRemoteReactive())
                .expectError(RuntimeException.class)
                .verify();

        verify(fetchService).fetchExactly1000Reactive();
        verify(repository, never()).save(any(CryptoCurrency.class));
    }

    @Test
    @DisplayName("Debe listar todas las cryptos con paginación")
    void testListCryptos_AllCryptos() {
        Pageable pageable = PageRequest.of(0, 10);
        List<CryptoCurrency> cryptos = Arrays.asList(mockCrypto);
        Page<CryptoCurrency> page = new PageImpl<>(cryptos, pageable, 1);

        when(repository.findAll(pageable)).thenReturn(page);

        StepVerifier.create(cryptoService.listCryptos(null, pageable))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getContent()).hasSize(1);
                    assertThat(result.getContent().get(0).getCoinId()).isEqualTo("bitcoin");
                })
                .verifyComplete();

        verify(repository).findAll(pageable);
    }

    @Test
    @DisplayName("Debe buscar cryptos por nombre")
    void testListCryptos_SearchByName() {
        Pageable pageable = PageRequest.of(0, 10);
        List<CryptoCurrency> cryptos = Arrays.asList(mockCrypto);
        Page<CryptoCurrency> page = new PageImpl<>(cryptos, pageable, 1);

        when(repository.findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(
                "bitcoin", "bitcoin", pageable)).thenReturn(page);

        StepVerifier.create(cryptoService.listCryptos("bitcoin", pageable))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getContent()).hasSize(1);
                    assertThat(result.getContent().get(0).getName()).isEqualToIgnoringCase("Bitcoin");
                })
                .verifyComplete();

        verify(repository).findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(
                "bitcoin", "bitcoin", pageable);
    }

    @Test
    @DisplayName("Debe devolver página vacía cuando no hay resultados")
    void testListCryptos_NoResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CryptoCurrency> emptyPage = Page.empty(pageable);

        when(repository.findAll(pageable)).thenReturn(emptyPage);

        StepVerifier.create(cryptoService.listCryptos(null, pageable))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getContent()).isEmpty();
                })
                .verifyComplete();

        verify(repository).findAll(pageable);
    }

    @Test
    @DisplayName("Debe encontrar crypto por coinId")
    void testFindByCoinId_Found() {
        when(repository.findByCoinId("bitcoin"))
                .thenReturn(Optional.of(mockCrypto));

        StepVerifier.create(cryptoService.findByCoinId("bitcoin"))
                .assertNext(crypto -> {
                    assertThat(crypto).isNotNull();
                    assertThat(crypto.getCoinId()).isEqualTo("bitcoin");
                    assertThat(crypto.getName()).isEqualTo("Bitcoin");
                })
                .verifyComplete();

        verify(repository).findByCoinId("bitcoin");
    }

    @Test
    @DisplayName("Debe devolver Mono vacío cuando crypto no existe")
    void testFindByCoinId_NotFound() {
        when(repository.findByCoinId("nonexistent"))
                .thenReturn(Optional.empty());

        StepVerifier.create(cryptoService.findByCoinId("nonexistent"))
                .verifyComplete();

        verify(repository).findByCoinId("nonexistent");
    }

    @Test
    @DisplayName("Debe obtener estadísticas correctamente cuando hay datos")
    void testGetStats_WithData() {
        Pageable latestPageable = PageRequest.of(0, 1,
                Sort.by("lastUpdated").descending());
        Page<CryptoCurrency> page = new PageImpl<>(Arrays.asList(mockCrypto));

        when(repository.count()).thenReturn(1028L);
        when(repository.findAll(latestPageable)).thenReturn(page);

        StepVerifier.create(cryptoService.getStats())
                .assertNext(stats -> {
                    assertThat(stats).isNotNull();
                    assertThat(stats.get("total")).isEqualTo(1028L);
                    assertThat(stats.get("hasSyncedData")).isEqualTo(true);
                    assertThat(stats.get("lastUpdated")).isNotNull();
                })
                .verifyComplete();

        verify(repository).count();
        verify(repository).findAll(latestPageable);
    }

    @Test
    @DisplayName("Debe obtener estadísticas correctamente cuando no hay datos")
    void testGetStats_NoData() {
        Pageable latestPageable = PageRequest.of(0, 1,
                Sort.by("lastUpdated").descending());
        Page<CryptoCurrency> emptyPage = Page.empty();

        when(repository.count()).thenReturn(0L);
        when(repository.findAll(latestPageable)).thenReturn(emptyPage);

        StepVerifier.create(cryptoService.getStats())
                .assertNext(stats -> {
                    assertThat(stats).isNotNull();
                    assertThat(stats.get("total")).isEqualTo(0L);
                    assertThat(stats.get("hasSyncedData")).isEqualTo(false);
                    assertThat(stats.get("lastUpdated")).isNull();
                })
                .verifyComplete();

        verify(repository).count();
        verify(repository).findAll(latestPageable);
    }

    @Test
    @DisplayName("Debe obtener estado del scheduler correctamente")
    void testGetSchedulerStatus() {
        Pageable latestPageable = PageRequest.of(0, 1,
                Sort.by("lastUpdated").descending());
        Page<CryptoCurrency> page = new PageImpl<>(Arrays.asList(mockCrypto));

        when(repository.count()).thenReturn(1028L);
        when(repository.findAll(latestPageable)).thenReturn(page);

        StepVerifier.create(cryptoService.getSchedulerStatus())
                .assertNext(status -> {
                    assertThat(status).isNotNull();
                    assertThat(status.get("enabled")).isEqualTo(true);
                    assertThat(status.get("frequency")).isEqualTo("Every 6 hours");
                    assertThat(status.get("schedule")).isEqualTo("00:00, 06:00, 12:00, 18:00 UTC");
                    assertThat(status.get("totalCryptos")).isEqualTo(1028L);
                    assertThat(status.get("lastSync")).isNotNull();
                    assertThat(status.get("nextSync")).isNotNull();
                    assertThat(status.get("minutesUntilNext")).isNotNull();
                })
                .verifyComplete();

        verify(repository).count();
        verify(repository).findAll(latestPageable);
    }
}
