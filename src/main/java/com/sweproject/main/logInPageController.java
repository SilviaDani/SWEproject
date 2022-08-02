package com.sweproject.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import org.w3c.dom.Text;

import java.awt.*;
import java.io.IOException;
import java.sql.*;
import java.util.Objects;

public class logInPageController {
    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    TextField fiscalCode;
    @FXML
    PasswordField passwordField;
    @FXML
    Label name;

    public ResultSet isThereFC(String FC) throws SQLException{
        System.out.println("isThereFC?");
        String url = "jdbc:mysql://eu-cdbr-west-03.cleardb.net/heroku_f233c9395cfa736?reconnect=true";
        String user = "b7911f8c83c59f";
        String password = "4b132502";
        Connection myConn = DriverManager.getConnection(url, user, password);
        Statement myStmt = myConn.createStatement();
        String query = "SELECT * FROM `Users` where `fiscalCode` =" + "'" + FC + "'";
        System.out.println(query);
        ResultSet rs = myStmt.executeQuery(query);
        return rs;
    }

    public void logIn(ActionEvent event) throws IOException, SQLException {
        String FC = fiscalCode.getText();
        String password = passwordField.getText();
        ResultSet user = isThereFC(FC);
        System.out.println(user);
        if(user.next()){ //user.next() = true se non è vuoto, = false se è vuoto
            String DBName = user.getString("firstName");
            System.out.println(DBName);
            String DBPassword = user.getString("psw");
            System.out.println(DBPassword);
            if(Objects.equals(DBPassword, password)) {
                System.out.println("coiao");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("userPage.fxml"));
                Parent root = loader.load();

                //Parent root = FXMLLoader.load(getClass().getResource("userPage.fxml"));
                UserPageController UserPageController = loader.getController();
                UserPageController.setName("Welcome " + DBName);

                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            }
        }else{
            //TODO gestire il caso in cui non esista corrispondenza sul db
        }
    }
}
