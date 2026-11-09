package org.vaadin.addons.skeleton;

import java.io.Serializable;

/**
 * Options for {@link Skeleton#show(com.vaadin.flow.component.Component, SkeletonOptions)}.
 * <p>
 * Example: {@code SkeletonOptions.defaults().depth(Depth.CHILDREN).animation(Animation.PULSE)}
 */
public final class SkeletonOptions implements Serializable {

    /** Which elements get their own placeholder block. */
    public enum Depth {
        /**
         * Walk nested layouts and draw one block per leaf component (default).
         * A form inside a card inside a layout becomes individual field-shaped blocks.
         */
        LEAVES,
        /** Draw one block per direct child of the container, without descending. */
        CHILDREN
    }

    /** How the placeholder blocks are animated. */
    public enum Animation {
        /** A highlight sweeps across each block (default). */
        SHIMMER,
        /** Blocks fade gently in and out. */
        PULSE,
        /** Static blocks. */
        NONE
    }

    private Depth depth = Depth.LEAVES;
    private Animation animation = Animation.SHIMMER;
    private boolean textLines = true;

    private SkeletonOptions() {
    }

    public static SkeletonOptions defaults() {
        return new SkeletonOptions();
    }

    public SkeletonOptions depth(Depth depth) {
        this.depth = depth == null ? Depth.LEAVES : depth;
        return this;
    }

    public SkeletonOptions animation(Animation animation) {
        this.animation = animation == null ? Animation.SHIMMER : animation;
        return this;
    }

    /**
     * When true (default), multi-line text such as paragraphs is drawn as several
     * thin lines instead of one solid block.
     */
    public SkeletonOptions textLines(boolean textLines) {
        this.textLines = textLines;
        return this;
    }

    public Depth getDepth() {
        return depth;
    }

    public Animation getAnimation() {
        return animation;
    }

    public boolean isTextLines() {
        return textLines;
    }
}
