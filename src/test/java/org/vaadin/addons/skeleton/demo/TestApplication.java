package org.vaadin.addons.skeleton.demo;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.lumo.Lumo;

/** App shell for the demo server: run {@code mvn} (defaults to {@code jetty:run}), then open http://localhost:8080. */
@Push
@StyleSheet(Lumo.STYLESHEET)
public class TestApplication implements AppShellConfigurator {
}
