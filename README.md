# Skeleton Loader for Vaadin Flow

Show skeleton placeholders over every component inside a container while its data loads, with one call:

```java
Skeleton.load(profileCard,
        () -> userService.fetchProfile(userId),   // runs in the background
        profile -> {                              // runs with the UI locked
            name.setText(profile.name());
            title.setText(profile.title());
            avatar.setName(profile.name());
        });
```

The add-on measures each child in the browser and draws a matching grey block over it: avatars become circles, buttons and fields keep their corner radius, and paragraphs become several text lines. The real components stay untouched underneath and are made `inert` (not clickable or focusable) until loading finishes.

## Install

```
mvn install
```

Then add the dependency to your application:

```xml
<dependency>
    <groupId>org.vaadin.addons</groupId>
    <artifactId>skeleton-loader</artifactId>
    <version>1.0.0</version>
</dependency>
```

Built against Vaadin 25.3 and Java 21.

## API

| Method | What it does |
|---|---|
| `Skeleton.show(container)` | Covers every leaf component inside `container` |
| `Skeleton.show(container, options)` | Same, with options (see below) |
| `Skeleton.hide(container)` | Removes the placeholders |
| `Skeleton.load(container, task, onSuccess)` | Shows, runs `task` off the UI thread, calls `onSuccess`, hides (requires `@Push`) |
| `Skeleton.load(container, task, onSuccess, onError)` | Same, with error handling |
| `Skeleton.markAsLeaf(component)` | Draw this component as one block, don't look inside |
| `Skeleton.exclude(component)` | Leave this component visible and uncovered |
| `Skeleton.setDefaultExecutor(executor)` | Executor for `load`, e.g. a Spring `TaskExecutor` |

Options:

```java
Skeleton.show(panel, SkeletonOptions.defaults()
        .depth(SkeletonOptions.Depth.CHILDREN)        // one block per direct child
        .animation(SkeletonOptions.Animation.PULSE)   // SHIMMER (default), PULSE, NONE
        .textLines(false));                           // solid block instead of lines
```

## Important: why `load()` instead of `show()` + `hide()`

Vaadin sends changes to the browser only after your Java code returns. This does **not** work:

```java
// Wrong: the user never sees the skeleton
Skeleton.show(card);
Profile p = service.fetch();   // blocks the request
fill(p);
Skeleton.hide(card);           // show and hide arrive in the same response
```

`load()` runs the task on a background thread and updates the UI afterwards with `ui.access()`. **Server push is required** so that this update reaches the browser:

```java
@Push
public class AppShell implements AppShellConfigurator { }
```

Without push, the browser isn't told when the task finishes, so the skeleton stays on screen until some other request reaches the server.

If you use `show()`/`hide()` yourself, call `hide()` from inside `ui.access()` once your own background work is done.

## Tips

- Empty text components have no size before their data arrives. The add-on gives empty text elements a single line of default width, but for exact shapes set placeholder text or a `min-width`/`min-height`.
- The container should be a layout or plain element (`VerticalLayout`, `HorizontalLayout`, `Div`, `FormLayout`, `Card`...). Components that render their children inside their own shadow DOM without a default slot may not show the overlay.
- A `Grid` inside the container is drawn as its header plus rows, with one bar per cell. While the grid is still empty, placeholder rows fill its height. Use `Skeleton.markAsLeaf(grid)` to draw it as a single block instead. Put the grid inside a layout and pass the layout as the container; passing the grid itself doesn't work, because the grid doesn't display extra child elements.
- The overlay is added as a child element of the container, so it briefly appears in `container.getChildren()` while showing.
- Styling uses CSS custom properties, set on the container or globally:

```css
html {
  --skeleton-color: var(--lumo-contrast-10pct);   /* block color */
  --skeleton-highlight: rgba(255, 255, 255, .35); /* shimmer color */
  --skeleton-background: var(--lumo-base-color);  /* covers the real content */
  --skeleton-radius: 6px;
  --skeleton-duration: 1.4s;
  --skeleton-z-index: 10;
}
```

- Animation is disabled automatically for users who prefer reduced motion.
- The container gets `aria-busy="true"` and screen readers hear "Loading" while it shows.

## Run the demo locally

```
mvn
```

Then open http://localhost:8080. The demo app is under `src/test/java` and is not part of the add-on jar.

## Demo view

```java
@Route("skeleton-demo")
public class SkeletonDemoView extends VerticalLayout {

    public SkeletonDemoView() {
        Avatar avatar = new Avatar();
        H3 name = new H3("Loading name");
        Paragraph bio = new Paragraph("A placeholder sentence long enough to wrap onto two lines in the card.");
        Button follow = new Button("Follow");

        VerticalLayout card = new VerticalLayout(
                new HorizontalLayout(avatar, name), bio, follow);
        card.setWidth("360px");
        add(card, new Button("Reload", e -> load(card, avatar, name, bio)));

        load(card, avatar, name, bio);
    }

    private void load(Component card, Avatar avatar, H3 name, Paragraph bio) {
        Skeleton.load(card,
                () -> { sleep(2000); return "Maya Rodriguez"; },
                person -> {
                    avatar.setName(person);
                    name.setText(person);
                    bio.setText("Leads the platform team and writes about distributed systems.");
                });
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```
