package com.sweproject.analysis;

import com.sweproject.dao.ObservationDAO;
import com.sweproject.dao.ObservationDAOTest;
import com.sweproject.model.Contact;
import com.sweproject.model.Environment;
import com.sweproject.model.Type;
import org.junit.jupiter.api.Test;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.models.stpn.TransientSolution;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Simulator {
    private static ObservationDAO observationDAO;
    int samples = 144;
    int steps = 1;
    int days = 6;
    final int maxReps = 10000;
    HashMap<LocalDateTime, ArrayList<Integer>> p1perTime = new HashMap<LocalDateTime, ArrayList<Integer>>();
    HashMap<LocalDateTime, ArrayList<Integer>> p2perTime = new HashMap<LocalDateTime, ArrayList<Integer>>();

    @Test
    void start_simulation(){
        //Created Enviornment Contacts
        ArrayList<String> subjects = new ArrayList<>();
        subjects.add("p1");
        LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6);
        Type[] tt = new Environment[6];
        LocalDateTime[] startDates1 = new LocalDateTime[]{t0.plusHours(8), t0.plusHours(28), t0.plusHours(32), t0.plusHours(40), t0.plusHours(64), t0.plusHours(70)};
        LocalDateTime[] endDates1 = new LocalDateTime[]{t0.plusHours(8+3), t0.plusHours(28+2), t0.plusHours(32+5), t0.plusHours(40+5), t0.plusHours(64+2), t0.plusHours(70+6)};
        String[] riskLevels1 = new String[]{"Medium", "Low", "High", "Low", "Low", "Medium"};
        boolean[] masks = new boolean[]{false, false, true, false, true, true};
        float[] risks1 = new float[6];
        for(int i = 0; i<6; i++) {
            tt[i] = new Environment(masks[i], riskLevels1[i], startDates1[i], endDates1[i]);
            risks1[i] = BigDecimal.valueOf(((Environment) tt[i]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
            observationDAO.insertObservation(subjects, tt[i], startDates1[i], endDates1[i]);
        }

        subjects.clear();
        subjects.add("p2");
        tt = new Environment[4];
        LocalDateTime[] startDates2 = new LocalDateTime[]{t0.plusHours(3), t0.plusHours(43), t0.plusHours(100), t0.plusHours(136)};
        LocalDateTime[] endDates2 = new LocalDateTime[]{t0.plusHours(3+1), t0.plusHours(43).plusMinutes(30), t0.plusHours(100+1), t0.plusHours(136).plusMinutes(45)};
        String[] riskLevels2 = new String[]{"Medium", "High", "High", "Low"};
        boolean[] masks2 = new boolean[]{false, false, true, false};
        float[] risks2 = new float[4];
        for (int i = 0; i<4; i++) {
            tt[i] = new Environment(masks2[i], riskLevels2[i], startDates2[i], endDates2[i]);
            risks2[i] = BigDecimal.valueOf(((Environment) tt[i]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
            observationDAO.insertObservation(subjects, tt[i], startDates2[i], endDates2[i]);
        }

        //Create Cluster Contacts
        subjects.add("p1");
        Type [] mm = new Contact [3];
        LocalDateTime[] startDates = new LocalDateTime[]{t0.plusHours(38), t0.plusHours(78), t0.plusHours(100), t0.plusHours(136)};
        LocalDateTime[] endDates = new LocalDateTime[]{t0.plusHours(38+3), t0.plusHours(78+1).plusMinutes(30), t0.plusHours(98+2).plusMinutes(15)};
        String[] riskLevels = new String[]{"Medium", "High", "Low"};
        masks = new boolean[]{false, false, true};
        float[] risks = new float[3];
        for (int i = 0; i<3; i++) {
            mm[i] = new Contact(subjects, masks[i], riskLevels[i], startDates[i], endDates[i]);
            risks[i] = BigDecimal.valueOf(((Environment) tt[i]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
            observationDAO.insertObservation(subjects, mm[i], startDates[i], endDates[i]);
        }

        for (int i=0; i<maxReps; i++){
            int p1_environment = 0;
            int p2_environment = 0;
            int p1_p2 = 0;
            int[] lastStates = new int[]{0, 0}; //0 -> sano, 1 -> infetto, 2 -> contagioso, 3 -> guarito
            //Get next contact in timeline and its risk
            for(int contact = 0; contact<13; contact++){
                ArrayList <LocalDateTime> nextContactTime = new ArrayList<>();
                ArrayList<Float> nextRisk = new ArrayList<>();
                ArrayList<String> nextContact = new ArrayList<>();

                if(startDates1[p1_environment].isBefore(startDates2[p2_environment]) & startDates1[p1_environment].isBefore(startDates[p1_p2])) {
                    nextContactTime.add(startDates1[p1_environment]);
                    nextRisk.add(risks1[p1_environment]);
                    nextContact.add("p1");
                    p1_environment++;
                }
                else if(startDates2[p2_environment].isBefore(startDates1[p1_environment]) & startDates2[p2_environment].isBefore(startDates[p1_p2])){
                    nextContactTime.add(startDates2[p2_environment]);
                    nextRisk.add(risks2[p2_environment]);
                    nextContact.add("p2");
                    p2_environment++;
                }
                else if(startDates[p1_p2].isBefore(startDates1[p1_environment]) & startDates[p1_p2].isBefore(startDates2[p2_environment])){
                    nextContactTime.add(startDates[p1_p2]);
                    nextRisk.add(risks[p1_p2]);
                    nextContact.add("p1");
                    nextContact.add("p2");
                    p1_p2++;
                }
                else if(startDates1[p1_environment].isEqual(startDates2[p2_environment])){
                    nextContactTime.add(startDates1[p1_environment]);
                    nextContactTime.add(startDates2[p2_environment]);
                    nextRisk.add(risks1[p1_environment]);
                    nextRisk.add(risks2[p2_environment]);
                    nextContact.add("p1");
                    nextContact.add("p2");
                    p1_environment++;
                    p2_environment++;
                }

                while (nextContactTime.size() != 0){
                    String nc = nextContact.remove(0);
                    if (nc.equals("p1")){
                        if (lastStates[0] == 0){ //sano
                            Random r = new Random();
                            float random = 0 + r.nextFloat() * (1 - 0);
                            if (random<nextRisk.remove(0)) { //Da sano a contagiato
                                lastStates[0] += 1;
                                LocalDateTime timeContagious = getSampleCC(nextContactTime.get(0), 12, 36);
                                addToHashMap(p1perTime, nextContactTime.remove(0), lastStates[0]);
                                int position = 0;
                                for (int iterator=0; iterator<nextContactTime.size(); iterator++){
                                    if (nextContactTime.get(iterator).isBefore(timeContagious))
                                        position = iterator + 1;
                                }
                                nextContactTime.add(position, timeContagious);
                                nextContact.add(position, "p1");
                            }
                            else {//altrimenti resta sano
                                addToHashMap(p1perTime, nextContactTime.remove(0), lastStates[0]);
                            }
                        }
                        else if (lastStates[0] == 1){ //contagiato
                            lastStates[0] += 1; //da contagiato a contagioso
                            LocalDateTime timeHealing = getSampleCH(nextContactTime.get(0));
                            addToHashMap(p1perTime, nextContactTime.remove(0), lastStates[0]);
                            int position = 0;
                            for (int iterator=0; iterator<nextContactTime.size(); iterator++){
                                if (nextContactTime.get(iterator).isBefore(timeHealing))
                                    position = iterator + 1;
                            }
                            nextContactTime.add(position, timeHealing);
                            nextContact.add(position, "p1");

                        }
                        else if (lastStates[0] == 2){ //contagioso
                            lastStates[0] += 1; //da contagioso a guarito
                            addToHashMap(p1perTime, nextContactTime.remove(0), lastStates[0]);
                        }
                    }
                    else if (nc.equals("p2")){
                        if (lastStates[1] == 0){ //sano
                            Random r = new Random();
                            float random = 0 + r.nextFloat() * (1 - 0);
                            if (random<nextRisk.remove(0)) { //Da sano a contagiato
                                lastStates[1] += 1;
                                LocalDateTime timeContagious = getSampleCC(nextContactTime.get(0), 12, 36);
                                addToHashMap(p2perTime, nextContactTime.remove(0), lastStates[1]);
                                int position = 0;
                                for (int iterator=0; iterator<nextContactTime.size(); iterator++){
                                    if (nextContactTime.get(iterator).isBefore(timeContagious))
                                        position = iterator + 1;
                                }
                                nextContactTime.add(position, timeContagious);
                                nextContact.add(position, "p2");
                            }
                            else {//altrimenti resta sano
                                addToHashMap(p2perTime, nextContactTime.remove(0), lastStates[1]);
                            }
                        }
                        else if (lastStates[1] == 1){ //contagiato
                            lastStates[1] += 1; //da contagiato a contagioso
                            LocalDateTime timeHealing = getSampleCH(nextContactTime.get(0));
                            addToHashMap(p2perTime, nextContactTime.remove(0), lastStates[1]);
                            int position = 0;
                            for (int iterator=0; iterator<nextContactTime.size(); iterator++){
                                if (nextContactTime.get(iterator).isBefore(timeHealing))
                                    position = iterator + 1;
                            }
                            nextContactTime.add(position, timeHealing);
                            nextContact.add(position, "p2");

                        }
                        else if (lastStates[1] == 2){ //contagioso
                            lastStates[1] += 1; //da contagioso a guarito
                            addToHashMap(p2perTime, nextContactTime.remove(0), lastStates[1]);
                        }
                    }
                }
            }

        }
        //TODO CONTARE PER OGNI CHIAVE NELLE HASHMAP QUANTE VOLTE LO STATO è 2 su n
        //TODO plottare grafici di p1 (oris e simulazione)
    }

    private LocalDateTime getSampleCC(LocalDateTime date, int min, int max) {
        Random r = new Random();
        int randomHours = min + r.nextInt() * (max - 1 - min);
        int randomMinutes = 0 + r.nextInt() * (60 - 0);
        date.plusHours(randomHours).plusMinutes(randomMinutes);
        return date;
    }

    private LocalDateTime getSampleCH(LocalDateTime date) {
        Random r = new Random();
        double result = Math.log(1-r.nextFloat())/(-0.04);
        int randomHours = (int) Math.floor(result);
        int randomMinutes = (int) Math.floor((result - randomHours) * 60);
        date.plusHours(randomHours).plusMinutes(randomMinutes);
        return date;
    }

    private void addToHashMap(HashMap<LocalDateTime, ArrayList<Integer>> hm, LocalDateTime date, int state){
        if(!hm.containsKey(date)){
            hm.put(date, new ArrayList());
        }
        hm.get(date).add(state);
    }
}


    /*
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
        //creating data structures for analysis
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
*/