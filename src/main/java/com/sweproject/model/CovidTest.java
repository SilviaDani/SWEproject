package com.sweproject.model;

public class CovidTest extends Type {
    private boolean isPositive = false;
    private float sensitivity = 0f;

    public CovidTest(CovidTestType type) {
        if (type == CovidTestType.ANTIGEN)
            sensitivity = 0.653f;
        else if (type == CovidTestType.MOLECULAR)
            sensitivity = 0.918f;
    }
    public CovidTest(CovidTestType type, boolean isPositive){
        this(type);
        this.isPositive = isPositive;
    }
    public float getSensitivity(){
        return sensitivity;
    }
    public boolean getPositivity(){
        return isPositive;
    }
}


