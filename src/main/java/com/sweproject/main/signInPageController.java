package com.sweproject.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.Objects;

public class signInPageController {

    private Stage stage;
    private Scene scene;
    private Parent root;

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

    public boolean isThereFC(String FC) throws SQLException {
        String url = "jdbc:mysql://eu-cdbr-west-03.cleardb.net/heroku_f233c9395cfa736?reconnect=true";
        String user = "b7911f8c83c59f";
        String password = "4b132502";
        Connection myConn = DriverManager.getConnection(url, user, password);
        Statement myStmt = myConn.createStatement();
        String query = "SELECT * FROM `Users` where `fiscalCode` =" + "'" + FC + "'";
        System.out.println(query);
        ResultSet rs = myStmt.executeQuery(query);
        if(rs.next())
            return true;
        else
            return false;
    }

    public void signIn(ActionEvent event) throws IOException, SQLException {
        String FC = fiscalCode.getText();
        String firstName = name.getText();
        String lastName = surname.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        if (isThereFC(FC)){
            //TODO ACCOUNT ALREADY EXISTS
        }
        else{
            if (firstName.length() == 0 || lastName.length() == 0 || password.length() == 0 || confirmPassword.length() == 0){
                //TODO RIEMPIRE TUTTI I CAMPI
            }
            else if (!Objects.equals(confirmPassword, password)){
                //TODO LE PASSWORD SONO DIVERSE
            }
            else {
                String url = "jdbc:mysql://eu-cdbr-west-03.cleardb.net/heroku_f233c9395cfa736?reconnect=true";
                String user = "b7911f8c83c59f";
                String passwordDB = "4b132502";
                Connection myConn = DriverManager.getConnection(url, user, passwordDB);
                Statement myStmt = myConn.createStatement();
                String query = "INSERT INTO `users` (`fiscalCode`, `firstName`, `surname`, `psw`) VALUES ('" + FC + "', '" + firstName + "', '" + lastName +"', '" + password + "')";
                myStmt.execute(query);

                Parent root = FXMLLoader.load(getClass().getResource("index.fxml"));
                stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
        }
    }
}
