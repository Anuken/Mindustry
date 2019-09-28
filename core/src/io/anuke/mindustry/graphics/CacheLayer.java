package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.glutils.Shader;

import static io.anuke.arc.Core.camera;
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
    tar{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.tar);
        }
    },
    normal,
    walls;

    public void begin(){

    }

    public void end(){

    }

    void beginShader(){
        if(!Core.settings.getBool("animatedwater")) return;

        renderer.blocks.floor.endc();
        renderer.shieldBuffer.begin();
        Core.graphics.clear(Color.clear);
        renderer.blocks.floor.beginc();
    }

    void endShader(Shader shader){
        if(!Core.settings.getBool("animatedwater")) return;

        renderer.blocks.floor.endc();
        renderer.shieldBuffer.end();

        Draw.shader(shader);
        Draw.rect(Draw.wrap(renderer.shieldBuffer.getTexture()), camera.position.x, camera.position.y, camera.width, -camera.height);
        Draw.shader();

        renderer.blocks.floor.beginc();
    }
}
