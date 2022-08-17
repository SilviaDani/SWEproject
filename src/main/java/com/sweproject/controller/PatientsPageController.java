package com.sweproject.controller;

import com.sweproject.dao.AccessDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class PatientsPageController extends UIController{
    @FXML
    ListView<String> patients;

    public PatientsPageController() {
        accessDAO = new AccessDAO();
    }

    public ObservableList<String> getPatients(String FC) throws SQLException {
        ArrayList<HashMap<String, Object>> arrayList = accessDAO.selectDoctor(FC);
        ObservableList <String> patientsObservableList = FXCollections.observableArrayList();

        for(int i = 0; i<arrayList.size(); i++){
            String patientFC = arrayList.get(i).get("patientFiscalCode").toString();
            patientsObservableList.add(patientFC);
        }

        return patientsObservableList;
    }

    public void addPatients(String FC) throws SQLException {
        int ROW_HEIGHT = 24;
        ObservableList<String> observableListPatients = getPatients(FC);
        patients.setItems(observableListPatients);
        patients.setPrefHeight(observableListPatients.size() * ROW_HEIGHT + 2);
    }

    public void handlePatientClick(MouseEvent mouseEvent) throws IOException, SQLException {
        String selectedPatient = patients.getSelectionModel().getSelectedItem();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sweproject/FXML/patientsObservations.fxml"));
        Parent root = loader.load();
        PatientsObservationsController PatientsObservationsController = loader.getController();
        PatientsObservationsController.addObservations(selectedPatient);
        System.out.println(selectedPatient);

        stage = (Stage)((Node)mouseEvent.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
