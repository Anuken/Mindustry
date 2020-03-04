package mindustry.graphics;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.gl.Shader;

import static arc.Core.camera;
import static mindustry.Vars.renderer;

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
    slag{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.slag);
        }
    },
    normal(5),
    walls;

    public static final CacheLayer[] all = values();
    /** Capacity multiplier. */
    public final int capacity;

    CacheLayer(){
        this(2);
    }

    CacheLayer(int capacity){
        this.capacity = capacity;
    }

    public void begin(){

    }

    public void end(){

    }

    void beginShader(){
        if(!Core.settings.getBool("animatedwater")) return;

        renderer.blocks.floor.endc();
        renderer.effectBuffer.begin();
        Core.graphics.clear(Color.clear);
        renderer.blocks.floor.beginc();
    }

    void endShader(Shader shader){
        if(!Core.settings.getBool("animatedwater")) return;

        renderer.blocks.floor.endc();
        renderer.effectBuffer.end();

        Draw.shader(shader);
        Draw.rect(Draw.wrap(renderer.effectBuffer.getTexture()), camera.position.x, camera.position.y, camera.width, -camera.height);
        Draw.shader();

        renderer.blocks.floor.beginc();
    }
}
