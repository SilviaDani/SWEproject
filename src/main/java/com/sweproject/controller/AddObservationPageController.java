package com.sweproject.controller;

import com.sweproject.dao.ObservationDAO;
import com.sweproject.model.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class AddObservationPageController extends UIController implements Initializable{
    @FXML private Label error_observation;
    @FXML private ComboBox observation_type_menu;
    @FXML private ComboBox select_cluster_menu;
    @FXML private HBox test_type_box;
    @FXML private ComboBox test_type_menu;
    @FXML private CheckBox positive_checkbox;
    @FXML private HBox symptomatic_box;
    @FXML private CheckBox symptomatic_checkbox;
    @FXML private DatePicker start_datePicker_menu;
    @FXML private DatePicker end_datePicker_menu;
    @FXML private TextField start_date_hour;
    @FXML private TextField start_date_minute;
    @FXML private TextField end_date_hour;
    @FXML private TextField end_date_minute;
    @FXML private TextField trasmission_rate;
    @FXML private Label error_interval;
    private boolean dateError;
    private Stage stage;
    private Scene scene;
    private Parent root;
    private static String eventType;
    private static LocalDateTime startDate;
    private static LocalDateTime endDate;
    private static CovidTestType testType;
    private static boolean positiveTest;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if(observation_type_menu != null) {
            observation_type_menu.getItems().clear();
            observation_type_menu.getItems().addAll("Contact", "Symptoms", "Covid test");
            test_type_box.setManaged(false);
            test_type_menu.getItems().clear();
            test_type_menu.getItems().addAll("Antigen", "Molecular");
            symptomatic_box.setManaged(false);
        }
        if(select_cluster_menu != null){
            select_cluster_menu.getItems().clear();
            //TODO scegliere quale cluster mostrare in base a quelli relativi all'utente
            select_cluster_menu.getItems().addAll("Cluster1","Cluster2"); //TODO in base ai cluster dell'utente, mostrare i cluster opportuni
        }
    }


    public void confirm(ActionEvent event) throws SQLException, IOException {
        //TODO Trovare il notifier opportuno e fare addObservation con i dati presi sopra
        ArrayList<Subject> subjects = new ArrayList<>();
        Type type = null;
        //FIXME i parametri sono un po'a caso
        switch (eventType){
            case "Contact":
                // XXX INIZIO CODICE CHE VA RIMOSSO
                subjects.add(new Subject("NCCNCL00P07D612X","Niccolò","Niccoli"));
                // XXX FINE CODICE CHE VA RIMOSSO
                type = new Contact(subjects, Integer.parseInt(trasmission_rate.getText()));
                break;
            case "Symptoms":
                subjects.add(user.getSubject());
                type = new Symptoms();
                break;
            case "Covid test":
                subjects.add(user.getSubject());
                type = new CovidTest(testType, positiveTest);
                break;
            default:
                System.out.println("Invalid type");
        }
        user.addObservation(subjects, type, startDate, endDate);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sweproject/FXML/userPage.fxml"));
        Parent root = loader.load();
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        //TODO aggiungere feedback visivo
        System.out.println("Aggiunta una nuova osservazione sul database");
    }

    public void confirmType(ActionEvent event) throws IOException {
        boolean isSymptomatic = false;
        try {
            try {
                eventType = observation_type_menu.getValue().toString();
            }catch (NullPointerException npe){
                throw new NullPointerException("Please select an observation type");
            }
            if (test_type_box.isVisible()) {
                positiveTest = positive_checkbox.isSelected();
                if (test_type_menu.getValue() == "Antigen")
                    testType = CovidTestType.ANTIGEN;
                else if (test_type_menu.getValue() == "Molecular")
                    testType = CovidTestType.MOLECULAR;
                else {
                    throw new NullPointerException("Please select a test type");
                }
            }
            if (symptomatic_box.isVisible()) {
                if (symptomatic_checkbox.isSelected())
                    isSymptomatic = true;
            }
            Parent root;
            if (eventType.equals("Contact") || (eventType.equals("Symptoms") && !isSymptomatic)) {
                root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/addObservationPage_chooseInterval.fxml"));
            } else {
                root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/addObservationPage_chooseDate.fxml"));
            }
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }catch(NullPointerException e){
            error_observation.setText(e.getMessage());
            error_observation.setVisible(true);
        }
    }

    public void confirmDate(ActionEvent event) throws IOException, SQLException {
        if(!dateError){
        endDate = end_datePicker_menu.getValue().atTime(Integer.parseInt(end_date_hour.getText()), Integer.parseInt(end_date_minute.getText()));
        if(eventType.equals("Symptoms")){
            startDate = endDate;
            endDate = null;
        }
        confirm(event);
    }
    }

    public void confirmInterval(ActionEvent event) throws IOException, SQLException {
        startDate = start_datePicker_menu.getValue().atTime(Integer.parseInt(start_date_hour.getText()), Integer.parseInt(start_date_minute.getText()));
        endDate = end_datePicker_menu.getValue().atTime(Integer.parseInt(end_date_hour.getText()), Integer.parseInt(end_date_minute.getText()));
        if(startDate.isBefore(endDate) && !dateError) {
            Parent root;
            if (eventType.equals("Contact")) {
                root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/addObservationPage_chooseCluster.fxml"));
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            } else {
                confirm(event);
            }
        }else{
            error_interval.setText("Insert valid date interval");
            error_interval.setVisible(true);
        }

    }
    public void backToTypeSelection(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/addObservationPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void backToIntervalSelection(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/addObservationPage_chooseInterval.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void checkObservationType(ActionEvent event) {
        if (observation_type_menu.getValue().toString().equals("Covid test")) {
            test_type_box.setVisible(true);
            test_type_box.setManaged(true);
            symptomatic_box.setVisible(false);
            symptomatic_box.setManaged(false);
        }else if(observation_type_menu.getValue().toString().equals("Symptoms")){
            symptomatic_box.setVisible(true);
            symptomatic_box.setManaged(true);
            test_type_box.setVisible(false);
            test_type_box.setManaged(false);
        }else {
            test_type_box.setVisible(false);
            test_type_box.setManaged(false);
            symptomatic_box.setVisible(false);
            symptomatic_box.setManaged(false);
        }
    }

    public void validateHourSelected(){
        try {
            int start_hour = start_date_hour != null ? Integer.parseInt(start_date_hour.getText()) : 0;
            int end_hour = end_date_hour != null ? Integer.parseInt(end_date_hour.getText()) : 0;
            dateError = true;
            if (0 > start_hour || start_hour > 23) {
                error_interval.setText("Insert valid start hour");
            } else if (0 > end_hour || end_hour > 23) {
                error_interval.setText("Insert valid end hour");
            } else {
                dateError = false;
            }
        }catch(Exception e){
            error_interval.setText("Insert a number, not a text");
            dateError = true;
        }
        if (dateError)
            error_interval.setVisible(true);
        else
            error_interval.setVisible(false);
    }
    public void validateMinutesSelected(){
        try{
        int start_minutes = start_date_minute != null ? Integer.parseInt(start_date_minute.getText()) : 0;
        int end_minutes = end_date_minute != null ? Integer.parseInt(end_date_minute.getText()) : 0;
        dateError = true;
        if (0 > start_minutes|| start_minutes  > 59){
            error_interval.setText("Insert valid start minute");
        } else if (0 > end_minutes || end_minutes > 59){
            error_interval.setText("Insert valid end minutes");
        }else{
            dateError = false;
        }
        }catch(Exception e){
            error_interval.setText("Insert a number, not a text");
            dateError = true;
        }
        if (dateError)
            error_interval.setVisible(true);
        else
            error_interval.setVisible(false);
    }
}