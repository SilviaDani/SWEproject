package com.sweproject.controller;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import com.sweproject.dao.AccessDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class UserPageController extends UIController implements Initializable{
    @FXML
    Label name;
    @FXML private Button reservedArea_button;

    public UserPageController() {
        accessDAO = new AccessDAO();
    }

    public void enterRestrictedArea(ActionEvent event) throws IOException, SQLException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sweproject/FXML/patientsPage.fxml"));
        Parent root = loader.load();
        PatientsPageController PatientsPageController = loader.getController();
        PatientsPageController.addPatients(user.getFiscalCode());

        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void addObservation(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sweproject/FXML/addObservationPage.fxml"));
        Parent root = loader.load();

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        name.setText("Welcome "+user.getName());
        try{
            ArrayList<HashMap<String, Object>> user = accessDAO.selectDoctor(UIController.user.getFiscalCode());
        if(user.size() == 0) {
            reservedArea_button.setVisible(false);
        }else{
            reservedArea_button.setVisible(true);
        }
        }catch(Exception e){
            System.out.println("Errore in UserPageController.initialize()");
        }
    }
}
