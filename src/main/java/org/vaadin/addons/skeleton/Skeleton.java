package org.vaadin.addons.skeleton;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shows skeleton placeholders over every child of a component while its data loads.
 *
 * <pre>{@code
 * // Simplest: show, then hide yourself
 * Skeleton.show(profileCard);
 * ...
 * Skeleton.hide(profileCard);
 *
 * // Recommended: load in the background, fill the UI, hide automatically
 * Skeleton.load(profileCard,
 *         () -> userService.fetchProfile(id),   // runs off the UI thread
 *         profile -> {                          // runs with the UI locked
 *             name.setText(profile.name());
 *             title.setText(profile.title());
 *         });
 * }</pre>
 *
 * All methods except the loading task itself must be called while holding the UI
 * lock (i.e. from a listener, a view constructor or {@code ui.access}).
 * <p>
 * {@link #load} requires server push ({@code @Push} on the app shell). Without push the
 * browser is not told when the background task finishes, so the skeleton stays visible
 * until the next unrelated server round trip.
 */
public final class Skeleton {

    private static final Logger LOG = Logger.getLogger(Skeleton.class.getName());

    private static volatile Executor defaultExecutor = ForkJoinPool.commonPool();

    private Skeleton() {
    }

    /** Shows placeholders over all leaf components inside {@code container}. */
    public static void show(Component container) {
        show(container, SkeletonOptions.defaults());
    }

    /** Shows placeholders over {@code container} using the given options. */
    public static void show(Component container, SkeletonOptions options) {
        Objects.requireNonNull(container, "container");
        Objects.requireNonNull(options, "options");
        if (isShowing(container)) {
            return;
        }
        SkeletonOverlay overlay = new SkeletonOverlay(options);
        container.getElement().appendChild(overlay.getElement());
        ComponentUtil.setData(container, SkeletonOverlay.class, overlay);
    }

    /** Removes the placeholders and reveals the real content. Safe to call twice. */
    public static void hide(Component container) {
        Objects.requireNonNull(container, "container");
        SkeletonOverlay overlay = ComponentUtil.getData(container, SkeletonOverlay.class);
        if (overlay != null) {
            overlay.getElement().removeFromParent();
            ComponentUtil.setData(container, SkeletonOverlay.class, null);
        }
    }

    public static boolean isShowing(Component container) {
        return ComponentUtil.getData(container, SkeletonOverlay.class) != null;
    }

    /**
     * Treats this component as a single placeholder block instead of descending
     * into its children (useful for custom composite components).
     */
    public static void markAsLeaf(Component component) {
        component.getElement().setAttribute("skeleton-leaf", true);
    }

    /** Leaves this component (and everything inside it) visible and uncovered. */
    public static void exclude(Component component) {
        component.getElement().setAttribute("skeleton-ignore", true);
    }

    /** Sets the executor used by {@link #load} when none is given, e.g. a Spring task executor. */
    public static void setDefaultExecutor(Executor executor) {
        defaultExecutor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Shows the skeleton, runs {@code task} in the background, then hands the result
     * to {@code onSuccess} with the UI locked and hides the skeleton. Requires {@code @Push}.
     */
    public static <T> CompletableFuture<T> load(Component container, Supplier<T> task,
            SerializableConsumer<T> onSuccess) {
        return load(container, SkeletonOptions.defaults(), task, onSuccess, null, defaultExecutor);
    }

    /** Like {@link #load(Component, Supplier, SerializableConsumer)} with an error callback. */
    public static <T> CompletableFuture<T> load(Component container, Supplier<T> task,
            SerializableConsumer<T> onSuccess, SerializableConsumer<Throwable> onError) {
        return load(container, SkeletonOptions.defaults(), task, onSuccess, onError, defaultExecutor);
    }

    /**
     * Full version of {@code load}.
     *
     * @param onError called with the UI locked if {@code task} throws; may be {@code null},
     *                in which case the error is logged. The skeleton is hidden either way.
     * @param executor where {@code task} runs
     */
    public static <T> CompletableFuture<T> load(Component container, SkeletonOptions options,
            Supplier<T> task, SerializableConsumer<T> onSuccess,
            SerializableConsumer<Throwable> onError, Executor executor) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(executor, "executor");

        UI ui = container.getUI().orElseGet(UI::getCurrent);
        if (ui == null) {
            throw new IllegalStateException(
                    "Skeleton.load must be called from a UI thread (listener, view constructor or ui.access)");
        }

        show(container, options);

        CompletableFuture<T> future = CompletableFuture.supplyAsync(task, executor);
        future.whenComplete((result, error) -> {
            try {
                ui.access(() -> {
                    try {
                        if (error == null) {
                            onSuccess.accept(result);
                        } else if (onError != null) {
                            onError.accept(unwrap(error));
                        } else {
                            LOG.log(Level.WARNING, "Skeleton loading task failed", unwrap(error));
                        }
                    } finally {
                        hide(container);
                    }
                });
            } catch (com.vaadin.flow.component.UIDetachedException detached) {
                // The user navigated away or closed the tab; nothing left to update.
            }
        });
        return future;
    }

    private static Throwable unwrap(Throwable t) {
        return (t instanceof java.util.concurrent.CompletionException && t.getCause() != null)
                ? t.getCause() : t;
    }
}
