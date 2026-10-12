package arc.fx.filters;

import arc.*;
import arc.fx.*;

/**
 * Fisheye distortion filter
 * @author tsagrista
 */
public class FisheyeDistortionFilter extends FxFilter{

    public FisheyeDistortionFilter(){
        super(compileShader(
        Core.files.internal("vfxshaders/screenspace.vert"),
        Core.files.internal("vfxshaders/fisheye.frag")));
    }
}
