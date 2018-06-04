package io.anuke.mindustry.entities.units.types;

import io.anuke.mindustry.entities.units.FlyingUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class Vtol extends FlyingUnit {

    public Vtol(UnitType type, Team team) {
        super(type, team);
    }

    @Override
    public void drawUnder() {
        float rotation = this.rotation - 90;
        float scl = 0.6f + Mathf.absin(Timers.time(), 1f, 0.3f);
        float dy = -6f*scl;

        Draw.color(Palette.lighterOrange, Palette.lightFlame, Mathf.absin(Timers.time(), 3f, 0.7f));

        Draw.rect("vtol-flame",
                x + Angles.trnsx(rotation, 0, dy),
                y + Angles.trnsy(rotation, 0, dy), Mathf.atan2(0, dy) + rotation);

        Draw.color();
    }

    @Override
    public void drawSmooth() {
        Draw.alpha(hitTime / hitDuration);

        Draw.rect(type.name, x, y, rotation - 90);
        for(int i : Mathf.signs){
            Draw.rect(type.name + "-booster-1", x, y, 12*i, 12, rotation - 90);
            Draw.rect(type.name + "-booster-2", x, y, 12*i, 12, rotation - 90);
        }

        Draw.alpha(1f);
    }

    @Override
    public void update() {
        super.update();

        x += Mathf.sin(Timers.time() + id * 999, 25f, 0.07f);
        y += Mathf.cos(Timers.time() + id * 999, 25f, 0.07f);

        if(velocity.len() <= 0.2f){
            rotation += Mathf.sin(Timers.time() + id * 99, 10f, 8f);
        }
    }

}
