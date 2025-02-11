
package com.sweproject.analysis2;

import com.google.gson.JsonObject;
import com.sweproject.model.Subject;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedGEN;
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
import java.math.BigDecimal;
import java.util.*;

public class STPNAnalysis {
    // 1. Build model
    // 2. Set Marking
    // 3. Run analysis
    // 4. Get Solution

    public static <R, S> TransientSolution<R, S> buildModel(int samples, int step){
        PetriNet net = new PetriNet();
        Marking marking = new Marking();
        double riskFactor = 0.9; // chance of developing symptoms FIXME: choose the right value

        // Add nodes
        Place EffectiveContact = net.addPlace("EffectiveContact");
        // Upper branch
        Place Infected = net.addPlace("Infected");
        Place Infectious = net.addPlace("Infectious");
        Place Healed = net.addPlace("Healed");
        Place Isolated = net.addPlace("Isolated");
        //Lower branch
        Place Symptomatology = net.addPlace("Symptomatology");
        Place DevelopingSymptoms = net.addPlace("DevelopingSymptoms");
        Place Asymptomatic = net.addPlace("Asymptomatic");
        Place Symptomatic = net.addPlace("Symptomatic");

        // Add transitions
        Transition effectiveContact = net.addTransition("effectiveContact");
        // Upper branch
        Transition incubation = net.addTransition("incubation");
        Transition healing = net.addTransition("healing");
        Transition isolating = net.addTransition("isolating");
        // Lower branch
        Transition noSymptoms = net.addTransition("noSymptoms");
        Transition symptoms = net.addTransition("symptoms");
        Transition symptomsOnset = net.addTransition("symptomsOnset");

        // Wire transitions
        net.addPrecondition(EffectiveContact, effectiveContact);
        net.addPostcondition(effectiveContact, Infected);
        net.addPostcondition(effectiveContact, Symptomatology);
        // Upper branch
        net.addPrecondition(Infected, incubation);
        net.addPostcondition(incubation, Infectious);
        net.addPrecondition(Infectious, healing);
        net.addPostcondition(healing, Healed);
        net.addPrecondition(Infectious, isolating);
        net.addPostcondition(isolating, Isolated);
        // Lower branch
        net.addPrecondition(Symptomatology, noSymptoms);
        net.addPostcondition(noSymptoms, Asymptomatic);
        net.addPrecondition(Asymptomatic, healing);
        net.addPrecondition(Symptomatology, symptoms);
        net.addPostcondition(symptoms, DevelopingSymptoms);
        net.addPrecondition(DevelopingSymptoms, symptomsOnset);
        net.addPostcondition(symptomsOnset, Symptomatic);
        net.addPrecondition(Symptomatic, isolating);

        // Add features
        effectiveContact.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0")));
        // Upper branch
        //incubation
        List<GEN> incubation_gens = new ArrayList<>();
        DBMZone incubation_d_0 = new DBMZone(new Variable("x"));
        Expolynomial incubation_e_0 = Expolynomial.fromString("400 *x^2 * Exp[-0.2 x] + 2* x^1 * Exp[-1 x]"); // FIXME change this
        incubation_e_0.multiply(new BigDecimal(0.0000099998));
        incubation_d_0.setCoefficient(new Variable("x"), new Variable("t*"), OmegaBigDecimal.POSITIVE_INFINITY);
        incubation_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("0"));
        GEN incubation_gen_0 = new GEN(incubation_d_0, incubation_e_0);
        incubation_gens.add(incubation_gen_0);
        PartitionedGEN incubation_pFunction = new PartitionedGEN(incubation_gens);
        incubation.addFeature(StochasticTransitionFeature.of(incubation_pFunction));

