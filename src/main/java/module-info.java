module ni.edu.uam.assistia {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires static lombok;


    opens ni.edu.uam.assistia to javafx.fxml;
    exports ni.edu.uam.assistia;
}