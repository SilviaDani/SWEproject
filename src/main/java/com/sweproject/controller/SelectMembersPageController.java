package com.sweproject.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class SelectMembersPageController extends UIController implements Initializable {
    @FXML private VBox add_cluster_vbox;
    @FXML private Button add_to_cluster;
    @FXML private Button remove_from_cluster;
    private ArrayList<String> cluster;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cluster = new ArrayList<>();
        remove_from_cluster.setOnAction(e->removeFromCluster(1));
        remove_from_cluster.setDisable(true);
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

    public void addToCluster() {
        int i = cluster.size()+1;
        System.out.println(((TextField) add_cluster_vbox.lookup("#text"+i)).getText());
        add_cluster_vbox.lookup("#add"+i).setDisable(true);
        add_cluster_vbox.lookup("#remove"+i).setDisable(false);
        cluster.add(((TextField) add_cluster_vbox.lookup("#text"+i)).getText().toUpperCase());
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

    public void back(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/userPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void confirm(ActionEvent event) throws IOException {
        cluster.add(user.getFiscalCode().toUpperCase());
        data = cluster;
        Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/contagiousLevelPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
