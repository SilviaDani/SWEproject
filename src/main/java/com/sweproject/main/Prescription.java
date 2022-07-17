package com.sweproject.main;

public class Prescription {
    private Subject subject;
    private Date date;
    private CovidTest covidTest;
    private Tracer tracer;

    public Prescription(Subject subject, Date date, CovidTest covidTest, Tracer tracer) {
        this.subject = subject;
        this.date = date;
        this.covidTest = covidTest;
        this.tracer = tracer;
    }
}
