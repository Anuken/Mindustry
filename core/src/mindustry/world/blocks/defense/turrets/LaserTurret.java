package mindustry.world.blocks.defense.turrets;

import arc.math.*;
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

        consumes.add(new ConsumeCoolant(0.01f)).update(false);
        coolantMultiplier = 1f;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.booster);
        stats.add(Stat.input, StatValues.boosters(reloadTime, consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount, coolantMultiplier, false, l -> consumes.liquidfilters.get(l.id)));
    }

    public class LaserTurretBuild extends PowerTurretBuild{
        public Bullet bullet;
        public float bulletLife;

        @Override
        protected void updateCooling(){
            //do nothing, cooling is irrelevant here
        }

        @Override
        public boolean shouldConsume(){
            //still consumes power when bullet is around
            return bullet != null || isActive();
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(bullet != null && (!bullet.isAdded() || bullet.type == null)){
                bullet = null;
            }

            if(bulletLife > 0 && bullet != null){
                wasShooting = true;
                bullet.rotation(rotation);
                bullet.set(x + bulletOffset.x, y + bulletOffset.y);
                bullet.time = bullet.type.lifetime * bullet.type.optimalLifeFract;
                heat = 1f;
                recoil = recoilAmount;
                bulletLife -= Time.delta / Math.max(efficiency(), 0.00001f);
                if(bulletLife <= 0f){
                    bullet = null;
                }
            }else if(reload > 0){
                wasShooting = true;
                Liquid liquid = liquids.current();
                float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

                float used = (cheating() ? maxUsed : Math.min(liquids.get(liquid), maxUsed)) * delta();
                reload -= used * liquid.heatCapacity * coolantMultiplier;
                liquids.remove(liquid, used);

                if(Mathf.chance(0.06 * used)){
                    coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                }
            }
        }

        @Override
        public float progress(){
            return 1f - Mathf.clamp(reload / reloadTime);
        }

        @Override
        protected void updateReload(){
            //updated in updateTile() depending on coolant
        }

        @Override
        protected void updateShooting(){
            if(bulletLife > 0 && bullet != null){
                return;
            }

            if(reload <= 0 && (consValid() || cheating()) && !charging && shootWarmup >= minWarmup){
                BulletType type = peekAmmo();

                shoot(type);

                reload = reloadTime;
            }
        }

        @Override
        protected void turnToTarget(float targetRot){
            rotation = Angles.moveToward(rotation, targetRot, efficiency() * rotateSpeed * delta() * (bulletLife > 0f ? firingMoveFract : 1f));
        }

        @Override
        protected void bullet(BulletType type, float angle){
            bullet = type.create(this, team, x + bulletOffset.x, y + bulletOffset.y, angle);
            bulletLife = shootDuration;
        }

        @Override
        public boolean shouldActiveSound(){
            return bulletLife > 0 && bullet != null;
        }
    }
}
