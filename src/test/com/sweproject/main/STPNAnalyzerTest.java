package com.sweproject.main;

import com.sweproject.dao.ObservationDAO;
import com.sweproject.dao.ObservationDAOTest;
import com.sweproject.model.Environment;
import com.sweproject.model.Type;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedGEN;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.TransientSolutionViewer;
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
import java.util.List;
import java.text.DecimalFormat;

import static org.junit.jupiter.api.Assertions.*;

class STPNAnalyzerTest {
    private static ObservationDAO observationDAO;
    private STPNAnalyzer stpnAnalyzer;

    STPNAnalyzerTest(){
        observationDAO = new ObservationDAO();
        stpnAnalyzer = new STPNAnalyzer(144, 1);
    }
    @Test
    void contactWithEnvironment(){
        //inserisco nel db le osservazioni che mi servono
        //8,20
        //3,2
        ArrayList<String> subjects = new ArrayList<>();
        subjects.add("p1");
        LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6);
        Type[] tt = new Environment[2];
        LocalDateTime[] startDates = new LocalDateTime[]{t0.plusHours(8), t0.plusHours(28)};
        LocalDateTime[] endDates = new LocalDateTime[]{t0.plusHours(8+3), t0.plusHours(28+2)};
        String[] riskLevels = new String[]{"Medium", "Low"};
        boolean[] masks = new boolean[]{false, false};
        float[] risks = new float[2];
        for(int i = 0; i<2; i++){
            tt[i] = new Environment(masks[i], riskLevels[i], startDates[i], endDates[i]);
            risks[i] = BigDecimal.valueOf(((Environment) tt[i]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
            observationDAO.insertObservation(subjects, tt[i], startDates[i], endDates[i]);

            System.out.println("Rischio test " + risks[i]);
        }
        //creo il modello e lo analizzo
        TransientSolution solution = stpnAnalyzer.makeModel(subjects.get(0));
        //confronto punto punto il modello con una rete creata a parte (passandogli i pesi giusti)
        PetriNet pn = new PetriNet();
        Marking marking = new Marking();
        //aaa(pn, marking, risks[0]);
        buildContactWithEnvironmentNet(pn, marking, risks);
        RegTransient analysis = RegTransient.builder()
                .greedyPolicy(new BigDecimal(144), new BigDecimal("0.001"))
                .timeStep(new BigDecimal(1)).build();
        var rewardRates = TransientSolution.rewardRates("Contagioso");
        TransientSolution<DeterministicEnablingState, Marking> controlSolution =
                analysis.compute(pn, marking);
        TransientSolution rewardedSolution = TransientSolution.computeRewards(false, controlSolution, rewardRates);
        //verifica
        assertEquals(rewardedSolution.getRegenerations().size(), solution.getRegenerations().size());
        assertEquals(rewardedSolution.getColumnStates().size(), solution.getColumnStates().size());
        assertEquals(rewardedSolution.getSamplesNumber(), solution.getSamplesNumber());
        int r = solution.getRegenerations().indexOf(solution.getInitialRegeneration());
        for (int m = 0; m<solution.getColumnStates().size(); m++){
            for(int i=0, size = solution.getSamplesNumber(); i<size; i++){
                System.out.println(i + " : " + rewardedSolution.getSolution()[i][r][m] + " - "+solution.getSolution()[i][r][m]);
                //assertEquals(rewardedSolution.getSolution()[i][r][m], solution.getSolution()[i][r][m], 0.001);
            }
        }
        new TransientSolutionViewer(solution);
       new TransientSolutionViewer(rewardedSolution);
       int i =0;
       while(true){
           i = 0;
       }
    }

    private void buildContactWithEnvironmentNet(PetriNet net, Marking marking, float[] weights) {
        //Generating Nodes
        Place Contagio = net.addPlace("Contagio");
        stpnAnalyzer.buildContagionEvolutionSection(net, marking, Contagio);
        Place p13 = net.addPlace("p13"); //Condizione_iniziale
        Place p14 = net.addPlace("p14");
        Place p22 = net.addPlace("p22");
        Place p23 = net.addPlace("p23");

        Transition contact1 = net.addTransition("contact1");
        Transition effective = net.addTransition("effective");
        Transition t16 = net.addTransition("t16");
        Transition t24 = net.addTransition("t24");
        Transition t32 = net.addTransition("t32");
        Transition uneffective = net.addTransition("uneffective");

        //Generating Connectors

        net.addPrecondition(p13, contact1);
        net.addPostcondition(effective, Contagio); // Contagio = contagious
        net.addPostcondition(contact1, p22); //p22 = firstContact
        net.addPrecondition(p22, effective); //effective = effective0
        net.addPrecondition(p22, uneffective);//Non-efficace1
        net.addPostcondition(uneffective, p14);//1 p14 = Condizione1
        net.addPrecondition(p14, t16); //2 t16 = t_1
        net.addPostcondition(t16, p23);//3 p23 = Contatto_1
        net.addPrecondition(p23, t32);//4.2 t32 = Efficace_1
        net.addPostcondition(t32, Contagio); //
        net.addPrecondition(p23, t24); //4.1 t24 = Non-Efficace_2


        //Generating Properties
        marking.setTokens(p13, 1);
        marking.setTokens(p14, 0);
        marking.setTokens(p22, 0);
        marking.setTokens(p23, 0);
        contact1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("8"), MarkingExpr.from("1", net)));
        contact1.addFeature(new Priority(0));
        effective.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weights[0]), net)));
        System.out.println("AAAA" + String.valueOf(weights[0]));
        effective.addFeature(new Priority(0));

        t16.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("20"), MarkingExpr.from("1", net)));
        t16.addFeature(new Priority(0));
        t24.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weights[1]), net)));
        t24.addFeature(new Priority(0));
        t32.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weights[1]), net)));
        t32.addFeature(new Priority(0));
        uneffective.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weights[0]), net)));
        uneffective.addFeature(new Priority(0));
    }

    private void aaa(PetriNet net, Marking marking, float weight) {
        Place Asintomatico = net.addPlace("Asintomatico");
        Place ContagiatoSintomatico = net.addPlace("ContagiatoSintomatico");
        Place Contagio = net.addPlace("Contagio");
        Place Contagioso = net.addPlace("Contagioso");
        Place Guarito = net.addPlace("Guarito");
        Place Isolato = net.addPlace("Isolato");
        Place Sintomatico = net.addPlace("Sintomatico");
        Place p13 = net.addPlace("p13");
        Place p22 = net.addPlace("p22");
        Place p30 = net.addPlace("p30");
        Place p38 = net.addPlace("p38");
        Transition contact1 = net.addTransition("contact1");
        Transition effective = net.addTransition("effective");
        Transition t0 = net.addTransition("t0");
        Transition t1 = net.addTransition("t1");
        Transition t3 = net.addTransition("t3");
        Transition t51 = net.addTransition("t51");
        Transition t52 = net.addTransition("t52");
        Transition t53 = net.addTransition("t53");
        Transition t54 = net.addTransition("t54");
        Transition uneffective = net.addTransition("uneffective");

        //Generating Connectors
        net.addPrecondition(p22, uneffective);
        net.addPostcondition(t52, Asintomatico);
        net.addPrecondition(ContagiatoSintomatico, t0);
        net.addPrecondition(Contagioso, t53);
        net.addPostcondition(t1, Contagioso);
        net.addPrecondition(p38, t51);
        net.addPrecondition(Sintomatico, t54);
        net.addPostcondition(t3, p38);
        net.addPostcondition(t3, p30);
        net.addPrecondition(p30, t1);
        net.addPrecondition(Contagio, t3);
        net.addPrecondition(p22, effective);
        net.addPrecondition(p13, contact1);
        net.addPrecondition(Contagioso, t54);
        net.addPostcondition(t0, Sintomatico);
        net.addPostcondition(t53, Guarito);
        net.addPostcondition(t51, ContagiatoSintomatico);
        net.addPostcondition(t54, Isolato);
        net.addPrecondition(p38, t52);
        net.addPrecondition(Asintomatico, t53);
        net.addPostcondition(effective, Contagio);
        net.addPostcondition(contact1, p22);

        //Generating Properties
        marking.setTokens(Asintomatico, 0);
        marking.setTokens(ContagiatoSintomatico, 0);
        marking.setTokens(Contagio, 0);
        marking.setTokens(Contagioso, 0);
        marking.setTokens(Guarito, 0);
        marking.setTokens(Isolato, 0);
        marking.setTokens(Sintomatico, 0);
        marking.setTokens(p13, 1);
        marking.setTokens(p22, 0);
        marking.setTokens(p30, 0);
        marking.setTokens(p38, 0);
        contact1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("8"), MarkingExpr.from("1", net)));
        contact1.addFeature(new Priority(0));
        effective.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weight), net)));
        effective.addFeature(new Priority(0));
        List<GEN> t0_gens = new ArrayList<>();

        DBMZone t0_d_0 = new DBMZone(new Variable("x"));
        Expolynomial t0_e_0 = Expolynomial.fromString("x^1 * Exp[-0.24 x] + -24*Exp[-0.24 x]");
        //Normalization
        t0_e_0.multiply(new BigDecimal(18.677114570749897));
        t0_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("48"));
        t0_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-24"));
        GEN t0_gen_0 = new GEN(t0_d_0, t0_e_0);
        t0_gens.add(t0_gen_0);

        PartitionedGEN t0_pFunction = new PartitionedGEN(t0_gens);
        StochasticTransitionFeature t0_feature = StochasticTransitionFeature.of(t0_pFunction);
        t0.addFeature(t0_feature);

        List<GEN> t1_gens = new ArrayList<>();

        DBMZone t1_d_0 = new DBMZone(new Variable("x"));
        Expolynomial t1_e_0 = Expolynomial.fromString("48 * x^1 + -1* x^2 + -432 * x^0");
        //Normalization
        t1_e_0.multiply(new BigDecimal(4.3402777777777775E-4));
        t1_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("36"));
        t1_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-12"));
        GEN t1_gen_0 = new GEN(t1_d_0, t1_e_0);
        t1_gens.add(t1_gen_0);

        PartitionedGEN t1_pFunction = new PartitionedGEN(t1_gens);
        StochasticTransitionFeature t1_feature = StochasticTransitionFeature.of(t1_pFunction);
        t1.addFeature(t1_feature);

        t3.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t3.addFeature(new Priority(0));
        t51.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t51.addFeature(new Priority(0));
        t52.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t52.addFeature(new Priority(0));
        t53.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.040", net)));
        t54.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.1", net)));
        uneffective.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weight), net)));
        uneffective.addFeature(new Priority(0));
    }
    private void bbb(PetriNet net, Marking marking, float[] weights){

        //Generating Nodes
        Place Asintomatico = net.addPlace("Asintomatico");
        Place ContagiatoSintomatico = net.addPlace("ContagiatoSintomatico");
        Place Contagio = net.addPlace("Contagio");
        Place Contagioso = net.addPlace("Contagioso");
        Place Guarito = net.addPlace("Guarito");
        Place Isolato = net.addPlace("Isolato");
        Place Sintomatico = net.addPlace("Sintomatico");
        Place p13 = net.addPlace("p13");
        Place p14 = net.addPlace("p14");
        Place p22 = net.addPlace("p22");
        Place p23 = net.addPlace("p23");
        Place p30 = net.addPlace("p30");
        Place p38 = net.addPlace("p38");
        Transition contact1 = net.addTransition("contact1");
        Transition effective = net.addTransition("effective");
        Transition t0 = net.addTransition("t0");
        Transition t1 = net.addTransition("t1");
        Transition t16 = net.addTransition("t16");
        Transition t24 = net.addTransition("t24");
        Transition t3 = net.addTransition("t3");
        Transition t32 = net.addTransition("t32");
        Transition t51 = net.addTransition("t51");
        Transition t52 = net.addTransition("t52");
        Transition t53 = net.addTransition("t53");
        Transition t54 = net.addTransition("t54");
        Transition uneffective = net.addTransition("uneffective");

        //Generating Connectors
        net.addPrecondition(p22, uneffective);
        net.addPostcondition(t52, Asintomatico);
        net.addPrecondition(ContagiatoSintomatico, t0);
        net.addPrecondition(Contagioso, t53);
        net.addPostcondition(t1, Contagioso);
        net.addPrecondition(p38, t51);
        net.addPrecondition(Sintomatico, t54);
        net.addPostcondition(t3, p38);
        net.addPostcondition(t3, p30);
        net.addPrecondition(p30, t1);
        net.addPrecondition(Contagio, t3);
        net.addPrecondition(p22, effective);
        net.addPrecondition(p13, contact1);
        net.addPrecondition(Contagioso, t54);
        net.addPostcondition(t0, Sintomatico);
        net.addPostcondition(t53, Guarito);
        net.addPostcondition(t51, ContagiatoSintomatico);
        net.addPostcondition(t54, Isolato);
        net.addPrecondition(p38, t52);
        net.addPrecondition(Asintomatico, t53);
        net.addPostcondition(effective, Contagio);
        net.addPostcondition(contact1, p22);
        net.addPrecondition(p14, t16);
        net.addPostcondition(uneffective, p14);
        net.addPostcondition(t32, Contagio);
        net.addPrecondition(p23, t32);
        net.addPostcondition(t16, p23);
        net.addPrecondition(p23, t24);

        //Generating Properties
        marking.setTokens(Asintomatico, 0);
        marking.setTokens(ContagiatoSintomatico, 0);
        marking.setTokens(Contagio, 0);
        marking.setTokens(Contagioso, 0);
        marking.setTokens(Guarito, 0);
        marking.setTokens(Isolato, 0);
        marking.setTokens(Sintomatico, 0);
        marking.setTokens(p13, 1);
        marking.setTokens(p14, 0);
        marking.setTokens(p22, 0);
        marking.setTokens(p23, 0);
        marking.setTokens(p30, 0);
        marking.setTokens(p38, 0);
        contact1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("8"), MarkingExpr.from("1", net)));
        contact1.addFeature(new Priority(0));
        effective.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weights[0]), net)));
        effective.addFeature(new Priority(0));
        List<GEN> t0_gens = new ArrayList<>();

        DBMZone t0_d_0 = new DBMZone(new Variable("x"));
        Expolynomial t0_e_0 = Expolynomial.fromString("x^1 * Exp[-0.24 x] + -24*Exp[-0.24 x]");
        //Normalization
        t0_e_0.multiply(new BigDecimal(18.677114570749897));
        t0_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("48"));
        t0_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-24"));
        GEN t0_gen_0 = new GEN(t0_d_0, t0_e_0);
        t0_gens.add(t0_gen_0);

        PartitionedGEN t0_pFunction = new PartitionedGEN(t0_gens);
        StochasticTransitionFeature t0_feature = StochasticTransitionFeature.of(t0_pFunction);
        t0.addFeature(t0_feature);

        List<GEN> t1_gens = new ArrayList<>();

        DBMZone t1_d_0 = new DBMZone(new Variable("x"));
        Expolynomial t1_e_0 = Expolynomial.fromString("48 * x^1 + -1* x^2 + -432 * x^0");
        //Normalization
        t1_e_0.multiply(new BigDecimal(4.3402777777777775E-4));
        t1_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("36"));
        t1_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-12"));
        GEN t1_gen_0 = new GEN(t1_d_0, t1_e_0);
        t1_gens.add(t1_gen_0);

        PartitionedGEN t1_pFunction = new PartitionedGEN(t1_gens);
        StochasticTransitionFeature t1_feature = StochasticTransitionFeature.of(t1_pFunction);
        t1.addFeature(t1_feature);

        t16.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("20"), MarkingExpr.from("1", net)));
        t16.addFeature(new Priority(0));
        t24.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weights[1]), net)));
        t24.addFeature(new Priority(0));
        t3.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t3.addFeature(new Priority(0));
        t32.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weights[1]), net)));
        t32.addFeature(new Priority(0));
        t51.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t51.addFeature(new Priority(0));
        t52.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t52.addFeature(new Priority(0));
        t53.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.040", net)));
        t54.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.1", net)));
        uneffective.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weights[0]), net)));
        uneffective.addFeature(new Priority(0));
    }

    @AfterAll
    static void clean(){
        ArrayList<HashMap<String, Object>> obs = observationDAO.getObservations("p1");
        for(int i = 0; i<obs.size();i++){
            ObservationDAOTest.deleteObservation(obs.get(i).get("id").toString());
        }
    }

}