package com.sweproject.analysis;

import com.github.sh0nk.matplotlib4j.NumpyUtils;
import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonConfig;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.opencsv.*;
import com.sweproject.controller.UIController;
import com.sweproject.gateway.AccessGateway;
import com.sweproject.gateway.AccessGatewayTest;
import com.sweproject.gateway.ObservationGateway;
import com.sweproject.gateway.ObservationGatewayTest;
import com.sweproject.model.*;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.oristool.models.stpn.TransientSolution;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.sweproject.main.Main.DEBUG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.google.common.base.Stopwatch;

class Subject{
    private int currentState;
    private TreeMap<LocalDateTime, Integer> relevantTimestamps;
    private String name;
    private boolean isSymptomatic;
    private boolean showsCovidLikeSymptoms;

    public Subject(String name) {
        this.name = name;
        currentState = 0;
        relevantTimestamps = new TreeMap<>();

    }
    public void initializeSubject(LocalDateTime ldt){
        currentState = 0;
        relevantTimestamps.clear();
        relevantTimestamps.put(ldt, 0);
        isSymptomatic = false;
        showsCovidLikeSymptoms = false;
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

    public void setSymptomatic(boolean symptomatic) {
        isSymptomatic = symptomatic;
    }

    public boolean isSymptomatic() {
        return isSymptomatic;
    }

    public boolean hasCovidLikeSymptoms() {
        return showsCovidLikeSymptoms;
    }

    public void setShowsCovidLikeSymptoms(boolean showsCovidLikeSymptoms) {
        this.showsCovidLikeSymptoms = showsCovidLikeSymptoms;
    }
}
class Event implements Comparator<Event>, Comparable<Event>{
    protected LocalDateTime startDate;
    protected LocalDateTime endDate;
    protected Type type;
    protected ArrayList<Subject> subject;
    protected Boolean isPositive;
    protected String testType;

    public Event(LocalDateTime startDate, LocalDateTime endDate, Type type, ArrayList<Subject> subject, Boolean isPositive, String testType) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.subject = subject;
        this.isPositive = isPositive;
        this.testType = testType;
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
    public String getTestType() { return testType; }
    public Boolean getPositive() { return isPositive; }

    public ArrayList<Subject> getSubject() {
        return subject;
    }
    public void updateRiskLevel(float newLevel){
        if(type instanceof Environment){
            ((Environment)type).changeRiskLevel(newLevel);
        }else if(type instanceof Contact) {
            ((Contact)type).changeRiskLevel(newLevel);
        }
    }

    @Override
    public int compareTo(Event o) {
        return startDate.compareTo(o.startDate);
    }

}
class ChangeStateEvent extends Event{

    public ChangeStateEvent(LocalDateTime startDate, ArrayList<Subject> subject) {
        super(startDate, null, null, subject, null, null);
    }
}

public class Simulator extends UIController {
    int samples = 168;
    int steps = 1;
    final int maxReps = 100;
    boolean considerEnvironment = true;
    private static ObservationGateway observationGateway;
    public STPNAnalyzer_ext stpnAnalyzer;
    String PYTHON_PATH;
    static final int np = 5;
    int nContact = 20; //this number should be high (?)
    int max_nEnvironment = 10;
    int min_nEnvironment = 7;
    int max_nSymptoms = 2; //fixme
    int min_nSymptoms = 0;
    int max_nCovTests = 2; //fixme
    int min_nCovTests = 1;
    File execTimes;
    File confInt;
    File RMSE;
    FileWriter outputFile;
    CSVWriter writer;
    ArrayList<String[]> outputStrings_execTimes;
    ArrayList<String[]> outputStrings_confInt;
    ArrayList<String[]> outputStrings_RMSE;
    Stopwatch timer;
    Random r;

    WeibullDistribution wCont = new WeibullDistribution(3.5, (double)14);
    WeibullDistribution wSymp = new WeibullDistribution(2, 11);
    ExponentialDistribution eNotSymp = new ExponentialDistribution(25);
    ExponentialDistribution eSymp = new ExponentialDistribution(10);
    long seed = 11;



    Simulator(){
        observationGateway = new ObservationGateway();
        stpnAnalyzer = new STPNAnalyzer_ext(samples, steps);
        timer = Stopwatch.createUnstarted();
        if(DEBUG){
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            execTimes = new File("./execTimes_"+now.toString().replace(':', '_')+".csv");
            confInt = new File("./confInt_"+now.toString().replace(':', '_')+".csv");
            RMSE = new File("./RMSE_"+now.toString().replace(':', '_')+".csv");
            outputStrings_execTimes = new ArrayList<>();
            outputStrings_confInt = new ArrayList<>();
            outputStrings_RMSE = new ArrayList<>();
            outputStrings_confInt.add(new String[]{"Persona", "Timestamp", "Limite inferiore", "Limite superiore"});
            outputStrings_RMSE.add(new String[]{"Persona", "Radice dell'errore quadratico medio"});
        }
        r = new Random();
        r.setSeed(seed);
        wCont.reseedRandomGenerator(seed);
        wSymp.reseedRandomGenerator(seed);
        eNotSymp.reseedRandomGenerator(seed);
        eSymp.reseedRandomGenerator(seed);
    }

