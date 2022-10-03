package com.sweproject.main;

import com.sweproject.dao.ObservationDAO;
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
import java.util.List;
import java.util.HashMap;

public class STPNAnalyzer<R,S> {
    private ObservationDAO observationDAO;
    private ArrayList<String> alreadyAnalizedCodes = new ArrayList<>();
    private int step;
    private int samples;

    public STPNAnalyzer(int samples, int step) {
        this.observationDAO = new ObservationDAO();
        this.samples = samples;
        this.step = step;
    }

    private boolean haveAlreadyBeenAnalized(String FC) {
        for (String code : alreadyAnalizedCodes) {
            if (code.equals(FC))
                return true;
        }
        alreadyAnalizedCodes.add(FC);
        return false;
    }

    public TransientSolution<R, S> makeModel(String fiscalCode) {
        //retrieving data from DB
        ArrayList<HashMap<String, Object>> arrayList = observationDAO.getEnvironmentObservation(fiscalCode);
        for(int i = 0; i<arrayList.size(); i++){
            System.out.println(arrayList.get(i).get("start_date"));
        }
        if (arrayList.size() > 0) {
            PetriNet pn = new PetriNet();
            //creating the central node
            Place contagious = pn.addPlace("Contagio");
            Marking m = new Marking();
            buildContagionEvolutionSection(pn, m, contagious);
            //making first module
            Place initialCondition = pn.addPlace("Condizione_iniziale");
            m.addTokens(initialCondition, 1);
            Place firstContact = pn.addPlace("Contatto_1");
            Transition t0 = pn.addTransition("t0");
            pn.addPrecondition(initialCondition, t0);
            pn.addPostcondition(t0, firstContact);
            double delta = ChronoUnit.MINUTES.between(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6), (LocalDateTime) arrayList.get(0).get("start_date")) / 60;
            System.out.println(delta);
            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(String.valueOf(delta)));
            Transition effective0 = pn.addTransition("Efficace_0");
            pn.addPrecondition(firstContact, effective0);
            pn.addPostcondition(effective0, contagious);
            effective0.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(arrayList.get(0).get("risk_level").toString(), pn)));

            System.out.println("effective 0 : "+arrayList.get(0).get("risk_level").toString());

            Place lastPlace = firstContact;
            //making intermediate modules
            for (int i = 1; i < arrayList.size(); i++) {
                Place iCondition = pn.addPlace("Condizione_" + i);
                Transition uneffectiveLast = pn.addTransition("Non-efficace" + i);
                pn.addPrecondition(lastPlace, uneffectiveLast);
                pn.addPostcondition(uneffectiveLast, iCondition);
                uneffectiveLast.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf((1 - (float) (arrayList.get(i - 1).get("risk_level")))), pn)));

                System.out.println("uneffective "+ (i-1) +" : "+ (1 - (float) (arrayList.get(i - 1).get("risk_level"))));

                Place iContact = pn.addPlace("Contatto_" + i);
                Transition ti = pn.addTransition("t" + i);
                pn.addPrecondition(iCondition, ti);
                pn.addPostcondition(ti, iContact);
                delta = ChronoUnit.MINUTES.between((LocalDateTime) arrayList.get(i - 1).get("start_date"), (LocalDateTime) arrayList.get(i).get("start_date")) / 60;

                System.out.println( delta);

                ti.addFeature(StochasticTransitionFeature.newDeterministicInstance(String.valueOf(delta)));
                Transition effectiveLast = pn.addTransition("Efficace_" + i);
                pn.addPrecondition(iContact, effectiveLast);
                pn.addPostcondition(effectiveLast, contagious);
                effectiveLast.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf((float) (arrayList.get(i).get("risk_level"))), pn)));

                System.out.println("effective " + i + " : "+ arrayList.get(i).get("risk_level").toString());

                lastPlace = iContact;
            }
            //making the last module
            /*if (arrayList.size() > 1) {
                int index = arrayList.size();
                Place finalCondition = pn.addPlace("Condizione_" + index);
                Transition uneffectiveLast1 = pn.addTransition("Non-efficace" + (index - 1));
                pn.addPrecondition(lastPlace, uneffectiveLast1);
                pn.addPostcondition(uneffectiveLast1, finalCondition);
                uneffectiveLast1.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf((1 - (float) (arrayList.get(index - 2).get("risk_level")))), pn)));
                Place lastContact = pn.addPlace("Contatto_" + index);
                Transition tFinal = pn.addTransition("t" + index);
                pn.addPrecondition(finalCondition, tFinal);
                pn.addPostcondition(tFinal, lastContact);
                delta = ChronoUnit.MINUTES.between((LocalDateTime) arrayList.get(index - 2).get("start_date"), (LocalDateTime) arrayList.get(index - 1).get("start_date")) / 60;
                tFinal.addFeature(StochasticTransitionFeature.newDeterministicInstance(String.valueOf(delta)));
                Transition effectiveLast = pn.addTransition("Efficace_" + index);
                pn.addPrecondition(lastContact, effectiveLast);
                pn.addPostcondition(effectiveLast, contagious);
                effectiveLast.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf(((float) (arrayList.get(index - 1).get("risk_level")))), pn)));
                Transition drop = pn.addTransition("Non-efficace" + index);
                pn.addPrecondition(lastContact, drop);
                drop.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf((1 - (float) (arrayList.get(index - 1).get("risk_level")))), pn)));
            }*/
            Transition drop = pn.addTransition("Drop");
            pn.addPrecondition(lastPlace, drop);
            drop.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf((1 - (float) (arrayList.get(arrayList.size() - 1).get("risk_level")))), pn)));

            System.out.println("uneffective (last) " + (1 - (float) (arrayList.get(arrayList.size() - 1).get("risk_level"))));



            // 144 -> 6 giorni
            RegTransient analysis = RegTransient.builder()
                    .greedyPolicy(new BigDecimal(samples), new BigDecimal("0.001"))
                    .timeStep(new BigDecimal(step)).build();

            //TODO: add plots of other rewards and change title
            //If(Contagioso>0&&Sintomatico==0,1,0);Contagioso;Sintomatico;If(Guarito+Isolato>0,1,0)
            var rewardRates = TransientSolution.rewardRates("Contagioso");

            TransientSolution<DeterministicEnablingState, Marking> solution =
                    analysis.compute(pn, m);

            var rewardedSolution = TransientSolution.computeRewards(false, solution, rewardRates);
            //new TransientSolutionViewer(rewardedSolution);
            return (TransientSolution<R, S>) rewardedSolution;
        } else {
            System.out.println("The subject has no Environment observations of the last 6 days");
            return null;
        }

    }

    public <S, R> XYChart.Series makeChart(TransientSolution<S, R> s ){
        XYChart.Series<String, Float> series = new XYChart.Series();
        int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
        for(int m=0; m<s.getColumnStates().size(); m++){
            double step = s.getStep().doubleValue();
            for(int i=0, size = s.getSamplesNumber(); i<size; i++){
                series.getData().add(new XYChart.Data((String.valueOf((int)(i * step))), (float)(s.getSolution()[i][r][m])));
            }
        }
        return series;
    }

    public float getChancesOfHavingContagiousPersonInCluster(ArrayList<TransientSolution> ss, LocalDateTime meeting_time, int step){
        //TODO inserire quanto è stretto un contatto
        int delta = (int) ((ChronoUnit.MINUTES.between(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6), meeting_time) / 60)/step);
        //System.out.println(delta);
        int r = ss.get(0).getRegenerations().indexOf(ss.get(0).getInitialRegeneration());
        float max = (float) ss.get(0).getSolution()[delta][r][0];
        for(int i=1; i<ss.size();i++){
            float currentValue = (float) ss.get(i).getSolution()[delta][r][0];
            if(currentValue > max)
                max = currentValue;
        }
        return max;
    }


    private void buildContagionEvolutionSection(PetriNet net, Marking marking, Place contagio){
        /*
           //Generating Node
            Place A_Asintomatico = net.addPlace("Asintomatico");
            Place A_ConSintomi = net.addPlace("ConSintomi");
            Place A_Contagio1 = net.addPlace("Contagio1");
            Place A_Contagio2 = net.addPlace("Contagio2");
            Place A_Contagioso = net.addPlace("Contagioso");
            Place A_Quarantenato = net.addPlace("Quarantenato");
            Place A_Sintomatico = net.addPlace("Sintomatico");
            Transition a_inContagioso = net.addTransition("inContagioso");
            Transition a_inSintomatico = net.addTransition("inSintomatico");
            Transition a_t0 = net.addTransition("_t0");
            Transition a_t1 = net.addTransition("_t1");
            Transition a_t2 = net.addTransition("_t2");
            Transition t12 = net.addTransition("_t12");
            //Generating Connctors
            net.addPostcondition(t12, A_Quarantenato);
            net.addPrecondition(A_Contagio, a_t2);
            net.addPrecondition(A_Contagio2, a_t0);
            net.addPrecondition(A_Contagio2, a_t1);
            net.addPostcondition(a_t2, A_Contagio2);
            net.addPrecondition(A_Asintomatico, t12);
            net.addPostcondition(a_t1, A_Asintomatico);
            net.addPostcondition(a_inContagioso, A_Contagioso);
            net.addPostcondition(a_inSintomatico, A_Sintomatico);
            net.addPrecondition(A_Contagio1, a_inContagioso);
            net.addPostcondition(a_t2, A_Contagio1);
            net.addPrecondition(A_ConSintomi, a_inSintomatico);
            net.addPostcondition(a_t0, A_ConSintomi);
            //Generating Proprties
            marking.setTokens(A_Asintomatico, 0);
            marking.setTokens(A_ConSintomi, 0);
            marking.setTokens(A_Contagio, 0);
            marking.setTokens(A_Contagio1, 0);
            marking.setTokens(A_Contagio2, 0);
            marking.setTokens(A_Contagioso, 0);
            marking.setTokens(A_Quarantenato, 0);
            marking.setTokens(A_Sintomatico, 0);
            List<GEN> a_inContagioso_gens = new ArrayList<>();
        DBMZone a_inContagioso_d_0 = new DBMZone(new Variable("x"));
        Expolynomial a_inContagioso_e_0 = Expolynomial.fromString("48 * x^1 + -1* x^2 + -432 * x^0");
        //Normalization
        a_inContagioso_e_0.multiply(new BigDecimal(4.3402777777777775E-4));
        a_inContagioso_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("36"));
        a_inContagioso_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-12"));
        GEN a_inContagioso_gen_0 = new GEN(a_inContagioso_d_0, a_inContagioso_e_0);
        a_inContagioso_gens.add(a_inContagioso_gen_0);

        PartitionedGEN a_inContagioso_pFunction = new PartitionedGEN(a_inContagioso_gens);
        StochasticTransitionFeature a_inContagioso_feature = StochasticTransitionFeature.of(a_inContagioso_pFunction);
        a_inContagioso.addFeature(a_inContagioso_feature);

        List<GEN> a_inSintomatico_gens = new ArrayList<>();

        DBMZone a_inSintomatico_d_0 = new DBMZone(new Variable("x"));
        Expolynomial a_inSintomatico_e_0 = Expolynomial.fromString("x^1 * Exp[-0.24 x] + -24*Exp[-0.24 x]");
        //Normalization
        a_inSintomatico_e_0.multiply(new BigDecimal(18.677114570749897));
        a_inSintomatico_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("48"));
        a_inSintomatico_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-24"));
        GEN a_inSintomatico_gen_0 = new GEN(a_inSintomatico_d_0, a_inSintomatico_e_0);
        a_inSintomatico_gens.add(a_inSintomatico_gen_0);

        PartitionedGEN a_inSintomatico_pFunction = new PartitionedGEN(a_inSintomatico_gens);
        StochasticTransitionFeature a_inSintomatico_feature = StochasticTransitionFeature.of(a_inSintomatico_pFunction);
        a_inSintomatico.addFeature(a_inSintomatico_feature);

        a_t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        a_t0.addFeature(new Priority(0));
        a_t1.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        a_t1.addFeature(new Priority(0));
        a_t2.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        a_t2.addFeature(new Priority(0));
        t12.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.040", net)));*/
        //Generating Nodes
        Place Asintomatico = net.addPlace("Asintomatico");
        Place ContagiatoSintomatico = net.addPlace("ContagiatoSintomatico");
        Place Contagioso = net.addPlace("Contagioso");
        Place Guarito = net.addPlace("Guarito");
        Place Isolato = net.addPlace("Isolato");
        Place Sintomatico = net.addPlace("Sintomatico");
        Place p30 = net.addPlace("p30");
        Place p38 = net.addPlace("p38");
        Transition t0 = net.addTransition("_t0");
        Transition t1 = net.addTransition("_t1");
        Transition t3 = net.addTransition("_t3");
        Transition t51 = net.addTransition("_t51");
        Transition t52 = net.addTransition("_t52");
        Transition t53 = net.addTransition("_t53");
        Transition t54 = net.addTransition("_t54");

        //Generating Connectors
        net.addPostcondition(t52, Asintomatico);
        net.addPrecondition(ContagiatoSintomatico, t0);
        net.addPrecondition(Contagioso, t53);
        net.addPostcondition(t1, Contagioso);
        net.addPrecondition(p38, t51);
        net.addPrecondition(Sintomatico, t54);
        net.addPostcondition(t3, p38);
        net.addPostcondition(t3, p30);
        net.addPrecondition(p30, t1);
        net.addPrecondition(contagio, t3);
        net.addPrecondition(Contagioso, t54);
        net.addPostcondition(t0, Sintomatico);
        net.addPostcondition(t53, Guarito);
        net.addPostcondition(t51, ContagiatoSintomatico);
        net.addPostcondition(t54, Isolato);
        net.addPrecondition(p38, t52);
        net.addPrecondition(Asintomatico, t53);

        //Generating Properties
        marking.setTokens(Asintomatico, 0);
        marking.setTokens(ContagiatoSintomatico, 0);
        marking.setTokens(contagio, 0);
        marking.setTokens(Contagioso, 0);
        marking.setTokens(Guarito, 0);
        marking.setTokens(Isolato, 0);
        marking.setTokens(Sintomatico, 0);
        marking.setTokens(p30, 0);
        marking.setTokens(p38, 0);
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
    }

    public TransientSolution<R, S> makeClusterModel(HashMap<String, TransientSolution> subjects_ss, ArrayList<HashMap<String, Object>> clusterSubjectsMet) {
        if (clusterSubjectsMet.size() > 0) {
            PetriNet pn = new PetriNet();
            //creating the central node
            Place contagious = pn.addPlace("Contagio");
            Marking m = new Marking();
            buildContagionEvolutionSection(pn, m, contagious);
            //making first module
            Place initialCondition = pn.addPlace("Condizione_iniziale");
            m.addTokens(initialCondition, 1);
            Place firstContact = pn.addPlace("Contatto_1");
            Transition t0 = pn.addTransition("t0");
            pn.addPrecondition(initialCondition, t0);
            pn.addPostcondition(t0, firstContact);
            double delta = ChronoUnit.MINUTES.between(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6), (LocalDateTime) clusterSubjectsMet.get(0).get("start_date")) / 60;
            //System.out.println(delta);
            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(String.valueOf(delta)));
            Transition effective0 = pn.addTransition("Efficace_0");
            pn.addPrecondition(firstContact, effective0);
            pn.addPostcondition(effective0, contagious);
            int i = 0; //index of contact
            int j = 0; //n. person met during contact counter
            String[] meeting_subjects = new String[clusterSubjectsMet.size()];
            LocalDateTime meeting_time1 = (LocalDateTime) clusterSubjectsMet.get(i).get("start_date");
            while (i < clusterSubjectsMet.size() && (LocalDateTime) clusterSubjectsMet.get(i).get("start_date") == meeting_time1) {
                meeting_subjects[j] = clusterSubjectsMet.get(i).get("fiscalCode").toString();
                j++;
                i++;
            }
            ArrayList<TransientSolution> subjectsMet_ss = new ArrayList<>();
            for (int k = 0; k < j; k++) {
                subjectsMet_ss.add(subjects_ss.get(meeting_subjects[k]));
            }
            float effectiveness = getChancesOfHavingContagiousPersonInCluster(subjectsMet_ss, meeting_time1, step); //fixme

            effective0.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf(effectiveness), pn)));
            Place lastPlace = firstContact;
            //making intermediate modules
            for (int l = i; l < clusterSubjectsMet.size(); l++) { //fixme
                j = 0;
                int p = 0; //index for transitions
                for (int n = 0; n < meeting_subjects.length; n++) {
                    meeting_subjects[n] = null;
                }
                LocalDateTime meeting_time2 = (LocalDateTime) clusterSubjectsMet.get(l).get("start_date");
                while (l < clusterSubjectsMet.size() && (LocalDateTime) clusterSubjectsMet.get(l).get("start_date") == meeting_time2) {
                    meeting_subjects[j] = clusterSubjectsMet.get(i).get("fiscalCode").toString();
                    j++;
                    l++;
                }
                subjectsMet_ss.clear();
                for (int k = 0; k < j; k++) {
                    subjectsMet_ss.add(subjects_ss.get(meeting_subjects[k]));
                }
                p++;

                Place iCondition = pn.addPlace("Condizione_" + p);
                Transition uneffectiveLast = pn.addTransition("Non-efficace" + p);
                pn.addPrecondition(lastPlace, uneffectiveLast);
                pn.addPostcondition(uneffectiveLast, iCondition);
                uneffectiveLast.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf(1 - effectiveness), pn)));
                effectiveness = getChancesOfHavingContagiousPersonInCluster(subjectsMet_ss, meeting_time2, step);
                Place iContact = pn.addPlace("Contatto_" + p);
                Transition ti = pn.addTransition("t" + p);
                pn.addPrecondition(iCondition, ti);
                pn.addPostcondition(ti, iContact);
                delta = ChronoUnit.MINUTES.between(meeting_time1 ,meeting_time2)/ 60;
                //System.out.println(delta);
                meeting_time1 = meeting_time2;
                ti.addFeature(StochasticTransitionFeature.newDeterministicInstance(String.valueOf(delta)));
                Transition effectiveLast = pn.addTransition("Efficace_" + p);
                pn.addPrecondition(iContact, effectiveLast);
                pn.addPostcondition(effectiveLast, contagious);
                effectiveLast.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf(effectiveness), pn)));
                lastPlace = iContact;
            }
            Transition drop = pn.addTransition("Drop");
            pn.addPrecondition(lastPlace, drop);
            drop.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf(1 - effectiveness), pn)));

            RegTransient analysis = RegTransient.builder()
                    .greedyPolicy(new BigDecimal(samples), new BigDecimal("0.005"))
                    .timeStep(new BigDecimal(step)).build();

            //TODO: add plots of other rewards and change title
            //If(Contagioso>0&&Sintomatico==0,1,0);Contagioso;Sintomatico;If(Guarito+Isolato>0,1,0)
            var rewardRates = TransientSolution.rewardRates("Contagioso");

            TransientSolution<DeterministicEnablingState, Marking> solution =
                    analysis.compute(pn, m);

            var rewardedSolution = TransientSolution.computeRewards(false, solution, rewardRates);
            //new TransientSolutionViewer(rewardedSolution);
            return (TransientSolution<R, S>) rewardedSolution;
        } else {
            System.out.println("The subject has no Environment observations of the last 6 days");
            return null;
            }
        }

    public XYChart.Series makeChart(ArrayList<HashMap<String, TransientSolution>> ss, String fiscalCode) {
        TransientSolution s = ss.get(0).get(fiscalCode);
        XYChart.Series<String, Float> series = new XYChart.Series();
        int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
        for(int m=0; m<s.getColumnStates().size(); m++){
            double step = s.getStep().doubleValue();
            for(int i=0, size = s.getSamplesNumber(); i<size; i++){
                float value = 0.f;
                for(int j = 0; j<ss.size();j++){
                    value += (float)ss.get(j).get(fiscalCode).getSolution()[i][r][m];
                }
                series.getData().add(new XYChart.Data((String.valueOf((int)(i * step))), value));
            }
        }
        return series;
    }
}
