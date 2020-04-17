package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.Effects.*;
import mindustry.entities.bullet.*;
import mindustry.world.*;

import static mindustry.Vars.tilesize;

public class ChargeTurret extends PowerTurret{

    public float chargeTime = 30f;
    public int chargeEffects = 5;
    public float chargeMaxDelay = 10f;
    public Effect chargeEffect = Fx.none;
    public Effect chargeBeginEffect = Fx.none;

    public ChargeTurret(String name){
        super(name);
        entityType = LaserTurretEntity::new;
    }

    @Override
    public void shoot(Tile tile, BulletType ammo){
        LaserTurretEntity entity = tile.ent();

        useAmmo(tile);

        tr.trns(entity.rotation, size * tilesize / 2);
        Effects.effect(chargeBeginEffect, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);

        for(int i = 0; i < chargeEffects; i++){
            Time.run(Mathf.random(chargeMaxDelay), () -> {
                if(!isTurret(tile)) return;
                tr.trns(entity.rotation, size * tilesize / 2);
                Effects.effect(chargeEffect, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);
            });
        }

        entity.shooting = true;

        Time.run(chargeTime, () -> {
            if(!isTurret(tile)) return;
            tr.trns(entity.rotation, size * tilesize / 2);
            entity.recoil = recoil;
            entity.heat = 1f;
            bullet(tile, ammo, entity.rotation + Mathf.range(inaccuracy));
            effects(tile);
            entity.shooting = false;
        });
    }

    @Override
    public boolean shouldTurn(Tile tile){
        LaserTurretEntity entity = tile.ent();
        return !entity.shooting;
    }

    public class LaserTurretEntity extends TurretEntity{
        public boolean shooting;
    }
}
