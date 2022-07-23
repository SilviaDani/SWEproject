package com.sweproject.main;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Notifier foo = new Notifier();
        ArrayList<Subject> bar = new ArrayList<>();
        bar.add(foo.getSubject());
        Type t = new Symptoms();
        TimeRecord tr = new Date(LocalDateTime.now());
        foo.createObservation(bar, t, tr);

        Tracer tracer = new Tracer();
        tracer.createPrescription(foo.getSubject(), new Date(LocalDateTime.of(2000,9,7,11,20)), new CovidTest(CovidTestType.MOLECULAR));

    }
}
