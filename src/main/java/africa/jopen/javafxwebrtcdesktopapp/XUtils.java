package africa.jopen.javafxwebrtcdesktopapp;

import javafx.application.Platform;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

public class XUtils {

    public static String ROOMS_JANUS_PASSWORD =  "";

    /**
     * Run this Runnable in the JavaFX Application Thread. This method can be
     * called whether or not the current thread is the JavaFX Application
     * Thread.
     *
     * @param runnable The code to be executed in the JavaFX Application Thread.
     */
    public static void invoke(Runnable runnable) {
        if (isNull(runnable)) {
            return;
        }

        try {
            if (Platform.isFxApplicationThread()) {
                runnable.run();
            }
            else {
                Platform.runLater(runnable);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}
