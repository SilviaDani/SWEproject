package com.sweproject.controller;

import com.sweproject.gateway.AccessGateway;
import com.sweproject.model.Notifier;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UIController {
    protected static Notifier user;
    protected AccessGateway accessGateway;
    protected Stage stage;
    protected Scene scene;
    protected Parent root;
    protected static Object data;
}
