package com.sweproject.analysis;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import java.time.LocalDateTime;
import java.util.*;

public class Utils {

    private static class PersonScorePair implements Comparable<PersonScorePair>{
        private final String person;
        private final double score;
        private int index = 0; //it's not the rank!!!!

        public PersonScorePair(String person, double score) {
            this.person = person;
            this.score = score;
        }
        public void setIndex(int index){
            this.index = index;
        }
        public int getIndex(){
            return index;
        }

        @Override
        public int compareTo(PersonScorePair o) {
            return Double.compare(this.score, o.score);
        }
    }

    public static HashMap<String, Double> MRR(HashMap<String, TreeMap<LocalDateTime, Double>> groundTruth, HashMap<String, HashMap<Integer, Double>> analysis, int timeLimit) {
        try {
            if (groundTruth.keySet().size() != analysis.keySet().size()) {
                throw new RuntimeException("Different cluster size between simulated solution and analytical one");
            } else {
                //ranking the subjects (Ground Truth)
                LocalDateTime nearestLdt = LocalDateTime.of(1980, 1, 1, 0, 0);
                int index = 0;
                for (LocalDateTime ldt : groundTruth.get(groundTruth.keySet().toArray()[0]).keySet()) {
                    if (ldt.isAfter(nearestLdt) && index < timeLimit - 1 ) {
                        nearestLdt = ldt;
                        index++;
                    }else
                        break;
                }
                TreeMap<Double, HashSet<String>> rankingsGT = new TreeMap<>();
                for (String person : groundTruth.keySet()) {
                    double currentValue = groundTruth.get(person).get(nearestLdt);
                    rankingsGT.computeIfAbsent(currentValue, k -> new HashSet<>()).add(person);
                }

                //ranking the subjects (Analysis)
                int maxIndex =  0;
                for (Integer i : analysis.get(analysis.keySet().toArray()[0]).keySet()) {
                    if (i > maxIndex) {
                        maxIndex = i;
                    }
                }
                TreeMap<Double, HashSet<String>> rankingsAN = new TreeMap<>();
                for (String person: analysis.keySet()){
                    double currentValue = analysis.get(person).get(maxIndex);
                    rankingsAN.computeIfAbsent(currentValue, k -> new HashSet<>()).add(person);
                }

                System.out.println("~~~~~~~~~~~");
                ArrayList<HashSet<String>> orderedRankingsGT = new ArrayList<>();
                for(Map.Entry<Double, HashSet<String>> entry : rankingsGT.entrySet()){
                    orderedRankingsGT.add(entry.getValue());
                    System.out.println(entry.getKey() + " " + entry.getValue());
                }
                System.out.println("~~~~~~~~~~~");
                ArrayList<HashSet<String>> orderedRankingsAN = new ArrayList<>();
                for (Map.Entry<Double, HashSet<String>> entry : rankingsAN.entrySet()){
                    orderedRankingsAN.add(entry.getValue());
                    System.out.println(entry.getKey() + " " + entry.getValue());
                }
                System.out.println("~~~~~~~~~~~");

                int iterations = 0;
                HashMap<String, Double> mrr = new HashMap<>();
                //for each person in the ground truth
                //find their position in orderedRankingGT
                //find their position in orderedRankingAN
                //calculate the score
                for (String s : groundTruth.keySet()){
                    int indexOfsInGT = -1;
                    int indexOfsInAN = -1;
                    for (HashSet<String> hs : orderedRankingsGT){
                        indexOfsInGT++;
                        if (hs.contains(s)){
                            break;
                        }
                    }
                    for (HashSet<String> hs : orderedRankingsAN){
                        indexOfsInAN++;
                        if (hs.contains(s)){
                            break;
                        }
                    }
                    double score = 1 / (double)(Math.abs(indexOfsInGT - indexOfsInAN) + 1);
                    mrr.put(s, score);
                }
               // System.out.println("According to the simulation, " + personMaxSim + " should be tested.\nAccording to the numerical analysis, " + personMaxAn + " should be tested.");
                return mrr;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static double topXaccuracy(HashMap<String, TreeMap<LocalDateTime, Double>> groundTruth, HashMap<String, HashMap<Integer, Double>> analysis, int x, int timeLimit){
        try {
            if (groundTruth.keySet().size() != analysis.keySet().size()) {
                throw new RuntimeException("Different cluster size between simulated solution and analytical one");
            } else {
                //ranking the subjects (Ground Truth)
                LocalDateTime nearestLdt = LocalDateTime.of(1980, 1, 1, 0, 0);
                int index = 0;
                for (LocalDateTime ldt : groundTruth.get(groundTruth.keySet().toArray()[0]).keySet()) {
                    if (ldt.isAfter(nearestLdt) && index < timeLimit - 1 ) {
                        nearestLdt = ldt;
                        index++;
                    }else
                        break;
                }
                TreeMap<Double, HashSet<String>> rankingsGT = new TreeMap<>();
                for (String person : groundTruth.keySet()) {
                    double currentValue = groundTruth.get(person).get(nearestLdt);
                    rankingsGT.computeIfAbsent(currentValue, k -> new HashSet<>()).add(person);
                }

                //ranking the subjects (Analysis)
                int maxIndex =  0;
                for (Integer i : analysis.get(analysis.keySet().toArray()[0]).keySet()) {
                    if (i > maxIndex) {
                        maxIndex = i;
                    }
                }
                TreeMap<Double, HashSet<String>> rankingsAN = new TreeMap<>();
                for (String person: analysis.keySet()){
                    double currentValue = analysis.get(person).get(maxIndex);
                    rankingsAN.computeIfAbsent(currentValue, k -> new HashSet<>()).add(person);
                }

                ArrayList<HashSet<String>> orderedRankingsGT = new ArrayList<>();
                for(Map.Entry<Double, HashSet<String>> entry : rankingsGT.entrySet()){
                    orderedRankingsGT.add(entry.getValue());
                }
                ArrayList<HashSet<String>> orderedRankingsAN = new ArrayList<>();
                for (Map.Entry<Double, HashSet<String>> entry : rankingsAN.entrySet()){
                    orderedRankingsAN.add(entry.getValue());
                }
                System.out.println("####### "+ orderedRankingsGT + " ####### " + orderedRankingsAN + " #######");
                int accuracy = 0;
                //for each position in orderedRankinkGT, check if the same position in orderedRankingAN contains the same person
                //for each person in the ground truth, find their position in orderedRankingGT and orderedRankingAN
                boolean found = false;
                for(String s : groundTruth.keySet()){
                    int indexOfsInGT = -1;
                    int indexOfsInAN = -1;
                    for (HashSet<String> hs : orderedRankingsGT){
                        if (hs.contains(s)){
                            indexOfsInGT++;
                            break;
                        }else{
                            indexOfsInGT+=hs.size();
                        }
                    }
                    for (HashSet<String> hs : orderedRankingsAN){
                        if (hs.contains(s)){
                            indexOfsInAN++;
                            break;
                        }else{
                            indexOfsInAN+=hs.size();
                        }
                    }
                    if (indexOfsInGT < x && indexOfsInAN < x){
                        accuracy++;
                    }
                }
                // System.out.println("According to the simulation, " + personMaxSim + " should be tested.\nAccording to the numerical analysis, " + personMaxAn + " should be tested.");

                return Math.min((double) accuracy / x, 1.0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static double normalizedKendallTauDistance(HashMap<String, TreeMap<LocalDateTime, Double>> groundTruth, HashMap<String, HashMap<Integer, Double>> analysis, int timeLimit) {

        if (groundTruth.keySet().size() != analysis.keySet().size()) {
            throw new RuntimeException("Different cluster size between simulated solution and analytical one");
        } else {
            //ranking the subjects (Ground Truth)
            LocalDateTime nearestLdt = LocalDateTime.of(1980, 1, 1, 0, 0);
            int index = 0;
            for (LocalDateTime ldt : groundTruth.get(groundTruth.keySet().toArray()[0]).keySet()) {
                if (ldt.isAfter(nearestLdt) && index < timeLimit - 1) {
                    nearestLdt = ldt;
                    index++;
                } else
                    break;
            }
            List<PersonScorePair> rankingsGT = new ArrayList<>();
            for (String person : groundTruth.keySet()) {
                double currentValue = groundTruth.get(person).get(nearestLdt);
                rankingsGT.add(new PersonScorePair(person, currentValue));
            }

            //ranking the subjects (Analysis)
            int maxIndex = 0;
            for (Integer i : analysis.get(analysis.keySet().toArray()[0]).keySet()) {
                if (i > maxIndex) {
                    maxIndex = i;
                }
            }
            List<PersonScorePair> rankingsAN = new ArrayList<>();
            for (String person : analysis.keySet()) {
                double currentValue = analysis.get(person).get(maxIndex);
                rankingsAN.add(new PersonScorePair(person, currentValue));
            }
            //assign ranks
            for (int i = 0; i < rankingsGT.size(); i++) {
                rankingsGT.get(i).setIndex(i + 1);
            }
            for (int i = 0; i < rankingsAN.size(); i++) {
                rankingsAN.get(i).setIndex(i + 1);
            }

            rankingsGT.sort(Collections.reverseOrder());
            rankingsAN.sort(Collections.reverseOrder());

            //print rankings
            System.out.println("~~~~~~~~~~~");
            for (PersonScorePair p : rankingsGT) {
                System.out.println(p.person + " " + p.score + " " + p.index);
            }
            System.out.println("~~~~~~~~~~~");
            for (PersonScorePair p : rankingsAN) {
                System.out.println(p.person + " " + p.score + " " + p.index);
            }
            //Compute the Kendall Tau distance between the two rankings
            int n = rankingsGT.size();
            assert n == rankingsAN.size();
            int i, j, v = 0;
            boolean a, b;
            for (i = 0; i < n; i++) {
                for (j = i + 1; j < n; j++) {
                    a = rankingsGT.get(i).index < rankingsGT.get(j).index && rankingsAN.get(i).index > rankingsAN.get(j).index;
                    b = rankingsGT.get(i).index > rankingsGT.get(j).index && rankingsAN.get(i).index < rankingsAN.get(j).index;
                    if (a || b) {
                        v++;
                    }
                }
            }
            int kt = Math.abs(v);
            double nkt = (double) kt / (n * (n - 1) / 2.0);
            return nkt;
        }
    }

    public static double spearmansCorrelation(HashMap<String, TreeMap<LocalDateTime, Double>> groundTruth, HashMap<String, HashMap<Integer, Double>> analysis, int timeLimit) {

        if (groundTruth.keySet().size() != analysis.keySet().size()) {
            throw new RuntimeException("Different cluster size between simulated solution and analytical one");
        } else {
            //ranking the subjects (Ground Truth)
            LocalDateTime nearestLdt = LocalDateTime.of(1980, 1, 1, 0, 0);
            int index = 0;
            for (LocalDateTime ldt : groundTruth.get(groundTruth.keySet().toArray()[0]).keySet()) {
                if (ldt.isAfter(nearestLdt) && index < timeLimit - 1) {
                    nearestLdt = ldt;
                    index++;
                } else
                    break;
            }
            List<PersonScorePair> rankingsGT = new ArrayList<>();
            for (String person : groundTruth.keySet()) {
                double currentValue = groundTruth.get(person).get(nearestLdt);
                rankingsGT.add(new PersonScorePair(person, currentValue));
            }

            //ranking the subjects (Analysis)
            int maxIndex = 0;
            for (Integer i : analysis.get(analysis.keySet().toArray()[0]).keySet()) {
                if (i > maxIndex) {
                    maxIndex = i;
                }
            }
            List<PersonScorePair> rankingsAN = new ArrayList<>();
            for (String person : analysis.keySet()) {
                double currentValue = analysis.get(person).get(maxIndex);
                rankingsAN.add(new PersonScorePair(person, currentValue));
            }
            //assign ranks
            for (int i = 0; i < rankingsGT.size(); i++) {
                rankingsGT.get(i).setIndex(i + 1);
            }
            for (int i = 0; i < rankingsAN.size(); i++) {
                rankingsAN.get(i).setIndex(i + 1);
            }

            rankingsGT.sort(Collections.reverseOrder());
            rankingsAN.sort(Collections.reverseOrder());

            double[] rGT = new double[rankingsGT.size()];
            double[] rAN = new double[rankingsAN.size()];
            for (int i = 0; i < rankingsGT.size(); i++) {
                rGT[i] = rankingsGT.get(i).index;
                rAN[i] = rankingsAN.get(i).index;
            }
            SpearmansCorrelation sc = new SpearmansCorrelation();
            return sc.correlation(rGT, rAN);
        }
    }

    public static void progressBar(int currentStep, int totalSteps, String message){
        int barLength = 20;
        int progress = (int)(((double) currentStep / totalSteps) * barLength);
        System.out.print(message + ": [");
        for (int i = 0; i < barLength; i++) {
            if (i < progress) {
                System.out.print("=");
            } else {
                System.out.print(" ");
            }
        }

        System.out.print("] " + (int)(((double) currentStep / totalSteps) * 100) + "%\r");
    }

}