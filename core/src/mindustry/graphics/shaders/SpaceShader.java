package mindustry.graphics.shaders;

import arc.*;
import arc.assets.loaders.TextureLoader.*;
import arc.graphics.*;
import arc.util.*;

import static mindustry.Vars.*;

//seed: 8kmfuix03fw
public class SpaceShader extends SurfaceShader{
    Texture texture;

    public SpaceShader(String frag){
        super(frag);

        Core.assets.load("sprites/space.png", Texture.class, new TextureParameter(){{
            magFilter = TextureFilter.linear;
            minFilter = TextureFilter.mipMapLinearLinear;
            wrapU = wrapV = TextureWrap.mirroredRepeat;
            genMipMaps = true;
        }}).loaded = t -> texture = t;
    }

    @Override
    public void apply(){
        setUniformf("u_campos", Core.camera.position.x, Core.camera.position.y);
        setUniformf("u_ccampos", Core.camera.position);
        setUniformf("u_resolution", Core.graphics.getWidth(), Core.graphics.getHeight());
        setUniformf("u_time", Time.time);

        texture.bind(1);
        renderer.effectBuffer.getTexture().bind(0);

        setUniformi("u_stars", 1);
    }
}
