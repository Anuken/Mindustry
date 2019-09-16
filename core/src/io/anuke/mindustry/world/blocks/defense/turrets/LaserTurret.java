package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.*;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import static io.anuke.mindustry.Vars.tilesize;

public class LaserTurret extends PowerTurret{
    protected float firingMoveFract = 0.25f;
    protected float shootDuration = 100f;

    public LaserTurret(String name){
        super(name);
        canOverdrive = false;

        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.01f)).update(false);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.boostEffect);
        stats.remove(BlockStat.damage);
        //damages every 5 ticks, at least in meltdown's case
        stats.add(BlockStat.damage, shootType.damage * 60f / 5f, StatUnit.perSecond);
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        LaserTurretEntity entity = tile.entity();

        if(entity.bulletLife > 0 && entity.bullet != null){
            tr.trns(entity.rotation, size * tilesize / 2f, 0f);
            entity.bullet.rot(entity.rotation);
            entity.bullet.set(tile.drawx() + tr.x, tile.drawy() + tr.y);
            entity.bullet.time(0f);
            entity.heat = 1f;
            entity.recoil = recoil;
            entity.bulletLife -= Time.delta();
            if(entity.bulletLife <= 0f){
                entity.bullet = null;
            }
        }
    }

    @Override
    protected void updateShooting(Tile tile){
        LaserTurretEntity entity = tile.entity();

        if(entity.bulletLife > 0 && entity.bullet != null){
            return;
        }

        if(entity.reload >= reload && (entity.cons.valid() || tile.isEnemyCheat())){
            BulletType type = peekAmmo(tile);

            shoot(tile, type);

            entity.reload = 0f;
        }else{
            Liquid liquid = entity.liquids.current();
            float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

            float used = baseReloadSpeed(tile) * (tile.isEnemyCheat() ? maxUsed : Math.min(entity.liquids.get(liquid), maxUsed * Time.delta()));
            entity.reload += used;
            entity.liquids.remove(liquid, used);

            if(Mathf.chance(0.06 * used)){
                Effects.effect(coolEffect, tile.drawx() + Mathf.range(size * tilesize / 2f), tile.drawy() + Mathf.range(size * tilesize / 2f));
            }
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
    public TileEntity newEntity(){
        return new LaserTurretEntity();
    }

    @Override
    public boolean shouldActiveSound(Tile tile){
        LaserTurretEntity entity = tile.entity();

        return entity.bulletLife > 0 && entity.bullet != null;
    }

    class LaserTurretEntity extends TurretEntity{
        Bullet bullet;
        float bulletLife;
    }
}
