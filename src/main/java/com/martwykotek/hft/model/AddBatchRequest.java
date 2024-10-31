package com.martwykotek.hft.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class AddBatchRequest {
    @NotEmpty
    private String symbol;
    
    @NotEmpty
    @Size(max = 10000)
    private List<Double> values;
} 