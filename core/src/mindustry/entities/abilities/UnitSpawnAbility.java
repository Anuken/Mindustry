package mindustry.entities.abilities;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class UnitSpawnAbility extends Ability{
    public UnitType unit;
    public float spawnTime = 60f, firstSpawnDelay, spawnX, spawnY;
    public Effect spawnEffect = Fx.spawn;

    protected float timer;

    public UnitSpawnAbility(UnitType unit, float spawnTime, float spawnX, float spawnY){
        this(unit, spawnTime, spawnX, spawnY, 0f);
    }

    public UnitSpawnAbility(UnitType unit, float spawnTime, float spawnX, float spawnY, float firstSpawnDelay){
        this.unit = unit;
        this.spawnTime = spawnTime;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.firstSpawnDelay = firstSpawnDelay;
    }

    public UnitSpawnAbility(){
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta * state.rules.unitBuildSpeedMultiplier;

        if(timer >= spawnTime + firstSpawnDelay && Units.canCreate(unit.team, this.unit)){
            float x = unit.x + Angles.trnsx(unit.rotation, spawnY, -spawnX), y = unit.y + Angles.trnsy(unit.rotation, spawnY, -spawnX);
            spawnEffect.at(x, y);
            Unit u = this.unit.create(unit.team);
            u.set(x, y);
            u.rotation = unit.rotation;
            if(!Vars.net.client()){
                u.add();
            }

            timer = firstSpawnDelay;
        }
    }

    @Override
    public void draw(Unit unit){
        if(Units.canCreate(unit.team, this.unit)){
            Draw.draw(Draw.z(), () -> {
                float x = unit.x + Angles.trnsx(unit.rotation, spawnY, -spawnX), y = unit.y + Angles.trnsy(unit.rotation, spawnY, -spawnX);
                Drawf.construct(x, y, this.unit.icon(Cicon.full), unit.rotation - 90, (timer - firstSpawnDelay) / spawnTime, 1f, timer - firstSpawnDelay);
            });
        }
    }

    @Override
    public String localized(){
        return Core.bundle.format("ability.unitspawn", unit.localizedName);
    }
}
