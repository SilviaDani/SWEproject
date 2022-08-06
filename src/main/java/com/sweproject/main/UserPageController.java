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
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class UserPageController{
    private Stage stage;
    private Scene scene;
    private Parent root;
    private String FC;
    private AccessDAO accessDAO;

    @FXML
    Label name;

    public UserPageController() {
        accessDAO = new AccessDAO();
    }

    public void setName(String firstName){
        name.setText(firstName);
    }

    public void setFC(String fiscalCode){
        FC = fiscalCode;
    }

    public void enterRestrictedArea(ActionEvent event) throws IOException, SQLException {
        ResultSet user = accessDAO.selectDoctor(FC);
        if(user.next()) {
            Parent root = FXMLLoader.load(getClass().getResource("tracerPage.fxml"));
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }
        else {
            //TODO NICK PERSONALE NON AUTORIZZATO testo errore
        }
    }

    public void addObservation(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("addObservationPage.fxml"));
        Parent root = loader.load();

        AddObservationPageController AddObservationPageController = loader.getController();
        AddObservationPageController.setFC(FC);
        System.out.println(FC);

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
