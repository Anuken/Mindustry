package arc.util.viewport;

import arc.graphics.Camera;
import arc.util.Scaling;

/**
 * A ScalingViewport that uses {@link Scaling#stretch} so it does not keep the aspect ratio, the world is scaled to take the whole
 * screen.
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
public class StretchViewport extends ScalingViewport{
    /** Creates a new viewport using a new {@link Camera}. */
    public StretchViewport(float worldWidth, float worldHeight){
        super(Scaling.stretch, worldWidth, worldHeight);
    }

    public StretchViewport(float worldWidth, float worldHeight, Camera camera){
        super(Scaling.stretch, worldWidth, worldHeight, camera);
    }
}
