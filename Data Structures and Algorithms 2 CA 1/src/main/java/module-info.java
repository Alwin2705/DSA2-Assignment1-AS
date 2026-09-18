module org.example.datastructuresandalgorithms2ca1 {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires javafx.swing;

    // JMH
    requires jmh.core;


    opens org.example to javafx.fxml;
    opens controller to javafx.fxml;
    opens utils to javafx.fxml;

    // JMH needs reflective access to the benchmark class
    opens benchmark to jmh.core;

    exports org.example;
    exports controller;
    exports utils;
    exports benchmark;
}