package com.martwykotek.hft.controller;

import com.martwykotek.hft.model.AddBatchRequest;
import com.martwykotek.hft.model.StatsResponse;
import com.martwykotek.hft.service.TradingDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class TradingController {
    private final TradingDataService tradingDataService;
    
    @PostMapping("/add_batch")
    public Mono<Void> addBatch(@RequestBody AddBatchRequest request) {
        return tradingDataService.addBatch(request.getSymbol(), request.getValues());
    }
    
    @GetMapping("/stats")
    public Mono<StatsResponse> getStats(
            @RequestParam String symbol,
            @RequestParam int k) {
        return tradingDataService.getStats(symbol, k);
    }
} 