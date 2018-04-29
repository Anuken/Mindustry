package io.anuke.mindustry.entities.units.types;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.FlyingUnitType;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class Vtol extends FlyingUnitType {

    public Vtol(){
        super("vtol");
        setAmmo(AmmoTypes.basicIron);
    }

    @Override
    public void drawUnder(BaseUnit unit) {


        for(int i : Mathf.signs) {

            float rotation = unit.rotation - 90;
            float dx = 5f * i, dx2 = 6f * i;
            float dy = 4f, dy2 = -5f;

            float rad = 1.5f + Mathf.absin(Timers.time(), 3f, 0.6f);
            float ds = 1.2f;

            Draw.color(Palette.lightishOrange, Palette.lightFlame, Mathf.absin(Timers.time(), 3f, 0.3f));

            Fill.circle(
                    unit.x + Angles.trnsx(rotation, dx, dy),
                    unit.y + Angles.trnsy(rotation, dx, dy), rad);

            Fill.circle(
                    unit.x + Angles.trnsx(rotation, dx2, dy2),
                    unit.y + Angles.trnsy(rotation, dx2, dy2), rad);

            Draw.color(Color.GRAY);

            Fill.circle(
                    unit.x + Angles.trnsx(rotation, dx, dy)/ds,
                    unit.y + Angles.trnsy(rotation, dx, dy)/ds, 2f);

            Fill.circle(
                    unit.x + Angles.trnsx(rotation, dx2, dy2)/ds,
                    unit.y + Angles.trnsy(rotation, dx2, dy2)/ds, 2f);

            Draw.color(Color.WHITE);
        }


    }

    @Override
    public void draw(BaseUnit unit) {
        Draw.alpha(unit.hitTime / Unit.hitDuration);

        Draw.rect(name, unit.x, unit.y, unit.rotation - 90);
        for(int i : Mathf.signs){
            Draw.rect(name + "-booster-1", unit.x + i, unit.y, 12*i, 12, unit.rotation - 90);
            Draw.rect(name + "-booster-2", unit.x + i, unit.y, 12*i, 12, unit.rotation - 90);
        }

        Draw.alpha(1f);
    }

    @Override
    public void update(BaseUnit unit) {
        super.update(unit);

        unit.x += Mathf.sin(Timers.time() + unit.id*999, 25f, 0.07f);
        unit.y += Mathf.cos(Timers.time() + unit.id*999, 25f, 0.07f);
        unit.rotation += Mathf.sin(Timers.time() + unit.id*99, 10f, 8f);
    }

    @Override
    public void behavior(BaseUnit unit) {
        //super.behavior(unit);

    }
}
