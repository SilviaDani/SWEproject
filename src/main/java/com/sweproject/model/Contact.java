package com.sweproject.model;

import java.util.ArrayList;

public class Contact extends Type{
    private ArrayList<Subject> subjects;
    private float trasmissionChance;
    private ArrayList<String> cluster;

    public Contact(ArrayList<Subject> subjects, float trasmissionChance) {
        this.trasmissionChance = trasmissionChance;
        this.subjects = subjects;
    }

    public Contact(ArrayList<String> cluster){
        this.cluster = cluster;
    }


    @Override
    public String getName() {
        return "Contact";
    }
}
