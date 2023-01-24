package com.sweproject.gateway;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class Gateway {
    protected String url = "jdbc:mysql://tracingapp.cqftfh4tbbqi.eu-south-1.rds.amazonaws.com:3306/";
    protected Connection connection;
    protected Statement statement;
    protected static String user;
    protected static String password;


    protected void setConnection(){
        try {
            connection = DriverManager.getConnection(this.url, this.user, this.password);
            statement = connection.createStatement();
            statement.execute("use `tracing-app`");
        }catch (Exception e){
            System.out.println("Si è verificato un problema durante la connessione al database.");
        }
    }
    public Gateway() {
        Properties prop = new Properties();
        try{
            FileInputStream fis = new FileInputStream("src/main/res/database_login.config");
            prop.load(fis);
        }catch(IOException ex){
            System.out.println("Impossibile trovare le credenziali per l'accesso al database");
        }
        user = prop.getProperty("db.user");
        password = prop.getProperty("db.password");
        setConnection();
    }


    protected void closeConnection(ResultSet resultSet){
        if(resultSet!=null) {
            try {
                resultSet.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        closeConnection();
    }

    protected void closeConnection(){
        if(statement!=null) {
            try {
                statement.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if(connection!=null) {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    protected ArrayList<HashMap<String, Object>> convertResultSet(ResultSet rs){
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            while(rs.next()){
                HashMap<String, Object> hashMap = new HashMap<>();
                for(int i = 1; i<=cols; i++){
                    hashMap.put(rsmd.getColumnName(i) ,rs.getObject(i));
                }
                arrayList.add(hashMap);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return arrayList;
    }
}
