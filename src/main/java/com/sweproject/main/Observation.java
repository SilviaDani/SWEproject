package com.sweproject.main;
import java.util.ArrayList;

public class Observation {
    private TimeRecord timeRecord; //XXX (CASO SINTOMI) All'inizio viene inserita solo la data di comparsa dei sintomi, poi in un secondo momento viene inserita la data di fine sintomi con conseguente cambio di tipo dell'oggetto: da Date a TimeInterval
    private Status status;
    private Type type;
    private ArrayList<Subject> subjects;

    public Observation(ArrayList<Subject> subjects, Type type, TimeRecord timeRecord) {
        this.timeRecord = timeRecord;
        this.status = Status.RELEVANT;
        this.type = type;
        this.subjects = subjects;
    }
    //TODO changeStatus()
}
