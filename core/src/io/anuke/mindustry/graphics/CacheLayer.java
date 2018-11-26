package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Shader;

import static io.anuke.mindustry.Vars.renderer;

public enum CacheLayer{
    water{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.water);
        }
    },
    lava{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.lava);
        }
    },
    oil{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.oil);
        }
    },
    space{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.space);
        }
    },
    normal;

    public void begin(){

    }

    public void end(){

    }

    protected void beginShader(){
        //renderer.getBlocks().endFloor();
        renderer.effectSurface.getBuffer().begin();
        Graphics.clear(Color.CLEAR);
        //renderer.getBlocks().beginFloor();
    }

    public void endShader(Shader shader){
        renderer.blocks.endFloor();

        //renderer.effectSurface.getBuffer().end();

        renderer.pixelSurface.getBuffer().begin();

        Graphics.shader(shader);
        Graphics.begin();
        Draw.rect(renderer.effectSurface.texture(), Core.camera.position.x, Core.camera.position.y,
                Core.camera.viewportWidth * Core.camera.zoom, -Core.camera.viewportHeight * Core.camera.zoom);
        Graphics.end();
        Graphics.shader();
        renderer.blocks.beginFloor();
    }
}
