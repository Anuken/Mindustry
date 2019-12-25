package mindustry.entities.traits;

import arc.math.geom.Position;

public interface MoveTrait extends Position{

    void setX(float x);

    void setY(float y);

    default void moveBy(float x, float y){
        setX(getX() + x);
        setY(getY() + y);
    }

    default void set(float x, float y){
        setX(x);
        setY(y);
    }
}
