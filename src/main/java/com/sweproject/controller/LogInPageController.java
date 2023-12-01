package com.sweproject.controller;

import com.sweproject.model.Notifier;
import com.sweproject.gateway.AccessGateway;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class LogInPageController extends UIController{

    @FXML private javafx.scene.control.Label errorText;
    @FXML
    TextField fiscalCode;
    @FXML
    PasswordField passwordField;

    public LogInPageController() {
        accessGateway = new AccessGateway();
    }

    public void logIn(ActionEvent event) throws IOException, SQLException {
        String FC = fiscalCode.getText();
        ArrayList<HashMap<String, Object>> user = accessGateway.selectUser(FC);
        if(user.size() > 0){ //user.next() = true se non è vuoto, = false se è vuoto
            String password = BCrypt.hashpw(passwordField.getText(), user.get(0).get("SALT").toString());
            String DBName = user.get(0).get("FIRSTNAME").toString();
            String DBPassword = user.get(0).get("PASSWORD").toString();
            if(Objects.equals(DBPassword, password)) {
                UIController.user = new Notifier(FC, DBName, user.get(0).get("SURNAME").toString());
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sweproject/FXML/userPage.fxml"));
                Parent root = loader.load();

                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }else{
                errorText.setText("Password is not correct");
            }
        }else{
            errorText.setText("This account does not exist");
        }
    }
}
