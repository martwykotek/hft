package com.martwykotek.hft.service;

import lombok.Getter;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class PriceStatistics {
    private final int maxSize;
    private final double[] values;
    private final AtomicInteger currentIndex;
    private int size;
    
    private double runningSum;
    private double runningSumSquares;
    private double currentMin;
    private double currentMax;
    
    public PriceStatistics(int maxSize) {
        this.maxSize = maxSize;
        this.values = new double[maxSize];
        this.currentIndex = new AtomicInteger(0);
        this.currentMin = Double.POSITIVE_INFINITY;
        this.currentMax = Double.NEGATIVE_INFINITY;
    }
    
    public synchronized void addValue(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new IllegalArgumentException("Value cannot be Infinite or NaN");
        }

        int index = currentIndex.get() % maxSize;
        
        // Aktualizacja sum z zabezpieczeniem przed przepełnieniem
        double newSum = runningSum + value;
        if (Double.isInfinite(newSum)) {
            throw new IllegalArgumentException("Value too large - would cause overflow");
        }
        runningSum = newSum;

        double newSumSquares = runningSumSquares + (value * value);
        if (Double.isInfinite(newSumSquares)) {
            throw new IllegalArgumentException("Value too large - would cause overflow in sum of squares");
        }
        runningSumSquares = newSumSquares;
        
        if (size >= maxSize) {
            double oldValue = values[index];
            runningSum -= oldValue;
            runningSumSquares -= oldValue * oldValue;
        } else {
            size++;
        }
        
        values[index] = value;
        currentIndex.incrementAndGet();
        
        // Optymalizacja aktualizacji min/max
        if (value < currentMin) currentMin = value;
        if (value > currentMax) currentMax = value;
    }
    
    public double getAverage() {
        return size > 0 ? runningSum / size : 0;
    }
    
    public double getVariance() {
        if (size == 0) return 0;
        double avg = getAverage();
        // Zabezpieczenie przed przepełnieniem przy obliczaniu wariancji
        try {
            return (runningSumSquares / size) - (avg * avg);
        } catch (ArithmeticException e) {
            return Double.MAX_VALUE;
        }
    }
    
    public double getLast() {
        return size > 0 ? values[(currentIndex.get() - 1 + maxSize) % maxSize] : 0;
    }
} 