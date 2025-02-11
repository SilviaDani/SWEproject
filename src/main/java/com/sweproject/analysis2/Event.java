package com.sweproject.analysis2;

public class Event {
    String type;
    String[] involvedSubjects;
    int time;
    Double riskFactor;
    Boolean result;

    public Event(String type, String[] involvedSubjects, int time, Double riskFactor, Boolean result) {
        this.type = type;
        this.involvedSubjects = involvedSubjects;
        this.time = time;
        this.riskFactor = riskFactor;
        this.result = result;
    }


}
