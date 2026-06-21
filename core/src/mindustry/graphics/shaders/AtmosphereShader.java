package mindustry.graphics.shaders;

import arc.*;
import arc.graphics.g3d.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class AtmosphereShader extends LoadShader{
    private static final Mat3D mat = new Mat3D();

    public Camera3D camera;
    public Planet planet;

    public AtmosphereShader(){
        super("atmosphere", "atmosphere");
    }

    @Override
    public void apply(){
        var buffer = renderer.planets.getDepthFramebuffer();

        setUniformMatrix4("u_projView", camera.combined.val);
        setUniformMatrix4("u_invProj", mat.set(camera.projection).inv().val);
        setUniformMatrix4("u_trans", planet.getTransform(mat).val);

        setUniformf("u_camPos", camera.position);
        setUniformf("u_relCamPos", Tmp.v31.set(camera.position).sub(planet.position));
        setUniformf("u_depthRange", camera.near, camera.far);
        setUniformf("u_center", planet.position);
        setUniformf("u_light", planet.getLightNormal());
        setUniformf("u_color", planet.atmosphereColor.r, planet.atmosphereColor.g, planet.atmosphereColor.b);

        setUniformf("u_innerRadius", planet.radius + planet.atmosphereRadIn);
        setUniformf("u_outerRadius", planet.radius + planet.atmosphereRadOut);

        buffer.depthTexture.bind(4);
        setUniformi("u_topology", 4);
        setUniformf("u_viewport", Core.graphics.getWidth(), Core.graphics.getHeight());
    }
}
