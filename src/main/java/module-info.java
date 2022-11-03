module africa.jopen.javafxwebrtcdesktopapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;

    opens africa.jopen.javafxwebrtcdesktopapp to javafx.fxml;
    exports africa.jopen.javafxwebrtcdesktopapp;
}