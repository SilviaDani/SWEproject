package com.sweproject.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AccessDAO extends DAO{

    public ResultSet selectUser(String fiscalCode) throws SQLException {
        return statement.executeQuery("SELECT * FROM `Users` where `fiscalCode` =" + "'" + fiscalCode + "'");
    }

    public void insertNewUser(String fiscalCode, String firstName, String lastName, String password, String salt) throws SQLException {
        statement.execute("INSERT INTO `users` (`fiscalCode`, `firstName`, `surname`, `psw`, `salt`) VALUES ('" + fiscalCode + "', '" + firstName + "', '" + lastName +"', '" + password + "', '" + salt + "')");
    }

    public ResultSet selectDoctor(String fiscalCode) throws SQLException {
        return statement.executeQuery("SELECT * FROM `doctors` where `doctorFiscalCode` =" + "'" + fiscalCode + "'");
    }
}
