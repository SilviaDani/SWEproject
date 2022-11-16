package com.sweproject.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;

public class Contact extends Type{
    private ArrayList<Subject> subjects;
    private float trasmissionChance;
    private ArrayList<String> cluster;
    private float riskLevel;

    public Contact(ArrayList<Subject> subjects, float trasmissionChance) {
        this.trasmissionChance = trasmissionChance;
        this.subjects = subjects;
    }

    public Contact(ArrayList<String> cluster){
        this.cluster = cluster;
    }

    public Contact(ArrayList<String> cluster, boolean maskUsed, String risk, LocalDateTime initialDate, LocalDateTime finalDate) {
        this(cluster);
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

        riskLevel *= 2 * Math.exp(minutes/40) / (1 + Math.exp(minutes/40)) - 1;
        riskLevel += r.nextFloat()/10 - 0.05f;
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
        return "Contact";
    }
}
