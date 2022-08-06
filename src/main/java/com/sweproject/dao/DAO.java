package com.sweproject.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DAO {
    protected String url = "jdbc:mysql://eu-cdbr-west-03.cleardb.net/heroku_f233c9395cfa736?reconnect=true";
    protected String user = "b7911f8c83c59f";
    protected String password = "4b132502";
    protected Connection connection;
    protected Statement statement;
    
    public DAO() {
        try {
            connection = DriverManager.getConnection(this.url, this.user, this.password);
            statement = connection.createStatement();
        }catch (Exception e){
            System.out.println("Si è verificato un problema durante la connessione al database.");
        }
    }
}
