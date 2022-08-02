package com.sweproject.main;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class UserPageController{
    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    Label welcome_user;
    @FXML
    Label name;

    public void setName(String firstName){
        name.setText(firstName);
    }

    public void enterRestrictedArea(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("tracerPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void addObservation(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("addObservationPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
