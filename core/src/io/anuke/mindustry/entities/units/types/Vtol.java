package io.anuke.mindustry.entities.units.types;

import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnitType;
import io.anuke.ucore.graphics.Draw;

public class Vtol extends FlyingUnitType {

    public Vtol(){
        super("vtol");
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation - 90);

        Draw.alpha(1f);
    }
}
