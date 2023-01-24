package com.sweproject.controller;

import com.sweproject.analysis.STPNAnalyzer;
import com.sweproject.analysis.STPNAnalyzer_ext;
import com.sweproject.dao.ObservationDAO;
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
import org.oristool.models.stpn.TransientSolution;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

public class AnalysisPageController extends UIController implements Initializable {
    @FXML private LineChart chart;
    private STPNAnalyzer stpnAnalyzer;
    private STPNAnalyzer_ext stpnAnalyzer_ext;
    ObservationDAO observationDAO;

    public AnalysisPageController(){
        stpnAnalyzer = new STPNAnalyzer(144,1);
        stpnAnalyzer_ext = new STPNAnalyzer_ext(144, 1);
        observationDAO = new ObservationDAO();
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TransientSolution analysis = null;
        try {
            LocalDateTime right_now = LocalDateTime.now();
            LocalDateTime now = right_now.truncatedTo(ChronoUnit.SECONDS);
            LocalDateTime start_time_analysis = now.minusDays(6);
            ArrayList<HashMap<String, Object>> environmentArrayList = observationDAO.getEnvironmentObservations(user.getFiscalCode());
            ArrayList<HashMap<String, Object>> testArrayList = observationDAO.getTestObservations(user.getFiscalCode(), start_time_analysis);
            ArrayList<HashMap<String, Object>> symptomsArrayList = observationDAO.getRelevantSymptomsObservations(user.getFiscalCode(), start_time_analysis);
            analysis = stpnAnalyzer_ext.makeModel(user.getFiscalCode(), environmentArrayList, testArrayList, symptomsArrayList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        XYChart.Series series = stpnAnalyzer.makeChart(analysis);
        series.setName("Contagion level");
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Minutes");
        yAxis.setLabel("Contagion level");
        chart.setTitle("Probability of being contagious due to the environment during the last 6 days");
        chart.setCreateSymbols(false);
        chart.getData().add(series);
    }

    public void back(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/userPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
