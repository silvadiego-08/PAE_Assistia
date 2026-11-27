module ni.edu.uam.assistia {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.hibernate.orm.core;
    requires org.postgresql.jdbc;
    requires jakarta.persistence;
    requires java.sql;
    requires java.naming;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires static lombok;
    requires jasperreports;
    requires javafx.swing;
    requires java.desktop;

    opens ni.edu.uam.assistia to javafx.fxml;
    opens ni.edu.uam.assistia.model to org.hibernate.orm.core;
    opens ni.edu.uam.assistia.dao to org.hibernate.orm.core;
    // JasperReports puede cargar BeanUtils desde el classpath o el module-path.
    opens ni.edu.uam.assistia.reporte;
    exports ni.edu.uam.assistia;
    exports ni.edu.uam.assistia.controller;
    exports ni.edu.uam.assistia.persistence;
    opens ni.edu.uam.assistia.controller to javafx.fxml;
}
