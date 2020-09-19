package mindustry.entities.abilities;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

public class UnitSpawnAbility extends Ability{
    public @NonNull UnitType type;
    public float spawnTime = 60f, spawnX, spawnY;
    public Effect spawnEffect = Fx.spawn;

    protected float timer;

    public UnitSpawnAbility(@NonNull UnitType type, float spawnTime, float spawnX, float spawnY){
        this.type = type;
        this.spawnTime = spawnTime;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }

    public UnitSpawnAbility(){
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta;

        if(timer >= spawnTime && Units.canCreate(unit.team, type)){

            float x = unit.x + Angles.trnsx(unit.rotation, spawnY, spawnX), y = unit.y + Angles.trnsy(unit.rotation, spawnY, spawnX);
            spawnEffect.at(x, y);
            Unit u = type.create(unit.team);
            u.set(x, y);
            u.rotation = unit.rotation;
            if(!Vars.net.client()){
                u.add();
            }

            timer = 0f;
        }
    }

    @Override
    public void draw(Unit unit){
        if(Units.canCreate(unit.team, type)){
            Draw.draw(Draw.z(), () -> {
                float x = unit.x + Angles.trnsx(unit.rotation, spawnY, spawnX), y = unit.y + Angles.trnsy(unit.rotation, spawnY, spawnX);
                Drawf.construct(x, y, type.icon(Cicon.full), unit.rotation - 90, timer / spawnTime, 1f, timer);
            });
        }
    }
}
