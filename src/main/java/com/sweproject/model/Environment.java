package com.sweproject.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


public class Environment extends Type{
    private float riskLevel;
    public Environment(boolean maskUsed, String risk, LocalDateTime initialDate, LocalDateTime finalDate) {
        long minutes = ChronoUnit.MINUTES.between(initialDate, finalDate);
        if(risk.equals("Low"))
            riskLevel = 0.5f;
        else if(risk.equals("Medium"))
            riskLevel = 0.7f;
        else
            riskLevel = 0.9f;
        if(maskUsed)
            riskLevel *= 0.1f;
        riskLevel *= 2 * Math.exp(minutes/40) / (1 + Math.exp(minutes/40)) - 1;
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
