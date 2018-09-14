package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;

import static io.anuke.mindustry.Vars.tilesize;

public class LaserTurret extends PowerTurret{
    protected float firingMoveFract = 0.25f;
    protected float shootDuration = 100f;

    public LaserTurret(String name){
        super(name);
    }

    @Override
    protected void updateShooting(Tile tile){
        LaserTurretEntity entity = tile.entity();

        if(entity.bulletLife > 0 && entity.bullet != null){
            tr.trns(entity.rotation, size * tilesize / 2, 0f);
            entity.bullet.setRotation(entity.rotation);
            entity.bullet.set(tile.drawx() + tr.x, tile.drawy() + tr.y);
            entity.bullet.time(0f);
            entity.heat = 1f;
            entity.recoil = recoil;
            entity.bulletLife -= Timers.delta();
            if(entity.bulletLife <= 0f){
                entity.bullet = null;
            }
            return;
        }

        if(entity.reload >= reload){
            AmmoType type = peekAmmo(tile);

            shoot(tile, type);

            entity.reload = 0f;
        }else{
            entity.reload += tile.entity.delta() * peekAmmo(tile).reloadMultiplier;
        }
    }

    @Override
    protected void turnToTarget(Tile tile, float targetRot){
        LaserTurretEntity entity = tile.entity();

        entity.rotation = Angles.moveToward(entity.rotation, targetRot, rotatespeed * entity.delta() * (entity.bulletLife > 0f ? firingMoveFract : 1f));
    }

    @Override
    protected void bullet(Tile tile, BulletType type, float angle){
        LaserTurretEntity entity = tile.entity();

        entity.bullet = Bullet.create(type, tile.entity, tile.getTeam(), tile.drawx() + tr.x, tile.drawy() + tr.y, angle);
        entity.bulletLife = shootDuration;
    }

    @Override
    public TileEntity getEntity(){
        return new LaserTurretEntity();
    }

    class LaserTurretEntity extends TurretEntity{
        Bullet bullet;
        float bulletLife;
    }
}
