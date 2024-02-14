package com.sweproject.analysis;

import com.sun.source.tree.Tree;
import com.sweproject.model.Subject;

import java.time.LocalDateTime;
import java.util.*;

public class Utils {

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
                return (double)accuracy/x;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}