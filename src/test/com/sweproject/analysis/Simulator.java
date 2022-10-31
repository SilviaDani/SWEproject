package com.sweproject.analysis;

import com.github.sh0nk.matplotlib4j.NumpyUtils;
import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonConfig;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.sun.source.tree.Tree;
import com.sweproject.controller.UIController;
import com.sweproject.dao.ObservationDAO;
import com.sweproject.dao.ObservationDAOTest;
import com.sweproject.model.Contact;
import com.sweproject.model.Environment;
import com.sweproject.model.Type;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.oristool.models.stpn.TransientSolution;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
class Subject{
    private int currentState;
    private TreeMap<LocalDateTime, Integer> relevantTimestamps;
    private String name;

    public Subject(String name) {
        this.name = name;
        currentState = 0;
        relevantTimestamps = new TreeMap<>();
    }
    public void initializeSubject(LocalDateTime ldt){
        currentState = 0;
        relevantTimestamps.clear();
        relevantTimestamps.put(ldt, 0);
    }
    public String getName() {
        return name;
    }

    public int getCurrentState() {
        return currentState;
    }

    public TreeMap<LocalDateTime, Integer> getTimestamps() {
        return relevantTimestamps;
    }

    public void changeState(LocalDateTime ldt){
        if(currentState<3) {
            currentState++;
            relevantTimestamps.put(ldt, currentState);
        }
    }
}
class Event implements Comparator<Event>, Comparable<Event>{
    protected LocalDateTime startDate;
    protected LocalDateTime endDate;
    protected Type type;
    protected ArrayList<Subject> subject;

    public Event(LocalDateTime startDate, LocalDateTime endDate, Type type, ArrayList<Subject> subject) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.subject = subject;
    }

    @Override
    public int compare(Event o1, Event o2) {
        return o1.startDate.compareTo(o2.startDate);
    }
    public LocalDateTime getStartDate() {
        return startDate;
    }
    public LocalDateTime getEndDate() {
        return endDate;
    }
    public Type getType() {
        return type;
    }
    public ArrayList<Subject> getSubject() {
        return subject;
    }

    @Override
    public int compareTo(Event o) {
        return startDate.compareTo(o.startDate);
    }
}
class ChangeStateEvent extends Event{

    public ChangeStateEvent(LocalDateTime startDate, ArrayList<Subject> subject) {
        super(startDate, null, null, subject);
    }
}

public class Simulator extends UIController {
    int samples = 144;
    int steps = 1;
    final int maxReps = 100000;
    @FXML
    private LineChart chart;
    private static ObservationDAO observationDAO;
    private STPNAnalyzer stpnAnalyzer;
    //Cambiare percorso in base al computer utilizzato e a dove è installato python
    String path1 = "C:\\Users\\super\\AppData\\Local\\Programs\\Python\\Python310\\python.exe";
    String path2 = "C:\\Python39\\python.exe";
    String path3 = "C:\\Users\\user\\AppData\\Local\\Programs\\Python\\Python310\\python.exe";
    String PYTHON_PATH; //XXX: environment variable
    static final int np = 3;
    int nContact = 2;
    int max_nEnvironment = 5;
    int min_nEnvironment = 2;
    //FIXME il problema potrebbe essere che non prende le osservazioni dell'ambiente dei soggetti dopo P0.

    Simulator(){
        observationDAO = new ObservationDAO();
        stpnAnalyzer = new STPNAnalyzer(samples, steps);
    }

