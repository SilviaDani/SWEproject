package com.sweproject.controller;

import com.sweproject.main.STPNAnalyzer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import com.sweproject.dao.ObservationDAO;
import org.oristool.models.stpn.TransientSolution;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;

public class ContagiousLevelPageController extends UIController implements Initializable {
    @FXML
    private Text result;
    private STPNAnalyzer stpnAnalyzer;
    private ObservationDAO observationDAO;

    public ContagiousLevelPageController(){
        stpnAnalyzer = new STPNAnalyzer();
        observationDAO = new ObservationDAO();
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ArrayList<String> clusterMembers = (ArrayList<String>) data;
        String FC = (clusterMembers).get(clusterMembers.size() - 1);
        clusterMembers.remove(clusterMembers.size() - 1);
        ArrayList<HashMap<String, Object>> clusterSubjectsMet = observationDAO.getContactObservation(FC, clusterMembers);

        ArrayList<TransientSolution> ss = new ArrayList<>();
        HashMap<String, TransientSolution> subjects_ss = new HashMap<>();
        ArrayList<String> contact_subjects_duplicates = new ArrayList<>();

        //analisi rischi ambiente di ogni soggetto diverso dall`utente
        for (int i = 0; i < clusterSubjectsMet.size(); i++){
            contact_subjects_duplicates.add(clusterSubjectsMet.get(i).get("obs2.fiscalCode").toString());
        }
        HashSet<String> hset = new HashSet<>(contact_subjects_duplicates);
        String [] contact_subject = hset.toArray(new String[hset.size()]);
        for (int i = 0; i < contact_subject.length; i++){
            ss.add(stpnAnalyzer.makeModel(contact_subject[i]));
            subjects_ss.put(contact_subject[i], ss.get(i));
        }
        stpnAnalyzer.makeClusterModel(subjects_ss, clusterSubjectsMet);
        //float r = stpnAnalyzer.getChancesOfHavingContagiousPersonInCluster(ss);
        //result.setText((r*100)+"%");
        /*var analysis = stpnAnalyzer.makeModel(user.getFiscalCode());
        XYChart.Series series = stpnAnalyzer.makeChart(analysis);
        series.setName("Risk level");
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Minutes");
        yAxis.setLabel("Risk level");
        chart.setTitle("Risk level during the last 6 days");
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
}