package com.sweproject.main;

import java.util.ArrayList;

public class ObservationRecord {
    private ArrayList<Observation> observations = new ArrayList<>();
    private Subject subject;

    public ObservationRecord(Subject subject) {
        this.subject = subject;
    }

    public void addObservation(Observation observation){
        observations.add(observation);
    }
}
