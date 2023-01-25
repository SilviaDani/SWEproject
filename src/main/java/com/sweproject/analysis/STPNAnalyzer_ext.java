package com.sweproject.analysis;

import com.sweproject.model.CovidTest;
import com.sweproject.model.CovidTestType;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

public class STPNAnalyzer_ext<R,S> extends STPNAnalyzer{
    public STPNAnalyzer_ext(int samples, int step) {
        super(samples, step);
    }
    public TransientSolution<R,S> makeModel(String fiscalCode, ArrayList<HashMap<String, Object>> environmentArrayList, ArrayList<HashMap<String, Object>> testArrayList, ArrayList<HashMap<String, Object>> symptomsArrayList) throws Exception {
        LocalDateTime endInterval = LocalDateTime.now();
        LocalDateTime now = endInterval.minusDays(6);
        if (environmentArrayList.size() > 0){
            for (int contact = 0; contact < environmentArrayList.size(); contact++){
                LocalDateTime contact_time = (LocalDateTime) environmentArrayList.get(contact).get("start_date");
                if (symptomsArrayList.size() > 0){
                    for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++){
                        LocalDateTime symptom_date = (LocalDateTime) symptomsArrayList.get(symptom).get("start_date");
                        if (contact_time.isBefore(symptom_date)){
                            float delta = (float) ChronoUnit.DAYS.between(contact_time, symptom_date);
                            float risk_level = (float) environmentArrayList.get(contact).get("risk_level");
                            float new_risk_level = risk_level * delta/4;
                            //TODO sostituire delta/4 con un valore basato su una curva e fare in modo di valutare per bene
                            // la probabilità perchè ora diminuisce sempre per forza
                            environmentArrayList.get(contact).replace("risk_level", risk_level, new_risk_level);
                        }
                    }
                }
                if (testArrayList.size() > 0){
                    for (int test = 0; test < testArrayList.size(); test++){
                        LocalDateTime test_time = (LocalDateTime) testArrayList.get(test).get("start_date");
                        if (contact_time.isBefore(test_time)){
                            CovidTest covidTest = new CovidTest((CovidTestType) testArrayList.get(test).get("testType"), (boolean) testArrayList.get(test).get("isPositive"));
                            System.out.println("Covid CCC" + covidTest.getName());
                            float risk_level = (float) environmentArrayList.get(contact).get("risk_level");
                            double testEvidence = covidTest.isInfected(contact_time, test_time);
                            System.out.println(testEvidence);
                            double new_risk_level;
                            new_risk_level = risk_level * testEvidence;
                            //TODO sostituire delta/4 con un valore basato su una curva e fare in modo di valutare per bene
                            // la probabilità perchè ora diminuisce sempre per forza
                            environmentArrayList.get(contact).replace("risk_level", risk_level, new_risk_level);*/
                        }
                    }
                }
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

    public float addTimeRelevance(float elapsedTime, float risk){
        risk = risk * 1/elapsedTime;
        //TODO funzione di riduzione del rischio tirata a caso per ora
        return risk;
    }
}
