package com.sweproject.controller;

import com.sweproject.model.Observation;
import com.sweproject.model.Subject;
import com.sweproject.model.TimeRecord;
import com.sweproject.model.Type;

import java.util.ArrayList;

public class Notifier {
    private Subject subject;

    public Notifier(String fiscalCode, String name, String surname) {
        subject = new Subject(fiscalCode, name,surname);
    }

    public void createObservation(ArrayList<Subject> cluster, Type type, TimeRecord timeRecord){
        Observation observation = new Observation(cluster, type, timeRecord);
        subject.addObservation(observation);
    }
    public void getProbabilityOfBeingInfected(){//FIXME
        subject.getProbabilityOfBeingInfected();
    }

    //for testing
    public Subject getSubject() {
        return subject;
    }

    public String getName() {
        return subject.getName();
    }
    public String getFiscalCode(){
        return subject.getFiscalCode();
    }
}
