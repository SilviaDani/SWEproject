package com.sweproject.main;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class IndexController extends UIController{

    private Stage stage;
    private Scene scene;
    private Parent root;

    public void logInClicked(ActionEvent event) throws IOException{
        Parent root = FXMLLoader.load(getClass().getResource("logInPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void signInClicked(ActionEvent event) throws IOException{
        Parent root = FXMLLoader.load(getClass().getResource("signUpPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }


}


