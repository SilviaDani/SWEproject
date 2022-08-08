package com.sweproject.controller;

import com.sweproject.dao.AccessDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.sql.*;
import java.util.Objects;

public class SignUpPageController extends UIController {

    private Stage stage;
    private Scene scene;
    private Parent root;
    private AccessDAO accessDAO;
    @FXML private Label errorText;

    @FXML
    TextField fiscalCode;
    @FXML
    TextField name;
    @FXML
    TextField surname;
    @FXML
    PasswordField passwordField;
    @FXML
    PasswordField confirmPasswordField;

    public SignUpPageController() {
        accessDAO = new AccessDAO();
    }

    public void signUp(ActionEvent event) throws IOException, SQLException {
        String FC = fiscalCode.getText();
        String firstName = name.getText();
        String lastName = surname.getText();
        String salt = BCrypt.gensalt();
        String password = BCrypt.hashpw(passwordField.getText(), salt);
        String confirmPassword = BCrypt.hashpw(confirmPasswordField.getText(), salt);
        if (accessDAO.selectUser(FC).next()){
            errorText.setText("This account already exists");
        }
        else{
            if (firstName.length() == 0 || lastName.length() == 0 || password.length() == 0 || confirmPassword.length() == 0){
                errorText.setText("Please enter all fields");
            }
            else if (!Objects.equals(confirmPassword, password)){
                errorText.setText("Passwords do not match");
            }
            else {
                accessDAO.insertNewUser(FC, firstName, lastName, password, salt);
                Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/index.fxml"));
                stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
        }
    }
}
