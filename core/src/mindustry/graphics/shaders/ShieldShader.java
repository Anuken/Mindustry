package mindustry.graphics.shaders;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;

public class ShieldShader extends LoadShader{
    /** Outline width in world units. */
    public float outlineWidth = 1.8f;

    protected FrameBuffer rows = new FrameBuffer(), field = new FrameBuffer();
    protected Shader rowShader = new FieldShader("shieldrow"), colShader = new FieldShader("shieldcol");
    protected float radius;

    public ShieldShader(){
        super("shield", "screenspace");
    }

    /** Builds a signed distance field of the buffer's contents, then blits the buffer with outlines. */
    public void render(FrameBuffer buffer){
        int w = buffer.getWidth(), h = buffer.getHeight();
        radius = Math.min(outlineWidth * w / Core.camera.width, 21f);
        rows.resize(w, h);
        field.resize(w, h);

        Blending.disabled.apply();
        rows.begin();
        buffer.blit(rowShader);
        rows.end();
        field.begin();
        rows.blit(colShader);
        field.end();
        Draw.getBlend().apply();

        field.texture.bind(1);
        buffer.texture.bind(0);
        Draw.blit(this);
    }

    @Override
    public void apply(){
        setUniformf("u_dp", Scl.scl(1f));

        setUniformf("u_time", Time.time / Scl.scl(1f));
        setUniformf("u_offset",
        Core.camera.position.x - Core.camera.width / 2,
        Core.camera.position.y - Core.camera.height / 2);
        setUniformf("u_texsize", Core.camera.width, Core.camera.height);
        setUniformf("u_invsize", 1f/Core.camera.width, 1f/Core.camera.height);
        setUniformf("u_radius", radius);
        setUniformi("u_field", 1);
    }

    protected class FieldShader extends LoadShader{
        public FieldShader(String frag){
            super(frag, "screenspace");
        }

        @Override
        public void apply(){
            setUniformf("u_invres", 1f / rows.getWidth(), 1f / rows.getHeight());
            setUniformf("u_radius", radius);
        }
    }
}
