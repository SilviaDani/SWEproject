package com.sweproject.controller;

import com.sweproject.controller.UIController;
import com.sweproject.dao.AccessDAO;
import com.sweproject.dao.ObservationDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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

public class PatientsObservationsController extends UIController {
    private Stage stage;
    private Scene scene;
    private Parent root;
    private ObservationDAO observationDAO;

    @FXML
    ListView<String> observations;
    @FXML
    ListView<String> observationsId;

    public PatientsObservationsController() {
        observationDAO = new ObservationDAO();
    }

    public void addObservations(String FC) throws SQLException {
        int ROW_HEIGHT = 24;
        ObservableList <String> [] observableLists = getObservations(FC);
        System.out.println(observableLists.length + " observableList.length");
        ObservableList <String> observableListObservations = observableLists[0];
        observations.setItems(observableListObservations);
        if(observableListObservations.size() == 0){
            //TODO dire che non ci sono osservazioni rilevanti
            System.out.println("Ooopsie dooopsie, non ci sono osservazioni rilevanti");
        }
        observations.setPrefHeight(observableListObservations.size() * ROW_HEIGHT + 2);

        ObservableList <String> observableListObservationsId = observableLists[1];
        observationsId.setItems(observableListObservationsId);
        observationsId.setPrefHeight(observableListObservationsId.size() * ROW_HEIGHT + 2);
    }

    public ObservableList<String>[] getObservations(String FC) throws SQLException {
        ResultSet rs = observationDAO.getRelevantObservations(FC);
        ObservableList <String> observationsObservableList = FXCollections.observableArrayList();
        ObservableList <String> observationsIdObservableList = FXCollections.observableArrayList();

        while(rs.next()){
            String observationType = rs.getString("type");
            String observationDate = rs.getString("start_date");
            String observationsId = rs.getString("ID");

            observationsObservableList.add(observationType + " " + observationDate);
            observationsIdObservableList.add(observationsId);
        }
        return new ObservableList[]{observationsObservableList, observationsIdObservableList};
    }

    public void handleObservationClick(MouseEvent mouseEvent) throws IOException, SQLException {
        int index = observations.getSelectionModel().getSelectedIndex();
        observationsId.getSelectionModel().select(index);
        String selectedObservationID = observationsId.getSelectionModel().getSelectedItem();
        observationDAO.changeRelevance(selectedObservationID);

        //TODO AGGIORNA TABELLA OSSERVAZIONI
    }

    public void fillPrescription(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sweproject/FXML/fillPrescriptionPage.fxml"));
        Parent root = loader.load();

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
