package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumePower;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

public abstract class PowerTurret extends CooledTurret{
    /** The percentage of power which will be used per shot. */
    protected float powerUsed = 0.5f;
    protected BulletType shootType;

    public PowerTurret(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.powerShot, powerUsed * consumes.get(ConsumePower.class).powerCapacity, StatUnit.powerUnits);
    }

    @Override
    public boolean hasAmmo(Tile tile){
        // Allow shooting as long as the turret is at least at 50% power
        return tile.entity.power.satisfaction >= powerUsed;
    }

    @Override
    public BulletType useAmmo(Tile tile){
        if(tile.isEnemyCheat()) return shootType;
        // Make sure that power can not go negative in case of threading issues or similar
        tile.entity.power.satisfaction -= Math.min(powerUsed, tile.entity.power.satisfaction);
        return shootType;
    }

    @Override
    public BulletType peekAmmo(Tile tile){
        return shootType;
    }
}
