package com.sweproject.controller;

import com.sweproject.dao.ObservationDAO;
import com.sweproject.model.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.w3c.dom.Text;

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
    @FXML private VBox add_cluster_vbox;
    @FXML private Button add_to_cluster;
    @FXML private Button remove_from_cluster;
    private boolean dateError;
    private static String eventType;
    private static LocalDateTime startDate;
    private static LocalDateTime endDate;
    private static CovidTestType testType;
    private static boolean positiveTest;
    private ArrayList<String> cluster;

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
       /* if(select_cluster_menu != null){
            select_cluster_menu.getItems().clear();
            //TODO scegliere quale cluster mostrare in base a quelli relativi all'utente
            select_cluster_menu.getItems().addAll("Cluster1","Cluster2"); //TODO in base ai cluster dell'utente, mostrare i cluster opportuni
        }*/
        if(add_cluster_vbox != null){
            cluster = new ArrayList<>();
            remove_from_cluster.setOnAction(e->removeFromCluster(1));
            remove_from_cluster.setDisable(true);
        }
    }


    public void confirm(ActionEvent event) throws SQLException, IOException {
        //TODO Trovare il notifier opportuno e fare addObservation con i dati presi sopra
        ArrayList<String> subjects = new ArrayList<>();
        Type type = null;
        //FIXME i parametri sono un po'a caso
        switch (eventType){
            case "Contact":
                subjects = cluster;
                subjects.add(user.getSubject().getFiscalCode());
                type = new Contact(subjects);
                //type = new Contact(subjects, Integer.parseInt(trasmission_rate.getText()));
                break;
            case "Symptoms":
                subjects.add(user.getSubject().getFiscalCode());
                type = new Symptoms();
                break;
            case "Covid test":
                subjects.add(user.getSubject().getFiscalCode());
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
        startDate = end_datePicker_menu.getValue().atTime(Integer.parseInt(end_date_hour.getText()), Integer.parseInt(end_date_minute.getText()));
        endDate = null;
        confirm(event);
        }
    }

    public void confirmInterval(ActionEvent event) throws IOException, SQLException {
        startDate = start_datePicker_menu.getValue().atTime(Integer.parseInt(start_date_hour.getText()), Integer.parseInt(start_date_minute.getText()));
        endDate = end_datePicker_menu.getValue().atTime(Integer.parseInt(end_date_hour.getText()), Integer.parseInt(end_date_minute.getText()));
        if(startDate.isBefore(endDate) && !dateError) {
            Parent root;
            if (eventType.equals("Contact")) {
                root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/addObservationPage_makeCluster.fxml"));
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

    public void addToCluster(){//TODO x silvia: fare scorrere se ci sono troppi membri del cluster
        int i = cluster.size()+1;
        System.out.println(((TextField) add_cluster_vbox.lookup("#text"+i)).getText());
        add_cluster_vbox.lookup("#add"+i).setDisable(true);
        add_cluster_vbox.lookup("#remove"+i).setDisable(false);
        cluster.add(((TextField) add_cluster_vbox.lookup("#text"+i)).getText()); //TODO controllare che il codice fiscale sia realmente esistente(?)
        i = cluster.size()+1;
        HBox newSubjectHbox = new HBox();
        newSubjectHbox.setAlignment(Pos.CENTER);
        newSubjectHbox.setId("hbox"+i);
        Label label = new Label(i+".");
        label.setId("label"+i);
        newSubjectHbox.getChildren().add(label);
        TextField textField = new TextField();
        textField.setId("text"+i);
        newSubjectHbox.getChildren().add(textField);
        Button add = new Button();
        add.setText("Add");
        add.setId("add"+i);
        add.setOnAction(e -> addToCluster());
        Button remove = new Button();
        remove.setText("Remove");
        remove.setId("remove"+i);
        remove.setDisable(true);
        int finalI = i;
        remove.setOnAction(e -> removeFromCluster(finalI));
        newSubjectHbox.getChildren().add(add);
        newSubjectHbox.getChildren().add(remove);
        HBox.setMargin(add, new Insets(0, 15, 0, 0));
        HBox.setMargin(remove, new Insets(0, 15, 0, 0));
        HBox.setMargin(textField, new Insets(0, 15, 0, 15));
        newSubjectHbox.setPrefHeight(50);
        add_cluster_vbox.getChildren().add(newSubjectHbox);
        System.out.println(cluster.size());
    }

    public void removeFromCluster(int index){
        int clSize = cluster.size() + 1;
        add_cluster_vbox.getChildren().remove(add_cluster_vbox.lookup("#hbox"+index));
        String removed = cluster.remove(index-1);
        System.out.println(removed);
        for(int i = index+1; i <= clSize; i++){
            int tmp = i-1;
            add_cluster_vbox.lookup("#hbox"+i).setId("hbox"+tmp);
            add_cluster_vbox.lookup("#label"+i).setId("label"+tmp);
            ((Label)add_cluster_vbox.lookup("#label"+tmp)).setText(tmp+".");
            add_cluster_vbox.lookup("#text"+i).setId("text"+tmp);
            add_cluster_vbox.lookup("#add"+i).setId("add"+tmp);
            add_cluster_vbox.lookup("#remove"+i).setId("remove"+tmp);
            ((Button)add_cluster_vbox.lookup("#remove"+tmp)).setOnAction(e->removeFromCluster(tmp));
        }
        System.out.println(cluster.size());
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
