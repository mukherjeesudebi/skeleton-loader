package org.vaadin.addons.skeleton.demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.vaadin.addons.skeleton.Skeleton;

import java.util.List;

@Route("grid")
public class GridDemoView extends VerticalLayout {

    public record Person(String name, String email, String city, long namesakes, long cityRank) {
    }

    // Two window functions over all one million rows make this take a second or two.
    private static final String QUERY = """
            SELECT name, email, city,
                   COUNT(*) OVER (PARTITION BY name, city) AS namesakes,
                   RANK() OVER (PARTITION BY city ORDER BY email) AS city_rank            FROM person
            ORDER BY city, name, email
            """;
    
    private final JdbcClient jdbc;
    private final Grid<Person> grid = new Grid<>(Person.class);
    private final Span status = new Span();
    private final VerticalLayout panel = new VerticalLayout(new H3("People"), grid);

    public GridDemoView(JdbcClient jdbc) {
        this.jdbc = jdbc;
        grid.setColumns("name", "email", "city", "namesakes", "cityRank");
        grid.setHeight("400px");
        panel.setWidth("800px");

        add(panel, new Button("Reload", e -> load()), status);
        load();
    }

    private void load() {
        long start = System.currentTimeMillis();
        Skeleton.load(panel,
                () -> jdbc.sql(QUERY).query(Person.class).list(),
                people -> {
                    grid.setItems(people);
                    status.setText("Loaded %,d rows in %,d ms".formatted(
                            people.size(), System.currentTimeMillis() - start));
                });
    }
}
