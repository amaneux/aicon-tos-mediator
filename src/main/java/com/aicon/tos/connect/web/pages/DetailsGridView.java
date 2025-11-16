package com.aicon.tos.connect.web.pages;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("details-grid")
public class DetailsGridView extends VerticalLayout {

    public DetailsGridView() {
        List<Person> people = List.of(
                new Person("Alice", 30, "Alice works in marketing."),
                new Person("Bob", 42, "Bob is a senior Java developer."),
                new Person("Charlie", 25, "Charlie just joined the team.")
        );

        Grid<Person> grid = new Grid<>(Person.class, false);
        grid.addColumn(Person::name).setHeader("Name");
        grid.addColumn(Person::age).setHeader("Age");

        // ✅ Proper button column for Vaadin 24.6
        grid.addComponentColumn(person -> {
            Button detailsBtn = new Button("Details");
            detailsBtn.addClickListener(e -> openDetailsDialog(person));
            // Prevent "inert button" issue (Vaadin patch for late click)
            detailsBtn.getElement().setAttribute("tabindex", "0");
            return detailsBtn;
        }).setHeader("More Info");

        grid.setItems(people);
        grid.setAllRowsVisible(true);

        add(grid);
    }

    private void openDetailsDialog(Person person) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Details for " + person.name());
        dialog.add(new Span(person.details()));

        Button closeButton = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    record Person(String name, int age, String details) {}

}