package com.sweproject.analysis;

import com.sweproject.gateway.AccessGateway;
import com.sweproject.gateway.AccessGatewayTest;
import com.sweproject.gateway.ObservationGateway;
import com.sweproject.analysis.Subject;
import com.sweproject.gateway.ObservationGatewayTest;
import com.sweproject.model.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.sweproject.main.Main.DEBUG;

class Dataset {
    private int n_subjects;
    private int in_contacts;
    private int n_tests;
    private int n_symptoms;
    private Random r = new Random();
    private int n_hours;
    private static ObservationGateway observationGateway;
    private ArrayList<Event> events = new ArrayList<>();
    private ArrayList<Event> tests = new ArrayList<>();
    private ArrayList<Event> symptoms = new ArrayList<>();
    private ArrayList<Subject> subjects = new ArrayList<>();


    public Dataset(int n_subjects, int in_contacts, int n_hours) {
        this.n_subjects = n_subjects;
        //numero totale di contatti interni a un cluster tra coppie
        this.in_contacts = in_contacts;
        this.n_tests = r.nextInt(this.n_subjects/2);
        this.n_symptoms = r.nextInt(this.n_subjects/2);
        this.n_hours = n_hours;
        observationGateway = new ObservationGateway();
        System.out.println("N tests: " + n_tests);
        System.out.println("N symptoms: " + n_symptoms);
    }

    public ArrayList<Event> getEvents() {
        return events;
    }

    public ArrayList<Event> getTests() {
        return tests;
    }

    public ArrayList<Event> getSymptoms() {
        return symptoms;
    }

    public ArrayList<Subject> getSubjects() {
        return subjects;
    }

    private ArrayList<LocalDateTime> generateDates(LocalDateTime t0, int nEvent, int samples) {
        ArrayList<LocalDateTime> dates = new ArrayList<>();
        for (int i = 0; i < (nEvent * 2); i++) {
            //Random r = new Random();
            int hours = r.nextInt(samples - 1) + 1;
            LocalDateTime date = t0.plusHours(hours);
            dates.add(date);
        }
        Collections.sort(dates);
        return dates;
    }

