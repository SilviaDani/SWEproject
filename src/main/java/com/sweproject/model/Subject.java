package com.sweproject.model;

import java.util.ArrayList;

public class Subject {
    private ArrayList<Observation> observationRecord;
    private ArrayList<Prescription> prescriptionRecord;
    //TODO add infos about the subject (ie name...)
    private String fiscalCode;
    private String name;
    private String surname;

    public Subject(String fiscalCode, String name, String surname) {
        observationRecord = new ArrayList<>();
        prescriptionRecord = new ArrayList<>();
        this.fiscalCode = fiscalCode;
        this.name = name;
        this.surname = surname;
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

    public String getFiscalCode() {
        return this.fiscalCode;
    }

    public String getName() {
        return name;
    }
}
