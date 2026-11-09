package org.vaadin.addons.skeleton.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.lumo.Lumo;

/** Demo server for trying the add-on locally: {@code mvn spring-boot:test-run}, then open http://localhost:8080. */
@Push
@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
public class TestApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
