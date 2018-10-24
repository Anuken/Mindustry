package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

public abstract class PowerTurret extends CooledTurret{
    protected float powerUsed = 0.5f;
    protected AmmoType shootType;

    public PowerTurret(String name){
        super(name);
        hasPower = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.powerShot, powerUsed, StatUnit.powerUnits);
    }

    @Override
    public boolean hasAmmo(Tile tile){
        return tile.entity.power.amount >= powerUsed;
    }

    @Override
    public AmmoType useAmmo(Tile tile){
        if(tile.isEnemyCheat()) return shootType;
        tile.entity.power.amount -= powerUsed;
        return shootType;
    }

    @Override
    public AmmoType peekAmmo(Tile tile){
        return shootType;
    }
}
