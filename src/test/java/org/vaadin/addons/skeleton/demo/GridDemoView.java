package org.vaadin.addons.skeleton.demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.skeleton.Skeleton;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Route("grid")
public class GridDemoView extends VerticalLayout {

    public record Person(String name, String email, String city, long namesakes, long cityRank) {
    }

    // Two window functions over all one million rows make this take a second or two.
    private static final String QUERY = """
            SELECT name, email, city,
                   COUNT(*) OVER (PARTITION BY name, city) AS namesakes,
                   RANK() OVER (PARTITION BY city ORDER BY email) AS city_rank
            FROM person
            ORDER BY city, name, email
            """;

    private final Grid<Person> grid = new Grid<>(Person.class);
    private final Span status = new Span();
    private final VerticalLayout panel = new VerticalLayout(new H3("People"), grid);

    public GridDemoView() {
        grid.setColumns("name", "email", "city", "namesakes", "cityRank");
        grid.setHeight("400px");
        panel.setWidth("800px");

        add(panel, new Button("Reload", e -> load()), status);
        load();
    }

    private void load() {
        long start = System.currentTimeMillis();
        Skeleton.load(panel,
                GridDemoView::queryPeople,
                people -> {
                    grid.setItems(people);
                    status.setText("Loaded %,d rows in %,d ms".formatted(
                            people.size(), System.currentTimeMillis() - start));
                });
    }

    private static List<Person> queryPeople() {
        try (Connection c = DemoDatabase.connect();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(QUERY)) {
            List<Person> people = new ArrayList<>();
            while (rs.next()) {
                people.add(new Person(rs.getString("name"), rs.getString("email"),
                        rs.getString("city"), rs.getLong("namesakes"), rs.getLong("city_rank")));
            }
            return people;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
