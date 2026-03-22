module ecocam.project_chat_console {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires org.kordamp.bootstrapfx.core;
    requires password4j;
    requires org.slf4j;

    opens ecocam.project_chat_console to javafx.fxml;
    opens ecocam.project_chat_console.model to javafx.base;
    opens ecocam.project_chat_console.client to javafx.base;
    opens ecocam.project_chat_console.server to javafx.base;
    opens ecocam.project_chat_console.service to javafx.base;

    exports ecocam.project_chat_console;
    exports ecocam.project_chat_console.model;
    exports ecocam.project_chat_console.client;
    exports ecocam.project_chat_console.server;
    exports ecocam.project_chat_console.service;
    exports ecocam.project_chat_console.controller;
    opens ecocam.project_chat_console.controller to javafx.fxml;
}