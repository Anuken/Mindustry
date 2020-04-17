package mindustry.graphics;

import arc.*;
import arc.fx.*;

public class SnowFilter extends FxFilter{

    public SnowFilter(){
        super(compileShader(Core.files.internal("shaders/screenspace.vert"), Core.files.internal("shaders/snow.frag")));
        autobind = true;
    }

    @Override
    public void setParams(){
        shader.setUniformf("u_time", time / 60f);
        shader.setUniformf("u_pos", Core.camera.position.x - Core.camera.width / 2, Core.camera.position.y - Core.camera.height / 2);
        shader.setUniformf("u_resolution", Core.camera.width, Core.camera.height);
    }
}
