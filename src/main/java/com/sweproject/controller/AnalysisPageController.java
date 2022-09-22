package com.sweproject.controller;

import com.sweproject.main.STPNAnalyzer;
import javafx.fxml.Initializable;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AnalysisPageController extends UIController implements Initializable {
    private STPNAnalyzer stpnAnalyzer;

    public AnalysisPageController(){
        stpnAnalyzer = new STPNAnalyzer();
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LocalDateTime localDateTime = LocalDateTime.of(2021,1,1,1,0);
        stpnAnalyzer.makeModel(user.getFiscalCode(), localDateTime);
        stpnAnalyzer.analyze();
    }
    //TODO everything

}
