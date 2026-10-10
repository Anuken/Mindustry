package arc.util.viewport;

import arc.graphics.Camera;
import arc.util.Scaling;

/**
 * A ScalingViewport that uses {@link Scaling#fill} so it keeps the aspect ratio by scaling the world up to take the whole screen
 * (some of the world may be off screen).
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
public class FillViewport extends ScalingViewport{
    /** Creates a new viewport using a new {@link Camera}. */
    public FillViewport(float worldWidth, float worldHeight){
        super(Scaling.fill, worldWidth, worldHeight);
    }

    public FillViewport(float worldWidth, float worldHeight, Camera camera){
        super(Scaling.fill, worldWidth, worldHeight, camera);
    }
}
