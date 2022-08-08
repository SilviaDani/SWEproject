package com.sweproject.model;

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

    public Subject getSubject() {
        return subject;
    }

    public Date getDate() {
        return date;
    }

    public CovidTest getCovidTest() {
        return covidTest;
    }

    public Tracer getTracer() {
        return tracer;
    }
}
