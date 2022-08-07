package com.sweproject.main;

import com.sweproject.controller.Notifier;
import com.sweproject.dao.AccessDAO;
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

import java.awt.*;
import java.io.IOException;
import java.sql.*;
import java.util.Objects;

public class LogInPageController extends UIController{
    private Stage stage;
    private Scene scene;
    private Parent root;
    private AccessDAO accessDAO;

    @FXML private javafx.scene.control.Label errorText;
    @FXML
    TextField fiscalCode;
    @FXML
    PasswordField passwordField;

    public LogInPageController() {
        accessDAO = new AccessDAO();
    }

    public void logIn(ActionEvent event) throws IOException, SQLException {
        String FC = fiscalCode.getText();
        ResultSet user = accessDAO.selectUser(FC);
        if(user.next()){ //user.next() = true se non è vuoto, = false se è vuoto
            String password = BCrypt.hashpw(passwordField.getText(), user.getString("salt"));
            String DBName = user.getString("firstName");
            String DBPassword = user.getString("psw");
            if(Objects.equals(DBPassword, password)) {
                UIController.user = new Notifier(FC, DBName, user.getString("surname"));
                FXMLLoader loader = new FXMLLoader(getClass().getResource("userPage.fxml"));
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
