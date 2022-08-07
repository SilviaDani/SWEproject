package com.sweproject.main;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    private Stage stage;
    private Scene scene;
    private Parent root;
    private AccessDAO accessDAO;

    @FXML
    Label name;
    @FXML private Button reservedArea_button;

    public UserPageController() {
        accessDAO = new AccessDAO();
    }

    public void enterRestrictedArea(ActionEvent event) throws IOException, SQLException {
        Parent root = FXMLLoader.load(getClass().getResource("tracerPage.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void addObservation(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("addObservationPage.fxml"));
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
        ResultSet user = accessDAO.selectDoctor(UIController.user.getFiscalCode());
        if(!user.next()) {
            reservedArea_button.setVisible(false);
        }else{
            reservedArea_button.setVisible(true);
        }
        }catch(Exception e){
            System.out.println("Errore in UserPageController.initialize()");
        }
    }
}
