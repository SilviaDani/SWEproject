package com.sweproject.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class AccessGatewayTest {
    private AccessGateway accessGateway;

    @Test
    void insertNewUser_selectUser() {
        accessGateway = new AccessGateway();
        String fiscalCode = "RSSMRA80A41H501Y";
        String name = "Maria";
        String surname = "Rossi";
        String password = "password";
        String salt = BCrypt.gensalt();
        AccessGatewayTest.deleteUser(fiscalCode);
        try {
            accessGateway.insertNewUser(fiscalCode, name, surname, password, salt);
            ArrayList<HashMap<String, Object>> arrayList = accessGateway.selectUser(fiscalCode);
            assertNotEquals(0,arrayList.size());
            assertEquals(fiscalCode, arrayList.get(0).get("FISCALCODE"));
            assertEquals(name, arrayList.get(0).get("FIRSTNAME"));
            assertEquals(surname, arrayList.get(0).get("SURNAME"));
            assertEquals(password, arrayList.get(0).get("PASSWORD"));
            assertEquals(salt, arrayList.get(0).get("SALT"));
            AccessGatewayTest.deleteUser(fiscalCode);
        }catch (Exception e){
            fail(e.getCause());
        }


    }

    @Test
    void selectDoctor() {
        accessGateway = new AccessGateway();
        String fiscalCode = "RSSMRA80A41H501Y";
        String name = "Maria";
        String surname = "Rossi";
        String password = "password";
        String salt = BCrypt.gensalt();
        AccessGatewayTest.deleteUser(fiscalCode);
        try {
            accessGateway.insertNewUser(fiscalCode, name, surname, password, salt);
            ArrayList<String> patients = new ArrayList<>();
            patients.add("RSSLRD00A01H501E");
            patients.add("FRRFNC66A01H501D");
            accessGateway.insertNewUser(patients.get(0), "xx", "yy", "zz", "ww");
            accessGateway.insertNewUser(patients.get(1), "xx", "yy", "zz", "ww");
            deleteDoctor(fiscalCode);
            insertDoctor(fiscalCode, patients);
            ArrayList<HashMap<String, Object>> arrayList = accessGateway.selectDoctor(fiscalCode);
            for(int i=0; i<2; i++) {
                assertNotEquals(0, arrayList.size());
                assertEquals(fiscalCode, arrayList.get(i).get("doctorFiscalCode".toUpperCase()));
                assertEquals(patients.get(i), arrayList.get(i).get("patientFiscalCode".toUpperCase()));
            }
            deleteDoctor(fiscalCode);
            AccessGatewayTest.deleteUser(fiscalCode);
        }catch (Exception e){
            e.printStackTrace();
            fail(e.getCause());
        }


    }

    public static void deleteUser(String fiscalCode) {
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
        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            statement.execute("DELETE FROM USERS WHERE FISCALCODE = '" + fiscalCode.toUpperCase() + "'");
        } catch (Exception e) {
            fail("Si è verificato un problema durante la connessione al database.");
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
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }

        }
    }

    public static void insertDoctor(String fiscalCode_, ArrayList<String> patients){
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
        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            Statement finalStatement = statement;
            patients.forEach((patient) -> {
                try {
                    System.out.println(fiscalCode_.toUpperCase() + " " + patient.toUpperCase());
                    finalStatement.execute("INSERT INTO DOCTORS (DOCTORFISCALCODE, PATIENTFISCALCODE) VALUES ('" + fiscalCode_.toUpperCase() + "', '" + patient.toUpperCase() + "')");
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    fail(throwables.getMessage());
                }
            });

        } catch (Exception e) {
            fail("Si è verificato un problema durante la connessione al database.");
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
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }

        }
    }

    public static void deleteDoctor(String fiscalCode){
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
        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            statement.execute("DELETE FROM doctors WHERE doctorFiscalCode = '" + fiscalCode.toUpperCase() + "'");
        } catch (Exception e) {
            fail("Si è verificato un problema durante la connessione al database.");
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
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }

        }
    }
}