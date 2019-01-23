package io.anuke.mindustry.graphics;

//TODO implement effects again
public enum CacheLayer{
    water{
    },
    lava{
    },
    oil{
    },
    space{
    },
    normal,
    walls{ //TODO implement walls
    };

    public void begin(){

    }

    public void end(){

    }

    protected void beginShader(){

    }

    public void endShader(){
    }
}
