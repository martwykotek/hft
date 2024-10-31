package com.martwykotek.hft.controller;

import com.martwykotek.hft.model.AddBatchRequest;
import com.martwykotek.hft.model.StatsResponse;
import com.martwykotek.hft.service.TradingDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(TradingController.class)
class TradingControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private TradingDataService tradingDataService;

    @Test
    void shouldHandleAddBatchRequest() {
        // given
        AddBatchRequest request = new AddBatchRequest();
        request.setSymbol("TEST");
        request.setValues(List.of(1.0, 2.0, 3.0));

        when(tradingDataService.addBatch(anyString(), any()))
                .thenReturn(Mono.empty());

        // when & then
        webClient.post()
                .uri("/add_batch")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldHandleGetStatsRequest() {
        // given
        when(tradingDataService.getStats(anyString(), anyInt()))
                .thenReturn(Mono.just(StatsResponse.builder().build()));

        // when & then
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stats")
                        .queryParam("symbol", "TEST")
                        .queryParam("k", "1")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.min").exists()
                .jsonPath("$.max").exists()
                .jsonPath("$.last").exists()
                .jsonPath("$.avg").exists()
                .jsonPath("$.var").exists();
    }

    @Test
    void performanceTest() {
        // given
        AddBatchRequest request = new AddBatchRequest();
        request.setSymbol("PERF_TEST");
        request.setValues(generateLargeValuesList());

        when(tradingDataService.addBatch(anyString(), any()))
                .thenReturn(Mono.empty());

        // when & then
        assertTimeout(ofMillis(1000), () -> {
            for (int i = 0; i < 100; i++) {
                webClient.post()
                        .uri("/add_batch")
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isOk();
            }
        });
    }

    private List<Double> generateLargeValuesList() {
        return IntStream.range(0, 10000)
                .mapToDouble(i -> (double) i)
                .boxed()
                .collect(Collectors.toList());
    }
} 