package org.vaadin.addons.skeleton.demo;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.skeleton.Skeleton;
import org.vaadin.addons.skeleton.SkeletonOptions;

import java.util.concurrent.ForkJoinPool;

@Route("")
public class SkeletonDemoView extends VerticalLayout {

    private final Avatar avatar = new Avatar();
    private final H3 name = new H3("Loading name");
    private final Paragraph bio = new Paragraph(
            "A placeholder sentence long enough to wrap onto two lines in the card.");
    private final VerticalLayout card = new VerticalLayout(
            new HorizontalLayout(avatar, name), bio, new Button("Follow"));
    private final RadioButtonGroup<SkeletonOptions.Animation> animation = new RadioButtonGroup<>("Animation");

    public SkeletonDemoView() {
        card.setWidth("360px");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)");

        animation.setItems(SkeletonOptions.Animation.values());
        animation.setValue(SkeletonOptions.Animation.SHIMMER);

        add(card, new Button("Reload (2 s)", e -> load()), animation);
        load();
    }

    private void load() {
        Skeleton.load(card, SkeletonOptions.defaults().animation(animation.getValue()),
                () -> {
                    sleep(2000);
                    return "Maya Rodriguez";
                },
                person -> {
                    avatar.setName(person);
                    name.setText(person);
                    bio.setText("Leads the platform team and writes about distributed systems.");
                },
                null, ForkJoinPool.commonPool());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
