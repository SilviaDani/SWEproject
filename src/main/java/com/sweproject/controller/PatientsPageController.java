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
import java.sql.ResultSet;
import java.sql.SQLException;

public class PatientsPageController extends UIController{
    private Stage stage;
    private Scene scene;
    private Parent root;
    private AccessDAO accessDAO;

    @FXML
    ListView<String> patients;

    public PatientsPageController() {
        accessDAO = new AccessDAO();
    }

    public ObservableList<String> getPatients(String FC) throws SQLException {
        ResultSet rs = accessDAO.selectDoctor(FC);
        ObservableList <String> patientsObservableList = FXCollections.observableArrayList();

        while(rs.next()){
            String patientFC = rs.getString("patientFiscalCode");
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

        stage = (Stage)((Node)mouseEvent.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}