package com.martwykotek.hft.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.IntStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static java.time.Duration.ofMillis;

class PriceStatisticsTest {

    @Test
    @DisplayName("Should calculate statistics correctly for simple case")
    void shouldCalculateStatisticsCorrectly() {
        // given
        PriceStatistics stats = new PriceStatistics(5);
        double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};

        // when
        for (double value : values) {
            stats.addValue(value);
        }

        // then
        assertThat(stats.getAverage()).isEqualTo(3.0);
        assertThat(stats.getCurrentMin()).isEqualTo(1.0);
        assertThat(stats.getCurrentMax()).isEqualTo(5.0);
        assertThat(stats.getLast()).isEqualTo(5.0);
        assertThat(stats.getVariance()).isCloseTo(2.0, within(0.0001));
    }

    @Test
    @DisplayName("Should handle circular buffer correctly")
    void shouldHandleCircularBuffer() {
        // given
        PriceStatistics stats = new PriceStatistics(3);
        
        // when
        stats.addValue(1.0);
        stats.addValue(2.0);
        stats.addValue(3.0);
        stats.addValue(4.0); // should replace 1.0

        // then
        assertThat(stats.getLast()).isEqualTo(4.0);
        assertThat(stats.getAverage()).isEqualTo(3.0);
        assertThat(stats.getCurrentMin()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should handle concurrent updates correctly")
    void concurrencyTest() throws InterruptedException {
        // given
        PriceStatistics stats = new PriceStatistics(1000);
        int numberOfThreads = 10;
        int updatesPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < updatesPerThread; j++) {
                        stats.addValue(1.0);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        assertThat(stats.getSize()).isLessThanOrEqualTo(1000);
        assertThat(stats.getAverage()).isEqualTo(1.0);
    }
} 