package com.martwykotek.hft.service;

import com.martwykotek.hft.model.StatsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TradingDataService {
    private static final int MIN_K = 1;
    private static final int MAX_K = 8;
    private static final int[] BUFFER_SIZES = {
        0,      // indeks 0 nieużywany
        10,     // k=1
        100,    // k=2
        1000,   // k=3
        10000,  // k=4
        50000,  // k=5
        100000, // k=6
        200000, // k=7
        500000  // k=8
    };
    
    private final ConcurrentHashMap<String, PriceStatistics[]> symbolData = new ConcurrentHashMap<>();
    
    public Mono<Void> addBatch(String symbol, List<Double> values) {
        return Mono.fromRunnable(() -> {
            if (symbol == null || symbol.trim().isEmpty()) {
                throw new IllegalArgumentException("Symbol cannot be null or empty");
            }
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException("Values cannot be null or empty");
            }

            PriceStatistics[] stats = symbolData.computeIfAbsent(symbol, 
                k -> new PriceStatistics[MAX_K + 1]);
            
            for (Double value : values) {
                if (value == null) {
                    throw new IllegalArgumentException("Value cannot be null");
                }
                
                for (int k = MIN_K; k <= MAX_K; k++) {
                    if (stats[k] == null) {
                        stats[k] = new PriceStatistics(BUFFER_SIZES[k]);
                    }
                    stats[k].addValue(value);
                }
            }
        });
    }
    
    public Mono<StatsResponse> getStats(String symbol, int k) {
        return Mono.fromCallable(() -> {
            if (k < MIN_K || k > MAX_K) {
                throw new IllegalArgumentException(
                    String.format("k must be between %d and %d", MIN_K, MAX_K));
            }
            
            if (symbol == null || symbol.trim().isEmpty()) {
                throw new IllegalArgumentException("Symbol cannot be null or empty");
            }

            PriceStatistics[] stats = symbolData.get(symbol);
            if (stats == null || stats[k] == null) {
                return StatsResponse.builder()
                        .min(0.0)
                        .max(0.0)
                        .last(0.0)
                        .avg(0.0)
                        .var(0.0)
                        .build();
            }
            
            PriceStatistics stat = stats[k];
            return StatsResponse.builder()
                    .min(stat.getCurrentMin())
                    .max(stat.getCurrentMax())
                    .last(stat.getLast())
                    .avg(stat.getAverage())
                    .var(stat.getVariance())
                    .build();
        });
    }
} 