    void plot(HashMap<String, TreeMap<LocalDateTime, Double>> tt, HashMap<String, HashMap<Integer, Double>> ss) throws PythonExecutionException, IOException {
        boolean alternativePlot = false;
        String[] codes = new String[tt.size()];
        int index = 0;
        for(Object o : tt.keySet()){
            codes[index] = (String) o;
            index++;
        }
        List<Double> x = NumpyUtils.linspace(0, samples, samples);
        Plot plt = Plot.create(PythonConfig.pythonBinPathConfig(PYTHON_PATH));
        String[] finalCodes = codes;
        HashMap<String, List<Double>> hPN = new HashMap<>();
        double max = 0;
        for(int j = 0; j<codes.length; j++) {
            List<Double> tYSampled1;
            Double[] tYSampledArray1 = new Double[samples];
            int finalJ = j;
            IntStream.range(0, samples).parallel().forEach(i -> {
                if (i < tt.get(finalCodes[finalJ]).keySet().size()) {
                    if(alternativePlot)
                        tYSampledArray1[samples - i - 1] = tt.get(finalCodes[finalJ]).get((LocalDateTime) tt.get(finalCodes[finalJ]).keySet().toArray()[i]);
                    else
                        tYSampledArray1[i] = tt.get(finalCodes[finalJ]).get((LocalDateTime) tt.get(finalCodes[finalJ]).keySet().toArray()[i]);
                }
            });

            tYSampled1 = Arrays.stream(tYSampledArray1).toList();
            List<Double> yPN1;
            Double[] yPNarray1 = new Double[samples];
            IntStream.range(0, samples).parallel().forEach(i->{
                if(alternativePlot)
                    yPNarray1[samples - i - 1] = ss.get(codes[finalJ]).get(i);
                else
                    yPNarray1[i] = ss.get(codes[finalJ]).get(i);
            });
            yPN1 = Arrays.stream(yPNarray1).toList();
            hPN.put(codes[j], yPN1);
            String style = "solid";
            if(j>=5)
                style = "dashed";
            plt.plot().add(x, tYSampled1).label("Sim "+codes[j]).linestyle(style);
            plt.plot().add(x, yPN1).label("PN "+codes[j]).linestyle(style);
            System.out.println(Collections.max(yPN1) + " " + Collections.max(tYSampled1) + " " + Math.max(Collections.max(yPN1), Collections.max(tYSampled1)));
            max = Math.max(max, Math.max(Collections.max(yPN1), Collections.max(tYSampled1)));
            plt.legend();
        }
        plt.xlim(Collections.min(x) * 1.05, Collections.max(x) * 1.05);
        plt.ylim(- max * 0.1, max * 1.1);
        if(alternativePlot)
            plt.xlabel("Ore fa");
        else
            plt.xlabel("Ore");
        plt.ylabel("Rischio");
        if(DEBUG){
            timer.stop();
            outputStrings_execTimes.add(new String[]{"Tempo per eseguire il plot dei dati", String.valueOf(timer)});
            timer.start();
            for(String subject : tt.keySet()){
                double rmse = 0;
                int indexPN = 0;
                for(LocalDateTime ldt : tt.get(subject).keySet()){
                    if(hPN.get(subject).get(indexPN) != null) { //hack
                        rmse += Math.pow((tt.get(subject).get(ldt) - hPN.get(subject).get(indexPN)), 2);
                    }
                    indexPN++;

                }
                rmse = Math.sqrt(rmse/tt.get(subject).keySet().size());
                outputStrings_RMSE.add(new String[]{subject, String.valueOf(rmse)});
            }
            timer.stop();
            outputStrings_execTimes.add(new String[]{"Tempo per calcolare gli errori", String.valueOf(timer)});
            timer.reset();
            outputFile = new FileWriter(execTimes);
            writer = new CSVWriter(outputFile, ';',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            writer.writeAll(outputStrings_execTimes);
            writer.close();
            outputFile = new FileWriter(confInt);
            writer=new CSVWriter(outputFile, ';',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            writer.writeAll(outputStrings_confInt);
            writer.close();
            outputFile = new FileWriter(RMSE);
            writer=new CSVWriter(outputFile, ';',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            writer.writeAll(outputStrings_RMSE);
            writer.close();
        }
        plt.show();
    }

    private void plotMRR(ArrayList<Double> mrr, int interval){
        List<Double>xValues = new ArrayList<>();
        for (int i=1; i<=mrr.size();i++){ //XXX controllare se funziona
            xValues.add((double) (interval * i));
        }
        Plot plt = Plot.create();
        plt.plot()
                .add(xValues, mrr)
                .linestyle("-")
                .linewidth(1.0)
                .color("b")
                .label("mrr");

        plt.xlabel("Hours after the starting point");
        plt.ylabel("MRR");
        plt.title("MRR");
        plt.legend();

        try {
            plt.show();
        } catch (PythonExecutionException | IOException e) {
            e.printStackTrace();
        }
    }

    private void plotTopXaccuracy(ArrayList<ArrayList<Double>> topXaccuracies, int interval){

        List<Double>xValues = new ArrayList<>();
        for (int i = 1; i<=topXaccuracies.get(0).size(); i++){ //XXX controllare se funziona
            xValues.add((double) (interval * i));
        }
        Plot plt = Plot.create();
        List<String> colors = Arrays.asList("b", "g", "r", "c", "m", "y", "k");
        for (int i = 0; i < topXaccuracies.size(); i++) {
            plt.plot()
                    .add(xValues, topXaccuracies.get(i))
                    .linestyle("-")
                    .linewidth(1.0)
                    .color(colors.get(i))
                    .label("Top-"+(i+1)+"-accuracy");
        }
        plt.xlabel("Hours after the starting point");
        plt.ylabel("Accuracy");
        plt.title("Top-X-accuracies");
        plt.legend();

        try {
            plt.show();
        } catch (PythonExecutionException | IOException e) {
            e.printStackTrace();
        }
    }

    private void plotMRR_comparison(ArrayList<Double> mrrw, ArrayList<Double>mrrwo, int interval){
        List<Double>xValues = new ArrayList<>();
        for (int i=1; i<=mrrw.size();i++){ //XXX controllare se funziona
            xValues.add((double) (interval * i));
        }
        Plot plt = Plot.create();
        plt.plot()
                .add(xValues, mrrw)
                .linestyle("-")
                .linewidth(1.0)
                .color("b")
                .label("MRR with observations");

        plt.plot()
                .add(xValues, mrrwo)
                .linestyle("-")
                .linewidth(1.0)
                .color("r")
                .label("MRR without observations");

        plt.xlabel("Hours after the starting point");
        plt.ylabel("MRR");
        plt.title("MRR");
        plt.legend();

        try {
            plt.show();
        } catch (PythonExecutionException | IOException e) {
            e.printStackTrace();
        }
    }

    private void plotTopXaccuracy_comparison(ArrayList<ArrayList<Double>> topXaccuraciesw, ArrayList<ArrayList<Double>> topXaccuracieswo, int interval){

        List<Double>xValues = new ArrayList<>();
        for (int i = 1; i<=topXaccuraciesw.get(0).size(); i++){ //XXX controllare se funziona
            xValues.add((double) (interval * i));
        }
        Plot plt = Plot.create();
        List<String> colors = Arrays.asList("b", "g", "r", "c", "m", "y", "k");
        for (int i = 0; i < topXaccuraciesw.size(); i++) {
            plt.plot()
                    .add(xValues, topXaccuraciesw.get(i))
                    .linestyle("-")
                    .linewidth(1.0)
                    .color(colors.get(i))
                    .label("Top-"+(i+1)+"-accuracy with observations");

            plt.plot()
                    .add(xValues, topXaccuracieswo.get(i))
                    .linestyle("--")
                    .linewidth(1.0)
                    .color(colors.get(i))
                    .label("Top-"+(i+1)+"-accuracy without observations");
        }
        plt.xlabel("Hours after the starting point");
        plt.ylabel("Accuracy");
        plt.title("Top-X-accuracies");
        plt.legend();

        try {
            plt.show();
        } catch (PythonExecutionException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void simulate(){
        try{
            //inizio generazione eventi
            LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusHours(samples);
            ArrayList<Event> events = new ArrayList<>();
            ArrayList<Event> tests = new ArrayList<>();
            ArrayList<Event> symptoms = new ArrayList<>();
            ArrayList<Subject> subjects = new ArrayList<>();

            createObservations(subjects, events, tests, symptoms, t0);
            //fine generazione eventi

            HashMap<String, TreeMap<LocalDateTime, Double>> meanTrees = new HashMap<>();

            for(Subject subject : subjects){
                TreeMap<LocalDateTime, Double> tmpTree = new TreeMap<>();
                for(int offset = 0; offset<samples; offset++){
                    tmpTree.put(LocalDateTime.from(t0).plusHours(offset), 0.0);
                }
                meanTrees.put(subject.getName(), tmpTree);
            }

            if(DEBUG) {
                timer.start();
                //System.out.println("Size: "+(events.size() + tests.size() + symptoms.size()));
            }

            //update env obs
            /*
            for(int i = 0; i<events.size(); i++) {
                if (events.get(i).getType() instanceof Environment) {
                    Subject subject = events.get(i).getSubject().get(0);
                    subject.setShowsCovidLikeSymptoms(false);
                    events.get(i).updateRiskLevel(updateEnvObservation(events.get(i), events.get(i).getStartDate(), tests, symptoms, subject));
                }else if (events.get(i).getType() instanceof Contact){
                    ArrayList<Subject> ss = events.get(i).getSubject();
                    events.get(i).updateRiskLevel(updateContObservation(events.get(i), events.get(i).getStartDate(), tests, symptoms, ss));
                }
            }
             */
            //Simulazione vera e propria
            for(int rep = 0; rep<maxReps; rep++){
               runMainCycle(subjects, events, tests, symptoms, t0, rep, meanTrees);
            }

            if(DEBUG){
                timer.stop();
                outputStrings_execTimes.add(new String[]{"Tempo per eseguire "+ maxReps + " ripetizioni della simulazione", String.valueOf(timer)});
                outputStrings_execTimes.add(new String[]{"Tempo medio per eseguire una ripetizione della simulazione", ((double)(timer.elapsed(TimeUnit.MILLISECONDS))/(double)(maxReps))+" ms"});
                timer.reset();
                timer.start();
            }

            //Calcolo degli intervalli di confidenza sulla misura ottenuta dalla simulazione
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
                System.out.println("---");
                timer.start();
            }

            //Inizio parte numerica

            final int max_iterations = subjects.size()<=2?subjects.size()-1:2;
            HashMap<String, ArrayList<HashMap<String, Object>>> clusterSubjectsMet = new HashMap<>();
            ArrayList<String> subjects_String = new ArrayList<>();

            HashMap<String, ArrayList<HashMap<String, Object>>> envObs = new HashMap<>();
            HashMap<String, ArrayList<HashMap<String, Object>>> testObs = new HashMap<>();
            HashMap<String, ArrayList<HashMap<String, Object>>> sympObs = new HashMap<>();

            retrieveObservations(subjects, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);

            if(DEBUG){
                timer.stop();
                outputStrings_execTimes.add(new String[]{"Tempo per fare retrieval dei dati dal database", String.valueOf(timer)});
                timer.reset();
                timer.start();
            }

            ArrayList<HashMap<String, HashMap<Integer, Double>>> pns = new ArrayList<>();

            runNumericalAnalysis(pns, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);

            if(DEBUG){
                timer.stop();
                outputStrings_execTimes.add(new String[]{"Tempo per eseguire analisi mediante reti di Petri", String.valueOf(timer)});
                timer.reset();
                timer.start();
            }

            //building solution
            HashMap<String, HashMap<Integer, Double>> solutions = buildSolution(pns, subjects_String);
            //printWhoShouldBeTested(meanTrees, solutions);
            var rr = Utils.MRR(meanTrees, solutions);
            double mrr = 0;
            for(String s : rr.keySet()){
                System.out.println(s + " " + rr.get(s));
                mrr += rr.get(s);
            }
            mrr /= rr.size();
            System.out.println("MRR  = " + mrr);
            for(int i = 1; i<6; i++) {
                double accuracy = Utils.topXaccuracy(meanTrees, solutions, i);
                System.out.println("Top-" + i + " accuracy: " + accuracy);
            }
            plot(meanTrees, solutions);
        }
        catch(Exception e){
           e.printStackTrace();
        } finally {
            clean();
        }
    }

    @Test
    void mrrAndAccuracyAtMultipleTimestamps(){ //TODO
        final int hoursBetweenTimestamps = 24;
        int currentNumberOfHours = hoursBetweenTimestamps;
        ArrayList<Double> MRRs = new ArrayList<>();
        ArrayList<ArrayList<Double>> topXaccuracies = new ArrayList<>();
        int topXlimit = 5;
        for (int i = 0; i < topXlimit; i++){
            topXaccuracies.add(new ArrayList<Double>());
        }
        try{
            LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusHours(samples);
            ArrayList<Event> events = new ArrayList<>();
            ArrayList<Event> tests = new ArrayList<>();
            ArrayList<Event> symptoms = new ArrayList<>();
            ArrayList<Subject> subjects = new ArrayList<>();

            createObservations(subjects, events, tests, symptoms, t0);
            //fine generazione eventi

            HashMap<String, TreeMap<LocalDateTime, Double>> meanTrees = new HashMap<>();

            for(Subject subject : subjects){
                TreeMap<LocalDateTime, Double> tmpTree = new TreeMap<>();
                for(int offset = 0; offset<samples; offset++){
                    tmpTree.put(LocalDateTime.from(t0).plusHours(offset), 0.0);
                }
                meanTrees.put(subject.getName(), tmpTree);
            }

            //------------------------------------RETRIEVAL DATI DAL DATABASE---------------------------------------------------------
            final int max_iterations = subjects.size()<=2?subjects.size()-1:2;
            HashMap<String, ArrayList<HashMap<String, Object>>> clusterSubjectsMet = new HashMap<>();
            ArrayList<String> subjects_String = new ArrayList<>();

            HashMap<String, ArrayList<HashMap<String, Object>>> envObs = new HashMap<>();
            HashMap<String, ArrayList<HashMap<String, Object>>> testObs = new HashMap<>();
            HashMap<String, ArrayList<HashMap<String, Object>>> sympObs = new HashMap<>();

            retrieveObservations(subjects, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);
            //------------------------------------ESPERIMENTO---------------------------------------------------------
            while(currentNumberOfHours <= samples){
                System.out.println("-----------"+ currentNumberOfHours + "-----------");
                //------------------------------------INIZIO SIMULAZIONE---------------------------------------------------------
                for(int rep = 0; rep<maxReps; rep++){
                    runMainCycle(subjects, events, tests, symptoms, t0, rep, meanTrees, currentNumberOfHours);
                }

                for(Subject subject : subjects){
                    for(LocalDateTime ldt : meanTrees.get(subject.getName()).keySet()){
                        double theta = meanTrees.get(subject.getName()).get(ldt); // hat{theta} = mean(x).   mean(X) ~ N(theta, theta(1-theta)/n) per TLC
                        double offset = (1.96 * Math.sqrt(theta * (1-theta)/maxReps));
                        outputStrings_confInt.add(new String[]{subject.getName(), String.valueOf(ldt), String.valueOf((theta - offset)), String.valueOf((theta+offset))});
                    }
                }

                //------------------------------------INIZIO PARTE NUMERICA---------------------------------------------------------
                clusterSubjectsMet = new HashMap<>();
                subjects_String = new ArrayList<>();
                envObs = new HashMap<>();
                testObs = new HashMap<>();
                sympObs = new HashMap<>();
                retrieveObservations(subjects, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);
                ArrayList<HashMap<String, HashMap<Integer, Double>>> pns = new ArrayList<>();
                stpnAnalyzer = new STPNAnalyzer_ext(currentNumberOfHours, steps);
                runNumericalAnalysis(pns, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);
                HashMap<String, HashMap<Integer, Double>> solutions = buildSolution(pns, subjects_String, currentNumberOfHours, steps);
                //------------------------------------CALCOLO MRR---------------------------------------------------------
                var rr = Utils.MRR(meanTrees, solutions);
                double mrr = 0;
                for(String s : rr.keySet()){
                    System.out.println(s + " " + rr.get(s));
                    mrr += rr.get(s);
                }
                mrr /= rr.size();
                MRRs.add(mrr);
                System.out.println("MRR  = " + mrr);
                //------------------------------------CALCOLO TOP-X-ACCURACY---------------------------------------------------------

                for(int i = 0; i<topXlimit; i++) {
                    double accuracy = Utils.topXaccuracy(meanTrees, solutions, i+1);
                    topXaccuracies.get(i).add(accuracy);
                    System.out.println("Top-" + (i+1) + " accuracy: " + accuracy);
                }
                currentNumberOfHours+=hoursBetweenTimestamps;
            }
            //plot(meanTrees, solutions);
            plotMRR(MRRs, hoursBetweenTimestamps);
            plotTopXaccuracy(topXaccuracies, hoursBetweenTimestamps);
        }
        catch(Exception e){
            e.printStackTrace();
        } finally {
            clean();
        }

    }


    @Test
    void analyzeMRRWithAndWithoutObservations(){
        final int hoursBetweenTimestamps = 24;
        int currentNumberOfHours = hoursBetweenTimestamps;
        ArrayList<Double> MRRs_withObs = new ArrayList<>();
        ArrayList<ArrayList<Double>> topXaccuracies_withObs  = new ArrayList<>();
        ArrayList<Double> MRRs_withoutObs = new ArrayList<>();
        ArrayList<ArrayList<Double>> topXaccuracies_withoutObs = new ArrayList<>();
        int topXlimit = 5;
        for (int i = 0; i < topXlimit; i++){
            topXaccuracies_withObs.add(new ArrayList<Double>());
            topXaccuracies_withoutObs.add(new ArrayList<Double>());
        }
        try{
        //##########################################WITH OBSERVATIONS##########################################
            LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusHours(samples);
            ArrayList<Event> events = new ArrayList<>();
            ArrayList<Event> tests = new ArrayList<>();
            ArrayList<Event> symptoms = new ArrayList<>();
            ArrayList<Subject> subjects = new ArrayList<>();

            createObservations(subjects, events, tests, symptoms, t0);
            //fine generazione eventi

            HashMap<String, TreeMap<LocalDateTime, Double>> meanTrees = new HashMap<>();

            for(Subject subject : subjects){
                TreeMap<LocalDateTime, Double> tmpTree = new TreeMap<>();
                for(int offset = 0; offset<samples; offset++){
                    tmpTree.put(LocalDateTime.from(t0).plusHours(offset), 0.0);
                }
                meanTrees.put(subject.getName(), tmpTree);
            }

            //------------------------------------RETRIEVAL DATI DAL DATABASE---------------------------------------------------------
            final int max_iterations = subjects.size()<=2?subjects.size()-1:2;
            HashMap<String, ArrayList<HashMap<String, Object>>> clusterSubjectsMet = new HashMap<>();
            ArrayList<String> subjects_String = new ArrayList<>();

            HashMap<String, ArrayList<HashMap<String, Object>>> envObs = new HashMap<>();
            HashMap<String, ArrayList<HashMap<String, Object>>> testObs = new HashMap<>();
            HashMap<String, ArrayList<HashMap<String, Object>>> sympObs = new HashMap<>();

            retrieveObservations(subjects, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);
            //------------------------------------ESPERIMENTO---------------------------------------------------------
            while(currentNumberOfHours <= samples){
                System.out.println("-----------"+ currentNumberOfHours + "-----------");
                //------------------------------------INIZIO SIMULAZIONE---------------------------------------------------------
                for(int rep = 0; rep<maxReps; rep++){
                    runMainCycle(subjects, events, tests, symptoms, t0, rep, meanTrees, currentNumberOfHours);
                }

                for(Subject subject : subjects){
                    for(LocalDateTime ldt : meanTrees.get(subject.getName()).keySet()){
                        double theta = meanTrees.get(subject.getName()).get(ldt); // hat{theta} = mean(x).   mean(X) ~ N(theta, theta(1-theta)/n) per TLC
                        double offset = (1.96 * Math.sqrt(theta * (1-theta)/maxReps));
                        outputStrings_confInt.add(new String[]{subject.getName(), String.valueOf(ldt), String.valueOf((theta - offset)), String.valueOf((theta+offset))});
                    }
                }

                //------------------------------------INIZIO PARTE NUMERICA---------------------------------------------------------
                clusterSubjectsMet = new HashMap<>();
                subjects_String = new ArrayList<>();
                envObs = new HashMap<>();
                testObs = new HashMap<>();
                sympObs = new HashMap<>();
                retrieveObservations(subjects, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);
                ArrayList<HashMap<String, HashMap<Integer, Double>>> pns = new ArrayList<>();
                stpnAnalyzer = new STPNAnalyzer_ext(currentNumberOfHours, steps);
                runNumericalAnalysis(pns, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);
                HashMap<String, HashMap<Integer, Double>> solutions = buildSolution(pns, subjects_String, currentNumberOfHours, steps);
                //------------------------------------CALCOLO MRR---------------------------------------------------------
                var rr = Utils.MRR(meanTrees, solutions);
                double mrr = 0;
                for(String s : rr.keySet()){
                    System.out.println(s + " " + rr.get(s));
                    mrr += rr.get(s);
                }
                mrr /= rr.size();
                MRRs_withObs.add(mrr);
                System.out.println("MRR  = " + mrr);
                //------------------------------------CALCOLO TOP-X-ACCURACY---------------------------------------------------------

                for(int i = 0; i<topXlimit; i++) {
                    double accuracy = Utils.topXaccuracy(meanTrees, solutions, i+1);
                    topXaccuracies_withObs.get(i).add(accuracy);
                    System.out.println("Top-" + (i+1) + " accuracy: " + accuracy);
                }
                currentNumberOfHours+=hoursBetweenTimestamps;
            }
            //plot(meanTrees, solutions);
            System.out.println("Pipipupu");
        //##########################################WITHOUT OBSERVATIONS##########################################
            t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusHours(samples);
            //events = new ArrayList<>();
            tests = new ArrayList<>();
            symptoms = new ArrayList<>();
            //subjects = new ArrayList<>();

            //createObservations(subjects, events, tests, symptoms, t0);
            //fine generazione eventi

            meanTrees = new HashMap<>();

            for(Subject subject : subjects){
                TreeMap<LocalDateTime, Double> tmpTree = new TreeMap<>();
                for(int offset = 0; offset<samples; offset++){
                    tmpTree.put(LocalDateTime.from(t0).plusHours(offset), 0.0);
                }
                meanTrees.put(subject.getName(), tmpTree);
            }

            //------------------------------------RETRIEVAL DATI DAL DATABASE---------------------------------------------------------
            clusterSubjectsMet = new HashMap<>();
            subjects_String = new ArrayList<>();

            envObs = new HashMap<>();
            testObs = new HashMap<>();
            sympObs = new HashMap<>();

            retrieveObservations(subjects, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);
            testObs = new HashMap<>();
            sympObs = new HashMap<>();
            for(String member : subjects_String) {
                member = member.toUpperCase();
                testObs.put(member, new ArrayList<>());
                sympObs.put(member,new ArrayList<>());
            }
            //------------------------------------ESPERIMENTO---------------------------------------------------------
            currentNumberOfHours = hoursBetweenTimestamps;
            while(currentNumberOfHours <= samples){
                System.out.println("-----------"+ currentNumberOfHours + "-----------");
                //------------------------------------INIZIO SIMULAZIONE---------------------------------------------------------
                for(int rep = 0; rep<maxReps; rep++){
                    runMainCycle(subjects, events, tests, symptoms, t0, rep, meanTrees, currentNumberOfHours);
                }

                for(Subject subject : subjects){
                    for(LocalDateTime ldt : meanTrees.get(subject.getName()).keySet()){
                        double theta = meanTrees.get(subject.getName()).get(ldt); // hat{theta} = mean(x).   mean(X) ~ N(theta, theta(1-theta)/n) per TLC
                        double offset = (1.96 * Math.sqrt(theta * (1-theta)/maxReps));
                        outputStrings_confInt.add(new String[]{subject.getName(), String.valueOf(ldt), String.valueOf((theta - offset)), String.valueOf((theta+offset))});
                    }
                }

                //------------------------------------INIZIO PARTE NUMERICA---------------------------------------------------------
                clusterSubjectsMet = new HashMap<>();
                subjects_String = new ArrayList<>();
                envObs = new HashMap<>();
                testObs = new HashMap<>();
                sympObs = new HashMap<>();
                retrieveObservations(subjects, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);
                testObs = new HashMap<>();
                sympObs = new HashMap<>();
                for(String member : subjects_String) {
                    member = member.toUpperCase();
                    testObs.put(member, new ArrayList<>());
                    sympObs.put(member,new ArrayList<>());
                }
                ArrayList<HashMap<String, HashMap<Integer, Double>>> pns = new ArrayList<>();
                stpnAnalyzer = new STPNAnalyzer_ext(currentNumberOfHours, steps);
                runNumericalAnalysis(pns, subjects_String, clusterSubjectsMet, max_iterations, envObs, testObs, sympObs, t0);
                HashMap<String, HashMap<Integer, Double>> solutions = buildSolution(pns, subjects_String, currentNumberOfHours, steps);
                //------------------------------------CALCOLO MRR---------------------------------------------------------
                var rr = Utils.MRR(meanTrees, solutions);
                double mrr = 0;
                for(String s : rr.keySet()){
                    System.out.println(s + " " + rr.get(s));
                    mrr += rr.get(s);
                }
                mrr /= rr.size();
                MRRs_withoutObs.add(mrr);
                System.out.println("MRR  = " + mrr);
                //------------------------------------CALCOLO TOP-X-ACCURACY---------------------------------------------------------

                for(int i = 0; i<topXlimit; i++) {
                    double accuracy = Utils.topXaccuracy(meanTrees, solutions, i+1);
                    topXaccuracies_withoutObs.get(i).add(accuracy);
                    System.out.println("Top-" + (i+1) + " accuracy: " + accuracy);
                }
                currentNumberOfHours+=hoursBetweenTimestamps;
            }
            plotMRR_comparison(MRRs_withObs, MRRs_withoutObs, hoursBetweenTimestamps);
            plotTopXaccuracy_comparison(topXaccuracies_withObs, topXaccuracies_withoutObs, hoursBetweenTimestamps);
        }
        catch(Exception e){
            e.printStackTrace();
        } finally {
            clean();
        }
    }
    //create observations
    private void createObservations(ArrayList<Subject> subjects,ArrayList<Event> events, ArrayList<Event> tests, ArrayList<Event>  symptoms, LocalDateTime t0){
        if(DEBUG){
            timer.start();
        }
        AccessGateway accessGateway = new AccessGateway();
        //creazione dei soggetti
        for(int p = 0; p < np; p++){
            subjects.add(new Subject("P" + p));
            accessGateway.insertNewUser("P"+p, "P"+p, "P"+p, "P"+p, "P"+p);
        }
        //creazione degli eventi ambientali
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
                events.add(new Event(startDates[i], endDates[i], t, s, null, null));

                observationGateway.insertObservation(s_String, t, startDates[i], endDates[i]);
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

            events.add(new Event(nc_startDates[c], nc_endDates[c], t, partecipatingSubjects, null, null));
            observationGateway.insertObservation(s_String, t, nc_startDates[c], nc_endDates[c]);
        }
        Collections.sort(events);
        if(DEBUG){
            timer.stop();
            outputStrings_execTimes.add(new String[]{"Tempo per creare le osservazioni e caricarle sul database", String.valueOf(timer)});
            timer.reset();
        }
        //inizio generazione dei sintomi
        for(int person = 0; person < np; person++){
            int actual_nSymptoms = Math.round(min_nSymptoms + r.nextFloat() * (max_nSymptoms - min_nSymptoms));
            LocalDateTime[] startDates = new LocalDateTime[actual_nSymptoms];
            LocalDateTime[] endDates = new LocalDateTime[actual_nSymptoms];
            ArrayList<LocalDateTime> datesSymp = new ArrayList<>(generateDates(t0, actual_nSymptoms));

            int startSymp = 0;
            int endSymp = 0;
            for (int date = 0; date < datesSymp.size(); date++) {
                if (date % 2 == 0) {
                    startDates[startSymp] = datesSymp.get(date);
                    startSymp++;
                } else {
                    endDates[endSymp] = datesSymp.get(date);
                    endSymp++;
                }
            }
            for(int nSymptom = 0; nSymptom < actual_nSymptoms; nSymptom++){
                ArrayList<Subject> s = new ArrayList<>(Collections.singletonList(subjects.get(person)));
                Type t = new Symptoms();
                symptoms.add(new Event(startDates[nSymptom], endDates[nSymptom], t, s, null, null));
                ArrayList<String> s_String = new ArrayList<>();
                for(Subject sub : s){
                    s_String.add(sub.getName());
                }
                observationGateway.insertObservation(s_String, t, startDates[nSymptom], endDates[nSymptom]);
            }
        }
        //inizio generazione dei test covid
        for(int person = 0; person < np; person++){
            int actual_nCovTests = Math.round(min_nCovTests + r.nextFloat() * (max_nCovTests - min_nCovTests));
            LocalDateTime[] startDates = new LocalDateTime[actual_nCovTests];
            ArrayList<LocalDateTime> datesCovTests = new ArrayList<>(generateDates(t0, actual_nCovTests));

            int idx = 0;
            for (int date = 0; date < datesCovTests.size(); date++) {
                if (date%2 == 0) {
                    startDates[idx] = datesCovTests.get(idx);
                    idx++;
                }
            }

            for(int nCovTest = 0; nCovTest < actual_nCovTests; nCovTest++){
                ArrayList<Subject> s = new ArrayList<>(Collections.singletonList(subjects.get(person)));
                float randomTest = r.nextFloat();
                float randomPositive = r.nextFloat();
                Type t = new CovidTest(randomTest>0.5f?CovidTestType.MOLECULAR:CovidTestType.ANTIGEN, randomPositive>0.5f);
                tests.add(new Event(startDates[nCovTest], null, t, s, randomPositive>0.5f,randomTest>0.5f?"MOLECULAR":"ANTIGEN"));
                ArrayList<String> s_String = new ArrayList<>();
                for(Subject sub : s){
                    s_String.add(sub.getName());
                }
                observationGateway.insertObservation(s_String, t, startDates[nCovTest], null);
            }
        }
    }

    private float updateEnvObservation( Event event, LocalDateTime contact_time, ArrayList<Event> tests,ArrayList<Event> symptoms, Subject subject) throws Exception {
        float risk_level = ((Environment) event.getType()).getRiskLevel();
        float symp_risk_level = 0;
        float test_risk_level = 0;
        int sympCount = 0, testCount = 0;
        if (tests.size() > 0) {
            for (int test = 0; test < tests.size(); test++) {
                if (tests.get(test).getSubject().get(0).getName().equals(subject.getName())) {
                    LocalDateTime test_time = tests.get(test).getStartDate();
                    if (contact_time.isBefore(test_time)) {
                        CovidTestType covidTestType = CovidTestType.ANTIGEN;
                        if (tests.get(test).getTestType().equals("MOLECULAR")){
                            covidTestType = CovidTestType.MOLECULAR;
                        }
                        CovidTest covidTest = new CovidTest(covidTestType, (tests.get(test)).getPositive());
                        double testEvidence = covidTest.isInfected(contact_time, test_time);
                        test_risk_level += testEvidence;
                        testCount++;
                    }
                }
            }
        }
        if (symptoms.size() > 0) {
            for (int symptom = 0; symptom < symptoms.size(); symptom++) {
                if (symptoms.get(symptom).getSubject().get(0).getName().equals(subject.getName())) {
                    LocalDateTime symptom_time = symptoms.get(symptom).getStartDate();
                    if (contact_time.isBefore(symptom_time)) {
                        subject.setShowsCovidLikeSymptoms(true);
                        Symptoms covidSymptom = new Symptoms();
                        double sympEvidence = covidSymptom.updateEvidence(contact_time, symptom_time, stpnAnalyzer.symptomSolution);
                        symp_risk_level += sympEvidence;
                        sympCount++;
                    }
                }
            }
        }
        double cumulativeRiskLevel = risk_level;
        double cumulativeRiskLevel3 = cumulativeRiskLevel;
        if (testCount != 0 && sympCount != 0){
            cumulativeRiskLevel = cumulativeRiskLevel * 0.5 + symp_risk_level/sympCount + test_risk_level/testCount * 1.5;
            cumulativeRiskLevel3 = cumulativeRiskLevel / 3;
        }
        else if (testCount !=0 && sympCount == 0){
            cumulativeRiskLevel = cumulativeRiskLevel * 0.5 + test_risk_level * 1.5;
            cumulativeRiskLevel3 = cumulativeRiskLevel / 2;
        }
        else if (testCount ==0 && sympCount != 0){
            cumulativeRiskLevel = cumulativeRiskLevel * 0.8 + symp_risk_level * 1.2;
            cumulativeRiskLevel3 = cumulativeRiskLevel / 2;
        }
        double [] cumulativeRiskLevel2;
        cumulativeRiskLevel2= stpnAnalyzer.updateRiskLevel(contact_time);
        double cumulativeRiskLevel1 = cumulativeRiskLevel2[0];
        cumulativeRiskLevel = cumulativeRiskLevel1 * cumulativeRiskLevel3;
        if (subject.hasCovidLikeSymptoms()){
            cumulativeRiskLevel /= (cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1]);
        }
        else{
            cumulativeRiskLevel /= (1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1]);
            //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
        }
        return (float) cumulativeRiskLevel;
    }


    public double[] updateRiskLevelSimulator(LocalDateTime contact_time) {
        //leggere i dati dal file cvs
        double[] cumulativeRiskLevel2 = new double[2];
        CSVReader reader = null;
        try{
            //covid file
            reader = new CSVReader(new FileReader("src/main/res/dati_sintomi_covid.CSV"));
            String[] nextLine;
            HashMap<LocalDateTime, Integer> covidSymp = new HashMap<>();
            while((nextLine = reader.readNext()) != null){
                for(String token : nextLine){
                    String[] ext = token.split(";");
                    if(ext[0].matches(".*(/2022|/12/2021)$")){
                        String[] d_m_y = ext[0].split("/");
                        covidSymp.put(LocalDateTime.of(Integer.parseInt(d_m_y[2]), Integer.parseInt(d_m_y[1]), Integer.parseInt(d_m_y[0]), 0, 0),Integer.parseInt(ext[1]));
                    }
                }
            }
            reader.close();

            //flu file
            nextLine = null;
            Reader r = Files.newBufferedReader(Path.of("src/main/res/incidenza-delle-sindromi.csv")); //mi baso sulla stagione 2021-2022
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator('\n')
                    .withIgnoreQuotations(true)
                    .build();
            CSVReader csvreader = new CSVReaderBuilder(r)
                    .withSkipLines(0)
                    .withCSVParser(parser)
                    .build();
            HashMap<LocalDateTime, Integer> fluSymp = new HashMap<>();
            while((nextLine = csvreader.readNext()) != null){
                for(String token : nextLine) {
                    String tk = token.replace(",", ".");
                    String[] ext = tk.split(";");
                    if (ext[0].matches("\\d+")) {
                        double nCont = Double.parseDouble(ext[1]);
                        nCont = nCont * 59110000/1000;
                        for (int day = 0; day < 7; day++){
                            fluSymp.put(LocalDateTime.of(2022,1,1,0,0).plusWeeks(Integer.parseInt(ext[0])-1).plusDays(day), (int) nCont); //alcuni dati si riferiscono alla fine del 2021 ma conviene trattarli come se fossero tutti del 2022 perché considero solo una stagione influenzale
                        }
                    }
                }
            }
            //STA ROBA SOTTO SERVE A QUALCOSA?
            fluSymp.put(LocalDateTime.of(2021,1,1,0,0).plusWeeks(40), fluSymp.get(LocalDateTime.of(2022,1,1,0,0).plusWeeks(40)));
            fluSymp.put(LocalDateTime.of(2021,1,1,0,0).plusWeeks(41), fluSymp.get(LocalDateTime.of(2022,1,1,0,0).plusWeeks(41)));
            csvreader.close();

            double nSymp = 0;
            double nCov = 0;
            LocalDateTime cTime = contact_time;
            cTime = cTime.withHour(0).withMinute(0);
            if(contact_time.getYear() != 2022)
                cTime = cTime.withYear(2022); //mi baso sui dati del 2022

            //Considero che una persona abbia i sintomi per 2 settimane e quindi quando calcolo quante persone hanno i sintomi del covid considero tutti quelli che hanno iniziato a mostrare i sintomi nelle ultime 2 settimane
            for(LocalDateTime ldt = cTime ; ldt.isAfter(cTime.minusWeeks(2)); ldt = ldt.minusDays(1)){
                nCov += covidSymp.get(ldt);
            }
            if(cTime.isAfter(LocalDateTime.of(2022,1,1,0,0).plusWeeks(16))&&cTime.isBefore(LocalDateTime.of(2022, 1, 1,0,0).plusWeeks(42))){
                //non ci sono i dati relativi a questo periodo
            }else {
                nSymp += fluSymp.get(cTime);
            }
            cumulativeRiskLevel2[0] = nCov / 59110000;
            cumulativeRiskLevel2[1] = nSymp / 59110000;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cumulativeRiskLevel2;
    }

    public double updateProbObsSymptomsSimulator(int subjects, ArrayList<Event> symptomsArrayList, LocalDateTime t0){
        Set<Subject> symptomaticSubjects = new HashSet<>();
        for(Event e : symptomsArrayList){
            if(e.startDate.isAfter(t0)){
                symptomaticSubjects.add(e.getSubject().get(0));
            }
        }
        return (double) symptomaticSubjects.size() / subjects;
    }

    public List<Double> updateProbObsTestsSimulator(int subjects, ArrayList<Event> testArrayList, LocalDateTime t0){
        Set<Subject> positiveTestedPeople = new HashSet<>();
        Set<Subject> negativeTestedPeople = new HashSet<>();
        for(Event e : testArrayList){
            if(e.startDate.isAfter(t0)){
                if(e.isPositive)
                    positiveTestedPeople.add(e.getSubject().get(0));
                else
                    negativeTestedPeople.add(e.getSubject().get(0));
            }
        }
        List<Double> l = Arrays.asList((double) positiveTestedPeople.size() / subjects, (double) negativeTestedPeople.size() / subjects);
        return l;
    }

    private double updateRiskLevel(double riskLevel, LocalDateTime contact_time, ArrayList<Event> testsArrayList,ArrayList<Event> symptomsArrayList, Subject subject, int subjects, LocalDateTime t0) throws Exception {
        double symp_risk_level = 0;
        double test_risk_level = 0;
        boolean symp = false;
        boolean test_Pos = false;
        boolean test_Neg = false;
        if (symptomsArrayList.size() > 0) {
            for (int symptom = 0; symptom < symptomsArrayList.size(); symptom++) {
                LocalDateTime symptom_date = (LocalDateTime) symptomsArrayList.get(symptom).getStartDate();
                if (symptomsArrayList.get(symptom).getSubject().get(0).equals(subject)) {
                    if (contact_time.isBefore(symptom_date)) {
                        Symptoms symptoms = new Symptoms();
                        double sympEvidence = symptoms.updateEvidence(contact_time, symptom_date, stpnAnalyzer.symptomSolution);
                        symp_risk_level += Math.log(sympEvidence <= 0 ? 1 : sympEvidence); //XXX [CHECK IF IT'S CORRECT]
                        //System.out.println("Symp " + sympEvidence);
                        symp = true;
                    }
                }
            }
        }
        if (testsArrayList.size() > 0) {
            for (int test = 0; test < testsArrayList.size(); test++) {
                LocalDateTime test_time = (LocalDateTime) testsArrayList.get(test).getStartDate();
                if(testsArrayList.get(test).getSubject().get(0).equals(subject)) {
                    if (contact_time.isBefore(test_time)) {
                        CovidTestType covidTestType = CovidTestType.ANTIGEN;
                        if (testsArrayList.get(test).getTestType().equals("MOLECULAR")) {
                            covidTestType = CovidTestType.MOLECULAR;
                        }
                        CovidTest covidTest = new CovidTest(covidTestType, testsArrayList.get(test).getPositive());
                        //System.out.println("Covid CCC " + covidTest.getName());
                        double testEvidence = covidTest.isInfected(contact_time, test_time);
                        //System.out.println(testEvidence);
                        test_risk_level += Math.log(testEvidence);
                        if(covidTest.isPositive()){
                            test_Pos = true;
                        }else{
                            test_Neg = true;
                        }
                    }
                }
            }
        }
        double cumulativeRiskLevel =  symp_risk_level + test_risk_level + Math.log(riskLevel);
        double[] cumulativeRiskLevel2;
        double probObs = 0;
        //cumulativeRiskLevel2 = updateRiskLevelSimulator(contact_time);
        //System.out.println(cumulativeRiskLevel + " - " + cumulativeRiskLevel2[0] + " " + cumulativeRiskLevel2[1]);
        //[0] Covid diagnosticati sulla popolazione, [1] Influenza sulla popolazione
        if (symp) { //TODO VA FATTO PER TUTTI UGUALE O IN BASE A SE SI HANNO SINTOMI? IO PENSO SIA SENZA ELSE
            probObs += updateProbObsSymptomsSimulator(subjects, symptomsArrayList, t0);
            //cumulativeRiskLevel /= (cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1]);
            //cumulativeRiskLevel -= (Math.log(cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori altissimi //fixme
        } else {
            probObs += (1-updateProbObsSymptomsSimulator(subjects, symptomsArrayList, t0));
            //cumulativeRiskLevel /= (1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1]);
             //cumulativeRiskLevel -= (Math.log(1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1])); //XXX io l'ho tolto perché se ce lo lascio questo fattore fa schizzare il valore a valori negativissimi //fixme
            //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
        }
        List<Double> probObsTests = updateProbObsTestsSimulator(subjects, testsArrayList, t0);
        if (test_Pos){
            probObs += probObsTests.get(0);
        }else{
            probObs += (1-probObsTests.get(0));
        }
        if(test_Neg){
            probObs+=probObsTests.get(1);
        }else{
            probObs += (1-probObsTests.get(1));
        }
        //System.out.println("CRL: "+cumulativeRiskLevel);
        return cumulativeRiskLevel;
    }

    private float updateContObservation( Event event, LocalDateTime contact_time, ArrayList<Event> tests,ArrayList<Event> symptoms, ArrayList<Subject> ss) throws Exception {
        float risk_level = ((Contact) event.getType()).getRiskLevel();
        float symp_risk_level = 0;
        float test_risk_level = 0;
        double cumulativeRiskLevel = risk_level;
        int sympCount = 0, testCount = 0;
        for (Subject subject : ss) { //update risk level
            subject.setShowsCovidLikeSymptoms(false);
            boolean showsSymptoms = false;
            if (tests.size() > 0) {
                for (int test = 0; test < tests.size(); test++) {
                    if (tests.get(test).getSubject().get(0).getName().equals(subject.getName())) {
                        LocalDateTime test_time = tests.get(test).getStartDate();
                        if (contact_time.isBefore(test_time)) {
                            CovidTest covidTest = new CovidTest(((CovidTest) tests.get(test).getType()).getTestType(), ((CovidTest) tests.get(test).getType()).isPositive());
                            double testEvidence = covidTest.isInfected(contact_time, test_time);
                            test_risk_level += testEvidence;
                            testCount++;
                        }
                    }
                }
            }
            if (symptoms.size() > 0) {
                for (int symptom = 0; symptom < symptoms.size(); symptom++) {
                    if (symptoms.get(symptom).getSubject().get(0).getName().equals(subject.getName())) {
                        LocalDateTime symptom_time = symptoms.get(symptom).getStartDate();
                        if (contact_time.isBefore(symptom_time)) {
                            subject.setShowsCovidLikeSymptoms(true);
                            Symptoms covidSymptom = new Symptoms();
                            symp_risk_level += covidSymptom.updateEvidence(contact_time, symptom_time, stpnAnalyzer.symptomSolution);
                            sympCount++;
                            showsSymptoms = true;
                        }
                    }
                }
            }
            double cumulativeRiskLevel3 = cumulativeRiskLevel;
            if (testCount != 0 && sympCount != 0) {
                cumulativeRiskLevel = cumulativeRiskLevel * 0.5 + symp_risk_level / sympCount + test_risk_level / testCount * 1.5;
                cumulativeRiskLevel3 = cumulativeRiskLevel / 3;
            } else if (testCount != 0 && sympCount == 0) {
                cumulativeRiskLevel = cumulativeRiskLevel * 0.5 + test_risk_level * 1.5;
                cumulativeRiskLevel3 = cumulativeRiskLevel / 2;
            } else if (testCount == 0 && sympCount != 0) {
                cumulativeRiskLevel = cumulativeRiskLevel * 0.8 + symp_risk_level * 1.2;
                cumulativeRiskLevel3 = cumulativeRiskLevel / 2;
            }
            double[] cumulativeRiskLevel2;
            cumulativeRiskLevel2 = stpnAnalyzer.updateRiskLevel(contact_time);
            double cumulativeRiskLevel1 = cumulativeRiskLevel2[0];
            cumulativeRiskLevel = cumulativeRiskLevel1 * cumulativeRiskLevel3;
            System.out.println(cumulativeRiskLevel + " prima");
            if (showsSymptoms) {
                cumulativeRiskLevel /= (cumulativeRiskLevel2[0] + cumulativeRiskLevel2[1]);
            } else {
                cumulativeRiskLevel /= (1 - cumulativeRiskLevel2[0] - cumulativeRiskLevel2[1]);
                //il denominatore dovrebbe andare bene dal momento che i due eventi che sottraggo sono riguardo alla stesso campione ma sono eventi disgiunti
            }
        }
        return (float)cumulativeRiskLevel;
    }
    //main cycle of the simulation
    private void runMainCycle(ArrayList<Subject> subjects,ArrayList<Event> eventsBackup, ArrayList<Event> testsBackup, ArrayList<Event>  symptomsBackup, LocalDateTime t0, int rep, HashMap<String, TreeMap<LocalDateTime, Double>> meanTrees) throws Exception {
        ArrayList<Event> events = new ArrayList<>(eventsBackup);
        ArrayList<Event> tests = new ArrayList<>(testsBackup);
        ArrayList<Event> symptoms = new ArrayList<>(symptomsBackup);
        for(Subject subject : subjects)
            subject.initializeSubject(t0);
        while(events.size() > 0){
            Collections.sort(events);
            Event event = events.remove(0);
            LocalDateTime contact_time = event.getStartDate();
            if(event.getType() instanceof Environment){
                Subject subject = event.getSubject().get(0);
                subject.setShowsCovidLikeSymptoms(false);
                if(subject.getCurrentState() == 0){
                    float d = r.nextFloat();
                    d = (float) Math.log(d);
                    double risk_level = ((Environment)event.getType()).getRiskLevel();
                    risk_level = updateRiskLevel(risk_level, contact_time, tests, symptoms, subject, subjects.size(), t0);
                    //System.out.println("Sim-e " + contact_time + " risk_l:" + risk_level+"\n");
                    if(d < risk_level){
                        LocalDateTime ldt = getSampleCC(event.getStartDate(), 12, 36);
                        subject.changeState(event.getStartDate());
                        //rescheduling dell'evento "subject" diventa contagioso
                        events.add(new ChangeStateEvent(ldt, event.getSubject()));
                        //System.out.println("ping");
                    }
                }
            }
            else if(event.getType() instanceof Contact){
                ArrayList<Subject> contagiousSub = new ArrayList<>();
                ArrayList<Subject> ss = event.getSubject();
                boolean isThereAtLeastOneContagious = false;
                for(Subject subject : ss){
                    if(subject.getCurrentState() == 2){
                        isThereAtLeastOneContagious = true;
                        contagiousSub.add(subject);
                    }
                }
                if(isThereAtLeastOneContagious){
                    boolean toUpdate = true;
                    for(Subject subject : ss){
                        for (Subject contSub : contagiousSub){
                            if (!subject.equals(contSub))
                                toUpdate = false;
                        }
                    }
                    if (toUpdate){
                        float d = r.nextFloat();
                        d = (float) Math.log(d);
                        for(Subject subject : ss){
                            if(subject.getCurrentState() == 0){
                            double risk_level = ((Contact) event.getType()).getRiskLevel();
                            risk_level = updateRiskLevel(risk_level, contact_time, tests, symptoms, subject, subjects.size(), t0);
                            System.out.println("Sim-c " + contact_time + " risk_l:" + risk_level+"\n");
                            if(d < risk_level){
                                LocalDateTime ldt = getSampleCC(event.getStartDate(), 12, 36);
                                subject.changeState(event.getStartDate());
                                //rescheduling dell'evento "subject" diventa contagioso
                                events.add(new ChangeStateEvent(ldt, new ArrayList<>(Collections.singletonList(subject))));
                                }
                            }
                        }
                    }
                }
            }
            else if(event instanceof ChangeStateEvent){
                Subject subject = event.getSubject().get(0);
                switch (subject.getCurrentState()) {
                    case 1 -> {
                        subject.changeState(event.getStartDate());
                        //se il valore random è < 0.1 il soggetto è asintomatico
                        boolean isSymptomatic = r.nextFloat() <= 0.9f;
                        subject.setSymptomatic(isSymptomatic);
                        //rescheduling dell'evento "subject" «guarisce»
                        events.add(new ChangeStateEvent(getSampleCH(event.getStartDate(), subject), event.getSubject()));
                    }
                    case 2 -> subject.changeState(event.getStartDate());
                    case 3 -> System.out.println("immune");
                    default -> System.out.println("error");
                }
            }
        }

        //filling

            HashMap<String, TreeMap<LocalDateTime, Integer>> timestampsAtIthIteration = new HashMap<>();
            for (Subject subject : subjects) {
                timestampsAtIthIteration.put(subject.getName(), convert(fill(subject.getTimestamps(), t0)));
            }
            for (Subject subject : subjects) {
                for (LocalDateTime ldt : meanTrees.get(subject.getName()).keySet()) {
                    double newValue = (meanTrees.get(subject.getName()).get(ldt) * (rep) + timestampsAtIthIteration.get(subject.getName()).get(ldt)) / (rep + 1);
                    meanTrees.get(subject.getName()).replace(ldt, newValue);
                }
            }
    }
    private void runMainCycle(ArrayList<Subject> subjects,ArrayList<Event> eventsBackup, ArrayList<Event> testsBackup, ArrayList<Event>  symptomsBackup, LocalDateTime t0, int rep, HashMap<String, TreeMap<LocalDateTime, Double>> meanTrees, int timeLimitHours) throws Exception{
        ArrayList<Event> events = new ArrayList<>(eventsBackup);
        ArrayList<Event> tests = new ArrayList<>(testsBackup);
        ArrayList<Event> symptoms = new ArrayList<>(symptomsBackup);
        for(Subject subject : subjects)
            subject.initializeSubject(t0);
        while(events.size() > 0){
            Collections.sort(events);
            Event event = events.remove(0);
            LocalDateTime contact_time = event.getStartDate();
            if(contact_time.isAfter(t0.plusHours(timeLimitHours))){
                break;
            }else {
                if (event.getType() instanceof Environment) {
                    Subject subject = event.getSubject().get(0);
                    subject.setShowsCovidLikeSymptoms(false);
                    if (subject.getCurrentState() == 0) {
                        float d = r.nextFloat();
                        d = (float) Math.log(d);
                        double risk_level = ((Environment) event.getType()).getRiskLevel();
                        risk_level = updateRiskLevel(risk_level, contact_time, tests, symptoms, subject, subjects.size(), t0);
                        //System.out.println("Sim-e " + contact_time + " risk_l:" + risk_level+"\n");
                        if (d < risk_level) {
                            LocalDateTime ldt = getSampleCC(event.getStartDate(), 12, 36);
                            subject.changeState(event.getStartDate());
                            //rescheduling dell'evento "subject" diventa contagioso
                            events.add(new ChangeStateEvent(ldt, event.getSubject()));
                            //System.out.println("ping");
                        }
                    }
                } else if (event.getType() instanceof Contact) {
                    ArrayList<Subject> contagiousSub = new ArrayList<>();
                    ArrayList<Subject> ss = event.getSubject();
                    boolean isThereAtLeastOneContagious = false;
                    for (Subject subject : ss) {
                        if (subject.getCurrentState() == 2) {
                            isThereAtLeastOneContagious = true;
                            contagiousSub.add(subject);
                        }
                    }
                    if (isThereAtLeastOneContagious) {
                        boolean toUpdate = true;
                        for (Subject subject : ss) {
                            for (Subject contSub : contagiousSub) {
                                if (!subject.equals(contSub))
                                    toUpdate = false;
                            }
                        }
                        if (toUpdate) {
                            float d = r.nextFloat();
                            d = (float) Math.log(d);
                            for (Subject subject : ss) {
                                if (subject.getCurrentState() == 0) {
                                    double risk_level = ((Contact) event.getType()).getRiskLevel();
                                    risk_level = updateRiskLevel(risk_level, contact_time, tests, symptoms, subject, subjects.size(), t0);
                                    System.out.println("Sim-c " + contact_time + " risk_l:" + risk_level + "\n");
                                    if (d < risk_level) {
                                        LocalDateTime ldt = getSampleCC(event.getStartDate(), 12, 36);
                                        subject.changeState(event.getStartDate());
                                        //rescheduling dell'evento "subject" diventa contagioso
                                        events.add(new ChangeStateEvent(ldt, new ArrayList<>(Collections.singletonList(subject))));
                                    }
                                }
                            }
                        }
                    }
                } else if (event instanceof ChangeStateEvent) {
                    Subject subject = event.getSubject().get(0);
                    switch (subject.getCurrentState()) {
                        case 1 -> {
                            subject.changeState(event.getStartDate());
                            //se il valore random è < 0.1 il soggetto è asintomatico
                            boolean isSymptomatic = r.nextFloat() <= 0.9f;
                            subject.setSymptomatic(isSymptomatic);
                            //rescheduling dell'evento "subject" «guarisce»
                            events.add(new ChangeStateEvent(getSampleCH(event.getStartDate(), subject), event.getSubject()));
                        }
                        case 2 -> subject.changeState(event.getStartDate());
                        case 3 -> System.out.println("immune");
                        default -> System.out.println("error");
                    }
                }
            }
        }

        //filling

        HashMap<String, TreeMap<LocalDateTime, Integer>> timestampsAtIthIteration = new HashMap<>();
        for (Subject subject : subjects) {
            timestampsAtIthIteration.put(subject.getName(), convert(fill(subject.getTimestamps(), t0)));
        }
        for (Subject subject : subjects) {
            for (LocalDateTime ldt : meanTrees.get(subject.getName()).keySet()) {
                double newValue = (meanTrees.get(subject.getName()).get(ldt) * (rep) + timestampsAtIthIteration.get(subject.getName()).get(ldt)) / (rep + 1);
                meanTrees.get(subject.getName()).replace(ldt, newValue);
            }
        }
    }
    //retrieve observations from db
    void retrieveObservations(ArrayList<Subject> subjects, ArrayList<String> subjects_String, HashMap<String,ArrayList<HashMap<String, Object>>> clusterSubjectsMet, int max_iterations, HashMap<String, ArrayList<HashMap<String, Object>>> envObs, HashMap<String, ArrayList<HashMap<String, Object>>> testObs, HashMap<String, ArrayList<HashMap<String, Object>>> sympObs, LocalDateTime t0){
        for(Subject subject : subjects){
            subjects_String.add(subject.getName());
        }

        if(max_iterations>0){
            for(int i = 0; i<subjects.size(); i++){
                ArrayList<String> otherMembers = new ArrayList<>(subjects_String);
                otherMembers.remove(i);
                clusterSubjectsMet.put(subjects_String.get(i), observationGateway.getContactObservations(subjects_String.get(i), otherMembers, samples));
            }
        }
        int size = 0;

        for(String member : subjects_String){
            member = member.toUpperCase();
            ArrayList env = observationGateway.getEnvironmentObservations(member, samples);
            ArrayList tst = observationGateway.getTestObservations(member, t0);
            ArrayList sym = observationGateway.getRelevantSymptomsObservations(member, t0);
            int clusterSubjectsMet_size = clusterSubjectsMet.get(member) == null ? 0 : clusterSubjectsMet.get(member).size();
            envObs.put(member, env == null ? new ArrayList<>() : env);
            testObs.put(member, tst == null ? new ArrayList<>() : tst);
            sympObs.put(member, sym == null ? new ArrayList<>() : sym);
            /*System.out.println("--- XXX ---");
            System.out.println(envObs.get(member).size());
            System.out.println(testObs.get(member).size());
            System.out.println(sympObs.get(member).size());
            System.out.println(clusterSubjectsMet_size);
            System.out.println("--- YYY ---");*/
            size+=envObs.get(member).size() + testObs.get(member).size()+sympObs.get(member).size() + clusterSubjectsMet_size;
        }
    }

    //run numerical analysis
    private void runNumericalAnalysis(ArrayList<HashMap<String, HashMap<Integer, Double>>> pns, ArrayList<String> subjects_String, HashMap<String,ArrayList<HashMap<String, Object>>> clusterSubjectsMet, int max_iterations, HashMap<String, ArrayList<HashMap<String, Object>>> envObs, HashMap<String, ArrayList<HashMap<String, Object>>> testObs, HashMap<String, ArrayList<HashMap<String, Object>>> sympObs, LocalDateTime t0){
        try {
            stpnAnalyzer.updateProbObsSymptoms(subjects_String, sympObs, t0);
            stpnAnalyzer.updateProbObsTest(subjects_String, testObs, t0);
        }catch (Exception e){
            e.printStackTrace();
        }
        for(int nIteration = 0; nIteration<= max_iterations; nIteration++){
            HashMap<String, HashMap<Integer, Double>> pits = new HashMap<>();//p^it_s
            for(String member : subjects_String){
                if(nIteration==0){
                    try {
                        TransientSolution s = stpnAnalyzer.makeModel(envObs.get(member), testObs.get(member), sympObs.get(member));
                        pits.put(member, stpnAnalyzer.computeAnalysis(s, envObs.get(member), t0));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    try {
                        pits.put(member, stpnAnalyzer.makeClusterModel(t0, pns.get(nIteration - 1), clusterSubjectsMet.get(member), testObs.get(member), sympObs.get(member), member));
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            pns.add(pits);
        }
    }

    //build numerical solution
    HashMap<String, HashMap<Integer, Double>> buildSolution(ArrayList<HashMap<String, HashMap<Integer, Double>>> pns, ArrayList<String> subjects_String){
        return buildSolution(pns, subjects_String, samples, steps);
    }
    HashMap<String, HashMap<Integer, Double>> buildSolution(ArrayList<HashMap<String, HashMap<Integer, Double>>> pns, ArrayList<String> subjects_String, int samples, int steps){
        HashMap<String, HashMap<Integer, Double>> solutions = new HashMap<>();
        int startingIndex = considerEnvironment?0:1;
        for(String member : subjects_String){
            HashMap<Integer, Double> sol_tmp = new HashMap<>();
            for(int i = 0; i < samples; i+=steps){
                double value = 0.0;
                for (int iteration = startingIndex; iteration < pns.size(); iteration++) {
                    value += (1-value) * pns.get(iteration).get(member).get(i);
                }
                sol_tmp.put(i, value);
            }
            solutions.put(member, sol_tmp);
        }
        return solutions;
    }

    //Utility functions
    ArrayList<LocalDateTime> generateDates(LocalDateTime t0, int nEvent){
        return generateDates(t0, nEvent, samples);
    }
    ArrayList<LocalDateTime> generateDates(LocalDateTime t0, int nEvent, int samples){
        ArrayList<LocalDateTime> dates = new ArrayList<>();
        for (int i=0; i<(nEvent*2); i++){
            //Random r = new Random();
            int hours = r.nextInt(samples-1)+1;
            LocalDateTime date = t0.plusHours(hours);
            dates.add(date);
        }
        Collections.sort(dates);
        return dates;
    }

    String[] generateRiskLevels(int nEvent){
       // Random r = new Random();
        String[] risk_levels = new String[nEvent];
        for (int i=0; i<nEvent; i++){
            String string_risk_level = "";
            int risk_number = r.nextInt(3) + 1;
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

    Boolean[] generateMasks(int nEvent){
       // Random r = new Random();
        Boolean[] masks = new Boolean[nEvent];
        for (int i=0; i<nEvent; i++) {
            int number = r.nextInt();
            masks[i] = number % 2 == 0;
        }
        return masks;
    }

    private LocalDateTime getSampleCC(LocalDateTime date, int min, int max) {
        double offset = wCont.sample() + min;
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

    private LocalDateTime getSampleCH(LocalDateTime date, Subject subject) {
       // Random r = new Random();
        int randomHours;
        if(subject.isSymptomatic()) {
            //System.out.println("now is " + date + " lower key :" + subject.getTimestamps().lowerKey(date));
            double alreadyElapsedTime = ChronoUnit.MINUTES.between(subject.getTimestamps().lowerKey(date), date)/60.0;
            randomHours = (int)Math.floor(wSymp.sample() + 24 - alreadyElapsedTime + eSymp.sample());
        }else{
            randomHours = (int) Math.floor(eNotSymp.sample());
        }
        return date.plusHours(randomHours);
    }

    private TreeMap<LocalDateTime,Integer> fill(TreeMap<LocalDateTime, Integer> treeMap, LocalDateTime t0){
        LocalDateTime t = LocalDateTime.from(t0);
        final int hours = 1;
        //trovo 1 e metto tutti 1, poi trovo 2 e metto tutti 2, poi trovo 3 e metto tutti 3 fine.
       /* for(Map.Entry<LocalDateTime, Integer> entry : treeMap.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());*/
        LocalDateTime tLimit = LocalDateTime.from(t).plusHours(samples);
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

    private void printWhoShouldBeTested(HashMap<String, TreeMap<LocalDateTime, Double>> tt, HashMap<String, HashMap<Integer, Double>> ss){
        try {
            if (tt.keySet().size() != ss.keySet().size()){
                throw new RuntimeException("Different cluster size between simulated solution and analytical one");
            }else{
                double maxSim = -1.0, maxAn = -1.0;
                String personMaxSim = null, personMaxAn = null;
                //finding the "last key" for simulated solution
                LocalDateTime nearestLdt = LocalDateTime.of(1980, 1, 1,0,0);
                for(LocalDateTime ldt : tt.get(tt.keySet().toArray()[0]).keySet()){
                    if (ldt.isAfter(nearestLdt))
                        nearestLdt = ldt;
                }
                int maxIndex = 0;
                for(Integer i : ss.get(ss.keySet().toArray()[0]).keySet()){
                    if(i > maxIndex){
                        maxIndex = i;
                    }
                }
                for(String member : tt.keySet()){
                    double tmp_sim = tt.get(member).get(nearestLdt);
                    if(tmp_sim > maxSim){
                        maxSim = tmp_sim;
                        personMaxSim = member;
                    }
                    double tmp_an = ss.get(member).get(maxIndex);
                    if(tmp_an > maxAn){
                        maxAn = tmp_an;
                        personMaxAn = member;
                    }
                }
                System.out.println("According to the simulation, " + personMaxSim + " should be tested.\nAccording to the numerical analysis, " + personMaxAn + " should be tested.");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println();
    }

    @AfterAll
    static void clean(){
        for(int p=0; p<np; p++) {
            ArrayList<HashMap<String, Object>> obs = observationGateway.getObservations("P"+p);
            for (int i = 0; i < obs.size(); i++) {
                ObservationGatewayTest.deleteObservation(obs.get(i).get("ID").toString());
            }
            AccessGatewayTest.deleteUser("P"+p);
        }
    }
}
