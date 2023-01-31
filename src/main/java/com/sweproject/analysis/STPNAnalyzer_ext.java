package com.sweproject.analysis;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.sweproject.model.CovidTest;
import com.sweproject.model.CovidTestType;
import com.sweproject.model.Symptoms;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

import java.io.File;
import java.io.FileNotFoundException;
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
    public TransientSolution<R,S> makeModel(String fiscalCode, ArrayList<HashMap<String, Object>> environmentArrayList, ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList) throws Exception {

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
            LocalDateTime eventDate = (LocalDateTime) environmentArrayList.get(0).get("start_date");
            float delta = (float) ChronoUnit.MINUTES.between(now, eventDate) / 60.f;
            float elapsedTime = (float) ChronoUnit.MINUTES.between(endInterval, eventDate) / 60.f;
            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(delta)));
            float risk_level = (float) environmentArrayList.get(0).get("risk_level");
            risk_level = addTimeRelevance(elapsedTime, risk_level);
            e0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(risk_level), net)));
            u0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1 - risk_level), net)));

            net.addPrecondition(p1, t0);
            net.addPostcondition(t0, p2);
            net.addPrecondition(p2, e0);
            net.addPostcondition(e0, Contagio);
            net.addPrecondition(p2, u0);

            Transition lastTransition = u0;
            System.out.println("ENV_ARRAY_SIZE "+environmentArrayList.size());
            for (int i = 1; i < environmentArrayList.size(); i++) {
                Place p3 = net.addPlace("Dopo incontro " + i);
                Place p4 = net.addPlace("Incontro " + (i + 1));
                Transition t1 = net.addTransition("t " + i), e1 = net.addTransition("effective " + i), u1 = net.addTransition("uneffective " + i);
                eventDate = (LocalDateTime) environmentArrayList.get(i).get("start_date");;
                delta = (float) ChronoUnit.MINUTES.between((LocalDateTime) environmentArrayList.get(i - 1).get("start_date"), eventDate) / 60.f;
                elapsedTime = (float) ChronoUnit.MINUTES.between(endInterval, eventDate) / 60.f;
                t1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(delta)));
                risk_level = (float) environmentArrayList.get(i).get("risk_level");
                risk_level = addTimeRelevance(elapsedTime, risk_level);
                e1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(risk_level), net)));
                u1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1 - risk_level), net)));
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

            //TODO: add plots of other rewards and change title
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

    private double updateRiskLevel(double cumulativeRiskLevel, LocalDateTime contact_time, boolean showsSymptoms) {
        //leggere i dati dal file cvs
        double updatedRiskLevel = cumulativeRiskLevel;
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
                        fluSymp.put(LocalDateTime.of(2022,1,1,0,0).plusWeeks(Integer.parseInt(ext[0])-1), (int) nCont); //alcuni dati si riferiscono alla fine del 2021 ma conviene trattarli come se fossero tutti del 2022 perché considero solo una stagione influenzale
                    }
                }
            }
            fluSymp.put(LocalDateTime.of(2021,1,1,0,0).plusWeeks(40), fluSymp.get(LocalDateTime.of(2022,1,1,0,0).plusWeeks(40)));
            fluSymp.put(LocalDateTime.of(2021,1,1,0,0).plusWeeks(41), fluSymp.get(LocalDateTime.of(2022,1,1,0,0).plusWeeks(41)));
            csvreader.close();

            double nSymp = 0;
            LocalDateTime cTime = contact_time;
            cTime = cTime.withHour(0).withMinute(0);
            if(contact_time.getYear() != 2022)
                cTime = cTime.withYear(2022); //mi baso sui dati del 2022

            //Considero che una persona abbia i sintomi per 2 settimane e quindi quando calcolo quante persone hanno i sintomi del covid considero tutti quelli che hanno iniziato a mostrare i sintomi nelle ultime 2 settimane
            for( LocalDateTime ldt = cTime ; ldt.isAfter(cTime.minusWeeks(2)); ldt = ldt.minusDays(1)){
                nSymp += covidSymp.get(ldt);
            }
            if(cTime.isAfter(LocalDateTime.of(2022,1,1,0,0).plusWeeks(16))&&cTime.isBefore(LocalDateTime.of(2022, 1, 1,0,0).plusWeeks(42))){
                //non ci sono i dati relativi a questo periodo
            }else {
                LocalDateTime nearestDate = LocalDateTime.of(1980, 1, 1, 0, 0);
                long deltaHours = Long.MAX_VALUE;
                for (LocalDateTime ldt : fluSymp.keySet()) {
                    if (ldt.isBefore(cTime) && ChronoUnit.HOURS.between(ldt, cTime) < deltaHours) {
                        deltaHours = ChronoUnit.HOURS.between(ldt, cTime);
                        nearestDate = ldt;
                    }
                }
                nSymp += fluSymp.get(nearestDate) + fluSymp.get(nearestDate.minusWeeks(1));
            }
            if(showsSymptoms) {
                updatedRiskLevel = cumulativeRiskLevel / (nSymp / 59110000);
            }else{
                updatedRiskLevel = cumulativeRiskLevel / (1-(nSymp / 59110000));
            }
            System.out.println(updatedRiskLevel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updatedRiskLevel;
    }

    public float addTimeRelevance(float elapsedTime, float risk){
        //TODO
       // risk = risk * 1/elapsedTime;
        //TODO funzione di riduzione del rischio tirata a caso per ora
        return risk;
    }
}
