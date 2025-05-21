
package com.sweproject.analysis2;

import com.google.gson.JsonObject;
import com.sweproject.model.Subject;
import javafx.scene.chart.XYChart;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedGEN;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trans.TreeTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;



public class STPNAnalysis {
    // 1. Build model
    // 2. Set Marking
    // 3. Run analysis
    // 4. Get Solution

    public static <R, S> TransientSolution<R, S> buildModel(int samples, float step){
        PetriNet net = new PetriNet();
        Marking marking = new Marking();

        Place Asymptomatic = net.addPlace("Asymptomatic");
        Place DevelopingSymptoms = net.addPlace("DevelopingSymptoms");
        Place EffectiveContact = net.addPlace("EffectiveContact");
        Place Healed = net.addPlace("Healed");
        Place Infected = net.addPlace("Infected");
        Place Infectious = net.addPlace("Infectious");
        Place Isolated = net.addPlace("Isolated");
        Place Symptomatic = net.addPlace("Symptomatic");
        Place Symptomatology = net.addPlace("Symptomatology");
        Place p0 = net.addPlace("p0");
        Place p1 = net.addPlace("p1");
        Place p2 = net.addPlace("p2");
        Place p3 = net.addPlace("p3");
        Place p4 = net.addPlace("p4");
        Place p5 = net.addPlace("p5");
        Place p6 = net.addPlace("p6");
        Place p7 = net.addPlace("p7");
        Transition effectiveContact = net.addTransition("effectiveContact");
        Transition noSymptoms = net.addTransition("noSymptoms");
        Transition symptoms = net.addTransition("symptoms");
        Transition t0 = net.addTransition("t0");
        Transition t1 = net.addTransition("t1");
        Transition t10 = net.addTransition("t10");
        Transition t11 = net.addTransition("t11");
        Transition t12 = net.addTransition("t12");
        Transition t13 = net.addTransition("t13");
        Transition t14 = net.addTransition("t14");
        Transition t15 = net.addTransition("t15");
        Transition t2 = net.addTransition("t2");
        Transition t3 = net.addTransition("t3");
        Transition t5 = net.addTransition("t5");
        Transition t6 = net.addTransition("t6");
        Transition t7 = net.addTransition("t7");
        Transition t8 = net.addTransition("t8");
        Transition t9 = net.addTransition("t9");

        //Generating Connectors
        net.addPostcondition(symptoms, DevelopingSymptoms);
        net.addPrecondition(p7, t14);
        net.addPrecondition(p3, t11);
        net.addPrecondition(DevelopingSymptoms, t0);
        net.addPrecondition(Symptomatology, symptoms);
        net.addPrecondition(p5, t9);
        net.addPostcondition(t7, p3);
        net.addPrecondition(Symptomatology, noSymptoms);
        net.addPrecondition(Infectious, t5);
        net.addPostcondition(t1, p1);
        net.addPostcondition(t3, Symptomatic);
        net.addPostcondition(t6, Healed);
        net.addPostcondition(t10, p7);
        net.addPostcondition(t2, Symptomatic);
        net.addPostcondition(effectiveContact, Symptomatology);
        net.addPrecondition(Asymptomatic, t5);
        net.addPostcondition(t13, Infectious);
        net.addPostcondition(t12, p5);
        net.addPostcondition(t8, p4);
        net.addPrecondition(p2, t6);
        net.addPostcondition(t15, Isolated);
        net.addPostcondition(t5, p2);
        net.addPostcondition(t11, p5);
        net.addPrecondition(Symptomatic, t15);
        net.addPrecondition(Infected, t8);
        net.addPrecondition(Infectious, t15);
        net.addPrecondition(EffectiveContact, effectiveContact);
        net.addPostcondition(effectiveContact, Infected);
        net.addPrecondition(p1, t3);
        net.addPrecondition(Infected, t7);
        net.addPrecondition(DevelopingSymptoms, t1);
        net.addPostcondition(t9, p6);
        net.addPrecondition(p0, t2);
        net.addPostcondition(t14, Infectious);
        net.addPostcondition(t0, p0);
        net.addPrecondition(p4, t12);
        net.addPrecondition(p5, t10);
        net.addPrecondition(p6, t13);
        net.addPostcondition(noSymptoms, Asymptomatic);

        //Generating Properties
        marking.setTokens(Asymptomatic, 0);
        marking.setTokens(DevelopingSymptoms, 0);
        marking.setTokens(EffectiveContact, 1);
        marking.setTokens(Healed, 0);
        marking.setTokens(Infected, 0);
        marking.setTokens(Infectious, 0);
        marking.setTokens(Isolated, 0);
        marking.setTokens(Symptomatic, 0);
        marking.setTokens(Symptomatology, 0);
        marking.setTokens(p0, 0);
        marking.setTokens(p1, 0);
        marking.setTokens(p2, 0);
        marking.setTokens(p3, 0);
        marking.setTokens(p4, 0);
        marking.setTokens(p5, 0);
        marking.setTokens(p6, 0);
        marking.setTokens(p7, 0);
        effectiveContact.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        effectiveContact.addFeature(new Priority(0));
        noSymptoms.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.35", net)));
        noSymptoms.addFeature(new Priority(0));
        symptoms.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.65", net)));
        symptoms.addFeature(new Priority(0));
        t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.81", net)));
        t0.addFeature(new Priority(0));
        t1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.19", net)));
        t1.addFeature(new Priority(0));
        t10.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.11", net)));
        t10.addFeature(new Priority(0));
        t11.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.6958 / 24", net)));
        t12.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.1626 / 24", net)));
        t13.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("1.357/24", net)));
        t14.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.170/24", net)));
        t15.addFeature(StochasticTransitionFeature.newUniformInstance(new BigDecimal("0"), new BigDecimal("24")));
        t2.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.6958 / 24", net)));
        t3.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.1626 / 24", net)));
        t5.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("1/(10.68 * 24)", net)));
        t6.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("1/(1.27 * 24)", net)));
        t7.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.81", net)));
        t7.addFeature(new Priority(0));
        t8.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.19", net)));
        t8.addFeature(new Priority(0));
        t9.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("0.89", net)));
        t9.addFeature(new Priority(0));

        // Run analysis
        TreeTransient analysis = TreeTransient.builder()
                .greedyPolicy(new BigDecimal(samples), BigDecimal.ZERO)
                .timeStep(new BigDecimal(step))
                .build();

        TransientSolution<Marking, Marking> result = analysis.compute(net, marking);

        var rewardRates = TransientSolution.rewardRates("Infectious");
        var rewardedSolution = TransientSolution.computeRewards(false, result, rewardRates);

