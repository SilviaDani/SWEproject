package com.sweproject.model;

import java.util.ArrayList;

public class Subject {
    private ArrayList<Observation> observationRecord;
    private ArrayList<Prescription> prescriptionRecord;
    //TODO add infos about the subject (ie name...)
    public Subject() {
        observationRecord = new ArrayList<>();
        prescriptionRecord = new ArrayList<>();
    }

    public void addObservation(Observation observation){
        observationRecord.add(observation);
    }

    public void addPrescription(Prescription prescription){
        prescriptionRecord.add(prescription);
    }

    public void getProbabilityOfBeingInfected(){
        //TODO
    }

    public ArrayList<Observation> getObservationRecord() {
        return observationRecord;
    }

    public ArrayList<Prescription> getPrescriptionRecord() {
        return prescriptionRecord;
    }
}
