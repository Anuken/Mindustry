package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.struct.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

/** A turret that fires a continuous beam bullet with no reload or coolant necessary. The bullet only disappears when the turret stops shooting. */
public class ContinuousTurret extends Turret{
    public BulletType shootType;

    public ContinuousTurret(String name){
        super(name);

        coolantMultiplier = 1f;
        envEnabled |= Env.space;
        acceptCoolant = false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.ammo, StatValues.ammo(ObjectMap.of(this, shootType)));
    }

    public class ContinuousTurretBuild extends TurretBuild{
        public Bullet bullet;

        @Override
        protected void updateCooling(){
            //TODO how does coolant work here, if at all?
        }

        @Override
        public BulletType useAmmo(){
            //nothing used directly
            return shootType;
        }

        @Override
        public boolean hasAmmo(){
            //TODO update ammo in unit so it corresponds to liquids
            return cons.canConsume();
        }

        @Override
        public BulletType peekAmmo(){
            return shootType;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            //unclean way of calculating ammo fraction to display
            float ammoFract = efficiency();
            var liq = consumes.getOrNull(ConsumeType.liquid);
            if(liq instanceof ConsumeLiquids cons){
                for(var stack : cons.liquids){
                    ammoFract = Math.min(ammoFract, liquids.get(stack.liquid) / liquidCapacity);
                }
            }else if(liq instanceof ConsumeLiquid cons){
                ammoFract = Math.min(ammoFract, liquids.get(cons.liquid) / liquidCapacity);
            }

            unit.ammo(unit.type().ammoCapacity * ammoFract);

            if(bullet != null){
                //check to see if bullet despawned
                if(bullet.owner != this || !bullet.isAdded() || bullet.type == null){
                    bullet = null;
                }else{
                    wasShooting = true;
                    bullet.rotation(rotation);
                    bullet.set(x + bulletOffset.x, y + bulletOffset.y);
                    heat = 1f;
                    recoil = recoilAmount;

                    if(isShooting()){
                        bullet.time = bullet.lifetime * bullet.type.optimalLifeFract * shootWarmup;
                    }
                }
            }
        }

        @Override
        public double sense(LAccess sensor){
            //no concept of reload here
            if(sensor == LAccess.progress) return bullet == null ? 0f : 1f;
            return super.sense(sensor);
        }

        @Override
        protected void updateShooting(){
            if(bullet != null){
                return;
            }

            if(reload <= 0 && (consValid() || cheating()) && !charging){
                BulletType type = peekAmmo();
                shoot(type);
            }
        }

        @Override
        protected void turnToTarget(float targetRot){
            rotation = Angles.moveToward(rotation, targetRot, efficiency() * rotateSpeed * delta());
        }

        @Override
        protected void bullet(BulletType type, float angle){
            bullet = type.create(this, team, x + bulletOffset.x, y + bulletOffset.y, angle);
        }

        @Override
        public boolean shouldActiveSound(){
            return bullet != null;
        }
    }
}
