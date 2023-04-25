package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private Controller controller;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        controller = fxmlLoader.getController();
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.setTitle("Chatting Client");
        stage.show();
    }

    @Override
    public void stop() {
        controller.Quit();
        System.out.printf("quit.");
    }

}
