package com.sweproject.controller;

import com.sweproject.model.*;

public class Tracer extends Notifier{

    public void createPrescription(Subject subject, Date date, CovidTest covidTest){
        Prescription prescription = new Prescription(subject, date, covidTest, this);
        subject.addPrescription(prescription);
    }

    public void editObservation(Observation observation){
        //TODO
        observation.setAsIrrelevant();
    }

    public void getProbabilityOfBeingInfected(Subject subject){//FIXME
        subject.getProbabilityOfBeingInfected();
    }
}
