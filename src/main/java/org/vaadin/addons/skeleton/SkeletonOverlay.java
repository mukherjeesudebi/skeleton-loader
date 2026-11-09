package org.vaadin.addons.skeleton;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * The client-side overlay that draws the placeholders. Created and removed by
 * {@link Skeleton}; you normally never instantiate this yourself.
 */
@Tag("skeleton-overlay")
@JsModule("./skeleton-overlay/skeleton-overlay.js")
public final class SkeletonOverlay extends Component {

    SkeletonOverlay(SkeletonOptions options) {
        getElement().setAttribute("depth", options.getDepth().name().toLowerCase());
        getElement().setAttribute("animation", options.getAnimation().name().toLowerCase());
        getElement().setAttribute("text-lines", Boolean.toString(options.isTextLines()));
    }
}
