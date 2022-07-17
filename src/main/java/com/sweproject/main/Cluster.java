package com.sweproject.main;

import java.util.ArrayList;

public class Cluster {
    private ArrayList<Subject> subjects;
    private TimeInterval duration;

    public Cluster(ArrayList<Subject> subjects, TimeInterval duration) {
        this.subjects = subjects;
        this.duration = duration;
    }
}
