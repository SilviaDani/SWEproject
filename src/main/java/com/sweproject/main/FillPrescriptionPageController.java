package com.sweproject.main;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class FillPrescriptionPageController extends UIController implements Initializable {
    @FXML
    private ComboBox covid_test_menu;
    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        covid_test_menu.getItems().clear();
        covid_test_menu.getItems().addAll("Molecular", "Antigen");

    }

    public void confirm(){
        //TODO changeStatut dell'osservazione selezionata
    }

}
