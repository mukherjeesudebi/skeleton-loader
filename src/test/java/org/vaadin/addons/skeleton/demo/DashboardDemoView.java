package org.vaadin.addons.skeleton.demo;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.skeleton.Skeleton;

import java.util.List;

/**
 * Several independent loads running at once, each with its own skeleton.
 * Also shows {@code exclude}, {@code markAsLeaf} and error handling.
 */
@Route("dashboard")
public class DashboardDemoView extends VerticalLayout {

    record Stat(String value, double progress, String trend, boolean up) {
    }

    private final List<StatTile> tiles = List.of(
            new StatTile("Revenue", 800, new Stat("€48,210", 0.72, "+12%", true)),
            new StatTile("New customers", 1400, new Stat("318", 0.45, "+4%", true)),
            new StatTile("Churn", 2000, new Stat("2.1%", 0.21, "-0.3%", false)),
            new StatTile("Open tickets", 2600, null)); // this one fails

    private final VirtualList<String> activity = new VirtualList<>();
    private final VerticalLayout activityPanel;

    public DashboardDemoView() {
        FlexLayout row = new FlexLayout();
        row.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        row.getStyle().set("gap", "var(--lumo-space-m)");
        tiles.forEach(row::add);

        activity.setRenderer(new ComponentRenderer<Div, String>(text -> new Div(text)));
        activity.setHeight("240px");
        // A virtual list manages its own children; draw it as one block.
        Skeleton.markAsLeaf(activity);

        Badge live = new Badge("Live");
        live.addThemeVariants(BadgeVariant.SUCCESS);
        // Stays visible while the rest of the panel is loading.
        Skeleton.exclude(live);

        activityPanel = new VerticalLayout(new HorizontalLayout(new H3("Recent activity"), live), activity);
        activityPanel.setPadding(false);
        activityPanel.setMaxWidth("720px");

        add(row, activityPanel, new Button("Reload all", e -> load()));
        load();
    }

    private void load() {
        tiles.forEach(StatTile::load);
        Skeleton.load(activityPanel,
                () -> DemoData.slow(1800, List.of(
                        "Aino Virtanen upgraded to Pro",
                        "Invoice #10421 was paid",
                        "Lukas Becker opened a support ticket",
                        "Weekly report was generated",
                        "María García joined the Enterprise trial",
                        "Invoice #10418 is overdue",
                        "Deployment 2026.11.3 finished")),
                activity::setItems);
    }

    /** A dashboard card that loads its own number. */
    private static final class StatTile extends Card {

        private final String label;
        private final long delayMs;
        private final Stat result;
        private final Span value = new Span("—");
        private final ProgressBar progress = new ProgressBar();
        private final Badge trend = new Badge("0%");

        StatTile(String label, long delayMs, Stat result) {
            this.label = label;
            this.delayMs = delayMs;
            this.result = result;
            setTitle(label);
            setWidth("220px");
            value.getStyle().set("font-size", "var(--lumo-font-size-xxl)").set("font-weight", "600");
            add(value, progress);
            setHeaderSuffix(trend);
        }

        void load() {
            Skeleton.load(this,
                    () -> {
                        DemoData.slow(delayMs, null);
                        if (result == null) {
                            throw new IllegalStateException("Ticket service is not responding");
                        }
                        return result;
                    },
                    stat -> {
                        value.setText(stat.value());
                        progress.setValue(stat.progress());
                        trend.setText(stat.trend());
                        trend.removeThemeVariants(BadgeVariant.SUCCESS, BadgeVariant.ERROR);
                        trend.addThemeVariants(stat.up() ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);
                    },
                    error -> {
                        value.setText("n/a");
                        trend.setText("error");
                        trend.addThemeVariants(BadgeVariant.ERROR);
                        Notification.show(label + ": " + error.getMessage(), 4000,
                                Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
        }
    }
}
