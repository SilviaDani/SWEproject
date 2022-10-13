package com.sweproject.analysis;

import com.sweproject.dao.ObservationDAO;
import com.sweproject.dao.ObservationDAOTest;
import javafx.scene.chart.XYChart;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.models.stpn.TransientSolution;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Simulator {
    int samples = 144;
    int steps = 1;
    int days = 6;
    final int maxReps = 10000;

    private float evaluateExpolynomial(int lowerBound, int upperBound){
        Expolynomial expolynomial = Expolynomial.fromString("48 * x^1 + -1* x^2 + -432 * x^0");
        expolynomial.multiply(new BigDecimal(4.3402777777777775E-4));
        Variable x = new Variable("x");
        return Float.parseFloat(expolynomial.integrate(x, new OmegaBigDecimal(lowerBound), new OmegaBigDecimal(upperBound)).toString());
    }
    @Test
    void evaluateExponential(){
        float lambda = 0.1f;
        Random rand = new Random();
        System.out.println(Math.ceil(Math.log(1-rand.nextFloat())/(-lambda)));
    }

    @Test
    void simulateEnvironmentalContacts() throws Exception {
        //getting all the observations
        ObservationDAO observationDAO = new ObservationDAO();
        ArrayList<String> users = ObservationDAOTest.getAllUsers();
        HashMap<String, ArrayList<HashMap<String, Object>>> allObservations = new HashMap<>();
        ArrayList<HashMap<String,Object>> observationsOfAPerson = new ArrayList<>();
        for(String s : users) {
            observationsOfAPerson = observationDAO.getEnvironmentObservations(s);
            if(observationsOfAPerson.size()>0)
                allObservations.put(s, observationsOfAPerson);
        }
        for(String s : users) {
            System.out.println(allObservations.get(s));
        }
        //creating data structurs for analysis
        int[] result = new int[allObservations.size() * samples];
        int[] state = new int[allObservations.size()]; //0 -> sano, 1 -> infetto, 2 -> contagioso
        Stack<LocalDateTime>[] nextObs = new Stack[allObservations.size()];
        Stack<Float>[] risks = new Stack[allObservations.size()];
        Stack<LocalDateTime>[] nextObs_copy = new Stack[allObservations.size()];
        Stack<Float>[] risks_copy = new Stack[allObservations.size()];
        for(int i = 0; i<nextObs.length; i++) {
            nextObs[i] = new Stack<>();
            risks[i] = new Stack<>();
            nextObs_copy[i] = new Stack<>();
            risks_copy[i] = new Stack<>();
        }
        int j=0;
        for(String s : allObservations.keySet()){
            for(int i = allObservations.get(s).size()-1; i>=0; i--){
                nextObs[j].push((LocalDateTime) allObservations.get(s).get(i).get("start_date"));
                nextObs_copy[j].push((LocalDateTime) allObservations.get(s).get(i).get("start_date"));
                risks[j].push((Float) allObservations.get(s).get(i).get("risk_level"));
                risks_copy[j].push((Float) allObservations.get(s).get(i).get("risk_level"));
            }
            j++;
        }
        Random rnd = new Random();

        for(int k = 0; k<maxReps; k++) {
            Arrays.fill(state,0);
            for(int i = 0; i<allObservations.size(); i++){
                risks[i] = (Stack<Float>) risks_copy[i].clone();
                nextObs[i] = (Stack<LocalDateTime>) nextObs_copy[i].clone();
            }
            LocalDateTime now = LocalDateTime.now().minusDays(days);

            // System.out.println(Arrays.toString(risks_copy));
            for (int i = 0; i < samples; i++) {
                for (j = 0; j < allObservations.size(); j++) {
                    if (nextObs[j].size() > 0 && ChronoUnit.HOURS.between(now, nextObs[j].peek()) < 1) {
                        nextObs[j].pop();
                        float risk = risks[j].pop();
                        if (state[j]==0 && rnd.nextFloat() < risk) { //allora si contagia
                                state[j] = 1;
                                nextObs[j].push(LocalDateTime.from(now).plusHours(12));
                                risks[j].push(12.f);
                                //skip = true;
                        } else if (state[j] == 1) {
                            if(rnd.nextFloat() < evaluateExpolynomial(12, (int)risk)){
                                state[j] = 2;
                                double min = Math.min((Math.log(1-rnd.nextFloat())/(-0.1)), Math.log(1-rnd.nextFloat())/(-0.04));
                                nextObs[j].push(LocalDateTime.from(now).plusHours((long)min));
                                risks[j].push(0.f);
                            }else{
                                nextObs[j].push(LocalDateTime.from(now).plusHours(1));
                                risks[j].push(risk+1);
                            }
                        } else if(state[j]==2){
                            state[j]=0;
                        }
                    }
                    result[i * allObservations.size() + j] += state[j] != 2 ? 0 : 1;
                }
                now = now.plusHours(1);
            }
        }
        double[] mean = new double[result.length];
        for(int i = 0; i<mean.length;i++)
            mean[i] = ((double) result[i])/((double)maxReps);

        System.out.println(Arrays.toString(mean));

        //STPN
        STPNAnalyzer analyzer = new STPNAnalyzer(samples, steps);
        j=0;
        for(String s : allObservations.keySet()){
            TransientSolution solution = analyzer.makeModel(s);
            int r = solution.getRegenerations().indexOf(solution.getInitialRegeneration());
            for(int m=0; m<solution.getColumnStates().size(); m++){
                for(int i=0, size = solution.getSamplesNumber()-1; i<size; i++){
                    //assertEquals(mean[j+i*allObservations.size()], solution.getSolution()[i+1][r][m], 0.1);
                    System.out.println((i)+". "+mean[i*allObservations.size()+j] + " - " + solution.getSolution()[i+1][r][m]);
                }
            }
            j++;
        }

    }
    private ArrayList<LocalDateTime> findObservationsThatHappensNow(LocalDateTime now){
        ArrayList<LocalDateTime> obs = new ArrayList<>();
        return obs;
    }

}
