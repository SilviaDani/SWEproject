package com.sweproject.main;

import com.sweproject.dao.AccessDAO;
import com.sweproject.dao.AccessDAOTest;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(ApplicationExtension.class)
public class UITest extends Application {
    private ResultSet resultSet;

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

        String fiscalCode = "RSSMRA80A41H501Y";
        String name = "Maria";
        String surname = "Rossi";
        String password = "password";

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
        AccessDAOTest.deleteUser(fiscalCode);
    }
    @AfterAll
    static void close(){
        Platform.exit();
    }

    /**
     * @param robot - Will be injected by the test runner.
     */

}
