package mindustry.world.blocks.defense.turrets;

import arc.util.ArcAnnotate.*;
import mindustry.entities.bullet.*;
import mindustry.world.meta.*;

public class PowerItemTurret extends ItemTurret{
    public float powerUse = 1f;

    public PowerItemTurret(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void setStats(){
        super.setStats();
    }

    @Override
    public void init(){
        consumes.powerCond(powerUse, entity -> ((ItemTurretEntity)entity).target != null);
        super.init();
    }

    public class PowerItemTurretEntity extends ItemTurretEntity{
        @Override
        protected float baseReloadSpeed(){
            return cheating() ? 1f : power.status;
        }
    }
}