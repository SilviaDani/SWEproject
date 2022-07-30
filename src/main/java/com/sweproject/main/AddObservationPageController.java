package com.sweproject.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitMenuButton;

import java.net.URL;
import java.util.ResourceBundle;

public class AddObservationPageController implements Initializable {
    @FXML private ComboBox observation_type_menu;
    @FXML private ComboBox select_cluster_menu;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        observation_type_menu.getItems().clear();
        observation_type_menu.getItems().addAll("Contacts", "Symptoms", "Covid test");
        select_cluster_menu.getItems().clear();
        select_cluster_menu.getItems().addAll("Cluster1","Cluster2"); //TODO in base ai cluster dell'utente, mostrare i cluster opportuni
    }

    public void confirm(ActionEvent event){
        //TODO Trovare il notifier opportuno e fare addObservation con i dati presi sopra
    }

}
