package com.sweproject.main;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;


public class Main extends Application {

    public static final boolean DEBUG = true;

    /* CODICE PER USARE DB
    *
    * LOGIN
    *   String url = "jdbc:mysql://eu-cdbr-west-03.cleardb.net/heroku_f233c9395cfa736?reconnect=true";
        String user = "b7911f8c83c59f";
        String password = "4b132502";
        Connection myConn = DriverManager.getConnection(url, user, password);
        Statement myStmt = myConn.createStatement();
        *
        * PER FARE RICHIESTE
        String sql = "INSERT INTO `users` (`fiscalCode`, `firstName`, `surname`, `psw`) VALUES ('NCCNCL00P07D612X', 'Niccolò', 'Niccoli', 'password')";
        myStmt.execute(sql);
        ResultSet rs1 = myStmt.executeQuery("select * from users");

            * PER STAMPARE
        while(rs1.next())
            System.out.println(rs1.getString("firstName"));
    *
    *
    */


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
        Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/index.fxml"));
        primaryStage.setTitle("ChITA");
        primaryStage.getIcons().add(new Image(new File("src/main/res/icon.png").toURI().toString()));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

}

