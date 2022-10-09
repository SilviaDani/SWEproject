package com.sweproject.main;

import com.sweproject.dao.AccessDAO;
import com.sweproject.dao.AccessDAOTest;
import com.sweproject.dao.ObservationDAO;
import com.sweproject.dao.ObservationDAOTest;
import com.sweproject.model.Environment;
import com.sweproject.model.Type;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UITest extends Application {
    private ResultSet resultSet;
    private static ArrayList<String> IDs;

    /**
     * Will be called with {@code @Before} semantics, i. e. before each test method.
     *
     * @param stage - Will be injected by the test runner.
     */
    @Start
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/sweproject/FXML/index.fxml"));
        stage.setTitle("Covid Tracing App");
        stage.getIcons().add(new Image(new File("src/main/res/icon.png").toURI().toString()));
        stage.setScene(new Scene(root));
        stage.show();
    }

    /**
     * @param robot - Will be injected by the test runner.
     */

    void signUp(FxRobot robot, String FC, String firstName, String lastName, String psw){
        robot.clickOn("#sign_up");

        //fill the fields
        robot.clickOn("#fiscal_code");
        robot.write(FC);
        robot.clickOn("#name");
        robot.write(firstName);
        robot.clickOn("#surname");
        robot.write(lastName);
        robot.clickOn("#password");
        robot.write(psw);
        robot.clickOn("#confirm_password");
        robot.write(psw);
        robot.clickOn("#sign_up");
    }

    @Test
    @Order(1)
    void UC_sign_up(FxRobot robot){
        //click on "sign up" button
        FxAssert.verifyThat("#sign_up", LabeledMatchers.hasText("Sign up"));
        String FC = "RSSMRA80A41H501Y";
        String    firstName = "Maria";
        String    lastName = "Rossi";
        String    psw = "password";

        signUp(robot, FC,firstName, lastName, psw);

        AccessDAO accessDAO = new AccessDAO();
        ArrayList<HashMap<String, Object>> arrayList = accessDAO.selectUser(FC);
        assertNotEquals(0, arrayList.size());
        assertEquals(FC, arrayList.get(0).get("fiscalCode"));
        assertEquals(firstName, arrayList.get(0).get("firstName"));
        assertEquals(lastName, arrayList.get(0).get("surname"));
        assertEquals(BCrypt.hashpw(psw, arrayList.get(0).get("salt").toString()), arrayList.get(0).get("psw"));
    }

    @Test
    @Order(2)
    void UC_add_observation(FxRobot robot){
       logIn(robot, "RSSMRA80A41H501Y", "password", "Maria");
       FxAssert.verifyThat("#add_observation", LabeledMatchers.hasText("Add observation"));
       FxAssert.verifyThat("#welcome_text", LabeledMatchers.hasText("Welcome Maria"));
       for(int i = 0; i<4; i++) {
           robot.clickOn("#add_observation");
           robot.clickOn("#observation_type_menu");
           robot.type(KeyCode.DOWN, i+1);
           robot.type(KeyCode.ENTER);
           switch (i){
               case 0:
                   robot.clickOn("#risk_combobox");
                   robot.type(KeyCode.DOWN, 1);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#next");
                   robot.clickOn("#start_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#start_date_hour");
                   robot.write("0");
                   robot.clickOn("#start_date_minute");
                   robot.write("0");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("31/12/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("23");
                   robot.clickOn("#end_date_minute");
                   robot.write("59");
                   robot.clickOn("#next");
                   robot.clickOn("#text1");
                   robot.write("FC1");
                   robot.clickOn("#add1");
                   robot.clickOn("#remove1");
                   for(int j = 1; j < 5; j++){
                       robot.clickOn("#text"+j);
                       robot.write("FC"+j);
                       robot.clickOn("#add"+j);
                   }
                   robot.clickOn("#remove3");
                   robot.clickOn("#next");
                   break;
               case 1:
                   //low risk without mask
                   robot.clickOn("#risk_combobox");
                   robot.type(KeyCode.DOWN, 1);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#next");
                   robot.clickOn("#start_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#start_date_hour");
                   robot.write("0");
                   robot.clickOn("#start_date_minute");
                   robot.write("0");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("1");
                   robot.clickOn("#end_date_minute");
                   robot.write("30");
                   robot.clickOn("#next");

                   //low risk with mask
                   robot.clickOn("#add_observation");
                   robot.clickOn("#observation_type_menu");
                   robot.type(KeyCode.DOWN, 2);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#risk_combobox");
                   robot.type(KeyCode.DOWN, 1);
                   robot.clickOn("#mask_used");
                   robot.clickOn("#next");
                   robot.clickOn("#start_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#start_date_hour");
                   robot.write("0");
                   robot.clickOn("#start_date_minute");
                   robot.write("0");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("1");
                   robot.clickOn("#end_date_minute");
                   robot.write("30");
                   robot.clickOn("#next");

                   //medium risk without mask
                   robot.clickOn("#add_observation");
                   robot.clickOn("#observation_type_menu");
                   robot.type(KeyCode.DOWN, 2);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#risk_combobox");
                   robot.type(KeyCode.DOWN, 2);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#next");
                   robot.clickOn("#start_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#start_date_hour");
                   robot.write("0");
                   robot.clickOn("#start_date_minute");
                   robot.write("0");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("1");
                   robot.clickOn("#end_date_minute");
                   robot.write("30");
                   robot.clickOn("#next");

                   //medium risk with mask
                   robot.clickOn("#add_observation");
                   robot.clickOn("#observation_type_menu");
                   robot.type(KeyCode.DOWN, 2);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#risk_combobox");
                   robot.type(KeyCode.DOWN, 2);
                   robot.clickOn("#mask_used");
                   robot.clickOn("#next");
                   robot.clickOn("#start_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#start_date_hour");
                   robot.write("0");
                   robot.clickOn("#start_date_minute");
                   robot.write("0");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("1");
                   robot.clickOn("#end_date_minute");
                   robot.write("30");
                   robot.clickOn("#next");

                   //high risk without mask
                   robot.clickOn("#add_observation");
                   robot.clickOn("#observation_type_menu");
                   robot.type(KeyCode.DOWN, 2);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#risk_combobox");
                   robot.type(KeyCode.DOWN, 3);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#next");
                   robot.clickOn("#start_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#start_date_hour");
                   robot.write("0");
                   robot.clickOn("#start_date_minute");
                   robot.write("0");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("1");
                   robot.clickOn("#end_date_minute");
                   robot.write("30");
                   robot.clickOn("#next");

                   //high risk with mask
                   robot.clickOn("#add_observation");
                   robot.clickOn("#observation_type_menu");
                   robot.type(KeyCode.DOWN, 2);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#risk_combobox");
                   robot.type(KeyCode.DOWN, 3);
                   robot.clickOn("#mask_used");
                   robot.clickOn("#next");
                   robot.clickOn("#start_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#start_date_hour");
                   robot.write("0");
                   robot.clickOn("#start_date_minute");
                   robot.write("0");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("1");
                   robot.clickOn("#end_date_minute");
                   robot.write("30");
                   robot.clickOn("#next");
                   break;

               case 2:
                   //case not symptomatic
                   robot.clickOn("#next");
                   robot.clickOn("#start_datePicker_menu");
                   robot.write("01/01/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#start_date_hour");
                   robot.write("0");
                   robot.clickOn("#start_date_minute");
                   robot.write("0");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("31/12/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("23");
                   robot.clickOn("#end_date_minute");
                   robot.write("59");
                   robot.clickOn("#next");

                   //case symptomatic
                   robot.clickOn("#add_observation");
                   robot.clickOn("#observation_type_menu");
                   robot.type(KeyCode.DOWN, 3);
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#still_symptomatic");
                   robot.clickOn("#next");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("31/12/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("23");
                   robot.clickOn("#end_date_minute");
                   robot.write("59");
                   robot.clickOn("#next");
                   break;
               case 3:
                   robot.clickOn("#test_type_menu");
                   robot.type(KeyCode.DOWN);robot.type(KeyCode.ENTER);
                   robot.clickOn("#next");
                   robot.clickOn("#end_datePicker_menu");
                   robot.write("31/12/2021");
                   robot.type(KeyCode.ENTER);
                   robot.clickOn("#end_date_hour");
                   robot.write("23");
                   robot.clickOn("#end_date_minute");
                   robot.write("59");
                   robot.clickOn("#next");
                   break;
               default:
                   fail("Non dovrebbe passare da qui");
           }
       }

        ObservationDAO observationDAO = new ObservationDAO();
        ArrayList<HashMap<String, Object>> arrayList = observationDAO.getRelevantObservations("RSSMRA80A41H501Y");
        assertEquals(10, arrayList.size());//XXX cambia in base alle osservazioni che vengono create prima
        IDs = new ArrayList<>();
        LocalDateTime startDate = LocalDateTime.of(2021,1,1,0,0);
        LocalDateTime endDate = LocalDateTime.of(2021,12,31,23,59);
        LocalDateTime end_date = LocalDateTime.of(2021, 1, 1, 1, 30);
        for(int i = 0; i<10; i++) {
            IDs.add(arrayList.get(i).get("ID").toString());
        }
        for(int i = 0; i<5; i++){
            switch (i){
                case 0:
                    assertEquals(4, ObservationDAOTest.findObservation(arrayList.get(i).get("ID").toString()));
                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("start_date"));
                    assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("end_date"));
                    assertEquals("Contact", arrayList.get(i).get("type"));
                    break;
                case 1:
                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("start_date"));
                    assertEquals(end_date.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("end_date"));
                    assertEquals("Environment", arrayList.get(i).get("type"));
                    float risk1 = (float) arrayList.get(i).get("risk_level");

                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+1).get("start_date"));
                    assertEquals(end_date.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+1).get("end_date"));
                    assertEquals("Environment", arrayList.get(i+1).get("type"));
                    float risk2 = (float) arrayList.get(i+1).get("risk_level");
                    assertTrue(risk1 > risk2);

                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+2).get("start_date"));
                    assertEquals(end_date.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+2).get("end_date"));
                    assertEquals("Environment", arrayList.get(i+2).get("type"));
                    float risk3 = (float) arrayList.get(i+2).get("risk_level");
                    assertTrue(risk3 > risk1);

                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+3).get("start_date"));
                    assertEquals(end_date.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+3).get("end_date"));
                    assertEquals("Environment", arrayList.get(i+3).get("type"));
                    float risk4 = (float) arrayList.get(i+3).get("risk_level");
                    assertTrue(risk3 > risk4 && risk4 > risk2);

                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+4).get("start_date"));
                    assertEquals(end_date.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+4).get("end_date"));
                    assertEquals("Environment", arrayList.get(i+4).get("type"));
                    float risk5 = (float) arrayList.get(i+4).get("risk_level");
                    assertTrue(risk5 > risk3);

                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+5).get("start_date"));
                    assertEquals(end_date.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i+5).get("end_date"));
                    assertEquals("Environment", arrayList.get(i+5).get("type"));
                    float risk6 = (float) arrayList.get(i+5).get("risk_level");
                    assertTrue(risk5 > risk6 && risk6 > risk4);

                    break;

                case 2:
                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(7).get("start_date"));
                    assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(7).get("end_date"));
                    assertEquals("Symptoms", arrayList.get(7).get("type"));
                    break;
                case 3:
                    assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(8).get("start_date"));
                    assertEquals("Symptoms", arrayList.get(8).get("type"));
                    break;
                case 4:
                    assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(9).get("start_date"));
                    assertEquals("Covid_test-ANTIGEN-false", arrayList.get(9).get("type"));
                    break;
                default:
                    fail("Non dovrebbe passare da qui");
            }
        }
    }

    @Test
    @Order(3)
    void UC_changeObservationRelevance(FxRobot robot){
        ArrayList<String> patients = new ArrayList<>();
        String patientCode = "RSSMRA80A41H501Y";
        patients.add(patientCode);
        AccessDAOTest.insertDoctor("DoctorFiscalCode", patients);
        ObservationDAO observationDAO = new ObservationDAO();
        ArrayList<HashMap<String, Object>> arrayList = observationDAO.getRelevantObservations(patientCode);
        assertEquals(10, arrayList.size());
        signUp(robot, "DoctorFiscalCode", "DoctorName", "DoctorSurname", "password");
        logIn(robot, "DoctorFiscalCode", "password", "DoctorName");
        robot.clickOn("#reserved_area");
        robot.clickOn("#listView");
        robot.clickOn("#observations");
        arrayList = observationDAO.getRelevantObservations(patientCode);
        assertEquals(9, arrayList.size());
    }

    private void logIn(FxRobot robot, String FC, String psw, String name){
        FxAssert.verifyThat("#log_in", LabeledMatchers.hasText("Log in"));
        robot.clickOn("#log_in");

        robot.clickOn("#fiscal_code");
        robot.write(FC);
        robot.clickOn("#password");
        robot.write(psw);
        robot.clickOn("#log_in");
    }



    @AfterAll
    static void close(){
        for(String id : IDs)
            ObservationDAOTest.deleteObservation(id);
        AccessDAOTest.deleteUser("RSSMRA80A41H501Y");
        AccessDAOTest.deleteUser("DoctorFiscalCode");
        AccessDAOTest.deleteDoctor("DoctorFiscalCode");
        Platform.exit();
    }

    /**
     * @param robot - Will be injected by the test runner.
     */

}