    private String[] generateRiskLevels(int nEvent) {
        // Random r = new Random();
        String[] risk_levels = new String[nEvent];
        for (int i = 0; i < nEvent; i++) {
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

    private Boolean[] generateMasks(int nEvent) {
        Boolean[] masks = new Boolean[nEvent];
        for (int i = 0; i < nEvent; i++) {
            int number = r.nextInt();
            masks[i] = number % 2 == 0;
        }
        return masks;
    }

    //TODO fare dataset separati o metodo che pulisce il database
    public void createDataset() {
        LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusHours(n_hours);
        AccessGateway accessGateway = new AccessGateway();
        //creazione dei soggetti
        for (int p = 0; p < n_subjects; p++) {
            subjects.add(new Subject("P" + p));
            accessGateway.insertNewUser("P" + p, "P" + p, "P" + p, "P" + p, "P" + p);
        }
        //creazione degli eventi ambientali
        for (int p = 0; p < n_subjects; p++) {
            int out_contacts = r.nextInt(in_contacts/n_subjects);
            LocalDateTime[] out_startDates = new LocalDateTime[out_contacts];
            LocalDateTime[] out_endDates = new LocalDateTime[out_contacts];
            ArrayList<LocalDateTime> dates = new ArrayList<>(generateDates(t0, out_contacts, n_hours));

            int start = 0;
            int end = 0;
            for (int date = 0; date < dates.size(); date++) {
                if (date % 2 == 0) {
                    out_startDates[start] = dates.get(date);
                    start++;
                } else {
                    out_endDates[end] = dates.get(date);
                    end++;
                }
            }

            String[] out_riskLevels = generateRiskLevels(out_contacts);
            Boolean[] out_masks = generateMasks(out_contacts);

            for (int i = 0; i < out_contacts; i++) {
                ArrayList<Subject> s = new ArrayList<>(Collections.singletonList(subjects.get(p)));
                ArrayList<String> s_String = new ArrayList<>();
                for (Subject sub : s) {
                    s_String.add(sub.getName());
                }
                Type t = new Environment(out_masks[i], out_riskLevels[i], out_startDates[i], out_endDates[i]);
                events.add(new Event(out_startDates[i], out_endDates[i], t, s, null, null));

                observationGateway.insertObservation(s_String, t, out_startDates[i], out_endDates[i]);
            }
            Utils.progressBar(p, n_subjects, "Creating contacts with the environment");
        }

        //creazione eventi nel cluster
        String[] in_riskLevels;
        Boolean[] in_masks;
        LocalDateTime[] in_startDates = new LocalDateTime[in_contacts];
        LocalDateTime[] in_endDates = new LocalDateTime[in_contacts];
        ArrayList<LocalDateTime> dates = new ArrayList<>(generateDates(t0, in_contacts, n_hours));
        int start = 0;
        int end = 0;
        for (int date = 0; date < dates.size(); date++) {
            if (date % 2 == 0) {
                in_startDates[start] = dates.get(date);
                start++;
            } else {
                in_endDates[end] = dates.get(date);
                end++;
            }
        }
        in_riskLevels = generateRiskLevels(in_contacts);
        in_masks = generateMasks(in_contacts);


        for (int c = 0; c < in_contacts; c++) {
            ArrayList<String> s_String = new ArrayList<>();
            ArrayList<Subject> subjects_copy = new ArrayList<>(subjects);
            ArrayList<Subject> partecipatingSubjects = new ArrayList<>();
            Collections.shuffle(subjects_copy);
            //int upperBound = subjects_copy.size() > 2 ? r.nextInt(subjects_copy.size() - 2) + 2 : 2;
            int upperBound = 2;
            for (int i = 0; i < upperBound; i++) {
                s_String.add(subjects_copy.get(i).getName());
                partecipatingSubjects.add(subjects_copy.get(i));
            }
            Type t = new Contact(s_String, in_masks[c], in_riskLevels[c], in_startDates[c], in_endDates[c]);

            events.add(new Event(in_startDates[c], in_endDates[c], t, partecipatingSubjects, null, null));
            observationGateway.insertObservation(s_String, t, in_startDates[c], in_endDates[c]);
            Utils.progressBar(c, in_contacts, "Creating contacts within the cluster");
        }
        Collections.sort(events);

        //inizio generazione dei sintomi

        LocalDateTime[] startDates_symptoms = new LocalDateTime[n_symptoms];
        LocalDateTime[] endDates_symptoms = new LocalDateTime[n_symptoms];
        ArrayList<LocalDateTime> datesSymp = new ArrayList<>(generateDates(t0, n_symptoms, n_hours));

        int startSymp = 0;
        int endSymp = 0;
        for (int date = 0; date < datesSymp.size(); date++) {
            if (date % 2 == 0) {
                startDates_symptoms[startSymp] = datesSymp.get(date);
                startSymp++;
            } else {
                endDates_symptoms[endSymp] = datesSymp.get(date);
                endSymp++;
            }
        }
        for (int nSymptom = 0; nSymptom < n_symptoms; nSymptom++) {
            int person = r.nextInt(n_subjects);
            ArrayList<Subject> s = new ArrayList<>(Collections.singletonList(subjects.get(person)));
            Type t = new Symptoms();
            symptoms.add(new Event(startDates_symptoms[nSymptom], endDates_symptoms[nSymptom], t, s, null, null));
            ArrayList<String> s_String = new ArrayList<>();
            for (Subject sub : s) {
                s_String.add(sub.getName());
            }
            observationGateway.insertObservation(s_String, t, startDates_symptoms[nSymptom], endDates_symptoms[nSymptom]);
            Utils.progressBar(nSymptom, n_symptoms, "Creating symptom observations");
        }


        //inizio generazione dei test covid

        LocalDateTime[] startDates_tests = new LocalDateTime[n_tests];
        ArrayList<LocalDateTime> datesCovTests = new ArrayList<>(generateDates(t0, n_tests, n_hours));

        int idx = 0;
        for (int date = 0; date < datesCovTests.size(); date++) {
            if (date % 2 == 0) {
                startDates_tests[idx] = datesCovTests.get(idx);
                idx++;
            }
        }

        for (int nCovTest = 0; nCovTest < n_tests; nCovTest++) {
            int person = r.nextInt(n_subjects);
            ArrayList<Subject> s = new ArrayList<>(Collections.singletonList(subjects.get(person)));
            float randomTest = r.nextFloat();
            float randomPositive = r.nextFloat();
            Type t = new CovidTest(randomTest > 0.5f ? CovidTestType.MOLECULAR : CovidTestType.ANTIGEN, randomPositive > 0.5f);
            tests.add(new Event(startDates_tests[nCovTest], null, t, s, randomPositive > 0.5f, randomTest > 0.5f ? "MOLECULAR" : "ANTIGEN"));
            ArrayList<String> s_String = new ArrayList<>();
            for (Subject sub : s) {
                s_String.add(sub.getName());
            }
            observationGateway.insertObservation(s_String, t, startDates_tests[nCovTest], null);
            Utils.progressBar(nCovTest, n_tests, "Creating test observations");
        }
    }

    public void cleanDataset() {
        int pBarProgress = 0;
        String message = "Cleaning dataset";
        for (Subject subject : subjects) {
            Utils.progressBar(pBarProgress, subjects.size(), message);
            ArrayList<HashMap<String, Object>> obs = observationGateway.getObservations(subject.getName());
            for (int i = 0; i < obs.size(); i++) {
                ObservationGatewayTest.deleteObservation(obs.get(i).get("ID").toString());
            }
            AccessGatewayTest.deleteUser(subject.getName());
            Utils.progressBar(++pBarProgress, subjects.size(), message);
        }
        System.out.println("Dataset cleaned!");
    }
}


