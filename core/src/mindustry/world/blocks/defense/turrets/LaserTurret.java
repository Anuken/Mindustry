package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** A turret that fires a continuous beam with a delay between shots. Liquid coolant is required. Yes, this class name is awful. */
public class LaserTurret extends PowerTurret{
    public float firingMoveFract = 0.25f;
    public float shootDuration = 100f;

    public LaserTurret(String name){
        super(name);

        coolantMultiplier = 1f;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.booster);
        stats.add(Stat.input, StatValues.boosters(reload, coolant.amount, coolantMultiplier, false, this::consumesLiquid));
    }

    @Override
    public void init(){
        super.init();

        if(coolant == null){
            coolant = findConsumer(c -> c instanceof ConsumeLiquidBase);
        }
    }

    public class LaserTurretBuild extends PowerTurretBuild{
        public Seq<BulletEntry> bullets = new Seq<>();

        @Override
        protected void updateCooling(){
            //do nothing, cooling is irrelevant here
        }

        @Override
        public boolean shouldConsume(){
            //still consumes power when bullet is around
            return bullets.any() || isActive() || isShooting();
        }

        @Override
        public void updateTile(){
            super.updateTile();

            bullets.removeAll(b -> !b.bullet.isAdded() || b.bullet.type == null || b.life <= 0f || b.bullet.owner != this);

            if(bullets.any()){

                for(var entry : bullets){
                    float
                    bulletX = x + Angles.trnsx(rotation - 90, shootX + entry.x, shootY + entry.y),
                    bulletY = y + Angles.trnsy(rotation - 90, shootX + entry.x, shootY + entry.y),
                    angle = rotation + entry.rotation;

                    entry.bullet.rotation(angle);
                    entry.bullet.set(bulletX, bulletY);
                    entry.bullet.time = entry.bullet.type.lifetime * entry.bullet.type.optimalLifeFract;
                    entry.bullet.keepAlive = true;
                    entry.life -= Time.delta / Math.max(efficiency, 0.00001f);
                }

                wasShooting = true;
                heat = 1f;
                curRecoil = 1f;
            }else if(reloadCounter > 0){
                wasShooting = true;

                if(coolant != null){
                    //TODO does not handle multi liquid req?
                    Liquid liquid = liquids.current();
                    float maxUsed = coolant.amount;
                    float used = (cheating() ? maxUsed : Math.min(liquids.get(liquid), maxUsed)) * delta();
                    reloadCounter -= used * liquid.heatCapacity * coolantMultiplier;
                    liquids.remove(liquid, used);

                    if(Mathf.chance(0.06 * used)){
                        coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                    }
                }else{
                    reloadCounter -= edelta();
                }
            }
        }

        @Override
        public float progress(){
            return 1f - Mathf.clamp(reloadCounter / reload);
        }

        @Override
        protected void updateReload(){
            //updated in updateTile() depending on coolant
        }

        @Override
        protected void updateShooting(){
            if(bullets.any()){
                return;
            }

            if(reloadCounter <= 0 && efficiency > 0 && !charging() && shootWarmup >= minWarmup){
                BulletType type = peekAmmo();

                shoot(type);

                reloadCounter = reload;
            }
        }

        @Override
        protected void turnToTarget(float targetRot){
            rotation = Angles.moveToward(rotation, targetRot, efficiency * rotateSpeed * delta() * (bullets.any() ? firingMoveFract : 1f));
        }

        @Override
        protected void handleBullet(@Nullable Bullet bullet, float offsetX, float offsetY, float angleOffset){
            if(bullet != null){
                bullets.add(new BulletEntry(bullet, offsetX, offsetY, angleOffset, shootDuration));
            }
        }

        @Override
        public boolean shouldActiveSound(){
            return bullets.any();
        }
    }
}
