package mindustry.ai.ai.utils;

import arc.math.geom.*;

public class VecLocation implements Location{
    float orientation;
    Vec2 position = new Vec2();

    @Override
    public Vec2 getPosition(){
        return position;
    }

    @Override
    public float getOrientation(){
        return orientation;
    }

    @Override
    public void setOrientation(float orientation){
        this.orientation = orientation;
    }
}
