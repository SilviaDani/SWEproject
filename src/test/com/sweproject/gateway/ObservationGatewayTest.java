package com.sweproject.gateway;

import com.sweproject.model.Symptoms;
import com.sweproject.model.Type;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ObservationGatewayTest {


    @Test
    void AllFunctionsTest_insertWithEndDate() {
        ObservationGateway observationDAO = new ObservationGateway();
        AccessGateway accessGateway = new AccessGateway();
        ArrayList<String> subjects = new ArrayList<>();
        String fiscalCode = "RSSMRA80A41H501Y";
        subjects.add(fiscalCode);
        Type type = new Symptoms();
        LocalDateTime startDate = LocalDateTime.of(1980, 1, 1,0, 0), endDate = LocalDateTime.of(2022, 12, 31, 23, 59);
        try {
            accessGateway.insertNewUser(fiscalCode, "xx", "yy", "zz", "ww");
            observationDAO.insertObservation(subjects, type, startDate, endDate);
            ArrayList<HashMap<String, Object>> arrayList = observationDAO.getRelevantObservations(fiscalCode);
            assertNotEquals(0, arrayList.size());
            assertEquals(fiscalCode, arrayList.get(0).get("fiscalCode".toUpperCase()));
            assertEquals(type.getName(), arrayList.get(0).get("type".toUpperCase()));
            assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), (((oracle.sql.TIMESTAMP) arrayList.get(0).get("start_date".toUpperCase())).timestampValue()).toLocalDateTime());
            assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), (((oracle.sql.TIMESTAMP) arrayList.get(0).get("end_date".toUpperCase())).timestampValue()).toLocalDateTime());
            assertEquals(true, ((BigDecimal)arrayList.get(0).get("is_relevant".toUpperCase())).intValue()==1);
            String id = arrayList.get(0).get("id".toUpperCase()).toString();
            observationDAO.changeRelevance(id);
            arrayList = observationDAO.getRelevantObservations(fiscalCode);
            assertEquals(0, arrayList.size());
            //non ci dovrebbero essere altre osservazioni rilevanti a carico di Maria Rossi e quindi resultSet.next() dovrebbe restituire false
            deleteObservation(id);
        }catch (Exception e){
            fail(e.getMessage());
        }finally {
            AccessGatewayTest.deleteUser(fiscalCode);
        }
    }

    @Test
    void AllFunctionsTest_insertWithoutEndDate() {
        ObservationGateway observationDAO = new ObservationGateway();
        AccessGateway accessGateway = new AccessGateway();
        ArrayList<String> subjects = new ArrayList<>();
        String fiscalCode = "RSSMRA80A41H501Y";
        subjects.add(fiscalCode);
        Type type = new Symptoms();
        LocalDateTime startDate = LocalDateTime.of(1980, 1, 1,0, 0);
        try {
            accessGateway.insertNewUser(fiscalCode, "xx", "yy", "zz", "ww");
            observationDAO.insertObservation(subjects, type, startDate, null);
            ArrayList<HashMap<String, Object>> arrayList = observationDAO.getRelevantObservations(fiscalCode);
            assertNotEquals(0, arrayList.size());
            assertEquals(fiscalCode, arrayList.get(0).get("fiscalCode".toUpperCase()));
            assertEquals(type.getName(), arrayList.get(0).get("type".toUpperCase()));
            assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), (((oracle.sql.TIMESTAMP) arrayList.get(0).get("start_date".toUpperCase())).timestampValue()).toLocalDateTime());
            assertEquals(true, ((BigDecimal) arrayList.get(0).get("is_relevant".toUpperCase())).intValue() == 1);
            String id = arrayList.get(0).get("id".toUpperCase()).toString();
            observationDAO.changeRelevance(id);
            arrayList = observationDAO.getRelevantObservations(fiscalCode);
            assertEquals(0, arrayList.size());
            //non ci dovrebbero essere altre osservazioni rilevanti a carico di Maria Rossi e quindi resultSet.next() dovrebbe restituire false
            deleteObservation(id);
        }catch (Exception e){
            fail(e.getMessage());
        }finally {
            AccessGatewayTest.deleteUser(fiscalCode);
        }
    }
    public static int findObservation(String id){
        String url;
        String user;
        String password;
        Properties prop = new Properties();
        try{
            FileInputStream fis = new FileInputStream("src/main/res/database_login.config");
            prop.load(fis);
        }catch(IOException ex){
            System.out.println("Impossibile trovare le credenziali per l'accesso al database");
        }
        url = prop.getProperty("db.url");
        System.out.println(url);
        user = prop.getProperty("db.user");
        password = prop.getProperty("db.password");
        Connection connection = null;
        Statement statement = null;
        int count = -1;
        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            statement.execute("use `tracing-app`");
            ResultSet rs = statement.executeQuery("SELECT count(*) AS clusterCount FROM observations WHERE id = '"+id+"'");
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
        String url;
        String user;
        String password;
        Properties prop = new Properties();
        try{
            FileInputStream fis = new FileInputStream("src/main/res/database_login.config");
            prop.load(fis);
        }catch(IOException ex){
            System.out.println("Impossibile trovare le credenziali per l'accesso al database");
        }
        url = prop.getProperty("db.url");
        //System.out.println(url);
        user = prop.getProperty("db.user");
        password = prop.getProperty("db.password");
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            statement.execute("DELETE FROM observations WHERE id = '" + id + "'");
            statement.execute("DELETE FROM events WHERE ID = '" + id + "'");
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

    public static ArrayList<String> getAllUsers(){
        String url;
        String user;
        String password;
        Properties prop = new Properties();
        try{
            FileInputStream fis = new FileInputStream("src/main/res/database_login.config");
            prop.load(fis);
        }catch(IOException ex){
            System.out.println("Impossibile trovare le credenziali per l'accesso al database");
        }
        url = prop.getProperty("db.url");
        System.out.println(url);
        user = prop.getProperty("db.user");
        password = prop.getProperty("db.password");
        Connection connection = null;
        Statement statement = null;
        ArrayList<String> arrayList = new ArrayList<>();
        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT DISTINCT users.fiscalCode FROM users RIGHT JOIN observations o on users.fiscalCode = o.fiscalCode WHERE users.fiscalCode IS NOT NULL");
            try {
                while(rs.next()){
                    arrayList.add((String) rs.getObject(1));
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Si è verificato un problema durante l'esecuzione della query");
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
        return arrayList;
    }
}