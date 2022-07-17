package com.sweproject.main;

public class CovidTest extends Type {
    private boolean isPositive;
    private float sensitivity;

    public CovidTest(boolean isPositive, float sensitivity) {
        this.isPositive = isPositive;
        this.sensitivity = sensitivity;
    }
}
