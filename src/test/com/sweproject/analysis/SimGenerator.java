package com.sweproject.analysis;

import com.sweproject.model.Environment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class SimGenerator {
    /**
     * This class is used to generate a simulated dataset for the analysis of the system. fixme:TBD
     */
    Random r = new Random();
    private int n;
    private int min_nEnvironment;
    private int max_nEnvironment;
    private int totalContacts;
    private int min_nSymptoms;
    private int max_nSymptoms;
    private int min_nCovTests;
    private int max_nCovTests;
    private int length;

    SimGenerator(int n, int min_nEnvironment, int max_nEnvironment, int totalContacts, int min_nSymptoms, int max_nSymptoms, int min_nCovTests, int max_nCovTests, int length){
    this.n = n;
    this.min_nEnvironment = min_nEnvironment;
    this.max_nEnvironment = max_nEnvironment;
    this.totalContacts = totalContacts;
    this.min_nSymptoms = min_nSymptoms;
    this.max_nSymptoms = max_nSymptoms;
    this.min_nCovTests = min_nCovTests;
    this.max_nCovTests = max_nCovTests;
    this.length = length;

    }
    SimGenerator(int n, int min_nEnvironment, int max_nEnvironment, int totalContacts, int min_nSymptoms, int max_nSymptoms, int min_nCovTests, int max_nCovTests, int seed, int length){
    this(n, min_nEnvironment, max_nEnvironment, totalContacts, min_nSymptoms, max_nSymptoms, min_nCovTests, max_nCovTests, length);
    r.setSeed(seed);
    }

    /* FIXME: TBD
    public HashMap<> generate(){
        HashMap<String, ArrayList<>> data = new HashMap<>();

        //-----------------generate n subjects-----------------
        ArrayList<Subject> people = new ArrayList<>();
        for (int person = 0; person < this.n; person++){
            people.add(new Subject("P"+person));
        }
        data.put("PEOPLE", people);

        //-----------------generate environmental events-----------------
        for(int person = 0; person < this.n; person++){
            int nEnvironment = r.nextInt(this.max_nEnvironment - this.min_nEnvironment) + this.min_nEnvironment;
            ArrayList<Environment> envs = new ArrayList<>();
            for (int env = 0; env < nEnvironment; env++){
            }
            data.put("ENVIRONMENT_P"+person, envs);
        }

        return data;
    }*/

    private void loadOnDatabase(){

    }

    private HashMap<String, ArrayList<LocalDateTime>> generateTimestamps(LocalDateTime t0, int nEvents){
        ArrayList<LocalDateTime> timestamps = new ArrayList<>();
        for (int i = 0; i < (nEvents * 2); i++){
            int hours = r.nextInt(length - 1) + 1;
            LocalDateTime timestamp = t0.plusHours(hours);
            timestamps.add(timestamp);
        }
        Collections.sort(timestamps);
        HashMap<String, ArrayList<LocalDateTime>> ts = new HashMap<>();
        ArrayList<LocalDateTime> startTimestamps = new ArrayList<>();
        ArrayList<LocalDateTime> endTimestamps = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++){
            if(i % 2 == 0){
                startTimestamps.add(timestamps.get(i));
            }else{
                endTimestamps.add(timestamps.get(i));
            }
        }
        ts.put("START", startTimestamps);
        ts.put("END", endTimestamps);
        return ts;
    }
}
