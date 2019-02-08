package io.anuke.mindustry.graphics;

//TODO implement effects again
public enum CacheLayer{
    water{
        public void begin(){
            //Draw.shader(Shaders.water);
        }

        public void end(){
            //Draw.shader();
        }
    },
    lava{
    },
    oil{
    },
    space{
    },
    normal,
    walls;

    public void begin(){

    }

    public void end(){

    }

    protected void beginShader(){

    }

    public void endShader(){
    }
}
