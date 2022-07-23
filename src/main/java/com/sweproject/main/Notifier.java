package com.sweproject.main;

import java.util.ArrayList;

public class Notifier {
    private Subject subject;

    public Notifier() {
        subject = new Subject();
    }

    public void createObservation(ArrayList<Subject> cluster, Type type, TimeRecord timeRecord){
        Observation observation = new Observation(cluster, type, timeRecord);
        subject.addObservation(observation);
    }
    public void getProbabilityOfBeingInfected(){//FIXME
        subject.getProbabilityOfBeingInfected();
    }

    public Subject getSubject() {
        return subject;
    }
}
