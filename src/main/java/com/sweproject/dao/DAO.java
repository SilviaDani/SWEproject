package com.sweproject.dao;

import com.sweproject.model.Observation;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DAO {
    protected String url = "jdbc:mysql://tracingapp.cqftfh4tbbqi.eu-south-1.rds.amazonaws.com:3306/";
    protected String user = "admin";
    protected String password = "password";
    protected Connection connection;
    protected Statement statement;


    protected void setConnection(){
        try {
            connection = DriverManager.getConnection(this.url, this.user, this.password);
            statement = connection.createStatement();
            statement.execute("use `tracing-app`");
        }catch (Exception e){
            System.out.println("Si è verificato un problema durante la connessione al database.");
        }
    }
    public DAO() {
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
