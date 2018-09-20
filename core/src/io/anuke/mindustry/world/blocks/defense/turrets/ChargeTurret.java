package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

public class ChargeTurret extends PowerTurret{

    protected float chargeTime = 30f;
    protected int chargeEffects = 5;
    protected float chargeMaxDelay = 10f;
    protected Effect chargeEffect = Fx.none;
    protected Effect chargeBeginEffect = Fx.none;

    public ChargeTurret(String name){
        super(name);
    }

    @Override
    public void shoot(Tile tile, AmmoType ammo){
        LaserTurretEntity entity = tile.entity();

        useAmmo(tile);

        tr.trns(entity.rotation, size * tilesize / 2);
        Effects.effect(chargeBeginEffect, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);

        for(int i = 0; i < chargeEffects; i++){
            Timers.run(Mathf.random(chargeMaxDelay), () -> {
                if(!isTurret(tile)) return;
                tr.trns(entity.rotation, size * tilesize / 2);
                Effects.effect(chargeEffect, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);
            });
        }

        entity.shooting = true;

        Timers.run(chargeTime, () -> {
            if(!isTurret(tile)) return;
            tr.trns(entity.rotation, size * tilesize / 2);
            entity.recoil = recoil;
            entity.heat = 1f;
            bullet(tile, ammo.bullet, entity.rotation + Mathf.range(inaccuracy));
            effects(tile);
            entity.shooting = false;
        });
    }

    @Override
    public boolean shouldTurn(Tile tile){
        LaserTurretEntity entity = tile.entity();
        return !entity.shooting;
    }

    @Override
    public TileEntity newEntity(){
        return new LaserTurretEntity();
    }

    public class LaserTurretEntity extends TurretEntity{
        public boolean shooting;
    }
}
