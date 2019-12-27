package mindustry.entities.type;

import arc.math.geom.Vec2;
import mindustry.entities.traits.SolidTrait;

public abstract class SolidEntity extends BaseEntity implements SolidTrait{
    protected transient Vec2 velocity = new Vec2(0f, 0.0001f);
    private transient Vec2 lastPosition = new Vec2();

    @Override
    public Vec2 lastPosition(){
        return lastPosition;
    }

    @Override
    public Vec2 velocity(){
        return velocity;
    }
}
