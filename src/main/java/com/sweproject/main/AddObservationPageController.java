package com.sweproject.main;

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

public class AddObservationPageController implements Initializable {
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
    private ObservationDAO observationDAO;
    private Stage stage;
    private Scene scene;
    private Parent root;
    private static String eventType;
    private static LocalDateTime startDate;
    private static LocalDateTime endDate;
    private static CovidTestType testType;
    public static boolean positiveTest;
    private String FC;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        observationDAO = new ObservationDAO();
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

    public void setFC(String fiscalCode){
        FC = fiscalCode;
    }

    public void confirm(ActionEvent event) throws SQLException, IOException {
        //TODO Trovare il notifier opportuno e fare addObservation con i dati presi sopra
        // XXX INIZIO CODICE CHE VA RIMOSSO
        ArrayList<Subject> subjects = new ArrayList<>();
        subjects.add(new Subject("NCCNCL00P07D612X"));
        // XXX FINE CODICE CHE VA RIMOSSO
        Type type = null;
        //FIXME i parametri sono un po'a caso
        switch (eventType){
            case "Contact":
                type = new Contact(subjects, Integer.parseInt(trasmission_rate.getText()));
                break;
            case "Symptoms":
                //TODO subjects dovrebbe avere solo il codice fiscale del notifier
                type = new Symptoms();
                break;
            case "Covid test":
                type = new CovidTest(testType, positiveTest);
                //TODO subjects dovrebbe avere solo il codice fiscale del notifier
                break;
            default:
                System.out.println("Invalid type");
        }
        observationDAO.insertObservation(subjects, type, startDate, endDate);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("userPage.fxml"));
        Parent root = loader.load();

        //Parent root = FXMLLoader.load(getClass().getResource("userPage.fxml"));
        UserPageController UserPageController = loader.getController();
        //TODO ci sarebbe da sapere il nome dell'utente UserPageController.setName("Welcome " + DBName);

        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        //TODO aggiungere feedback visivo
        System.out.println("Aggiunta una nuova osservazione sul database");
    }

    public void confirmType(ActionEvent event) throws IOException {
        boolean isSymptomatic = false;
        eventType = observation_type_menu.getValue().toString();
        if(test_type_box.isVisible()) {
            positiveTest = positive_checkbox.isSelected();
            if(test_type_menu.getValue() == "Antigen")
                testType = CovidTestType.ANTIGEN;
            else if(test_type_menu.getValue() == "Molecular")
                testType = CovidTestType.MOLECULAR;
            else
                System.out.println("Errore: non è stato selezionato alcun tipo di test");
        }
        if(symptomatic_box.isVisible()) {
            if(symptomatic_checkbox.isSelected())
                isSymptomatic = true;
        }
        Parent root;
        if(eventType.equals("Contact") || (eventType.equals("Symptomatic") && !isSymptomatic)) {
            root = FXMLLoader.load(getClass().getResource("addObservationPage_chooseInterval.fxml"));
        }else{
            root = FXMLLoader.load(getClass().getResource("addObservationPage_chooseDate.fxml"));
        }
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void confirmDate(ActionEvent event) throws IOException, SQLException {
        //TODO come back to dashboard + add observation
        endDate = end_datePicker_menu.getValue().atTime(Integer.parseInt(end_date_hour.getText()), Integer.parseInt(end_date_minute.getText()));
        if(eventType.equals("Symptoms")){
            startDate = endDate;
            endDate = null;
        }
        confirm(event);
    }

    public void confirmInterval(ActionEvent event) throws IOException, SQLException {
        System.out.println(eventType);
        //TODO Fare controlli sull'ora inserita dall'utente - deve essere un'ora ammissibile! -
        startDate = start_datePicker_menu.getValue().atTime(Integer.parseInt(start_date_hour.getText()), Integer.parseInt(start_date_minute.getText()));
        endDate = end_datePicker_menu.getValue().atTime(Integer.parseInt(end_date_hour.getText()), Integer.parseInt(end_date_minute.getText()));
        Parent root;
        if(eventType.equals("Contact")) {
            root = FXMLLoader.load(getClass().getResource("addObservationPage_chooseCluster.fxml"));
            stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }else{
            confirm(event);
        }

    }
    public void backToTypeSelection(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("addObservationPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void backToIntervalSelection(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("addObservationPage_chooseInterval.fxml"));
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
}
