package com.sweproject.main;
import java.util.ArrayList;

public class Observation {
    private TimeRecord timeRecord; //XXX (CASO SINTOMI) All'inizio viene inserita solo la data di comparsa dei sintomi, poi in un secondo momento viene inserita la data di fine sintomi con conseguente cambio di tipo dell'oggetto: da Date a TimeInterval
    private boolean isRelevant;
    private Type type;
    private ArrayList<Subject> subjects;

    public Observation(ArrayList<Subject> subjects, Type type, TimeRecord timeRecord) {
        this.timeRecord = timeRecord;
        isRelevant = true;
        this.type = type;
        this.subjects = subjects;
    }


    public void changeStatus(){
        isRelevant = !isRelevant;
        //XXX serve che lo status da non rilevante torni rilevante?
    }

    public TimeRecord getTimeRecord() {
        return timeRecord;
    }

    public boolean isRelevant() {
        return isRelevant;
    }

    public Type getType() {
        return type;
    }

    public ArrayList<Subject> getSubjects() {
        return subjects;
    }
}
