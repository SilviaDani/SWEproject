package com.sweproject.analysis;

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
import java.util.List;
import java.util.HashMap;

public class STPNAnalyzer<R,S> {
    private ObservationDAO observationDAO;
    private int step;
    private int samples;

    public STPNAnalyzer(int samples, int step) {
        this.observationDAO = new ObservationDAO();
        this.samples = samples;
        this.step = step;
    }

    public TransientSolution<R,S> makeModel(String fiscalCode) throws Exception {
        LocalDateTime now = LocalDateTime.now().minusDays(6);
        ArrayList<HashMap<String, Object>> arrayList = observationDAO.getEnvironmentObservations(fiscalCode);
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
            throw new Exception("No environmental observation are present on the database");
        }
    }

    //delete from here
    public TransientSolution<R, S> mm(String fiscalCode) {
        //retrieving data from DB
        LocalDateTime now = LocalDateTime.now();
        ArrayList<HashMap<String, Object>> arrayList = observationDAO.getEnvironmentObservations(fiscalCode);
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
            Transition t_0 = pn.addTransition("t_0");
            pn.addPrecondition(initialCondition, t_0);
            pn.addPostcondition(t_0, firstContact);
            double delta = ChronoUnit.MINUTES.between(now.truncatedTo(ChronoUnit.MINUTES).minusDays(6), (LocalDateTime) arrayList.get(0).get("start_date")) / 60;
            System.out.println("delta 0" + delta);
            t_0.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(delta), MarkingExpr.from("1", pn)));
            Transition effective0 = pn.addTransition("Efficace_0");
            pn.addPrecondition(firstContact, effective0);
            pn.addPostcondition(effective0, contagious);
            effective0.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(arrayList.get(0).get("risk_level").toString(), pn)));
            System.out.println("BBBBB" + arrayList.get(0).get("risk_level").toString());

            System.out.println("effective 0 : " + arrayList.get(0).get("risk_level"));

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
                Transition ti = pn.addTransition("t_" + i);
                pn.addPrecondition(iCondition, ti);
                pn.addPostcondition(ti, iContact);
                delta = ChronoUnit.MINUTES.between((LocalDateTime) arrayList.get(i - 1).get("start_date"), (LocalDateTime) arrayList.get(i).get("start_date")) / 60;
                System.out.println("delta"+ i + " " + delta);

                ti.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(delta), MarkingExpr.from("1", pn)));
                Transition effectiveLast = pn.addTransition("Efficace_" + i);
                pn.addPrecondition(iContact, effectiveLast);
                pn.addPostcondition(effectiveLast, contagious);
                effectiveLast.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0), MarkingExpr.from(String.valueOf((float) (arrayList.get(i).get("risk_level"))), pn)));

                System.out.println("effective " + i + " : "+ arrayList.get(i).get("risk_level").toString());

                lastPlace = iContact;
            }
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
    //to here

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

    public float getChancesOfHavingContagiousPersonInCluster(ArrayList<TransientSolution> ss, LocalDateTime meeting_time, int step, LocalDateTime now, float riskLevel){
        //TODO inserire quanto è stretto un contatto
        int delta = (int) ((ChronoUnit.MINUTES.between(now.truncatedTo(ChronoUnit.MINUTES).minusDays(6), meeting_time) / 60)/step);
        //System.out.println(delta);
        int r = ss.get(0).getRegenerations().indexOf(ss.get(0).getInitialRegeneration());
        float max = (float) ss.get(0).getSolution()[delta][r][0];
        for(int i=1; i<ss.size();i++){
            float currentValue = (float) ss.get(i).getSolution()[delta][r][0];
            if(currentValue > max)
                max = currentValue;
        }
        return max*riskLevel;
    }


    public void buildContagionEvolutionSection(PetriNet net, Marking marking, Place contagio){
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
        for(int i = 0; i<clusterSubjectsMet.size(); i++){
            for(String key : clusterSubjectsMet.get(i).keySet()){
                System.out.println("CSM ["+i+"]["+key+"]:"+clusterSubjectsMet.get(i).get(key));
            }
        }
        if (clusterSubjectsMet.size() > 0) {
            LocalDateTime now = LocalDateTime.now();
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
                subjectsMet_ss.add(subjects_ss.get(meeting_subjects[k])); //XXX
            }
            float effectiveness = getChancesOfHavingContagiousPersonInCluster(subjectsMet_ss, meeting_time1, step, now, (float) clusterSubjectsMet.get(j-1).get("risk_level")); //fixme
            float delta = (float)ChronoUnit.MINUTES.between(now.minusDays(6),meeting_time1)/60.f;
            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(delta)));
            e0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(effectiveness), net)));
            u0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(1-effectiveness), net)));
            net.addPrecondition(p1,t0);
            net.addPostcondition(t0,p2);
            net.addPrecondition(p2, e0);
            net.addPostcondition(e0, Contagio);
            net.addPrecondition(p2, u0);
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

                Place p3 = net.addPlace("Dopo incontro "+p);
                Place p4 = net.addPlace("Incontro "+(p+1));
                Transition t1 = net.addTransition("t "+p), e1 = net.addTransition("effective "+p), u1 = net.addTransition("uneffective "+p);
                delta = (float) ChronoUnit.MINUTES.between(meeting_time1, meeting_time2) / 60.f;
                effectiveness = getChancesOfHavingContagiousPersonInCluster(subjectsMet_ss, meeting_time2, step, now, (float) clusterSubjectsMet.get(l-1).get("risk_level")); //fixme

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

            //TODO: add plots of other rewards and change title
            //If(Contagioso>0&&Sintomatico==0,1,0);Contagioso;Sintomatico;If(Guarito+Isolato>0,1,0)
            var rewardRates = TransientSolution.rewardRates("Contagioso");

            TransientSolution<DeterministicEnablingState, Marking> solution =
                    analysis.compute(net, marking);

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
                for(int j = 1; j<ss.size();j++){ //FIXME: j=1 -> j=0 se vogliamo tenere di conto anche l'ambiente
                    value += (float)ss.get(j).get(fiscalCode).getSolution()[i][r][m];
                }
                series.getData().add(new XYChart.Data((String.valueOf((int)(i * step))), value));
            }
        }
        return series;
    }
}