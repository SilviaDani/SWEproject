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
            assertEquals(fiscalCode, arrayList.get(0).get("fiscalCode"));
            assertEquals(name, arrayList.get(0).get("firstName"));
            assertEquals(surname, arrayList.get(0).get("surname"));
            assertEquals(password, arrayList.get(0).get("psw"));
            assertEquals(salt, arrayList.get(0).get("salt"));
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
            deleteDoctor(fiscalCode);
            insertDoctor(fiscalCode, patients);
            ArrayList<HashMap<String, Object>> arrayList = accessGateway.selectDoctor(fiscalCode);
            for(int i=0; i<2; i++) {
                assertNotEquals(0, arrayList.size());
                assertEquals(fiscalCode, arrayList.get(i).get("doctorFiscalCode"));
                assertEquals(patients.get(patients.size() - 1 -i), arrayList.get(i).get("patientFiscalCode"));
            }
            deleteDoctor(fiscalCode);
            AccessGatewayTest.deleteUser(fiscalCode);
        }catch (Exception e){
            e.printStackTrace();
            fail(e.getCause());
        }


    }

    public static void deleteUser(String fiscalCode) {
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
            statement.execute("DELETE FROM `users` WHERE `fiscalCode` = '" + fiscalCode + "'");
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

    public static void insertDoctor(String fiscalCode, ArrayList<String> patients){
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
            Statement finalStatement = statement;
            patients.forEach((patient) -> {
                try {
                    finalStatement.execute("INSERT INTO `doctors` (`doctorFiscalCode`, `patientFiscalCode`) VALUES ('" + fiscalCode + "', '" + patient + "')");
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
            statement.execute("DELETE FROM `doctors` WHERE `doctorFiscalCode` = '" + fiscalCode + "'");
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