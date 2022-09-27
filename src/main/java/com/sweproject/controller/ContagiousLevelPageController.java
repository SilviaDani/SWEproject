package com.sweproject.controller;

import com.sweproject.main.STPNAnalyzer;
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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.oristool.models.stpn.TransientSolution;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ContagiousLevelPageController extends UIController implements Initializable {
    @FXML
    private Text result;
    private STPNAnalyzer stpnAnalyzer;

    public ContagiousLevelPageController(){
        stpnAnalyzer = new STPNAnalyzer();
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ArrayList<String> clusterMembers = (ArrayList<String>) data;
        ArrayList<TransientSolution> ss = new ArrayList<>();
        for (int i = 0; i<clusterMembers.size(); i++){
            ss.add(stpnAnalyzer.makeModel(clusterMembers.get(i)));
        }
        float r = stpnAnalyzer.getChancesOfHavingContagiousPersonInCluster(ss);
        result.setText((r*100)+"%");
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