    void plot(TreeMap<LocalDateTime, Double> t, ArrayList<HashMap<String, TransientSolution>> ss, String fiscalCode) throws PythonExecutionException, IOException {
       /* TransientSolution s = ss.get(0).get(fiscalCode);
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
        plt.show();*/
        List<Double> tYSampled = new ArrayList<>();
        Double[] tYSampledArray = new Double[samples];
        IntStream.range(0, samples).parallel().forEach(i->{
            if(i < t.keySet().size())
                tYSampledArray[i] = t.get((LocalDateTime) t.keySet().toArray()[i]);
        });
        tYSampled = Arrays.stream(tYSampledArray).toList();

        TransientSolution s = ss.get(0).get(fiscalCode);
        Double[] yPNarray = new Double[samples];
        List<Double> yPN = new ArrayList<>();
        int r = s.getRegenerations().indexOf(s.getInitialRegeneration());
        IntStream.range(0, samples).parallel().forEach(i -> {
            double value = 0.f;
            for(int j = 0; j<ss.size();j++){ //FIXME: j=1 -> j=0 se vogliamo tenere di conto anche l'ambiente
                value += ss.get(j).get(fiscalCode).getSolution()[i][r][0];
            }
            yPNarray[i] = value;
        });
        yPN = Arrays.stream(yPNarray).toList();
        List<Double> x = NumpyUtils.linspace(0, samples, samples);
        Plot plt = Plot.create(PythonConfig.pythonBinPathConfig(PYTHON_PATH));
        plt.plot().add(x, tYSampled);
        plt.plot().add(x, yPN);
        plt.xlim(Collections.min(x) * 1.1, Collections.max(x) * 1.1);
        plt.ylim(-0.1, 1.1);
        plt.show();

    }
    void plot(TreeMap<LocalDateTime, Double> t1, ArrayList<HashMap<String, TransientSolution>> ss,String fiscalCode1, TreeMap<LocalDateTime, Double> t2, String fiscalCode2) throws PythonExecutionException, IOException {
       /* TransientSolution s = ss.get(0).get(fiscalCode);
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
        plt.show();*/
        List<Double> tYSampled1 = new ArrayList<>();
        List<Double> tYSampled2 = new ArrayList<>();
        Double[] tYSampledArray1 = new Double[samples];
        Double[] tYSampledArray2 = new Double[samples];
        /*IntStream.range(0, samples).parallel().forEach(i->{
            if(i*60 < t1.keySet().size()) {
                tYSampledArray1[i] = t1.get((LocalDateTime) t1.keySet().toArray()[i * 60]);
                tYSampledArray2[i] = t2.get((LocalDateTime) t2.keySet().toArray()[i * 60]);
            }
        });*/
        IntStream.range(0, samples).parallel().forEach(i->{
            if(i< t1.keySet().size()) {
                tYSampledArray1[i] = t1.get((LocalDateTime) t1.keySet().toArray()[i]);
                tYSampledArray2[i] = t2.get((LocalDateTime) t2.keySet().toArray()[i]);
            }
        });

        tYSampled1 = Arrays.stream(tYSampledArray1).toList();
        tYSampled2 = Arrays.stream(tYSampledArray2).toList();

        TransientSolution s1 = ss.get(0).get(fiscalCode1);
        TransientSolution s2 = ss.get(0).get(fiscalCode2);
        Double[] yPNarray1 = new Double[samples];
        List<Double> yPN1 = new ArrayList<>();
        Double[] yPNarray2 = new Double[samples];
        List<Double> yPN2 = new ArrayList<>();
        int r = s1.getRegenerations().indexOf(s1.getInitialRegeneration());
        IntStream.range(0, samples).parallel().forEach(i -> {
            double value1 = 0.f;
            double value2 = 0.f;
            for(int j = 0; j<ss.size();j++){ //FIXME: j=1 -> j=0 se vogliamo tenere di conto anche l'ambiente
                value1 += ss.get(j).get(fiscalCode1).getSolution()[i][r][0];
                value2 += ss.get(j).get(fiscalCode2).getSolution()[i][r][0];
            }
            yPNarray1[i] = value1;
            yPNarray2[i] = value2;
        });
        yPN1 = Arrays.stream(yPNarray1).toList();
        yPN2 = Arrays.stream(yPNarray2).toList();
        List<Double> x = NumpyUtils.linspace(0, samples, samples);
        Plot plt = Plot.create(PythonConfig.pythonBinPathConfig(PYTHON_PATH));
        plt.plot().add(x, tYSampled1);
        plt.plot().add(x, yPN1);
        plt.plot().add(x, tYSampled2);
        plt.plot().add(x, yPN2);
        plt.xlim(Collections.min(x) * 1.1, Collections.max(x) * 1.1);
        plt.ylim(-0.1,1.1);
        plt.show();

    }


    void plot(HashMap<String, TreeMap<LocalDateTime, Double>> tt, ArrayList<HashMap<String, TransientSolution>> ss) throws PythonExecutionException, IOException {
       /* TransientSolution s = ss.get(0).get(fiscalCode);
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
        plt.show();*/
        String[] codes = new String[tt.size()];
        int index = 0;
        for(Object o : tt.keySet()){
            codes[index] = (String) o;
            index++;
        }
        List<Double> x = NumpyUtils.linspace(0, samples, samples);
        Plot plt = Plot.create(PythonConfig.pythonBinPathConfig(PYTHON_PATH));
        String[] finalCodes = codes;
        for(int j = 0; j<codes.length; j++) {
            List<Double> tYSampled1 = new ArrayList<>();
            Double[] tYSampledArray1 = new Double[samples];
            int finalJ = j;
            IntStream.range(0, samples).parallel().forEach(i -> {
                if (i < tt.get(finalCodes[finalJ]).keySet().size()) {
                    tYSampledArray1[i] = tt.get(finalCodes[finalJ]).get((LocalDateTime) tt.get(finalCodes[finalJ]).keySet().toArray()[i]);
                }
            });

            tYSampled1 = Arrays.stream(tYSampledArray1).toList();

            Double[] yPNarray1 = new Double[samples];
            List<Double> yPN1 = new ArrayList<>();
            Double[] yPNarray2 = new Double[samples];
            List<Double> yPN2 = new ArrayList<>();
            int r = ss.get(0).get(finalCodes[finalJ]).getRegenerations().indexOf(ss.get(0).get(finalCodes[finalJ]).getInitialRegeneration());
            IntStream.range(0, samples).parallel().forEach(i -> {
                double value1 = 0.f;
                double value2 = 0.f;
                for (int k = 0; k < ss.size(); k++) { //FIXME: j=1 -> j=0 se vogliamo tenere di conto anche l'ambiente
                    value1 += ss.get(k).get(finalCodes[finalJ]).getSolution()[i][r][0];
                }
                yPNarray1[i] = value1;
            });
            yPN1 = Arrays.stream(yPNarray1).toList();
            plt.plot().add(x, tYSampled1);
            plt.plot().add(x, yPN1);
        }
        plt.xlim(Collections.min(x) * 1.1, Collections.max(x) * 1.1);
        plt.ylim(-0.1,1.1);
        plt.show();

    }


