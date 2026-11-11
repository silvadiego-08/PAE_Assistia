module ni.edu.uam.assistia {
    requires javafx.controls;
    requires javafx.fxml;


    opens ni.edu.uam.assistia to javafx.fxml;
    exports ni.edu.uam.assistia;
}