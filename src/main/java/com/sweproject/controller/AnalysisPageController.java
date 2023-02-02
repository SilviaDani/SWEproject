package com.sweproject.controller;

import com.sweproject.analysis.STPNAnalyzer;
import com.sweproject.analysis.STPNAnalyzer_ext;
import com.sweproject.gateway.ObservationGateway;
import com.sweproject.model.CovidTestType;
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
    ObservationGateway observationGateway;

    public AnalysisPageController(){
        stpnAnalyzer = new STPNAnalyzer(144,1);
        stpnAnalyzer_ext = new STPNAnalyzer_ext(144, 1);
        observationGateway = new ObservationGateway();
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TransientSolution analysis = null;
        ArrayList<HashMap<String, Object>> environmentArrayList = null;
        LocalDateTime right_now = LocalDateTime.now();
        LocalDateTime now = right_now.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime start_time_analysis = now.minusDays(6);
        try {
            environmentArrayList = observationGateway.getEnvironmentObservations(user.getFiscalCode());
            ArrayList<HashMap<String, Object>> testArrayList = observationGateway.getTestObservations(user.getFiscalCode(), start_time_analysis);
            ArrayList<HashMap<String, Object>> symptomsArrayList = observationGateway.getRelevantSymptomsObservations(user.getFiscalCode(), start_time_analysis);
            analysis = stpnAnalyzer_ext.makeModel2(environmentArrayList, testArrayList, symptomsArrayList);
            //analysis = stpnAnalyzer.makeModel(user.getFiscalCode(), environmentArrayList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        XYChart.Series series = stpnAnalyzer_ext.makeChart2(analysis, environmentArrayList, start_time_analysis);
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
