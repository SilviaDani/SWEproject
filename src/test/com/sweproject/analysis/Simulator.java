package com.sweproject.analysis;

import com.github.sh0nk.matplotlib4j.NumpyUtils;
import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonConfig;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.opencsv.CSVWriter;
import com.sun.source.tree.Tree;
import com.sweproject.controller.UIController;
import com.sweproject.dao.ObservationDAO;
import com.sweproject.dao.ObservationDAOTest;
import com.sweproject.model.Contact;
import com.sweproject.model.Environment;
import com.sweproject.model.Type;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
import org.jfree.data.io.CSV;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.oristool.models.stpn.TransientSolution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.sweproject.main.Main.DEBUG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.google.common.base.Stopwatch;
import org.testfx.framework.junit5.Stop;

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
//TODO add timer
public class Simulator extends UIController {
    int samples = 144;
    int steps = 1;
    final int maxReps = 100000;
    private static ObservationDAO observationDAO;
    private STPNAnalyzer stpnAnalyzer;
    String PYTHON_PATH;
    static final int np = 6;
    int nContact = 20;
    int max_nEnvironment = 20;
    int min_nEnvironment = 15;
    File execTimes;
    File confInt;
    FileWriter outputFile;
    CSVWriter writer;
    ArrayList<String[]> outputStrings_execTimes;
    ArrayList<String[]> outputStrings_confInt;
    Stopwatch timer;


    Simulator(){
        observationDAO = new ObservationDAO();
        stpnAnalyzer = new STPNAnalyzer(samples, steps);
        timer = Stopwatch.createUnstarted();
        if(DEBUG){
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            execTimes = new File("./execTimes_"+now.toString().replace(':', '_')+".csv");
            confInt = new File("./confInt_"+now.toString().replace(':', '_')+".csv");
            outputStrings_execTimes = new ArrayList<>();
            outputStrings_confInt = new ArrayList<>();
            outputStrings_confInt.add(new String[]{"Persona", "Timestamp", "Limite inferiore", "Limite superiore"});
        }
    }

