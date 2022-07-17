package com.sweproject.main;

import java.util.ArrayList;

public class Contact extends Type{
    private ArrayList<Subject> subjects;
    private float trasmissionChance;

    public Contact(ArrayList<Subject> subjects, float trasmissionChance) {
        this.trasmissionChance = trasmissionChance;
        this.subjects = subjects;
    }
}
