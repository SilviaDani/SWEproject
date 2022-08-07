package com.sweproject.main;

import com.sweproject.dao.AccessDAO;
import com.sweproject.dao.ObservationDAO;
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

public class PatientsObservationsController {
    private Stage stage;
    private Scene scene;
    private Parent root;
    private ObservationDAO observationDAO;

    @FXML
    ListView<String> observations;

    public PatientsObservationsController() {
        observationDAO = new ObservationDAO();
    }

    public void addObservations(String FC) throws SQLException {
        int ROW_HEIGHT = 24;
        ObservableList <String> observableListObservations = getObservations(FC);
        observations.setItems(observableListObservations);
        observations.setPrefHeight(observableListObservations.size() * ROW_HEIGHT + 2);
    }

    public ObservableList<String> getObservations(String FC) throws SQLException {
        ResultSet rs = observationDAO.getRelevantObservations(FC);
        ObservableList <String> observationsObservableList = FXCollections.observableArrayList();

        while(rs.next()){
            String observationType = rs.getString("type");
            observationsObservableList.add(observationType);
        }

        return observationsObservableList;
    }

    public void handleObservationClick(MouseEvent mouseEvent) throws IOException {
        String selectedObservation = observations.getSelectionModel().getSelectedItem();
        Parent root = FXMLLoader.load(getClass().getResource("observationPage.fxml"));
        //TODO CARICARE IN UNA NUOVA PAGINA FXML LE INFORMAZIONI DELL'OSSERVAZIONE E POTER CAMBIARNE LA RILEVANZA
        stage = (Stage)((Node)mouseEvent.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
