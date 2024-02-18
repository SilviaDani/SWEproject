package com.sweproject.analysis;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.sweproject.model.CovidTest;
import com.sweproject.model.CovidTestType;
import com.sweproject.model.Subject;
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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class STPNAnalyzer_ext<R,S> extends STPNAnalyzer{
    TransientSolution<R,S> rewardedSolution;
    TransientSolution<R,S> symptomSolution;
    double probObsSymptoms = 0;
    double probObsPositiveTests = 0;
    double probObsNegativeTests = 0;
    public STPNAnalyzer_ext(int samples, int step) {
        super(samples, step);
        PetriNet net = new PetriNet();
        Marking marking = new Marking();
        //Generating Nodes
        Place Contagio = net.addPlace("Contagio");
        buildContagionEvolutionSection(net, marking, Contagio);
        marking.setTokens(Contagio, 1);


        // 144 -> 6 giorni
        RegTransient analysis = RegTransient.builder()
                .greedyPolicy(new BigDecimal(samples), new BigDecimal("0.0000001"))
                .timeStep(new BigDecimal(step)).build();

        //If(Contagioso>0&&Sintomatico==0,1,0);Contagioso;Sintomatico;If(Guarito+Isolato>0,1,0)
        var rewardRates = TransientSolution.rewardRates("Contagioso");
        var symptomRates = TransientSolution.rewardRates("Sintomatico");
        TransientSolution<DeterministicEnablingState, Marking> solution =
                analysis.compute(net, marking);

        rewardedSolution = (TransientSolution<R, S>) TransientSolution.computeRewards(false, solution, rewardRates);
        symptomSolution = (TransientSolution<R, S>) TransientSolution.computeRewards(false, solution, symptomRates);
    }

    public TransientSolution<R,S> makeModel(ArrayList<HashMap<String, Object>> environmentArrayList, ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList) throws Exception {
        for(int testIndex = 0; testIndex < testArrayList.size(); testIndex++){
            String rawType = (String) testArrayList.get(testIndex).get("type".toUpperCase());
            //.out.println("RAW TYPE " + rawType);
            String[] extractedType = rawType.split("-"); // 0 -> Covid_test, 1 -> type of test, 2 -> outcome
            testArrayList.get(testIndex).put("testType", extractedType[1].equals("MOLECULAR")? CovidTestType.MOLECULAR:CovidTestType.ANTIGEN);
            testArrayList.get(testIndex).put("isPositive", extractedType[2].equals("true"));
        }
        boolean showsSymptoms;
        boolean isThereAPositiveTest;
        boolean isThereANegativeTest;
        if (environmentArrayList.size() > 0){
            for (int contact = 0; contact < environmentArrayList.size(); contact++){
                showsSymptoms = false;
                isThereAPositiveTest = false;
                isThereANegativeTest = false;
                //System.out.println("CONCTACT TIME" + environmentArrayList.get(contact).get("START_DATE"));
                LocalDateTime contact_time = (((oracle.sql.TIMESTAMP)environmentArrayList.get(contact).get("START_DATE")).timestampValue()).toLocalDateTime();
                //System.out.println("CONTACT TIME " + contact_time);
                float risk_level = ((BigDecimal)environmentArrayList.get(contact).get("RISK_LEVEL")).floatValue();
                environmentArrayList.get(contact).replace("RISK_LEVEL", (float) risk_level);
                float symp_risk_level = 0;
                float test_risk_level = 0;
                if(testArrayList.size() > 0 || symptomsArrayList.size() > 0) {
                    //System.out.println("dentro");
                    if (symptomsArrayList.size() > 0) {
                        for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++) {
                            LocalDateTime symptom_date = (((oracle.sql.TIMESTAMP)symptomsArrayList.get(symptom).get("START_DATE")).timestampValue()).toLocalDateTime();
                            if (contact_time.isBefore(symptom_date)) {
                                Symptoms symptoms = new Symptoms();
                                double sympEvidence = symptoms.updateEvidence(contact_time, symptom_date, symptomSolution);
                                symp_risk_level += Math.log(sympEvidence <= 0 ? 1 : sympEvidence);
                                //System.out.println("Symp " + sympEvidence);
                                showsSymptoms = true;
                            }
                        }
                    }
                    if (testArrayList.size() > 0) {
                        for (int test = 0; test < testArrayList.size(); test++) {
                            LocalDateTime test_time = (((oracle.sql.TIMESTAMP)testArrayList.get(test).get("START_DATE")).timestampValue()).toLocalDateTime();
                            if (contact_time.isBefore(test_time)) {
                                CovidTest covidTest = new CovidTest((CovidTestType) testArrayList.get(test).get("testType"), (boolean) testArrayList.get(test).get("isPositive"));
                                //System.out.println("Covid CCC " + covidTest.getName());
                                if(covidTest.isPositive())
                                    isThereAPositiveTest = true;
                                else
                                    isThereANegativeTest = true;
                                double testEvidence = covidTest.isInfected(contact_time, test_time);
                                //System.out.println(testEvidence);
                                test_risk_level += Math.log(testEvidence);
                            }
                        }
                    }
                    // il calcolo è fatto dando la stessa importanza al rischio attribuito, e alle varie osservazioni
                    double cumulativeRiskLevel =  symp_risk_level + test_risk_level + Math.log(risk_level);
                    double[] cumulativeRiskLevel2;
                    //cumulativeRiskLevel2 = updateRiskLevel(contact_time); //fixme
                    //[0] Covid diagnosticati sulla popolazione, [1] Influenza sulla popolazione
                    double probObs = 0;
                    if (showsSymptoms) { //TODO VA FATTO PER TUTTI UGUALE O IN BASE A SE SI HANNO SINTOMI? IO PENSO SIA SENZA ELSE
                        probObs += this.probObsSymptoms;
                        //cumulativeRiskLevel /= (cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1]);
                        //cumulativeRiskLevel -= (Math.log(cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori altissimi //fixme
                    } else {
                        probObs += (1-this.probObsSymptoms);
                        //cumulativeRiskLevel -= (Math.log(1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori negativissimi //fixme
                        //cumulativeRiskLevel /= (1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1]);
                        //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
                    }
                    if(isThereAPositiveTest){
                        probObs += this.probObsPositiveTests;
                    }else{
                        probObs += (1-this.probObsPositiveTests);
                    }
                    if(isThereANegativeTest){
                        probObs += this.probObsNegativeTests;
                    }else{
                        probObs += (1-this.probObsNegativeTests);
                    }
                    cumulativeRiskLevel-=Math.log(probObs);
                    var b = environmentArrayList.get(contact).replace("RISK_LEVEL", (float)Math.exp(cumulativeRiskLevel));
                    //var b = environmentArrayList.get(contact).replace("RISK_LEVEL", risk_level, (float) cumulativeRiskLevel);
                   // System.out.println("PN " + contact_time + " -> " + cumulativeRiskLevel + " -> " + b);
                }
                /*
                System.out.println(environmentArrayList.get(contact).get("risk_level") + " dopo");
                */
            }
            return rewardedSolution;
        }else{
            System.out.println("The subject has no 'Environment' observations of the last 6 days");
            return super.makeFakeNet();
        }
    }

    //with time limit
    public TransientSolution<R,S> makeModel(ArrayList<HashMap<String, Object>> environmentArrayList, ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList, int timeLimit) throws Exception {
        for(int testIndex = 0; testIndex < testArrayList.size(); testIndex++){
            String rawType = (String) testArrayList.get(testIndex).get("type".toUpperCase());
            //.out.println("RAW TYPE " + rawType);
            String[] extractedType = rawType.split("-"); // 0 -> Covid_test, 1 -> type of test, 2 -> outcome
            testArrayList.get(testIndex).put("testType", extractedType[1].equals("MOLECULAR")? CovidTestType.MOLECULAR:CovidTestType.ANTIGEN);
            testArrayList.get(testIndex).put("isPositive", extractedType[2].equals("true"));
        }
        boolean showsSymptoms;
        boolean isThereAPositiveTest;
        boolean isThereANegativeTest;
        if (environmentArrayList.size() > 0){
            for (int contact = 0; contact < environmentArrayList.size(); contact++){
                showsSymptoms = false;
                isThereAPositiveTest = false;
                isThereANegativeTest = false;
                //System.out.println("CONCTACT TIME" + environmentArrayList.get(contact).get("START_DATE"));
                LocalDateTime contact_time = (((oracle.sql.TIMESTAMP)environmentArrayList.get(contact).get("START_DATE")).timestampValue()).toLocalDateTime();
                //System.out.println("CONTACT TIME " + contact_time);
                float risk_level = ((BigDecimal)environmentArrayList.get(contact).get("RISK_LEVEL")).floatValue();
                environmentArrayList.get(contact).replace("RISK_LEVEL", (float) risk_level);
                float symp_risk_level = 0;
                float test_risk_level = 0;
                if(testArrayList.size() > 0 || symptomsArrayList.size() > 0) {
                    //System.out.println("dentro");
                    if (symptomsArrayList.size() > 0) {
                        for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++) {
                            LocalDateTime symptom_date = (((oracle.sql.TIMESTAMP) symptomsArrayList.get(symptom).get("START_DATE")).timestampValue()).toLocalDateTime();
                            if (symptom_date.isAfter(now.plusHours(timeLimit))) {
                                break;
                            } else {
                                if (contact_time.isBefore(symptom_date) && !symptom_date.isAfter(now.minusHours(samples).plusHours(timeLimit))) {
                                    Symptoms symptoms = new Symptoms();
                                    double sympEvidence = symptoms.updateEvidence(contact_time, symptom_date, symptomSolution);
                                    symp_risk_level += Math.log(sympEvidence <= 0 ? 1 : sympEvidence);
                                    //System.out.println("Symp " + sympEvidence);
                                    showsSymptoms = true;
                                }
                            }
                        }
                    }
                    if (testArrayList.size() > 0) {
                        for (int test = 0; test < testArrayList.size(); test++) {
                            LocalDateTime test_time = (((oracle.sql.TIMESTAMP) testArrayList.get(test).get("START_DATE")).timestampValue()).toLocalDateTime();
                            if (test_time.isAfter(now.minusHours(samples).plusHours(timeLimit))) {
                                break;
                            } else {
                                if (contact_time.isBefore(test_time)) {
                                    CovidTest covidTest = new CovidTest((CovidTestType) testArrayList.get(test).get("testType"), (boolean) testArrayList.get(test).get("isPositive"));
                                    //System.out.println("Covid CCC " + covidTest.getName());
                                    if (covidTest.isPositive())
                                        isThereAPositiveTest = true;
                                    else
                                        isThereANegativeTest = true;
                                    double testEvidence = covidTest.isInfected(contact_time, test_time);
                                    //System.out.println(testEvidence);
                                    test_risk_level += Math.log(testEvidence);
                                }
                            }
                        }
                    }
                    // il calcolo è fatto dando la stessa importanza al rischio attribuito, e alle varie osservazioni
                    double cumulativeRiskLevel =  symp_risk_level + test_risk_level + Math.log(risk_level);
                    double[] cumulativeRiskLevel2;
                    //cumulativeRiskLevel2 = updateRiskLevel(contact_time); //fixme
                    //[0] Covid diagnosticati sulla popolazione, [1] Influenza sulla popolazione
                    double probObs = 0;
                    if (showsSymptoms) { //TODO VA FATTO PER TUTTI UGUALE O IN BASE A SE SI HANNO SINTOMI? IO PENSO SIA SENZA ELSE
                        probObs += this.probObsSymptoms;
                        //cumulativeRiskLevel /= (cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1]);
                        //cumulativeRiskLevel -= (Math.log(cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori altissimi //fixme
                    } else {
                        probObs += (1-this.probObsSymptoms);
                        //cumulativeRiskLevel -= (Math.log(1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori negativissimi //fixme
                        //cumulativeRiskLevel /= (1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1]);
                        //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
                    }
                    if(isThereAPositiveTest){
                        probObs += this.probObsPositiveTests;
                    }else{
                        probObs += (1-this.probObsPositiveTests);
                    }
                    if(isThereANegativeTest){
                        probObs += this.probObsNegativeTests;
                    }else{
                        probObs += (1-this.probObsNegativeTests);
                    }
                    cumulativeRiskLevel-=Math.log(probObs);
                    var b = environmentArrayList.get(contact).replace("RISK_LEVEL", (float)Math.exp(cumulativeRiskLevel));
                    //var b = environmentArrayList.get(contact).replace("RISK_LEVEL", risk_level, (float) cumulativeRiskLevel);
                    // System.out.println("PN " + contact_time + " -> " + cumulativeRiskLevel + " -> " + b);
                }
                /*
                System.out.println(environmentArrayList.get(contact).get("risk_level") + " dopo");
                */
            }
            return rewardedSolution;
        }else{
            System.out.println("The subject has no 'Environment' observations of the last 6 days");
            return super.makeFakeNet();
        }
    }
    public HashMap<Integer, Double> computeAnalysis(TransientSolution<S, R> s, ArrayList<HashMap<String, Object>> eventsArrayList, LocalDateTime pastStartTime){
        HashMap<Integer, Double> output = new HashMap<>();
        double notInfectedYet = 1;
        int size = s.getSamplesNumber();
        for (int i = 0; i < size; i++){
            output.put(i, 0.0);
        }
        int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
        for(int m = 0; m < s.getColumnStates().size(); m++){
            //System.out.println(m + " m");
            double step = s.getStep().doubleValue();
            for (int event = 0; event < eventsArrayList.size(); event++){
                LocalDateTime eventTime = null;
                try {
                    eventTime = (((oracle.sql.TIMESTAMP) eventsArrayList.get(event).get("START_DATE")).timestampValue()).toLocalDateTime();
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
                int delta = (int) ChronoUnit.HOURS.between(pastStartTime, eventTime);
                float risk = (float) eventsArrayList.get(event).get("RISK_LEVEL");
                //System.out.println("PN2 " + eventTime + " -> "+risk);
                int i = 0;
                for (int j = delta; j < size; j += step){
                    double y = s.getSolution()[i][r][m] * risk;
                    double oldY = output.get(j);
                    output.replace(j, notInfectedYet * y + oldY);
                    i++;
                }
                notInfectedYet *= (1 - risk);
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

    public XYChart.Series makeClusterModelForApp(LocalDateTime pastStartTime, HashMap<String, XYChart.Series<String,Float>> subjects_ss, ArrayList<HashMap<String, Object>> clusterSubjectsMet,
                                                 ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList, String nameOfPersonAskingForAnalysis) throws Exception{
        HashMap<String, HashMap<Integer, Double>> subjects_solutions = new HashMap<>();
        for(String s : subjects_ss.keySet()){
            HashMap<Integer, Double> sol = new HashMap<>();

            for(int i = 0; i < samples; i++){
                sol.put(i, subjects_ss.get(s).getData().get(i).getYValue().doubleValue());
            }
            subjects_solutions.put(s, sol);
        }
        var model = makeClusterModel(pastStartTime, subjects_solutions, clusterSubjectsMet, testArrayList, symptomsArrayList, nameOfPersonAskingForAnalysis);
        return adaptForApp(model);
    }
    public HashMap<Integer, Double> makeClusterModel(LocalDateTime pastStartTime, HashMap<String, HashMap<Integer, Double>> subjects_solutions, ArrayList<HashMap<String, Object>> clusterSubjectsMet,
                                                     ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList, String nameOfPersonAskingForAnalysis) throws Exception {

        for (int testIndex = 0; testIndex < testArrayList.size(); testIndex++) {
            String rawType = (String) testArrayList.get(testIndex).get("type".toUpperCase());
            String[] extractedType = rawType.split("-"); // 0 -> Covid_test, 1 -> type of test, 2 -> outcome
            testArrayList.get(testIndex).put("testType", extractedType[1].equals("MOLECULAR") ? CovidTestType.MOLECULAR : CovidTestType.ANTIGEN);
            testArrayList.get(testIndex).put("isPositive", extractedType[2].equals("true"));
        }
        boolean showsSymptoms = false;
        boolean isThereAPositiveTest = false;
        boolean isThereANegativeTest = false;
        HashMap<String, Boolean> symptomaticSubjects = new HashMap<>();
        for(String subject : subjects_solutions.keySet()){
            symptomaticSubjects.put(subject, false);
        }
        Double factorDueToSymptoms = null;
        for (int contact = 0; contact < clusterSubjectsMet.size(); contact++) {
            for (String subject : subjects_solutions.keySet()) {
                symptomaticSubjects.replace(subject, false);
            }
            clusterSubjectsMet.get(contact).put("symptomaticSubjects", new HashMap<>(symptomaticSubjects));
            LocalDateTime contact_time = (((oracle.sql.TIMESTAMP) clusterSubjectsMet.get(contact).get("START_DATE")).timestampValue()).toLocalDateTime();
            var rl = clusterSubjectsMet.get(contact).get("RISK_LEVEL");
            float risk_level = 0;
            if (rl instanceof  BigDecimal){
                risk_level = ((BigDecimal)clusterSubjectsMet.get(contact).get("RISK_LEVEL")).floatValue();
            }else{
                risk_level = (float) clusterSubjectsMet.get(contact).get("RISK_LEVEL");
            }
            clusterSubjectsMet.get(contact).replace("RISK_LEVEL", (float) risk_level);
            float symp_risk_level = 0;
            float test_risk_level = 0;
            if (symptomsArrayList.size() > 0) {
                for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++) {
                    LocalDateTime symptom_date = (((oracle.sql.TIMESTAMP)symptomsArrayList.get(symptom).get("START_DATE")).timestampValue()).toLocalDateTime();
                    if (contact_time.isBefore(symptom_date)) {
                        Symptoms symptoms = new Symptoms();
                        double sympEvidence = symptoms.updateEvidence(contact_time, symptom_date, symptomSolution);
                        symp_risk_level += Math.log(sympEvidence<=0?1:sympEvidence); //XXX [CHECK IF IT'S CORRECT]
                        symptomaticSubjects.replace((String) symptomsArrayList.get(symptom).get("FISCALCODE"), true);
                    }
                }
            }
            if (testArrayList.size() > 0) {
                for (int test = 0; test < testArrayList.size(); test++) {
                    LocalDateTime test_time = (((oracle.sql.TIMESTAMP)testArrayList.get(test).get("START_DATE")).timestampValue()).toLocalDateTime();
                    if (contact_time.isBefore(test_time)) {
                        CovidTest covidTest = new CovidTest((CovidTestType) testArrayList.get(test).get("testType"), (boolean) testArrayList.get(test).get("isPositive"));
                        //System.out.println("Covid CCC" + covidTest.getName());
                        if(covidTest.isPositive()){
                            isThereAPositiveTest = true;
                        }else{
                            isThereANegativeTest = true;
                        }
                        double testEvidence = covidTest.isInfected(contact_time, test_time);
                        //System.out.println(testEvidence);
                        test_risk_level += Math.log(testEvidence);
                    }
                }
            }
            double cumulativeRiskLevel = symp_risk_level + test_risk_level + Math.log(risk_level);
            double[] cumulativeRiskLevel2;
            //cumulativeRiskLevel2 = updateRiskLevel(contact_time); //fixme
            //System.out.println(cumulativeRiskLevel + " prima");
            clusterSubjectsMet.get(contact).replace("symptomaticSubjects", symptomaticSubjects);
            double probObs = 0;
            if (showsSymptoms) { //TODO VA FATTO PER TUTTI UGUALE O IN BASE A SE SI HANNO SINTOMI? IO PENSO SIA SENZA ELSE
                probObs += this.probObsSymptoms;
                //cumulativeRiskLevel /= (cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1]);
                //cumulativeRiskLevel -= (Math.log(cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori altissimi //fixme
            } else {
                probObs += (1-this.probObsSymptoms);
                //cumulativeRiskLevel -= (Math.log(1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori negativissimi //fixme
                //cumulativeRiskLevel /= (1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1]);
                //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
            }
            if(isThereAPositiveTest){
                probObs += this.probObsPositiveTests;
            }else{
                probObs += (1-this.probObsPositiveTests);
            }
            if(isThereANegativeTest){
                probObs += this.probObsNegativeTests;
            }else{
                probObs += (1-this.probObsNegativeTests);
            }
            cumulativeRiskLevel-=Math.log(probObs);
            var b = clusterSubjectsMet.get(contact).replace("RISK_LEVEL", risk_level, (float) Math.exp(cumulativeRiskLevel));
            //System.out.println("PN " + contact_time + " -> " + cumulativeRiskLevel + " -> " + b);

        }

        HashMap<Integer, Double> output = new HashMap<>();
        int size = subjects_solutions.get(subjects_solutions.keySet().iterator().next()).size();
        for (int i = 0; i < size; i++) {
            output.put(i, 0.0);
        }
        if (clusterSubjectsMet.size() > 0) {
            TransientSolution<S, R> s = (TransientSolution<S, R>) rewardedSolution;

            int i = 0; //index of contact
            int j = 0; //n. person met during contact counter
            int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
            for (int m = 0; m < s.getColumnStates().size(); m++) {
                int contactNumber = 0;
                double notInfectedYet = 1;
                while(i < clusterSubjectsMet.size()) { //itera sugli eventi
                    j = 0;
                    String[] meeting_subjects = new String[clusterSubjectsMet.size()];
                    (((oracle.sql.TIMESTAMP) clusterSubjectsMet.get(i).get("START_DATE")).timestampValue()).toLocalDateTime();
                    LocalDateTime meeting_time1 = LocalDateTime.from((((oracle.sql.TIMESTAMP) clusterSubjectsMet.get(i).get("START_DATE")).timestampValue()).toLocalDateTime());
                    if (pastStartTime.plusHours(samples).isAfter(meeting_time1)) {
                        break;
                    } else {
                        while (i < clusterSubjectsMet.size() && (((oracle.sql.TIMESTAMP) clusterSubjectsMet.get(i).get("START_DATE")).timestampValue()).toLocalDateTime().equals(meeting_time1)) {
                            meeting_subjects[j] = clusterSubjectsMet.get(i).get("FISCALCODE").toString();
                            j++;
                            i++;
                        }
                        //System.out.println("Print i " + i);
                        double maxRisk = 0.0;
                        int delta = (int) ChronoUnit.HOURS.between(pastStartTime, meeting_time1);
                        for (int k = 0; k < j; k++) {
                            double tmp = subjects_solutions.get(meeting_subjects[k]).get(delta);
                            if (tmp > maxRisk) {
                                maxRisk = tmp;
                                //      System.out.println("risk increased!");
                            }
                        }
                        double step = s.getStep().doubleValue();
                        int index = 0;

                        float risk = (float) clusterSubjectsMet.get(contactNumber).get("RISK_LEVEL");
                        for (int jj = delta; jj < size; jj += (int) step) {
                            double y = s.getSolution()[index][r][m] * maxRisk * risk;
                            if (factorDueToSymptoms != null) { //XXX remove?
                                if (((HashMap<String, Boolean>) clusterSubjectsMet.get(contactNumber).get("symptomaticSubjects")).get(nameOfPersonAskingForAnalysis)) {
                                    y /= factorDueToSymptoms;
                                } else {
                                    y /= (1 - factorDueToSymptoms);
                                    //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
                                }
                            }
                            double oldY = output.get(jj);
                            output.replace(jj, notInfectedYet * y + oldY);
                            index++;
                        }
                        notInfectedYet *= (1 - maxRisk * risk);
                        contactNumber = i;
                    }
                }
            }
        //System.out.println("STPNAnalyzer_ext, 552");
        }else{
            System.out.println("Non ci sono contatti con altre persone interne al cluster");
        }
        return output;
    }

    //with time limit
    public HashMap<Integer, Double> makeClusterModel(LocalDateTime pastStartTime, HashMap<String, HashMap<Integer, Double>> subjects_solutions, ArrayList<HashMap<String, Object>> clusterSubjectsMet,
                                                     ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList, String nameOfPersonAskingForAnalysis, int timeLimit) throws Exception {

        for (int testIndex = 0; testIndex < testArrayList.size(); testIndex++) {
            String rawType = (String) testArrayList.get(testIndex).get("type".toUpperCase());
            String[] extractedType = rawType.split("-"); // 0 -> Covid_test, 1 -> type of test, 2 -> outcome
            testArrayList.get(testIndex).put("testType", extractedType[1].equals("MOLECULAR") ? CovidTestType.MOLECULAR : CovidTestType.ANTIGEN);
            testArrayList.get(testIndex).put("isPositive", extractedType[2].equals("true"));
        }
        boolean showsSymptoms = false;
        boolean isThereAPositiveTest = false;
        boolean isThereANegativeTest = false;
        HashMap<String, Boolean> symptomaticSubjects = new HashMap<>();
        for(String subject : subjects_solutions.keySet()){
            symptomaticSubjects.put(subject, false);
        }
        Double factorDueToSymptoms = null;
        for (int contact = 0; contact < clusterSubjectsMet.size(); contact++) {
            for (String subject : subjects_solutions.keySet()) {
                symptomaticSubjects.replace(subject, false);
            }
            clusterSubjectsMet.get(contact).put("symptomaticSubjects", new HashMap<>(symptomaticSubjects));
            LocalDateTime contact_time = (((oracle.sql.TIMESTAMP) clusterSubjectsMet.get(contact).get("START_DATE")).timestampValue()).toLocalDateTime();
            var rl = clusterSubjectsMet.get(contact).get("RISK_LEVEL");
            float risk_level = 0;
            if (rl instanceof  BigDecimal){
                risk_level = ((BigDecimal)clusterSubjectsMet.get(contact).get("RISK_LEVEL")).floatValue();
            }else{
                risk_level = (float) clusterSubjectsMet.get(contact).get("RISK_LEVEL");
            }
            clusterSubjectsMet.get(contact).replace("RISK_LEVEL", (float) risk_level);
            float symp_risk_level = 0;
            float test_risk_level = 0;
            if (symptomsArrayList.size() > 0) {
                for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++) {
                    LocalDateTime symptom_date = (((oracle.sql.TIMESTAMP) symptomsArrayList.get(symptom).get("START_DATE")).timestampValue()).toLocalDateTime();
                    if (symptom_date.isAfter(now.minusHours(samples).plusHours(timeLimit))) {
                        break;
                    } else {
                        if (contact_time.isBefore(symptom_date)) {
                            Symptoms symptoms = new Symptoms();
                            double sympEvidence = symptoms.updateEvidence(contact_time, symptom_date, symptomSolution);
                            symp_risk_level += Math.log(sympEvidence <= 0 ? 1 : sympEvidence); //XXX [CHECK IF IT'S CORRECT]
                            symptomaticSubjects.replace((String) symptomsArrayList.get(symptom).get("FISCALCODE"), true);
                        }
                    }
                }
            }
            if (testArrayList.size() > 0) {
                for (int test = 0; test < testArrayList.size(); test++) {
                    LocalDateTime test_time = (((oracle.sql.TIMESTAMP) testArrayList.get(test).get("START_DATE")).timestampValue()).toLocalDateTime();
                    if (test_time.isAfter(now.minusHours(samples).plusHours(timeLimit))) {
                        break;
                    } else {
                        if (contact_time.isBefore(test_time)) {
                            CovidTest covidTest = new CovidTest((CovidTestType) testArrayList.get(test).get("testType"), (boolean) testArrayList.get(test).get("isPositive"));
                            //System.out.println("Covid CCC" + covidTest.getName());
                            if (covidTest.isPositive()) {
                                isThereAPositiveTest = true;
                            } else {
                                isThereANegativeTest = true;
                            }
                            double testEvidence = covidTest.isInfected(contact_time, test_time);
                            //System.out.println(testEvidence);
                            test_risk_level += Math.log(testEvidence);
                        }
                    }
                }
            }
            double cumulativeRiskLevel = symp_risk_level + test_risk_level + Math.log(risk_level);
            double[] cumulativeRiskLevel2;
            //cumulativeRiskLevel2 = updateRiskLevel(contact_time); //fixme
            //System.out.println(cumulativeRiskLevel + " prima");
            clusterSubjectsMet.get(contact).replace("symptomaticSubjects", symptomaticSubjects);
            double probObs = 0;
            if (showsSymptoms) { //TODO VA FATTO PER TUTTI UGUALE O IN BASE A SE SI HANNO SINTOMI? IO PENSO SIA SENZA ELSE
                probObs += this.probObsSymptoms;
                //cumulativeRiskLevel /= (cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1]);
                //cumulativeRiskLevel -= (Math.log(cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori altissimi //fixme
            } else {
                probObs += (1-this.probObsSymptoms);
                //cumulativeRiskLevel -= (Math.log(1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori negativissimi //fixme
                //cumulativeRiskLevel /= (1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1]);
                //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
            }
            if(isThereAPositiveTest){
                probObs += this.probObsPositiveTests;
            }else{
                probObs += (1-this.probObsPositiveTests);
            }
            if(isThereANegativeTest){
                probObs += this.probObsNegativeTests;
            }else{
                probObs += (1-this.probObsNegativeTests);
            }
            cumulativeRiskLevel-=Math.log(probObs);
            var b = clusterSubjectsMet.get(contact).replace("RISK_LEVEL", risk_level, (float) Math.exp(cumulativeRiskLevel));
            //System.out.println("PN " + contact_time + " -> " + cumulativeRiskLevel + " -> " + b);

        }

        HashMap<Integer, Double> output = new HashMap<>();
        int size = subjects_solutions.get(subjects_solutions.keySet().iterator().next()).size();
        for (int i = 0; i < size; i++) {
            output.put(i, 0.0);
        }
        if (clusterSubjectsMet.size() > 0) {
            TransientSolution<S, R> s = (TransientSolution<S, R>) rewardedSolution;

            int i = 0; //index of contact
            int j = 0; //n. person met during contact counter
            int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
            for (int m = 0; m < s.getColumnStates().size(); m++) {
                int contactNumber = 0;
                double notInfectedYet = 1;
                while(i < clusterSubjectsMet.size()) { //itera sugli eventi
                    j = 0;
                    String[] meeting_subjects = new String[clusterSubjectsMet.size()];
                    (((oracle.sql.TIMESTAMP) clusterSubjectsMet.get(i).get("START_DATE")).timestampValue()).toLocalDateTime();
                    LocalDateTime meeting_time1 = LocalDateTime.from((((oracle.sql.TIMESTAMP) clusterSubjectsMet.get(i).get("START_DATE")).timestampValue()).toLocalDateTime());
                    if (pastStartTime.plusHours(samples).isAfter(meeting_time1)) {
                        break;
                    } else {
                        while (i < clusterSubjectsMet.size() && (((oracle.sql.TIMESTAMP) clusterSubjectsMet.get(i).get("START_DATE")).timestampValue()).toLocalDateTime().equals(meeting_time1)) {
                            meeting_subjects[j] = clusterSubjectsMet.get(i).get("FISCALCODE").toString();
                            j++;
                            i++;
                        }
                        //System.out.println("Print i " + i);
                        double maxRisk = 0.0;
                        int delta = (int) ChronoUnit.HOURS.between(pastStartTime, meeting_time1);
                        for (int k = 0; k < j; k++) {
                            double tmp = subjects_solutions.get(meeting_subjects[k]).get(delta);
                            if (tmp > maxRisk) {
                                maxRisk = tmp;
                                //      System.out.println("risk increased!");
                            }
                        }
                        double step = s.getStep().doubleValue();
                        int index = 0;

                        float risk = (float) clusterSubjectsMet.get(contactNumber).get("RISK_LEVEL");
                        for (int jj = delta; jj < size; jj += (int) step) {
                            double y = s.getSolution()[index][r][m] * maxRisk * risk;
                            if (factorDueToSymptoms != null) { //XXX remove?
                                if (((HashMap<String, Boolean>) clusterSubjectsMet.get(contactNumber).get("symptomaticSubjects")).get(nameOfPersonAskingForAnalysis)) {
                                    y /= factorDueToSymptoms;
                                } else {
                                    y /= (1 - factorDueToSymptoms);
                                    //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
                                }
                            }
                            double oldY = output.get(jj);
                            output.replace(jj, notInfectedYet * y + oldY);
                            index++;
                        }
                        notInfectedYet *= (1 - maxRisk * risk);
                        contactNumber = i;
                    }
                }
            }
            //System.out.println("STPNAnalyzer_ext, 552");
        }else{
            System.out.println("Non ci sono contatti con altre persone interne al cluster");
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
        return cumulativeRiskLevel2;
    }

    public void updateProbObsSymptoms(ArrayList<String> subjects_String, HashMap<String, ArrayList<HashMap<String, Object>>> sympObs, LocalDateTime t0) throws SQLException {
        int symptomaticPeople = 0;
        for (String person : subjects_String){
            boolean symptomaticPersonInCluster = false;
            for(HashMap<String, Object> observation : sympObs.get(person)){
                if((((oracle.sql.TIMESTAMP)observation.get("START_DATE")).timestampValue()).toLocalDateTime().isAfter(t0)){
                    symptomaticPersonInCluster = true;
                }
            }
            if (symptomaticPersonInCluster)
                symptomaticPeople++;
        }
        this.probObsSymptoms = (double) symptomaticPeople/subjects_String.size();
    }

    public void updateProbObsTest(ArrayList<String> subjects_String, HashMap<String, ArrayList<HashMap<String, Object>>> testObs, LocalDateTime t0) throws SQLException {
        int positiveTests = 0;
        int negativeTests = 0;
        for (String person : subjects_String){
            boolean pTest = false;
            boolean nTest = false;
            for(HashMap<String, Object> observation : testObs.get(person)) {
                    if ((((oracle.sql.TIMESTAMP) observation.get("START_DATE")).timestampValue()).toLocalDateTime().isAfter(t0)) {
                        if (observation.get("isPositive") != null) {
                            if ((boolean) observation.get("isPositive"))
                                pTest = true;
                            else
                                nTest = true;
                        }
                    }
                }
            if (pTest)
                positiveTests++;
            if (nTest)
                negativeTests++;
        }
        this.probObsPositiveTests = (double) positiveTests/subjects_String.size();
        this.probObsNegativeTests = (double) negativeTests/subjects_String.size();
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
