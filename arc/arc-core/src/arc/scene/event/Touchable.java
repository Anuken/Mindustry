package arc.scene.event;

/**
 * Determines how touch input events are distributed to an element and any children.
 * @author Nathan Sweet
 */
public enum Touchable{
    /** All touch input events will be received by the element and any children. */
    enabled,
    /** No touch input events will be received by the element or any children. */
    disabled,
    /**
     * No touch input events will be received by the element, but children will still receive events. Note that events on the
     * children will still bubble to the parent.
     */
    childrenOnly
}
