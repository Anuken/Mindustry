package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;

import static io.anuke.arc.Core.camera;
import static io.anuke.mindustry.Vars.renderer;

public enum CacheLayer{
    water{
        @Override
        public void begin(){
            if(!Core.settings.getBool("animatedwater")) return;

            renderer.blocks.floor.endc();
            renderer.shieldBuffer.begin();
            Core.graphics.clear(Color.CLEAR);
            renderer.blocks.floor.beginc();
        }

        @Override
        public void end(){
            if(!Core.settings.getBool("animatedwater")) return;

            renderer.blocks.floor.endc();
            renderer.shieldBuffer.end();

            Draw.shader(Shaders.water);
            Draw.rect(Draw.wrap(renderer.shieldBuffer.getTexture()), camera.position.x, camera.position.y, camera.width, -camera.height);
            Draw.shader();

            renderer.blocks.floor.beginc();
        }
    },
    lava,
    oil,
    space,
    normal,
    walls;

    public void begin(){

    }

    public void end(){

    }
}
