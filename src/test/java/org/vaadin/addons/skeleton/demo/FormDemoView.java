package org.vaadin.addons.skeleton.demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.skeleton.Skeleton;
import org.vaadin.addons.skeleton.SkeletonOptions;
import org.vaadin.addons.skeleton.SkeletonOptions.Depth;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/** An edit form whose record is fetched in the background. Try both depth options. */
@Route("form")
public class FormDemoView extends VerticalLayout {

    record Customer(String firstName, String lastName, String email, String country,
            LocalDate since, String tier, String notes, boolean newsletter) {
    }

    private final TextField firstName = new TextField("First name");
    private final TextField lastName = new TextField("Last name");
    private final EmailField email = new EmailField("Email");
    private final ComboBox<String> country = new ComboBox<>("Country",
            List.of("Finland", "Germany", "Spain", "United States"));
    private final DatePicker since = new DatePicker("Customer since");
    private final Select<String> tier = new Select<>();
    private final TextArea notes = new TextArea("Notes");
    private final Checkbox newsletter = new Checkbox("Subscribed to newsletter");

    private final VerticalLayout panel = new VerticalLayout();
    private final RadioButtonGroup<Depth> depth = new RadioButtonGroup<>("Depth");

    public FormDemoView() {
        tier.setLabel("Tier");
        tier.setItems("Free", "Pro", "Enterprise");

        FormLayout form = new FormLayout(firstName, lastName, email, country, since, tier, notes, newsletter);
        form.setColspan(notes, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));

        Button save = new Button("Save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        panel.add(new H3("Edit customer"), form, new HorizontalLayout(save, new Button("Cancel")));
        panel.setMaxWidth("720px");

        depth.setItems(Depth.values());
        depth.setValue(Depth.LEAVES);

        add(panel, new HorizontalLayout(new Button("Reload (1.5 s)", e -> load()), depth));
        load();
    }

    private void load() {
        Skeleton.load(panel, SkeletonOptions.defaults().depth(depth.getValue()),
                () -> DemoData.slow(1500, new Customer("Aino", "Virtanen", "aino.virtanen@example.com",
                        "Finland", LocalDate.of(2019, 4, 12), "Pro",
                        "Prefers email contact. Renewal discussion planned for Q1.", true)),
                c -> {
                    firstName.setValue(c.firstName());
                    lastName.setValue(c.lastName());
                    email.setValue(c.email());
                    country.setValue(c.country());
                    since.setValue(c.since());
                    tier.setValue(c.tier());
                    notes.setValue(c.notes());
                    newsletter.setValue(c.newsletter());
                },
                null, ForkJoinPool.commonPool());
    }
}
