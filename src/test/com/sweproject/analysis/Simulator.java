package com.sweproject.analysis;

import com.sweproject.controller.UIController;
import com.sweproject.dao.ObservationDAO;
import com.sweproject.dao.ObservationDAOTest;
import com.sweproject.model.Contact;
import com.sweproject.model.Environment;
import com.sweproject.model.Type;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.AfterAll;
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

public class Simulator extends UIController {
    int samples = 144;
    int steps = 1;
    final int maxReps = 10000;
    HashMap<LocalDateTime, ArrayList<Integer>> p1perTime = new HashMap<LocalDateTime, ArrayList<Integer>>();
    HashMap<LocalDateTime, ArrayList<Integer>> p2perTime = new HashMap<LocalDateTime, ArrayList<Integer>>();
    @FXML
    private LineChart chart;
    private static ObservationDAO observationDAO;
    private STPNAnalyzer stpnAnalyzer;

    Simulator(){
        observationDAO = new ObservationDAO();
        stpnAnalyzer = new STPNAnalyzer(samples, steps);
    }

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
        subjects.add(0, "p1");
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
            ArrayList<LocalDateTime> nextContactTime = new ArrayList<>();
            ArrayList<Float> nextRisk = new ArrayList<>();
            ArrayList<String> nextContact = new ArrayList<>();

