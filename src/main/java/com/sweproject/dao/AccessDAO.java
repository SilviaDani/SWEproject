package com.sweproject.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class AccessDAO extends DAO{

    public ArrayList<HashMap<String, Object>> selectUser(String fiscalCode){
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            rs = statement.executeQuery("SELECT * FROM `Users` where `fiscalCode` =" + "'" + fiscalCode + "'");
            arrayList = convertResultSet(rs);
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

    public void insertNewUser(String fiscalCode, String firstName, String lastName, String password, String salt){
        try {
            setConnection();
            statement.execute("INSERT INTO `users` (`fiscalCode`, `firstName`, `surname`, `psw`, `salt`) VALUES ('" + fiscalCode + "', '" + firstName + "', '" + lastName + "', '" + password + "', '" + salt + "')");
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection();
        }
    }

    public ArrayList<HashMap<String, Object>> selectDoctor(String fiscalCode) throws SQLException {
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            rs = statement.executeQuery("SELECT * FROM `doctors` where `doctorFiscalCode` =" + "'" + fiscalCode + "'");
            arrayList = convertResultSet(rs);
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

}
