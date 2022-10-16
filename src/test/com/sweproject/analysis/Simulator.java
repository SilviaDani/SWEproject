package com.sweproject.analysis;

import com.github.sh0nk.matplotlib4j.NumpyUtils;
import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonConfig;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.sweproject.controller.UIController;
import com.sweproject.dao.ObservationDAO;
import com.sweproject.dao.ObservationDAOTest;
import com.sweproject.model.Contact;
import com.sweproject.model.Environment;
import com.sweproject.model.Type;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.oristool.models.stpn.TransientSolution;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Simulator extends UIController {
    int samples = 144;
    int steps = 1;
    final int maxReps = 1;
    TreeMap<LocalDateTime, ArrayList<Integer>> p1perTime = new TreeMap<>();
    TreeMap<LocalDateTime, ArrayList<Integer>> p2perTime = new TreeMap<>();
    @FXML
    private LineChart chart;
    private static ObservationDAO observationDAO;
    private STPNAnalyzer stpnAnalyzer;
    //Cambiare percorso in base al computer utilizzato e a dove è installato python
    String path1 = "C:\\Users\\super\\AppData\\Local\\Programs\\Python\\Python310\\python.exe";
    String path2 = "C:\\Python39\\python.exe";
    String path3 = "C:\\Users\\user\\AppData\\Local\\Microsoft\\WindowsApps\\python.exe";

    Simulator(){
        observationDAO = new ObservationDAO();
        stpnAnalyzer = new STPNAnalyzer(samples, steps);
    }

    @Test
    void plot_plot() throws PythonExecutionException, IOException {
        List<Double> x = NumpyUtils.linspace(0, samples, samples);
        List<Double> yPN = new ArrayList<>();
        for(int i=0;i<samples;i++)
            yPN.add((double) i);
        List<Double> S = x.stream().map(xi -> Math.sin(xi)).collect(Collectors.toList());

        Plot plt = Plot.create(PythonConfig.pythonBinPathConfig(path2));
        plt.plot().add(x, yPN);
        plt.plot().add(x, S);
        plt.xlim(Collections.min(x) * 1.1, Collections.max(x) * 1.1);
        plt.ylim(Collections.min(yPN) * 1.1, Collections.max(yPN) * 1.1);
        plt.show();
    }

    void plot(ArrayList<HashMap<String, TransientSolution>> ss, String fiscalCode) throws PythonExecutionException, IOException {
        TransientSolution s = ss.get(0).get(fiscalCode);
        List<Double> x = NumpyUtils.linspace(0, samples, samples);
        Double[] yPNarray = new Double[samples];
        List<Double> yPN = new ArrayList<>();
        int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
        IntStream.range(0, samples).parallel().forEach(i -> {
            double value = 0.f;
            for(int j = 1; j<ss.size();j++){ //FIXME: j=1 -> j=0 se vogliamo tenere di conto anche l'ambiente
                value += ss.get(j).get(fiscalCode).getSolution()[i][r][0];
            }
            yPNarray[i] = value;
        });
        yPN = Arrays.stream(yPNarray).toList();
        List<Double> S = x.stream().map(xi -> Math.sin(xi)).collect(Collectors.toList());
        Plot plt = Plot.create(PythonConfig.pythonBinPathConfig(path2));
        plt.plot().add(x, yPN);
        //plt.plot().add(x, S);
        plt.xlim(Collections.min(x) * 1.1, Collections.max(x) * 1.1);
        plt.ylim(Collections.min(yPN) * 1.1, Collections.max(yPN) * 1.1);
        plt.show();
    }

    @Test
    void start_simulation() throws PythonExecutionException, IOException {
        //Created Environment Contacts
        ArrayList<String> subjects = new ArrayList<>();
        subjects.add("P1");
        LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6);
        System.out.println(t0 + "<-t0");
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
        subjects.add("P2");
        tt = new Environment[4];
        LocalDateTime[] startDates2 = new LocalDateTime[]{t0.plusHours(3), t0.plusHours(43), t0.plusHours(96), t0.plusHours(136)};
        LocalDateTime[] endDates2 = new LocalDateTime[]{t0.plusHours(3+1), t0.plusHours(43).plusMinutes(30), t0.plusHours(96+1), t0.plusHours(136).plusMinutes(45)};
        String[] riskLevels2 = new String[]{"Medium", "High", "High", "Low"};
        boolean[] masks2 = new boolean[]{false, false, true, false};
        float[] risks2 = new float[4];
        for (int i = 0; i<4; i++) {
            tt[i] = new Environment(masks2[i], riskLevels2[i], startDates2[i], endDates2[i]);
            risks2[i] = BigDecimal.valueOf(((Environment) tt[i]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
            observationDAO.insertObservation(subjects, tt[i], startDates2[i], endDates2[i]);
        }

        //Create Cluster Contacts
        subjects.add(0, "P1");
        Type [] mm = new Contact [3];
        LocalDateTime[] startDates = new LocalDateTime[]{t0.plusHours(38), t0.plusHours(78), t0.plusHours(100)};
        LocalDateTime[] endDates = new LocalDateTime[]{t0.plusHours(38+3), t0.plusHours(78+1).plusMinutes(30), t0.plusHours(98+2).plusMinutes(15)};
        String[] riskLevels = new String[]{"Medium", "High", "Low"};
        masks = new boolean[]{false, false, true};
        float[] risks = new float[3];
        for (int i = 0; i<3; i++) {
            mm[i] = new Contact(subjects, masks[i], riskLevels[i], startDates[i], endDates[i]);
            risks[i] = BigDecimal.valueOf(((Environment) tt[i]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
            observationDAO.insertObservation(subjects, mm[i], startDates[i], endDates[i]);
        }

        //simulate
        for (int i=0; i<maxReps; i++){
            int p1_environment = 0;
            int p2_environment = 0;
            int p1_p2 = 0;
            ArrayList<LocalDateTime> nextContactTime = new ArrayList<>();
            ArrayList<Float> nextRisk = new ArrayList<>();
            ArrayList<String> nextContact = new ArrayList<>();

            //Get next contact in timeline and its risk
            for(int contact = 0; contact<13; contact++) {
                if (p1_environment < startDates1.length && p2_environment < startDates2.length && startDates.length > p1_p2){
                    if (startDates1[p1_environment].isBefore(startDates2[p2_environment]) && startDates1[p1_environment].isBefore(startDates[p1_p2])) {
                        nextContactTime.add(startDates1[p1_environment]);
                        nextRisk.add(risks1[p1_environment]);
                        nextContact.add("p1");
                        p1_environment++;
                    } else if (startDates2[p2_environment].isBefore(startDates1[p1_environment]) && startDates2[p2_environment].isBefore(startDates[p1_p2])) {
                        nextContactTime.add(startDates2[p2_environment]);
                        nextRisk.add(risks2[p2_environment]);
                        nextContact.add("p2");
                        p2_environment++;
                    } else if (startDates[p1_p2].isBefore(startDates1[p1_environment]) && startDates[p1_p2].isBefore(startDates2[p2_environment])) {
                        nextContactTime.add(startDates[p1_p2]);
                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
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
                else if (p1_environment >= startDates1.length && p2_environment < startDates2.length && startDates.length > p1_p2){
                    if (startDates2[p2_environment].isEqual(startDates[p1_p2])){
                        nextContactTime.add(startDates2[p2_environment]);
                        nextRisk.add(risks2[p2_environment]);
                        nextContact.add("p2");
                        p2_environment++;

                        nextContactTime.add(startDates[p1_p2]);
                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
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
                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
                        nextContact.add("p1");
                        nextContact.add("p2");
                        p1_p2++;
                    }
                }
                else if (p1_environment < startDates1.length && p2_environment >= startDates2.length && startDates.length > p1_p2){
                    if (startDates[p1_p2].isEqual(startDates1[p1_environment])){
                        nextContactTime.add(startDates[p1_p2]);
                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
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
                        nextContactTime.add(startDates[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
                        nextRisk.add(risks[p1_p2]);
                        nextContact.add("p1");
                        nextContact.add("p2");
                        p1_p2++;
                    }
                }
                else if (p1_environment < startDates1.length && p2_environment < startDates2.length && startDates.length >= p1_p2){
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
                else if (p1_environment >= startDates1.length && p2_environment >= startDates2.length && startDates.length < p1_p2){
                    nextContactTime.add(startDates[p1_p2]);
                    nextContactTime.add(startDates[p1_p2]);
                    nextRisk.add(risks[p1_p2]);
                    nextRisk.add(risks[p1_p2]);
                    nextContact.add("p1");
                    nextContact.add("p2");
                    p1_p2++;
                }
                else if (p1_environment >= startDates1.length && p2_environment < startDates2.length && startDates.length >= p1_p2){
                    nextContactTime.add(startDates2[p2_environment]);
                    nextRisk.add(risks2[p2_environment]);
                    nextContact.add("p2");
                    p2_environment++;
                }
                else if (p1_environment < startDates1.length && p2_environment >= startDates2.length && startDates.length <= p1_p2){
                    nextContactTime.add(startDates1[p1_environment]);
                    nextRisk.add(risks1[p1_environment]);
                    nextContact.add("p1");
                    p1_environment++;
                }
            }

            //XXX
            TreeMap<LocalDateTime, Integer> date_stateToInfected1 = new TreeMap<>();
            TreeMap<LocalDateTime, Integer> date_stateToContagious1 = new TreeMap<>();
            TreeMap<LocalDateTime, Integer> date_stateToInfected2 = new TreeMap<>();
            TreeMap<LocalDateTime, Integer> date_stateToContagious2 = new TreeMap<>();
            ArrayList<Integer> ids = new ArrayList<>();
            System.out.println("IDS"+ids.size());
            System.out.println("NC"+nextContact.size());
            for (int id=0; id< nextContact.size(); id++){
                ids.add(id, id);
                System.out.println("ids array " + ids.get(id));
            }
            System.out.println("IDS2"+ids.size());
            HashMap<Integer, Integer> id_states= new HashMap<>();
            for (int id=0; id<ids.size(); id++){
                id_states.put(id, 0);
            }
            LocalDateTime toContagious1 = LocalDateTime.from(t0);
            LocalDateTime toInfected1 = LocalDateTime.from(t0);
            LocalDateTime toContagious2 = LocalDateTime.from(t0);
            LocalDateTime toInfected2 = LocalDateTime.from(t0);
            while (nextContactTime.size() > 0) {
                System.out.println(id_states.values());
                String nc = nextContact.remove(0);
                LocalDateTime nct = nextContactTime.remove(0);
                int event_id = ids.remove(0);
                System.out.println("event " + event_id);
                System.out.println("nc " + nc);
                if (nc.equals("p1")) {
                    if (id_states.get(event_id) == 0) { //sano
                        Random r = new Random();
                        float random = 0 + r.nextFloat() * (1 - 0);
                        if (random < nextRisk.remove(0)) { //Da sano a contagiato
                            System.out.println("da sano a contagiato evento " + event_id);
                            LocalDateTime timeContagious = getSampleCC(nct, 12, 36);
                            int position = 0;
                            for (int iterator = 0; iterator < nextContactTime.size(); iterator++) {
                                if (nextContactTime.get(iterator).isBefore(timeContagious))
                                    position = iterator + 1;
                            }
                            nextContactTime.add(position, timeContagious);
                            nextContact.add(position, nc);
                            id_states.put(event_id, 1);
                            ids.add(position, event_id);
                            if (toInfected1.isBefore(nct)) {
                                date_stateToInfected1.remove(toInfected1);
                                toInfected1 = nct;
                                for (int l = 0; l < date_stateToInfected1.keySet().size(); l++) {
                                    LocalDateTime currentKey = (LocalDateTime) date_stateToInfected1.keySet().toArray()[l];
                                    if (toInfected1.isBefore(currentKey)) {
                                        System.out.println("rimosso "+date_stateToInfected1.get(currentKey));
                                        date_stateToInfected1.remove(currentKey);
                                    }
                                }
                            }
                            date_stateToInfected1.put(toInfected1, 1);
                            if (timeContagious.isBefore(toContagious1)) {
                                date_stateToContagious1.remove(toContagious1);
                                toContagious1 = timeContagious;
                                for (int l = 0; l < date_stateToContagious1.keySet().size(); l++) {
                                    LocalDateTime currentKey = (LocalDateTime) date_stateToContagious1.keySet().toArray()[l];
                                    if (toContagious1.isBefore(currentKey)) {
                                        date_stateToContagious1.remove(currentKey);
                                        System.out.println("Rimosso "+date_stateToInfected1.get(currentKey));
                                    }
                                }
                            }
                        } else {// resta sano
                            date_stateToInfected1.put(nct, 0);
                            System.out.println("da sano a sano " + event_id);
                        }
                    } else if (id_states.get(event_id) == 1) { //contagiato
                        //da contagiato a contagioso
                        System.out.println("da contagiato a contagioso evento " + event_id);
                        LocalDateTime timeHealing = getSampleCH(nct);
                        System.out.println(timeHealing);
                        int position = 0;
                        for (int iterator = 0; iterator < nextContactTime.size(); iterator++) {
                            if (nextContactTime.get(iterator).isBefore(timeHealing))
                                position = iterator + 1;
                        }
                        nextContactTime.add(position, timeHealing);
                        nextContact.add(position, nc);
                        id_states.put(event_id, 2);
                        System.out.println("inserito stato contagioso");
                        ids.add(position, event_id);
                        date_stateToContagious1.put(nct, 2);
                        System.out.println("stato al tempo "+ nct + " " +date_stateToContagious1.get(nct));

                    } else if (id_states.get(event_id) == 2) { //contagioso
                        //da contagioso a guarito
                        System.out.println("da contagioso a guarito evento " + event_id);
                        date_stateToContagious1.put(nct, 3);
                        id_states.put(event_id, 3);
                        System.out.println("stato al tempo "+ nct + " " +date_stateToContagious1.get(nct));
                    }
                } else if (nc.equals("p2")) {
                    if (id_states.get(event_id) == 0) { //sano
                        Random r = new Random();
                        float random = 0 + r.nextFloat() * (1 - 0);
                        if (random < nextRisk.remove(0)) { //Da sano a contagiato
                            System.out.println("da sano a contagiato evento " + event_id);
                            LocalDateTime timeContagious = getSampleCC(nct, 12, 36);
                            int position = 0;
                            for (int iterator = 0; iterator < nextContactTime.size(); iterator++) {
                                if (nextContactTime.get(iterator).isBefore(timeContagious))
                                    position = iterator + 1;
                            }
                            nextContactTime.add(position, timeContagious);
                            nextContact.add(position, nc);
                            id_states.put(event_id, 1);
                            ids.add(position, event_id);
                            if (toInfected2.isBefore(nct)) {
                                date_stateToInfected2.remove(toInfected2);
                                toInfected2 = nct;
                                for (int l = 0; l < date_stateToInfected2.keySet().size(); l++) {
                                    LocalDateTime currentKey = (LocalDateTime) date_stateToInfected2.keySet().toArray()[l];
                                    if (toInfected2.isBefore(currentKey)) {
                                        date_stateToInfected2.remove(currentKey);
                                    }
                                }
                            }
                            date_stateToInfected2.put(toInfected2, 1);
                            if (timeContagious.isBefore(toContagious2)) {
                                date_stateToContagious2.remove(toContagious2);
                                toContagious2 = timeContagious;
                                for (int l = 0; l < date_stateToContagious2.keySet().size(); l++) {
                                    LocalDateTime currentKey = (LocalDateTime) date_stateToContagious2.keySet().toArray()[l];
                                    if (toContagious2.isBefore(currentKey)) {
                                        date_stateToContagious2.remove(currentKey);
                                    }
                                }
                            }
                        } else {// resta sano
                            System.out.println("da sano a sano " + event_id);
                            date_stateToInfected2.put(nct, 0);
                        }
                    } else if (id_states.get(event_id) == 1) { //contagiato
                        //da contagiato a contagioso
                        System.out.println("da contagiato a contagioso evento " + event_id);
                        LocalDateTime timeHealing = getSampleCH(nct);
                        int position = 0;
                        for (int iterator = 0; iterator < nextContactTime.size(); iterator++) {
                            if (nextContactTime.get(iterator).isBefore(timeHealing))
                                position = iterator + 1;
                        }
                        nextContactTime.add(position, timeHealing);
                        nextContact.add(position, nc);
                        id_states.put(event_id, 2);
                        ids.add(position, event_id);
                        date_stateToContagious2.put(nct, 2);

                    } else if (id_states.get(event_id) == 2) { //contagioso
                        //da contagioso a guarito
                        System.out.println("da contagioso a guarito evento " + event_id);
                        date_stateToContagious2.put(nct, 3);
                        id_states.put(event_id, 3);
                    }
                }
            }
            //filling
            fill(date_stateToInfected1, date_stateToContagious1, t0);
            fill(date_stateToInfected2, date_stateToContagious2, t0);

            for (int l=0; l<date_stateToInfected1.size(); l++){
                LocalDateTime currentKey = (LocalDateTime) date_stateToInfected1.keySet().toArray()[l];
                int currentValue = date_stateToInfected1.get(currentKey);
                addToHashMap(p1perTime, currentKey, currentValue);
            }
            for (int l=0; l<date_stateToContagious1.size(); l++){
                System.out.println("2half size "+date_stateToContagious1.size());
                LocalDateTime currentKey = (LocalDateTime) date_stateToContagious1.keySet().toArray()[l];
                int currentValue = date_stateToContagious1.get(currentKey);
                System.out.println("states "+currentValue);
                addToHashMap(p1perTime, currentKey, currentValue);
            }

            for (int l=0; l<date_stateToInfected2.size(); l++){
                LocalDateTime currentKey = (LocalDateTime) date_stateToInfected2.keySet().toArray()[l];
                int currentValue = date_stateToInfected2.get(currentKey);
                addToHashMap(p2perTime, currentKey, currentValue);
            }
            for (int l=0; l<date_stateToContagious2.size(); l++){
                LocalDateTime currentKey = (LocalDateTime) date_stateToContagious2.keySet().toArray()[l];
                int currentValue = date_stateToContagious2.get(currentKey);
                addToHashMap(p2perTime, currentKey, currentValue);
            }
        }

        float[] contagiousProbability1 = new float[p1perTime.size()];
        float[] hours1 = new float[p1perTime.size()];
        for (int r=0; r<p1perTime.size(); r++){
            LocalDateTime currentKey = (LocalDateTime) p1perTime.keySet().toArray()[r];
            int p1_count = 0;
            for (int state=0; state<p1perTime.get(currentKey).size(); state++){
                //System.out.println("pppp " + p1perTime.get(currentKey).get(state));
                if (p1perTime.get(currentKey).get(state) == 2)
                    p1_count++;
            }
            System.out.println("p1 count " + p1_count);
            System.out.println("tot " + p1perTime.get(currentKey).size());
            float p1_probability = ((float)p1_count)/((float)p1perTime.get(currentKey).size());
            System.out.println("p1 prb " + p1_probability);
            contagiousProbability1[r] = p1_probability;
            float hour1 = ChronoUnit.MINUTES.between(t0, (LocalDateTime) p1perTime.keySet().toArray()[r])/60.f;
            hours1[r] = hour1;
        }

        float[] contagiousProbability2 = new float[p2perTime.size()];
        float[] hours2 = new float[p2perTime.size()];
        for (int r=0; r<p2perTime.size(); r++){
            LocalDateTime currentKey = (LocalDateTime) p2perTime.keySet().toArray()[r];
            int p2_count = 0;
            for (int state=0; state<p2perTime.get(currentKey).size(); state++){
                if (p2perTime.get(currentKey).get(state) == 2)
                    p2_count++;
            }
            float p2_probability = ((float)p2_count)/((float)p2perTime.get(currentKey).size());
            contagiousProbability2[r] = p2_probability;
            float hour2 = ChronoUnit.MINUTES.between(t0, (LocalDateTime) p2perTime.keySet().toArray()[r])/60.f;
            hours2[r] = hour2;
        }
        System.out.println(p1perTime.size());
        System.out.println(p1perTime.keySet());
        System.out.println(Arrays.toString(contagiousProbability1));
        System.out.println(Arrays.toString(hours1));

        //TODO PLOT SIMULAZIONE


        //TODO FIXA LA PARTE DI SIRIO
        /*
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
        System.out.println("---");
        System.out.println(clusterSubjectsMet.keySet());
        System.out.println(subjects.get(0));
        for(String s : clusterSubjectsMet.keySet()){
            System.out.println(s+ " "+ clusterSubjectsMet.get(s));
        }
        System.out.println("---");
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

        plot(pns, "P1");
        */
    }

    private LocalDateTime getSampleCC(LocalDateTime date, int min, int max) {
        Random r = new Random();
        int randomHours = min + r.nextInt(max + 1 - min);
        int randomMinutes = r.nextInt(60);
        return date.plusHours(randomHours).plusMinutes(randomMinutes);
    }

    private LocalDateTime getSampleCH(LocalDateTime date) {
        Random r = new Random();
        double result = Math.log(1-r.nextFloat())/(-0.04);
        int randomHours = (int) Math.floor(result);
        int randomMinutes = (int) Math.floor((result - randomHours) * 60);
        return date.plusHours(randomHours).plusMinutes(randomMinutes);
    }

    private void addToHashMap(TreeMap<LocalDateTime, ArrayList<Integer>> hm, LocalDateTime date, int state){
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
        ArrayList<HashMap<String, Object>> obs2 = observationDAO.getObservations("p2");
        for(int i = 0; i<obs2.size();i++){
            ObservationDAOTest.deleteObservation(obs2.get(i).get("id").toString());
        }
    }

    private TreeMap<LocalDateTime,Integer> fill(TreeMap<LocalDateTime, Integer> date_stateToInfected, TreeMap<LocalDateTime, Integer> date_stateToContagious, LocalDateTime t0){
        LocalDateTime t = LocalDateTime.from(t0);
        final int minutes = 1;
        //trovo 1 e metto tutti 1, poi trovo 2 e metto tutti 2, poi trovo 3 e metto tutti 3 fine.
        TreeMap<LocalDateTime, Integer> treeMap = new TreeMap<>();
        treeMap.putAll(date_stateToInfected);
        treeMap.putAll(date_stateToContagious);
        for(Map.Entry<LocalDateTime, Integer> entry : treeMap.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());
        LocalDateTime tLimit = LocalDateTime.from(t).plusDays(6);
        TreeMap<LocalDateTime, Integer> state = new TreeMap<>();
        int currentState = 0;
        while(t.isBefore(tLimit) && treeMap.size() > 0){
            if(ChronoUnit.MINUTES.between(t, treeMap.firstKey())<minutes){
                currentState = currentState<treeMap.firstEntry().getValue() && treeMap.firstEntry().getValue() == currentState+1 ?treeMap.firstEntry().getValue():currentState;
                treeMap.remove(treeMap.firstKey());
            }
            state.put(t, currentState);
            t = t.plusMinutes(minutes);
        }
        System.out.println("---");
        System.out.println("---");
       /*( for(Map.Entry<LocalDateTime, Integer> entry : state.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());*/
        System.out.println("---");
        return state;
    }
}


    /*
    private float evaluateExpolynomial(int lowerBound, int upperBound){
        Expolynomial expolynomial = Expolynomial.fromString("48 * x^1 + -1* x^2 + -432 * x^0");
        expolynomial.multiply(new BigDecimal(4.3402777777777775E-4));
        Variable x = new Variable("x");
        return Float.parseFloat(expolynomial.integrate(x, new OmegaBigDecimal(lowerBound), new OmegaBigDecimal(upperBound)).toString());
    }

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