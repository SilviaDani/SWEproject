package com.sweproject.analysis;

import com.github.sh0nk.matplotlib4j.NumpyUtils;
import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonConfig;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.google.common.base.Stopwatch;
import com.opencsv.CSVWriter;
import com.sweproject.gateway.ObservationGateway;
import com.sweproject.gateway.ObservationGatewayTest;
import com.sweproject.model.Contact;
import com.sweproject.model.Environment;
import com.sweproject.model.Type;
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
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static com.sweproject.main.Main.DEBUG;
import static org.junit.jupiter.api.Assertions.*;

class STPNAnalyzerTest {
    private static ObservationGateway observationGateway;
    private STPNAnalyzer stpnAnalyzer;
    private STPNAnalyzer_ext stpnAnalyzer_ext;
    int samples = 144;
    static int np = 6;
    Simulator sim = new Simulator();

    STPNAnalyzerTest(){
        observationGateway = new ObservationGateway();
        stpnAnalyzer = new STPNAnalyzer(samples, 1);
        stpnAnalyzer_ext = new STPNAnalyzer_ext(samples, 1);

    }
    @Test
    void contactWithEnvironment() throws Exception {
        //inserisco nel db le osservazioni che mi servono
        //8,20
        //3,2
        ArrayList<String> subjects = new ArrayList<>();
        subjects.add("p1");
        LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6);
        Type[] tt = new Environment[6];
        LocalDateTime[] startDates = new LocalDateTime[]{t0.plusHours(8), t0.plusHours(28), t0.plusHours(32), t0.plusHours(40), t0.plusHours(64), t0.plusHours(70)};
        LocalDateTime[] endDates = new LocalDateTime[]{t0.plusHours(8+3), t0.plusHours(28+2), t0.plusHours(32+5), t0.plusHours(40+5), t0.plusHours(64+2), t0.plusHours(70+6)};
        String[] riskLevels = new String[]{"Medium", "Low", "High", "Low", "Low", "Medium"};
        boolean[] masks = new boolean[]{false, false, true, false, true, true};
        float[] risks = new float[6];
        for(int i = 0; i<6; i++) {
            tt[i] = new Environment(masks[i], riskLevels[i], startDates[i], endDates[i]);
            risks[i] = BigDecimal.valueOf(((Environment) tt[i]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
            observationGateway.insertObservation(subjects, tt[i], startDates[i], endDates[i]);
        }
        //creo il modello e lo analizzo
        ArrayList<HashMap<String, Object>> arrayList = observationGateway.getEnvironmentObservations(subjects.get(0), samples);
        TransientSolution solution = stpnAnalyzer.makeModel(subjects.get(0), arrayList);
        //confronto punto punto il modello con una rete creata a parte (passandogli i pesi giusti)
        PetriNet pn = new PetriNet();
        Marking marking = new Marking();
        buildContactWithEnvironmentNetxd(pn, marking, risks);
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
                assertEquals(rewardedSolution.getSolution()[i][r][m], solution.getSolution()[i][r][m], 0.001);
            }
        }
       /* new TransientSolutionViewer(solution);
       new TransientSolutionViewer(rewardedSolution);
       int i =0;
       while(true){
           i = 0;
       }*/
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
    private void buildContactWithEnvironmentNetxd(PetriNet net, Marking marking, float[] weights) {
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
        Place p15 = net.addPlace("p15");
        Place p16 = net.addPlace("p16");
        Place p17 = net.addPlace("p17");
        Place p18 = net.addPlace("p18");
        Place p22 = net.addPlace("p22");
        Place p23 = net.addPlace("p23");
        Place p24 = net.addPlace("p24");
        Place p25 = net.addPlace("p25");
        Place p26 = net.addPlace("p26");
        Place p27 = net.addPlace("p27");
        Place p30 = net.addPlace("p30");
        Place p38 = net.addPlace("p38");
        Transition contact1 = net.addTransition("contact1");
        Transition effective = net.addTransition("effective");
        Transition t0 = net.addTransition("t0");
        Transition t1 = net.addTransition("t1");
        Transition t16 = net.addTransition("t16");
        Transition t17 = net.addTransition("t17");
        Transition t18 = net.addTransition("t18");
        Transition t19 = net.addTransition("t19");
        Transition t20 = net.addTransition("t20");
        Transition t24 = net.addTransition("t24");
        Transition t25 = net.addTransition("t25");
        Transition t26 = net.addTransition("t26");
        Transition t27 = net.addTransition("t27");
        Transition t28 = net.addTransition("t28");
        Transition t3 = net.addTransition("t3");
        Transition t32 = net.addTransition("t32");
        Transition t33 = net.addTransition("t33");
        Transition t34 = net.addTransition("t34");
        Transition t35 = net.addTransition("t35");
        Transition t36 = net.addTransition("t36");
        Transition t51 = net.addTransition("t51");
        Transition t52 = net.addTransition("t52");
        Transition t53 = net.addTransition("t53");
        Transition t54 = net.addTransition("t54");
        Transition uneffective = net.addTransition("uneffective");

        //Generating Connectors
        net.addPostcondition(t33, Contagio);
        net.addPrecondition(p22, uneffective);
        net.addPostcondition(t52, Asintomatico);
        net.addPostcondition(t35, Contagio);
        net.addPrecondition(p27, t28);
        net.addPrecondition(p26, t35);
        net.addPrecondition(ContagiatoSintomatico, t0);
        net.addPostcondition(t32, Contagio);
        net.addPostcondition(t36, Contagio);
        net.addPrecondition(p27, t36);
        net.addPrecondition(Contagioso, t53);
        net.addPostcondition(t1, Contagioso);
        net.addPrecondition(p23, t32);
        net.addPrecondition(p24, t25);
        net.addPrecondition(p38, t51);
        net.addPrecondition(Sintomatico, t54);
        net.addPostcondition(t3, p38);
        net.addPostcondition(t3, p30);
        net.addPostcondition(t19, p26);
        net.addPostcondition(t24, p15);
        net.addPrecondition(p26, t27);
        net.addPrecondition(p30, t1);
        net.addPostcondition(uneffective, p14);
        net.addPrecondition(Contagio, t3);
        net.addPrecondition(p22, effective);
        net.addPrecondition(p13, contact1);
        net.addPrecondition(Contagioso, t54);
        net.addPostcondition(t0, Sintomatico);
        net.addPostcondition(t17, p24);
        net.addPostcondition(t34, Contagio);
        net.addPostcondition(t16, p23);
        net.addPostcondition(t27, p18);
        net.addPrecondition(p15, t17);
        net.addPrecondition(p18, t20);
        net.addPrecondition(p17, t19);
        net.addPostcondition(t26, p17);
        net.addPostcondition(t53, Guarito);
        net.addPrecondition(p16, t18);
        net.addPostcondition(t51, ContagiatoSintomatico);
        net.addPrecondition(p25, t26);
        net.addPrecondition(p23, t24);
        net.addPostcondition(t54, Isolato);
        net.addPostcondition(t20, p27);
        net.addPrecondition(p24, t33);
        net.addPrecondition(p14, t16);
        net.addPrecondition(p25, t34);
        net.addPrecondition(p38, t52);
        net.addPrecondition(Asintomatico, t53);
        net.addPostcondition(t18, p25);
        net.addPostcondition(effective, Contagio);
        net.addPostcondition(t25, p16);
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
        marking.setTokens(p14, 0);
        marking.setTokens(p15, 0);
        marking.setTokens(p16, 0);
        marking.setTokens(p17, 0);
        marking.setTokens(p18, 0);
        marking.setTokens(p22, 0);
        marking.setTokens(p23, 0);
        marking.setTokens(p24, 0);
        marking.setTokens(p25, 0);
        marking.setTokens(p26, 0);
        marking.setTokens(p27, 0);
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
        t17.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("4"), MarkingExpr.from("1", net)));
        t17.addFeature(new Priority(0));
        t18.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("8"), MarkingExpr.from("1", net)));
        t18.addFeature(new Priority(0));
        t19.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("24"), MarkingExpr.from("1", net)));
        t19.addFeature(new Priority(0));
        t20.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("6"), MarkingExpr.from("1", net)));
        t20.addFeature(new Priority(0));
        t24.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weights[1]), net)));
        t24.addFeature(new Priority(0));
        t25.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weights[2]), net)));
        t25.addFeature(new Priority(0));
        t26.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weights[3]), net)));
        t26.addFeature(new Priority(0));
        t27.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weights[4]), net)));
        t27.addFeature(new Priority(0));
        t28.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-weights[5]), net)));
        t28.addFeature(new Priority(0));
        t3.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t3.addFeature(new Priority(0));
        t32.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weights[1]), net)));
        t32.addFeature(new Priority(0));
        t33.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weights[2]), net)));
        t33.addFeature(new Priority(0));
        t34.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weights[3]), net)));
        t34.addFeature(new Priority(0));
        t35.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weights[4]), net)));
        t35.addFeature(new Priority(0));
        t36.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(weights[5]), net)));
        t36.addFeature(new Priority(0));
        t51.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t51.addFeature(new Priority(0));
        t52.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t52.addFeature(new Priority(0));
        t53.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.040", net)));
        t54.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.1", net)));
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

    @Test
    void checkBetweenTheTwoMethods() throws PythonExecutionException, IOException {

        Random r = new Random();
        r.setSeed(11);
        int nContact = 25;
        int max_nEnvironment = 30;
        int min_nEnvironment = 20;
        LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusHours(samples);
        ArrayList<Subject> subjects = new ArrayList<>();

        //creazione dei soggetti
        for(int p = 0; p < np; p++){
            subjects.add(new Subject("P" + p));
        }
        //creazione degli eventi ambientali
        for(int p = 0; p < np; p++){
            int nEnvironment = r.nextInt(max_nEnvironment - min_nEnvironment) + min_nEnvironment;
            LocalDateTime[] startDates = new LocalDateTime[nEnvironment];
            LocalDateTime[] endDates = new LocalDateTime[nEnvironment];
            ArrayList<LocalDateTime> dates = new ArrayList<>(sim.generateDates(t0, nEnvironment, samples));

            int start = 0;
            int end = 0;
            for (int date = 0; date < dates.size(); date++) {
                if (date % 2 == 0) {
                    startDates[start] = dates.get(date);
                    start++;
                } else {
                    endDates[end] = dates.get(date);
                    end++;
                }
            }

            String[] riskLevels = sim.generateRiskLevels(nEnvironment);
            Boolean[] masks = sim.generateMasks(nEnvironment);

            for(int i = 0; i < nEnvironment; i++){
                ArrayList<Subject> s = new ArrayList<Subject>(Collections.singletonList(subjects.get(p)));
                ArrayList<String> s_String = new ArrayList<>();
                for(Subject sub : s){
                    s_String.add(sub.getName());
                }
                Type t = new Environment(masks[i], riskLevels[i], startDates[i], endDates[i]);
                observationGateway.insertObservation(s_String, t, startDates[i], endDates[i]);
            }
        }

        String[] nc_riskLevels;
        Boolean[] nc_masks;
        LocalDateTime[] nc_startDates = new LocalDateTime[nContact];
        LocalDateTime[] nc_endDates = new LocalDateTime[nContact];
        ArrayList<LocalDateTime> dates = new ArrayList<>(sim.generateDates(t0, nContact, samples));
        int start = 0;
        int end = 0;
        for (int date = 0; date < dates.size(); date++) {
            if (date % 2 == 0) {
                nc_startDates[start] = dates.get(date);
                start++;
            } else {
                nc_endDates[end] = dates.get(date);
                end++;
            }
        }
        nc_riskLevels = sim.generateRiskLevels(nContact);
        nc_masks = sim.generateMasks(nContact);

        for(int c = 0; c < nContact; c++){
            ArrayList<String> s_String = new ArrayList<>();
            ArrayList<Subject> subjects_copy = new ArrayList<>(subjects);
            ArrayList<Subject> partecipatingSubjects = new ArrayList<>();
            Collections.shuffle(subjects_copy);
            int upperBound = subjects_copy.size() > 2 ? r.nextInt(subjects_copy.size() - 2) + 2 : 2;
            for(int i = 0; i<upperBound; i++) {
                s_String.add(subjects_copy.get(i).getName());
                partecipatingSubjects.add(subjects_copy.get(i));
            }
            Type t = new Contact(s_String, nc_masks[c], nc_riskLevels[c], nc_startDates[c], nc_endDates[c]);
            observationGateway.insertObservation(s_String, t, nc_startDates[c], nc_endDates[c]);
        }

        final int max_iterations = subjects.size()<=2?subjects.size()-1:1;
        HashMap<String, ArrayList<HashMap<String, Object>>> clusterSubjectsMet = new HashMap<>();
        ArrayList<String> subjects_String = new ArrayList<>();

        HashMap<String, ArrayList<HashMap<String, Object>>> envObs = new HashMap<>();
        HashMap<String, ArrayList<HashMap<String, Object>>> testObs = new HashMap<>();
        HashMap<String, ArrayList<HashMap<String, Object>>> sympObs = new HashMap<>();
        sim.retrieveObservations(subjects, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);

        for(String s : clusterSubjectsMet.keySet()) {
            assertEquals(0, testObs.get(s).size());
            assertEquals(0, sympObs.get(s).size());
            assertTrue(0 <= envObs.get(s).size());
            assertTrue(0 <= clusterSubjectsMet.get(s).size());
        }
        ArrayList<HashMap<String, HashMap<Integer, Double>>> pns_ext = new ArrayList<>();

        Stopwatch timerExt = Stopwatch.createUnstarted();
        Stopwatch timerOr = Stopwatch.createUnstarted();

        timerExt.start();
        timerOr.start();

        ArrayList<HashMap<String, HashMap<Integer, Double>>> pns_or = new ArrayList<>();
        HashMap<String, TransientSolution> subject_ss = new HashMap<>();
        for(int nIteration = 0; nIteration<= max_iterations; nIteration++){
            HashMap<String, HashMap<Integer, Double>> pits = new HashMap<>();//p^it_s
            TransientSolution sol = null;
            HashMap<String, HashMap<Integer, Double>> hm = new HashMap<>();
            HashMap<String, TransientSolution> new_subject_ss = new HashMap<>();
            for(String member : subjects_String){
                if(nIteration==0){
                    try {
                        timerOr.stop();
                        TransientSolution s = stpnAnalyzer_ext.makeModel(envObs.get(member), testObs.get(member), sympObs.get(member));
                        pits.put(member, stpnAnalyzer_ext.computeAnalysis(s, envObs.get(member), t0));
                        timerExt.stop();
                        timerOr.start();
                        sol = stpnAnalyzer.makeModel(member, envObs.get(member));
                        new_subject_ss.put(member, sol);
                        timerExt.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    try {
                        timerOr.stop();
                        pits.put(member, stpnAnalyzer_ext.makeClusterModel(t0, pns_ext.get(nIteration - 1), clusterSubjectsMet.get(member), testObs.get(member), sympObs.get(member), member));
                        timerExt.stop();
                        timerOr.start();
                        sol = stpnAnalyzer.makeClusterModel(subject_ss, clusterSubjectsMet.get(member));
                        new_subject_ss.put(member, sol);
                        timerExt.start();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                timerExt.stop();
                HashMap<Integer, Double> values = new HashMap<>();
                for(int i = 0; i < samples; i++){
                    values.put(i, sol.getSolution()[i][0][0]);
                }
                hm.put(member, values);
                timerExt.start();
            }
            timerExt.stop();
            subject_ss = new_subject_ss;
            pns_or.add(hm);
            timerOr.stop();
            timerExt.start();
            pns_ext.add(pits);
            timerOr.start();
        }
        timerOr.stop();
        HashMap<String, HashMap<Integer, Double>> solutions = sim.buildSolution(pns_ext, subjects_String, samples, 1);
        timerExt.stop();
        timerOr.start();
        HashMap<String, HashMap<Integer, Double>> solutions_or = sim.buildSolution(pns_or, subjects_String, samples, 1);
        timerOr.stop();
        System.out.println("T or: "+timerOr.elapsed().toNanos()/10e9+" ns\nTime ext: "+timerExt.elapsed().toNanos()/10e9+" ns");
        for(String s : subjects_String){
            for(int i : solutions.get(s).keySet()){
                assertEquals(solutions_or.get(s).get(i), solutions.get(s).get(i), 10e-6);
                //System.out.println(i+" "+solutions_or.get(s).get(i) + " N:" + solutions.get(s).get(i));
            }
        }
        plot(solutions_or, solutions);



    }


    void plot(HashMap<String, HashMap<Integer, Double>> tt, HashMap<String, HashMap<Integer, Double>> ss) throws PythonExecutionException, IOException {
        boolean alternativePlot = false;
        String[] codes = new String[tt.size()];
        int index = 0;
        for(Object o : tt.keySet()){
            codes[index] = (String) o;
            index++;
        }
        List<Double> x = NumpyUtils.linspace(0, samples, samples);
        Plot plt = Plot.create(PythonConfig.pythonBinPathConfig(sim.PYTHON_PATH));
        String[] finalCodes = codes;
        HashMap<String, List<Double>> hPN = new HashMap<>();
        double max = 0;
        for(int j = 0; j<codes.length; j++) {
            int finalJ = j;
            List<Double> yPN;
            Double[] yPNarray = new Double[samples];
            List<Double> yPN1;
            Double[] yPNarray1 = new Double[samples];
            IntStream.range(0, samples).parallel().forEach(i->{
                if(alternativePlot) {
                    yPNarray[samples - i - 1] = tt.get(codes[finalJ]).get(i);
                    yPNarray1[samples - i - 1] = ss.get(codes[finalJ]).get(i);
                }
                else {
                    yPNarray[i] = ss.get(codes[finalJ]).get(i);
                    yPNarray1[i] = ss.get(codes[finalJ]).get(i);
                }
            });
            yPN = Arrays.stream(yPNarray).toList();
            yPN1 = Arrays.stream(yPNarray1).toList();
            hPN.put(codes[j], yPN1);
            String style = "solid";
            if(j>=5)
                style = "dashed";
            plt.plot().add(x, yPN).label("Old Net "+codes[j]).linestyle(style);
            plt.plot().add(x, yPN1).label("New Net "+codes[j]).linestyle(style);
            System.out.println(Collections.max(yPN1) + " " + Collections.max(yPN) + " " + Math.max(Collections.max(yPN1), Collections.max(yPN)));
            max = Math.max(max, Math.max(Collections.max(yPN1), Collections.max(yPN)));
            plt.legend();
        }
        plt.xlim(Collections.min(x) * 1.05, Collections.max(x) * 1.05);
        plt.ylim(- max * 0.1, max * 1.1);
        if(alternativePlot)
            plt.xlabel("Ore fa");
        else
            plt.xlabel("Ore");
        plt.ylabel("Rischio");
        plt.show();
    }


    @AfterAll
    static void clean(){
        for(int p=0; p<np; p++) {
            ArrayList<HashMap<String, Object>> obs = observationGateway.getObservations("P"+p);
            for (int i = 0; i < obs.size(); i++) {
                ObservationGatewayTest.deleteObservation(obs.get(i).get("id").toString());
            }
        }
    }

}