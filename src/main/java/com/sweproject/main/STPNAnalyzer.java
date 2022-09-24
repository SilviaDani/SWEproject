package com.sweproject.main;

import com.sweproject.dao.ObservationDAO;
import javafx.scene.chart.XYChart;
import org.checkerframework.checker.units.qual.A;
import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedGEN;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.RewardRate;
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

import static org.oristool.models.stpn.TransientSolutionViewer.solutionChart;

public class STPNAnalyzer<R,S> {
    private ObservationDAO observationDAO;
    private ArrayList<String> alreadyAnalizedCodes = new ArrayList<>();

    public STPNAnalyzer() {
        this.observationDAO = new ObservationDAO();
    }

    private boolean haveAlreadyBeenAnalized(String FC){
        for(String code : alreadyAnalizedCodes){
            if(code.equals(FC))
                return true;
        }
        alreadyAnalizedCodes.add(FC);
        return false;
    }

    public TransientSolution<R,S> makeModel (String fiscalCode){
        //retrieving data from DB
        ArrayList<HashMap<String, Object>> arrayList = observationDAO.getEnvironmentObservation(fiscalCode);
        if (arrayList.size() > 0){
            PetriNet pn = new PetriNet();
            //creating the central node
            Place contagious = pn.addPlace("Contagio");
            Marking m = new Marking();
            buildContagionEvolutionSection(pn,m,contagious);
            //making first module
            Place initialCondition = pn.addPlace("Condizione_iniziale");
            m.addTokens(initialCondition, 1);
            Place firstContact = pn.addPlace("Contatto_1");
            Transition t0 = pn.addTransition("t0");
            pn.addPrecondition(initialCondition, t0);
            pn.addPostcondition(t0, firstContact);
            double delta = ChronoUnit.MINUTES.between(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6), (LocalDateTime)arrayList.get(0).get("start_date"))/60;
            System.out.println(delta);
            t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(String.valueOf(delta)));
            Transition effective0 = pn.addTransition("Efficace_0");
            pn.addPrecondition(firstContact, effective0);
            pn.addPostcondition(effective0, contagious);
            effective0.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0),  MarkingExpr.from(arrayList.get(0).get("risk_level").toString(), pn)));
            Place lastPlace = firstContact;
            for(int i =0; i<arrayList.size();i++){
                System.out.println(arrayList.get(i).get("start_date"));
            }
            for(int i = 1; i<arrayList.size()-1; i++){
                Place iCondition = pn.addPlace("Condizione_"+i);
                Transition uneffectiveLast = pn.addTransition("Non-efficace"+ i);
                pn.addPrecondition(lastPlace, uneffectiveLast);
                pn.addPostcondition(uneffectiveLast, iCondition);
                uneffectiveLast.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0),  MarkingExpr.from(String.valueOf((1-(float)(arrayList.get(i-1).get("risk_level")))), pn)));
                Place iContact = pn.addPlace("Contatto_"+i);
                Transition ti = pn.addTransition("t"+i);
                pn.addPrecondition(iCondition, ti);
                pn.addPostcondition(ti,iContact);
                delta = ChronoUnit.MINUTES.between((LocalDateTime)arrayList.get(i-1).get("start_date"),(LocalDateTime)arrayList.get(i).get("start_date"))/60;
                System.out.println(i+" "+delta);
                ti.addFeature(StochasticTransitionFeature.newDeterministicInstance(String.valueOf(delta)));
                Transition effectiveLast = pn.addTransition("Efficace_"+i);
                pn.addPrecondition(iContact, effectiveLast);
                pn.addPostcondition(effectiveLast, contagious);
                effectiveLast.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0),  MarkingExpr.from(String.valueOf((float)(arrayList.get(i).get("risk_level"))), pn)));
                lastPlace = iContact;
            }
            //making the last module
            if(arrayList.size() > 1){
                int index = arrayList.size();
                Place finalCondition = pn.addPlace("Condizione_"+index);
                Transition uneffectiveLast1 = pn.addTransition("Non-efficace"+ (index - 1));
                pn.addPrecondition(lastPlace, uneffectiveLast1);
                pn.addPostcondition(uneffectiveLast1, finalCondition);
                uneffectiveLast1.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0),  MarkingExpr.from(String.valueOf((1-(float)(arrayList.get(index-2).get("risk_level")))), pn)));
                Place lastContact = pn.addPlace("Contatto_"+index);
                Transition tFinal = pn.addTransition("t"+index);
                pn.addPrecondition(finalCondition, tFinal);
                pn.addPostcondition(tFinal,lastContact);
                delta = ChronoUnit.MINUTES.between((LocalDateTime)arrayList.get(index-2).get("start_date"),(LocalDateTime)arrayList.get(index-1).get("start_date"))/60;
                tFinal.addFeature(StochasticTransitionFeature.newDeterministicInstance(String.valueOf(delta)));
                Transition effectiveLast = pn.addTransition("Efficace_"+index);
                pn.addPrecondition(lastContact, effectiveLast);
                pn.addPostcondition(effectiveLast, contagious);
                effectiveLast.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0),  MarkingExpr.from(String.valueOf(((float)(arrayList.get(index-1).get("risk_level")))), pn)));
                Transition drop = pn.addTransition("Non-efficace"+index);
                pn.addPrecondition(lastContact, drop);
                drop.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.valueOf(0),  MarkingExpr.from(String.valueOf((1-(float)(arrayList.get(index-1).get("risk_level")))), pn)));
            }

            // 144 -> 6 giorni
            RegTransient analysis = RegTransient.builder()
                    .greedyPolicy(new BigDecimal("200"), new BigDecimal("0.005"))
                    .timeStep(new BigDecimal("2")).build();

            var rewardRates = TransientSolution.rewardRates("If(Contagioso>0&&Sintomatico==0,1,0)");

            TransientSolution<DeterministicEnablingState, Marking> solution =
                    analysis.compute(pn, m);

            var rewardedSolution = TransientSolution.computeRewards(false, solution, rewardRates);

            return (TransientSolution<R, S>) rewardedSolution;
        }
        else {
            System.out.println("The subject has no Environment observations");
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
        System.out.println(series.getData().get(20));
        return series;
    }


    private void buildContagionEvolutionSection(PetriNet net, Marking marking, Place A_Contagio){
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
        t12.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.040", net)));
    }
}
