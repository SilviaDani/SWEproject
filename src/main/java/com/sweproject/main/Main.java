package com.sweproject.main;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    /*public static void main(String[] args) {
        Notifier foo = new Notifier();
        ArrayList<Subject> bar = new ArrayList<>();
        bar.add(foo.getSubject());
        Type t = new Symptoms();
        TimeRecord tr = new Date(LocalDateTime.now());
        foo.createObservation(bar, t, tr);

        Tracer tracer = new Tracer();
        tracer.createPrescription(foo.getSubject(), new Date(LocalDateTime.of(2000,9,7,11,20)), new CovidTest(CovidTestType.MOLECULAR));

    }*/
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("index.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}

