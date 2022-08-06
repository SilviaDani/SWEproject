package com.sweproject.model;

public class CovidTest extends Type {
    private CovidTestType type;
    private boolean isPositive = false;
    private float sensitivity = 0f;

    public CovidTest(CovidTestType type) {
        this.type = type;
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

    @Override
    public String getName() {
        return "Covid_test-" + type + "-" + isPositive;
    }
}


