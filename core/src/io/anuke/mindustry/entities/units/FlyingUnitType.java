package io.anuke.mindustry.entities.units;

import io.anuke.mindustry.entities.Unit;
import io.anuke.ucore.graphics.Draw;

public class FlyingUnitType extends UnitType {

    public FlyingUnitType(String name) {
        super(name);
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation);

        Draw.alpha(1f);
    }

    @Override
    public void behavior(BaseUnit unit) {

    }
}