//        System.out.println(rewardedSolution);

        return (TransientSolution<R, S>) rewardedSolution;




    }


    public static void fillWithGranularity(HashMap<Double, Double> map) {
        HashMap<Double, Double> filledMap = new HashMap<>();
        Double[] keys = map.keySet().toArray(new Double[0]);
        Arrays.sort(keys);

        for (int i = 0; i < keys.length - 1; i++) {
            double start = keys[i];
            double end = keys[i + 1];
            double startValue = map.get(start);
            double endValue = map.get(end);
            double step = (endValue - startValue) / (end - start);


            for (double j = start; j <= end; j++) {
                double key = Math.round(j * 10.0) / 10.0; // Round to one decimal place
//                System.out.println("Key: " + key);
                filledMap.put(key, startValue + (j - start) * step);
            }
        }

        // Add the last key-value pair
        filledMap.put(keys[keys.length - 1], map.get(keys[keys.length - 1]));

        // Replace the original map with the filled map
        map.clear();
        map.putAll(filledMap);
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        // read from json
        Path startPath = Path.of("C:\\Users\\super\\Documents\\Uni\\Chita\\Simulator");
        List<String> jsonFiles = new ArrayList<>();

        float time_step = 0.1f;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(startPath)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && entry.toString().endsWith("simulated.json")) {
                    jsonFiles.add(entry.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        jsonFiles.forEach(System.out::println);
        HashMap<Double, Double> phi = new HashMap<>(); // curve of relevance of symptoms
        // the key is in DAYS * 24 (hours)
        phi.put(0.0, 0.0);
        phi.put(2.5 * (int)(Math.round(24.0 / time_step)), 0.65 * 0.75);
        phi.put(4.0 * (int)(Math.round(24.0 / time_step)), 0.65 * 0.95);
        phi.put(10.0 * (int)(Math.round(24.0 / time_step)), 0.65 * 0.75);
        phi.put(15.0 * (int)(Math.round(24.0 / time_step)), 0.65 * 0.5);
        phi.put(20.0 * (int)(Math.round(24.0 / time_step)), 0.65 * 0.25);
        phi.put(25.0 * (int)(Math.round(24.0 / time_step)), 0.65 * 0.125);
        phi.put(30.0 * (int)(Math.round(24.0 / time_step)), 0.65 * 0.0625);
        // fill
        fillWithGranularity(phi); // FIXME check timestep

        HashMap<Double, Double> theta = new HashMap<>(); // curve of relevance of tests
        // the key is in DAYS * 24 (hours)
        theta.put(0.0, 0.0);
        theta.put(2.5 * (int)(Math.round(24.0 / time_step)), 0.75);
        theta.put(4.0 * (int)(Math.round(24.0 / time_step)), 0.95);
        theta.put(14.0 * (int)(Math.round(24.0 / time_step)), 0.75);
        theta.put(19.0 * (int)(Math.round(24.0 / time_step)), 0.5);
        theta.put(24.0 * (int)(Math.round(24.0 / time_step)), 0.25);
        theta.put(29.0 * (int)(Math.round(24.0 / time_step)), 0.125);
        theta.put(34.0 * (int)(Math.round(24.0 / time_step)), 0.0625);
        // fill
        fillWithGranularity(theta); // FIXME check timestep
        ArrayList<double[]> priorsCombinations = new ArrayList<>();
        // generate all combinations of priorsValues
        priorsCombinations.add(new double[]{0.25, 0.25, 0.75});
        priorsCombinations.add(new double[]{0.5, 0.5, 0.5});
        priorsCombinations.add(new double[]{0.75, 0.75, 0.25});

        for(int documentId = 0; documentId < jsonFiles.size(); documentId++) {
            for (double[] priorsValues : priorsCombinations) {
                String filePath = jsonFiles.get(documentId);
                String documentName = filePath.substring(filePath.lastIndexOf("\\") + 1, filePath.lastIndexOf("."));
                System.out.println("Document: " + documentName);
                JsonObject jsonObject = JsonFileReader.readJsonFromFile(filePath);
                int n_subjects = 0;
                int time_limit = 0;

                Queue<Event> events = new LinkedList<>();
//            double[] priorsValues = {0.25, 0.5, 0.75};
                Random random = new Random();
                if (jsonObject != null) {
                    n_subjects = jsonObject.get("n_subjects").getAsInt();
                    time_limit = jsonObject.get("time_limit").getAsInt();
//                System.out.println(jsonObject);
                    // print on a new line every element of the json object["events"]
                    jsonObject.getAsJsonArray("events").forEach(System.out::println);
                    // parse the events
                    for (int i = 0; i < jsonObject.getAsJsonArray("events").size(); i++) {
                        JsonObject event = jsonObject.getAsJsonArray("events").get(i).getAsJsonObject();
                        String[] involved_subjects = new String[event.get("involved_subjects").getAsJsonArray().size()];
                        for (int j = 0; j < event.get("involved_subjects").getAsJsonArray().size(); j++) {
                            involved_subjects[j] = event.get("involved_subjects").getAsJsonArray().get(j).getAsString();
                        }
                        Double riskFactor = event.has("risk_factor") && !event.get("risk_factor").isJsonNull() ? event.get("risk_factor").getAsDouble() : null;
                        Boolean result = event.has("result") && !event.get("result").isJsonNull() ? event.get("result").getAsBoolean() : null;
                        events.add(new Event(event.get("type").getAsString(),
                                involved_subjects,
                                event.get("time").getAsInt(),
                                riskFactor,
                                result));
                    }
                } else {
                    throw new Exception("Failed to read JSON from file.");
                }
                int time_horizon = (int)(Math.round(time_limit * 24.0 / time_step));
                System.out.println("Time horizon: " + time_horizon);

                TransientSolution<Integer, Double> solution = buildModel(time_limit * 24, time_step);

                // Create the names based on n_subjects
                String[] names = new String[n_subjects];
                for (int i = 0; i < n_subjects; i++) {
                    names[i] = String.valueOf(i + 1);
                }


                int n_iterations = n_subjects - 1;
                if (n_iterations == n_subjects || n_iterations <= 0) throw new AssertionError();
                HashMap<Integer, Tracks> tracks_record = new HashMap<>();

                // Here we iterate over the symptoms and tests to gather that information
                ArrayList<Event> symptomsAndTests = new ArrayList<>();
                for (Event event : events) {
                    if (((event.type.equals("Symptoms") && event.result.equals(true)) || event.type.equals("Test")) && event.time < time_limit) { // we assume that the symptoms are only for the subject involved in the external contact
                        symptomsAndTests.add(event);
                    }
                }

                for (int current_iteration = 0; current_iteration < n_iterations; current_iteration++) {
                    HashMap<String, double[]> probabilityOfNotBeingInfectedDueToPreviousContact = new HashMap<>();
                    // HashMap<String, HashMap<Integer, String>> symptomsAndTestsRecords = new HashMap<>(); // key: subject, value: key: time, value : type
                    for (int i = 0; i < n_subjects; i++) {
                        probabilityOfNotBeingInfectedDueToPreviousContact.put(String.valueOf(i + 1), new double[time_horizon]); // \prod_{j=0}^{k-1} 1 - r_{s,j}^{ext}(t) # XXX it should be 15 x 6720
                        Arrays.fill(probabilityOfNotBeingInfectedDueToPreviousContact.get(String.valueOf(i + 1)), 1.0);

                        // symptomsAndTestsRecords.put(String.valueOf(i + 1), new HashMap<>());
                    }
                    tracks_record.put(current_iteration, new Tracks(names, time_horizon)); // FIXME check the size of the array
                    for (Event event : events) {
                        int eventTime = Math.round(event.time / time_step); // this is the event time scaled by the time step
                        if (eventTime >= time_horizon) {
//                        System.err.println("##############\\Event @ " + eventTime + " is out of bounds");
                            continue;
                        }
                        String[] involvedSubjects = event.involvedSubjects;

                        if (current_iteration == 0 && event.type.equals("External")) {
//                        System.out.println("\u001B[36mExternal event @ " + eventTime + "\u001B[0m");
                            assert involvedSubjects.length == 1;
                            String involvedSubject = involvedSubjects[0];
                            int offset_external = 0;
                            double riskFactor_external = event.riskFactor;
                            while (eventTime + offset_external < time_horizon) {
                                Double previousValue = tracks_record.get(current_iteration).getSample(involvedSubject, eventTime + offset_external);
                                // c(t, \tau) is the probability that a subject is infectious and not isolated at time t given an effective contact at time \tau (computed by forward transient analysis)
                                // P{w_h | e_{s,k} is effective} of an observation w_h given that the k-th external contact e_{s,k} of subject s occurred at time \tau_{s,k}^ext is effective
                                // r_{s,k}^{ext} = P{w_h | e_{s,k} is effective} * riskFactor_external / prior
                                Double Pw_h_given_e_s_k_is_effective = 1.0;
                                Double prior = 1.0;
                                for (int index = 0; index < symptomsAndTests.size(); index++) {
                                    Event entry = symptomsAndTests.get(index);
//                                    System.out.println("Keys in phi: " + phi.keySet());
//                                    System.out.println("Computed key: " + ((double) entry.time - (double) eventTime));
                                    double desiredKey = (double)Math.round(((double) entry.time - (double) eventTime) / time_step);
//                                    System.out.println("Desired key: " + desiredKey);
                                    if (desiredKey < 0.0 || desiredKey >= time_horizon){
                                        continue;
                                    }
                                    if (entry.type.equals("Symptoms") && entry.result.equals(true) && entry.time >= eventTime && entry.involvedSubjects[0].equals(involvedSubject)) { // we assume that the symptoms are only for the subject involved in the external contact
                                        Pw_h_given_e_s_k_is_effective *= phi.get(desiredKey);
                                        prior *= priorsValues[0];
                                    } else if (entry.type.equals("Test") && entry.time >= eventTime && entry.involvedSubjects[0].equals(involvedSubject)) {
                                        if (entry.result) {
                                            Pw_h_given_e_s_k_is_effective *= theta.get(desiredKey);
                                            prior *= priorsValues[1];
                                        } else {
                                            Pw_h_given_e_s_k_is_effective *= (1.0 - theta.get(desiredKey));
                                            prior *= priorsValues[2];
                                        }
                                    }
                                }
                                Double r_ext = Pw_h_given_e_s_k_is_effective * riskFactor_external / prior;
//                            r_ext = riskFactor_external;


                                Double newValue = solution.getSolution()[offset_external][0][0] * r_ext * probabilityOfNotBeingInfectedDueToPreviousContact.get(involvedSubject)[eventTime + offset_external] + previousValue;
//                            probabilityOfNotBeingInfectedDueToPreviousContact.get(involvedSubject)[eventTime + offset_external] *= (1.0 - riskFactor_external * solution.getSolution()[offset_external][0][0]);
                                probabilityOfNotBeingInfectedDueToPreviousContact.get(involvedSubject)[eventTime + offset_external] *= (1.0 - r_ext);
                                tracks_record.get(current_iteration).editTrack(involvedSubject, eventTime + offset_external, newValue);
                                offset_external++;
                            }
                        } else if (current_iteration > 0 && event.type.equals("Internal")) {
                            // q(s,l,n-1) is the subject with the highest risk of being infectious @ eventTime according to the previous iterations
                            String highestRiskSubject = null;
                            double highestRisk = 0.0;
                            String secondHighestRiskSubject = null;
                            double secondHighestRisk = 0.0;
                            for (String subject : involvedSubjects) { // get the highest risk subject
                                if (tracks_record.get(current_iteration - 1).getSample(subject, eventTime) > highestRisk) {
                                    highestRisk = tracks_record.get(current_iteration - 1).getSample(subject, eventTime);
                                    highestRiskSubject = subject;
                                }
                            }
                            for (String subject : involvedSubjects) { // get the second highest risk subject. This is used to update ~P for the highest risk subject
                                if (tracks_record.get(current_iteration - 1).getSample(subject, eventTime) > secondHighestRisk && tracks_record.get(current_iteration - 1).getSample(subject, eventTime) < highestRisk) {
                                    secondHighestRisk = tracks_record.get(current_iteration - 1).getSample(subject, eventTime);
                                    secondHighestRiskSubject = subject;
                                }
                            }
                            double riskFactor_internal = event.riskFactor;
                            int offset_internal = 0;
                            while (eventTime + offset_internal < time_horizon){
                                for (String subject : involvedSubjects) {
                                    Double previousValue = tracks_record.get(current_iteration).getSample(subject, eventTime + offset_internal);
                                    double q = highestRisk;
                                    if (subject.equals(highestRiskSubject)) {
                                        q = secondHighestRisk;
//                                    System.err.println("Event @ " + eventTime + " [" + offset_internal + "] Subject : " + subject + " is the highest risk subject");
                                    }

                                    Double Pw_h_given_e_s_k_is_effective = 1.0;
                                    Double prior = 1.0;
                                    for (int index = 0; index < symptomsAndTests.size(); index++) {
                                        Event entry = symptomsAndTests.get(index);
                                        double desiredKey = (double)Math.round(((double) entry.time - (double) eventTime) / time_step);
//                                        System.out.println("Desired key: " + desiredKey);
                                        if (desiredKey < 0.0 || desiredKey >= time_horizon){
                                            continue;
                                        }
                                        if (entry.type.equals("Symptoms") && entry.result.equals(true) && entry.time >= eventTime && entry.involvedSubjects[0].equals(subject)) { // we assume that the symptoms are only for the subject involved in the external contact
                                            try {
                                                Pw_h_given_e_s_k_is_effective *= phi.get(desiredKey);
                                                prior *= priorsValues[0];
                                            } catch (Exception e) {
                                                System.err.println("Event @ " + eventTime + " [" + offset_internal + "] Subject : " + subject + " is the highest risk subject");
                                                System.err.println("Entry: " + entry);
                                                System.err.println("Phi: " + phi);
                                                System.err.println("Event time: " + eventTime);
                                                System.err.println("Entry time: " + entry.time);
                                                throw new Exception("Error in phi calculation", e);
                                            }
                                        } else if (entry.type.equals("Test") && entry.time >= eventTime && entry.involvedSubjects[0].equals(subject)) {
                                            if (entry.result) {
                                                Pw_h_given_e_s_k_is_effective *= theta.get(desiredKey);
                                                prior *= priorsValues[1];
                                            } else {
                                                Pw_h_given_e_s_k_is_effective *= (1.0 - theta.get(desiredKey));
                                                prior *= priorsValues[2];
                                            }
                                        }
                                    }
                                    Double r_int = Pw_h_given_e_s_k_is_effective * riskFactor_internal / prior;

                                    Double newValue = r_int * q * solution.getSolution()[offset_internal][0][0] * probabilityOfNotBeingInfectedDueToPreviousContact.get(subject)[eventTime + offset_internal] + previousValue;
                                    probabilityOfNotBeingInfectedDueToPreviousContact.get(subject)[eventTime + offset_internal] *= (1.0 - q);
//                                probabilityOfNotBeingInfectedDueToPreviousContact.get(subject)[eventTime + offset_internal] *= (1.0 - r_int);
                                    tracks_record.get(current_iteration).editTrack(subject, eventTime + offset_internal, newValue);

                                }
                                offset_internal++;
                            }
                        }
                    }
                }


                Tracks tracks = new Tracks(names, time_horizon);
                for (int i = 0; i < n_subjects; i++) {
                    for (int j = 0; j < time_horizon; j++) {
                        double sum = 0.0;
                        for (int k = 0; k < n_iterations; k++) {
                            sum += tracks_record.get(k).getSample(String.valueOf(i + 1), j);
                        }
                        tracks.editTrack(String.valueOf(i + 1), j, sum);
                    }
                }

                try (FileWriter file = new FileWriter(documentName + "_" + priorsValues[0] + "," + priorsValues[1] + "," + priorsValues[2] + "_tracks.json")) {
                    file.write(tracks.toJson().toString());
                    file.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        long end = System.currentTimeMillis();
        System.out.println("Execution time: " + (end - start) + " ms");
    }
}