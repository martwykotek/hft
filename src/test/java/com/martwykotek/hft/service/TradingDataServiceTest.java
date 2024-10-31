package com.martwykotek.hft.service;

import com.martwykotek.hft.model.StatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static java.time.Duration.ofMillis;

class TradingDataServiceTest {

    @Test
    @DisplayName("Should handle batch updates correctly")
    void shouldHandleBatchUpdatesCorrectly() {
        TradingDataService service = new TradingDataService();
        List<Double> values = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        String symbol = "TEST";

        Mono<Void> addResult = service.addBatch(symbol, values);
        Mono<StatsResponse> statsResult = service.getStats(symbol, 1);

        // then
        StepVerifier.create(addResult)
                .verifyComplete();

        StepVerifier.create(statsResult)
                .assertNext(stats -> {
                    assertThat(stats.getMin()).isEqualTo(1.0);
                    assertThat(stats.getMax()).isEqualTo(5.0);
                    assertThat(stats.getLast()).isEqualTo(5.0);
                    assertThat(stats.getAvg()).isEqualTo(3.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle concurrent requests for different symbols")
    void concurrencyTest() throws InterruptedException {
        // given
        TradingDataService service = new TradingDataService();
        int numberOfThreads = 10;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    String symbol = "SYM" + threadNum;
                    List<Double> values = List.of(1.0, 2.0, 3.0);
                    StepVerifier.create(service.addBatch(symbol, values))
                            .verifyComplete();

                    StepVerifier.create(service.getStats(symbol, 1))
                            .assertNext(stats ->
                                    assertThat(stats.getLast()).isEqualTo(3.0))
                            .verifyComplete();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle empty symbol data gracefully")
    void shouldHandleEmptySymbolData() {
        // given
        TradingDataService service = new TradingDataService();
        String nonExistentSymbol = "NONE";

        // when & then
        StepVerifier.create(service.getStats(nonExistentSymbol, 1))
                .assertNext(stats -> {
                    assertThat(stats.getAvg()).isEqualTo(0.0);
                    assertThat(stats.getVar()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle invalid k values")
    void shouldHandleInvalidKValues() {
        // given
        TradingDataService service = new TradingDataService();
        String symbol = "TEST";

        // when & then
        StepVerifier.create(service.getStats(symbol, 0))
                .verifyError(IllegalArgumentException.class);

        StepVerifier.create(service.getStats(symbol, 9))
                .verifyError(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle null and empty inputs")
    void shouldHandleNullAndEmptyInputs() {
        // given
        TradingDataService service = new TradingDataService();

        // when & then
        StepVerifier.create(service.addBatch(null, List.of(1.0)))
                .verifyError(IllegalArgumentException.class);

        StepVerifier.create(service.addBatch("TEST", null))
                .verifyError(IllegalArgumentException.class);

        StepVerifier.create(service.addBatch("TEST", List.of()))
                .verifyError(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle extreme values correctly")
    void shouldHandleExtremeValues() {
        TradingDataService service = new TradingDataService();
        String symbol = "EXTREME";
        List<Double> extremeValues = List.of(
                999_999_999.99,
                0.00001,
                500_000_000.50
        );

        StepVerifier.create(service.addBatch(symbol, extremeValues))
                .verifyComplete();

        // then
        StepVerifier.create(service.getStats(symbol, 1))
                .assertNext(stats -> {
                    assertThat(stats.getMax()).isEqualTo(999_999_999.99);
                    assertThat(stats.getMin()).isEqualTo(0.00001);
                    assertThat(stats.getAvg()).isGreaterThan(0);
                    assertThat(stats.getVar()).isGreaterThan(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should maintain separate statistics for different symbols")
    void shouldMaintainSeparateStats() {
        TradingDataService service = new TradingDataService();
        String symbol1 = "SYM1";
        String symbol2 = "SYM2";

        StepVerifier.create(service.addBatch(symbol1, List.of(1.0, 2.0)))
                .verifyComplete();
        StepVerifier.create(service.addBatch(symbol2, List.of(10.0, 20.0)))
                .verifyComplete();

        StepVerifier.create(service.getStats(symbol1, 1))
                .assertNext(stats -> {
                    assertThat(stats.getAvg()).isEqualTo(1.5);
                    assertThat(stats.getMax()).isEqualTo(2.0);
                })
                .verifyComplete();

        StepVerifier.create(service.getStats(symbol2, 1))
                .assertNext(stats -> {
                    assertThat(stats.getAvg()).isEqualTo(15.0);
                    assertThat(stats.getMax()).isEqualTo(20.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle invalid extreme values appropriately")
    void shouldHandleInvalidExtremeValues() {
        TradingDataService service = new TradingDataService();
        String symbol = "EXTREME";
        List<Double> invalidExtremeValues = List.of(Double.MAX_VALUE, Double.MIN_VALUE);

        StepVerifier.create(service.addBatch(symbol, invalidExtremeValues))
                .verifyError(IllegalArgumentException.class);
    }

} 