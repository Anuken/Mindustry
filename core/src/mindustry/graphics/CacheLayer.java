package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;

import static mindustry.Vars.*;

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
    normal,
    walls;

    public static final CacheLayer[] all = values();

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
