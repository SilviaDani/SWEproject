package com.sweproject.analysis;

import com.sun.source.tree.Tree;
import com.sweproject.model.Subject;

import java.time.LocalDateTime;
import java.util.*;

public class Utils {

    public static HashMap<String, Double> MRR(HashMap<String, TreeMap<LocalDateTime, Double>> groundTruth, HashMap<String, HashMap<Integer, Double>> analysis) {
        try {
            if (groundTruth.keySet().size() != analysis.keySet().size()) {
                throw new RuntimeException("Different cluster size between simulated solution and analytical one");
            } else {
                //ranking the subjects (Ground Truth)
                LocalDateTime nearestLdt = LocalDateTime.of(1980, 1, 1, 0, 0);
                for (LocalDateTime ldt : groundTruth.get(groundTruth.keySet().toArray()[0]).keySet()) {
                    if (ldt.isAfter(nearestLdt))
                        nearestLdt = ldt;
                }
                TreeMap<Double, List<String>> rankingsGT = new TreeMap<>();
                for (String person : groundTruth.keySet()) {
                    double currentValue = groundTruth.get(person).get(nearestLdt);
                    if (rankingsGT.containsKey(currentValue)){
                        List<String> l = (List<String>)rankingsGT.get(currentValue);
                        l.add(person);
                        rankingsGT.put(currentValue, l);
                    }else{
                        List<String> l = new ArrayList<String>(Arrays.asList(person));
                        rankingsGT.put(currentValue, l);
                    }

                }

                //ranking the subjects (Analysis)
                int maxIndex =  0;
                for (Integer i : analysis.get(analysis.keySet().toArray()[0]).keySet()) {
                    if (i > maxIndex) {
                        maxIndex = i;
                    }
                }
                TreeMap<Double, List<String>> rankingsAN = new TreeMap<>();
                for (String person: analysis.keySet()){
                    double currentValue = analysis.get(person).get(maxIndex);
                    if(rankingsAN.containsKey(currentValue)){
                        List<String> l = rankingsAN.get(currentValue);
                        l.add(person);
                        rankingsAN.put(currentValue, l);
                    }else{
                        List<String> l = new ArrayList<>(Arrays.asList(person));
                        rankingsAN.put(currentValue, l);
                    }
                }

                System.out.println("~~~~~~~~~~~");
                ArrayList<String> orderedRankingsGT = new ArrayList<>();
                for(Map.Entry<Double, List<String>> entry : rankingsGT.entrySet()){
                    orderedRankingsGT.addAll(entry.getValue());
                    System.out.println(entry.getKey() + " " + entry.getValue());
                }
                System.out.println("~~~~~~~~~~~");
                ArrayList<String> orderedRankingsAN = new ArrayList<>();
                for (Map.Entry<Double, List<String>> entry : rankingsAN.entrySet()){
                    orderedRankingsAN.addAll(entry.getValue());
                    System.out.println(entry.getKey() + " " + entry.getValue());
                }
                System.out.println("~~~~~~~~~~~");

                int iterations = 0;
                HashMap<String, Double> mrr = new HashMap<>();
                for (String s : orderedRankingsGT){
                    int indexOfsInGT = orderedRankingsGT.indexOf(s);
                    int indexOfsInAT = orderedRankingsAN.indexOf(s);
                    double score = 1 / (double)(Math.abs(indexOfsInGT - indexOfsInAT) + 1);
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

    public static double topXaccuracy(HashMap<String, TreeMap<LocalDateTime, Double>> groundTruth, HashMap<String, HashMap<Integer, Double>> analysis, int x){
        try {
            if (groundTruth.keySet().size() != analysis.keySet().size()) {
                throw new RuntimeException("Different cluster size between simulated solution and analytical one");
            } else {
                //ranking the subjects (Ground Truth)
                LocalDateTime nearestLdt = LocalDateTime.of(1980, 1, 1, 0, 0);
                for (LocalDateTime ldt : groundTruth.get(groundTruth.keySet().toArray()[0]).keySet()) {
                    if (ldt.isAfter(nearestLdt))
                        nearestLdt = ldt;
                }
                TreeMap<Double, List<String>> rankingsGT = new TreeMap<>();
                for (String person : groundTruth.keySet()) {
                    double currentValue = groundTruth.get(person).get(nearestLdt);
                    if (rankingsGT.containsKey(currentValue)){
                        List<String> l = (List<String>)rankingsGT.get(currentValue);
                        l.add(person);
                        rankingsGT.put(currentValue, l);
                    }else{
                        List<String> l = new ArrayList<String>(Arrays.asList(person));
                        rankingsGT.put(currentValue, l);
                    }

                }

                //ranking the subjects (Analysis)
                int maxIndex =  0;
                for (Integer i : analysis.get(analysis.keySet().toArray()[0]).keySet()) {
                    if (i > maxIndex) {
                        maxIndex = i;
                    }
                }
                TreeMap<Double, List<String>> rankingsAN = new TreeMap<>();
                for (String person: analysis.keySet()){
                    double currentValue = analysis.get(person).get(maxIndex);
                    if(rankingsAN.containsKey(currentValue)){
                        List<String> l = rankingsAN.get(currentValue);
                        l.add(person);
                        rankingsAN.put(currentValue, l);
                    }else{
                        List<String> l = new ArrayList<>(Arrays.asList(person));
                        rankingsAN.put(currentValue, l);
                    }
                }

                ArrayList<String> orderedRankingsGT = new ArrayList<>();
                for(Map.Entry<Double, List<String>> entry : rankingsGT.entrySet()){
                    orderedRankingsGT.addAll(entry.getValue());
                }
                ArrayList<String> orderedRankingsAN = new ArrayList<>();
                for (Map.Entry<Double, List<String>> entry : rankingsAN.entrySet()){
                    orderedRankingsAN.addAll(entry.getValue());
                }
                double accuracy = 0;
                ArrayList<String> topXGT = new ArrayList<>();
                ArrayList<String> topXAN = new ArrayList<>();
                for (int i = 0; i<x; i++){
                topXGT.add(orderedRankingsGT.get(orderedRankingsGT.size() - 1 - i));
                topXAN.add(orderedRankingsAN.get(orderedRankingsAN.size() - 1 - i));
                }
                topXGT.retainAll(topXAN);
                accuracy = (double)topXGT.size() / x;
                // System.out.println("According to the simulation, " + personMaxSim + " should be tested.\nAccording to the numerical analysis, " + personMaxAn + " should be tested.");
                return accuracy;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}