        // healing
        List<GEN> healing_gens = new ArrayList<>();
        DBMZone healing_d_0 = new DBMZone(new Variable("x"));
        Expolynomial healing_e_0 = Expolynomial.fromString("400 *x^2 * Exp[-0.2 x] + 2* x^1 * Exp[-1 x]"); // FIXME change this
        healing_e_0.multiply(new BigDecimal(0.0000099998));
        healing_d_0.setCoefficient(new Variable("x"), new Variable("t*"), OmegaBigDecimal.POSITIVE_INFINITY);
        healing_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("0"));
        GEN healing_gen_0 = new GEN(healing_d_0, healing_e_0);
        healing_gens.add(healing_gen_0);
        PartitionedGEN healing_pFunction = new PartitionedGEN(healing_gens);
        healing.addFeature(StochasticTransitionFeature.of(healing_pFunction));

        // isolating
        List<GEN> isolating_gens = new ArrayList<>();
        DBMZone isolating_d_0 = new DBMZone(new Variable("x"));
        Expolynomial isolating_e_0 = Expolynomial.fromString("400 *x^2 * Exp[-0.2 x] + 2* x^1 * Exp[-1 x]"); // FIXME change this
        isolating_e_0.multiply(new BigDecimal(0.0000099998));
        isolating_d_0.setCoefficient(new Variable("x"), new Variable("t*"), OmegaBigDecimal.POSITIVE_INFINITY);
        isolating_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("0"));
        GEN isolating_gen_0 = new GEN(isolating_d_0, isolating_e_0);
        isolating_gens.add(isolating_gen_0);
        PartitionedGEN isolating_pFunction = new PartitionedGEN(isolating_gens);
        isolating.addFeature(StochasticTransitionFeature.of(isolating_pFunction));

        // Lower branch
        noSymptoms.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1 - riskFactor), net)));
        symptoms.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(riskFactor), net)));

        // symptomsOnset
        // symptomsOnset
        List<GEN> symptomsOnset_gens = new ArrayList<>();
        DBMZone symptomsOnset_d_0 = new DBMZone(new Variable("x"));
        Expolynomial symptomsOnset_e_0 = Expolynomial.fromString("400 *x^2 * Exp[-0.2 x] + 2* x^1 * Exp[-1 x]"); // FIXME change this
        symptomsOnset_e_0.multiply(new BigDecimal(0.0000099998));
        symptomsOnset_d_0.setCoefficient(new Variable("x"), new Variable("t*"), OmegaBigDecimal.POSITIVE_INFINITY);
        symptomsOnset_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("0"));
        GEN symptomsOnset_gen_0 = new GEN(symptomsOnset_d_0, symptomsOnset_e_0);
        symptomsOnset_gens.add(symptomsOnset_gen_0);
        PartitionedGEN symptomsOnset_pFunction = new PartitionedGEN(symptomsOnset_gens);
        symptomsOnset.addFeature(StochasticTransitionFeature.of(symptomsOnset_pFunction));

        // Set marking
        marking.setTokens(EffectiveContact, 1);

        // Run analysis
        TreeTransient analysis = TreeTransient.builder()
                .greedyPolicy(new BigDecimal(samples), BigDecimal.ZERO)
                .timeStep(new BigDecimal(step))
                .build();

        TransientSolution<Marking, Marking> result = analysis.compute(net, marking);

        var rewardRates = TransientSolution.rewardRates("Infectious");
        var rewardedSolution = TransientSolution.computeRewards(false, result, rewardRates);

        System.out.println(rewardedSolution);

        return (TransientSolution<R, S>) rewardedSolution;




    }
    /*public TransientSolution<R,S> makeModel(String fiscalCode, ArrayList<HashMap<String, Object>> arrayList) throws Exception {
        LocalDateTime now = LocalDateTime.now().minusDays(6);
        if(arrayList.size() > 0) {
            PetriNet net = new PetriNet();
            Marking marking = new Marking();
            //Generating Nodes
            Place Contagio = net.addPlace("Contagio");
            buildContagionEvolutionSection(net, marking, Contagio);
            Place p1 = net.addPlace("Condizione iniziale");
            Place p2 = net.addPlace("Primo incontro");
            marking.setTokens(p1, 1);
            Transition t0 = net.addTransition("t0");
            Transition e0 = net.addTransition("effective0");
            Transition u0 = net.addTransition("uneffective0");
            float delta = (float) ChronoUnit.MINUTES.between(now, (LocalDateTime) arrayList.get(0).get("start_date")) / 60.f;
            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(delta)));
            e0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf((float) arrayList.get(0).get("risk_level")), net)));
            u0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1 - (float) arrayList.get(0).get("risk_level")), net)));
            net.addPrecondition(p1, t0);
            net.addPostcondition(t0, p2);
            net.addPrecondition(p2, e0);
            net.addPostcondition(e0, Contagio);
            net.addPrecondition(p2, u0);

            Transition lastTransition = u0;
            for (int i = 1; i < arrayList.size(); i++) {
                Place p3 = net.addPlace("Dopo incontro " + i);
                Place p4 = net.addPlace("Incontro " + (i + 1));
                Transition t1 = net.addTransition("t " + i), e1 = net.addTransition("effective " + i), u1 = net.addTransition("uneffective " + i);
                delta = (float) ChronoUnit.MINUTES.between((LocalDateTime) arrayList.get(i - 1).get("start_date"), (LocalDateTime) arrayList.get(i).get("start_date")) / 60.f;
                t1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(delta)));
                e1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf((float) arrayList.get(i).get("risk_level")), net)));
                u1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1 - (float) arrayList.get(i).get("risk_level")), net)));
                net.addPostcondition(lastTransition, p3);
                net.addPrecondition(p3, t1);
                net.addPostcondition(t1, p4);
                net.addPrecondition(p4, e1);
                net.addPostcondition(e1, Contagio);
                net.addPrecondition(p4, u1);
                lastTransition = u1;
            }
            // 144 -> 6 giorni
            RegTransient analysis = RegTransient.builder()
                    .greedyPolicy(new BigDecimal(samples), new BigDecimal("0.0000001"))
                    .timeStep(new BigDecimal(step)).build();

            //If(Contagioso>0&&Sintomatico==0,1,0);Contagioso;Sintomatico;If(Guarito+Isolato>0,1,0)
            var rewardRates = TransientSolution.rewardRates("Contagioso");

            TransientSolution<DeterministicEnablingState, Marking> solution =
                    analysis.compute(net, marking);

            var rewardedSolution = TransientSolution.computeRewards(false, solution, rewardRates);
            return (TransientSolution<R, S>) rewardedSolution;
        }else{
            System.out.println("The subject has no 'Environment' observations of the last 6 days");
            return makeFakeNet();
        }
    }*/

    public static void main(String[] args) throws Exception {
        // read from json
        String filePath = "C:\\Users\\super\\Documents\\Uni\\Chita\\Simulator\\dataset_s5_t14_5_simulated.json";
        JsonObject jsonObject = JsonFileReader.readJsonFromFile(filePath);
        int n_subjects = 0;
        int time_limit = 0;
        Queue<Event> events = new LinkedList<>();
        if (jsonObject != null) {
            n_subjects = jsonObject.get("n_subjects").getAsInt();
            time_limit = jsonObject.get("time_limit").getAsInt();
            System.out.println(jsonObject);
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
        TransientSolution<Integer, Double> solution = buildModel(time_limit * 24, 1);

        // Create the names based on n_subjects
        String[] names = new String[n_subjects];
        for (int i = 0; i < n_subjects; i++) {
            names[i] = String.valueOf(i+1);
        }

        // cycle on events and print them all
        for (Event event : events) {
            if ("Internal".equals(event.type)) {
                System.out.println("\u001B[33m" + event.type + " " + Arrays.toString(event.involvedSubjects) + " " + event.time + " " + event.riskFactor + " " + event.result + "\u001B[0m");
            } else if ("External".equals(event.type)) {
                System.out.println("\u001B[38;5;214m" + event.type + " " + Arrays.toString(event.involvedSubjects) + " " + event.time + " " + event.riskFactor + " " + event.result + "\u001B[0m");
            } else {
                System.out.println(event.type + " " + Arrays.toString(event.involvedSubjects) + " " + event.time + " " + event.riskFactor + " " + event.result);
            }
        }

        System.out.println(Arrays.deepToString(solution.getSolution()));

        int n_iterations = 10;
        if (n_iterations == n_subjects || n_iterations <= 0) throw new AssertionError();
        HashMap<Integer, Tracks> tracks_record = new HashMap<>();
        for (int current_iteration = 0; current_iteration < n_iterations; current_iteration++){
            HashMap<String, double[]> probabilityOfNotBeingInfectedDueToPreviousContact = new HashMap<>();
            for (int i = 0; i < n_subjects; i++){
                probabilityOfNotBeingInfectedDueToPreviousContact.put(String.valueOf(i+1), new double[time_limit * 24]); // \prod_{j=0}^{k-1} 1 - r_{s,j}^{ext}(t)
                Arrays.fill(probabilityOfNotBeingInfectedDueToPreviousContact.get(String.valueOf(i+1)), 1.0);
            }
            tracks_record.put(current_iteration, new Tracks(names, time_limit * 24));
            for (Event event : events){
                int eventTime = event.time;
                String[] involvedSubjects = event.involvedSubjects;
                if (current_iteration == 0 && event.type.equals("External")) {
                   System.out.println("\u001B[36mExternal event @ " + eventTime + "\u001B[0m");
                    assert involvedSubjects.length == 1;
                    String involvedSubject = involvedSubjects[0];
                    int offset_external = 0;
                    double riskFactor_external = event.riskFactor;
                    while (eventTime + offset_external < time_limit * 24) {
                        Double previousValue = tracks_record.get(current_iteration).getSample(involvedSubject, eventTime + offset_external);
                        Double newValue = riskFactor_external * solution.getSolution()[offset_external][0][0] * probabilityOfNotBeingInfectedDueToPreviousContact.get(involvedSubject)[eventTime + offset_external] + previousValue;
                        probabilityOfNotBeingInfectedDueToPreviousContact.get(involvedSubject)[eventTime + offset_external] *= (1.0 - riskFactor_external * solution.getSolution()[offset_external][0][0]);
                        tracks_record.get(current_iteration).editTrack(involvedSubject, eventTime + offset_external, newValue);
                        offset_external++;
                    }
                }
                else if (current_iteration > 0 && event.type.equals("Internal")){
                    // q(s,l,n-1) is the subject with the highest risk of being infectious @ eventTime according to the previous iterations
                    String highestRiskSubject = null;
                    double highestRisk = 0.0;
                    String secondHighestRiskSubject = null;
                    double secondHighestRisk = 0.0;
                    for (String subject : involvedSubjects){ // get the highest risk subject
                        if (tracks_record.get(current_iteration-1).getSample(subject, eventTime) > highestRisk) {
                            highestRisk = tracks_record.get(current_iteration-1).getSample(subject, eventTime);
                            highestRiskSubject = subject;
                        }
                    }
                    for (String subject : involvedSubjects){ // get the second highest risk subject. This is used to update ~P for the highest risk subject
                        if (tracks_record.get(current_iteration-1).getSample(subject, eventTime) > secondHighestRisk && tracks_record.get(current_iteration-1).getSample(subject, eventTime) < highestRisk){
                            secondHighestRisk = tracks_record.get(current_iteration-1).getSample(subject, eventTime);
                            secondHighestRiskSubject = subject;
                        }
                    }
                    double riskFactor_internal = event.riskFactor;
                    int offset_internal = 0;
                    while(eventTime + offset_internal < time_limit * 24){
                        for (String subject : involvedSubjects){
                            Double previousValue = tracks_record.get(current_iteration).getSample(subject, eventTime + offset_internal);
                            double q = highestRisk;
                            if (subject.equals(highestRiskSubject)){
                                q = secondHighestRisk;
                                System.err.println("Event @ " + eventTime + " [" +offset_internal+"] Subject : " + subject + " is the highest risk subject");
                            }
                            Double newValue = riskFactor_internal * q * solution.getSolution()[offset_internal][0][0] * probabilityOfNotBeingInfectedDueToPreviousContact.get(subject)[eventTime + offset_internal] + previousValue;
                            probabilityOfNotBeingInfectedDueToPreviousContact.get(subject)[eventTime + offset_internal] *= (1.0 - q);
                            tracks_record.get(current_iteration).editTrack(subject, eventTime + offset_internal, newValue);

                        }
                        offset_internal++;
                    }
                }
            }
        }



        Tracks tracks = new Tracks(names, time_limit * 24);
        for (int i = 0; i < n_subjects; i++){
            for (int j = 0; j < time_limit * 24; j++){
                double sum = 0.0;
                for (int k = 0; k < n_iterations; k++){
                    sum += tracks_record.get(k).getSample(String.valueOf(i+1), j);
                }
                tracks.editTrack(String.valueOf(i+1), j, sum);
            }
        }
        // make a graph of the tracks
        SwingUtilities.invokeLater(() -> {
            TrackPlotter example = new TrackPlotter("Track Values", tracks);
            example.setSize(800, 400);
            example.setLocationRelativeTo(null);
            example.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            example.setVisible(true);
        });


    }
}