package io.anuke.mindustry.entities.units.types;

import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.content.fx.UnitFx;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnitType;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class Vtol extends FlyingUnitType {

    public Vtol(){
        super("vtol");
        setAmmo(AmmoTypes.basicIron);
        speed = 0.3f;
        maxVelocity = 2f;
        reload = 7;
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
        for(int i : Mathf.signs){
            Draw.rect(name + "-booster-1", unit.x, unit.y, 12*i, 12, unit.rotation - 90);
            Draw.rect(name + "-booster-2", unit.x, unit.y, 12*i, 12, unit.rotation - 90);
        }

        Draw.alpha(1f);
    }

    @Override
    public void update(BaseUnit unit) {
        super.update(unit);

        unit.x += Mathf.sin(Timers.time() + unit.id * 999, 25f, 0.07f);
        unit.y += Mathf.cos(Timers.time() + unit.id * 999, 25f, 0.07f);

        if(unit.velocity.len() <= 0.2f){
            unit.rotation += Mathf.sin(Timers.time() + unit.id * 99, 10f, 8f);
        }

        if(unit.timer.get(timerBoost, 2)){
            unit.effectAt(UnitFx.vtolHover, unit.rotation + 180f, 4f, 0);
        }
    }

}
