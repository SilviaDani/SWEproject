package com.sweproject.controller;

import com.sweproject.analysis.STPNAnalyzer;
import com.sweproject.analysis.STPNAnalyzer_ext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import com.sweproject.gateway.ObservationGateway;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

public class ContagiousLevelPageController extends UIController implements Initializable {
    @FXML private LineChart chart;
    private STPNAnalyzer stpnAnalyzer;
    private STPNAnalyzer_ext stpnAnalyzer_ext;
    private ObservationGateway observationGateway;
    private int samples = 144;

    public ContagiousLevelPageController(){
        stpnAnalyzer_ext = new STPNAnalyzer_ext(samples, 1);
        observationGateway = new ObservationGateway();
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ArrayList<String> clusterMembers = new ArrayList<>((ArrayList<String>) data);
        System.out.println(clusterMembers.size()+"size");
        final int max_iterations = clusterMembers.size()<=2?clusterMembers.size()-1:2;
        HashMap<String, ArrayList<HashMap<String, Object>>> clusterSubjectsMet = new HashMap<>();
        if(max_iterations>0){
            for(int i = 0; i<clusterMembers.size(); i++){
            ArrayList<String> otherMembers = new ArrayList<>(clusterMembers);
            otherMembers.remove(i);
            clusterSubjectsMet.put(clusterMembers.get(i), observationGateway.getContactObservations(clusterMembers.get(i), otherMembers, samples));
            }
        }

        System.out.println(clusterSubjectsMet.keySet());
        System.out.println(clusterMembers.get(0));
        for(String s : clusterSubjectsMet.keySet()){
            System.out.println(s+ " "+ clusterSubjectsMet.get(s));
        }

        ArrayList<HashMap<String, XYChart.Series>> pns = new ArrayList<>();
        HashMap<String, ArrayList<HashMap<String, Object>>> envObs = new HashMap<>();
        HashMap<String, ArrayList<HashMap<String, Object>>> testObs = new HashMap<>();
        HashMap<String, ArrayList<HashMap<String, Object>>> sympObs = new HashMap<>();
        LocalDateTime right_now = LocalDateTime.now();
        LocalDateTime now = right_now.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime start_time_analysis = now.minusDays(6);
        for(String member : clusterMembers){
            envObs.put(member, observationGateway.getEnvironmentObservations(member, samples));
            testObs.put(member, observationGateway.getTestObservations(user.getFiscalCode(), start_time_analysis));
            sympObs.put(member, observationGateway.getRelevantSymptomsObservations(user.getFiscalCode(), start_time_analysis));
        }
        for(int nIteration = 0; nIteration<=max_iterations; nIteration++){
            HashMap<String, XYChart.Series> pits = new HashMap<>();//p^it_s
            for(String member : clusterMembers){
                System.out.println(member + " it:"+nIteration + " started");
                if(nIteration==0){
                    try {
                        var s = stpnAnalyzer_ext.makeModel(envObs.get(member), testObs.get(member), sympObs.get(member));
                        pits.put(member, stpnAnalyzer_ext.adaptForApp(stpnAnalyzer_ext.computeAnalysis(s, envObs.get(member),start_time_analysis)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        pits.put(member, stpnAnalyzer_ext.makeClusterModelForApp(start_time_analysis, pns.get(nIteration-1), clusterSubjectsMet.get(member), testObs.get(member), sympObs.get(member), member));
                        //pits.put(member, stpnAnalyzer_ext.makeClusterModel(pns.get(nIteration-1), clusterSubjectsMet.get(member)));
                    } catch (Exception e) {
                        System.out.println("EXC");
                        e.printStackTrace();
                    }
                }
                System.out.println(member + " it:"+nIteration + " completed");
            }
            pns.add(pits);
        }
        XYChart.Series series = stpnAnalyzer_ext.buildSolution(pns, user.getFiscalCode().toUpperCase());
        series.setName("Contagion level");
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Minutes");
        yAxis.setLabel("Contagion level");
        chart.setTitle("Probability of being contagious due to a contact during the last 6 days");
        chart.setCreateSymbols(false);
        chart.getData().add(series);
        /* String FC = (clusterMembers).get(clusterMembers.size() - 1);
        clusterMembers.remove(clusterMembers.size() - 1);
        ArrayList<HashMap<String, Object>> clusterSubjectsMet = observationDAO.getContactObservation(FC, clusterMembers);

        ArrayList<TransientSolution> ss = new ArrayList<>();
        HashMap<String, TransientSolution> pns = new HashMap<>(); //P^n_s(t)
        ArrayList<String> contact_subjects_duplicates = new ArrayList<>();

        //calcolo p^0_s(t) per s in S
        //analisi rischi ambiente di ogni soggetto diverso dall`utente
        for (int i = 0; i < clusterSubjectsMet.size(); i++){
            contact_subjects_duplicates.add(clusterSubjectsMet.get(i).get("fiscalCode").toString());
        }
        HashSet<String> hset = new HashSet<>(contact_subjects_duplicates);
        String [] contact_subject = hset.toArray(new String[hset.size()]);
        for (int i = 0; i < contact_subject.length; i++){
            ss.add(stpnAnalyzer.makeModel(contact_subject[i]));
            pns.put(contact_subject[i], ss.get(i));
            System.out.println(contact_subject[i] + " "+ss.get(i).getSolution()[0][0][0]);
        }
        //analisi rischi ambiente per l'utente
        pns.put(FC, stpnAnalyzer.makeModel(FC));
        System.out.println(FC + "---");
        for(int i = 0; i<max_iterations; i++){

        }
        //calcolo p^{n+1}_s(t)
        //passare lista di persone cluster e persona s, poi fare copia della lista e fare pop di s. Costruire la rete utilizzando la copia.
        var analysis = stpnAnalyzer.makeClusterModel(pns, clusterSubjectsMet);
        System.out.println(analysis.getSolution().length + "lunghezza soluzione");
        XYChart.Series series = stpnAnalyzer.makeChart(analysis);
        series.setName("Contagion level");
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Minutes");
        yAxis.setLabel("Contagion level");
        chart.setTitle("Probability of being contagious due to the environment during the last 6 days");
        chart.setCreateSymbols(false);
        chart.getData().add(series);*/

    }

    public void ok(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/userPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    public void back(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/selectMembersPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}