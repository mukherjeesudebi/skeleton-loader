package org.vaadin.addons.skeleton.demo;

/** Helpers that fake slow backend calls for the demo views. */
final class DemoData {

    private DemoData() {
    }

    /** Waits {@code ms} milliseconds, then returns {@code value}. */
    static <T> T slow(long ms, T value) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return value;
    }
}
