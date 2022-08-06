package com.sweproject.controller;

import com.sweproject.model.Observation;
import com.sweproject.model.Subject;
import com.sweproject.model.TimeRecord;
import com.sweproject.model.Type;

import java.util.ArrayList;

public class Notifier {
    private Subject subject;

    public Notifier() {
        //TODO: È NECESSARIO SAPERE IL CODICE FISCALE DELL'UTENTE UNA VOLTA CHE È ENTRATO NELL'APP
        subject = new Subject("RSSMRA80A01H501U");
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
}
