    module ni.edu.uam.assistia {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires static lombok;


    opens ni.edu.uam.assistia to javafx.fxml;
    opens ni.edu.uam.assistia.models to org.hibernate.orm.core;
}