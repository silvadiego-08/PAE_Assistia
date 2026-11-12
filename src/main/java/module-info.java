module ni.edu.uam.assistia {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;

    opens ni.edu.uam.assistia to javafx.fxml;
    opens ni.edu.uam.assistia.controller to javafx.fxml;
    opens ni.edu.uam.assistia.model;

    exports ni.edu.uam.assistia;
}