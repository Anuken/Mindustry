package mindustry.graphics.shaders;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;

public class ShieldShader extends LoadShader{

    public ShieldShader(){
        super("shield", "screenspace");
    }

    @Override
    public void apply(){
        setUniformf("u_dp", Scl.scl(1f));
        setUniformf("u_time", Time.time / Scl.scl(1f));
        setUniformf("u_offset",
        Core.camera.position.x - Core.camera.width / 2,
        Core.camera.position.y - Core.camera.height / 2);
        setUniformf("u_texsize", Core.camera.width, Core.camera.height);
        setUniformf("u_invsize", 1f / Core.camera.width, 1f / Core.camera.height);
    }
}
