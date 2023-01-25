package com.sweproject.model;

import org.apache.commons.math3.distribution.WeibullDistribution;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Symptoms extends Type{
    //XXX la classe Symptoms può essere un elenco di sintomi legati al covid e in base a quali sono presenti viene determinata la rilevanza dell'osservazione?
    static private double correctionValue = 0;
    private double hoursFromContact = 24; //quante ore devono passare dal contagio affinché compaiano i sintomi

    public Symptoms() {
        if(correctionValue == 0){
            WeibullDistribution w = new WeibullDistribution(2, 11);
            double increment  = 0.1;
            double upperBound = 10;
            double x = 5;
            double max = 0;
            while(x<upperBound){
                double density = w.density(x);
                if(density > max)
                    max = density;
                x += increment;
            }
            correctionValue = 1/max;
        }
    }

    @Override
    public String getName() {
        return "Symptoms";
    }

    public double updateEvidence(LocalDateTime contactTime, LocalDateTime symptomsTime){
        WeibullDistribution w = new WeibullDistribution(2, 11);
        double delta = ChronoUnit.MINUTES.between(contactTime, symptomsTime)/60.0 - hoursFromContact;
        delta = delta < 0 ? 0 : delta;
        return w.density(delta) * correctionValue;
    }
}
