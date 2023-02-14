package com.sweproject.analysis;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.sweproject.model.CovidTest;
import com.sweproject.model.CovidTestType;
import com.sweproject.model.Symptoms;
import javafx.scene.chart.XYChart;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class STPNAnalyzer_ext<R,S> extends STPNAnalyzer{
    public STPNAnalyzer_ext(int samples, int step) {
        super(samples, step);
    }

    /*public TransientSolution<R,S> makeModel(String fiscalCode, ArrayList<HashMap<String, Object>> environmentArrayList, ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList) throws Exception {
        for(int testIndex = 0; testIndex < testArrayList.size(); testIndex++){
            String rawType = (String) testArrayList.get(testIndex).get("type");
            String[] extractedType = rawType.split("-"); // 0 -> Covid_test, 1 -> type of test, 2 -> outcome
            testArrayList.get(testIndex).put("testType", extractedType[1].equals("MOLECULAR")? CovidTestType.MOLECULAR:CovidTestType.ANTIGEN);
            testArrayList.get(testIndex).put("isPositive", extractedType[2].equals("true"));
        }
        LocalDateTime endInterval = LocalDateTime.now();
        LocalDateTime now = endInterval.minusDays(6);
        boolean showsSymptoms = false;
        if (environmentArrayList.size() > 0){
            for (int contact = 0; contact < environmentArrayList.size(); contact++){
                showsSymptoms = false;
                LocalDateTime contact_time = (LocalDateTime) environmentArrayList.get(contact).get("start_date");
                float risk_level = (float) environmentArrayList.get(contact).get("risk_level");
                double cumulativeRiskLevel = risk_level;
                if (symptomsArrayList.size() > 0){
                    for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++){
                        LocalDateTime symptom_date = (LocalDateTime) symptomsArrayList.get(symptom).get("start_date");
                        if (contact_time.isBefore(symptom_date)){
                            Symptoms symptoms = new Symptoms();
                            cumulativeRiskLevel += symptoms.updateEvidence(contact_time, symptom_date);
                            showsSymptoms = true;
                        }
                    }
                }
                if (testArrayList.size() > 0){
                    for (int test = 0; test < testArrayList.size(); test++){
                        LocalDateTime test_time = (LocalDateTime) testArrayList.get(test).get("start_date");
                        if (contact_time.isBefore(test_time)){
                            CovidTest covidTest = new CovidTest((CovidTestType) testArrayList.get(test).get("testType"), (boolean) testArrayList.get(test).get("isPositive"));
                            System.out.println("Covid CCC" + covidTest.getName());
                            double testEvidence = covidTest.isInfected(contact_time, test_time);
                            System.out.println(testEvidence);
                            cumulativeRiskLevel+=testEvidence;
                        }
                    }
                }
                cumulativeRiskLevel /= (symptomsArrayList.size() + testArrayList.size() + 1);
                System.out.println(cumulativeRiskLevel + " prima");
                cumulativeRiskLevel = updateRiskLevel(cumulativeRiskLevel, contact_time, showsSymptoms);
                System.out.println(cumulativeRiskLevel + " dopo");
                environmentArrayList.get(contact).replace("risk_level", risk_level, (float) cumulativeRiskLevel);
            }
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
            float delta = (float) ChronoUnit.MINUTES.between(now, (LocalDateTime) environmentArrayList.get(0).get("start_date")) / 60.f;
            float newRiskLevel = addTimeRelevance(delta, (float) environmentArrayList.get(0).get("risk_level"));
            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(delta)));
            e0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(newRiskLevel), net)));
            u0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1 - newRiskLevel), net)));
            net.addPrecondition(p1, t0);
            net.addPostcondition(t0, p2);
            net.addPrecondition(p2, e0);
            net.addPostcondition(e0, Contagio);
            net.addPrecondition(p2, u0);

            Transition lastTransition = u0;
            for (int i = 1; i < environmentArrayList.size(); i++) {
                Place p3 = net.addPlace("Dopo incontro " + i);
                Place p4 = net.addPlace("Incontro " + (i + 1));
                Transition t1 = net.addTransition("t " + i), e1 = net.addTransition("effective " + i), u1 = net.addTransition("uneffective " + i);
                delta = (float) ChronoUnit.MINUTES.between((LocalDateTime) environmentArrayList.get(i - 1).get("start_date"), (LocalDateTime) environmentArrayList.get(i).get("start_date")) / 60.f;
                newRiskLevel = addTimeRelevance(delta, (float) environmentArrayList.get(i).get("risk_level"));
                t1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(delta)));
                e1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(newRiskLevel), net)));
                u1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1 - newRiskLevel), net)));
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
                    .greedyPolicy(new BigDecimal(samples), new BigDecimal("0.001"))
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
    }
   */
    public TransientSolution<R,S> makeModel(ArrayList<HashMap<String, Object>> environmentArrayList, ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList) throws Exception {
        for(int testIndex = 0; testIndex < testArrayList.size(); testIndex++){
            String rawType = (String) testArrayList.get(testIndex).get("type");
            String[] extractedType = rawType.split("-"); // 0 -> Covid_test, 1 -> type of test, 2 -> outcome
            testArrayList.get(testIndex).put("testType", extractedType[1].equals("MOLECULAR")? CovidTestType.MOLECULAR:CovidTestType.ANTIGEN);
            testArrayList.get(testIndex).put("isPositive", extractedType[2].equals("true"));
        }
        LocalDateTime endInterval = LocalDateTime.now();
        LocalDateTime now = endInterval.minusDays(6);
        boolean showsSymptoms = false;
        if (environmentArrayList.size() > 0){
            for (int contact = 0; contact < environmentArrayList.size(); contact++){
                showsSymptoms = false;
                LocalDateTime contact_time = (LocalDateTime) environmentArrayList.get(contact).get("start_date");
                float risk_level = (float) environmentArrayList.get(contact).get("risk_level");
                double cumulativeRiskLevel = risk_level;
                if (symptomsArrayList.size() > 0){
                    for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++){
                        LocalDateTime symptom_date = (LocalDateTime) symptomsArrayList.get(symptom).get("start_date");
                        if (contact_time.isBefore(symptom_date)){
                            Symptoms symptoms = new Symptoms();
                            double sympEvidence = symptoms.updateEvidence(contact_time, symptom_date);
                            cumulativeRiskLevel += sympEvidence;
                            System.out.println("Symp " + sympEvidence);
                            showsSymptoms = true;
                        }
                    }
                }
                if (testArrayList.size() > 0){
                    for (int test = 0; test < testArrayList.size(); test++){
                        LocalDateTime test_time = (LocalDateTime) testArrayList.get(test).get("start_date");
                        if (contact_time.isBefore(test_time)){
                            CovidTest covidTest = new CovidTest((CovidTestType) testArrayList.get(test).get("testType"), (boolean) testArrayList.get(test).get("isPositive"));
                            System.out.println("Covid CCC" + covidTest.getName());
                            double testEvidence = covidTest.isInfected(contact_time, test_time);
                            System.out.println(testEvidence);
                            cumulativeRiskLevel+=testEvidence;
                        }
                    }
                }
                double cumulativeRiskLevel3 = (cumulativeRiskLevel) / (symptomsArrayList.size() + testArrayList.size() + 1);
                double [] cumulativeRiskLevel2;
                cumulativeRiskLevel2 = updateRiskLevel(contact_time);
                double cumulativeRiskLevel1 = cumulativeRiskLevel2[0];
                cumulativeRiskLevel = cumulativeRiskLevel1 * cumulativeRiskLevel3;
                System.out.println(cumulativeRiskLevel + " prima");
                if (showsSymptoms){
                    cumulativeRiskLevel /= (cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1]);
                }
                else{
                    cumulativeRiskLevel /= (1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1]);
                    //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
                }
                System.out.println(cumulativeRiskLevel + " dopo");
                environmentArrayList.get(contact).replace("risk_level", risk_level, (float) cumulativeRiskLevel);
            }
            PetriNet net = new PetriNet();
            Marking marking = new Marking();
            //Generating Nodes
            Place Contagio = net.addPlace("Contagio");
            buildContagionEvolutionSection(net, marking, Contagio);
            marking.setTokens(Contagio, 1);


            // 144 -> 6 giorni
            RegTransient analysis = RegTransient.builder()
                    .greedyPolicy(new BigDecimal(samples), new BigDecimal("0.001"))
                    .timeStep(new BigDecimal(step)).build();

            //If(Contagioso>0&&Sintomatico==0,1,0);Contagioso;Sintomatico;If(Guarito+Isolato>0,1,0)
            var rewardRates = TransientSolution.rewardRates("Contagioso");

            TransientSolution<DeterministicEnablingState, Marking> solution =
                    analysis.compute(net, marking);

            var rewardedSolution = TransientSolution.computeRewards(false, solution, rewardRates);
            return (TransientSolution<R, S>) rewardedSolution;
        }else{
            System.out.println("The subject has no 'Environment' observations of the last 6 days");
            return super.makeFakeNet();
        }
    }

    public HashMap<Integer, Double> computeAnalysis(TransientSolution<S, R> s, ArrayList<HashMap<String, Object>> eventsArrayList, LocalDateTime pastStartTime){
        HashMap<Integer, Double> output = new HashMap<>();
        int size = s.getSamplesNumber();
        for (int i = 0; i < size; i++){
            output.put(i, 0.0);
        }
        int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
        for(int m = 0; m < s.getColumnStates().size(); m++){
            double step = s.getStep().doubleValue();
            for (int event = 0; event < eventsArrayList.size(); event++){
                LocalDateTime eventTime = (LocalDateTime) eventsArrayList.get(event).get("start_date");
                int delta = (int) ChronoUnit.HOURS.between(pastStartTime, eventTime);
                int i = 0;
                for (int j = delta; j < size; j += step){
                    double y = s.getSolution()[i][r][m] * (float)eventsArrayList.get(event).get("risk_level");
                    double oldY = output.get(j);
                    output.replace(j,  y + oldY); //TODO anziché y + oldY non è meglio (1-oldY) * y + oldY?
                    i++;
                }
            }
        }
        return output;
    }

    public <S, R> XYChart.Series adaptForApp(HashMap<Integer, Double> s){
        XYChart.Series<String, Float> series = new XYChart.Series();
        for(int sample : s.keySet()) {
            series.getData().add(new XYChart.Data<>(String.valueOf(sample), s.get(sample).floatValue()));
        }
        return series;
    }
    /*public HashMap<Integer, Double> adaptForSim(TransientSolution<S, R> s, ArrayList<HashMap<String, Object>> eventsArrayList, LocalDateTime pastStartTime){
        HashMap<Integer, Double> output = new HashMap<>();
        int size = s.getSamplesNumber();
        for (int i = 0; i < size; i++){
            output.put(i, 0.0);
        }
        int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
        for(int m = 0; m < s.getColumnStates().size(); m++){
            double step = s.getStep().doubleValue();
            for (int event = 0; event < eventsArrayList.size(); event++){
                LocalDateTime eventTime = (LocalDateTime) eventsArrayList.get(event).get("start_date");
                int delta = (int) ChronoUnit.HOURS.between(pastStartTime, eventTime);
                int i = 0;
                for (int j = delta; j < size; j += step){
                    double y = s.getSolution()[i][r][m] * (float)eventsArrayList.get(event).get("risk_level");
                    double oldY = output.get(j);
                    output.replace(j,  y + oldY); //TODO anziché y + oldY non è meglio (1-oldY) * y + oldY?
                    i++;
                }
            }
        }
        return output;
    }*/

    /*public TransientSolution<R, S> makeClusterModel(HashMap<String, TransientSolution> subjects_ss, ArrayList<HashMap<String, Object>> clusterSubjectsMet,
                                                    ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList) throws Exception {

       for(int testIndex = 0; testIndex < testArrayList.size(); testIndex++){
            String rawType = (String) testArrayList.get(testIndex).get("type");
            String[] extractedType = rawType.split("-"); // 0 -> Covid_test, 1 -> type of test, 2 -> outcome
            testArrayList.get(testIndex).put("testType", extractedType[1].equals("MOLECULAR")? CovidTestType.MOLECULAR:CovidTestType.ANTIGEN);
            testArrayList.get(testIndex).put("isPositive", extractedType[2].equals("true"));
        }

        LocalDateTime endInterval = LocalDateTime.now();
       LocalDateTime initialTime = endInterval.minusDays(6);
        boolean showsSymptoms = false;

        for (int contact = 0; contact < clusterSubjectsMet.size(); contact++){
                showsSymptoms = false;
                LocalDateTime contact_time = (LocalDateTime) clusterSubjectsMet.get(contact).get("start_date");
                float risk_level = (float) clusterSubjectsMet.get(contact).get("risk_level");
                double cumulativeRiskLevel = risk_level;
                if (symptomsArrayList.size() > 0){
                    for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++){
                        LocalDateTime symptom_date = (LocalDateTime) symptomsArrayList.get(symptom).get("start_date");
                        if (contact_time.isBefore(symptom_date)){
                            Symptoms symptoms = new Symptoms();
                            cumulativeRiskLevel += symptoms.updateEvidence(contact_time, symptom_date);
                            showsSymptoms = true;
                        }
                    }
                }
                if (testArrayList.size() > 0){
                    for (int test = 0; test < testArrayList.size(); test++){
                        LocalDateTime test_time = (LocalDateTime) testArrayList.get(test).get("start_date");
                        if (contact_time.isBefore(test_time)){
                            CovidTest covidTest = new CovidTest((CovidTestType) testArrayList.get(test).get("testType"), (boolean) testArrayList.get(test).get("isPositive"));
                            System.out.println("Covid CCC" + covidTest.getName());
                            double testEvidence = covidTest.isInfected(contact_time, test_time);
                            System.out.println(testEvidence);
                            cumulativeRiskLevel+=testEvidence;
                        }
                    }
                }
                cumulativeRiskLevel /= (symptomsArrayList.size() + testArrayList.size() + 1);
                System.out.println(cumulativeRiskLevel + " prima");
                cumulativeRiskLevel = updateRiskLevel(cumulativeRiskLevel, contact_time, showsSymptoms);
                System.out.println(cumulativeRiskLevel + " dopo");
                clusterSubjectsMet.get(contact).replace("risk_level", risk_level, (float) cumulativeRiskLevel);
            }

        if (clusterSubjectsMet.size() > 0) {
            PetriNet net = new PetriNet();
            //creating the central node
            Marking marking = new Marking();
            Place Contagio = net.addPlace("Contagio");
            buildContagionEvolutionSection(net, marking, Contagio);
            Place p1 = net.addPlace("Condizione iniziale");
            Place p2 = net.addPlace("Primo incontro");
            marking.setTokens(p1, 1);
            Transition t0 = net.addTransition("t0");
            Transition e0 = net.addTransition("effective0");
            Transition u0 = net.addTransition("uneffective0");

            Transition lastTransition = u0;
            //dato un tempo "meeting_time1" si segna quante persone, oltre al soggetto analizzato, hanno partecipato
            int i = 0; //index of contact
            int j = 0; //n. person met during contact counter
            String[] meeting_subjects = new String[clusterSubjectsMet.size()];
            LocalDateTime meeting_time1 = LocalDateTime.from((LocalDateTime)clusterSubjectsMet.get(i).get("start_date"));
            while (i < clusterSubjectsMet.size() && ((LocalDateTime) clusterSubjectsMet.get(i).get("start_date")).equals(meeting_time1)) {
                meeting_subjects[j] = clusterSubjectsMet.get(i).get("fiscalCode").toString();
                j++;
                i++;
            }
            ArrayList<TransientSolution> subjectsMet_ss = new ArrayList<>();
            for (int k = 0; k < j; k++) {
                subjectsMet_ss.add(subjects_ss.get(meeting_subjects[k]));
            }
            float effectiveness = getChancesOfHavingContagiousPersonInCluster(subjectsMet_ss, meeting_time1, step, (float) clusterSubjectsMet.get(j-1).get("risk_level"));
            float delta = (float) ChronoUnit.MINUTES.between(initialTime, meeting_time1) / 60.f;
            float elapsedTime = (float) ChronoUnit.MINUTES.between(endInterval, meeting_time1) / 60.f;
            effectiveness = addTimeRelevance(elapsedTime, effectiveness);

            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(delta)));
            e0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(effectiveness), net)));
            u0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-effectiveness), net)));
            net.addPrecondition(p1,t0);
            net.addPostcondition(t0,p2);
            net.addPrecondition(p2, e0);
            net.addPostcondition(e0, Contagio);
            net.addPrecondition(p2, u0);
            //making intermediate modules
            int p = 0; //index for transitions
            for (int l = i; l < clusterSubjectsMet.size(); l++) {
                j = 0;
                for (int n = 0; n < meeting_subjects.length; n++) {
                    meeting_subjects[n] = null;
                }
                LocalDateTime meeting_time2 = LocalDateTime.from((LocalDateTime) clusterSubjectsMet.get(l).get("start_date"));
                while (l < clusterSubjectsMet.size() && ((LocalDateTime) clusterSubjectsMet.get(l).get("start_date")).equals(meeting_time2)) {
                    meeting_subjects[j] = clusterSubjectsMet.get(i).get("fiscalCode").toString();
                    j++;
                    l++;
                }
                subjectsMet_ss.clear();
                for (int k = 0; k < j; k++) {
                    subjectsMet_ss.add(subjects_ss.get(meeting_subjects[k]));
                }
                p++;

                Place p3 = net.addPlace("Dopo incontro "+p);
                Place p4 = net.addPlace("Incontro "+(p+1));
                Transition t1 = net.addTransition("t "+p), e1 = net.addTransition("effective "+p), u1 = net.addTransition("uneffective "+p);
                delta = (float) ChronoUnit.MINUTES.between(meeting_time1, meeting_time2) / 60.f;
                effectiveness = getChancesOfHavingContagiousPersonInCluster(subjectsMet_ss, meeting_time2, step, (float) clusterSubjectsMet.get(l-1).get("risk_level"));
                elapsedTime = (float) ChronoUnit.MINUTES.between(endInterval, meeting_time2) / 60.f;
                effectiveness = addTimeRelevance(elapsedTime, effectiveness);

                t1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(delta)));
                e1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(effectiveness), net)));
                u1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1 - effectiveness), net)));
                net.addPostcondition(lastTransition, p3);
                net.addPrecondition(p3, t1);
                net.addPostcondition(t1, p4);
                net.addPrecondition(p4, e1);
                net.addPostcondition(e1, Contagio);
                net.addPrecondition(p4, u1);
                lastTransition = u1;
                meeting_time1 = meeting_time2;
            }
            RegTransient analysis = RegTransient.builder()
                    .greedyPolicy(new BigDecimal(samples), new BigDecimal("0.001"))
                    .timeStep(new BigDecimal(step)).build();

            //If(Contagioso>0&&Sintomatico==0,1,0);Contagioso;Sintomatico;If(Guarito+Isolato>0,1,0)
            var rewardRates = TransientSolution.rewardRates("Contagioso");

            TransientSolution<DeterministicEnablingState, Marking> solution =
                    analysis.compute(net, marking);

            var rewardedSolution = TransientSolution.computeRewards(false, solution, rewardRates);
            //new TransientSolutionViewer(rewardedSolution);
            return (TransientSolution<R, S>) rewardedSolution;
        } else {
            System.out.println("The subject has no 'Contact' observations of the last 6 days");
            return makeFakeNet();
        }
    }
*/

    public XYChart.Series makeClusterModelForApp(LocalDateTime pastStartTime, HashMap<String, XYChart.Series<String,Float>> subjects_ss, ArrayList<HashMap<String, Object>> clusterSubjectsMet,
                                                 ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList) throws Exception{
        HashMap<String, HashMap<Integer, Double>> subjects_solutions = new HashMap<>();
        for(String s : subjects_ss.keySet()){
            HashMap<Integer, Double> sol = new HashMap<>();

            for(int i = 0; i < samples; i++){
                sol.put(i, subjects_ss.get(s).getData().get(i).getYValue().doubleValue());
            }
            subjects_solutions.put(s, sol);
        }
        var model = makeClusterModel(pastStartTime, subjects_solutions, clusterSubjectsMet, testArrayList, symptomsArrayList);
        return adaptForApp(model);
    }
    public HashMap<Integer, Double> makeClusterModel(LocalDateTime pastStartTime, HashMap<String, HashMap<Integer, Double>> subjects_solutions, ArrayList<HashMap<String, Object>> clusterSubjectsMet,
                                                     ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList) throws Exception {

        for (int testIndex = 0; testIndex < testArrayList.size(); testIndex++) {
            String rawType = (String) testArrayList.get(testIndex).get("type");
            String[] extractedType = rawType.split("-"); // 0 -> Covid_test, 1 -> type of test, 2 -> outcome
            testArrayList.get(testIndex).put("testType", extractedType[1].equals("MOLECULAR") ? CovidTestType.MOLECULAR : CovidTestType.ANTIGEN);
            testArrayList.get(testIndex).put("isPositive", extractedType[2].equals("true"));
        }

        LocalDateTime endInterval = LocalDateTime.now();
        LocalDateTime initialTime = endInterval.minusDays(6);
        boolean showsSymptoms = false;

        for (int contact = 0; contact < clusterSubjectsMet.size(); contact++) {
            showsSymptoms = false;
            LocalDateTime contact_time = (LocalDateTime) clusterSubjectsMet.get(contact).get("start_date");
            float risk_level = (float) clusterSubjectsMet.get(contact).get("risk_level");
            double cumulativeRiskLevel = risk_level;
            if (symptomsArrayList.size() > 0) {
                for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++) {
                    LocalDateTime symptom_date = (LocalDateTime) symptomsArrayList.get(symptom).get("start_date");
                    if (contact_time.isBefore(symptom_date)) {
                        Symptoms symptoms = new Symptoms();
                        cumulativeRiskLevel += symptoms.updateEvidence(contact_time, symptom_date);
                        showsSymptoms = true;
                    }
                }
            }
            if (testArrayList.size() > 0) {
                for (int test = 0; test < testArrayList.size(); test++) {
                    LocalDateTime test_time = (LocalDateTime) testArrayList.get(test).get("start_date");
                    if (contact_time.isBefore(test_time)) {
                        CovidTest covidTest = new CovidTest((CovidTestType) testArrayList.get(test).get("testType"), (boolean) testArrayList.get(test).get("isPositive"));
                        System.out.println("Covid CCC" + covidTest.getName());
                        double testEvidence = covidTest.isInfected(contact_time, test_time);
                        System.out.println(testEvidence);
                        cumulativeRiskLevel += testEvidence;
                    }
                }
            }
            double cumulativeRiskLevel3 = (cumulativeRiskLevel) / (symptomsArrayList.size() + testArrayList.size() + 1);
            double [] cumulativeRiskLevel2;
            cumulativeRiskLevel2= updateRiskLevel(contact_time);
            double cumulativeRiskLevel1 = cumulativeRiskLevel2[0];
            cumulativeRiskLevel = cumulativeRiskLevel1 * cumulativeRiskLevel3;
            System.out.println(cumulativeRiskLevel + " prima");
            if (showsSymptoms){
                cumulativeRiskLevel /= (cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1]);
            }
            else{
                cumulativeRiskLevel /= (1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1]);
                //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
            }
            System.out.println(cumulativeRiskLevel + " dopo");
            clusterSubjectsMet.get(contact).replace("risk_level", risk_level, (float) cumulativeRiskLevel);
        }

        HashMap<Integer, Double> output = new HashMap<>();
        int size = subjects_solutions.get(subjects_solutions.keySet().iterator().next()).size();
        for (int i = 0; i < size; i++) {
            output.put(i, 0.0);
        }
        if (clusterSubjectsMet.size() > 0) {
            PetriNet net = new PetriNet();
            //creating the central node
            Marking marking = new Marking();
            Place Contagio = net.addPlace("Contagio");
            buildContagionEvolutionSection(net, marking, Contagio);
            marking.setTokens(Contagio, 1);

            RegTransient analysis = RegTransient.builder()
                    .greedyPolicy(new BigDecimal(samples), new BigDecimal("0.001"))
                    .timeStep(new BigDecimal(step)).build();

            //If(Contagioso>0&&Sintomatico==0,1,0);Contagioso;Sintomatico;If(Guarito+Isolato>0,1,0)
            var rewardRates = TransientSolution.rewardRates("Contagioso");

            TransientSolution<DeterministicEnablingState, Marking> solution =
                    analysis.compute(net, marking);

            var rewardedSolution = TransientSolution.computeRewards(false, solution, rewardRates);
            //new TransientSolutionViewer(rewardedSolution);
            TransientSolution<S, R> s = (TransientSolution<S, R>) rewardedSolution;

            int i = 0; //index of contact
            int j = 0; //n. person met during contact counter
            int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
            for (int m = 0; m < s.getColumnStates().size(); m++) {
                while(i < clusterSubjectsMet.size()) { //itera sugli eventi
                    j = 0;
                    String[] meeting_subjects = new String[clusterSubjectsMet.size()];
                    LocalDateTime meeting_time1 = LocalDateTime.from((LocalDateTime) clusterSubjectsMet.get(i).get("start_date"));
                    while (i < clusterSubjectsMet.size() && clusterSubjectsMet.get(i).get("start_date").equals(meeting_time1)) {
                        meeting_subjects[j] = clusterSubjectsMet.get(i).get("fiscalCode").toString();
                        j++;
                        i++;
                    }
                    System.out.println("Print i " + i);
                    double maxRisk = 0.0;
                    int delta = (int) ChronoUnit.HOURS.between(pastStartTime, meeting_time1);
                    for (int k = 0; k < j; k++) {
                        double tmp = subjects_solutions.get(meeting_subjects[k]).get(delta);
                        if (tmp > maxRisk)
                            maxRisk = tmp;
                    }
                    double step = s.getStep().doubleValue();
                    int index = 0;
                    for (int jj = delta; jj < samples; jj += step){
                        double y = s.getSolution()[index][r][m] * maxRisk;
                        double oldY = output.get(jj);
                        output.put(jj, y + oldY); //TODO anziché y + oldY non è meglio (1-oldY) * y + oldY?
                        index++;
                    }
                }
            }

        }
        return output;
    }

    public double[] updateRiskLevel(LocalDateTime contact_time) {
        //leggere i dati dal file cvs
        double[] cumulativeRiskLevel2 = new double[2];
        CSVReader reader = null;
        try{
            //covid file
            reader = new CSVReader(new FileReader("src/main/res/dati_sintomi_covid.CSV"));
            String[] nextLine;
            HashMap<LocalDateTime, Integer> covidSymp = new HashMap<>();
            while((nextLine = reader.readNext()) != null){
                for(String token : nextLine){
                    String[] ext = token.split(";");
                    if(ext[0].matches(".*(/2022|/12/2021)$")){
                        String[] d_m_y = ext[0].split("/");
                        covidSymp.put(LocalDateTime.of(Integer.parseInt(d_m_y[2]), Integer.parseInt(d_m_y[1]), Integer.parseInt(d_m_y[0]), 0, 0),Integer.parseInt(ext[1]));
                    }
                }
            }
            reader.close();

            //flu file
            nextLine = null;
            Reader r = Files.newBufferedReader(Path.of("src/main/res/incidenza-delle-sindromi.csv")); //mi baso sulla stagione 2021-2022
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator('\n')
                    .withIgnoreQuotations(true)
                    .build();
            CSVReader csvreader = new CSVReaderBuilder(r)
                    .withSkipLines(0)
                    .withCSVParser(parser)
                    .build();
            HashMap<LocalDateTime, Integer> fluSymp = new HashMap<>();
            while((nextLine = csvreader.readNext()) != null){
                for(String token : nextLine) {
                        String tk = token.replace(",", ".");
                        String[] ext = tk.split(";");
                    if (ext[0].matches("\\d+")) {
                        double nCont = Double.parseDouble(ext[1]);
                        nCont = nCont * 59110000/1000;
                        for (int day = 0; day < 7; day++){
                            fluSymp.put(LocalDateTime.of(2022,1,1,0,0).plusWeeks(Integer.parseInt(ext[0])-1).plusDays(day), (int) nCont); //alcuni dati si riferiscono alla fine del 2021 ma conviene trattarli come se fossero tutti del 2022 perché considero solo una stagione influenzale
                        }
                    }
                }
            }
            //STA ROBA SOTTO SERVE A QUALCOSA?
            fluSymp.put(LocalDateTime.of(2021,1,1,0,0).plusWeeks(40), fluSymp.get(LocalDateTime.of(2022,1,1,0,0).plusWeeks(40)));
            fluSymp.put(LocalDateTime.of(2021,1,1,0,0).plusWeeks(41), fluSymp.get(LocalDateTime.of(2022,1,1,0,0).plusWeeks(41)));
            csvreader.close();

            double nSymp = 0;
            double nCov = 0;
            LocalDateTime cTime = contact_time;
            cTime = cTime.withHour(0).withMinute(0);
            if(contact_time.getYear() != 2022)
                cTime = cTime.withYear(2022); //mi baso sui dati del 2022

            //Considero che una persona abbia i sintomi per 2 settimane e quindi quando calcolo quante persone hanno i sintomi del covid considero tutti quelli che hanno iniziato a mostrare i sintomi nelle ultime 2 settimane
            for(LocalDateTime ldt = cTime ; ldt.isAfter(cTime.minusWeeks(2)); ldt = ldt.minusDays(1)){
                nCov += covidSymp.get(ldt);
            }
            if(cTime.isAfter(LocalDateTime.of(2022,1,1,0,0).plusWeeks(16))&&cTime.isBefore(LocalDateTime.of(2022, 1, 1,0,0).plusWeeks(42))){
                //non ci sono i dati relativi a questo periodo
            }else {
                nSymp += fluSymp.get(cTime);
            }
            cumulativeRiskLevel2[0] = nCov / 59110000;
            cumulativeRiskLevel2[1] = nSymp / 59110000;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cumulativeRiskLevel2; //FIXME il rischio è altissimo se una persona mostra i sintomi. Come likelihood che indica quale soggetto è più a rischio può andare bene però è un po'brutto quando si plotta il grafico.
    }

    public float addTimeRelevance(float elapsedTime, float risk){
        //TODO forse va fatto qualcosa. Forse però. Forse va bene così
       // risk = risk * 1/elapsedTime;
        return risk;
    }

    public XYChart.Series buildSolution(ArrayList<HashMap<String, XYChart.Series<String,Float>>> pns, String fiscalCode) {
        XYChart.Series<String, Float> output = new XYChart.Series<>();
        for(int singleStep = 0; singleStep < samples; singleStep+=step){
            float value = 0.0f;
            for(int i = 0; i < pns.size(); i++) {
                if (pns.get(i).get(fiscalCode) != null) {
                    value += (1-value) * pns.get(i).get(fiscalCode).getData().get(singleStep).getYValue();
                }
            }
            output.getData().add(new XYChart.Data<>(String.valueOf(singleStep), value));
        }
        return output;
    }
}
