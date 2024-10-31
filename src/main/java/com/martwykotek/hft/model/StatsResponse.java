package com.martwykotek.hft.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsResponse {
    private double min;
    private double max;
    private double last;
    private double avg;
    private double var;
} 