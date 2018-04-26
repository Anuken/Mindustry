package io.anuke.mindustry.entities.units.types;

import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnitType;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class Vtol extends FlyingUnitType {

    public Vtol(){
        super("vtol");
        setAmmo(AmmoTypes.basicIron);
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation - 90);
        for(int i : Mathf.signs){
            Draw.rect(name + "-booster-1", unit.x, unit.y, 12*i, 12, unit.rotation - 90);
            Draw.rect(name + "-booster-2", unit.x, unit.y, 12*i, 12, unit.rotation - 90);
        }

        Draw.alpha(1f);
    }
}
