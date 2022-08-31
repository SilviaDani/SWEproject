package com.sweproject.controller;

import com.sweproject.main.STPNAnalyzer;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class AnalysisPageController extends UIController implements Initializable {
    private STPNAnalyzer stpnAnalyzer;

    public AnalysisPageController(){
        stpnAnalyzer = new STPNAnalyzer();
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        stpnAnalyzer.makeModel(user.getFiscalCode());
        stpnAnalyzer.analyze();
    }
    //TODO everything

}
