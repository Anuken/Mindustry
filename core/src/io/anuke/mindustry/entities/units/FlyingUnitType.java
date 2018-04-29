package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.Unit;
import io.anuke.ucore.graphics.Draw;

public class FlyingUnitType extends UnitType {
    protected static Vector2 vec = new Vector2();

    protected float boosterLength = 4.5f;

    public FlyingUnitType(String name) {
        super(name);
        speed = 0.2f;
        maxVelocity = 2f;
        drag = 0.01f;
        isFlying = true;
    }

    @Override
    public void update(BaseUnit unit) {
        super.update(unit);

        unit.rotation = unit.velocity.angle();
        unit.state.update(unit);
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation - 90);

        Draw.alpha(1f);
    }

    @Override
    public void behavior(BaseUnit unit) {

    }
}
