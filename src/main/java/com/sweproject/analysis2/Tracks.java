package com.sweproject.analysis2;

import java.util.Arrays;
import java.util.HashMap;

public class Tracks {
    HashMap<String, Double[]> tracks;
    String[] names;
    int nSamples;

    public Tracks(String[] names, int nSamples) {
        this.names = names;
        this.nSamples = nSamples;
        tracks = new HashMap<>();

        for (String name : names) {
            tracks.put(name, new Double[nSamples]);
            Arrays.fill(tracks.get(name), 0.0);
        }
        System.out.println(tracks);
        // print the values of the tracks
        for (String name : names) {
            System.out.println(name + ": " + Arrays.toString(tracks.get(name)));
        }
    }

    public void editTrack (String name, int time, Double value){
        tracks.get(name)[time] = value;
    }

    public Double getSample (String name, int time){
        return tracks.get(name)[time];
    }

    public Tracks copy(){
        Tracks copy = new Tracks(Arrays.copyOf(this.names, this.names.length), this.nSamples);
        for (String name : names) {
            copy.tracks.put(name, Arrays.copyOf(this.tracks.get(name), this.nSamples));
        }
        // assert copy and this.tracks different object but with equal values
        for (String name : names){
            if (copy.tracks.get(name) == this.tracks.get(name)) throw new AssertionError("Same object");
            if (!Arrays.equals(copy.tracks.get(name), this.tracks.get(name))) throw new AssertionError("Track values are different :(");
        }
        return copy;
    }
}