            //Get next contact in timeline and its risk
            for(int contact = 0; contact<13; contact++) {
                if (p1_environment < startDates1.length & p2_environment < startDates2.length & startDates.length < p1_p2){
                    if (startDates1[p1_environment].isBefore(startDates2[p2_environment]) & startDates1[p1_environment].isBefore(startDates[p1_p2])) {
                        nextContactTime.add(startDates1[p1_environment]);
                        nextRisk.add(risks1[p1_environment]);
                        nextContact.add("p1");
                        p1_environment++;
                    } else if (startDates2[p2_environment].isBefore(startDates1[p1_environment]) & startDates2[p2_environment].isBefore(startDates[p1_p2])) {
                        nextContactTime.add(startDates2[p2_environment]);
                        nextRisk.add(risks2[p2_environment]);
                        nextContact.add("p2");
                        p2_environment++;
                    } else if (startDates[p1_p2].isBefore(startDates1[p1_environment]) & startDates[p1_p2].isBefore(startDates2[p2_environment])) {
                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
                        nextContact.add("p1");
                        nextContact.add("p2");
                        p1_p2++;
                    } else if (startDates1[p1_environment].isEqual(startDates2[p2_environment])) {
                        nextContactTime.add(startDates1[p1_environment]);
                        nextContactTime.add(startDates2[p2_environment]);
                        nextRisk.add(risks1[p1_environment]);
                        nextRisk.add(risks2[p2_environment]);
                        nextContact.add("p1");
                        nextContact.add("p2");
                        p1_environment++;
                        p2_environment++;
                    }
                }
                else if (p1_environment >= startDates1.length & p2_environment < startDates2.length & startDates.length < p1_p2){
                    if (startDates2[p2_environment].isEqual(startDates[p1_p2])){
                        nextContactTime.add(startDates2[p2_environment]);
                        nextRisk.add(risks2[p2_environment]);
                        nextContact.add("p2");
                        p2_environment++;

                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
                        nextContact.add("p1");
                        nextContact.add("p2");
                        p1_p2++;
                    }
                    else if (startDates2[p2_environment].isBefore(startDates[p1_p2])){
                        nextContactTime.add(startDates2[p2_environment]);
                        nextRisk.add(risks2[p2_environment]);
                        nextContact.add("p2");
                        p2_environment++;
                    }
                    else {
                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks2[p1_p2]);
                        nextContact.add("p1");
                        nextContact.add("p2");
                        p1_p2++;
                    }
                }
                else if (p1_environment < startDates1.length & p2_environment >= startDates2.length & startDates.length < p1_p2){
                    if (startDates[p1_p2].isEqual(startDates1[p1_environment])){
                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks2[p1_p2]);
                        nextContact.add("p1");
                        nextContact.add("p2");
                        p1_p2++;

                        nextContactTime.add(startDates1[p1_environment]);
                        nextRisk.add(risks1[p1_environment]);
                        nextContact.add("p1");
                        p1_environment++;
                    }
                    else if (startDates1[p1_environment].isBefore(startDates[p1_p2])){
                        nextContactTime.add(startDates1[p1_environment]);
                        nextRisk.add(risks1[p1_environment]);
                        nextContact.add("p1");
                        p1_environment++;
                    }
                    else {
                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks2[p1_p2]);
                        nextContact.add("p1");
                        nextContact.add("p2");
                        p1_p2++;
                    }
                }
                else if (p1_environment < startDates1.length & p2_environment < startDates2.length & startDates.length <= p1_p2){
                    if (startDates2[p2_environment].isEqual(startDates1[p1_environment])){
                        nextContactTime.add(startDates2[p2_environment]);
                        nextRisk.add(risks2[p2_environment]);
                        nextContact.add("p2");
                        p2_environment++;

                        nextContactTime.add(startDates1[p1_environment]);
                        nextRisk.add(risks1[p1_environment]);
                        nextContact.add("p1");
                        p1_environment++;
                    }
                    else if (startDates2[p2_environment].isEqual(startDates1[p1_environment])){
                        nextContactTime.add(startDates2[p2_environment]);
                        nextRisk.add(risks2[p2_environment]);
                        nextContact.add("p2");
                        p2_environment++;

                        nextContactTime.add(startDates1[p1_environment]);
                        nextRisk.add(risks1[p1_environment]);
                        nextContact.add("p1");
                        p1_environment++;
                    }
                    else if (startDates2[p2_environment].isBefore(startDates1[p1_environment])){
                        nextContactTime.add(startDates2[p2_environment]);
                        nextRisk.add(risks2[p2_environment]);
                        nextContact.add("p2");
                        p2_environment++;
                    }
                    else {
                        nextContactTime.add(startDates1[p1_environment]);
                        nextRisk.add(risks1[p1_environment]);
                        nextContact.add("p1");
                        p1_environment++;
                    }
                }
                else if (p1_environment >= startDates1.length & p2_environment >= startDates2.length & startDates.length < p1_p2){
                    nextContactTime.add(startDates[p1_p2]);
                    nextRisk.add(risks2[p1_p2]);
                    nextContact.add("p1");
                    nextContact.add("p2");
                    p1_p2++;
                }
                else if (p1_environment >= startDates1.length & p2_environment < startDates2.length & startDates.length >= p1_p2){
                    nextContactTime.add(startDates2[p2_environment]);
                    nextRisk.add(risks2[p2_environment]);
                    nextContact.add("p2");
                    p2_environment++;
                }
                else if (p1_environment < startDates1.length & p2_environment >= startDates2.length & startDates.length >= p1_p2){
                    nextContactTime.add(startDates1[p1_environment]);
                    nextRisk.add(risks1[p1_environment]);
                    nextContact.add("p1");
                    p1_environment++;
                }
            }

            System.out.println("NEXTCONTACT SIZE " + nextContact.size());

            while (nextContactTime.size() > 0){
                System.out.println("1 " + nextContact.size());
                String nc = nextContact.remove(0);
                if (nc.equals("p1")){
                    if (lastStates[0] == 0){ //sano
                        Random r = new Random();
                        float random = 0 + r.nextFloat() * (1 - 0);
                        if (random<nextRisk.remove(0)) { //Da sano a contagiato
                            lastStates[0] += 1;
                            System.out.println("new state  " + lastStates[0]);
                            LocalDateTime timeContagious = getSampleCC(nextContactTime.get(0), 12, 36);
                            addToHashMap(p1perTime, nextContactTime.remove(0), lastStates[0]);
                            System.out.println("nextcontact.size 1 " + nextContact.size());
                            int position = 0;
                            for (int iterator=0; iterator<nextContactTime.size(); iterator++){
                                if (nextContactTime.get(iterator).isBefore(timeContagious))
                                    position = iterator + 1;
                            }
                            nextContactTime.add(position, timeContagious);
                            nextContact.add(position, "p1");
                            System.out.println("nextcontact.size 2 " + nextContact.size());
                            System.out.println("2 " + nextContact.size());
                        }
                        else {// resta sano
                            addToHashMap(p1perTime, nextContactTime.remove(0), lastStates[0]);
                            System.out.println("resta sano");
                            System.out.println("nextcontact.size 1 " + nextContact.size());
                        }
                    }
                    else if (lastStates[0] == 1){ //contagiato
                        lastStates[0] += 1; //da contagiato a contagioso
                        LocalDateTime timeHealing = getSampleCH(nextContactTime.get(0));
                        System.out.println("new state  " + lastStates[0]);
                        addToHashMap(p1perTime, nextContactTime.remove(0), lastStates[0]);
                        System.out.println("nextcontact.size 1 " + nextContact.size());
                        int position = 0;
                        for (int iterator=0; iterator<nextContactTime.size(); iterator++){
                            if (nextContactTime.get(iterator).isBefore(timeHealing))
                                position = iterator + 1;
                        }
                        nextContactTime.add(position, timeHealing);
                        nextContact.add(position, "p1");
                        System.out.println("nextcontact.size 2 " + nextContact.size());
                        System.out.println("3 " + nextContact.size());

                    }
                    else if (lastStates[0] == 2){ //contagioso
                        lastStates[0] += 1; //da contagioso a guarito
                        System.out.println("new state  " + lastStates[0]);
                        addToHashMap(p1perTime, nextContactTime.remove(0), lastStates[0]);
                        System.out.println("nextcontact.size 1 " + nextContact.size());
                    }
                }
                else if (nc.equals("p2")){
                    System.out.println("contatto di p2");
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
        float[] contagiousProbability1 = new float[p1perTime.size()];
        float[] hours1 = new float[p1perTime.size()];
        for (int r=0; r<p1perTime.size(); r++){
            int p1_count = 0;
            for (int state=0; state<p1perTime.get(r).size(); state++){
                if (p1perTime.get(r).get(state) == 2)
                    p1_count++;
            }
            float p1_probability = p1_count/p1perTime.get(r).size();
            contagiousProbability1[r] = p1_probability;
            float hour1 = ChronoUnit.MINUTES.between(t0, (LocalDateTime) p1perTime.keySet().toArray()[0])/60;
            hours1[r] = hour1;
        }

        float[] contagiousProbability2 = new float[p2perTime.size()];
        float[] hours2 = new float[p2perTime.size()];
        for (int r=0; r<p2perTime.size(); r++){
            int p2_count = 0;
            for (int state=0; state<p2perTime.get(r).size(); state++){
                if (p2perTime.get(r).get(state) == 2)
                    p2_count++;
            }
            float p2_probability = p2_count/p2perTime.get(r).size();
            contagiousProbability2[r] = p2_probability;
            float hour2 = ChronoUnit.MINUTES.between(t0, (LocalDateTime) p1perTime.keySet().toArray()[0])/60;
            hours2[r] = hour2;
        }


        //TODO PLOT SIMULAZIONE


        //TODO FIXA LA PARTE DI SIRIO
        ArrayList<HashMap<String, TransientSolution>> pns = new ArrayList<>();
        final int max_iterations = subjects.size()<=2?subjects.size()-1:2;
        HashMap<String, ArrayList<HashMap<String, Object>>> clusterSubjectsMet = new HashMap<>();
        if(max_iterations>0){
            for(int i = 0; i<subjects.size(); i++){
                ArrayList<String> otherMembers = new ArrayList<>(subjects);
                otherMembers.remove(i);
                clusterSubjectsMet.put(subjects.get(i), observationDAO.getContactObservations(subjects.get(i), otherMembers));
            }
        }

        for(int nIteration = 0; nIteration<= max_iterations; nIteration++){
            HashMap<String, TransientSolution> pits = new HashMap<>();//p^it_s
            for(String member : subjects){
                System.out.println(member + " it:"+nIteration + " started");
                if(nIteration==0){
                    try {
                        pits.put(member, stpnAnalyzer.makeModel(member));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    pits.put(member, stpnAnalyzer.makeClusterModel(pns.get(nIteration-1), clusterSubjectsMet.get(member)));
                }
                System.out.println(member + " it:"+nIteration + " completed");
            }
            pns.add(pits);
        }

        //TODO user da rivedere
        XYChart.Series series = stpnAnalyzer.makeChart(pns, user.getFiscalCode().toUpperCase());
        series.setName("Contagion level");
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Minutes");
        yAxis.setLabel("Contagion level");
        chart.setTitle("Probability of being contagious due to a contact during the last 6 days");
        chart.setCreateSymbols(false);
        chart.getData().add(series);
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

    @AfterAll
    static void clean(){
        ArrayList<HashMap<String, Object>> obs = observationDAO.getObservations("p1");
        for(int i = 0; i<obs.size();i++){
            ObservationDAOTest.deleteObservation(obs.get(i).get("id").toString());
        }
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