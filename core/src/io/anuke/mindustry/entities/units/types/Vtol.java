package io.anuke.mindustry.entities.units.types;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.units.FlyingUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.game.Team;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class Vtol extends FlyingUnit {
    static TextureRegion
            booster1 = Draw.region("vtol-booster-1"),
            booster2 = Draw.region("vtol-booster-2"),
            region = Draw.region("vtol");

    public Vtol(UnitType type, Team team) {
        super(type, team);
    }

    @Override
    public void draw() {
        Draw.alpha(hitTime / hitDuration);

        Draw.rect(region, x, y, rotation - 90);
        for(int i : Mathf.signs){
            Draw.rect(booster1, x, y, 12*i, 12, rotation - 90);
            Draw.rect(booster2, x, y, 12*i, 12, rotation - 90);
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
