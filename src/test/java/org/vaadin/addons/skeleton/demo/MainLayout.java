package org.vaadin.addons.skeleton.demo;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;

/** Side navigation shared by all demo views. */
@Layout
public class MainLayout extends AppLayout {

    public MainLayout() {
        H1 title = new H1("Skeleton Loader");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");
        addToNavbar(new DrawerToggle(), title);

        SideNav nav = new SideNav();
        nav.addItem(
                new SideNavItem("Profile card", SkeletonDemoView.class),
                new SideNavItem("Form", FormDemoView.class),
                new SideNavItem("Dashboard", DashboardDemoView.class),
                new SideNavItem("Tabs", TabsDemoView.class),
                new SideNavItem("Grid", GridDemoView.class));
        addToDrawer(nav);
    }
}
