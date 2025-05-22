module com.example.group {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;


    opens com.example.group to javafx.fxml;
    exports com.example.group;
}