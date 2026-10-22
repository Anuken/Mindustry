package arc.scene.utils;

import arc.math.geom.Rect;
import arc.scene.Group;

/**
 * Allows a parent to set the area that is visible on a child actor to allow the child to cull when drawing itself. This must only
 * be used for actors that are not rotated or scaled.
 * <p>
 * When Group is given a culling rectangle with {@link Group#setCullingArea(Rect)}, it will automatically call
 * {@link #setCullingArea(Rect)} on its children.
 * @author Nathan Sweet
 */
public interface Cullable{
    /** @param cullingArea The culling area in the child actor's coordinates. */
    void setCullingArea(Rect cullingArea);
}
