package com.sweproject.gateway;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class AccessGateway extends Gateway {

    public ArrayList<HashMap<String, Object>> selectUser(String fiscalCode){
        ResultSet rs = null;
        ArrayList<HashMap<String, Object>> arrayList = null;
        try {
            setConnection();
            fiscalCode = fiscalCode.toUpperCase();
            rs = statement.executeQuery("SELECT * FROM users where fiscalCode =" + "'" + fiscalCode + "'");
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
            fiscalCode = fiscalCode.toUpperCase();
            statement.execute("INSERT INTO users (fiscalCode, firstName, surname, password, salt) VALUES ('" + fiscalCode + "', '" + firstName + "', '" + lastName + "', '" + password + "', '" + salt + "')");
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
            rs = statement.executeQuery("SELECT * FROM doctors where doctorFiscalCode =" + "'" + fiscalCode + "'");
            arrayList = convertResultSet(rs);
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            closeConnection(rs);
        }
        return arrayList;
    }

}
