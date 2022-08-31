package com.sweproject.dao;

import com.sweproject.model.Subject;
import com.sweproject.model.Symptoms;
import com.sweproject.model.Type;
import org.junit.jupiter.api.Test;

import javax.xml.transform.Result;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ObservationDAOTest {


    @Test
    void AllFunctionsTest_insertWithEndDate() {
        ObservationDAO observationDAO = new ObservationDAO();
        ArrayList<String> subjects = new ArrayList<>();
        String fiscalCode = "RSSMRA80A41H501Y";
        subjects.add(fiscalCode);
        Type type = new Symptoms();
        LocalDateTime startDate = LocalDateTime.of(1980, 1, 1,0, 0), endDate = LocalDateTime.of(2022, 12, 31, 23, 59);
        try {
            observationDAO.insertObservation(subjects, type, startDate, endDate);
            ArrayList<HashMap<String, Object>> arrayList = observationDAO.getRelevantObservations(fiscalCode);
            assertNotEquals(0, arrayList.size());
            assertEquals(fiscalCode, arrayList.get(0).get("fiscalCode"));
            assertEquals(type.getName(), arrayList.get(0).get("type"));
            assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(0).get("start_date"));
            assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(0).get("end_date"));
            assertEquals(true, arrayList.get(0).get("is_relevant"));
            String id = arrayList.get(0).get("id").toString();
            observationDAO.changeRelevance(id);
            arrayList = observationDAO.getRelevantObservations(fiscalCode);
            assertEquals(0, arrayList.size());
            //non ci dovrebbero essere altre osservazioni rilevanti a carico di Maria Rossi e quindi resultSet.next() dovrebbe restituire false
            deleteObservation(id);
        }catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Test
    void AllFunctionsTest_insertWithoutEndDate() {
        ObservationDAO observationDAO = new ObservationDAO();
        ArrayList<String> subjects = new ArrayList<>();
        String fiscalCode = "RSSMRA80A41H501Y";
        subjects.add(fiscalCode);
        Type type = new Symptoms();
        LocalDateTime startDate = LocalDateTime.of(1980, 1, 1,0, 0);
        try {
            observationDAO.insertObservation(subjects, type, startDate, null);
            ArrayList<HashMap<String, Object>> arrayList = observationDAO.getRelevantObservations(fiscalCode);
            assertNotEquals(0, arrayList.size());
            assertEquals(fiscalCode, arrayList.get(0).get("fiscalCode"));
            assertEquals(type.getName(), arrayList.get(0).get("type"));
            assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(0).get("start_date"));
            assertEquals(true, arrayList.get(0).get("is_relevant"));
            String id = arrayList.get(0).get("id").toString();
            observationDAO.changeRelevance(id);
            arrayList = observationDAO.getRelevantObservations(fiscalCode);
            assertEquals(0, arrayList.size());
            //non ci dovrebbero essere altre osservazioni rilevanti a carico di Maria Rossi e quindi resultSet.next() dovrebbe restituire false
            deleteObservation(id);
        }catch (Exception e){
            fail(e.getMessage());
        }
    }
    public static int findObservation(String id){
        String url = "jdbc:mysql://tracingapp.cqftfh4tbbqi.eu-south-1.rds.amazonaws.com:3306/";
        Properties prop = new Properties();
        try{
            FileInputStream fis = new FileInputStream("src/main/res/database_login.config");
            prop.load(fis);
        }catch(IOException ex){
            System.out.println("Impossibile trovare le credenziali per l'accesso al database");
        }
        String user = prop.getProperty("db.user");
        String password = prop.getProperty("db.password");
        Connection connection = null;
        Statement statement = null;
        int count = -1;
        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            statement.execute("use `tracing-app`");
            ResultSet rs = statement.executeQuery("SELECT count(*) AS `clusterCount` FROM `observations` WHERE `id` = '"+id+"'");
            rs.next();
            count = rs.getInt("clusterCount");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Si è verificato un problema durante l'eliminazione delle osservazioni");
        }finally {
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
                }catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return count;
    }

    public static void deleteObservation(String id){
        String url = "jdbc:mysql://tracingapp.cqftfh4tbbqi.eu-south-1.rds.amazonaws.com:3306/";
        Properties prop = new Properties();
        try{
            FileInputStream fis = new FileInputStream("src/main/res/database_login.config");
            prop.load(fis);
        }catch(IOException ex){
            System.out.println("Impossibile trovare le credenziali per l'accesso al database");
        }
        String user = prop.getProperty("db.user");
        String password = prop.getProperty("db.password");
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            statement.execute("use `tracing-app`");
            statement.execute("DELETE FROM `events` WHERE `ID` = '" + id + "'");
            statement.execute("DELETE FROM `observations` WHERE `id` = '" + id + "'");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Si è verificato un problema durante l'eliminazione delle osservazioni");
        }finally {
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
                }catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            
        }
    }
}