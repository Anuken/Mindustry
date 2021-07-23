package mindustry.world.blocks.defense.turrets;

import arc.struct.*;
import mindustry.entities.bullet.*;
import mindustry.logic.*;
import mindustry.world.meta.*;

public class PowerTurret extends Turret{
    public BulletType shootType;
    public float powerUse = 1f;

    public PowerTurret(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.ammo, StatValues.ammo(ObjectMap.of(this, shootType)));
    }

    @Override
    public void init(){
        consumes.powerCond(powerUse, TurretBuild::isActive);
        super.init();
    }

    public class PowerTurretBuild extends TurretBuild{

        @Override
        public void updateTile(){
            if(unit != null){
                unit.ammo(power.status * unit.type().ammoCapacity);
            }

            super.updateTile();
        }

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case ammo -> power.status;
                case ammoCapacity -> 1;
                default -> super.sense(sensor);
            };
        }

        @Override
        public BulletType useAmmo(){
            //nothing used directly
            return shootType;
        }

        @Override
        public boolean hasAmmo(){
            //you can always rotate, but never shoot if there's no power
            return true;
        }

        @Override
        public BulletType peekAmmo(){
            return shootType;
        }
    }
}
