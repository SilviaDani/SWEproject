package com.sweproject.controller;

import com.sweproject.analysis.STPNAnalyzer;
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
import com.sweproject.dao.ObservationDAO;
import org.oristool.models.stpn.TransientSolution;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

public class ContagiousLevelPageController extends UIController implements Initializable {
    @FXML private LineChart chart;
    private STPNAnalyzer stpnAnalyzer;
    private ObservationDAO observationDAO;

    public ContagiousLevelPageController(){
        stpnAnalyzer = new STPNAnalyzer(144, 1);
        observationDAO = new ObservationDAO();
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
            clusterSubjectsMet.put(clusterMembers.get(i), observationDAO.getContactObservations(clusterMembers.get(i), otherMembers));
            }
        }
        ArrayList<HashMap<String, TransientSolution>> pns = new ArrayList<>();
        for(int nIteration = 0; nIteration<=max_iterations; nIteration++){
            HashMap<String, TransientSolution> pits = new HashMap<>();//p^it_s
            for(String member : clusterMembers){
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
        XYChart.Series series = stpnAnalyzer.makeChart(pns, user.getFiscalCode());
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