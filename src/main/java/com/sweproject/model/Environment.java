package com.sweproject.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;


public class Environment extends Type{
    private float riskLevel;
    public Environment(boolean maskUsed, String risk, LocalDateTime initialDate, LocalDateTime finalDate) {
        long minutes = ChronoUnit.MINUTES.between(initialDate, finalDate);
        Random r = new Random();
        if(risk.equals("Low"))
            riskLevel = 0.5f;
        else if(risk.equals("Medium"))
            riskLevel = 0.7f;
        else
            riskLevel = 0.9f;
        if(maskUsed)
            riskLevel *= 0.1f;
        riskLevel += r.nextFloat()/10 - 0.05f; // -> health state
        riskLevel *= 2 * Math.exp(minutes/40) / (1 + Math.exp(minutes/40)) - 1;
        if(Float.isNaN(riskLevel)) {
            riskLevel = 0.99f;
            System.out.println("nan");
        }
        if(riskLevel < 0.001)
            riskLevel = 0.001f;
        //FIXME mettere valori plausibili
    }

    public float getRiskLevel(){
        return riskLevel;
    }

    @Override
    public String getName() {
        return "Environment";
    }
}
