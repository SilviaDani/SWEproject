package com.sweproject.analysis;

import com.github.sh0nk.matplotlib4j.NumpyUtils;
import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonConfig;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.opencsv.CSVWriter;
import com.sweproject.controller.UIController;
import com.sweproject.gateway.ObservationGateway;
import com.sweproject.gateway.ObservationGatewayTest;
import com.sweproject.model.*;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.oristool.models.stpn.TransientSolution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    final int maxReps = 5000;
    private static ObservationGateway observationGateway;
    private STPNAnalyzer_ext stpnAnalyzer;
    String PYTHON_PATH;
    static final int np = 3;
    int nContact = 14;
    int max_nEnvironment = 16;
    int min_nEnvironment = 8;
    int max_nSymptoms = 3;
    int min_nSymptoms = 1;
    int max_nCovTests = 2;
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
        HashMap<String, List<Double>> hPN = new HashMap<>();
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
            List<Double> yPN1 = new ArrayList<>();
            Double[] yPNarray1 = new Double[samples];
            int r = ss.get(0).get(finalCodes[finalJ]).getRegenerations().indexOf(ss.get(0).get(finalCodes[finalJ]).getInitialRegeneration());
            IntStream.range(0, samples).parallel().forEach(i -> {
                double value1 = 0.f;
                for (int k = 0; k < ss.size(); k++) { //FIXME: j=1 -> j=0 se vogliamo tenere di conto anche l'ambiente
                    value1 += (1-value1) * ss.get(k).get(finalCodes[finalJ]).getSolution()[i][r][0];
                }
                //value1=ss.get(ss.size()-1).get(finalCodes[finalJ]).getSolution()[i][r][0];
                yPNarray1[i] = value1;
            });
            yPN1 = Arrays.stream(yPNarray1).toList();
            hPN.put(codes[j], yPN1);
            String style = "solid";
            if(j>=5)
                style = "dashed";
            plt.plot().add(x, tYSampled1).label("Sim "+codes[j]).linestyle(style);
            plt.plot().add(x, yPN1).label("PN "+codes[j]).linestyle(style);
            plt.legend();
        }
        plt.xlim(Collections.min(x) * 1.1, Collections.max(x) * 1.1);
        plt.ylim(-0.1,1.1);
        if(DEBUG){
            timer.stop();
            outputStrings_execTimes.add(new String[]{"Tempo per eseguire il plot dei dati", String.valueOf(timer)});
            timer.start();
            for(String subject : tt.keySet()){
                double rmse = 0;
                int indexPN = 0;
                for(LocalDateTime ldt : tt.get(subject).keySet()){
                    rmse += Math.pow((tt.get(subject).get(ldt) - hPN.get(subject).get(indexPN)), 2);
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
            subjects.add(new Subject("P" + p));
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
             int upperBound = subjects_copy.size() >= 2 ? r.nextInt(subjects_copy.size() - 2) + 2 : 2;
             for(int i = 0; i<upperBound; i++) {
                 s_String.add(subjects_copy.get(i).getName());
                 partecipatingSubjects.add(subjects_copy.get(i));
             }
             Type t = new Contact(s_String, nc_masks[c], nc_riskLevels[c], nc_startDates[c], nc_endDates[c]);

             events.add(new Event(nc_startDates[c], nc_endDates[c], t, partecipatingSubjects));
             observationGateway.insertObservation(s_String, t, nc_startDates[c], nc_endDates[c]);
         }
         Collections.sort(events);
        if(DEBUG){
            timer.stop();
            outputStrings_execTimes.add(new String[]{"Tempo per creare le osservazioni e caricarle sul database", String.valueOf(timer)});
            timer.reset();
        }
        //inizio generazione dei sintomi
        ArrayList<Event> symptoms = new ArrayList<>();
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
                symptoms.add(new Event(startDates[nSymptom], endDates[nSymptom], t, s));
                ArrayList<String> s_String = new ArrayList<>();
                for(Subject sub : s){
                    s_String.add(sub.getName());
                }
                observationGateway.insertObservation(s_String, t, startDates[nSymptom], endDates[nSymptom]);
            }
        }
        //inizio generazione dei test covid
        ArrayList<Event> tests = new ArrayList<>();
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
                    Type t = new CovidTest(r.nextFloat()>0.5f?CovidTestType.MOLECULAR:CovidTestType.ANTIGEN, r.nextFloat()>0.5f);
                    tests.add(new Event(startDates[nCovTest], null, t, s));
                    ArrayList<String> s_String = new ArrayList<>();
                    for(Subject sub : s){
                        s_String.add(sub.getName());
                    }
                    observationGateway.insertObservation(s_String, t, startDates[nCovTest], null);
                }
            }

         //fine generazione eventi
            ArrayList<Event> eventsCopy = new ArrayList<>(events);
            ArrayList<Event> testCopy = new ArrayList<>(tests);
            ArrayList<Event> symptomCopy = new ArrayList<>(symptoms);
            HashMap<String, TreeMap<LocalDateTime, Double>> meanTrees = new HashMap<>();
            for(Subject subject : subjects){
                TreeMap<LocalDateTime, Double> tmpTree = new TreeMap<>();
                for(int offset = 0; offset<144; offset++){
                    tmpTree.put(LocalDateTime.from(t0).plusHours(offset), 0.0);
                }
                meanTrees.put(subject.getName(), tmpTree);
            }
            if(DEBUG) {
                timer.start();
            }
        for(int rep = 0; rep<maxReps; rep++){
                events = new ArrayList<>(eventsCopy);
                tests = new ArrayList<>(testCopy);
                symptoms = new ArrayList<>(symptomCopy);
                for(Subject subject : subjects)
                    subject.initializeSubject(t0);
                while(events.size() > 0){
                    Collections.sort(events);
                    Event event = events.remove(0);
                    LocalDateTime contact_time = event.getStartDate();
                    if(event.getType() instanceof Environment){
                        Subject subject = event.getSubject().get(0);
                        if(subject.getCurrentState() == 0){
                            float d = r.nextFloat();
                            float risk_level = ((Environment) event.getType()).getRiskLevel();
                            if (tests.size() > 0) {
                                for (int test = 0; test < tests.size(); test++) {
                                    LocalDateTime test_time = tests.get(test).getStartDate();
                                    if (contact_time.isBefore(test_time)) {
                                        CovidTest covidTest = new CovidTest(((CovidTest) tests.get(test).getType()).getTestType(), ((CovidTest)tests.get(test).getType()).isPositive());
                                        double testEvidence = covidTest.isInfected(contact_time, test_time);
                                        risk_level += testEvidence;
                                    }
                                }
                            }
                            if (symptoms.size() > 0) {
                                for (int symptom = 0; symptom < symptoms.size(); symptom++) {
                                    LocalDateTime symptom_time = symptoms.get(symptom).getStartDate();
                                    if (contact_time.isBefore(symptom_time)) {
                                        Symptoms covidSymptom = new Symptoms();
                                        double testEvidence = covidSymptom.updateEvidence(contact_time, symptom_time);
                                        risk_level += testEvidence;
                                    }
                                }
                            }
                            risk_level /= (tests.size() + symptoms.size() + 1);
                            if(d < risk_level){
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
                                    float risk_level = ((Contact) event.getType()).getRiskLevel();
                                    if (tests.size() > 0) {
                                        for (int test = 0; test < tests.size(); test++) {
                                            LocalDateTime test_time = tests.get(test).getStartDate();
                                            if (contact_time.isBefore(test_time)) {
                                                CovidTest covidTest = new CovidTest(((CovidTest) tests.get(test).getType()).getTestType(), ((CovidTest)tests.get(test).getType()).isPositive());
                                                double testEvidence = covidTest.isInfected(contact_time, test_time);
                                                risk_level += testEvidence;
                                            }
                                        }
                                    }
                                    if (symptoms.size() > 0) {
                                        for (int symptom = 0; symptom < symptoms.size(); symptom++) {
                                            LocalDateTime symptom_time = symptoms.get(symptom).getStartDate();
                                            if (contact_time.isBefore(symptom_time)) {
                                                Symptoms covidSymptom = new Symptoms();
                                                double testEvidence = covidSymptom.updateEvidence(contact_time, symptom_time);
                                                risk_level += testEvidence;
                                            }
                                        }
                                    }
                                    risk_level /= (tests.size() + symptoms.size() + 1);
                                    if(d < risk_level){
                                        LocalDateTime ldt = getSampleCC(event.getStartDate(), 12, 36);
                                        subject.changeState(event.getStartDate());
                                        //rescheduling dell'evento "subject" diventa contagioso
                                        events.add(new ChangeStateEvent(ldt, new ArrayList<>(Collections.singletonList(subject))));
                                    }
                                }
                            }
                        }
                    }else if(event instanceof ChangeStateEvent){
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
            outputStrings_execTimes.add(new String[]{"Tempo medio per eseguire una ripetizione della simulazione", ((double)(timer.elapsed(TimeUnit.MILLISECONDS))/(double)(maxReps))+" ms"});
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
                    clusterSubjectsMet.put(subjects_String.get(i), observationGateway.getContactObservations(subjects_String.get(i), otherMembers));
                }
            }

            HashMap<String, ArrayList<HashMap<String, Object>>> envObs = new HashMap<>();
            HashMap<String, ArrayList<HashMap<String, Object>>> testObs = new HashMap<>();
            HashMap<String, ArrayList<HashMap<String, Object>>> sympObs = new HashMap<>();
            for(String member : subjects_String){
                envObs.put(member, observationGateway.getEnvironmentObservations(member));
                testObs.put(member, observationGateway.getTestObservations(member, t0));
                sympObs.put(member, observationGateway.getRelevantSymptomsObservations(member, t0));
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
                            pits.put(member, stpnAnalyzer.makeModel2(member, envObs.get(member), testObs.get(member), sympObs.get(member)));
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

            for(int c = 0; c<nContact; c++){
                ArrayList<String> s_String = new ArrayList<>();
                ArrayList<Subject> subjects_copy = new ArrayList<>(subjects);
                ArrayList<Subject> partecipatingSubjects = new ArrayList<>();
                Collections.shuffle(subjects_copy);
                int upperBound = subjects_copy.size() >= 2 ? r.nextInt(subjects_copy.size()-2) + 2 : 2;
                for(int i = 0; i<upperBound; i++) {
                    s_String.add(subjects_copy.get(i).getName());
                    partecipatingSubjects.add(subjects_copy.get(i));
                }
                Type t = new Contact(s_String, nc_masks[c], nc_riskLevels[c], nc_startDates[c], nc_endDates[c]);

                events.add(new Event(nc_startDates[c], nc_endDates[c], t, partecipatingSubjects));
                observationGateway.insertObservation(s_String, t, nc_startDates[c], nc_endDates[c]);
            }
            Collections.sort(events);
            if(DEBUG){
                timer.stop();
                outputStrings_execTimes.add(new String[]{"Tempo per creare le osservazioni e caricarle sul database", String.valueOf(timer)});
                timer.reset();
                timer.start();
            }
            //fine generazione eventi

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
                    clusterSubjectsMet.put(subjects_String.get(i), observationGateway.getContactObservations(subjects_String.get(i), otherMembers));
                }
            }

            HashMap<String, ArrayList<HashMap<String, Object>>> envObs = new HashMap<>();
            for(String member : subjects_String){
                envObs.put(member, observationGateway.getEnvironmentObservations(member));
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
                outputStrings_execTimes.add(new String[]{"Tempo per eseguire analisi mediante reti di Petri", String.valueOf(timer)});
                timeLimit=timer.elapsed(TimeUnit.NANOSECONDS);
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
                                //se il valore random è < 0.1 il soggetto è asintomatico
                                boolean isSymptomatic = r.nextFloat() <= 0.9f;
                                subject.setSymptomatic(isSymptomatic);
                                //rescheduling dell'evento "subject" «guarisce»
                                events.add(new ChangeStateEvent(getSampleCH(event.getStartDate(), subject), event.getSubject()));
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
                outputStrings_execTimes.add(new String[]{"Tempo per eseguire "+ rep + " ripetizioni della simulazione", String.valueOf(timer)});
                outputStrings_execTimes.add(new String[]{"Tempo medio per eseguire una ripetizione della simulazione", ((double) timer.elapsed(TimeUnit.MILLISECONDS)/(double)rep)+" ms"});
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
       WeibullDistribution w = new WeibullDistribution(3.5, (double)14);
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
        ExponentialDistribution e = new ExponentialDistribution(25); //1/25 = 0.04
        int randomHours = (int) Math.floor(e.sample());
        return date.plusHours(randomHours);
    }

    private LocalDateTime getSampleCH(LocalDateTime date, Subject subject) {
        Random r = new Random();
        int mean;
        int randomHours;
        if(subject.isSymptomatic()) {
            mean = 10; //1/10 = 0.1
            WeibullDistribution w = new WeibullDistribution(2, 11);

            ExponentialDistribution e = new ExponentialDistribution(mean);
            //System.out.println("now is " + date + " lower key :" + subject.getTimestamps().lowerKey(date));
            double alreadyElapsedTime = ChronoUnit.MINUTES.between(subject.getTimestamps().lowerKey(date), date)/60.0;
            randomHours = (int)Math.floor(w.sample() + 24 - alreadyElapsedTime + e.sample());
        }else{
            mean = 25;//1/25 = 0.04
            ExponentialDistribution e = new ExponentialDistribution(mean);
            randomHours = (int) Math.floor(e.sample());
        }
        return date.plusHours(randomHours);
    }

    @AfterAll
    static void clean(){
        for(int p=0; p<np; p++) {
            ArrayList<HashMap<String, Object>> obs = observationGateway.getObservations("P"+p);
            for (int i = 0; i < obs.size(); i++) {
                ObservationGatewayTest.deleteObservation(obs.get(i).get("id").toString());
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
