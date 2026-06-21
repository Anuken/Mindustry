package mindustry.graphics.shaders;

import static mindustry.Vars.*;

public class DepthScreenspaceShader extends LoadShader{

    public DepthScreenspaceShader(){
        super("depth-screenspace", "depth-screenspace");
    }

    @Override
    public void apply(){
        var buffer = renderer.planets.getDepthFramebuffer();

        buffer.getTexture().bind(1);
        buffer.getDepthTexture().bind(4);

        setUniformi("u_color", 1);
        setUniformi("u_depth", 4);
    }
}
