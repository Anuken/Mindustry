package io.anuke.mindustry.entities.units.types;

import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnitType;
import io.anuke.mindustry.entities.units.UnitState;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class Cruiser extends FlyingUnitType {


    public Cruiser(){
        super("vtol");
        setAmmo(AmmoTypes.bulletIron);
        speed = 0.2f;
        maxVelocity = 1.4f;
        health = 300f;
    }

    @Override
    public void drawUnder(BaseUnit unit) {
        float rotation = unit.rotation - 90;
        float scl = 0.6f + Mathf.absin(Timers.time(), 1f, 0.3f);
        float dy = -6f*scl;

        Draw.color(Palette.lighterOrange, Palette.lightFlame, Mathf.absin(Timers.time(), 3f, 0.7f));

        Draw.rect("vtol-flame",
                unit.x + Angles.trnsx(rotation, 0, dy),
                unit.y + Angles.trnsy(rotation, 0, dy), Mathf.atan2(0, dy) + rotation);

        Draw.color();
    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation - 90);

        Draw.alpha(1f);
    }

    @Override
    public void update(BaseUnit unit) {
        super.update(unit);

        unit.x += Mathf.sin(Timers.time() + unit.id * 999, 25f, 0.06f);
        unit.y += Mathf.cos(Timers.time() + unit.id * 999, 25f, 0.06f);

        if(unit.velocity.len() <= 0.2f){
            unit.rotation += Mathf.sin(Timers.time() + unit.id * 99, 10f, 8f);
        }

        if(unit.timer.get(timerBoost, 2)){
        //    unit.effectAt(UnitFx.vtolHover, unit.rotation + 180f, 4f, 0);
        }
    }

    @Override
    public UnitState getStartState(){
        return resupply;
    }
}
