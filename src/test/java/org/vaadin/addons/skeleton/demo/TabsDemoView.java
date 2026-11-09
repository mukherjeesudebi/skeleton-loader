package org.vaadin.addons.skeleton.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.skeleton.Skeleton;
import org.vaadin.addons.skeleton.SkeletonOptions;
import org.vaadin.addons.skeleton.SkeletonOptions.Animation;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.Map;

/** Each tab loads its content the first time it's opened. */
@Route("tabs")
public class TabsDemoView extends VerticalLayout {

    record Member(String name, String role) {
    }

    record Faq(String question, String answer) {
    }

    // Each tab's loader runs once, the first time the tab is shown.
    private final Map<Component, SerializableRunnable> loaders = new HashMap<>();

    public TabsDemoView() {
        VerticalLayout overview = new VerticalLayout();
        VerticalLayout team = new VerticalLayout();
        VerticalLayout faq = new VerticalLayout();

        TabSheet tabs = new TabSheet();
        tabs.setMaxWidth("720px");
        tabs.setWidthFull();
        tabs.add("Overview", overview);
        tabs.add("Team", team);
        tabs.add("FAQ", faq);

        // Placeholder content gives the skeleton realistic shapes to draw.
        overview.add(new Paragraph("Project summary text that is long enough to wrap over a couple of lines."),
                new Paragraph("Second paragraph with more placeholder text for the overview tab."));
        for (int i = 0; i < 4; i++) {
            team.add(memberRow(new Member("Team member name", "Role")));
        }
        for (int i = 0; i < 3; i++) {
            faq.add(new Details("Frequently asked question?"));
        }

        loaders.put(overview, () -> loadOverview(overview));
        loaders.put(team, () -> loadTeam(team));
        loaders.put(faq, () -> loadFaq(faq));

        tabs.addSelectedChangeListener(e -> loadTab(tabs.getComponent(e.getSelectedTab())));
        add(tabs);
        loadTab(overview);
    }

    private void loadTab(Component content) {
        SerializableRunnable loader = loaders.remove(content);
        if (loader != null) {
            loader.run();
        }
    }

    private void loadOverview(VerticalLayout layout) {
        Skeleton.load(layout,
                () -> DemoData.slow(1200, List.of(
                        "Skeleton Loader shows placeholder shapes while a view waits for its data.",
                        "It measures the real components in the browser, so the placeholders match "
                                + "the final layout and nothing jumps when the data arrives.")),
                texts -> {
                    layout.removeAll();
                    texts.forEach(t -> layout.add(new Paragraph(t)));
                });
    }

    private void loadTeam(VerticalLayout layout) {
        Skeleton.load(layout, SkeletonOptions.defaults().animation(Animation.PULSE),
                () -> DemoData.slow(1500, List.of(
                        new Member("Maya Rodriguez", "Engineering lead"),
                        new Member("Lukas Becker", "Frontend developer"),
                        new Member("Aino Virtanen", "Product designer"),
                        new Member("Kenji Tanaka", "Backend developer"))),
                members -> {
                    layout.removeAll();
                    members.forEach(m -> layout.add(memberRow(m)));
                },
                null, ForkJoinPool.commonPool());
    }

    private void loadFaq(VerticalLayout layout) {
        Skeleton.load(layout,
                () -> DemoData.slow(1000, List.of(
                        new Faq("Does it add server-side state?",
                                "Only one small overlay element per loading container, removed when loading ends."),
                        new Faq("Do I need push?", "Yes. Add @Push to your app shell."),
                        new Faq("Can I leave a component uncovered?", "Yes, call Skeleton.exclude(component)."))),
                faqs -> {
                    layout.removeAll();
                    faqs.forEach(f -> layout.add(new Details(f.question(), new Span(f.answer()))));
                });
    }

    private static Component memberRow(Member m) {
        Avatar avatar = new Avatar(m.name());
        VerticalLayout text = new VerticalLayout(new Span(m.name()), new Span(m.role()));
        text.setPadding(false);
        text.setSpacing(false);
        HorizontalLayout row = new HorizontalLayout(avatar, text);
        row.setAlignItems(Alignment.CENTER);
        return row;
    }
}