    @Test
    void start_simulation() throws PythonExecutionException, IOException {
        //Created Environment Contacts
        ArrayList<String> subjects = new ArrayList<>();
        ArrayList<String[]> np_contact = new ArrayList<>();
        ArrayList<LocalDateTime[]> np_startDates = new ArrayList<>();
        ArrayList<LocalDateTime[]> np_endDates = new ArrayList<>();
        ArrayList<String[]> np_riskLevels = new ArrayList<>();
        ArrayList<Boolean[]> np_masks = new ArrayList<>();
        ArrayList<float[]> np_risks = new ArrayList<>();
        LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6);

        for (int p=0; p<np; p++){
            subjects.clear();
            String current_subject = "P" + p;
            //System.out.println(current_subject);
            subjects.add(current_subject);
            Random r = new Random();
            int nEnvironment = r.nextInt(max_nEnvironment - min_nEnvironment) + min_nEnvironment;
            //System.out.println("nEnvironment" + p + ": " + nEnvironment);
            Type[] tt = new Environment[nEnvironment];
            LocalDateTime[] startDates = new LocalDateTime[nEnvironment];
            LocalDateTime[] endDates = new LocalDateTime[nEnvironment];
            ArrayList<LocalDateTime> dates = new ArrayList<>(generateDates(t0, nEnvironment));

            int start = 0;
            int end = 0;
            for (int date=0; date<dates.size(); date++){
                if (date%2==0) {
                    startDates[start] = dates.get(date);
                    start++;
                }
                else {
                    endDates[end] = dates.get(date);
                    end++;
                }
            }
            np_startDates.add(startDates);
            np_endDates.add(endDates);
            if(np_riskLevels.size()<p) {
                np_riskLevels.set(p, generateRiskLevels(nEnvironment));
            }else{
                np_riskLevels.add(p, generateRiskLevels(nEnvironment));
            }
            if(np_masks.size()<p){
                np_masks.set(p, generateMasks(nEnvironment));
            }else{
                np_masks.add(p, generateMasks(nEnvironment));
            }


            float[] risks = new float[nEnvironment];
            String[] contacts = new String[nEnvironment];
            for(int i = 0; i<nEnvironment; i++) {
               contacts[i] =  current_subject;
                tt[i] = new Environment(np_masks.get(p)[i], np_riskLevels.get(p)[i], np_startDates.get(p)[i], np_endDates.get(p)[i]);
                risks[i] = BigDecimal.valueOf(((Environment) tt[i]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
                observationDAO.insertObservation(subjects, tt[i], np_startDates.get(p)[i], np_endDates.get(p)[i]);
                //System.out.println("aggiunta osservazione ");
            }
            np_risks.add(risks);
            np_contact.add(contacts);
        }


        //Create Cluster Contacts
        Random r = new Random();
        //int nContact = (int) (r.nextInt((int) (np - Math.ceil(np/2))) + Math.ceil(np/2));
        subjects.clear();
        for (int p=0; p<np; p++){
            subjects.add("P" + p);
        }
        Type [] mm = new Contact [nContact];


        LocalDateTime[] nc_startDates = new LocalDateTime[nContact];
        LocalDateTime[] nc_endDates = new LocalDateTime[nContact];
        String[] nc_riskLevels = new String[nContact];
        Boolean[] nc_masks = new Boolean[nContact];
        float[] nc_risks = new float[nContact];
        ArrayList<LocalDateTime> dates = new ArrayList<>(generateDates(t0, nContact));
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
        nc_riskLevels = generateRiskLevels(nContact);
        nc_masks = generateMasks(nContact);

        for(int c = 0; c<nContact; c++){
            mm[c] = new Contact(subjects, nc_masks[c], nc_riskLevels[c], nc_startDates[c], nc_endDates[c]);
            nc_risks[c] = BigDecimal.valueOf(((Contact) mm[c]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
            observationDAO.insertObservation(subjects, mm[c], nc_startDates[c], nc_endDates[c]);
        }

        /*ArrayList<LocalDateTime[]> nc_startDates = new ArrayList<>();
        ArrayList<LocalDateTime[]> nc_endDates = new ArrayList<>();
        ArrayList<String[]> nc_riskLevels = new ArrayList<>();
        ArrayList<Boolean[]> nc_masks = new ArrayList<>();
        ArrayList<float[]> nc_risks = new ArrayList<>();

        for (int c=0; c<nContact; c++) {
            LocalDateTime[] startDates = new LocalDateTime[nContact];
            LocalDateTime[] endDates = new LocalDateTime[nContact];
            ArrayList<LocalDateTime> dates = new ArrayList<>(generateDates(t0, nContact));

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
            nc_startDates.add(startDates);
            nc_endDates.add(endDates);
            if(nc_riskLevels.size()<c){
                nc_riskLevels.set(c, generateRiskLevels(nContact));
            }else{
                nc_riskLevels.add(c, generateRiskLevels(nContact));
            }
            if(nc_masks.size()<c){
                nc_masks.set(c, generateMasks(nContact));
            }else{
                nc_masks.add(c, generateMasks(nContact));
            }

            float[] risks = new float[nContact];
            for (int i = 0; i < nContact; i++) {
                mm[i] = new Contact(subjects, nc_masks.get(c)[i], nc_riskLevels.get(c)[i], nc_startDates.get(c)[i], nc_endDates.get(c)[i]);
                risks[i] = BigDecimal.valueOf(((Contact) mm[i]).getRiskLevel()).setScale(6, BigDecimal.ROUND_HALF_UP).floatValue();
                observationDAO.insertObservation(subjects, mm[i], nc_startDates.get(c)[i], nc_endDates.get(c)[i]);
            }
            nc_risks.add(risks);
        }*/


        ArrayList<LocalDateTime> nextContactTime = new ArrayList<>();
        ArrayList<Float> nextRisk = new ArrayList<>();
        ArrayList<String> nextContact = new ArrayList<>();
        ArrayList<ArrayList<TreeMap<LocalDateTime, Integer>>> trees = new ArrayList<>();
        //simulate
        for (int i=0; i<maxReps; i++) {
            TreeMap<LocalDateTime, ArrayList<Object>> tmp = new TreeMap<>();
            for (int j=0; j<np_startDates.size(); j++){
                for (int l=0; l<np_startDates.get(j).length; l++){
                    ArrayList<Object> tmp_obj = new ArrayList<>();
                    tmp_obj.add(np_risks.get(j)[l]);
                    tmp_obj.add(np_contact.get(j)[l]);
                    //27-10-2022
                    LocalDateTime workaroundDate = LocalDateTime.from(np_startDates.get(j)[l]);
                    while(tmp.get(workaroundDate) != null){
                        workaroundDate = workaroundDate.plusNanos(1);
                    }
                    tmp.put(workaroundDate, tmp_obj);
                }
            }

            for (int j=0; j<nc_startDates.length; j++){
                    ArrayList<Object> tmp_obj = new ArrayList<>();
                    tmp_obj.add(nc_risks[j]);
                    tmp_obj.add(subjects);
                    tmp.put(nc_startDates[j], tmp_obj);
            }

            nextContact.clear();
            nextContactTime.clear();
            nextRisk.clear();
            //System.out.println("tmp.size: " + tmp.size());
            int tmp_size = tmp.size();
            for (int key_index=0; key_index<tmp_size; key_index++){
                LocalDateTime current_key = tmp.firstKey();
                //System.out.println("current key " + current_key);
                //FIXME rimuovere solo la prima occorrenza
                ArrayList<Object> tmp_obj = tmp.remove(current_key); //IL REMOVE CANCELLA TUTTE LE DATE UGUALI A current_key QUINDI SE CI SONO DOPPIONI CANCELLA TUTTO
                if (tmp_obj.get(1).equals(subjects)){ //tmp_obj.get(1).equals(subjects) XXX
                    for (int s=0; s<subjects.size(); s++){
                        nextContactTime.add(current_key.truncatedTo(ChronoUnit.MINUTES));
                        nextRisk.add((Float)tmp_obj.get(0));
                        nextContact.add(subjects.get(s));
                    }
                }
                else {
                    //System.out.println("current risk " + tmp_obj.get(0));
                    //System.out.println("current contact " + tmp_obj.get(1));
                    nextContactTime.add(current_key.truncatedTo(ChronoUnit.MINUTES));
                    nextRisk.add((Float)tmp_obj.get(0));
                    nextContact.add((String)tmp_obj.get(1));
                }
            }
            ArrayList<Integer> ids = new ArrayList<>();
            //System.out.println("nextContact.size: " + nextContact.size());
            for (int id = 0; id < nextContact.size(); id++) {
                ids.add(id, id);
            }
            ArrayList<Integer> idsInfected = new ArrayList<>();
            for (int p=0; p<np; p++){
                Integer idInfected = null;
                idsInfected.add(idInfected);
            }
            ArrayList<TreeMap<LocalDateTime, Integer>> states = new ArrayList<>();
            for (int p=0; p<np; p++){
                TreeMap<LocalDateTime, Integer> date_state = new TreeMap<>();
                states.add(p, date_state);
            }
            // System.out.println("states.size: " + states.size());
            while (nextContactTime.size() > 0) {
                String nc = nextContact.remove(0);
                LocalDateTime nct = nextContactTime.remove(0);
                int event_id = ids.remove(0);
                //System.out.println("event " + event_id);
                //System.out.println("nc " + nc);

                for (int p=0; p<np; p++){
                    if (nc.equals("P" + p)){
                        ArrayList<Integer> pStates = new ArrayList<>();
                        for(Map.Entry<LocalDateTime, Integer> entry : states.get(p).entrySet()) {
                            Integer value = entry.getValue();
                            pStates.add(value);
                        }
                        int current_state = pStates.size() == 0 ? 0 : pStates.get(pStates.size() - 1); //XXX
                        if (current_state == 0 && idsInfected.get(p) == null) { //sano
                            float random = 0 + r.nextFloat() * (1 - 0);
                            if (random < nextRisk.remove(0)) { //Da sano a contagiato
                                idsInfected.add(p, event_id);
                                //System.out.println("da sano a contagiato evento " + event_id);
                                LocalDateTime timeContagious = getSampleCC(nct, 12, 36);
                                int position = 0;
                                for (int iterator = 0; iterator < nextContactTime.size(); iterator++) {
                                    if (nextContactTime.get(iterator).isBefore(timeContagious))
                                        position = iterator + 1;
                                }
                                nextContactTime.add(position, timeContagious);
                                nextContact.add(position, nc);
                                ids.add(position, event_id);
                                states.get(p).put(nct, 1);
                            } else {// resta sano
                                //System.out.println("da sano a sano evento " + event_id);
                                states.get(p).put(nct, 0);
                            }
                        } else if (idsInfected.get(p) != null && current_state == 1 && idsInfected.get(p) == event_id) { //contagiato //XXX
                            //da contagiato a contagioso
                            //System.out.println("da contagiato a contagioso evento " + event_id);
                            LocalDateTime timeHealing = getSampleCH(nct);
                            int position = 0;
                            for (int iterator = 0; iterator < nextContactTime.size(); iterator++) {
                                if (nextContactTime.get(iterator).isBefore(timeHealing))
                                    position = iterator + 1;
                            }
                            nextContactTime.add(position, timeHealing);
                            nextContact.add(position, nc);
                            states.get(p).put(nct, 2);
                            ids.add(position, event_id);

                        } else if (idsInfected.get(p) != null && current_state == 2 && idsInfected.get(p) == event_id) { //contagioso
                            //da contagioso a guarito
                            //System.out.println("da contagiato a guarito evento " + event_id);
                            states.get(p).put(nct, 3);
                        }
                    }
                }
            }
            //filling
            ArrayList<TreeMap<LocalDateTime, Integer>> treesIIteration = new ArrayList<>();
            for(int p = 0; p<states.size(); p++) {
                treesIIteration.add(convert(fill(states.get(p), t0)));
            }
            trees.add(treesIIteration);
            /*for(int p = 0; p<treesIIteration.size(); p++) {
                for(Map.Entry<LocalDateTime, Integer> entry : states.get(p).entrySet()) {
                    Integer value = entry.getValue();
                    //System.out.println("states di " + p + " " + value);
                }
                for(Map.Entry<LocalDateTime, Integer> entry : treesIIteration.get(p).entrySet()) {
                    Integer value = entry.getValue();
                    //System.out.println("filled states di " + p + " " + value);
                }
            }*/
        }
        //System.out.println(trees.size() + " " + trees.get(0).size());

        var indices = trees.get(0).get(0).keySet().toArray();
        // System.out.println(Arrays.toString(indices));
        HashMap<String, ArrayList<TreeMap<LocalDateTime, Integer>>> treeHM = new HashMap<>();
        for(int p = 0; p<np; p++){
            ArrayList<TreeMap<LocalDateTime, Integer>> t = new ArrayList<>();
            for(int i = 0; i<trees.size(); i++){
                t.add(trees.get(i).get(p));
            }
            treeHM.put("P"+p, t);
        }
        HashMap<String, TreeMap<LocalDateTime, Double>> tt = new HashMap();
        for(String subject : treeHM.keySet()){
            TreeMap<LocalDateTime, Double> t = new TreeMap<>();
            for(int j = 0; j<indices.length; j++) {
                double currentT = 0;
                for (int i = 0; i < treeHM.get(subject).size(); i++) {
                    currentT += treeHM.get(subject).get(i).get((LocalDateTime) indices[j]);
                }
                t.put((LocalDateTime) indices[j], currentT/treeHM.get(subject).size());
            }
            tt.put(subject, t);
        }
        /*
        var indices = trees1[0].keySet().toArray();
        IntStream.range(0, trees1[0].size()).parallel().forEach(i->{
            double currentT = 0;
            for (int j = 0; j<trees1.length; j++) {
                if((LocalDateTime) indices[i] != null)
                    currentT += trees1[j].get((LocalDateTime) indices[i]);
            }
            currentT/=(double)maxReps;
            t1.put((LocalDateTime) indices[i], currentT);

            currentT = 0;
            for (int j = 0; j<trees2.length; j++) {
                if((LocalDateTime) indices[i] != null)
                    currentT += trees2[j].get((LocalDateTime) indices[i]);
            }
            currentT/=(double)maxReps;
            t2.put((LocalDateTime) indices[i], currentT);
        });
        */
        for(int i = 0; i<tt.size(); i++) {
            TreeMap<LocalDateTime, Integer>[] treesOfASubject = new TreeMap[treeHM.size()];
            for(int j = 0; j<treeHM.size(); j++){
                treesOfASubject[j] = treeHM.get("P"+i).get(j);
            }
            var v = getVariance(treesOfASubject, tt.get("P"+i));
            var ci = getConfidenceIntervalOffset(v, maxReps);
            for (LocalDateTime l : v.keySet()) {
                System.out.println(i+" "+l + " " + v.get(l) + " -> " + tt.get("P"+i).get(l) + " +- " + ci.get(l));
            }
        }

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
        // System.out.println("---");
        // System.out.println(clusterSubjectsMet.keySet());
        // System.out.println(subjects.get(0));
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

        //for(LocalDateTime l : tt.get("P0").keySet())
        //System.out.println(tt.get("P0").get(l));
        plot(tt, pns);

    }

    @Test
    void simulate(){
        try{
        Random r = new Random();
        LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(6);
        //creazione dei soggetti
        ArrayList<Subject> subjects = new ArrayList<>();
        for(int p = 0; p < np; p++){
            subjects.add(new Subject("P"+p));
        }
        //creazione degli eventi ambientali
        ArrayList<Event> events = new ArrayList<>();
        for(int p = 0; p < np; p++){
            int nEnvironment = r.nextInt(max_nEnvironment - min_nEnvironment) + min_nEnvironment;
            LocalDateTime[] startDates = new LocalDateTime[nEnvironment];
            LocalDateTime[] endDates = new LocalDateTime[nEnvironment];
            ArrayList<LocalDateTime> dates = new ArrayList<>(generateDates(t0, nEnvironment));

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

            String[] riskLevels = generateRiskLevels(nEnvironment);
            Boolean[] masks = generateMasks(nEnvironment);

            for(int i = 0; i < nEnvironment; i++){
                ArrayList<Subject> s = new ArrayList<Subject>(Collections.singletonList(subjects.get(p)));
                ArrayList<String> s_String = new ArrayList<>();
                for(Subject sub : s){
                    s_String.add(sub.getName());
                }
                Type t = new Environment(masks[i], riskLevels[i], startDates[i], endDates[i]);
                events.add(new Event(startDates[i], endDates[i], t, s));

                observationDAO.insertObservation(s_String, t, startDates[i], endDates[i]);
            }
        }

            String[] nc_riskLevels;
            Boolean[] nc_masks;
            LocalDateTime[] nc_startDates = new LocalDateTime[nContact];
            LocalDateTime[] nc_endDates = new LocalDateTime[nContact];
            ArrayList<LocalDateTime> dates = new ArrayList<>(generateDates(t0, nContact));
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
            nc_riskLevels = generateRiskLevels(nContact);
            nc_masks = generateMasks(nContact);

         for(int c = 0; c<nContact; c++){
             ArrayList<String> s_String = new ArrayList<>();
             for(Subject sub : subjects){
                 s_String.add(sub.getName());
             }
             Type t = new Contact(s_String, nc_masks[c], nc_riskLevels[c], nc_startDates[c], nc_endDates[c]);
             events.add(new Event(nc_startDates[c], nc_endDates[c], t, subjects));
             observationDAO.insertObservation(s_String, t, nc_startDates[c], nc_endDates[c]);
         }
         Collections.sort(events);
         /*for(Event e : events){
             if(e.getSubject().size() == 1)
                System.out.println(e.getStartDate() + " " + e.getSubject().get(0).getName());
             else{
                 for(Subject s : e.getSubject())
                    System.out.println(e.getStartDate()+" "+s.getName());
             }
         }*/
         //fine generazione eventi
            ArrayList<Event> eventsCopy = new ArrayList<>(events);
        ArrayList<ArrayList<TreeMap<LocalDateTime, Integer>>> timestamps = new ArrayList<>();
        for(int rep = 0; rep<maxReps; rep++){
                events = new ArrayList<>(eventsCopy);
                for(Subject subject : subjects)
                    subject.initializeSubject(t0);
                while(events.size() > 0){
                    Collections.sort(events);
                    Event event = events.remove(0);
                    if(event.getType() instanceof Environment){
                        Subject subject = event.getSubject().get(0);
                        if(subject.getCurrentState() == 0){
                            float d = r.nextFloat();
                            if(d < ((Environment) event.getType()).getRiskLevel()){
                                LocalDateTime ldt = getSampleCC(event.getStartDate(), 12, 36);
                                subject.changeState(event.getStartDate());
                                //rescheduling dell'evento "subject" diventa contagioso
                                events.add(new ChangeStateEvent(ldt, event.getSubject()));
                            }
                        }
                    }else if(event.getType() instanceof Contact){
                        ArrayList<Subject> ss = event.getSubject();
                        boolean isThereAtLeastOneContagious = false;
                        for(Subject subject : ss){
                            if(subject.getCurrentState() == 2)
                                isThereAtLeastOneContagious = true;
                        }
                        if(isThereAtLeastOneContagious){
                            for(Subject subject : ss){
                                if(subject.getCurrentState()==0){
                                    float d = r.nextFloat();
                                    if(d < ((Contact) event.getType()).getRiskLevel()){
                                        LocalDateTime ldt = getSampleCC(event.getStartDate(), 12, 36);
                                        subject.changeState(event.getStartDate());
                                        //rescheduling dell'evento "subject" diventa contagioso
                                        events.add(new ChangeStateEvent(ldt, new ArrayList<>(Collections.singletonList(subject))));
                                    }
                                }
                            }
                        }
                    }else if(((ChangeStateEvent) event) instanceof ChangeStateEvent){
                        Subject subject = event.getSubject().get(0);
                        switch (subject.getCurrentState()) {
                            case 1 -> {
                                subject.changeState(event.getStartDate());
                                //rescheduling dell'evento "subject" «guarisce»
                                events.add(new ChangeStateEvent(getSampleCH(event.getStartDate()), event.getSubject()));
                            }
                            case 2 -> subject.changeState(event.getStartDate());
                            default -> System.out.println("error");
                        }
                    }
                }
                //filling
                //XXX copiato dalla vecchia funzione
                ArrayList<TreeMap<LocalDateTime, Integer>> timestampsAtIthIteration = new ArrayList<>();
                for(Subject subject : subjects) {
                    timestampsAtIthIteration.add(convert(fill(subject.getTimestamps(), t0)));
                }
                timestamps.add(timestampsAtIthIteration);
            }
        //XXX copiato dalla vecchia funzione
        var indices = timestamps.get(0).get(0).keySet().toArray();
        // System.out.println(Arrays.toString(indices));
        HashMap<String, ArrayList<TreeMap<LocalDateTime, Integer>>> treeHM = new HashMap<>();
        for(int p = 0; p<np; p++){
            ArrayList<TreeMap<LocalDateTime, Integer>> t = new ArrayList<>();
            for(int i = 0; i<timestamps.size(); i++){
                t.add(timestamps.get(i).get(p));
            }
            treeHM.put("P"+p, t);
        }
        HashMap<String, TreeMap<LocalDateTime, Double>> tt = new HashMap<>();
        for(String subject : treeHM.keySet()){
            TreeMap<LocalDateTime, Double> t = new TreeMap<>();
            for(int j = 0; j<indices.length; j++) {
                double currentT = 0;
                for (int i = 0; i < treeHM.get(subject).size(); i++) {
                    currentT += treeHM.get(subject).get(i).get((LocalDateTime) indices[j]);
                }
                t.put((LocalDateTime) indices[j], currentT/treeHM.get(subject).size());
            }
            tt.put(subject, t);
        }
        //calcolo degli intervalli
            for(int i = 0; i<tt.size(); i++) {
                TreeMap<LocalDateTime, Integer>[] treesOfASubject = new TreeMap[treeHM.size()];
                for(int j = 0; j<treeHM.size(); j++){
                    treesOfASubject[j] = treeHM.get("P"+i).get(j);
                }
                var v = getVariance(treesOfASubject, tt.get("P"+i));
                var ci = getConfidenceIntervalOffset(v, maxReps);
                for (LocalDateTime l : v.keySet()) {
                    System.out.println(i+" "+l + " " + v.get(l) + " -> " + tt.get("P"+i).get(l) + " +- " + ci.get(l));
                }
            }

            //PN
            ArrayList<HashMap<String, TransientSolution>> pns = new ArrayList<>();
            final int max_iterations = subjects.size()<=2?subjects.size()-1:2;
            HashMap<String, ArrayList<HashMap<String, Object>>> clusterSubjectsMet = new HashMap<>();
            ArrayList<String> subjects_String = new ArrayList<>();
            for(Subject subject : subjects){
                subjects_String.add(subject.getName());
            }
            if(max_iterations>0){
                for(int i = 0; i<subjects.size(); i++){
                    ArrayList<String> otherMembers = new ArrayList<>(subjects_String);
                    otherMembers.remove(i);
                    clusterSubjectsMet.put(subjects_String.get(i), observationDAO.getContactObservations(subjects_String.get(i), otherMembers));
                }
            }
            // System.out.println("---");
            // System.out.println(clusterSubjectsMet.keySet());
            // System.out.println(subjects.get(0));
            for(String s : clusterSubjectsMet.keySet()){
                System.out.println(s+ " "+ clusterSubjectsMet.get(s));
            }
            System.out.println("---");
            for(int nIteration = 0; nIteration<= max_iterations; nIteration++){
                HashMap<String, TransientSolution> pits = new HashMap<>();//p^it_s
                for(String member : subjects_String){
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

            //for(LocalDateTime l : tt.get("P0").keySet())
            //System.out.println(tt.get("P0").get(l));
            plot(tt, pns);
        }catch(Exception e){
           e.printStackTrace();
        } finally {
            clean();
        }
    }

    private ArrayList<LocalDateTime> generateDates(LocalDateTime t0, int nEvent){
        ArrayList<LocalDateTime> dates = new ArrayList<>();
        for (int i=0; i<(nEvent*2); i++){
            Random r = new Random();
            int hours = r.nextInt(144-0)+0;
            LocalDateTime date = t0.plusHours(hours);
            dates.add(date);
        }
        Collections.sort(dates);
        return dates;
    }

    private String[] generateRiskLevels(int nEvent){
        Random r = new Random();
        String[] risk_levels = new String[nEvent];
        for (int i=0; i<nEvent; i++){
            String string_risk_level = "";
            int risk_number = r.nextInt(3 - 1) + 1;
            if (risk_number == 1)
                string_risk_level = "High";
            else if (risk_number == 2)
                string_risk_level = "Medium";
            else if (risk_number == 3)
                string_risk_level = "Low";
            risk_levels[i] = string_risk_level;
        }
        return risk_levels;
    }

    private Boolean[] generateMasks(int nEvent){
        Random r = new Random();
        Boolean[] masks = new Boolean[nEvent];
        for (int i=0; i<nEvent; i++) {
            int number = r.nextInt();
            masks[i] = number % 2 == 0;
        }
        return masks;
    }

    private LocalDateTime getSampleCC(LocalDateTime date, int min, int max) {
       WeibullDistribution w = new WeibullDistribution(3.2, (double)13);
        double offset = w.sample() + min;
        if(offset>max)
            offset = max;/*
        NormalDistribution n = new NormalDistribution(24, 3.5);
        double offset = n.sample();*/
        int hours = (int)Math.floor(offset);
        return date.plusHours(hours);
        /*Random r = new Random();
        int randomHours = min + r.nextInt(max - min);
        return date.plusHours(randomHours);*/
    }

    private LocalDateTime getSampleCH(LocalDateTime date) {
        Random r = new Random();
        double result = Math.log(1-r.nextFloat())/(-0.04);
        int randomHours = (int) Math.floor(result);
        return date.plusHours(randomHours);
    }

    @AfterAll
    static void clean(){
        for(int p=0; p<np; p++) {
            ArrayList<HashMap<String, Object>> obs = observationDAO.getObservations("P"+p);
            for (int i = 0; i < obs.size(); i++) {
                ObservationDAOTest.deleteObservation(obs.get(i).get("id").toString());
            }
        }
    }

    private TreeMap<LocalDateTime,Integer> fill(TreeMap<LocalDateTime, Integer> treeMap, LocalDateTime t0){
        LocalDateTime t = LocalDateTime.from(t0);
        final int hours = 1;
        //trovo 1 e metto tutti 1, poi trovo 2 e metto tutti 2, poi trovo 3 e metto tutti 3 fine.
       /* for(Map.Entry<LocalDateTime, Integer> entry : treeMap.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());*/
        LocalDateTime tLimit = LocalDateTime.from(t).plusDays(6);
        TreeMap<LocalDateTime, Integer> state = new TreeMap<>();
        int currentState = 0;
        while(t.isBefore(tLimit) && treeMap.size() > 0){

            if(ChronoUnit.HOURS.between(t, treeMap.firstKey())<hours){
                currentState = currentState<treeMap.firstEntry().getValue() && treeMap.firstEntry().getValue() == currentState+1 ?treeMap.firstEntry().getValue():currentState;
                //CERTO CHE AMI COMPLICARTI LA VITA. UN SEMPLICE IF TI FACEVA SCHIFO?
                treeMap.remove(treeMap.firstKey());
            }
            state.put(t, currentState);
            t = t.plusHours(hours);
        }
        //complete
        //SECONDO ME LA PARTE SOTTO è RIDONDANTE
        currentState = treeMap.size()==0 ? 0 : state.lastEntry().getValue();
        while(t.isBefore(tLimit)){
            state.put(t, currentState);
            t = t.plusHours(hours);
        }
       /* System.out.println("---");
        System.out.println("---");
       for(Map.Entry<LocalDateTime, Integer> entry : state.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());
        System.out.println("---");*/
        return state;
    }

    private TreeMap<LocalDateTime, Integer> convert(TreeMap<LocalDateTime, Integer> state){
        TreeMap<LocalDateTime, Integer> binarizedStates = new TreeMap<>(state);
        var indices = binarizedStates.keySet().toArray();
        IntStream.range(0, state.size()).parallel().forEach(i -> {
            if(binarizedStates.get(indices[i]) == 2)
                binarizedStates.put((LocalDateTime) indices[i], 1);
            else
                binarizedStates.put((LocalDateTime) indices[i], 0);
        });
        return binarizedStates;
    }

    private TreeMap<LocalDateTime, Double> getVariance(TreeMap<LocalDateTime, Integer>[] trees, TreeMap<LocalDateTime, Double> meanTree){
        ArrayList<TreeMap<LocalDateTime, Double>> tt = new ArrayList<>();
        IntStream.range(0, trees.length).parallel().forEach(i->{
            TreeMap<LocalDateTime, Double> t = new TreeMap<>();
            for(LocalDateTime l : trees[i].keySet())
                t.put(l, Math.pow((trees[i].get(l) - meanTree.get(l)), 2));
            tt.add(t);
        });
        System.out.println(tt.size());
        TreeMap<LocalDateTime, Double> output = new TreeMap<>();
        for(LocalDateTime l : trees[0].keySet()) {
            double sum = 0;
            for (int i = 0; i < tt.size(); i++) {
                sum+=tt.get(i).get(l);
            }
            output.put(l, sum/(tt.size()-1));
        }
        return output;
    }
    private TreeMap<LocalDateTime, Double> getConfidenceIntervalOffset (TreeMap<LocalDateTime, Double> s2, int n){
        final double z95 = 1.96;
        TreeMap<LocalDateTime, Double> offsets = new TreeMap<>();
        for(LocalDateTime l : s2.keySet()){
            offsets.put(l, z95*Math.sqrt(s2.get(l)/n));
        }
        return offsets;
    }
}
