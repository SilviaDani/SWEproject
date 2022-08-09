package com.sweproject.controller;

import com.sweproject.dao.AccessDAO;
import com.sweproject.model.Notifier;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UIController {
    protected static Notifier user;
    protected AccessDAO accessDAO;
    protected Stage stage;
    protected Scene scene;
    protected Parent root;
}
