package com.sweproject.main;

import com.sweproject.dao.AccessDAO;
import com.sweproject.dao.AccessDAOTest;
import com.sweproject.dao.ObservationDAO;
import com.sweproject.dao.ObservationDAOTest;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
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
public class UITest extends Application {
    private ResultSet resultSet;
    private static String fiscalCode;
    private static String password;
    private static String name;
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
    @Test
    void UC_sign_up(FxRobot robot){
        //click on "sign up" button
        FxAssert.verifyThat("#sign_up", LabeledMatchers.hasText("Sign up"));
        robot.clickOn("#sign_up");

        fiscalCode = "RSSMRA80A41H501Y";
        name = "Maria";
        String surname = "Rossi";
        password = "password";

        //fill the fields
        robot.clickOn("#fiscal_code");
        robot.write(fiscalCode);
        robot.clickOn("#name");
        robot.write(name);
        robot.clickOn("#surname");
        robot.write(surname);
        robot.clickOn("#password");
        robot.write(password);
        robot.clickOn("#confirm_password");
        robot.write(password);
        robot.clickOn("#sign_up");

        AccessDAO accessDAO = new AccessDAO();
        ArrayList<HashMap<String, Object>> arrayList = accessDAO.selectUser(fiscalCode);
        assertNotEquals(0, arrayList.size());
        assertEquals(fiscalCode, arrayList.get(0).get("fiscalCode"));
        assertEquals(name, arrayList.get(0).get("firstName"));
        assertEquals(surname, arrayList.get(0).get("surname"));
        assertEquals(BCrypt.hashpw(password, arrayList.get(0).get("salt").toString()), arrayList.get(0).get("psw"));
    }

    @Test
    void UC_add_observation(FxRobot robot){
       logIn(robot);
       FxAssert.verifyThat("#add_observation", LabeledMatchers.hasText("Add observation"));
       FxAssert.verifyThat("#welcome_text", LabeledMatchers.hasText("Welcome "+name));
       for(int i = 0; i<3; i++) {
           robot.clickOn("#add_observation");
           robot.clickOn("#observation_type_menu");
           robot.type(KeyCode.DOWN, i+1);
           robot.type(KeyCode.ENTER);
           switch (i){
               case 0:
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
                   robot.type(KeyCode.DOWN, i+1);
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
               case 2:
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
        ArrayList<HashMap<String, Object>> arrayList = observationDAO.getRelevantObservations(fiscalCode);
        assertEquals(4, arrayList.size());//FIXME
        IDs = new ArrayList<>();
        LocalDateTime startDate = LocalDateTime.of(2021,1,1,0,0);
        LocalDateTime endDate = LocalDateTime.of(2021,12,31,23,59);
        for(int i = 0; i<arrayList.size(); i++){
            IDs.add(arrayList.get(i).get("ID").toString());
            switch (i){//FIXME
                case 0:
                    assertEquals(4, ObservationDAOTest.findObservation(arrayList.get(i).get("ID").toString()));
                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("start_date"));
                    assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("end_date"));
                    assertEquals("Contact", arrayList.get(i).get("type"));
                    break;
                case 1:
                    assertEquals(startDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("start_date"));
                    assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("end_date"));
                    assertEquals("Symptoms", arrayList.get(i).get("type"));
                    break;
                case 2:
                    assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("start_date"));
                    assertEquals("Symptoms", arrayList.get(i).get("type"));
                    break;
                case 3:
                    assertEquals(endDate.truncatedTo(ChronoUnit.SECONDS), arrayList.get(i).get("start_date"));
                    assertEquals("Covid_test-ANTIGEN-false", arrayList.get(i).get("type"));
                    break;
                default:
                    fail("Non dovrebbe passare da qui");
            }
        }
    }

    private void logIn(FxRobot robot){
        FxAssert.verifyThat("#log_in", LabeledMatchers.hasText("Log in"));
        robot.clickOn("#log_in");

        robot.clickOn("#fiscal_code");
        robot.write(fiscalCode);
        robot.clickOn("#password");
        robot.write(password);
        robot.clickOn("#log_in");
    }



    @AfterAll
    static void close(){
        for(String id : IDs)
            ObservationDAOTest.deleteObservation(id);
        AccessDAOTest.deleteUser(fiscalCode);
        Platform.exit();
    }

    /**
     * @param robot - Will be injected by the test runner.
     */

}
