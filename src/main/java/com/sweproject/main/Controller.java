package com.sweproject.main;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;

import java.io.IOException;

public class Controller {

    private Stage stage;
    private Scene scene;
    private Parent root;

    public void logInClicked(MouseEvent event) throws IOException{
        Parent root = FXMLLoader.load(getClass().getResource("logInPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void signInClicked(MouseEvent event) {
    }


}


