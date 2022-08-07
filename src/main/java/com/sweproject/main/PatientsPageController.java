package com.sweproject.main;

import com.sweproject.dao.AccessDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PatientsPageController {
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

        while(rs.next()) {
            String patientFC = rs.getString("patientFiscalCode");
            patientsObservableList.add(patientFC);
        }
        System.out.println(patientsObservableList);
        return patientsObservableList;
    }

    public void addLabel(String FC) throws SQLException {
        patients.setItems(getPatients(FC));
    }
}
