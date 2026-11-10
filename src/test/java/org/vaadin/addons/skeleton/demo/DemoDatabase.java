package org.vaadin.addons.skeleton.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** In-memory H2 database for the demo, created on first use from schema.sql and data.sql. */
final class DemoDatabase {

    private static final String URL = "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1";

    private DemoDatabase() {
    }

    /** Opens a new connection; the database is initialized the first time. */
    static Connection connect() throws SQLException {
        Initializer.ensureInitialized();
        return DriverManager.getConnection(URL);
    }

    private static final class Initializer {
        static {
            try (Connection c = DriverManager.getConnection(URL); Statement s = c.createStatement()) {
                s.execute(read("/schema.sql"));
                s.execute(read("/data.sql"));
            } catch (SQLException e) {
                throw new IllegalStateException("Could not initialize demo database", e);
            }
        }

        static void ensureInitialized() {
            // Class initialization above runs exactly once.
        }

        private static String read(String resource) {
            try (InputStream in = DemoDatabase.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("Missing resource " + resource);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
