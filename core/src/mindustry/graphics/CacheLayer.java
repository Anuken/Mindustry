package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;

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
    mud{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.mud);
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
    walls(3);

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

        renderer.effectBuffer.blit(shader);

        renderer.blocks.floor.beginc();
    }
}
