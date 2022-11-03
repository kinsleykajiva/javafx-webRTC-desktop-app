package africa.jopen.javafxwebrtcdesktopapp;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    @FXML
    private Label welcomeText;
    @FXML
    public VBox sideProfileVBox;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
      var  callManager = new CallManager(this,sideProfileVBox);

        callManager.initJanusWebrtcSession();
    }
}