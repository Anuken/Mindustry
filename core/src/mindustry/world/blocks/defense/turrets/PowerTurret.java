package mindustry.world.blocks.defense.turrets;

import arc.util.ArcAnnotate.*;
import mindustry.entities.bullet.*;
import mindustry.world.meta.*;

public class PowerTurret extends Turret{
    public @NonNull BulletType shootType;
    public float powerUse = 1f;

    public PowerTurret(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.damage, shootType.damage, StatUnit.none);
    }

    @Override
    public void init(){
        consumes.powerCond(powerUse, entity -> ((TurretBuild)entity).target != null);
        super.init();
    }

    public class PowerTurretBuild extends TurretBuild{

        @Override
        public void updateTile(){
            unit.ammo(power.status * unit.type().ammoCapacity);

            super.updateTile();
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