    void plot(HashMap<String, TreeMap<LocalDateTime, Double>> tt) throws PythonExecutionException, IOException {
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
            plt.plot().add(x, tYSampled1).label("Sim "+codes[j]);
            plt.legend();
        }
        plt.xlim(Collections.min(x) * 1.1, Collections.max(x) * 1.1);
        plt.ylim(-0.1,1.1);
        if(DEBUG){
            timer.stop();
            System.out.println("Tempo per eseguire il plot dei dati: "+timer);
        }
        plt.show();
    }
    void plot(HashMap<String, TreeMap<LocalDateTime, Double>> tt, ArrayList<HashMap<String, TransientSolution>> ss) throws PythonExecutionException, IOException {
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
                //value1=ss.get(ss.size()-1).get(finalCodes[finalJ]).getSolution()[i][r][0];
                yPNarray1[i] = value1;
            });
            yPN1 = Arrays.stream(yPNarray1).toList();
            plt.plot().add(x, tYSampled1).label("Sim "+codes[j]);
            plt.plot().add(x, yPN1).label("PN "+codes[j]);
            plt.legend();
        }
        plt.xlim(Collections.min(x) * 1.1, Collections.max(x) * 1.1);
        plt.ylim(-0.1,1.1);
        if(DEBUG){
            timer.stop();
            outputStrings_execTimes.add(new String[]{"Tempo per eseguire il plot dei dati", String.valueOf(timer)});
            outputFile = new FileWriter(execTimes);
            writer = new CSVWriter(outputFile, ';',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            System.out.println(outputStrings_execTimes.get(0)[0] +" "+ outputStrings_execTimes.get(0)[1]);
            writer.writeAll(outputStrings_execTimes);
            writer.close();
            outputFile = new FileWriter(confInt);
            writer=new CSVWriter(outputFile, ';',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            writer.writeAll(outputStrings_confInt);
            writer.close();
        }
        plt.show();
    }

    @Test
    void simulate(){
        try{
        if(DEBUG){
            timer.start();
            }
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
        if(DEBUG){
            timer.stop();
            outputStrings_execTimes.add(new String[]{"Tempo per creare le osservazioni e caricarle sul database", String.valueOf(timer)});
            timer.reset();
        }
         //fine generazione eventi
            ArrayList<Event> eventsCopy = new ArrayList<>(events);
            HashMap<String, TreeMap<LocalDateTime, Double>> meanTrees = new HashMap<>(); //TODO initialize this
            for(Subject subject : subjects){
                TreeMap<LocalDateTime, Double> tmpTree = new TreeMap<>();
                for(int offset = 0; offset<144; offset++){
                    tmpTree.put(LocalDateTime.from(t0).plusHours(offset), 0.0);
                }
                meanTrees.put(subject.getName(), tmpTree);
            }
//        ArrayList<ArrayList<TreeMap<LocalDateTime, Integer>>> timestamps = new ArrayList<>();
            if(DEBUG) {
                timer.start();
            }
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
                HashMap<String, TreeMap<LocalDateTime, Integer>> timestampsAtIthIteration = new HashMap<>();
                for(Subject subject : subjects) {
                    timestampsAtIthIteration.put(subject.getName() ,convert(fill(subject.getTimestamps(), t0)));
                }
                for(Subject subject : subjects){
                    for(LocalDateTime ldt : meanTrees.get(subject.getName()).keySet()){
                        double newValue = (meanTrees.get(subject.getName()).get(ldt) * (rep) + timestampsAtIthIteration.get(subject.getName()).get(ldt))/(rep+1);
                        meanTrees.get(subject.getName()).replace(ldt, newValue);
                    }
                }
                //timestamps.add(timestampsAtIthIteration);
            }
        if(DEBUG){
            timer.stop();
            outputStrings_execTimes.add(new String[]{"Tempo per eseguire "+ maxReps + " ripetizioni della simulazione", String.valueOf(timer)});
            outputStrings_execTimes.add(new String[]{"Tempo medio per eseguire una ripetizione della simulazione", (timer.elapsed(TimeUnit.MILLISECONDS)/maxReps)+"ms"});
            timer.reset();
        }
        if(DEBUG)
            timer.start();
        for(Subject subject : subjects){
            for(LocalDateTime ldt : meanTrees.get(subject.getName()).keySet()){
                double theta = meanTrees.get(subject.getName()).get(ldt); // hat{theta} = mean(x).   mean(X) ~ N(theta, theta(1-theta)/n) per TLC
                double z95bi = 1.96;
                double offset = (1.96 * Math.sqrt(theta * (1-theta)/maxReps));
                outputStrings_confInt.add(new String[]{subject.getName(), String.valueOf(ldt), String.valueOf((theta - offset)), String.valueOf((theta+offset))});
            }
        }
        if(DEBUG){
            timer.stop();
            outputStrings_execTimes.add(new String[]{"Tempo per calcolare gli intervalli di confidenza", String.valueOf(timer)});
            timer.reset();
        }

            //PN
            if(DEBUG){
                timer.start();
            }
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

            HashMap<String, ArrayList<HashMap<String, Object>>> envObs = new HashMap<>();
            for(String member : subjects_String){
                envObs.put(member, observationDAO.getEnvironmentObservations(member));
            }
            System.out.println("---");
            if(DEBUG){
                timer.stop();
                outputStrings_execTimes.add(new String[]{"Tempo per fare retrieval dei dati dal database", String.valueOf(timer)});
                timer.reset();
                timer.start();
            }
            for(int nIteration = 0; nIteration<= max_iterations; nIteration++){
                HashMap<String, TransientSolution> pits = new HashMap<>();//p^it_s
                for(String member : subjects_String){
                    //System.out.println(member + " it:"+nIteration + " started");
                    if(nIteration==0){
                        try {
                            pits.put(member, stpnAnalyzer.makeModel(member, envObs.get(member)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else{
                        pits.put(member, stpnAnalyzer.makeClusterModel(pns.get(nIteration-1), clusterSubjectsMet.get(member)));
                    }
                    //System.out.println(member + " it:"+nIteration + " completed");
                }
                pns.add(pits);
            }
            if(DEBUG){
                timer.stop();
                outputStrings_execTimes.add(new String[]{"Tempo per eseguire analisi mediante reti di Petri", String.valueOf(timer)});
                timer.reset();
                timer.start();
            }
            plot(meanTrees, pns);
        }catch(Exception e){
           e.printStackTrace();
        } finally {
            clean();
        }
    }

    @Test
    void simulateButChangingTheOrder(){
        try{
            if(DEBUG){
                timer.start();
            }
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
            if(DEBUG){
                timer.stop();
                outputStrings_execTimes.add(new String[]{"Tempo per creare le osservazioni e caricarle sul database", String.valueOf(timer)});
                timer.reset();
            }
            //fine generazione eventi

            //PN
            if(DEBUG){
                timer.start();
            }
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

            HashMap<String, ArrayList<HashMap<String, Object>>> envObs = new HashMap<>();
            for(String member : subjects_String){
                envObs.put(member, observationDAO.getEnvironmentObservations(member));
            }
            System.out.println("---");
            if(DEBUG){
                timer.stop();
                outputStrings_execTimes.add(new String[]{"Tempo per fare retrieval dei dati dal database", String.valueOf(timer)});
                timer.reset();
                timer.start();
            }
            for(int nIteration = 0; nIteration<= max_iterations; nIteration++){
                HashMap<String, TransientSolution> pits = new HashMap<>();//p^it_s
                for(String member : subjects_String){
                    //System.out.println(member + " it:"+nIteration + " started");
                    if(nIteration==0){
                        try {
                            pits.put(member, stpnAnalyzer.makeModel(member, envObs.get(member)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else{
                        pits.put(member, stpnAnalyzer.makeClusterModel(pns.get(nIteration-1), clusterSubjectsMet.get(member)));
                    }
                    //System.out.println(member + " it:"+nIteration + " completed");
                }
                pns.add(pits);
            }
            long timeLimit;
            if(DEBUG){
                timer.stop();
                timeLimit=timer.elapsed(TimeUnit.NANOSECONDS);
                outputStrings_execTimes.add(new String[]{"Tempo per eseguire analisi mediante reti di Petri", String.valueOf(timer)});
                timer.reset();
            }
            Stopwatch simTimer = Stopwatch.createUnstarted();
            ArrayList<Event> eventsCopy = new ArrayList<>(events);
            HashMap<String, TreeMap<LocalDateTime, Double>> meanTrees = new HashMap<>(); //TODO initialize this
            for(Subject subject : subjects){
                TreeMap<LocalDateTime, Double> tmpTree = new TreeMap<>();
                for(int offset = 0; offset<144; offset++){
                    tmpTree.put(LocalDateTime.from(t0).plusHours(offset), 0.0);
                }
                meanTrees.put(subject.getName(), tmpTree);
            }
//        ArrayList<ArrayList<TreeMap<LocalDateTime, Integer>>> timestamps = new ArrayList<>();
            if(DEBUG) {
                timer.start();
                simTimer.start();
            }
            int rep = 0;
            while(simTimer.elapsed(TimeUnit.NANOSECONDS) < timeLimit){
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
                HashMap<String, TreeMap<LocalDateTime, Integer>> timestampsAtIthIteration = new HashMap<>();
                for(Subject subject : subjects) {
                    timestampsAtIthIteration.put(subject.getName() ,convert(fill(subject.getTimestamps(), t0)));
                }
                for(Subject subject : subjects){
                    for(LocalDateTime ldt : meanTrees.get(subject.getName()).keySet()){
                        double newValue = (meanTrees.get(subject.getName()).get(ldt) * (rep) + timestampsAtIthIteration.get(subject.getName()).get(ldt))/(rep+1);
                        meanTrees.get(subject.getName()).replace(ldt, newValue);
                    }
                }
                rep++;
            }
            if(DEBUG){
                timer.stop();
                simTimer.stop();
                System.out.println(rep);
                outputStrings_execTimes.add(new String[]{"Tempo per eseguire "+ maxReps + " ripetizioni della simulazione", String.valueOf(timer)});
                outputStrings_execTimes.add(new String[]{"Tempo medio per eseguire una ripetizione della simulazione", (timer.elapsed(TimeUnit.MILLISECONDS)/maxReps)+"ms"});
                timer.reset();
            }
            if(DEBUG)
                timer.start();
            for(Subject subject : subjects){
                for(LocalDateTime ldt : meanTrees.get(subject.getName()).keySet()){
                    double theta = meanTrees.get(subject.getName()).get(ldt); // hat{theta} = mean(x).   mean(X) ~ N(theta, theta(1-theta)/n) per TLC
                    double offset = (1.96 * Math.sqrt(theta * (1-theta)/maxReps));
                    outputStrings_confInt.add(new String[]{subject.getName(), String.valueOf(ldt), String.valueOf((theta - offset)), String.valueOf((theta+offset))});
                }
            }
            if(DEBUG){
                timer.stop();
                outputStrings_execTimes.add(new String[]{"Tempo per calcolare gli intervalli di confidenza", String.valueOf(timer)});
                timer.reset();
                timer.start();
            }

            plot(meanTrees, pns);
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
            int hours = r.nextInt(143-0)+1;
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
       WeibullDistribution w = new WeibullDistribution(3.5, (double)13);
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
}
